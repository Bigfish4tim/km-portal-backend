package com.kmportal.backend.controller;

import com.kmportal.backend.dto.common.ApiResponse;
import com.kmportal.backend.entity.Comment;
import com.kmportal.backend.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * CommentController
 *
 * 댓글 시스템의 REST API 컨트롤러입니다.
 * 클라이언트(Vue 프론트엔드)의 HTTP 요청을 받아서 처리하고 응답을 반환합니다.
 *
 * REST API 설계:
 * - 댓글은 게시글의 하위 리소스이므로 /api/boards/{boardId}/comments 구조 사용
 * - 대댓글은 댓글의 하위 리소스이므로 /api/boards/{boardId}/comments/{commentId}/replies 구조 사용
 *
 * 제공하는 API (7개):
 * 1. POST   /api/boards/{boardId}/comments              - 댓글 작성
 * 2. POST   /api/boards/{boardId}/comments/{id}/replies - 대댓글 작성
 * 3. GET    /api/boards/{boardId}/comments              - 댓글 목록 조회
 * 4. GET    /api/boards/{boardId}/comments/{id}/replies - 대댓글 목록 조회
 * 5. PUT    /api/boards/{boardId}/comments/{id}         - 댓글 수정
 * 6. DELETE /api/boards/{boardId}/comments/{id}         - 댓글 삭제
 * 7. GET    /api/boards/{boardId}/comments/count        - 댓글 수 조회
 *
 * 작성일: 2025년 11월 21일 (30일차)
 * 작성자: 30일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-21
 */
@RestController  // @Controller + @ResponseBody: REST API를 위한 컨트롤러
@RequestMapping("/api/boards/{boardId}/comments")  // 기본 URL 경로: /api/boards/{boardId}/comments
@RequiredArgsConstructor  // Lombok: final 필드에 대한 생성자 자동 생성
@Slf4j  // Lombok: 로그 객체 자동 생성
@CrossOrigin(origins = "*")  // CORS 허용 (모든 출처에서 접근 가능)
public class CommentController {

    /**
     * CommentService 의존성 주입
     *
     * 실제 비즈니스 로직은 Service에서 처리하고,
     * Controller는 HTTP 요청/응답 처리만 담당합니다.
     */
    private final CommentService commentService;

    // ================================
    // 1. 댓글 작성 API
    // ================================

    /**
     * 댓글 작성 API
     *
     * POST /api/boards/{boardId}/comments
     *
     * 게시글에 새로운 댓글을 작성합니다.
     * 로그인한 사용자만 작성할 수 있습니다.
     *
     * @PathVariable boardId: URL 경로에서 게시글 ID 추출
     * @RequestBody request: JSON 형태의 요청 본문 (content 필드 포함)
     *
     * @param boardId 게시글 ID
     * @param request 댓글 작성 요청 데이터 {"content": "댓글 내용"}
     * @return ResponseEntity<ApiResponse<Comment>> 응답 객체
     *
     * 요청 예시:
     * POST /api/boards/1/comments
     * Content-Type: application/json
     * {
     *   "content": "좋은 글이네요!"
     * }
     *
     * 응답 예시:
     * HTTP/1.1 201 Created
     * {
     *   "success": true,
     *   "message": "댓글이 성공적으로 작성되었습니다.",
     *   "data": {
     *     "id": 1,
     *     "content": "좋은 글이네요!",
     *     "authorName": "홍길동",
     *     "createdAt": "2025-11-21T10:30:00",
     *     ...
     *   }
     * }
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")  // 로그인한 사용자만 접근 가능
    public ResponseEntity<ApiResponse<Map<String, Object>>> createComment(
            @PathVariable Long boardId,
            @RequestBody CommentRequest request) {

        log.info("댓글 작성 API 호출 - 게시글 ID: {}", boardId);

        try {
            // Service 레이어 호출하여 댓글 생성
            Comment comment = commentService.createComment(boardId, request.getContent());

            // 응답 데이터 구성 (순환 참조 방지를 위해 필요한 필드만 추출)
            Map<String, Object> responseData = convertToResponseMap(comment);

            log.info("댓글 작성 완료 - ID: {}", comment.getId());

            // 성공 응답 반환
            return ResponseEntity
                    .status(HttpStatus.CREATED)  // 201 Created
                    .body(ApiResponse.success(
                            responseData,
                            "댓글이 성공적으로 작성되었습니다."
                    ));

        } catch (Exception e) {
            log.error("댓글 작성 실패 - 게시글 ID: {}, 오류: {}", boardId, e.getMessage());
            return ResponseEntity
                    .badRequest()  // 400 Bad Request
                    .body(ApiResponse.failure(e.getMessage()));
        }
    }

    // ================================
    // 2. 대댓글 작성 API
    // ================================

    /**
     * 대댓글 작성 API
     *
     * POST /api/boards/{boardId}/comments/{commentId}/replies
     *
     * 특정 댓글에 대한 답글(대댓글)을 작성합니다.
     *
     * @param boardId 게시글 ID (URL 경로 일관성을 위해 포함)
     * @param commentId 부모 댓글 ID
     * @param request 대댓글 작성 요청 데이터
     * @return ResponseEntity<ApiResponse<Map>> 응답 객체
     *
     * 요청 예시:
     * POST /api/boards/1/comments/5/replies
     * {
     *   "content": "저도 그렇게 생각해요!"
     * }
     */
    @PostMapping("/{commentId}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createReply(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {

        log.info("대댓글 작성 API 호출 - 게시글 ID: {}, 부모 댓글 ID: {}", boardId, commentId);

        try {
            // Service 레이어 호출하여 대댓글 생성
            Comment reply = commentService.createReply(commentId, request.getContent());

            // 응답 데이터 구성
            Map<String, Object> responseData = convertToResponseMap(reply);

            log.info("대댓글 작성 완료 - ID: {}", reply.getId());

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(
                            responseData,
                            "답글이 성공적으로 작성되었습니다."
                    ));

        } catch (Exception e) {
            log.error("대댓글 작성 실패 - 부모 댓글 ID: {}, 오류: {}", commentId, e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ApiResponse.failure(e.getMessage()));
        }
    }

    // ================================
    // 3. 댓글 목록 조회 API
    // ================================

    /**
     * 댓글 목록 조회 API
     *
     * GET /api/boards/{boardId}/comments
     *
     * 특정 게시글의 댓글 목록을 조회합니다.
     * 페이징을 지원하며, 최상위 댓글만 조회합니다.
     * 대댓글은 별도 API로 조회합니다.
     *
     * @RequestParam: URL 쿼리 파라미터 (예: ?page=0&size=20)
     *
     * @param boardId 게시글 ID
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * GET /api/boards/1/comments?page=0&size=20
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "댓글 목록 조회 성공",
     *   "data": {
     *     "content": [...],
     *     "page": 0,
     *     "size": 20,
     *     "totalElements": 50,
     *     "totalPages": 3
     *   }
     * }
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getComments(
            @PathVariable Long boardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        log.info("댓글 목록 조회 API 호출 - 게시글 ID: {}, 페이지: {}, 크기: {}", boardId, page, size);

        try {
            // 페이징 조회
            Page<Comment> commentsPage = commentService.getCommentsByBoard(boardId, page, size);

            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("content", commentsPage.getContent().stream()
                    .map(this::convertToResponseMap)
                    .toList());
            responseData.put("page", commentsPage.getNumber());
            responseData.put("size", commentsPage.getSize());
            responseData.put("totalElements", commentsPage.getTotalElements());
            responseData.put("totalPages", commentsPage.getTotalPages());
            responseData.put("hasNext", commentsPage.hasNext());
            responseData.put("hasPrevious", commentsPage.hasPrevious());

            log.info("댓글 목록 조회 완료 - 게시글 ID: {}, 댓글 수: {}",
                    boardId, commentsPage.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success(
                    responseData,
                    "댓글 목록 조회 성공"
            ));

        } catch (Exception e) {
            log.error("댓글 목록 조회 실패 - 게시글 ID: {}, 오류: {}", boardId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("댓글 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 4. 대댓글 목록 조회 API
    // ================================

    /**
     * 대댓글 목록 조회 API
     *
     * GET /api/boards/{boardId}/comments/{commentId}/replies
     *
     * 특정 댓글의 대댓글 목록을 조회합니다.
     *
     * @param boardId 게시글 ID
     * @param commentId 부모 댓글 ID
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * GET /api/boards/1/comments/5/replies
     */
    @GetMapping("/{commentId}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getReplies(
            @PathVariable Long boardId,
            @PathVariable Long commentId) {

        log.info("대댓글 목록 조회 API 호출 - 게시글 ID: {}, 부모 댓글 ID: {}", boardId, commentId);

        try {
            List<Comment> replies = commentService.getReplies(commentId);

            // 응답 데이터 변환
            List<Map<String, Object>> responseData = replies.stream()
                    .map(this::convertToResponseMap)
                    .toList();

            log.info("대댓글 목록 조회 완료 - 부모 댓글 ID: {}, 대댓글 수: {}",
                    commentId, replies.size());

            return ResponseEntity.ok(ApiResponse.success(
                    responseData,
                    "대댓글 목록 조회 성공"
            ));

        } catch (Exception e) {
            log.error("대댓글 목록 조회 실패 - 부모 댓글 ID: {}, 오류: {}", commentId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("대댓글 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 5. 댓글 수정 API
    // ================================

    /**
     * 댓글 수정 API
     *
     * PUT /api/boards/{boardId}/comments/{commentId}
     *
     * 기존 댓글의 내용을 수정합니다.
     * 본인이 작성한 댓글만 수정할 수 있습니다.
     *
     * @param boardId 게시글 ID
     * @param commentId 댓글 ID
     * @param request 수정할 데이터
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * PUT /api/boards/1/comments/5
     * {
     *   "content": "수정된 댓글 내용입니다."
     * }
     */
    @PutMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId,
            @RequestBody CommentRequest request) {

        log.info("댓글 수정 API 호출 - 게시글 ID: {}, 댓글 ID: {}", boardId, commentId);

        try {
            Comment updatedComment = commentService.updateComment(commentId, request.getContent());

            Map<String, Object> responseData = convertToResponseMap(updatedComment);

            log.info("댓글 수정 완료 - ID: {}", commentId);

            return ResponseEntity.ok(ApiResponse.success(
                    responseData,
                    "댓글이 성공적으로 수정되었습니다."
            ));

        } catch (Exception e) {
            log.error("댓글 수정 실패 - 댓글 ID: {}, 오류: {}", commentId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)  // 403 Forbidden (권한 없음)
                    .body(ApiResponse.failure(e.getMessage()));
        }
    }

    // ================================
    // 6. 댓글 삭제 API
    // ================================

    /**
     * 댓글 삭제 API
     *
     * DELETE /api/boards/{boardId}/comments/{commentId}
     *
     * 댓글을 논리적으로 삭제합니다 (Soft Delete).
     * 본인이 작성한 댓글 또는 관리자만 삭제할 수 있습니다.
     *
     * @param boardId 게시글 ID
     * @param commentId 댓글 ID
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * DELETE /api/boards/1/comments/5
     */
    @DeleteMapping("/{commentId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long boardId,
            @PathVariable Long commentId) {

        log.info("댓글 삭제 API 호출 - 게시글 ID: {}, 댓글 ID: {}", boardId, commentId);

        try {
            commentService.deleteComment(commentId);

            log.info("댓글 삭제 완료 - ID: {}", commentId);

            return ResponseEntity.ok(ApiResponse.success(
                    "댓글이 성공적으로 삭제되었습니다."
            ));

        } catch (Exception e) {
            log.error("댓글 삭제 실패 - 댓글 ID: {}, 오류: {}", commentId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));
        }
    }

    // ================================
    // 7. 댓글 수 조회 API
    // ================================

    /**
     * 댓글 수 조회 API
     *
     * GET /api/boards/{boardId}/comments/count
     *
     * 특정 게시글의 댓글 수를 조회합니다.
     * 게시글 목록에서 각 게시글의 댓글 수를 표시할 때 사용합니다.
     *
     * @param boardId 게시글 ID
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "data": {
     *     "count": 15
     *   }
     * }
     */
    @GetMapping("/count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getCommentCount(
            @PathVariable Long boardId) {

        log.info("댓글 수 조회 API 호출 - 게시글 ID: {}", boardId);

        try {
            Long count = commentService.getCommentCount(boardId);

            Map<String, Long> responseData = new HashMap<>();
            responseData.put("count", count);

            log.info("댓글 수 조회 완료 - 게시글 ID: {}, 댓글 수: {}", boardId, count);

            return ResponseEntity.ok(ApiResponse.success(
                    responseData,
                    "댓글 수 조회 성공"
            ));

        } catch (Exception e) {
            log.error("댓글 수 조회 실패 - 게시글 ID: {}, 오류: {}", boardId, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("댓글 수 조회 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * Comment 엔티티를 응답용 Map으로 변환
     *
     * 순환 참조 방지와 필요한 필드만 반환하기 위해 사용합니다.
     * JPA 엔티티를 직접 반환하면 순환 참조 문제가 발생할 수 있습니다.
     *
     * @param comment Comment 엔티티
     * @return 응답용 Map
     */
    private Map<String, Object> convertToResponseMap(Comment comment) {
        Map<String, Object> map = new HashMap<>();

        map.put("id", comment.getId());
        map.put("content", comment.isDeleted() ? "삭제된 댓글입니다." : comment.getContent());
        map.put("isDeleted", comment.isDeleted());

        // 작성자 정보
        map.put("authorId", comment.getAuthorId());
        map.put("authorName", comment.getAuthorName());

        // 게시글 정보
        map.put("boardId", comment.getBoardId());

        // 부모 댓글 정보 (대댓글인 경우)
        map.put("parentId", comment.getParentId());
        map.put("isReply", comment.isReply());

        // 시간 정보
        map.put("createdAt", comment.getCreatedAt());
        map.put("updatedAt", comment.getUpdatedAt());

        // 대댓글 수 (최상위 댓글인 경우)
        if (!comment.isReply()) {
            map.put("replyCount", comment.getReplyCount());
        }

        return map;
    }

    // ================================
    // 내부 DTO 클래스
    // ================================

    /**
     * 댓글 작성/수정 요청 DTO
     *
     * 클라이언트에서 댓글 작성/수정 시 보내는 데이터 형식입니다.
     */
    @lombok.Data
    public static class CommentRequest {
        /**
         * 댓글 내용
         * - 필수 입력
         * - 최대 1000자
         */
        private String content;
    }
}

/*
 * ====== REST API 설계 원칙 (댓글 API) ======
 *
 * 1. URL 설계:
 *    - 댓글은 게시글의 하위 리소스: /api/boards/{boardId}/comments
 *    - 대댓글은 댓글의 하위 리소스: /api/boards/{boardId}/comments/{commentId}/replies
 *
 * 2. HTTP 메서드:
 *    - POST: 댓글/대댓글 작성
 *    - GET: 댓글 목록 조회, 대댓글 조회
 *    - PUT: 댓글 수정
 *    - DELETE: 댓글 삭제 (Soft Delete)
 *
 * 3. HTTP 상태 코드:
 *    - 200 OK: 조회, 수정, 삭제 성공
 *    - 201 Created: 작성 성공
 *    - 400 Bad Request: 잘못된 요청 (유효성 검증 실패)
 *    - 403 Forbidden: 권한 없음
 *    - 500 Internal Server Error: 서버 오류
 *
 * 4. 응답 형식:
 *    - ApiResponse 사용하여 일관된 형식 유지
 *    - success, message, data 필드
 *
 * 5. 순환 참조 방지:
 *    - 엔티티 직접 반환 대신 Map으로 변환
 *    - 필요한 필드만 포함
 */

/*
 * ====== SecurityConfig에 추가해야 할 권한 설정 ======
 *
 * SecurityConfig.java에서 다음 권한 설정을 추가해야 합니다:
 *
 * // 댓글 API - 인증된 사용자만 접근 가능
 * .requestMatchers("/api/boards/ * /comments/**").authenticated()
 *
 * 또는 더 세분화된 설정:
 * .requestMatchers(HttpMethod.GET, "/api/boards/ * /comments/**").authenticated()
 * .requestMatchers(HttpMethod.POST, "/api/boards/ * /comments/**").authenticated()
 * .requestMatchers(HttpMethod.PUT, "/api/boards/ * /comments/**").authenticated()
 * .requestMatchers(HttpMethod.DELETE, "/api/boards/ * /comments/**").authenticated()
 */

/*
 * ====== Postman 테스트 시나리오 ======
 *
 * 1. 댓글 작성 테스트:
 *    POST http://localhost:8080/api/boards/1/comments
 *    Headers: Authorization: Bearer {token}
 *    Body: {"content": "테스트 댓글입니다."}
 *
 * 2. 댓글 목록 조회:
 *    GET http://localhost:8080/api/boards/1/comments?page=0&size=20
 *
 * 3. 대댓글 작성:
 *    POST http://localhost:8080/api/boards/1/comments/1/replies
 *    Body: {"content": "대댓글입니다."}
 *
 * 4. 댓글 수정:
 *    PUT http://localhost:8080/api/boards/1/comments/1
 *    Body: {"content": "수정된 댓글입니다."}
 *
 * 5. 댓글 삭제:
 *    DELETE http://localhost:8080/api/boards/1/comments/1
 *
 * 6. 댓글 수 조회:
 *    GET http://localhost:8080/api/boards/1/comments/count
 */