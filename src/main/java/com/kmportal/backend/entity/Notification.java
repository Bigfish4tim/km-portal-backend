package com.kmportal.backend.entity;

import com.kmportal.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Notification 엔티티 (알림)
 *
 * 사용자에게 전달되는 모든 알림 정보를 저장하는 엔티티입니다.
 * BaseEntity를 상속받아 생성일시, 수정일시, 삭제 여부 등의 공통 필드를 자동으로 관리합니다.
 *
 * 알림 유형:
 * - NEW_COMMENT: 내 게시글에 새 댓글이 달림
 * - NEW_REPLY: 내 댓글에 대댓글이 달림
 * - NEW_BOARD: 새 공지사항 등록
 * - MENTION: 게시글/댓글에서 멘션됨
 * - SYSTEM: 시스템 알림 (공지, 점검 등)
 * - FILE_SHARED: 파일이 공유됨
 *
 * 작성일: 2025년 11월 26일 (34일차)
 * 작성자: 34일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-26
 */
@Entity
@Table(name = "notifications",  // 데이터베이스 테이블명을 'notifications'로 지정
        indexes = {
                // 인덱스 생성: 자주 조회되는 컬럼에 인덱스를 걸어 검색 성능 향상
                @Index(name = "idx_notification_recipient", columnList = "recipient_id"),    // 수신자별 조회 최적화
                @Index(name = "idx_notification_type", columnList = "type"),                  // 알림 유형별 조회 최적화
                @Index(name = "idx_notification_is_read", columnList = "is_read"),           // 읽음 여부 필터링 최적화
                @Index(name = "idx_notification_created_at", columnList = "created_at")      // 날짜 정렬 최적화
        })
@Data  // Lombok: Getter, Setter, toString, equals, hashCode 자동 생성
@EqualsAndHashCode(callSuper = false)  // BaseEntity 상속 시 경고 제거
@NoArgsConstructor  // Lombok: 파라미터 없는 기본 생성자 자동 생성 (JPA 필수)
@AllArgsConstructor // Lombok: 모든 필드를 파라미터로 받는 생성자 자동 생성
@Builder  // Lombok: 빌더 패턴 자동 생성 (객체 생성을 더 쉽고 읽기 좋게)
public class Notification extends BaseEntity {

    // ====== 필드 정의 ======

    /**
     * 알림 ID (Primary Key)
     *
     * @GeneratedValue: 자동 증가 전략 사용
     * - GenerationType.IDENTITY: 데이터베이스의 AUTO_INCREMENT 기능 활용
     * - INSERT 시 자동으로 1, 2, 3... 순서대로 증가
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    /**
     * 알림 수신자 (User와 다대일 관계)
     *
     * @ManyToOne: 여러 알림(Notification)은 한 명의 사용자(User)에게 속함
     * - Many = Notification (여러 개)
     * - One = User (한 명)
     * - 한 사용자는 여러 알림을 받을 수 있음
     *
     * fetch = FetchType.LAZY: 지연 로딩 (성능 최적화)
     * - 알림 조회 시 수신자 정보는 실제로 필요할 때만 조회
     *
     * @JoinColumn: 외래키(Foreign Key) 설정
     * - name = "recipient_id": 데이터베이스 컬럼명
     * - nullable = false: 수신자는 필수 (알림은 반드시 수신자가 있어야 함)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    @NotNull(message = "알림 수신자는 필수입니다.")
    private User recipient;

    /**
     * 알림 유형
     *
     * 알림의 종류를 구분하는 필드입니다.
     *
     * 알림 유형 목록:
     * - "NEW_COMMENT"   : 내 게시글에 새 댓글
     * - "NEW_REPLY"     : 내 댓글에 대댓글
     * - "NEW_BOARD"     : 새 공지사항 등록
     * - "MENTION"       : 멘션됨 (@사용자명)
     * - "SYSTEM"        : 시스템 알림
     * - "FILE_SHARED"   : 파일 공유됨
     * - "BOARD_PINNED"  : 게시글이 고정됨
     * - "ROLE_CHANGED"  : 권한이 변경됨
     *
     * 최대 50자
     */
    @Column(name = "type", nullable = false, length = 50)
    @NotBlank(message = "알림 유형은 필수입니다.")
    @Size(max = 50, message = "알림 유형은 50자 이하로 입력해주세요.")
    private String type;

    /**
     * 알림 제목
     *
     * 알림 목록에서 표시되는 짧은 제목입니다.
     *
     * 예시:
     * - "새 댓글이 달렸습니다"
     * - "홍길동님이 회원님을 멘션했습니다"
     * - "새 공지사항이 등록되었습니다"
     *
     * 최대 200자
     */
    @Column(name = "title", nullable = false, length = 200)
    @NotBlank(message = "알림 제목은 필수입니다.")
    @Size(max = 200, message = "알림 제목은 200자 이하로 입력해주세요.")
    private String title;

    /**
     * 알림 내용 (상세 메시지)
     *
     * 알림의 상세 내용을 저장합니다.
     *
     * 예시:
     * - "홍길동님이 '프로젝트 회의 안내' 게시글에 댓글을 남겼습니다."
     * - "시스템 점검이 예정되어 있습니다. (2025-11-27 02:00~06:00)"
     *
     * 최대 500자
     */
    @Column(name = "message", length = 500)
    @Size(max = 500, message = "알림 내용은 500자 이하로 입력해주세요.")
    private String message;

    /**
     * 연결 링크 (클릭 시 이동할 URL)
     *
     * 알림을 클릭했을 때 이동할 페이지 경로입니다.
     *
     * 예시:
     * - "/board/123"           : 게시글 상세 페이지
     * - "/board/123#comment-5" : 게시글의 특정 댓글 위치
     * - "/files"               : 파일 관리 페이지
     * - "/admin/users"         : 사용자 관리 페이지
     *
     * 최대 255자
     */
    @Column(name = "link", length = 255)
    @Size(max = 255, message = "링크는 255자 이하로 입력해주세요.")
    private String link;

    /**
     * 읽음 여부
     *
     * false: 읽지 않은 알림 (기본값)
     * true: 읽은 알림
     *
     * 헤더의 알림 뱃지에 표시되는 숫자는
     * isRead = false인 알림의 개수입니다.
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 읽은 일시
     *
     * 알림을 읽은 시점을 기록합니다.
     * isRead가 true로 변경될 때 함께 설정됩니다.
     *
     * NULL이면 아직 읽지 않은 상태입니다.
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 관련 게시글 ID (선택사항)
     *
     * 알림이 특정 게시글과 관련된 경우 해당 게시글 ID를 저장합니다.
     * 통계나 필터링에 활용할 수 있습니다.
     *
     * NULL이면 특정 게시글과 관련 없는 알림입니다.
     */
    @Column(name = "related_board_id")
    private Long relatedBoardId;

    /**
     * 관련 댓글 ID (선택사항)
     *
     * 알림이 특정 댓글과 관련된 경우 해당 댓글 ID를 저장합니다.
     *
     * NULL이면 특정 댓글과 관련 없는 알림입니다.
     */
    @Column(name = "related_comment_id")
    private Long relatedCommentId;

    /**
     * 알림 발신자 이름 (선택사항)
     *
     * 알림을 유발한 사용자의 이름입니다.
     * 직접 User와 관계를 맺지 않고 이름만 저장합니다.
     *
     * 예시:
     * - "홍길동" (댓글 작성자)
     * - "SYSTEM" (시스템 알림)
     *
     * 최대 50자
     */
    @Column(name = "sender_name", length = 50)
    @Size(max = 50, message = "발신자 이름은 50자 이하로 입력해주세요.")
    private String senderName;

    // ====== 비즈니스 메서드들 ======

    /**
     * 알림 읽음 처리 메서드
     *
     * 알림을 읽음 상태로 변경하고 읽은 시간을 기록합니다.
     *
     * 사용 예시:
     * notification.markAsRead();  // 읽음 처리
     * notificationRepository.save(notification);  // 변경사항 저장
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * 알림 읽지 않음 처리 메서드 (복원용)
     *
     * 알림을 읽지 않음 상태로 되돌립니다.
     * 주로 테스트나 관리 목적으로 사용합니다.
     *
     * 사용 예시:
     * notification.markAsUnread();  // 읽지 않음으로 변경
     * notificationRepository.save(notification);
     */
    public void markAsUnread() {
        this.isRead = false;
        this.readAt = null;
    }

    /**
     * 알림이 읽음 상태인지 확인하는 메서드
     *
     * @return true: 읽음, false: 읽지 않음
     *
     * 사용 예시:
     * if (notification.isRead()) {
     *     // 읽은 알림 스타일 적용
     * }
     */
    public boolean isRead() {
        return Boolean.TRUE.equals(this.isRead);
    }

    /**
     * 특정 사용자의 알림인지 확인하는 메서드
     *
     * @param userId 확인할 사용자 ID
     * @return true: 해당 사용자의 알림, false: 다른 사용자의 알림
     *
     * 사용 예시:
     * if (!notification.isRecipient(currentUserId)) {
     *     throw new UnauthorizedException("본인의 알림만 조회할 수 있습니다.");
     * }
     *
     * 참고: User.java에서 getUserId() 메서드를 사용합니다.
     */
    public boolean isRecipient(Long userId) {
        return this.recipient != null &&
                this.recipient.getUserId() != null &&
                this.recipient.getUserId().equals(userId);
    }

    /**
     * 알림 유형별 아이콘 클래스명 반환
     *
     * 프론트엔드에서 알림 유형에 따라 다른 아이콘을 표시할 때 사용합니다.
     *
     * @return Element Plus 아이콘 클래스명
     *
     * 사용 예시:
     * <i :class="notification.getIconClass()"></i>
     */
    public String getIconClass() {
        if (this.type == null) {
            return "Bell";  // 기본 아이콘
        }

        switch (this.type) {
            case "NEW_COMMENT":
            case "NEW_REPLY":
                return "ChatDotRound";      // 댓글 아이콘
            case "NEW_BOARD":
                return "Document";          // 문서 아이콘
            case "MENTION":
                return "ChatLineRound";     // 멘션 아이콘
            case "SYSTEM":
                return "InfoFilled";        // 시스템 알림 아이콘
            case "FILE_SHARED":
                return "Folder";            // 파일 아이콘
            case "BOARD_PINNED":
                return "Star";              // 고정 아이콘
            case "ROLE_CHANGED":
                return "User";              // 사용자 아이콘
            default:
                return "Bell";              // 기본 알림 아이콘
        }
    }

    /**
     * 알림 유형 한글 이름 반환
     *
     * @return 알림 유형의 한글 이름
     */
    public String getTypeDisplayName() {
        if (this.type == null) {
            return "알림";
        }

        switch (this.type) {
            case "NEW_COMMENT":
                return "새 댓글";
            case "NEW_REPLY":
                return "새 답글";
            case "NEW_BOARD":
                return "새 게시글";
            case "MENTION":
                return "멘션";
            case "SYSTEM":
                return "시스템";
            case "FILE_SHARED":
                return "파일 공유";
            case "BOARD_PINNED":
                return "게시글 고정";
            case "ROLE_CHANGED":
                return "권한 변경";
            default:
                return "알림";
        }
    }

    /**
     * 수신자 이름 조회 편의 메서드
     *
     * @return 수신자 이름 (수신자가 없으면 "알 수 없음")
     */
    public String getRecipientName() {
        if (this.recipient != null && this.recipient.getFullName() != null) {
            return this.recipient.getFullName();
        }
        return "알 수 없음";
    }

    /**
     * 수신자 ID 조회 편의 메서드
     *
     * @return 수신자 ID (수신자가 없으면 null)
     */
    public Long getRecipientId() {
        if (this.recipient != null) {
            return this.recipient.getUserId();
        }
        return null;
    }
}

/*
 * ====== 알림 유형 상수 (참고용) ======
 *
 * 향후 Enum으로 변경 가능:
 *
 * public enum NotificationType {
 *     NEW_COMMENT("새 댓글", "ChatDotRound"),
 *     NEW_REPLY("새 답글", "ChatDotRound"),
 *     NEW_BOARD("새 게시글", "Document"),
 *     MENTION("멘션", "ChatLineRound"),
 *     SYSTEM("시스템", "InfoFilled"),
 *     FILE_SHARED("파일 공유", "Folder"),
 *     BOARD_PINNED("게시글 고정", "Star"),
 *     ROLE_CHANGED("권한 변경", "User");
 *
 *     private final String displayName;
 *     private final String iconClass;
 * }
 *
 * ====== 향후 추가할 수 있는 기능들 ======
 *
 * 1. 알림 우선순위:
 *    @Column(name = "priority")
 *    @Builder.Default
 *    private Integer priority = 0;  // 0: 일반, 1: 중요, 2: 긴급
 *
 * 2. 알림 만료일:
 *    @Column(name = "expires_at")
 *    private LocalDateTime expiresAt;  // 만료된 알림은 자동 삭제
 *
 * 3. 알림 그룹화:
 *    @Column(name = "group_key")
 *    private String groupKey;  // 같은 키의 알림은 그룹화
 */