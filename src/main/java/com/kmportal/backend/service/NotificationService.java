package com.kmportal.backend.service;

import com.kmportal.backend.entity.Notification;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.NotificationRepository;
import com.kmportal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * NotificationService - 알림 비즈니스 로직 처리 서비스
 *
 * 알림 관련 모든 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * Repository와 Controller 사이에서 데이터 처리, 검증, 변환 등을 담당합니다.
 *
 * 주요 기능:
 * 1. 알림 생성 (다양한 유형별 생성 메서드 제공)
 * 2. 알림 조회 (페이징, 필터링)
 * 3. 알림 읽음 처리 (개별, 전체)
 * 4. 알림 삭제
 * 5. 알림 통계
 *
 * 작성일: 2025년 11월 26일 (34일차)
 * 작성자: 34일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-26
 */
@Service
@RequiredArgsConstructor  // Lombok: final 필드를 파라미터로 받는 생성자 자동 생성
@Slf4j  // Lombok: 로그 객체 자동 생성 (log.info(), log.error() 등 사용 가능)
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 트랜잭션 사용 (성능 최적화)
public class NotificationService {

    // ====== 의존성 주입 ======

    /**
     * 알림 Repository - 알림 엔티티 데이터 접근
     */
    private final NotificationRepository notificationRepository;

    /**
     * 사용자 Repository - 수신자 조회용
     */
    private final UserRepository userRepository;

    // ====== 알림 생성 메서드 ======

    /**
     * 기본 알림 생성 메서드
     *
     * 가장 기본적인 알림 생성 메서드입니다.
     * 모든 필수 정보를 직접 전달받아 알림을 생성합니다.
     *
     * @param recipientId 수신자 ID
     * @param type 알림 유형 (예: "NEW_COMMENT", "SYSTEM")
     * @param title 알림 제목
     * @param message 알림 내용 (선택사항)
     * @param link 클릭 시 이동할 링크 (선택사항)
     * @return 생성된 알림 엔티티
     * @throws IllegalArgumentException 수신자를 찾을 수 없는 경우
     *
     * 사용 예시:
     * Notification notification = notificationService.createNotification(
     *     userId,
     *     "NEW_COMMENT",
     *     "새 댓글이 달렸습니다",
     *     "홍길동님이 회원님의 게시글에 댓글을 남겼습니다.",
     *     "/board/123"
     * );
     */
    @Transactional  // 쓰기 작업이므로 readOnly = false로 오버라이드
    public Notification createNotification(Long recipientId, String type, String title,
                                           String message, String link) {
        // 1. 수신자 조회 및 검증
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "수신자를 찾을 수 없습니다. ID: " + recipientId));

        // 2. 알림 엔티티 생성 (Builder 패턴 사용)
        Notification notification = Notification.builder()
                .recipient(recipient)   // 수신자 설정
                .type(type)             // 알림 유형
                .title(title)           // 제목
                .message(message)       // 내용
                .link(link)             // 이동 링크
                .isRead(false)          // 기본값: 읽지 않음
                .build();

        // 3. 알림 저장 및 반환
        Notification saved = notificationRepository.save(notification);
        log.info("알림 생성 완료 - ID: {}, 수신자: {}, 유형: {}",
                saved.getId(), recipient.getUsername(), type);

        return saved;
    }

    /**
     * 새 댓글 알림 생성
     *
     * 사용자의 게시글에 새 댓글이 달렸을 때 알림을 생성합니다.
     *
     * @param recipientId 게시글 작성자 ID (알림 수신자)
     * @param commenterName 댓글 작성자 이름
     * @param boardId 게시글 ID
     * @param boardTitle 게시글 제목
     * @return 생성된 알림
     *
     * 사용 예시:
     * notificationService.createNewCommentNotification(
     *     boardAuthorId, "홍길동", 123L, "프로젝트 회의 안내"
     * );
     */
    @Transactional
    public Notification createNewCommentNotification(Long recipientId, String commenterName,
                                                     Long boardId, String boardTitle) {
        // 알림 제목 생성
        String title = commenterName + "님이 댓글을 남겼습니다";

        // 알림 메시지 생성 (게시글 제목을 30자로 제한)
        String truncatedTitle = boardTitle.length() > 30
                ? boardTitle.substring(0, 30) + "..."
                : boardTitle;
        String message = "'" + truncatedTitle + "' 게시글에 새 댓글이 달렸습니다.";

        // 이동 링크 생성
        String link = "/board/" + boardId;

        // 알림 생성
        Notification notification = createNotification(
                recipientId, "NEW_COMMENT", title, message, link);

        // 관련 게시글 ID 설정
        notification.setRelatedBoardId(boardId);
        notification.setSenderName(commenterName);

        return notificationRepository.save(notification);
    }

    /**
     * 새 대댓글 알림 생성
     *
     * 사용자의 댓글에 대댓글이 달렸을 때 알림을 생성합니다.
     *
     * @param recipientId 원 댓글 작성자 ID (알림 수신자)
     * @param replierName 대댓글 작성자 이름
     * @param boardId 게시글 ID
     * @param commentId 원 댓글 ID
     * @return 생성된 알림
     */
    @Transactional
    public Notification createNewReplyNotification(Long recipientId, String replierName,
                                                   Long boardId, Long commentId) {
        String title = replierName + "님이 답글을 남겼습니다";
        String message = "회원님의 댓글에 새 답글이 달렸습니다.";
        String link = "/board/" + boardId + "#comment-" + commentId;

        Notification notification = createNotification(
                recipientId, "NEW_REPLY", title, message, link);

        notification.setRelatedBoardId(boardId);
        notification.setRelatedCommentId(commentId);
        notification.setSenderName(replierName);

        return notificationRepository.save(notification);
    }

    /**
     * 멘션 알림 생성
     *
     * 게시글이나 댓글에서 사용자가 멘션(@username)되었을 때 알림을 생성합니다.
     *
     * @param recipientId 멘션된 사용자 ID
     * @param mentionerName 멘션한 사용자 이름
     * @param boardId 게시글 ID
     * @param contentType 멘션된 위치 ("게시글" 또는 "댓글")
     * @return 생성된 알림
     */
    @Transactional
    public Notification createMentionNotification(Long recipientId, String mentionerName,
                                                  Long boardId, String contentType) {
        String title = mentionerName + "님이 회원님을 멘션했습니다";
        String message = contentType + "에서 멘션되었습니다. 확인해보세요!";
        String link = "/board/" + boardId;

        Notification notification = createNotification(
                recipientId, "MENTION", title, message, link);

        notification.setRelatedBoardId(boardId);
        notification.setSenderName(mentionerName);

        return notificationRepository.save(notification);
    }

    /**
     * 시스템 알림 생성 (일반)
     *
     * 시스템에서 발생하는 일반적인 알림을 생성합니다.
     * 예: 점검 안내, 업데이트 공지 등
     *
     * @param recipientId 수신자 ID
     * @param title 알림 제목
     * @param message 알림 내용
     * @return 생성된 알림
     */
    @Transactional
    public Notification createSystemNotification(Long recipientId, String title, String message) {
        Notification notification = createNotification(
                recipientId, "SYSTEM", title, message, null);

        notification.setSenderName("SYSTEM");

        return notificationRepository.save(notification);
    }

    /**
     * 시스템 알림 일괄 생성 (모든 사용자에게)
     *
     * 모든 활성 사용자에게 동일한 시스템 알림을 발송합니다.
     *
     * @param title 알림 제목
     * @param message 알림 내용
     * @return 생성된 알림 개수
     */
    @Transactional
    public int createSystemNotificationForAll(String title, String message) {
        // 모든 활성 사용자 조회
        List<User> activeUsers = userRepository.findByIsActiveTrue();
        int count = 0;

        for (User user : activeUsers) {
            try {
                createSystemNotification(user.getUserId(), title, message);
                count++;
            } catch (Exception e) {
                log.error("시스템 알림 생성 실패 - 사용자: {}", user.getUsername(), e);
            }
        }

        log.info("시스템 알림 일괄 생성 완료 - 대상: {}명", count);
        return count;
    }

    /**
     * 권한 변경 알림 생성
     *
     * 사용자의 권한이 변경되었을 때 알림을 생성합니다.
     *
     * @param recipientId 권한이 변경된 사용자 ID
     * @param newRoleName 새로운 권한명
     * @return 생성된 알림
     */
    @Transactional
    public Notification createRoleChangedNotification(Long recipientId, String newRoleName) {
        String title = "권한이 변경되었습니다";
        String message = "회원님의 권한이 '" + newRoleName + "'(으)로 변경되었습니다.";
        String link = "/mypage";

        Notification notification = createNotification(
                recipientId, "ROLE_CHANGED", title, message, link);

        notification.setSenderName("SYSTEM");

        return notificationRepository.save(notification);
    }

    // ====== 알림 조회 메서드 ======

    /**
     * 알림 목록 조회 (페이징)
     *
     * 특정 사용자의 알림 목록을 페이징하여 조회합니다.
     *
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return 알림 목록 (Page 객체)
     *
     * 사용 예시:
     * Pageable pageable = PageRequest.of(0, 10);
     * Page<Notification> notifications = notificationService.getNotifications(userId, pageable);
     */
    public Page<Notification> getNotifications(Long userId, Pageable pageable) {
        log.debug("알림 목록 조회 - 사용자 ID: {}, 페이지: {}", userId, pageable.getPageNumber());
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 읽음/안읽음 필터링하여 알림 조회
     *
     * @param userId 사용자 ID
     * @param isRead 읽음 여부 (true: 읽은 알림, false: 읽지 않은 알림, null: 전체)
     * @param pageable 페이징 정보
     * @return 필터링된 알림 목록
     */
    public Page<Notification> getNotifications(Long userId, Boolean isRead, Pageable pageable) {
        if (isRead == null) {
            return getNotifications(userId, pageable);
        }
        return notificationRepository.findByRecipientIdAndIsRead(userId, isRead, pageable);
    }

    /**
     * 알림 유형별 조회
     *
     * @param userId 사용자 ID
     * @param type 알림 유형
     * @param pageable 페이징 정보
     * @return 해당 유형의 알림 목록
     */
    public Page<Notification> getNotificationsByType(Long userId, String type, Pageable pageable) {
        return notificationRepository.findByRecipientIdAndType(userId, type, pageable);
    }

    /**
     * 최근 알림 조회 (드롭다운용)
     *
     * 헤더의 알림 드롭다운에 표시할 최근 알림을 조회합니다.
     *
     * @param userId 사용자 ID
     * @param limit 조회할 개수 (기본값: 5)
     * @return 최근 알림 목록
     */
    public List<Notification> getRecentNotifications(Long userId, int limit) {
        log.debug("최근 알림 조회 - 사용자 ID: {}, 개수: {}", userId, limit);
        return notificationRepository.findRecentByRecipientId(userId, PageRequest.of(0, limit));
    }

    /**
     * 최근 알림 5개 조회 (편의 메서드)
     *
     * @param userId 사용자 ID
     * @return 최근 알림 5개
     */
    public List<Notification> getRecentNotifications(Long userId) {
        return getRecentNotifications(userId, 5);
    }

    /**
     * 읽지 않은 알림 개수 조회
     *
     * 헤더의 알림 뱃지에 표시되는 숫자입니다.
     *
     * @param userId 사용자 ID
     * @return 읽지 않은 알림 개수
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByRecipientId(userId);
    }

    /**
     * 단일 알림 조회
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID (권한 검증용)
     * @return 알림 (Optional)
     */
    public Optional<Notification> getNotification(Long notificationId, Long userId) {
        return notificationRepository.findByIdAndRecipientId(notificationId, userId);
    }

    // ====== 알림 읽음 처리 메서드 ======

    /**
     * 개별 알림 읽음 처리
     *
     * 특정 알림을 읽음 상태로 변경합니다.
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID (권한 검증용)
     * @return 읽음 처리된 알림
     * @throws IllegalArgumentException 알림을 찾을 수 없거나 권한이 없는 경우
     *
     * 사용 예시:
     * Notification notification = notificationService.markAsRead(123L, userId);
     */
    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        // 1. 알림 조회 및 권한 검증
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "알림을 찾을 수 없거나 접근 권한이 없습니다. ID: " + notificationId));

        // 2. 이미 읽은 알림이면 그대로 반환
        if (notification.isRead()) {
            log.debug("이미 읽은 알림 - ID: {}", notificationId);
            return notification;
        }

        // 3. 읽음 처리
        notification.markAsRead();
        Notification updated = notificationRepository.save(notification);

        log.info("알림 읽음 처리 완료 - ID: {}, 사용자: {}", notificationId, userId);
        return updated;
    }

    /**
     * 전체 알림 읽음 처리
     *
     * 특정 사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.
     *
     * @param userId 사용자 ID
     * @return 읽음 처리된 알림 개수
     *
     * 사용 예시:
     * int count = notificationService.markAllAsRead(userId);
     * // 결과: 10 (10개의 알림이 읽음 처리됨)
     */
    @Transactional
    public int markAllAsRead(Long userId) {
        int count = notificationRepository.markAllAsReadByRecipientId(userId, LocalDateTime.now());
        log.info("전체 알림 읽음 처리 완료 - 사용자: {}, 개수: {}", userId, count);
        return count;
    }

    // ====== 알림 삭제 메서드 ======

    /**
     * 개별 알림 삭제 (Soft Delete)
     *
     * 특정 알림을 논리적으로 삭제합니다.
     *
     * @param notificationId 알림 ID
     * @param userId 사용자 ID (권한 검증용)
     * @throws IllegalArgumentException 알림을 찾을 수 없거나 권한이 없는 경우
     */
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        // 1. 알림 조회 및 권한 검증
        Notification notification = notificationRepository
                .findByIdAndRecipientId(notificationId, userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "알림을 찾을 수 없거나 접근 권한이 없습니다. ID: " + notificationId));

        // 2. Soft Delete 처리
        notification.softDelete();
        notificationRepository.save(notification);

        log.info("알림 삭제 완료 - ID: {}, 사용자: {}", notificationId, userId);
    }

    /**
     * 오래된 알림 정리 (배치 작업용)
     *
     * 지정된 일수보다 오래된 알림을 삭제합니다.
     * 스케줄러나 배치 작업에서 호출합니다.
     *
     * @param days 보존 기간 (일)
     * @return 삭제된 알림 개수
     *
     * 사용 예시:
     * // 30일 이전의 알림 삭제
     * int deleted = notificationService.cleanOldNotifications(30);
     */
    @Transactional
    public int cleanOldNotifications(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        int count = notificationRepository.softDeleteOlderThan(cutoffDate);
        log.info("오래된 알림 정리 완료 - 기준: {}일 전, 삭제 개수: {}", days, count);
        return count;
    }

    /**
     * 읽은 오래된 알림만 정리
     *
     * 읽지 않은 알림은 보존하고, 읽은 알림 중 오래된 것만 삭제합니다.
     *
     * @param days 보존 기간 (일)
     * @return 삭제된 알림 개수
     */
    @Transactional
    public int cleanOldReadNotifications(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        int count = notificationRepository.softDeleteReadNotificationsOlderThan(cutoffDate);
        log.info("읽은 오래된 알림 정리 완료 - 기준: {}일 전, 삭제 개수: {}", days, count);
        return count;
    }

    // ====== 통계 메서드 ======

    /**
     * 알림 유형별 통계 조회
     *
     * @param userId 사용자 ID
     * @return 유형별 알림 개수 목록
     */
    public List<Object[]> getNotificationStatsByType(Long userId) {
        return notificationRepository.countByTypeForRecipient(userId);
    }

    /**
     * 특정 기간 동안의 알림 개수 조회
     *
     * @param userId 사용자 ID
     * @param days 조회 기간 (일)
     * @return 알림 개수
     */
    public long getNotificationCountForDays(Long userId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        LocalDateTime endDate = LocalDateTime.now();
        return notificationRepository.countByRecipientIdAndDateRange(userId, startDate, endDate);
    }
}

/*
 * ====== 사용 예시 ======
 *
 * // 컨트롤러에서 사용
 * @RestController
 * @RequestMapping("/api/notifications")
 * public class NotificationController {
 *
 *     private final NotificationService notificationService;
 *
 *     // 알림 목록 조회
 *     @GetMapping
 *     public Page<Notification> getNotifications(@AuthenticationPrincipal User user,
 *                                                Pageable pageable) {
 *         return notificationService.getNotifications(user.getUserId(), pageable);
 *     }
 *
 *     // 읽지 않은 알림 개수
 *     @GetMapping("/unread-count")
 *     public long getUnreadCount(@AuthenticationPrincipal User user) {
 *         return notificationService.getUnreadCount(user.getUserId());
 *     }
 * }
 *
 * // 다른 서비스에서 알림 생성
 * @Service
 * public class CommentService {
 *
 *     private final NotificationService notificationService;
 *
 *     public Comment createComment(CommentRequest request, User currentUser) {
 *         // 댓글 저장 로직...
 *
 *         // 게시글 작성자에게 알림 전송
 *         if (!board.getAuthor().getUserId().equals(currentUser.getUserId())) {
 *             notificationService.createNewCommentNotification(
 *                 board.getAuthor().getUserId(),
 *                 currentUser.getFullName(),
 *                 board.getId(),
 *                 board.getTitle()
 *             );
 *         }
 *     }
 * }
 */