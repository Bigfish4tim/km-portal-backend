package com.kmportal.backend.controller;

import com.kmportal.backend.dto.common.ApiResponse;
import com.kmportal.backend.entity.Board;
import com.kmportal.backend.service.BoardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BoardController
 *
 * 게시판 시스템의 REST API 컨트롤러입니다.
 * 클라이언트(Vue 프론트엔드)의 HTTP 요청을 받아서 처리하고 응답을 반환합니다.
 *
 * REST API란?
 * - Representational State Transfer의 약자
 * - HTTP 프로토콜을 사용하여 자원(Resource)을 관리하는 아키텍처
 * - URL과 HTTP 메서드(GET, POST, PUT, DELETE)로 작업을 명시
 *
 * HTTP 메서드:
 * - GET: 데이터 조회 (Read)
 * - POST: 데이터 생성 (Create)
 * - PUT: 데이터 수정 (Update)
 * - DELETE: 데이터 삭제 (Delete)
 *
 * 이 컨트롤러의 주요 역할:
 * 1. HTTP 요청 수신 및 파라미터 파싱
 * 2. 입력값 검증 (@Valid)
 * 3. Service 레이어 호출
 * 4. 결과를 ApiResponse 형태로 변환하여 반환
 * 5. 예외 처리 및 적절한 HTTP 상태 코드 반환
 * 6. 권한 확인 (@PreAuthorize)
 *
 * 제공하는 API (8개):
 * 1. POST   /api/boards              - 게시글 작성
 * 2. PUT    /api/boards/{id}         - 게시글 수정
 * 3. DELETE /api/boards/{id}         - 게시글 삭제
 * 4. GET    /api/boards/{id}         - 게시글 상세 조회
 * 5. GET    /api/boards              - 게시글 목록 조회
 * 6. GET    /api/boards/search       - 게시글 검색
 * 7. PUT    /api/boards/{id}/pin     - 게시글 상단 고정
 * 8. GET    /api/boards/statistics   - 통계 정보
 *
 * 작성일: 2025년 11월 16일 (24일차)
 * 작성자: 24일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-16
 */
@RestController  // @Controller + @ResponseBody: REST API를 위한 컨트롤러
@RequestMapping("/api/boards")  // 기본 URL 경로: /api/boards
@RequiredArgsConstructor  // Lombok: final 필드에 대한 생성자 자동 생성
@Slf4j  // Lombok: 로그 객체 자동 생성
@CrossOrigin(origins = "*")  // CORS 허용 (모든 출처에서 접근 가능)
public class BoardController {

    /**
     * BoardService 의존성 주입
     *
     * 실제 비즈니스 로직은 Service에서 처리하고,
     * Controller는 HTTP 요청/응답 처리만 담당합니다.
     */
    private final BoardService boardService;

    // ================================
    // 1. 게시글 작성 API
    // ================================

    /**
     * 게시글 작성 API
     *
     * POST /api/boards
     *
     * 새로운 게시글을 작성합니다.
     * 로그인한 사용자만 작성할 수 있습니다.
     *
     * @PreAuthorize: Spring Security 메서드 레벨 보안
     * - "hasRole('USER')": ROLE_USER 권한을 가진 사용자만 접근 가능
     * - "isAuthenticated()": 로그인한 모든 사용자 접근 가능
     *
     * @RequestBody: HTTP 요청의 Body 데이터를 Java 객체로 변환
     * - JSON 형태로 전송된 데이터를 받음
     * - @Valid: 입력값 검증 수행
     *
     * @param request 게시글 작성 요청 데이터 (DTO)
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * POST /api/boards
     * Content-Type: application/json
     * {
     *   "title": "게시글 제목",
     *   "content": "게시글 내용",
     *   "category": "FREE"
     * }
     *
     * 응답 예시:
     * HTTP/1.1 201 Created
     * {
     *   "success": true,
     *   "message": "게시글이 성공적으로 작성되었습니다.",
     *   "data": {
     *     "id": 1,
     *     "title": "게시글 제목",
     *     "content": "게시글 내용",
     *     ...
     *   }
     * }
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")  // 로그인한 사용자만 접근 가능
    public ResponseEntity<ApiResponse<Board>> createBoard(
            @RequestBody @Valid BoardCreateRequest request) {

        log.info("게시글 작성 API 호출 - 제목: {}", request.getTitle());

        try {
            // Service 레이어 호출하여 게시글 생성
            Board board = boardService.createBoard(
                    request.getTitle(),
                    request.getContent(),
                    request.getCategory()
            );

            log.info("게시글 작성 완료 - ID: {}", board.getId());

            // 성공 응답 반환
            return ResponseEntity
                    .status(HttpStatus.CREATED)  // 201 Created
                    .body(ApiResponse.success(
                            board,
                            "게시글이 성공적으로 작성되었습니다."
                    ));

        } catch (IllegalArgumentException e) {
            // 입력값 검증 실패
            log.error("게시글 작성 실패 - 입력값 오류: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()  // 400 Bad Request
                    .body(ApiResponse.failure(e.getMessage()));

        } catch (Exception e) {
            // 기타 예외
            log.error("게시글 작성 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)  // 500 Internal Server Error
                    .body(ApiResponse.failure("게시글 작성 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 2. 게시글 수정 API
    // ================================

    /**
     * 게시글 수정 API
     *
     * PUT /api/boards/{id}
     *
     * 기존 게시글의 제목, 내용, 카테고리를 수정합니다.
     * 본인이 작성한 게시글만 수정할 수 있습니다.
     *
     * @PathVariable: URL 경로에 포함된 변수를 파라미터로 받음
     * - /api/boards/1 → id = 1
     * - /api/boards/100 → id = 100
     *
     * @param id 게시글 ID
     * @param request 수정할 데이터
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * PUT /api/boards/1
     * {
     *   "title": "수정된 제목",
     *   "content": "수정된 내용",
     *   "category": "TECH"
     * }
     */
    @PutMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Board>> updateBoard(
            @PathVariable Long id,
            @RequestBody @Valid BoardUpdateRequest request) {

        log.info("게시글 수정 API 호출 - ID: {}", id);

        try {
            Board board = boardService.updateBoard(
                    id,
                    request.getTitle(),
                    request.getContent(),
                    request.getCategory()
            );

            log.info("게시글 수정 완료 - ID: {}", id);

            return ResponseEntity.ok(ApiResponse.success(
                    board,
                    "게시글이 성공적으로 수정되었습니다."
            ));

        } catch (RuntimeException e) {
            log.error("게시글 수정 실패 - ID: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)  // 403 Forbidden
                    .body(ApiResponse.failure(e.getMessage()));

        } catch (Exception e) {
            log.error("게시글 수정 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("게시글 수정 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 3. 게시글 삭제 API
    // ================================

    /**
     * 게시글 삭제 API
     *
     * DELETE /api/boards/{id}
     *
     * 게시글을 논리적으로 삭제합니다 (Soft Delete).
     * 본인이 작성한 게시글만 삭제할 수 있습니다.
     *
     * @param id 게시글 ID
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * DELETE /api/boards/1
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteBoard(@PathVariable Long id) {
        log.info("게시글 삭제 API 호출 - ID: {}", id);

        try {
            boardService.deleteBoard(id);
            log.info("게시글 삭제 완료 - ID: {}", id);

            return ResponseEntity.ok(ApiResponse.success(
                    "게시글이 성공적으로 삭제되었습니다."
            ));

        } catch (RuntimeException e) {
            log.error("게시글 삭제 실패 - ID: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));

        } catch (Exception e) {
            log.error("게시글 삭제 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("게시글 삭제 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 4. 게시글 상세 조회 API
    // ================================

    /**
     * 게시글 상세 조회 API
     *
     * GET /api/boards/{id}
     *
     * 게시글의 상세 정보를 조회하고 조회수를 1 증가시킵니다.
     * 모든 사용자가 조회할 수 있습니다.
     *
     * @param id 게시글 ID
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * GET /api/boards/1
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "게시글 조회 성공",
     *   "data": {
     *     "id": 1,
     *     "title": "게시글 제목",
     *     "content": "게시글 내용",
     *     "viewCount": 10,
     *     "author": {...},
     *     ...
     *   }
     * }
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Board>> getBoardById(@PathVariable Long id) {
        log.info("게시글 조회 API 호출 - ID: {}", id);

        try {
            Board board = boardService.getBoardById(id);
            log.info("게시글 조회 완료 - ID: {}, 제목: {}", id, board.getTitle());

            return ResponseEntity.ok(ApiResponse.success(
                    board,
                    "게시글 조회 성공"
            ));

        } catch (RuntimeException e) {
            log.error("게시글 조회 실패 - ID: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)  // 404 Not Found
                    .body(ApiResponse.failure(e.getMessage()));

        } catch (Exception e) {
            log.error("게시글 조회 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("게시글 조회 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 5. 게시글 목록 조회 API
    // ================================

    /**
     * 게시글 목록 조회 API
     *
     * GET /api/boards
     *
     * 게시글 목록을 페이징하여 조회합니다.
     *
     * @RequestParam: URL 쿼리 파라미터를 받음
     * - required = false: 선택적 파라미터 (없어도 됨)
     * - defaultValue: 파라미터가 없을 때 기본값
     *
     * @param page 페이지 번호 (0부터 시작, 기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param sort 정렬 기준 (기본값: createdAt)
     * @param direction 정렬 방향 (기본값: DESC - 내림차순)
     * @param category 카테고리 필터 (선택)
     * @param authorId 작성자 ID 필터 (선택)
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * GET /api/boards?page=0&size=10&sort=createdAt&direction=DESC
     * GET /api/boards?category=NOTICE&page=0&size=20
     * GET /api/boards?authorId=1
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "게시글 목록 조회 성공",
     *   "data": {
     *     "content": [...],      // 게시글 목록
     *     "totalElements": 100,  // 전체 게시글 수
     *     "totalPages": 10,      // 전체 페이지 수
     *     "number": 0,           // 현재 페이지 번호
     *     "size": 10,            // 페이지 크기
     *     ...
     *   }
     * }
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Board>>> getBoardList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sort,
            @RequestParam(defaultValue = "DESC") String direction,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long authorId) {

        log.info("게시글 목록 조회 API 호출 - page: {}, size: {}, category: {}",
                page, size, category);

        try {
            // 정렬 설정
            Sort.Direction sortDirection = direction.equalsIgnoreCase("ASC")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

            // 카테고리 필터가 있으면 카테고리별 조회
            Page<Board> boards;
            if (category != null && !category.isEmpty()) {
                boards = boardService.getBoardsByCategory(category, pageable);
            } else if (authorId != null) {
                boards = boardService.getBoardsByAuthor(authorId, pageable);
            } else {
                boards = boardService.getBoardList(pageable);
            }

            log.info("게시글 목록 조회 완료 - 총 {}개", boards.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success(
                    boards,
                    "게시글 목록 조회 성공"
            ));

        } catch (Exception e) {
            log.error("게시글 목록 조회 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("게시글 목록 조회 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 6. 게시글 검색 API
    // ================================

    /**
     * 게시글 검색 API
     *
     * GET /api/boards/search
     *
     * 제목, 내용, 작성자 이름으로 게시글을 검색합니다.
     *
     * @param keyword 검색어 (제목, 내용, 작성자 이름)
     * @param category 카테고리 필터 (선택)
     * @param authorId 작성자 ID 필터 (선택)
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * GET /api/boards/search?keyword=spring
     * GET /api/boards/search?keyword=공지&category=NOTICE
     * GET /api/boards/search?keyword=java&authorId=1
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Board>>> searchBoards(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long authorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        log.info("게시글 검색 API 호출 - 검색어: {}, 카테고리: {}", keyword, category);

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<Board> results = boardService.searchBoards(keyword, category, authorId, pageable);

            log.info("게시글 검색 완료 - 검색어: {}, 결과: {}개", keyword, results.getTotalElements());

            return ResponseEntity.ok(ApiResponse.success(
                    results,
                    "게시글 검색 성공"
            ));

        } catch (Exception e) {
            log.error("게시글 검색 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("게시글 검색 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 7. 게시글 상단 고정/해제 API
    // ================================

    /**
     * 게시글 상단 고정 API
     *
     * PUT /api/boards/{id}/pin
     *
     * 게시글을 상단에 고정하거나 고정을 해제합니다.
     * 관리자만 사용할 수 있습니다.
     *
     * @PreAuthorize("hasRole('ADMIN')"): ROLE_ADMIN 권한만 접근 가능
     *
     * @param id 게시글 ID
     * @param pin true: 고정, false: 고정 해제
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * PUT /api/boards/1/pin?pin=true   // 상단 고정
     * PUT /api/boards/1/pin?pin=false  // 고정 해제
     */
    @PutMapping("/{id}/pin")
    @PreAuthorize("hasRole('ADMIN')")  // 관리자만 접근 가능
    public ResponseEntity<ApiResponse<Void>> pinBoard(
            @PathVariable Long id,
            @RequestParam boolean pin) {

        log.info("게시글 상단 고정 API 호출 - ID: {}, pin: {}", id, pin);

        try {
            if (pin) {
                boardService.pinBoard(id);
                log.info("게시글 상단 고정 완료 - ID: {}", id);
                return ResponseEntity.ok(ApiResponse.success(
                        "게시글이 상단에 고정되었습니다."
                ));
            } else {
                boardService.unpinBoard(id);
                log.info("게시글 상단 고정 해제 완료 - ID: {}", id);
                return ResponseEntity.ok(ApiResponse.success(
                        "게시글 상단 고정이 해제되었습니다."
                ));
            }

        } catch (RuntimeException e) {
            log.error("게시글 상단 고정 실패 - ID: {}, 오류: {}", id, e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure(e.getMessage()));

        } catch (Exception e) {
            log.error("게시글 상단 고정 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("게시글 상단 고정 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 8. 게시판 통계 정보 API
    // ================================

    /**
     * 게시판 통계 정보 API
     *
     * GET /api/boards/statistics
     *
     * 게시판 전체 통계 정보를 조회합니다.
     * 관리자 대시보드에서 사용합니다.
     *
     * @return ResponseEntity<ApiResponse> 응답 객체
     *
     * 요청 예시:
     * GET /api/boards/statistics
     *
     * 응답 예시:
     * {
     *   "success": true,
     *   "message": "통계 조회 성공",
     *   "data": {
     *     "totalBoards": 100,
     *     "categoryStats": {
     *       "NOTICE": 10,
     *       "FREE": 50,
     *       "QNA": 40
     *     },
     *     "todayBoards": 5,
     *     "weekBoards": 30
     *   }
     * }
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")  // 관리자 또는 매니저만 접근
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBoardStatistics() {
        log.info("게시판 통계 정보 API 호출");

        try {
            Map<String, Object> statistics = boardService.getBoardStatistics();
            log.info("게시판 통계 정보 조회 완료");

            return ResponseEntity.ok(ApiResponse.success(
                    statistics,
                    "통계 조회 성공"
            ));

        } catch (Exception e) {
            log.error("통계 조회 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("통계 조회 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 추가 API: 상단 고정 게시글 조회
    // ================================

    /**
     * 상단 고정 게시글 조회 API
     *
     * GET /api/boards/pinned
     *
     * 상단에 고정된 게시글 목록을 조회합니다.
     * 일반적으로 공지사항 등이 여기에 해당합니다.
     *
     * @return ResponseEntity<ApiResponse> 응답 객체
     */
    @GetMapping("/pinned")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<Board>>> getPinnedBoards() {
        log.info("상단 고정 게시글 조회 API 호출");

        try {
            List<Board> pinnedBoards = boardService.getPinnedBoards();
            log.info("상단 고정 게시글 조회 완료 - {}개", pinnedBoards.size());

            return ResponseEntity.ok(ApiResponse.success(
                    pinnedBoards,
                    "상단 고정 게시글 조회 성공"
            ));

        } catch (Exception e) {
            log.error("상단 고정 게시글 조회 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("상단 고정 게시글 조회 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 추가 API: 인기 게시글 조회
    // ================================

    /**
     * 인기 게시글 조회 API
     *
     * GET /api/boards/popular
     *
     * 조회수가 높은 게시글을 조회합니다.
     * 메인 페이지나 사이드바에 "인기 게시글" 표시용입니다.
     *
     * @param limit 조회할 게시글 수 (기본값: 10)
     * @return ResponseEntity<ApiResponse> 응답 객체
     */
    @GetMapping("/popular")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Page<Board>>> getPopularBoards(
            @RequestParam(defaultValue = "10") int limit) {

        log.info("인기 게시글 조회 API 호출 - limit: {}", limit);

        try {
            Pageable pageable = PageRequest.of(0, limit);
            Page<Board> popularBoards = boardService.getPopularBoards(pageable);

            log.info("인기 게시글 조회 완료 - {}개", popularBoards.getNumberOfElements());

            return ResponseEntity.ok(ApiResponse.success(
                    popularBoards,
                    "인기 게시글 조회 성공"
            ));

        } catch (Exception e) {
            log.error("인기 게시글 조회 실패 - 서버 오류", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("인기 게시글 조회 중 오류가 발생했습니다."));
        }
    }

    // ================================
    // 내부 DTO 클래스들
    // ================================

    /**
     * 게시글 작성 요청 DTO
     *
     * 클라이언트에서 게시글 작성 시 보내는 데이터 형식입니다.
     */
    @lombok.Data
    public static class BoardCreateRequest {

        @jakarta.validation.constraints.NotBlank(message = "제목은 필수 입력 항목입니다.")
        @jakarta.validation.constraints.Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하로 입력해주세요.")
        private String title;

        @jakarta.validation.constraints.NotBlank(message = "내용은 필수 입력 항목입니다.")
        private String content;

        private String category;
    }

    /**
     * 게시글 수정 요청 DTO
     *
     * 클라이언트에서 게시글 수정 시 보내는 데이터 형식입니다.
     */
    @lombok.Data
    public static class BoardUpdateRequest {

        @jakarta.validation.constraints.Size(min = 1, max = 200, message = "제목은 1자 이상 200자 이하로 입력해주세요.")
        private String title;

        private String content;

        private String category;
    }
}

/*
 * ====== REST API 설계 원칙 ======
 *
 * 1. URL 설계:
 *    - 명사 사용 (동사 X)
 *    - 복수형 사용: /api/boards (O), /api/board (X)
 *    - 계층 구조 표현: /api/boards/{id}/comments
 *
 * 2. HTTP 메서드:
 *    - GET: 조회 (멱등성 O, 부작용 X)
 *    - POST: 생성 (멱등성 X, 부작용 O)
 *    - PUT: 전체 수정 (멱등성 O, 부작용 O)
 *    - PATCH: 부분 수정 (멱등성 △, 부작용 O)
 *    - DELETE: 삭제 (멱등성 O, 부작용 O)
 *
 * 3. HTTP 상태 코드:
 *    - 200 OK: 성공
 *    - 201 Created: 생성 성공
 *    - 204 No Content: 성공 (응답 본문 없음)
 *    - 400 Bad Request: 잘못된 요청
 *    - 401 Unauthorized: 인증 필요
 *    - 403 Forbidden: 권한 없음
 *    - 404 Not Found: 리소스 없음
 *    - 500 Internal Server Error: 서버 오류
 *
 * 4. 응답 형식:
 *    - 일관된 형식 사용 (ApiResponse)
 *    - success: 성공 여부
 *    - message: 메시지
 *    - data: 실제 데이터
 *
 * 5. 보안:
 *    - @PreAuthorize로 권한 확인
 *    - 민감한 정보는 DTO로 필터링
 *    - SQL Injection 방지 (JPA 사용)
 *    - XSS 방지 (HTML 이스케이프)
 */