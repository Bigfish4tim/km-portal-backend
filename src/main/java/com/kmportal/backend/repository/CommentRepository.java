package com.kmportal.backend.repository;

import com.kmportal.backend.entity.Comment;
import com.kmportal.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
 *
 * 잘못된 예: findByBoardIdAndIsDeletedFalse (❌ 에러 발생)
 * 올바른 예: findByBoard_IdAndIsDeletedFalse (✅ 정상 동작)
 * =========================================================
 *
 * JpaRepository<Comment, Long>를 상속받으면 자동으로 제공되는 메서드:
 * - save(Comment): 댓글 저장/수정
 * - findById(Long): ID로 댓글 조회
 * - findAll(): 모든 댓글 조회
 * - deleteById(Long): ID로 댓글 삭제
 * - count(): 전체 댓글 수
 *
 * 주요 기능:
 * 1. 기본 CRUD 메서드 (JpaRepository에서 상속)
 * 2. 게시글별 댓글 조회 (최상위 댓글만)
 * 3. 대댓글 조회 (특정 댓글의 자식 댓글)
 * 4. 작성자별 댓글 조회
 * 5. Soft Delete 적용 (삭제된 댓글 제외)
 * 6. 댓글 통계 (게시글별 댓글 수)
 *
 * 작성일: 2025년 11월 21일 (30일차)
 * 수정일: 2025년 11월 24일 (에러 수정 - 언더스코어 추가)
 * 작성자: 30일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.1
 * @since 2025-11-21
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    // ================================
    // 기본 조회 메서드
    // ================================

    /**
     * 삭제되지 않은 댓글 ID로 조회
     *
     * 댓글 상세 조회, 수정, 삭제 시 사용합니다.
     * 삭제된 댓글은 조회되지 않습니다.
     *
     * @param id 댓글 ID
     * @return 댓글 Optional (존재하지 않거나 삭제되었으면 empty)
     *
     * 사용 예시:
     * Optional<Comment> comment = commentRepository.findByIdAndIsDeletedFalse(1L);
     */
    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    // ================================
    // 게시글별 댓글 조회 메서드
    // ⚠️ Board_Id 형식 사용 (연관 엔티티 ID 참조)
    // ================================

    /**
     * 게시글별 최상위 댓글 조회 (페이징)
     *
     * 특정 게시글의 최상위 댓글만 조회합니다.
     * - 삭제되지 않은 댓글만 조회
     * - 대댓글 제외 (parent가 null인 것만)
     * - 페이징 지원
     *
     * ⚠️ Board_Id: board 연관 엔티티의 id 필드를 참조
     * - Comment.board.id를 조회하려면 Board_Id로 작성
     *
     * @param boardId 게시글 ID
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기, 정렬)
     * @return 최상위 댓글 목록 (페이징)
     *
     * 사용 예시:
     * Pageable pageable = PageRequest.of(0, 20, Sort.by("createdAt").ascending());
     * Page<Comment> comments = commentRepository.findByBoard_IdAndIsDeletedFalseAndParentIsNull(1L, pageable);
     */
    Page<Comment> findByBoard_IdAndIsDeletedFalseAndParentIsNull(Long boardId, Pageable pageable);

    /**
     * 게시글별 최상위 댓글 조회 (전체, 페이징 없음)
     *
     * 페이징 없이 특정 게시글의 모든 최상위 댓글을 조회합니다.
     * 댓글이 적은 게시글에서 사용하기 좋습니다.
     *
     * @param boardId 게시글 ID
     * @return 최상위 댓글 목록
     */
    List<Comment> findByBoard_IdAndIsDeletedFalseAndParentIsNullOrderByCreatedAtAsc(Long boardId);

    /**
     * 게시글별 모든 댓글 조회 (대댓글 포함, 페이징)
     *
     * 특정 게시글의 모든 댓글을 조회합니다.
     * 대댓글도 포함됩니다.
     *
     * @param boardId 게시글 ID
     * @param pageable 페이징 정보
     * @return 모든 댓글 목록 (페이징)
     */
    Page<Comment> findByBoard_IdAndIsDeletedFalse(Long boardId, Pageable pageable);

    /**
     * 게시글별 댓글 수 조회
     *
     * 특정 게시글의 댓글 개수를 조회합니다.
     * 삭제되지 않은 댓글만 카운트합니다.
     *
     * @param boardId 게시글 ID
     * @return 댓글 개수
     *
     * 사용 예시:
     * Long commentCount = commentRepository.countByBoard_IdAndIsDeletedFalse(1L);
     */
    Long countByBoard_IdAndIsDeletedFalse(Long boardId);

    // ================================
    // 대댓글 조회 메서드
    // ⚠️ Parent_Id 형식 사용 (Self Referencing)
    // ================================

    /**
     * 특정 댓글의 대댓글 조회
     *
     * 부모 댓글 ID로 해당 댓글의 대댓글들을 조회합니다.
     * 삭제되지 않은 대댓글만 조회합니다.
     * 생성일시 오름차순으로 정렬합니다.
     *
     * ⚠️ Parent_Id: parent 연관 엔티티(Comment)의 id 필드를 참조
     *
     * @param parentId 부모 댓글 ID
     * @return 대댓글 목록
     *
     * 사용 예시:
     * List<Comment> replies = commentRepository.findByParent_IdAndIsDeletedFalseOrderByCreatedAtAsc(1L);
     */
    List<Comment> findByParent_IdAndIsDeletedFalseOrderByCreatedAtAsc(Long parentId);

    /**
     * 특정 댓글의 대댓글 수 조회
     *
     * @param parentId 부모 댓글 ID
     * @return 대댓글 개수
     */
    Long countByParent_IdAndIsDeletedFalse(Long parentId);

    // ================================
    // 작성자별 조회 메서드
    // ================================

    /**
     * 특정 사용자가 작성한 댓글 조회 (페이징)
     *
     * 마이페이지에서 해당 사용자가 작성한 댓글 목록을 볼 때 사용합니다.
     * 삭제되지 않은 댓글만 조회합니다.
     *
     * @param author 작성자
     * @param pageable 페이징 정보
     * @return 해당 사용자가 작성한 댓글 목록 (페이징)
     *
     * 사용 예시:
     * User user = userRepository.findById(userId).orElseThrow();
     * Page<Comment> myComments = commentRepository.findByAuthorAndIsDeletedFalse(user, pageable);
     */
    Page<Comment> findByAuthorAndIsDeletedFalse(User author, Pageable pageable);

    /**
     * 특정 사용자의 댓글 개수 조회
     *
     * @param author 작성자
     * @return 해당 사용자의 댓글 개수
     */
    Long countByAuthorAndIsDeletedFalse(User author);

    /**
     * 특정 사용자의 ID로 댓글 조회 (페이징)
     *
     * ⚠️ Author_UserId: author 연관 엔티티(User)의 userId 필드를 참조
     *
     * @param authorId 작성자 ID
     * @param pageable 페이징 정보
     * @return 해당 사용자가 작성한 댓글 목록 (페이징)
     */
    Page<Comment> findByAuthor_UserIdAndIsDeletedFalse(Long authorId, Pageable pageable);

    // ================================
    // 통계 조회 메서드
    // ================================

    /**
     * 전체 댓글 수 조회 (삭제된 것 제외)
     *
     * @return 전체 댓글 개수
     */
    Long countByIsDeletedFalse();

    /**
     * 오늘 작성된 댓글 수 조회
     *
     * 대시보드나 통계 페이지에서 사용합니다.
     *
     * @param startOfDay 오늘 시작 시간 (00:00:00)
     * @return 오늘 작성된 댓글 개수
     *
     * 사용 예시:
     * LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
     * Long todayCount = commentRepository.countByCreatedAtAfterAndIsDeletedFalse(startOfDay);
     */
    Long countByCreatedAtAfterAndIsDeletedFalse(LocalDateTime startOfDay);

    // ================================
    // JPQL 커스텀 쿼리 메서드
    // (JPQL에서는 c.board.id 형식으로 직접 참조 가능)
    // ================================

    /**
     * 게시글별 최상위 댓글과 대댓글 함께 조회 (N+1 문제 해결)
     *
     * JOIN FETCH를 사용하여 N+1 문제를 해결합니다.
     * - 한 번의 쿼리로 댓글과 작성자 정보를 함께 조회
     * - 성능 최적화에 중요
     *
     * @param boardId 게시글 ID
     * @return 최상위 댓글 목록 (작성자 정보 포함)
     *
     * 사용 예시:
     * List<Comment> comments = commentRepository.findTopLevelCommentsWithAuthor(1L);
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
     *
     * @param parentId 부모 댓글 ID
     * @return 대댓글 목록 (작성자 정보 포함)
     */
    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "WHERE c.parent.id = :parentId " +
            "AND c.isDeleted = false " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findRepliesWithAuthor(@Param("parentId") Long parentId);

    /**
     * 게시글별 댓글 수와 대댓글 수 통계 조회
     *
     * 게시글 목록에서 각 게시글의 댓글 수를 표시할 때 사용합니다.
     *
     * @param boardId 게시글 ID
     * @return 댓글 총 개수 (대댓글 포함)
     */
    @Query("SELECT COUNT(c) FROM Comment c " +
            "WHERE c.board.id = :boardId " +
            "AND c.isDeleted = false")
    Long countAllCommentsByBoardId(@Param("boardId") Long boardId);

    /**
     * 삭제된 댓글 중 대댓글이 있는 것 조회
     *
     * 대댓글이 있는 삭제된 댓글은 "삭제된 댓글입니다"로 표시해야 합니다.
     *
     * @param boardId 게시글 ID
     * @return 대댓글이 있는 삭제된 댓글 목록
     */
    @Query("SELECT c FROM Comment c " +
            "WHERE c.board.id = :boardId " +
            "AND c.isDeleted = true " +
            "AND c.parent IS NULL " +
            "AND EXISTS (SELECT r FROM Comment r WHERE r.parent = c AND r.isDeleted = false)")
    List<Comment> findDeletedCommentsWithActiveReplies(@Param("boardId") Long boardId);

    /**
     * 최근 댓글 조회
     *
     * 대시보드나 메인 페이지에서 최근 댓글을 표시할 때 사용합니다.
     *
     * @param pageable 페이징 정보 (조회할 개수 등)
     * @return 최근 댓글 목록
     *
     * 사용 예시:
     * Pageable pageable = PageRequest.of(0, 10, Sort.by("createdAt").descending());
     * List<Comment> recentComments = commentRepository.findRecentComments(pageable);
     */
    @Query("SELECT c FROM Comment c " +
            "LEFT JOIN FETCH c.author " +
            "LEFT JOIN FETCH c.board " +
            "WHERE c.isDeleted = false " +
            "ORDER BY c.createdAt DESC")
    List<Comment> findRecentComments(Pageable pageable);

    // ================================
    // 존재 여부 확인 메서드
    // ⚠️ Board_Id, Parent_Id 형식 사용
    // ================================

    /**
     * 특정 게시글에 댓글이 존재하는지 확인
     *
     * 게시글 삭제 전 댓글 존재 여부를 확인할 때 사용합니다.
     *
     * @param boardId 게시글 ID
     * @return 댓글 존재 여부
     */
    boolean existsByBoard_IdAndIsDeletedFalse(Long boardId);

    /**
     * 특정 댓글에 대댓글이 존재하는지 확인
     *
     * 댓글 삭제 시 대댓글 존재 여부를 확인할 때 사용합니다.
     * 대댓글이 있으면 완전 삭제 대신 "삭제된 댓글입니다" 표시를 권장합니다.
     *
     * @param parentId 부모 댓글 ID
     * @return 대댓글 존재 여부
     */
    boolean existsByParent_IdAndIsDeletedFalse(Long parentId);
}

/*
 * ====== ⚠️ Spring Data JPA 연관 엔티티 ID 참조 규칙 ======
 *
 * 문제 상황:
 * - Comment 엔티티에 board 필드가 있음 (Board 타입, @ManyToOne)
 * - Comment 엔티티에 boardId 필드는 없음 (직접적인 Long 타입 필드 X)
 *
 * 잘못된 방법:
 * findByBoardIdAndIsDeletedFalse(Long boardId)
 * → "Could not resolve attribute 'boardId' of 'Comment'" 에러 발생!
 *
 * 올바른 방법:
 * findByBoard_IdAndIsDeletedFalse(Long boardId)
 * → board 연관 엔티티의 id 필드를 정상적으로 참조
 *
 * 규칙 정리:
 * ┌─────────────────┬────────────────┬──────────────────────┐
 * │ 연관 엔티티      │ 잘못된 방식    │ 올바른 방식          │
 * ├─────────────────┼────────────────┼──────────────────────┤
 * │ board.id        │ BoardId        │ Board_Id             │
 * │ parent.id       │ ParentId       │ Parent_Id            │
 * │ author.userId   │ AuthorUserId   │ Author_UserId        │
 * └─────────────────┴────────────────┴──────────────────────┘
 *
 * 또는 @Query JPQL 사용 시:
 * @Query("SELECT c FROM Comment c WHERE c.board.id = :boardId")
 * → JPQL에서는 c.board.id 형식으로 직접 참조 가능 (언더스코어 불필요)
 */

/*
 * ====== 변경된 메서드 목록 (v1.0 → v1.1 에러 수정) ======
 *
 * 수정 전 (v1.0)                                    → 수정 후 (v1.1)
 * ──────────────────────────────────────────────────────────────────────
 * findByBoardIdAndIsDeletedFalseAndParentIsNull     → findByBoard_IdAndIsDeletedFalseAndParentIsNull
 * findByBoardIdAndIsDeletedFalseAndParentIsNullOrderByCreatedAtAsc
 *                                                   → findByBoard_IdAndIsDeletedFalseAndParentIsNullOrderByCreatedAtAsc
 * findByBoardIdAndIsDeletedFalse                    → findByBoard_IdAndIsDeletedFalse
 * countByBoardIdAndIsDeletedFalse                   → countByBoard_IdAndIsDeletedFalse
 * findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc
 *                                                   → findByParent_IdAndIsDeletedFalseOrderByCreatedAtAsc
 * countByParentIdAndIsDeletedFalse                  → countByParent_IdAndIsDeletedFalse
 * findByAuthorUserIdAndIsDeletedFalse               → findByAuthor_UserIdAndIsDeletedFalse
 * existsByBoardIdAndIsDeletedFalse                  → existsByBoard_IdAndIsDeletedFalse
 * existsByParentIdAndIsDeletedFalse                 → existsByParent_IdAndIsDeletedFalse
 */