package com.kmportal.backend.controller;

import com.kmportal.backend.dto.common.ApiResponse;
import com.kmportal.backend.entity.Notification;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NotificationController - 알림 REST API 컨트롤러
 *
 * 알림 관련 HTTP 요청을 처리하는 REST 컨트롤러입니다.
 * 모든 API는 인증된 사용자만 접근 가능합니다.
 *
 * API 엔드포인트 목록:
 * - GET    /api/notifications              : 알림 목록 조회 (페이징)
 * - GET    /api/notifications/unread-count : 읽지 않은 알림 개수 조회
 * - GET    /api/notifications/recent       : 최근 알림 조회 (드롭다운용)
 * - PUT    /api/notifications/{id}/read    : 개별 알림 읽음 처리
 * - PUT    /api/notifications/read-all     : 전체 알림 읽음 처리
 * - DELETE /api/notifications/{id}         : 알림 삭제
 *
 * 작성일: 2025년 11월 26일 (34일차)
 * 작성자: 34일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-26
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor  // Lombok: final 필드를 파라미터로 받는 생성자 자동 생성
@Slf4j  // Lombok: 로그 객체 자동 생성
public class NotificationController {

    // ====== 의존성 주입 ======

    /**
     * 알림 서비스 - 비즈니스 로직 처리
     */
    private final NotificationService notificationService;

    // ====== 알림 조회 API ======

    /**
     * 알림 목록 조회 (페이징)
     *
     * 현재 로그인한 사용자의 알림 목록을 페이징하여 조회합니다.
     *
     * @param user 현재 인증된 사용자 (@AuthenticationPrincipal로 주입)
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param isRead 읽음 여부 필터 (선택사항: true, false, null)
     * @param type 알림 유형 필터 (선택사항)
     * @return 알림 목록 (페이징 정보 포함)
     *
     * 요청 예시:
     * GET /api/notifications?page=0&size=10
     * GET /api/notifications?page=0&size=10&isRead=false
     * GET /api/notifications?page=0&size=10&type=NEW_COMMENT
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "data": {
     *     "content": [ { "id": 1, "title": "새 댓글", ... }, ... ],
     *     "totalElements": 50,
     *     "totalPages": 5,
     *     "number": 0,
     *     "size": 10
     *   }
     * }
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean isRead,
            @RequestParam(required = false) String type) {

        log.info("알림 목록 조회 요청 - 사용자: {}, 페이지: {}, 크기: {}, 읽음: {}, 유형: {}",
                user.getUsername(), page, size, isRead, type);

        try {
            // 페이징 정보 생성 (최신순 정렬)
            Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

            // 필터에 따라 알림 조회
            Page<Notification> notifications;
            if (type != null && !type.isEmpty()) {
                // 유형별 필터링
                notifications = notificationService.getNotificationsByType(
                        user.getUserId(), type, pageable);
            } else if (isRead != null) {
                // 읽음/안읽음 필터링
                notifications = notificationService.getNotifications(
                        user.getUserId(), isRead, pageable);
            } else {
                // 전체 조회
                notifications = notificationService.getNotifications(
                        user.getUserId(), pageable);
            }

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("content", notifications.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList()));
            response.put("totalElements", notifications.getTotalElements());
            response.put("totalPages", notifications.getTotalPages());
            response.put("number", notifications.getNumber());
            response.put("size", notifications.getSize());
            response.put("first", notifications.isFirst());
            response.put("last", notifications.isLast());

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("알림 목록 조회 실패 - 사용자: {}", user.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("알림 목록 조회 중 오류가 발생했습니다: " + e.getMessage()));
        }
    }

    /**
     * 읽지 않은 알림 개수 조회
     *
     * 헤더의 알림 뱃지에 표시할 읽지 않은 알림 개수를 반환합니다.
     * 이 API는 자주 호출되므로 가볍게 유지합니다.
     *
     * @param user 현재 인증된 사용자
     * @return 읽지 않은 알림 개수
     *
     * 요청 예시:
     * GET /api/notifications/unread-count
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "data": { "count": 5 }
     * }
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthenticationPrincipal User user) {

        log.debug("읽지 않은 알림 개수 조회 - 사용자: {}", user.getUsername());

        try {
            long count = notificationService.getUnreadCount(user.getUserId());

            Map<String, Long> response = new HashMap<>();
            response.put("count", count);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("읽지 않은 알림 개수 조회 실패 - 사용자: {}", user.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("알림 개수 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 최근 알림 조회 (드롭다운용)
     *
     * 헤더의 알림 드롭다운에 표시할 최근 알림 목록을 조회합니다.
     * 기본적으로 최근 5개를 반환합니다.
     *
     * @param user 현재 인증된 사용자
     * @param limit 조회할 개수 (기본값: 5, 최대: 10)
     * @return 최근 알림 목록
     *
     * 요청 예시:
     * GET /api/notifications/recent
     * GET /api/notifications/recent?limit=3
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "data": {
     *     "notifications": [ { "id": 1, "title": "새 댓글", ... }, ... ],
     *     "unreadCount": 5
     *   }
     * }
     */
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentNotifications(
            @AuthenticationPrincipal User user,
            @RequestParam(defaultValue = "5") int limit) {

        log.debug("최근 알림 조회 - 사용자: {}, 개수: {}", user.getUsername(), limit);

        try {
            // 최대 10개로 제한
            int actualLimit = Math.min(limit, 10);

            // 최근 알림 조회
            List<Notification> notifications = notificationService
                    .getRecentNotifications(user.getUserId(), actualLimit);

            // 읽지 않은 알림 개수도 함께 반환 (뱃지 표시용)
            long unreadCount = notificationService.getUnreadCount(user.getUserId());

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("notifications", notifications.stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList()));
            response.put("unreadCount", unreadCount);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            log.error("최근 알림 조회 실패 - 사용자: {}", user.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure("최근 알림 조회 중 오류가 발생했습니다."));
        }
    }

    // ====== 알림 읽음 처리 API ======

    /**
     * 개별 알림 읽음 처리
     *
     * 특정 알림을 읽음 상태로 변경합니다.
     * 본인의 알림만 읽음 처리할 수 있습니다.
     *
     * @param id 알림 ID
     * @param user 현재 인증된 사용자
     * @return 읽음 처리된 알림 정보
     *
     * 요청 예시:
     * PUT /api/notifications/123/read
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "알림을 읽음 처리했습니다.",
     *   "data": { "id": 123, "isRead": true, ... }
     * }
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("알림 읽음 처리 요청 - 알림 ID: {}, 사용자: {}", id, user.getUsername());

        try {
            // 알림 읽음 처리
            Notification notification = notificationService.markAsRead(id, user.getUserId());

            // 응답 데이터 구성
            Map<String, Object> response = convertToDto(notification);

            return ResponseEntity.ok(ApiResponse.success(response, "알림을 읽음 처리했습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("알림 읽음 처리 실패 - 알림 ID: {}, 사유: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure(e.getMessage()));

        } catch (Exception e) {
            log.error("알림 읽음 처리 오류 - 알림 ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("알림 읽음 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 전체 알림 읽음 처리
     *
     * 현재 사용자의 모든 읽지 않은 알림을 읽음 상태로 변경합니다.
     *
     * @param user 현재 인증된 사용자
     * @return 읽음 처리된 알림 개수
     *
     * 요청 예시:
     * PUT /api/notifications/read-all
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "10개의 알림을 읽음 처리했습니다.",
     *   "data": { "count": 10 }
     * }
     */
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> markAllAsRead(
            @AuthenticationPrincipal User user) {

        log.info("전체 알림 읽음 처리 요청 - 사용자: {}", user.getUsername());

        try {
            // 전체 읽음 처리
            int count = notificationService.markAllAsRead(user.getUserId());

            // 응답 데이터 구성
            Map<String, Integer> response = new HashMap<>();
            response.put("count", count);

            String message = count > 0
                    ? count + "개의 알림을 읽음 처리했습니다."
                    : "읽지 않은 알림이 없습니다.";

            return ResponseEntity.ok(ApiResponse.success(response, message));

        } catch (Exception e) {
            log.error("전체 알림 읽음 처리 오류 - 사용자: {}", user.getUsername(), e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("전체 알림 읽음 처리 중 오류가 발생했습니다."));
        }
    }

    // ====== 알림 삭제 API ======

    /**
     * 알림 삭제
     *
     * 특정 알림을 삭제합니다. (Soft Delete)
     * 본인의 알림만 삭제할 수 있습니다.
     *
     * @param id 알림 ID
     * @param user 현재 인증된 사용자
     * @return 삭제 결과
     *
     * 요청 예시:
     * DELETE /api/notifications/123
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "알림이 삭제되었습니다."
     * }
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @PathVariable Long id,
            @AuthenticationPrincipal User user) {

        log.info("알림 삭제 요청 - 알림 ID: {}, 사용자: {}", id, user.getUsername());

        try {
            // 알림 삭제
            notificationService.deleteNotification(id, user.getUserId());

            return ResponseEntity.ok(ApiResponse.success("알림이 삭제되었습니다."));

        } catch (IllegalArgumentException e) {
            log.warn("알림 삭제 실패 - 알림 ID: {}, 사유: {}", id, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.failure(e.getMessage()));

        } catch (Exception e) {
            log.error("알림 삭제 오류 - 알림 ID: {}", id, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.internalError("알림 삭제 중 오류가 발생했습니다."));
        }
    }

    // ====== 내부 헬퍼 메서드 ======

    /**
     * Notification 엔티티를 DTO Map으로 변환
     *
     * 엔티티의 불필요한 정보는 제외하고 필요한 정보만 반환합니다.
     * 특히 User 엔티티의 순환 참조 문제를 방지합니다.
     *
     * @param notification 변환할 알림 엔티티
     * @return DTO Map
     */
    private Map<String, Object> convertToDto(Notification notification) {
        Map<String, Object> dto = new HashMap<>();

        // 기본 정보
        dto.put("id", notification.getId());
        dto.put("type", notification.getType());
        dto.put("typeDisplayName", notification.getTypeDisplayName());
        dto.put("title", notification.getTitle());
        dto.put("message", notification.getMessage());
        dto.put("link", notification.getLink());

        // 읽음 상태
        dto.put("isRead", notification.getIsRead());
        dto.put("readAt", notification.getReadAt());

        // 관련 엔티티 ID
        dto.put("relatedBoardId", notification.getRelatedBoardId());
        dto.put("relatedCommentId", notification.getRelatedCommentId());

        // 발신자 정보
        dto.put("senderName", notification.getSenderName());

        // 날짜 정보
        dto.put("createdAt", notification.getCreatedAt());

        // 아이콘 정보 (프론트엔드에서 사용)
        dto.put("iconClass", notification.getIconClass());

        return dto;
    }
}

/*
 * ====== API 응답 형식 ======
 *
 * 모든 API는 ApiResponse 래퍼를 사용합니다:
 *
 * 성공 응답:
 * {
 *   "success": true,
 *   "message": "성공 메시지",
 *   "data": { ... },
 *   "timestamp": "2025-11-26T..."
 * }
 *
 * 실패 응답:
 * {
 *   "success": false,
 *   "message": "에러 메시지",
 *   "errorCode": "GENERAL_ERROR",
 *   "data": null,
 *   "timestamp": "2025-11-26T..."
 * }
 *
 * ====== 테스트 방법 (Postman) ======
 *
 * 1. 알림 목록 조회
 *    GET http://localhost:8080/api/notifications?page=0&size=10
 *    Headers: Authorization: Bearer {accessToken}
 *
 * 2. 읽지 않은 알림 개수
 *    GET http://localhost:8080/api/notifications/unread-count
 *    Headers: Authorization: Bearer {accessToken}
 *
 * 3. 최근 알림 조회
 *    GET http://localhost:8080/api/notifications/recent?limit=5
 *    Headers: Authorization: Bearer {accessToken}
 *
 * 4. 개별 읽음 처리
 *    PUT http://localhost:8080/api/notifications/1/read
 *    Headers: Authorization: Bearer {accessToken}
 *
 * 5. 전체 읽음 처리
 *    PUT http://localhost:8080/api/notifications/read-all
 *    Headers: Authorization: Bearer {accessToken}
 *
 * 6. 알림 삭제
 *    DELETE http://localhost:8080/api/notifications/1
 *    Headers: Authorization: Bearer {accessToken}
 */