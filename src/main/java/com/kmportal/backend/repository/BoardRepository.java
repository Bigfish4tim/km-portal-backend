package com.kmportal.backend.repository;

import com.kmportal.backend.entity.Board;
import com.kmportal.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * BoardRepository
 *
 * Board 엔티티를 위한 JPA Repository 인터페이스입니다.
 * Spring Data JPA가 자동으로 구현체를 생성합니다.
 *
 * JpaRepository<Board, Long>를 상속받으면 자동으로 제공되는 메서드:
 * - save(Board): 게시글 저장/수정
 * - findById(Long): ID로 게시글 조회
 * - findAll(): 모든 게시글 조회
 * - deleteById(Long): ID로 게시글 삭제
 * - count(): 전체 게시글 수
 *
 * 주요 기능:
 * 1. 기본 CRUD 메서드 (JpaRepository에서 상속)
 * 2. 삭제되지 않은 게시글만 조회 (Soft Delete)
 * 3. 카테고리별 게시글 조회
 * 4. 작성자별 게시글 조회
 * 5. 상단 고정 게시글 조회
 * 6. 복합 검색 (제목, 내용, 작성자)
 * 7. 조회수 증가
 * 8. 통계 정보 제공
 *
 * 작성일: 2025년 11월 16일 (24일차)
 * 작성자: 24일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-16
 */
@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    // ================================
    // 기본 조회 메서드
    // ================================

    /**
     * 삭제되지 않은 모든 게시글 조회 (페이징)
     *
     * Soft Delete 방식을 사용하여 isDeleted가 false인 게시글만 조회합니다.
     *
     * 메서드명 규칙: findBy + 조건 + Pageable
     * - findBy: 조회를 의미
     * - IsDeletedFalse: isDeleted가 false인 조건
     * - Pageable: 페이징 정보 (페이지 번호, 페이지 크기, 정렬)
     *
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기, 정렬)
     * @return 삭제되지 않은 게시글 목록 (페이징)
     *
     * 사용 예시:
     * Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
     * Page<Board> boards = boardRepository.findByIsDeletedFalse(pageable);
     */
    Page<Board> findByIsDeletedFalse(Pageable pageable);

    /**
     * ID로 삭제되지 않은 게시글 조회
     *
     * 게시글 상세 조회 시 사용합니다.
     * 삭제된 게시글은 조회되지 않습니다.
     *
     * @param id 게시글 ID
     * @return 게시글 Optional (존재하지 않거나 삭제되었으면 empty)
     *
     * 사용 예시:
     * Optional<Board> board = boardRepository.findByIdAndIsDeletedFalse(1L);
     * if (board.isPresent()) {
     *     // 게시글 처리
     * } else {
     *     // 게시글이 없거나 삭제됨
     * }
     */
    Optional<Board> findByIdAndIsDeletedFalse(Long id);

    // ================================
    // 카테고리별 조회 메서드
    // ================================

    /**
     * 카테고리별 게시글 조회 (페이징)
     *
     * 특정 카테고리의 게시글만 조회합니다.
     * 삭제되지 않은 게시글만 조회합니다.
     *
     * @param category 카테고리 (예: "NOTICE", "FREE", "QNA")
     * @param pageable 페이징 정보
     * @return 해당 카테고리의 게시글 목록 (페이징)
     *
     * 사용 예시:
     * Page<Board> notices = boardRepository.findByCategoryAndIsDeletedFalse("NOTICE", pageable);
     */
    Page<Board> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);

    /**
     * 카테고리별 게시글 개수 조회
     *
     * @param category 카테고리
     * @return 해당 카테고리의 게시글 개수
     *
     * 사용 예시:
     * Long count = boardRepository.countByCategoryAndIsDeletedFalse("NOTICE");
     */
    Long countByCategoryAndIsDeletedFalse(String category);

    // ================================
    // 작성자별 조회 메서드
    // ================================

    /**
     * 특정 사용자가 작성한 게시글 조회 (페이징)
     *
     * 마이페이지나 사용자 프로필에서 해당 사용자의 게시글 목록을 볼 때 사용합니다.
     * 삭제되지 않은 게시글만 조회합니다.
     *
     * @param author 작성자
     * @param pageable 페이징 정보
     * @return 해당 사용자가 작성한 게시글 목록 (페이징)
     *
     * 사용 예시:
     * User user = userRepository.findById(userId).orElseThrow();
     * Page<Board> myBoards = boardRepository.findByAuthorAndIsDeletedFalse(user, pageable);
     */
    Page<Board> findByAuthorAndIsDeletedFalse(User author, Pageable pageable);

    /**
     * 특정 사용자의 게시글 개수 조회
     *
     * @param author 작성자
     * @return 해당 사용자의 게시글 개수
     */
    Long countByAuthorAndIsDeletedFalse(User author);

    // ================================
    // 상단 고정 게시글 조회 메서드
    // ================================

    /**
     * 상단 고정 게시글 조회 (최신순)
     *
     * 게시판 목록 상단에 항상 표시되는 공지사항 등을 조회합니다.
     * isPinned가 true이고 삭제되지 않은 게시글만 조회합니다.
     *
     * @return 상단 고정 게시글 목록 (최신순)
     *
     * 사용 예시:
     * List<Board> pinnedBoards = boardRepository.findByIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc();
     */
    List<Board> findByIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc();

    /**
     * 카테고리별 상단 고정 게시글 조회
     *
     * @param category 카테고리
     * @return 해당 카테고리의 상단 고정 게시글 목록
     */
    List<Board> findByCategoryAndIsPinnedTrueAndIsDeletedFalseOrderByCreatedAtDesc(String category);

    // ================================
    // 검색 메서드
    // ================================

    /**
     * 제목으로 게시글 검색 (부분 일치, 페이징)
     *
     * 제목에 검색어가 포함된 게시글을 찾습니다.
     * 대소문자를 구분하지 않고, 삭제되지 않은 게시글만 조회합니다.
     *
     * 메서드명 규칙:
     * - Containing: LIKE '%검색어%' 조건 (부분 일치)
     * - IgnoreCase: 대소문자 구분 안함
     *
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @return 검색 결과 게시글 목록 (페이징)
     *
     * 사용 예시:
     * Page<Board> results = boardRepository.findByTitleContainingIgnoreCaseAndIsDeletedFalse("공지", pageable);
     */
    Page<Board> findByTitleContainingIgnoreCaseAndIsDeletedFalse(String keyword, Pageable pageable);

    /**
     * 복합 검색 쿼리
     *
     * 제목, 내용, 작성자 이름, 카테고리를 동시에 검색합니다.
     * JPQL (Java Persistence Query Language)을 사용한 복잡한 검색입니다.
     *
     * @Query: 직접 JPQL 쿼리를 작성
     * - b: Board 엔티티의 별칭
     * - b.author: Board와 연관된 User 엔티티
     * - LOWER(): 소문자로 변환하여 대소문자 구분 없이 검색
     * - CONCAT(): 문자열 연결 ('%' + 검색어 + '%')
     * - :keyword: 파라미터 바인딩
     * - OR: 여러 조건 중 하나라도 만족하면 조회
     * - AND: 모든 조건을 만족해야 조회
     * - IS NULL OR: 파라미터가 null이면 조건 무시
     *
     * 주의: content는 @Lob(CLOB) 타입이므로 LOWER() 함수를 사용하지 않고
     * LIKE만 사용합니다. 대부분의 DB에서 CLOB 타입은 함수 사용에 제한이 있습니다.
     *
     * @param keyword 검색어 (제목, 내용, 작성자 이름에서 검색)
     * @param category 카테고리 (null이면 모든 카테고리)
     * @param authorId 작성자 ID (null이면 모든 작성자)
     * @param pageable 페이징 정보
     * @return 검색 결과 게시글 목록 (페이징)
     *
     * 사용 예시 1: 제목/내용에 "spring"이 포함된 모든 게시글
     * boardRepository.searchBoards("spring", null, null, pageable);
     *
     * 사용 예시 2: "NOTICE" 카테고리에서 "공지" 검색
     * boardRepository.searchBoards("공지", "NOTICE", null, pageable);
     *
     * 사용 예시 3: 특정 작성자의 게시글에서 "java" 검색
     * boardRepository.searchBoards("java", null, 1L, pageable);
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
     *
     * 통계나 리포트 생성 시 사용합니다.
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param pageable 페이징 정보
     * @return 해당 기간의 게시글 목록 (페이징)
     *
     * 사용 예시:
     * LocalDateTime start = LocalDateTime.now().minusDays(7); // 7일 전
     * LocalDateTime end = LocalDateTime.now();
     * Page<Board> recentBoards = boardRepository.findBoardsCreatedBetween(start, end, pageable);
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
     * 조회수 증가
     *
     * 게시글 상세 조회 시 조회수를 1 증가시킵니다.
     *
     * @Modifying: UPDATE, DELETE 쿼리임을 명시
     * - 이 어노테이션이 없으면 에러 발생
     * - 트랜잭션 내에서만 실행 가능
     *
     * @param boardId 게시글 ID
     * @return 업데이트된 레코드 수 (정상이면 1)
     *
     * 사용 예시 (Service에서):
     * @Transactional
     * public BoardResponseDTO getBoard(Long id) {
     *     boardRepository.increaseViewCount(id);
     *     Board board = boardRepository.findById(id).orElseThrow();
     *     return convertToDTO(board);
     * }
     */
    @Modifying
    @Query("UPDATE Board b SET b.viewCount = b.viewCount + 1 WHERE b.id = :boardId")
    int increaseViewCount(@Param("boardId") Long boardId);

    /**
     * 조회수가 높은 게시글 조회 (인기 게시글)
     *
     * 조회수가 많은 순으로 게시글을 정렬하여 조회합니다.
     * 메인 페이지나 사이드바에 "인기 게시글" 표시용입니다.
     *
     * @param pageable 페이징 정보 (주로 상위 10개 정도)
     * @return 조회수가 많은 게시글 목록
     *
     * 사용 예시:
     * Pageable pageable = PageRequest.of(0, 10);
     * Page<Board> popularBoards = boardRepository.findByIsDeletedFalseOrderByViewCountDesc(pageable);
     */
    Page<Board> findByIsDeletedFalseOrderByViewCountDesc(Pageable pageable);

    // ================================
    // 통계 메서드
    // ================================

    /**
     * 삭제되지 않은 전체 게시글 개수 조회
     *
     * 통계 정보나 대시보드에서 사용합니다.
     *
     * @return 전체 게시글 개수
     *
     * 사용 예시:
     * Long totalBoards = boardRepository.countByIsDeletedFalse();
     */
    Long countByIsDeletedFalse();

    /**
     * 카테고리별 게시글 수 통계
     *
     * 대시보드나 통계 화면에서 카테고리별 게시글 수를 보여줄 때 사용합니다.
     *
     * GROUP BY: 카테고리별로 그룹화
     * COUNT(b): 각 카테고리의 게시글 수
     * ORDER BY COUNT(b) DESC: 게시글이 많은 순으로 정렬
     *
     * @return 카테고리별 게시글 수 (Object[] 형태: [category, count])
     *
     * 사용 예시:
     * List<Object[]> stats = boardRepository.countByCategory();
     * for (Object[] stat : stats) {
     *     String category = (String) stat[0];
     *     Long count = (Long) stat[1];
     *     System.out.println(category + ": " + count + "개");
     * }
     */
    @Query("SELECT b.category, COUNT(b) FROM Board b WHERE " +
            "b.isDeleted = false " +
            "GROUP BY b.category " +
            "ORDER BY COUNT(b) DESC")
    List<Object[]> countByCategory();

    /**
     * 작성자별 게시글 수 통계
     *
     * @return 작성자별 게시글 수 (Object[] 형태: [author, count])
     */
    @Query("SELECT b.author, COUNT(b) FROM Board b WHERE " +
            "b.isDeleted = false " +
            "GROUP BY b.author " +
            "ORDER BY COUNT(b) DESC")
    List<Object[]> countByAuthor();

    /**
     * 최근 게시글 조회 (Top N)
     *
     * 메인 페이지나 대시보드에 "최근 게시글" 표시용입니다.
     *
     * @param pageable 페이징 정보 (주로 상위 10개)
     * @return 최근 작성된 게시글 목록
     *
     * 사용 예시:
     * Pageable pageable = PageRequest.of(0, 10);
     * Page<Board> recentBoards = boardRepository.findByIsDeletedFalseOrderByCreatedAtDesc(pageable);
     */
    Page<Board> findByIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);

    // ================================
    // 상단 고정/해제 메서드
    // ================================

    /**
     * 게시글 상단 고정
     *
     * 관리자가 중요한 게시글을 상단에 고정할 때 사용합니다.
     *
     * @param boardId 게시글 ID
     * @return 업데이트된 레코드 수
     *
     * 사용 예시 (Service에서):
     * @Transactional
     * public void pinBoard(Long id) {
     *     boardRepository.pinBoard(id);
     * }
     */
    @Modifying
    @Query("UPDATE Board b SET b.isPinned = true WHERE b.id = :boardId")
    int pinBoard(@Param("boardId") Long boardId);

    /**
     * 게시글 상단 고정 해제
     *
     * @param boardId 게시글 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Board b SET b.isPinned = false WHERE b.id = :boardId")
    int unpinBoard(@Param("boardId") Long boardId);

    // ================================
    // Soft Delete 메서드
    // ================================

    /**
     * 게시글 논리적 삭제
     *
     * 실제 데이터베이스에서 삭제하지 않고 isDeleted 플래그만 변경합니다.
     *
     * @param boardId 게시글 ID
     * @return 업데이트된 레코드 수
     *
     * 사용 예시 (Service에서):
     * @Transactional
     * public void deleteBoard(Long id) {
     *     boardRepository.softDeleteBoard(id);
     * }
     */
    @Modifying
    @Query("UPDATE Board b SET b.isDeleted = true WHERE b.id = :boardId")
    int softDeleteBoard(@Param("boardId") Long boardId);

    /**
     * 게시글 복구
     *
     * 삭제된 게시글을 복구합니다.
     *
     * @param boardId 게시글 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Board b SET b.isDeleted = false WHERE b.id = :boardId")
    int restoreBoard(@Param("boardId") Long boardId);

    /**
     * 특정 기간 이전에 삭제된 게시글 조회
     *
     * 배치 작업으로 오래된 삭제 게시글을 물리적으로 삭제할 때 사용합니다.
     *
     * @param deletedBefore 기준 날짜 (이 날짜 이전에 삭제된 게시글)
     * @return 오래된 삭제 게시글 목록
     *
     * 사용 예시:
     * LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
     * List<Board> oldDeletedBoards = boardRepository.findDeletedBoardsOlderThan(thirtyDaysAgo);
     * boardRepository.deleteAll(oldDeletedBoards); // 물리적 삭제
     */
    @Query("SELECT b FROM Board b WHERE b.isDeleted = true AND b.deletedAt < :deletedBefore")
    List<Board> findDeletedBoardsOlderThan(@Param("deletedBefore") LocalDateTime deletedBefore);
}

/*
 * ====== Repository 메서드명 작성 규칙 정리 ======
 *
 * Spring Data JPA는 메서드 이름만으로 쿼리를 자동 생성합니다.
 *
 * 1. 기본 키워드:
 *    - findBy: SELECT 조회
 *    - countBy: COUNT 조회
 *    - deleteBy: DELETE 삭제
 *    - existsBy: 존재 여부 확인
 *
 * 2. 조건 키워드:
 *    - And: 그리고 (AND)
 *    - Or: 또는 (OR)
 *    - Is, Equals: 같음 (=)
 *    - IsNot, Not: 같지 않음 (!=)
 *    - IsNull: NULL임
 *    - IsNotNull: NULL이 아님
 *    - IsTrue: true임
 *    - IsFalse: false임
 *
 * 3. 비교 키워드:
 *    - LessThan: 작음 (<)
 *    - LessThanEqual: 작거나 같음 (<=)
 *    - GreaterThan: 큼 (>)
 *    - GreaterThanEqual: 크거나 같음 (>=)
 *    - Between: 범위 (BETWEEN)
 *
 * 4. 문자열 키워드:
 *    - Like: 포함 (LIKE)
 *    - NotLike: 포함하지 않음 (NOT LIKE)
 *    - StartingWith: ~로 시작 (LIKE '값%')
 *    - EndingWith: ~로 끝남 (LIKE '%값')
 *    - Containing: 포함 (LIKE '%값%')
 *    - IgnoreCase: 대소문자 구분 안함
 *
 * 5. 정렬 키워드:
 *    - OrderBy: 정렬
 *    - Asc: 오름차순
 *    - Desc: 내림차순
 *
 * 6. 제한 키워드:
 *    - First: 첫 번째 결과
 *    - Top: 상위 N개 결과
 *    - Distinct: 중복 제거
 *
 * 예시:
 * - findByTitle(String title): title이 일치하는 게시글
 * - findByTitleAndCategory(String title, String category): title과 category가 모두 일치
 * - findByTitleContaining(String keyword): title에 keyword가 포함
 * - findByViewCountGreaterThan(Integer count): 조회수가 count보다 큼
 * - findTop10ByOrderByCreatedAtDesc(): 최근 10개 게시글
 */