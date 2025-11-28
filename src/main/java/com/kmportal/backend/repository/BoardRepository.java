package com.kmportal.backend.repository;

import com.kmportal.backend.entity.Board;
import com.kmportal.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

/**
 * BoardRepository
 *
 * Board 엔티티를 위한 JPA Repository 인터페이스입니다.
 * Spring Data JPA가 자동으로 구현체를 생성합니다.
 *
 * ==== 37일차 업데이트: N+1 문제 해결 및 쿼리 최적화 ====
 *
 * 1. JOIN FETCH를 사용한 연관 엔티티 즉시 로딩
 * 2. @EntityGraph를 사용한 선언적 페치 전략
 * 3. 커스텀 쿼리 최적화
 *
 * N+1 문제란?
 * - 1개의 쿼리로 N개의 게시글을 조회 후
 * - 각 게시글의 작성자를 조회하기 위해 N번의 추가 쿼리 발생
 * - 예: 게시글 10개 조회 → 11번의 쿼리 실행 (1 + 10)
 *
 * 해결 방법:
 * - JOIN FETCH: JPQL에서 연관 엔티티를 함께 조회
 * - @EntityGraph: 어노테이션으로 페치 전략 지정
 * - @BatchSize: 엔티티에서 배치 로딩 설정
 *
 * 작성일: 2025년 11월 16일 (24일차)
 * 수정일: 2025년 11월 28일 (37일차 - N+1 문제 해결)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.1 (37일차 쿼리 최적화)
 * @since 2025-11-16
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // ================================
    // 37일차 추가: N+1 문제 해결 메서드
    // ================================

    /**
     * 37일차 추가: 작성자 정보와 함께 게시글 목록 조회 (N+1 해결)
     *
     * JOIN FETCH를 사용하여 게시글과 작성자를 한 번의 쿼리로 조회합니다.
     *
     * 기존 쿼리 (N+1 문제):
     * 1. SELECT * FROM boards WHERE is_deleted = false  -- 1번
     * 2. SELECT * FROM users WHERE user_id = ?          -- N번 (게시글 수만큼)
     *
     * 최적화된 쿼리 (JOIN FETCH):
     * SELECT b.*, u.* FROM boards b
     * JOIN users u ON b.author_id = u.user_id
     * WHERE b.is_deleted = false
     * -- 1번의 쿼리로 모든 데이터 조회
     *
     * 주의: JOIN FETCH와 페이징을 함께 사용하면 메모리에서 페이징됨
     * 대용량 데이터에서는 countQuery를 별도로 지정해야 함
     *
     * @param pageable 페이징 정보
     * @return 작성자 정보가 포함된 게시글 목록
     */
    @Query(value = "SELECT b FROM Board b JOIN FETCH b.author WHERE b.isDeleted = false",
            countQuery = "SELECT COUNT(b) FROM Board b WHERE b.isDeleted = false")
    Page<Board> findAllWithAuthor(Pageable pageable);

    /**
     * 37일차 추가: @EntityGraph를 사용한 작성자 정보 즉시 로딩
     *
     * @EntityGraph는 JPQL 없이도 연관 엔티티를 즉시 로딩할 수 있습니다.
     * attributePaths에 로딩할 필드명을 지정합니다.
     *
     * 장점:
     * - 메서드명 쿼리와 함께 사용 가능
     * - 선언적이고 재사용 가능
     *
     * @param pageable 페이징 정보
     * @return 작성자 정보가 포함된 게시글 목록
     */
    @EntityGraph(attributePaths = {"author"})
    Page<Board> findByIsDeletedFalse(Pageable pageable);

    /**
     * 37일차 추가: ID로 게시글 상세 조회 (작성자 정보 포함)
     *
     * 게시글 상세 페이지에서 작성자 이름을 표시하기 위해
     * 작성자 정보를 함께 조회합니다.
     *
     * @param id 게시글 ID
     * @return 작성자 정보가 포함된 게시글 Optional
     */
    @Query("SELECT b FROM Board b JOIN FETCH b.author WHERE b.id = :id AND b.isDeleted = false")
    Optional<Board> findByIdWithAuthor(@Param("id") Long id);

    /**
     * 37일차 추가: 카테고리별 게시글 조회 (작성자 정보 포함)
     *
     * @param category 카테고리
     * @param pageable 페이징 정보
     * @return 작성자 정보가 포함된 해당 카테고리 게시글 목록
     */
    @Query(value = "SELECT b FROM Board b JOIN FETCH b.author " +
            "WHERE b.category = :category AND b.isDeleted = false",
            countQuery = "SELECT COUNT(b) FROM Board b " +
                    "WHERE b.category = :category AND b.isDeleted = false")
    Page<Board> findByCategoryWithAuthor(@Param("category") String category, Pageable pageable);

    /**
     * 37일차 추가: 상단 고정 게시글 조회 (작성자 정보 포함)
     *
     * @return 작성자 정보가 포함된 상단 고정 게시글 목록
     */
    @Query("SELECT b FROM Board b JOIN FETCH b.author " +
            "WHERE b.isPinned = true AND b.isDeleted = false " +
            "ORDER BY b.createdAt DESC")
    List<Board> findPinnedWithAuthor();

    // ================================
    // 기존 메서드 (하위 호환성 유지)
    // ================================

    /**
     * ID로 삭제되지 않은 게시글 조회
     * @deprecated 37일차: findByIdWithAuthor() 사용 권장
     */
    Optional<Board> findByIdAndIsDeletedFalse(Long id);

    /**
     * 카테고리별 게시글 조회 (페이징)
     * @deprecated 37일차: findByCategoryWithAuthor() 사용 권장
     */
    Page<Board> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);

    /**
     * 카테고리별 게시글 개수 조회
     */
    Long countByCategoryAndIsDeletedFalse(String category);

    /**
     * 특정 사용자가 작성한 게시글 조회 (페이징)
     */
    Page<Board> findByAuthorAndIsDeletedFalse(User author, Pageable pageable);

    /**
     * 특정 사용자의 게시글 개수 조회
     */
    Long countByAuthorAndIsDeletedFalse(User author);

    /**
     * 상단 고정 게시글 조회 (최신순)
     * @deprecated 37일차: findPinnedWithAuthor() 사용 권장
     */
    List<Board> findByIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc();

    /**
     * 카테고리별 상단 고정 게시글 조회
     */
    List<Board> findByCategoryAndIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc(String category);

    /**
     * 제목으로 게시글 검색 (부분 일치, 페이징)
     */
    Page<Board> findByTitleContainingIgnoreCaseAndIsDeletedFalse(String keyword, Pageable pageable);

    /**
     * 37일차 업데이트: 복합 검색 쿼리 (작성자 정보 포함)
     *
     * 기존 searchBoards()에 JOIN FETCH 추가하여 N+1 문제 해결
     *
     * @param keyword 검색어
     * @param category 카테고리 (null이면 모든 카테고리)
     * @param authorId 작성자 ID (null이면 모든 작성자)
     * @param pageable 페이징 정보
     * @return 검색 결과 게시글 목록
     */
    @Query(value = "SELECT b FROM Board b JOIN FETCH b.author WHERE " +
            "b.isDeleted = false AND " +
            "(:keyword IS NULL OR " +
            " LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " b.content LIKE CONCAT('%', :keyword, '%') OR " +
            " LOWER(b.author.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:category IS NULL OR b.category = :category) AND " +
            "(:authorId IS NULL OR b.author.userId = :authorId)",
            countQuery = "SELECT COUNT(b) FROM Board b WHERE " +
                    "b.isDeleted = false AND " +
                    "(:keyword IS NULL OR " +
                    " LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
                    " b.content LIKE CONCAT('%', :keyword, '%') OR " +
                    " LOWER(b.author.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
                    "(:category IS NULL OR b.category = :category) AND " +
                    "(:authorId IS NULL OR b.author.userId = :authorId)")
    Page<Board> searchBoardsWithAuthor(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("authorId") Long authorId,
            Pageable pageable
    );

    /**
     * 기존 검색 메서드 (하위 호환성)
     * @deprecated 37일차: searchBoardsWithAuthor() 사용 권장
     */
    @Query("SELECT b FROM Board b WHERE " +
            "b.isDeleted = false AND " +
            "(:keyword IS NULL OR " +
            " LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " b.content LIKE CONCAT('%', :keyword, '%') OR " +
            " LOWER(b.author.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:category IS NULL OR b.category = :category) AND " +
            "(:authorId IS NULL OR b.author.userId = :authorId)")
    Page<Board> searchBoards(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("authorId") Long authorId,
            Pageable pageable
    );

    /**
     * 특정 기간 내 작성된 게시글 조회
     */
    @Query("SELECT b FROM Board b WHERE " +
            "b.isDeleted = false AND " +
            "b.createdAt BETWEEN :startDate AND :endDate")
    Page<Board> findBoardsCreatedBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    // ================================
    // 조회수 관련 메서드
    // ================================

    /**
     * 조회수 증가 (벌크 업데이트)
     *
     * 37일차 참고:
     * - @Modifying 쿼리는 영속성 컨텍스트를 우회
     * - 동시성 문제 해결을 위해 낙관적 락 또는 비관적 락 고려
     */
    @Modifying
    @Query("UPDATE Board b SET b.viewCount = b.viewCount + 1 WHERE b.id = :boardId")
    int increaseViewCount(@Param("boardId") Long boardId);

    /**
     * 조회수가 높은 게시글 조회 (작성자 정보 포함)
     */
    @Query(value = "SELECT b FROM Board b JOIN FETCH b.author " +
            "WHERE b.isDeleted = false ORDER BY b.viewCount DESC",
            countQuery = "SELECT COUNT(b) FROM Board b WHERE b.isDeleted = false")
    Page<Board> findPopularWithAuthor(Pageable pageable);

    /**
     * 조회수 순 정렬
     */
    Page<Board> findByIsDeletedFalseOrderByViewCountDesc(Pageable pageable);

    // ================================
    // 통계 메서드
    // ================================

    /**
     * 삭제되지 않은 전체 게시글 개수 조회
     */
    Long countByIsDeletedFalse();

    /**
     * 카테고리별 게시글 수 통계
     */
    @Query("SELECT b.category, COUNT(b) FROM Board b WHERE " +
            "b.isDeleted = false " +
            "GROUP BY b.category " +
            "ORDER BY COUNT(b) DESC")
    List<Object[]> countByCategory();

    /**
     * 작성자별 게시글 수 통계
     */
    @Query("SELECT b.author, COUNT(b) FROM Board b WHERE " +
            "b.isDeleted = false " +
            "GROUP BY b.author " +
            "ORDER BY COUNT(b) DESC")
    List<Object[]> countByAuthor();

    /**
     * 최근 게시글 조회 (작성자 정보 포함)
     */
    @Query(value = "SELECT b FROM Board b JOIN FETCH b.author " +
            "WHERE b.isDeleted = false ORDER BY b.createdAt DESC",
            countQuery = "SELECT COUNT(b) FROM Board b WHERE b.isDeleted = false")
    Page<Board> findRecentWithAuthor(Pageable pageable);

    /**
     * 최근 게시글 조회 (Top N)
     */
    Page<Board> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // ================================
    // 상단 고정/해제 메서드
    // ================================

    @Modifying
    @Query("UPDATE Board b SET b.isPinned = true WHERE b.id = :boardId")
    int pinBoard(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE Board b SET b.isPinned = false WHERE b.id = :boardId")
    int unpinBoard(@Param("boardId") Long boardId);

    // ================================
    // Soft Delete 메서드
    // ================================

    @Modifying
    @Query("UPDATE Board b SET b.isDeleted = true WHERE b.id = :boardId")
    int softDeleteBoard(@Param("boardId") Long boardId);

    @Modifying
    @Query("UPDATE Board b SET b.isDeleted = false WHERE b.id = :boardId")
    int restoreBoard(@Param("boardId") Long boardId);

    @Query("SELECT b FROM Board b WHERE b.isDeleted = true AND b.deletedAt < :deletedBefore")
    List<Board> findDeletedBoardsOlderThan(@Param("deletedBefore") LocalDateTime deletedBefore);

    // ================================
    // 통계 API용 메서드 (32일차)
    // ================================

    long countByCreatedAtAfter(LocalDateTime createdAt);

    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT AVG(b.viewCount) FROM Board b WHERE b.isDeleted = false")
    Double findAverageViewCount();

    /**
     * 37일차 업데이트: 인기 게시글 TOP 5 조회 (작성자 정보 포함)
     */
    @Query("SELECT b FROM Board b JOIN FETCH b.author " +
            "WHERE b.isDeleted = false ORDER BY b.viewCount DESC")
    List<Board> findTop5PopularWithAuthor(Pageable pageable);

    /**
     * 인기 게시글 TOP 5 조회 (기존)
     */
    List<Board> findTop5ByIsDeletedFalseOrderByViewCountDesc();

    /**
     * 최근 게시글 5개 조회
     */
    List<Board> findTop5ByIsDeletedFalseOrderByCreatedAtDesc();

    @Query("SELECT b.author.department, COUNT(b) FROM Board b " +
            "WHERE b.isDeleted = false AND b.author IS NOT NULL AND b.author.department IS NOT NULL " +
            "GROUP BY b.author.department " +
            "ORDER BY COUNT(b) DESC")
    List<Object[]> findBoardCountByDepartmentRaw();

    default Map<String, Long> findBoardCountByDepartment() {
        List<Object[]> results = findBoardCountByDepartmentRaw();
        Map<String, Long> map = new java.util.HashMap<>();
        for (Object[] row : results) {
            String department = (String) row[0];
            Long count = (Long) row[1];
            map.put(department, count);
        }
        return map;
    }
}

/*
 * ====== 37일차 N+1 문제 해결 가이드 ======
 *
 * 1. 메서드 선택 가이드:
 *
 *    a) 게시글 목록 (작성자 이름 필요):
 *       → findAllWithAuthor() 또는 findByIsDeletedFalse() 사용
 *
 *    b) 게시글 상세 (작성자 정보 필요):
 *       → findByIdWithAuthor() 사용
 *
 *    c) 검색 결과 (작성자 이름 표시):
 *       → searchBoardsWithAuthor() 사용
 *
 *    d) 게시글 개수만 필요 (작성자 불필요):
 *       → countByIsDeletedFalse() 사용
 *
 * 2. 성능 비교:
 *
 *    기존 (N+1 문제):
 *    - 게시글 10개 조회 → 11번 쿼리 (약 50-100ms)
 *
 *    최적화 후 (JOIN FETCH):
 *    - 게시글 10개 조회 → 1번 쿼리 (약 5-10ms)
 *
 * 3. 주의사항:
 *
 *    a) JOIN FETCH + 페이징:
 *       - HHH90003004 경고 발생 가능
 *       - countQuery를 별도로 지정해야 함
 *       - 대용량 데이터에서는 주의
 *
 *    b) @EntityGraph + Collection:
 *       - OneToMany 관계에서는 카테시안 곱 주의
 *       - distinct 사용 고려
 *
 *    c) 영속성 컨텍스트:
 *       - JOIN FETCH로 로딩된 엔티티는 영속 상태
 *       - 수정 시 자동으로 UPDATE 쿼리 발생
 *
 * 4. 모니터링:
 *
 *    // application.yml에서
 *    logging:
 *      level:
 *        org.hibernate.stat: DEBUG
 *
 *    // 콘솔에서 확인
 *    - 실행된 SQL 수
 *    - 엔티티 로딩 수
 *    - 2차 캐시 히트율
 */