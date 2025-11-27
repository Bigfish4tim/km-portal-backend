package com.kmportal.backend.service;

import com.kmportal.backend.entity.Board;
import com.kmportal.backend.entity.Comment;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.BoardRepository;
import com.kmportal.backend.repository.CommentRepository;
import com.kmportal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * CommentService
 *
 * 댓글 관련 비즈니스 로직을 처리하는 서비스 클래스입니다.
 *
 * 주요 기능:
 * 1. 댓글 CRUD (생성, 조회, 수정, 삭제)
 * 2. 대댓글 생성 및 조회
 * 3. 권한 검증 (본인 댓글만 수정/삭제)
 * 4. 댓글 통계 조회
 * 5. ⭐ 35일차 추가: 댓글/대댓글 작성 시 알림 자동 생성
 *
 * 보안 정책:
 * - 수정: 본인만 가능
 * - 삭제: 본인 또는 관리자만 가능
 *
 * 작성일: 2025년 11월 21일 (30일차)
 * 수정일: 2025년 11월 27일 (35일차)
 *   - NotificationService 연동 추가
 *   - createComment()에 알림 생성 로직 추가
 *   - createReply()에 알림 생성 로직 추가
 *
 * 작성자: 30일차 개발 담당자
 * 수정자: 35일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.3 (35일차: 알림 연동 추가)
 * @since 2025-11-21
 */
@Service
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성 (의존성 주입)
@Slf4j  // 로깅을 위한 Logger 자동 생성 (log 변수 사용 가능)
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 트랜잭션
public class CommentService {

    // ================================
    // 의존성 주입 (생성자 주입)
    // ================================

    /**
     * CommentRepository - 댓글 데이터 접근
     */
    private final CommentRepository commentRepository;

    /**
     * BoardRepository - 게시글 존재 확인용
     */
    private final BoardRepository boardRepository;

    /**
     * UserRepository - 사용자 조회용
     */
    private final UserRepository userRepository;

    /**
     * ⭐ 35일차 추가: NotificationService - 알림 생성용
     *
     * 댓글/대댓글 작성 시 게시글 작성자 또는 댓글 작성자에게
     * 알림을 전송하기 위해 사용합니다.
     */
    private final NotificationService notificationService;

    // ================================
    // 댓글 생성 메서드
    // ================================

    /**
     * 댓글 작성
     *
     * 특정 게시글에 새로운 댓글을 작성합니다.
     * 현재 로그인한 사용자가 작성자로 설정됩니다.
     *
     * ⭐ 35일차 업데이트:
     * - 댓글 작성 시 게시글 작성자에게 알림을 전송합니다.
     * - 단, 본인 게시글에 본인이 댓글을 작성하는 경우는 제외합니다.
     *
     * @param boardId 게시글 ID
     * @param content 댓글 내용
     * @return 생성된 댓글
     * @throws NoSuchElementException 게시글이 존재하지 않는 경우
     *
     * 사용 예시:
     * Comment comment = commentService.createComment(1L, "좋은 글이네요!");
     */
    @Transactional  // 쓰기 작업이므로 readOnly = false
    public Comment createComment(Long boardId, String content) {
        log.info("댓글 작성 시작 - 게시글 ID: {}", boardId);

        // 1. 게시글 존재 확인
        Board board = boardRepository.findById(boardId)
                .orElseThrow(() -> {
                    log.error("게시글을 찾을 수 없습니다. ID: {}", boardId);
                    return new NoSuchElementException("게시글을 찾을 수 없습니다. ID: " + boardId);
                });

        // 2. 현재 로그인한 사용자 조회
        User currentUser = getCurrentUser();

        // 3. 댓글 엔티티 생성
        Comment comment = Comment.builder()
                .content(content)
                .board(board)
                .author(currentUser)
                .parent(null)  // 최상위 댓글 (대댓글 아님)
                .isDeleted(false)
                .build();

        // 4. 저장
        Comment savedComment = commentRepository.save(comment);

        log.info("댓글 작성 완료 - 댓글 ID: {}, 작성자: {}",
                savedComment.getId(), currentUser.getFullName());

        // ====== 35일차 추가: 알림 생성 ======
        // 게시글 작성자에게 새 댓글 알림 전송
        // 단, 본인 게시글에 본인이 댓글을 작성하는 경우는 알림을 보내지 않음
        try {
            sendNewCommentNotification(board, currentUser);
        } catch (Exception e) {
            // 알림 전송 실패는 댓글 작성에 영향을 주지 않음 (로그만 남김)
            log.warn("댓글 알림 전송 실패 - 댓글 ID: {}, 오류: {}",
                    savedComment.getId(), e.getMessage());
        }
        // ====================================

        return savedComment;
    }

    /**
     * 대댓글 작성
     *
     * 특정 댓글에 대한 대댓글을 작성합니다.
     * 대댓글의 대댓글은 불가능합니다 (1단계만 지원).
     *
     * ⭐ 35일차 업데이트:
     * - 대댓글 작성 시 원 댓글 작성자에게 알림을 전송합니다.
     * - 단, 본인 댓글에 본인이 대댓글을 작성하는 경우는 제외합니다.
     *
     * @param parentCommentId 부모 댓글 ID
     * @param content 대댓글 내용
     * @return 생성된 대댓글
     * @throws NoSuchElementException 부모 댓글이 존재하지 않는 경우
     * @throws IllegalArgumentException 대댓글에 대댓글을 작성하려는 경우
     *
     * 사용 예시:
     * Comment reply = commentService.createReply(1L, "저도 동의합니다.");
     */
    @Transactional
    public Comment createReply(Long parentCommentId, String content) {
        log.info("대댓글 작성 시작 - 부모 댓글 ID: {}", parentCommentId);

        // 1. 부모 댓글 조회 (삭제되지 않은 것만)
        Comment parentComment = commentRepository.findByIdAndIsDeletedFalse(parentCommentId)
                .orElseThrow(() -> {
                    log.error("부모 댓글을 찾을 수 없습니다. ID: {}", parentCommentId);
                    return new NoSuchElementException("부모 댓글을 찾을 수 없습니다. ID: " + parentCommentId);
                });

        // 2. 대댓글의 대댓글 방지 (1단계만 지원)
        // 부모 댓글이 이미 대댓글이면, 그 부모의 부모(최상위 댓글)에 연결
        Comment actualParent = parentComment;
        if (parentComment.isReply()) {
            actualParent = parentComment.getParent();  // 최상위 댓글로 변경
            log.info("대댓글의 대댓글 요청 - 최상위 댓글로 변경. 최상위 댓글 ID: {}", actualParent.getId());
        }

        // 3. 현재 로그인한 사용자 조회
        User currentUser = getCurrentUser();

        // 4. 대댓글 엔티티 생성
        Comment reply = Comment.builder()
                .content(content)
                .board(actualParent.getBoard())  // 같은 게시글
                .author(currentUser)
                .parent(actualParent)  // 부모 댓글 설정
                .isDeleted(false)
                .build();

        // 5. 저장
        Comment savedReply = commentRepository.save(reply);

        // 6. 부모 댓글에 대댓글 추가 (양방향 관계)
        actualParent.addReply(savedReply);

        log.info("대댓글 작성 완료 - 대댓글 ID: {}, 부모 댓글 ID: {}",
                savedReply.getId(), actualParent.getId());

        // ====== 35일차 추가: 알림 생성 ======
        // 원 댓글 작성자에게 새 답글 알림 전송
        // 단, 본인 댓글에 본인이 대댓글을 작성하는 경우는 알림을 보내지 않음
        try {
            sendNewReplyNotification(actualParent, currentUser, savedReply);
        } catch (Exception e) {
            // 알림 전송 실패는 대댓글 작성에 영향을 주지 않음 (로그만 남김)
            log.warn("대댓글 알림 전송 실패 - 대댓글 ID: {}, 오류: {}",
                    savedReply.getId(), e.getMessage());
        }
        // ====================================

        return savedReply;
    }

    // ================================
    // 35일차 추가: 알림 전송 헬퍼 메서드
    // ================================

    /**
     * 새 댓글 알림 전송
     *
     * 게시글 작성자에게 새 댓글 알림을 전송합니다.
     * 본인 게시글에 본인이 댓글을 작성하는 경우는 알림을 보내지 않습니다.
     *
     * @param board 게시글
     * @param commenter 댓글 작성자
     */
    private void sendNewCommentNotification(Board board, User commenter) {
        // 게시글 작성자 확인
        User boardAuthor = board.getAuthor();

        // 본인 게시글에 본인이 댓글을 작성하는 경우 알림 제외
        if (boardAuthor.getUserId().equals(commenter.getUserId())) {
            log.debug("본인 게시글에 본인 댓글 - 알림 전송 생략");
            return;
        }

        // 알림 생성
        notificationService.createNewCommentNotification(
                boardAuthor.getUserId(),    // 알림 수신자: 게시글 작성자
                commenter.getFullName(),    // 댓글 작성자 이름
                board.getId(),              // 게시글 ID
                board.getTitle()            // 게시글 제목
        );

        log.info("새 댓글 알림 전송 완료 - 수신자: {}, 게시글: {}",
                boardAuthor.getUsername(), board.getTitle());
    }

    /**
     * 새 대댓글 알림 전송
     *
     * 원 댓글 작성자에게 새 답글 알림을 전송합니다.
     * 본인 댓글에 본인이 대댓글을 작성하는 경우는 알림을 보내지 않습니다.
     *
     * ⭐ 추가로 게시글 작성자에게도 알림을 전송합니다.
     * (단, 원 댓글 작성자와 게시글 작성자가 같은 경우 중복 전송하지 않음)
     *
     * @param parentComment 부모 댓글
     * @param replier 대댓글 작성자
     * @param reply 생성된 대댓글
     */
    private void sendNewReplyNotification(Comment parentComment, User replier, Comment reply) {
        // 원 댓글 작성자 확인
        User commentAuthor = parentComment.getAuthor();
        Board board = parentComment.getBoard();
        User boardAuthor = board.getAuthor();

        // 1. 원 댓글 작성자에게 알림 전송
        // 본인 댓글에 본인이 대댓글을 작성하는 경우 제외
        if (!commentAuthor.getUserId().equals(replier.getUserId())) {
            notificationService.createNewReplyNotification(
                    commentAuthor.getUserId(),  // 알림 수신자: 원 댓글 작성자
                    replier.getFullName(),      // 대댓글 작성자 이름
                    board.getId(),              // 게시글 ID
                    parentComment.getId()       // 원 댓글 ID
            );

            log.info("새 대댓글 알림 전송 완료 (원 댓글 작성자) - 수신자: {}",
                    commentAuthor.getUsername());
        } else {
            log.debug("본인 댓글에 본인 대댓글 - 알림 전송 생략");
        }

        // 2. 게시글 작성자에게도 알림 전송 (원 댓글 작성자와 다른 경우)
        // - 게시글 작성자 ≠ 원 댓글 작성자
        // - 게시글 작성자 ≠ 대댓글 작성자
        if (!boardAuthor.getUserId().equals(commentAuthor.getUserId()) &&
                !boardAuthor.getUserId().equals(replier.getUserId())) {

            notificationService.createNewCommentNotification(
                    boardAuthor.getUserId(),    // 알림 수신자: 게시글 작성자
                    replier.getFullName(),      // 대댓글 작성자 이름
                    board.getId(),              // 게시글 ID
                    board.getTitle()            // 게시글 제목
            );

            log.info("새 대댓글 알림 전송 완료 (게시글 작성자) - 수신자: {}",
                    boardAuthor.getUsername());
        }
    }

    // ================================
    // 댓글 조회 메서드
    // ================================

    /**
     * 게시글별 댓글 조회 (페이징)
     *
     * 특정 게시글의 최상위 댓글을 페이징하여 조회합니다.
     * 대댓글은 별도로 조회해야 합니다.
     *
     * @param boardId 게시글 ID
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 댓글 목록 (페이징)
     *
     * 사용 예시:
     * Page<Comment> comments = commentService.getCommentsByBoard(1L, 0, 20);
     */
    public Page<Comment> getCommentsByBoard(Long boardId, int page, int size) {
        log.debug("게시글별 댓글 조회 - 게시글 ID: {}, 페이지: {}, 크기: {}", boardId, page, size);

        // 페이징 정보 생성 (생성일시 오름차순 정렬)
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        return commentRepository.findByBoard_IdAndIsDeletedFalseAndParentIsNull(boardId, pageable);
    }

    /**
     * 게시글별 전체 댓글 조회 (JOIN FETCH)
     *
     * N+1 문제를 해결한 메서드로, 댓글과 작성자 정보를 함께 조회합니다.
     * 댓글이 많지 않은 경우에 사용하기 좋습니다.
     *
     * @param boardId 게시글 ID
     * @return 최상위 댓글 목록 (작성자 정보 포함)
     */
    public List<Comment> getAllCommentsByBoard(Long boardId) {
        log.debug("게시글별 전체 댓글 조회 (JOIN FETCH) - 게시글 ID: {}", boardId);
        return commentRepository.findTopLevelCommentsWithAuthor(boardId);
    }

    /**
     * 대댓글 조회
     *
     * 특정 댓글의 대댓글 목록을 조회합니다.
     *
     * @param parentId 부모 댓글 ID
     * @return 대댓글 목록
     *
     * 사용 예시:
     * List<Comment> replies = commentService.getReplies(1L);
     */
    public List<Comment> getReplies(Long parentId) {
        log.debug("대댓글 조회 - 부모 댓글 ID: {}", parentId);
        return commentRepository.findRepliesWithAuthor(parentId);
    }

    /**
     * 댓글 단건 조회
     *
     * @param commentId 댓글 ID
     * @return 댓글
     * @throws NoSuchElementException 댓글이 존재하지 않는 경우
     */
    public Comment getComment(Long commentId) {
        return commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new NoSuchElementException("댓글을 찾을 수 없습니다. ID: " + commentId));
    }

    /**
     * 게시글별 댓글 수 조회
     *
     * @param boardId 게시글 ID
     * @return 댓글 개수
     */
    public Long getCommentCount(Long boardId) {
        return commentRepository.countByBoard_IdAndIsDeletedFalse(boardId);
    }

    // ================================
    // 댓글 수정 메서드
    // ================================

    /**
     * 댓글 수정
     *
     * 본인이 작성한 댓글만 수정할 수 있습니다.
     *
     * @param commentId 댓글 ID
     * @param content 수정할 내용
     * @return 수정된 댓글
     * @throws NoSuchElementException 댓글이 존재하지 않는 경우
     * @throws AccessDeniedException 본인이 작성한 댓글이 아닌 경우
     *
     * 사용 예시:
     * Comment updated = commentService.updateComment(1L, "수정된 내용입니다.");
     */
    @Transactional
    public Comment updateComment(Long commentId, String content) {
        log.info("댓글 수정 시작 - 댓글 ID: {}", commentId);

        // 1. 댓글 조회
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> {
                    log.error("댓글을 찾을 수 없습니다. ID: {}", commentId);
                    return new NoSuchElementException("댓글을 찾을 수 없습니다. ID: " + commentId);
                });

        // 2. 본인 확인 (본인만 수정 가능)
        User currentUser = getCurrentUser();
        if (!comment.isAuthor(currentUser.getUserId())) {
            log.warn("댓글 수정 권한 없음 - 댓글 ID: {}, 요청자: {}", commentId, currentUser.getFullName());
            throw new AccessDeniedException("본인이 작성한 댓글만 수정할 수 있습니다.");
        }

        // 3. 내용 수정
        comment.updateContent(content);
        log.info("댓글 수정 완료 - 댓글 ID: {}", commentId);

        return comment;
    }

    // ================================
    // 댓글 삭제 메서드
    // ================================

    /**
     * 댓글 삭제 (Soft Delete)
     *
     * 본인이 작성한 댓글 또는 관리자만 삭제할 수 있습니다.
     * 실제 데이터는 삭제되지 않고 isDeleted 플래그만 true로 변경됩니다.
     *
     * @param commentId 댓글 ID
     * @throws NoSuchElementException 댓글이 존재하지 않는 경우
     * @throws AccessDeniedException 삭제 권한이 없는 경우
     *
     * 사용 예시:
     * commentService.deleteComment(1L);
     */
    @Transactional
    public void deleteComment(Long commentId) {
        log.info("댓글 삭제 시작 - 댓글 ID: {}", commentId);

        // 1. 댓글 조회
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> {
                    log.error("댓글을 찾을 수 없습니다. ID: {}", commentId);
                    return new NoSuchElementException("댓글을 찾을 수 없습니다. ID: " + commentId);
                });

        // 2. 권한 확인 (본인 또는 관리자만 삭제 가능)
        User currentUser = getCurrentUser();
        boolean isAuthor = comment.isAuthor(currentUser.getUserId());
        boolean isAdmin = hasRole("ROLE_ADMIN");

        if (!isAuthor && !isAdmin) {
            log.warn("댓글 삭제 권한 없음 - 댓글 ID: {}, 요청자: {}", commentId, currentUser.getFullName());
            throw new AccessDeniedException("댓글을 삭제할 권한이 없습니다.");
        }

        // 3. Soft Delete 수행
        comment.softDelete();

        log.info("댓글 삭제 완료 (Soft Delete) - 댓글 ID: {}, 삭제자: {} (관리자: {})",
                commentId, currentUser.getFullName(), isAdmin);
    }

    // ================================
    // 통계 메서드
    // ================================

    /**
     * 댓글 통계 조회
     *
     * 전체 댓글 수와 오늘 작성된 댓글 수를 조회합니다.
     * 대시보드에서 사용됩니다.
     *
     * @return 통계 정보 (Map)
     *
     * 반환 예시:
     * {
     *   "totalCount": 150,
     *   "todayCount": 12
     * }
     */
    public Map<String, Long> getCommentStatistics() {
        Map<String, Long> stats = new HashMap<>();

        // 전체 댓글 수
        Long totalCount = commentRepository.countByIsDeletedFalse();
        stats.put("totalCount", totalCount);

        // 오늘 작성된 댓글 수
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        Long todayCount = commentRepository.countByCreatedAtAfterAndIsDeletedFalse(startOfDay);
        stats.put("todayCount", todayCount);

        log.debug("댓글 통계 조회 - 전체: {}, 오늘: {}", totalCount, todayCount);
        return stats;
    }

    // ================================
    // 보안/인증 관련 헬퍼 메서드
    // ================================

    /**
     * 현재 로그인한 사용자 조회
     *
     * SecurityContextHolder에서 현재 인증 정보를 가져와
     * 해당 사용자의 User 엔티티를 조회합니다.
     *
     * @return 현재 로그인한 사용자
     * @throws NoSuchElementException 사용자를 찾을 수 없는 경우
     */
    private User getCurrentUser() {
        // SecurityContextHolder에서 현재 인증 정보 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증 정보에서 사용자 이름(username) 가져오기
        String username = authentication.getName();

        // 사용자 조회 (username으로 조회 시도, 없으면 email로 조회)
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> {
                    log.error("현재 사용자를 찾을 수 없습니다. username: {}", username);
                    return new NoSuchElementException("사용자를 찾을 수 없습니다.");
                });
    }

    /**
     * 현재 사용자가 특정 권한을 가지고 있는지 확인
     *
     * @param role 확인할 권한 (예: "ROLE_ADMIN")
     * @return 권한 보유 여부
     */
    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals(role));
    }
}

/*
 * ====== 수정 내역 (v1.0 → v1.3) ======
 *
 * v1.1 (2025-11-24):
 * - Repository 메서드명 변경 (연관 엔티티 ID 참조 시 언더스코어 사용)
 *   - findByBoardIdAndIsDeletedFalseAndParentIsNull → findByBoard_IdAndIsDeletedFalseAndParentIsNull
 *   - countByBoardIdAndIsDeletedFalse → countByBoard_IdAndIsDeletedFalse
 *
 * v1.2 (2025-11-24):
 * - User 엔티티 메서드명 수정
 *   - getName() → getFullName() (User.java에는 getName() 메서드가 없음)
 * - UserRepository 메서드명 수정
 *   - findByEmployeeNumber() → findByUsername()
 *
 * v1.3 (2025-11-27) - 35일차:
 * - NotificationService 의존성 추가
 * - createComment()에 알림 생성 로직 추가
 *   - 게시글 작성자에게 새 댓글 알림 전송
 *   - 본인 게시글에 본인 댓글 시 알림 제외
 * - createReply()에 알림 생성 로직 추가
 *   - 원 댓글 작성자에게 새 답글 알림 전송
 *   - 게시글 작성자에게도 알림 전송 (중복 제외)
 *   - 본인 댓글에 본인 대댓글 시 알림 제외
 * - sendNewCommentNotification() 헬퍼 메서드 추가
 * - sendNewReplyNotification() 헬퍼 메서드 추가
 *
 * 알림 전송 정책:
 * 1. 댓글 작성 → 게시글 작성자에게 알림
 * 2. 대댓글 작성 → 원 댓글 작성자에게 알림 + 게시글 작성자에게 알림
 * 3. 본인에게는 알림 전송하지 않음
 * 4. 알림 전송 실패해도 댓글/대댓글 작성은 정상 진행
 */