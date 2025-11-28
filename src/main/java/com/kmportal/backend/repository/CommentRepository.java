package com.kmportal.backend.repository;

import com.kmportal.backend.entity.Board;
import com.kmportal.backend.entity.Comment;
import com.kmportal.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * CommentRepository
 *
 * Comment 엔티티를 위한 JPA Repository 인터페이스입니다.
 * Spring Data JPA가 자동으로 구현체를 생성합니다.
 *
 * ⚠️ 중요: Spring Data JPA 메서드 네이밍 규칙 (연관 엔티티 참조)
 * =========================================================
 * Comment 엔티티에는 board, parent, author 등 연관 엔티티가 있습니다.
 * 연관 엔티티의 ID를 참조할 때는 언더스코어(_)를 사용해야 합니다!
 *
 * - board (Board 타입) → Board_Id로 참조 (boardId ❌)
 * - parent (Comment 타입) → Parent_Id로 참조 (parentId ❌)
 * - author (User 타입) → Author_UserId로 참조
 * =========================================================
 *
 * ==== 37일차 업데이트: N+1 문제 해결 및 쿼리 최적화 ====
 * - JOIN FETCH 메서드 추가
 * - @EntityGraph 적용
 * - 기존 메서드 100% 유지 (하위 호환성)
 *
 * 작성일: 2025년 11월 21일 (30일차)
 * 수정일: 2025년 11월 28일 (37일차 - N+1 문제 해결 + 기존 메서드 유지)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.2 (37일차 - 완전 통합본)
 * @since 2025-11-21
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ================================================================
    // ✅ 기존 메서드 - 반드시 유지 (CommentService, StatisticsService에서 사용)
    // ================================================================

    // ================================
    // 기본 조회 메서드
    // ================================

    /**
     * 삭제되지 않은 댓글 ID로 조회
     * CommentService에서 사용
     */
    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    // ================================
    // 게시글별 댓글 조회 메서드
    // ⚠️ Board_Id 형식 사용 (연관 엔티티 ID 참조)
    // ================================

    /**
     * 게시글별 최상위 댓글 조회 (페이징)
     * CommentService.getComments()에서 사용
     */
    Page<Comment> findByBoard_IdAndIsDeletedFalseAndParentIsNull(Long boardId, Pageable pageable);

    /**
     * 게시글별 최상위 댓글 조회 (전체, 페이징 없음)
     */
    List<Comment> findByBoard_IdAndIsDeletedFalseAndParentIsNullOrderByCreatedAtAsc(Long boardId);

    /**
     * 게시글별 모든 댓글 조회 (대댓글 포함, 페이징)
     */
    Page<Comment> findByBoard_IdAndIsDeletedFalse(Long boardId, Pageable pageable);

    /**
     * 게시글별 댓글 수 조회
     * CommentService.getCommentCount()에서 사용
     */
    Long countByBoard_IdAndIsDeletedFalse(Long boardId);

    // ================================
    // 대댓글 조회 메서드
    // ⚠️ Parent_Id 형식 사용 (Self Referencing)
    // ================================

    /**
     * 특정 댓글의 대댓글 조회
     */
    List<Comment> findByParent_IdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);

    /**
     * 특정 댓글의 대댓글 수 조회
     */
    Long countByParent_IdAndIsDeletedFalse(Long parentId);

    // ================================
    // 작성자별 조회 메서드
    // ================================

    /**
     * 특정 사용자가 작성한 댓글 조회 (페이징)
     */
    Page<Comment> findByAuthorAndIsDeletedFalse(User author, Pageable pageable);

    /**
     * 특정 사용자의 댓글 개수 조회
     */
    Long countByAuthorAndIsDeletedFalse(User author);

    /**
     * 특정 사용자의 ID로 댓글 조회 (페이징)
     */
    Page<Comment> findByAuthor_UserIdAndIsDeletedFalse(Long authorId, Pageable pageable);

    // ================================
    // 통계 조회 메서드
    // ================================

    /**
     * 전체 댓글 수 조회 (삭제된 것 제외)
     * CommentService.getCommentStatistics()에서 사용
     */
    Long countByIsDeletedFalse();

    /**
     * 오늘 작성된 댓글 수 조회
     * CommentService.getCommentStatistics()에서 사용
     */
    Long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime startOfDay);

    /**
     * 특정 시점 이후 작성된 댓글 수 조회 (삭제 여부 무관)
     * StatisticsService에서 사용
     */
    long countByCreatedAtAfter(LocalDateTime createdAt);

    // ================================
    // JPQL 커스텀 쿼리 메서드 (기존)
    // ================================

    /**
     * 게시글별 최상위 댓글과 작성자 함께 조회 (N+1 문제 해결)
     * CommentService에서 사용
     */
    @Query("SELECT DISTINCT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "WHERE c.board.id = :boardId " +
            "AND c.isDeleted = false " +
            "AND c.parent IS NULL " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelCommentsWithAuthor(@Param("boardId") Long boardId);

    /**
     * 특정 댓글의 대댓글과 작성자 함께 조회 (N+1 문제 해결)
     * CommentService에서 사용
     */
    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "WHERE c.parent.id = :parentId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findRepliesWithAuthor(@Param("parentId") Long parentId);

    /**
     * 게시글별 댓글 수와 대댓글 수 통계 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.board.id = :boardId " +
            "AND c.isDeleted = false")
    Long countAllCommentsByBoardId(@Param("boardId") Long boardId);

    /**
     * 삭제된 댓글 중 대댓글이 있는 것 조회
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.board.id = :boardId " +
            "AND c.isDeleted = true " +
            "AND c.parent IS NULL " +
            "AND EXISTS (SELECT r FROM Comment r WHERE r.parent = c AND r.isDeleted = false)")
    List<Comment> findDeletedCommentsWithActiveReplies(@Param("boardId") Long boardId);

    /**
     * 최근 댓글 조회
     */
    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "LEFT JOIN FETCH c.board " +
            "WHERE c.isDeleted = false " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments(Pageable pageable);

    // ================================
    // 존재 여부 확인 메서드
    // ================================

    /**
     * 특정 게시글에 댓글이 존재하는지 확인
     */
    boolean existsByBoard_IdAndIsDeletedFalse(Long boardId);

    /**
     * 특정 댓글에 대댓글이 존재하는지 확인
     */
    boolean existsByParent_IdAndIsDeletedFalse(Long parentId);

    // ================================
    // 통계 API용 메서드 (32일차)
    // ================================

    /**
     * 최근 댓글 5개 조회 (삭제되지 않은 것만)
     */
    List<Comment> findTop5ByIsDeletedFalseOrderByCreatedAtDesc();

    // ================================================================
    // ✅ 37일차 추가: N+1 문제 해결 최적화 메서드
    // ================================================================

    /**
     * 37일차 추가: 게시글의 최상위 댓글 조회 (작성자 정보 포함) - 개선 버전
     *
     * @param boardId 게시글 ID
     * @return 작성자 정보가 포함된 최상위 댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author " +
            "WHERE c.board.id = :boardId AND c.parent IS NULL AND c.isDeleted = false " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findTopLevelCommentsByBoardIdWithAuthor(@Param("boardId") Long boardId);

    /**
     * 37일차 추가: 부모 댓글의 대댓글 조회 (작성자 정보 포함)
     *
     * @param parentId 부모 댓글 ID
     * @return 작성자 정보가 포함된 대댓글 목록
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author " +
            "WHERE c.parent.id = :parentId AND c.isDeleted = false " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findRepliesByParentIdWithAuthor(@Param("parentId") Long parentId);

    /**
     * 37일차 추가: 게시글의 모든 댓글 조회 (작성자 정보 + 부모 정보 포함)
     *
     * @param boardId 게시글 ID
     * @return 모든 댓글 (작성자, 부모 정보 포함)
     */
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.author " +
            "LEFT JOIN FETCH c.parent " +
            "WHERE c.board.id = :boardId AND c.isDeleted = false " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findAllByBoardIdWithAuthorAndParent(@Param("boardId") Long boardId);

    /**
     * 37일차 추가: 댓글 상세 조회 (작성자 정보 포함)
     *
     * @param id 댓글 ID
     * @return 작성자 정보가 포함된 댓글 Optional
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author " +
            "WHERE c.id = :id AND c.isDeleted = false")
    Optional<Comment> findByIdWithAuthor(@Param("id") Long id);

    /**
     * 37일차 추가: @EntityGraph를 사용한 작성자 정보 즉시 로딩
     *
     * @param board 게시글 엔티티
     * @return 작성자 정보가 포함된 댓글 목록
     */
    @EntityGraph(attributePaths = {"author"})
    List<Comment> findByBoardAndIsDeletedFalseOrderByCreatedAtAsc(Board board);

    /**
     * 37일차 추가: 여러 부모 댓글의 대댓글 일괄 조회
     *
     * @param parentIds 부모 댓글 ID 목록
     * @return 대댓글 목록 (작성자 정보 포함)
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author " +
            "WHERE c.parent.id IN :parentIds AND c.isDeleted = false " +
            "ORDER BY c.parent.id, c.createdAt ASC")
    List<Comment> findRepliesByParentIdsWithAuthor(@Param("parentIds") List<Long> parentIds);

    /**
     * 37일차 추가: 특정 사용자가 작성한 댓글 조회 (게시글 정보 포함)
     */
    @Query(value = "SELECT c FROM Comment c JOIN FETCH c.author JOIN FETCH c.board " +
            "WHERE c.author.userId = :userId AND c.isDeleted = false",
            countQuery = "SELECT COUNT(c) FROM Comment c WHERE c.author.userId = :userId AND c.isDeleted = false")
    Page<Comment> findByAuthorIdWithBoard(@Param("userId") Long userId, Pageable pageable);

    /**
     * 37일차 추가: 게시글의 댓글 수 조회 (JPQL)
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.board.id = :boardId AND c.isDeleted = false")
    Long countByBoardId(@Param("boardId") Long boardId);

    /**
     * 37일차 추가: 게시글의 최상위 댓글 수 조회
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.board.id = :boardId AND c.parent IS NULL AND c.isDeleted = false")
    Long countTopLevelByBoardId(@Param("boardId") Long boardId);

    /**
     * 37일차 추가: 부모 댓글의 대댓글 수 조회 (JPQL)
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.parent.id = :parentId AND c.isDeleted = false")
    Long countRepliesByParentId(@Param("parentId") Long parentId);

    // ================================
    // 37일차 추가: Soft Delete 메서드
    // ================================

    /**
     * 댓글 논리적 삭제
     */
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.id = :commentId")
    int softDeleteComment(@Param("commentId") Long commentId);

    /**
     * 댓글 복구
     */
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = false WHERE c.id = :commentId")
    int restoreComment(@Param("commentId") Long commentId);

    /**
     * 게시글의 모든 댓글 논리적 삭제
     */
    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.board.id = :boardId")
    int softDeleteByBoardId(@Param("boardId") Long boardId);

    // ================================
    // 37일차 추가: 통계 메서드
    // ================================

    /**
     * 특정 기간 내 작성된 댓글 수
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.isDeleted = false AND c.createdAt BETWEEN :startDate AND :endDate")
    Long countByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    /**
     * 가장 댓글이 많은 게시글 조회
     */
    @Query("SELECT c.board.id, COUNT(c) FROM Comment c WHERE c.isDeleted = false GROUP BY c.board.id ORDER BY COUNT(c) DESC")
    List<Object[]> findBoardsWithMostComments(Pageable pageable);

    /**
     * 최근 댓글 조회 (작성자 정보 포함)
     */
    @Query("SELECT c FROM Comment c JOIN FETCH c.author JOIN FETCH c.board " +
            "WHERE c.isDeleted = false ORDER BY c.createdAt DESC")
    List<Comment> findRecentCommentsWithDetails(Pageable pageable);

    // ================================
    // 37일차 추가: 알림 관련 메서드
    // ================================

    /**
     * 특정 게시글의 작성자 ID 조회 (댓글 알림용)
     */
    @Query("SELECT c.board.author.userId FROM Comment c WHERE c.id = :commentId")
    Optional<Long> findBoardAuthorIdByCommentId(@Param("commentId") Long commentId);

    /**
     * 부모 댓글 작성자 ID 조회 (대댓글 알림용)
     */
    @Query("SELECT c.parent.author.userId FROM Comment c WHERE c.id = :commentId AND c.parent IS NOT NULL")
    Optional<Long> findParentAuthorIdByCommentId(@Param("commentId") Long commentId);
}

/*
 * ====== 37일차 업데이트 요약 ======
 *
 * 이 파일은 기존 CommentRepository의 모든 메서드를 유지하면서
 * 37일차 N+1 문제 해결 메서드를 추가한 완전 통합본입니다.
 *
 * 기존 메서드 (유지됨):
 * - findByIdAndIsDeletedFalse
 * - findByBoard_IdAndIsDeletedFalseAndParentIsNull (페이징)
 * - findTopLevelCommentsWithAuthor
 * - findRepliesWithAuthor
 * - countByBoard_IdAndIsDeletedFalse
 * - countByIsDeletedFalse
 * - countByCreatedAtAfterAndIsDeletedFalse
 * - countByCreatedAtAfter
 * - 기타 모든 기존 메서드
 *
 * 37일차 추가 메서드:
 * - findTopLevelCommentsByBoardIdWithAuthor (개선된 버전)
 * - findRepliesByParentIdWithAuthor
 * - findAllByBoardIdWithAuthorAndParent
 * - findByIdWithAuthor
 * - findRepliesByParentIdsWithAuthor (배치 조회)
 * - softDeleteComment, restoreComment (벌크 업데이트)
 *
 * ⚠️ 주의: 언더스코어 규칙
 * - Board_Id, Parent_Id, Author_UserId 형식 사용
 * - JPQL에서는 c.board.id 형식 사용 가능
 */