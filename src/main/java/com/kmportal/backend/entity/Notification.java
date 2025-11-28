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
 * ==== 37일차 업데이트: 쿼리 최적화 ====
 * 1. 사용자별 알림 조회 최적화 인덱스
 * 2. 읽지 않은 알림 필터링 최적화
 * 3. 오래된 알림 삭제 배치 작업 최적화
 *
 * 작성일: 2025년 11월 26일 (34일차)
 * 수정일: 2025년 11월 28일 (37일차 - 인덱스 최적화)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.1 (37일차 인덱스 최적화)
 * @since 2025-11-26
 */
@Entity
@Table(name = "notifications",
        indexes = {
                // ======================================
                // 기존 단일 컬럼 인덱스
                // ======================================

                /**
                 * 수신자별 조회 인덱스
                 */
                @Index(name = "idx_notification_recipient", columnList = "recipient_id"),

                /**
                 * 알림 유형별 조회 인덱스
                 */
                @Index(name = "idx_notification_type", columnList = "type"),

                // ======================================
                // 37일차 추가: 복합 인덱스 (쿼리 패턴 최적화)
                // ======================================

                /**
                 * 복합 인덱스 1: 수신자별 읽지 않은 알림 개수
                 *
                 * 최적화 대상 쿼리:
                 * SELECT COUNT(*) FROM notifications
                 * WHERE recipient_id = ? AND is_read = false
                 *
                 * 사용 빈도: 매우 높음 (헤더 알림 뱃지, 30초마다 폴링)
                 */
                @Index(name = "idx_notification_recipient_unread",
                        columnList = "recipient_id, is_read"),

                /**
                 * 복합 인덱스 2: 수신자별 알림 목록 (최신순)
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM notifications
                 * WHERE recipient_id = ?
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 높음 (알림 목록 페이지)
                 */
                @Index(name = "idx_notification_recipient_created",
                        columnList = "recipient_id, created_at DESC"),

                /**
                 * 복합 인덱스 3: 수신자별 읽지 않은 알림 목록 (최신순)
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM notifications
                 * WHERE recipient_id = ? AND is_read = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 높음 (알림 드롭다운)
                 */
                @Index(name = "idx_notification_recipient_unread_created",
                        columnList = "recipient_id, is_read, created_at DESC"),

                /**
                 * 복합 인덱스 4: 관련 게시글별 알림
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM notifications
                 * WHERE related_board_id = ?
                 *
                 * 사용 빈도: 중간 (게시글 삭제 시 관련 알림 처리)
                 */
                @Index(name = "idx_notification_related_board",
                        columnList = "related_board_id"),

                /**
                 * 복합 인덱스 5: 오래된 알림 정리 (배치 작업용)
                 *
                 * 최적화 대상 쿼리:
                 * DELETE FROM notifications
                 * WHERE created_at < ? AND is_read = true
                 *
                 * 사용 빈도: 낮음 (정기 배치 작업)
                 */
                @Index(name = "idx_notification_cleanup",
                        columnList = "created_at, is_read")
        })
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification extends BaseEntity {

    // ====== 필드 정의 ======

    /**
     * 알림 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    /**
     * 알림 수신자 (User와 다대일 관계)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    @NotNull(message = "알림 수신자는 필수입니다.")
    private User recipient;

    /**
     * 알림 유형
     * 최대 50자
     */
    @Column(name = "type", nullable = false, length = 50)
    @NotBlank(message = "알림 유형은 필수입니다.")
    @Size(max = 50, message = "알림 유형은 50자 이하로 입력해주세요.")
    private String type;

    /**
     * 알림 제목
     * 최대 200자
     */
    @Column(name = "title", nullable = false, length = 200)
    @NotBlank(message = "알림 제목은 필수입니다.")
    @Size(max = 200, message = "알림 제목은 200자 이하로 입력해주세요.")
    private String title;

    /**
     * 알림 내용 (상세 메시지)
     * 최대 500자
     */
    @Column(name = "message", length = 500)
    @Size(max = 500, message = "알림 내용은 500자 이하로 입력해주세요.")
    private String message;

    /**
     * 연결 링크 (클릭 시 이동할 URL)
     * 최대 255자
     */
    @Column(name = "link", length = 255)
    @Size(max = 255, message = "링크는 255자 이하로 입력해주세요.")
    private String link;

    /**
     * 읽음 여부
     * 기본값 false
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 읽은 일시
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * 관련 게시글 ID (선택사항)
     */
    @Column(name = "related_board_id")
    private Long relatedBoardId;

    /**
     * 관련 댓글 ID (선택사항)
     */
    @Column(name = "related_comment_id")
    private Long relatedCommentId;

    /**
     * 알림 발신자 이름 (선택사항)
     * 최대 50자
     */
    @Column(name = "sender_name", length = 50)
    @Size(max = 50, message = "발신자 이름은 50자 이하로 입력해주세요.")
    private String senderName;

    // ====== 비즈니스 메서드들 ======

    /**
     * 알림 읽음 처리 메서드
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * 알림 읽지 않음 처리 메서드 (복원용)
     */
    public void markAsUnread() {
        this.isRead = false;
        this.readAt = null;
    }

    /**
     * 알림이 읽음 상태인지 확인
     */
    public boolean isRead() {
        return Boolean.TRUE.equals(this.isRead);
    }

    /**
     * 특정 사용자의 알림인지 확인
     */
    public boolean isRecipient(Long userId) {
        return this.recipient != null &&
                this.recipient.getUserId() != null &&
                this.recipient.getUserId().equals(userId);
    }

    /**
     * 알림 유형별 아이콘 클래스명 반환
     */
    public String getIconClass() {
        if (this.type == null) {
            return "Bell";
        }

        switch (this.type) {
            case "NEW_COMMENT":
            case "NEW_REPLY":
                return "ChatDotRound";
            case "NEW_BOARD":
                return "Document";
            case "MENTION":
                return "ChatLineRound";
            case "SYSTEM":
                return "InfoFilled";
            case "FILE_SHARED":
                return "Folder";
            case "BOARD_PINNED":
                return "Star";
            case "ROLE_CHANGED":
                return "User";
            default:
                return "Bell";
        }
    }

    /**
     * 알림 유형 한글 이름 반환
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
     */
    public String getRecipientName() {
        if (this.recipient != null && this.recipient.getFullName() != null) {
            return this.recipient.getFullName();
        }
        return "알 수 없음";
    }

    /**
     * 수신자 ID 조회 편의 메서드
     */
    public Long getRecipientId() {
        if (this.recipient != null) {
            return this.recipient.getUserId();
        }
        return null;
    }
}

/*
 * ====== 37일차 알림 쿼리 최적화 가이드 ======
 *
 * 1. 알림 시스템 특성:
 *    - 읽기 빈도가 쓰기 빈도보다 훨씬 높음
 *    - 사용자별 조회가 대부분 (다른 사용자 알림 조회 없음)
 *    - 읽지 않은 알림 개수 조회가 가장 빈번 (헤더 폴링)
 *
 * 2. 최적화 우선순위:
 *    - 1순위: 읽지 않은 알림 개수 조회 (30초마다 폴링)
 *    - 2순위: 최근 알림 목록 조회 (드롭다운 열 때)
 *    - 3순위: 전체 알림 목록 페이징 조회
 *    - 4순위: 알림 일괄 읽음 처리
 *
 * 3. 성능 모니터링:
 *    - /actuator/metrics/http.server.requests 확인
 *    - 알림 API 응답 시간 < 100ms 목표
 *
 * 4. 알림 데이터 정리 전략:
 *    - 읽은 알림: 30일 후 삭제
 *    - 읽지 않은 알림: 90일 후 삭제
 *    - 배치 스케줄러로 주기적 정리
 *
 * 5. 캐싱 전략 (선택적):
 *    - 읽지 않은 알림 개수: Redis 캐시 (TTL 30초)
 *    - 캐시 무효화: 알림 생성/읽음 처리 시
 */