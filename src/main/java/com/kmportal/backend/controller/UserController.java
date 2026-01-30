package com.kmportal.backend.controller;

import com.kmportal.backend.entity.User;
import com.kmportal.backend.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * =============================================================================
 * 📁 사용자 관리 REST API 컨트롤러 (2일차 수정 버전 v2.3)
 * =============================================================================
 *
 * 【버전 히스토리】
 * - v2.0 (2일차): ROLE_MANAGER → ROLE_BUSINESS_SUPPORT 변경
 * - v2.1: 12개 Role 시스템 반영
 * - v2.2: UserService 메서드 호환성 수정
 *         - getActiveUsers(int, int) → getActiveUsers(Pageable)
 *         - searchUsers(String, int, int) → searchUsers(String)
 * - v2.3: changeUserRole 메서드 → updateUserRoles 사용으로 변경
 *
 * [Controller의 역할]
 *
 * 이 클래스는 HTTP 요청을 받아서 적절한 Service 메서드를 호출하고,
 * 그 결과를 HTTP 응답으로 변환하는 역할만 합니다.
 *
 * Controller는 다음만 처리합니다:
 * 1. HTTP 요청 매핑 (@GetMapping, @PostMapping 등)
 * 2. 권한 체크 (@PreAuthorize)
 * 3. 요청 파라미터 검증 (@Valid)
 * 4. Service 메서드 호출
 * 5. HTTP 응답 구성 (ResponseEntity)
 * 6. 예외 처리 및 에러 응답
 *
 * Controller가 하지 않는 것:
 * 1. 비즈니스 로직 (→ Service 계층)
 * 2. 데이터베이스 접근 (→ Repository 계층)
 * 3. 복잡한 데이터 처리 (→ Service 계층)
 * 4. 트랜잭션 관리 (→ Service 계층)
 *
 * [권한 체크 규칙 - 12개 Role 시스템]
 *
 * - ROLE_ADMIN: 모든 기능 접근 가능
 * - ROLE_BUSINESS_SUPPORT: 사용자 조회/수정/권한변경/잠금 가능 (기존 ROLE_MANAGER 대체)
 * - 기타 Role: 접근 불가 (403 Forbidden)
 *
 * @author KM Portal Dev Team
 * @version 2.3 (changeUserRole → updateUserRoles)
 * @since 2025-11-12
 * @modified 2026-01-30 - 컴파일 오류 해결
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    /**
     * 로깅을 위한 Logger 인스턴스
     *
     * Controller에서는 주로 다음을 로깅합니다:
     * - HTTP 요청 수신 (요청 파라미터)
     * - Service 호출 성공/실패
     * - HTTP 응답 반환 (상태 코드)
     */
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
     * 사용자 관리 비즈니스 로직을 담당하는 Service
     *
     * [의존성 주입 패턴]
     *
     * Controller는 Service에 의존합니다:
     * - Controller가 Service의 메서드를 호출
     * - Service는 비즈니스 로직 처리 후 결과 반환
     * - Controller는 결과를 HTTP 응답으로 변환
     *
     * 이를 통해:
     * - Controller는 HTTP에만 집중
     * - Service는 비즈니스 로직에만 집중
     * - 각자의 책임이 명확해짐
     */
    private final UserService userService;

    /**
     * 생성자 기반 의존성 주입
     *
     * [왜 생성자 주입인가?]
     *
     * 1. 불변성 (Immutability)
     *    - final 키워드 사용 가능
     *    - 객체 생성 후 의존성 변경 불가
     *
     * 2. 테스트 용이성
     *    - Mock 객체 주입 용이
     *    - @Autowired 없이 순수 Java 테스트 가능
     *
     * 3. 명시성
     *    - 필요한 의존성이 생성자에 명확히 드러남
     *    - 의존성이 많으면 리팩토링 신호
     *
     * 4. Spring의 권장 방식
     *    - Spring 4.3 이후 단일 생성자는 @Autowired 생략 가능
     *    - 순환 참조 방지
     *
     * @param userService 사용자 관리 서비스
     */
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
        logger.info("✅ UserController 초기화 완료");
        logger.debug("   - UserService: {}", userService.getClass().getSimpleName());
    }

    // ================================
    // 조회 API (Read Operations)
    // ================================

    /**
     * 모든 사용자 목록 조회 (페이징 지원)
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users
     * - Query Parameters:
     *   - page: 페이지 번호 (기본값: 0)
     *   - size: 페이지 크기 (기본값: 10)
     *   - sortBy: 정렬 필드 (기본값: username)
     *   - sortDir: 정렬 방향 (기본값: asc)
     * - Response: 페이징된 사용자 목록 + 메타정보
     * - Status: 200 OK
     *
     * [권한] 【2일차 수정】
     *
     * - ROLE_ADMIN: 모든 사용자 조회 가능
     * - ROLE_BUSINESS_SUPPORT: 모든 사용자 조회 가능 (기존 ROLE_MANAGER 대체)
     * - 기타: 접근 불가 (403 Forbidden)
     *
     * [사용 예시]
     *
     * ```
     * GET /api/users?page=0&size=10&sortBy=username&sortDir=asc
     * Authorization: Bearer {token}
     * ```
     *
     * [응답 예시]
     *
     * ```json
     * {
     *   "users": [...],
     *   "currentPage": 0,
     *   "totalPages": 5,
     *   "totalElements": 50,
     *   "pageSize": 10,
     *   "hasNext": true,
     *   "hasPrevious": false
     * }
     * ```
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @param sortBy 정렬 기준 필드
     * @param sortDir 정렬 방향 (asc 또는 desc)
     * @return 페이징된 사용자 목록과 메타정보
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")  // 【2일차 수정】 MANAGER → BUSINESS_SUPPORT
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        logger.info("📥 [GET /api/users] 사용자 목록 조회 요청");
        logger.debug("   - 페이지: {}, 크기: {}, 정렬: {} {}", page, size, sortBy, sortDir);

        try {
            // Service에 비즈니스 로직 위임
            Page<User> userPage = userService.getAllUsers(page, size, sortBy, sortDir);

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("users", userPage.getContent());
            response.put("currentPage", userPage.getNumber());
            response.put("totalPages", userPage.getTotalPages());
            response.put("totalElements", userPage.getTotalElements());
            response.put("pageSize", userPage.getSize());
            response.put("hasNext", userPage.hasNext());
            response.put("hasPrevious", userPage.hasPrevious());

            logger.info("📤 [200 OK] 사용자 목록 조회 성공");
            logger.debug("   - 조회된 사용자 수: {}", userPage.getContent().size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 사용자 목록 조회 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 목록을 조회할 수 없습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 활성 사용자만 조회
     *
     * 【v2.2 수정】 UserService.getActiveUsers(Pageable) 메서드 호출 방식 변경
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/active
     * - Response: 활성 사용자 목록 (페이징 지원)
     * - Status: 200 OK
     *
     * [권한] 【2일차 수정】
     *
     * - ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
     *
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 활성 사용자 목록
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")  // 【2일차 수정】 MANAGER → BUSINESS_SUPPORT
    public ResponseEntity<Map<String, Object>> getActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("📥 [GET /api/users/active] 활성 사용자 목록 조회 요청");

        try {
            // 【v2.2 수정】 Pageable 객체 생성하여 Service 호출
            // UserService.getActiveUsers(Pageable) 메서드 시그니처에 맞춤
            Pageable pageable = PageRequest.of(page, size, Sort.by("username").ascending());
            Page<User> userPage = userService.getActiveUsers(pageable);

            Map<String, Object> response = new HashMap<>();
            response.put("users", userPage.getContent());
            response.put("currentPage", userPage.getNumber());
            response.put("totalPages", userPage.getTotalPages());
            response.put("totalElements", userPage.getTotalElements());
            response.put("pageSize", userPage.getSize());

            logger.info("📤 [200 OK] 활성 사용자 목록 조회 성공");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 활성 사용자 목록 조회 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "활성 사용자 목록을 조회할 수 없습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 특정 사용자 상세 정보 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/{id}
     * - Path Variable: id (사용자 ID)
     * - Response: 사용자 상세 정보
     * - Status: 200 OK / 404 Not Found
     *
     * [권한] 【2일차 수정】
     *
     * - ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
     *
     * @param id 조회할 사용자 ID
     * @return 사용자 정보 또는 404 응답
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")  // 【2일차 수정】 MANAGER → BUSINESS_SUPPORT
    public ResponseEntity<Object> getUserById(@PathVariable Long id) {

        logger.info("📥 [GET /api/users/{}] 사용자 상세 조회 요청", id);

        try {
            Optional<User> userOptional = userService.getUserById(id);

            if (userOptional.isPresent()) {
                logger.info("📤 [200 OK] 사용자 조회 성공");
                return ResponseEntity.ok(userOptional.get());
            } else {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음 - ID: {}", id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 사용자 조회 실패 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자를 조회할 수 없습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 사용자 검색 (이름 또는 이메일)
     *
     * 【v2.2 수정】 UserService.searchUsers(String) 메서드 호출 방식 변경
     * - 반환 타입: List<User> (페이징 미지원)
     * - page, size 파라미터는 Controller에서 처리하지 않음
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/search
     * - Query Parameter: keyword (검색 키워드)
     * - Response: 검색된 사용자 목록
     * - Status: 200 OK
     *
     * [권한] 【2일차 수정】
     *
     * - ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
     *
     * @param keyword 검색 키워드
     * @param page 페이지 번호 (현재 미사용 - List 반환)
     * @param size 페이지 크기 (현재 미사용 - List 반환)
     * @return 검색된 사용자 목록
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")  // 【2일차 수정】 MANAGER → BUSINESS_SUPPORT
    public ResponseEntity<Map<String, Object>> searchUsers(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        logger.info("📥 [GET /api/users/search] 사용자 검색 요청: '{}'", keyword);

        try {
            // 【v2.2 수정】 UserService.searchUsers(String) 호출
            // 반환 타입이 List<User>이므로 직접 List로 받음
            List<User> users = userService.searchUsers(keyword);

            // List를 사용하여 응답 구성 (페이징 정보 없음)
            Map<String, Object> response = new HashMap<>();
            response.put("users", users);
            response.put("totalElements", users.size());
            response.put("keyword", keyword);
            // 페이징 정보는 제공하지 않음 (Service에서 List 반환)

            logger.info("📤 [200 OK] 사용자 검색 성공: {}건", users.size());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 사용자 검색 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 검색에 실패했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // ================================
    // 생성 API (Create Operations)
    // ================================

    /**
     * 신규 사용자 생성
     *
     * [API 명세]
     *
     * - Method: POST
     * - URL: /api/users
     * - Request Body: User 객체
     * - Response: 생성된 사용자 정보
     * - Status: 201 Created / 400 Bad Request
     *
     * [권한]
     *
     * - ROLE_ADMIN: 사용자 생성 가능
     * - 기타: 접근 불가
     *
     * @param user 생성할 사용자 정보
     * @return 생성된 사용자 또는 에러 응답
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> createUser(@Valid @RequestBody User user) {

        logger.info("📥 [POST /api/users] 사용자 생성 요청: {}", user.getUsername());

        try {
            User createdUser = userService.createUser(user);

            logger.info("📤 [201 CREATED] 사용자 생성 성공");

            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("이미 사용 중인")) {
                logger.warn("📤 [400 BAD REQUEST] 중복된 사용자명 또는 이메일");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());

                return ResponseEntity.badRequest().body(errorResponse);
            }

            logger.error("❌ [500 ERROR] 사용자 생성 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 생성 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // ================================
    // 수정 API (Update Operations)
    // ================================

    /**
     * 사용자 정보 수정
     *
     * [API 명세]
     *
     * - Method: PUT
     * - URL: /api/users/{id}
     * - Path Variable: id (사용자 ID)
     * - Request Body: 수정할 사용자 정보
     * - Response: 수정된 사용자 정보
     * - Status: 200 OK / 404 Not Found
     *
     * [권한] 【2일차 수정】
     *
     * - ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
     *
     * @param id 수정할 사용자 ID
     * @param userDetails 수정할 정보
     * @return 수정된 사용자 또는 에러 응답
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")  // 【2일차 수정】 MANAGER → BUSINESS_SUPPORT
    public ResponseEntity<Object> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User userDetails) {

        logger.info("📥 [PUT /api/users/{}] 사용자 수정 요청", id);

        try {
            User updatedUser = userService.updateUser(id, userDetails);

            logger.info("📤 [200 OK] 사용자 수정 성공");

            return ResponseEntity.ok(updatedUser);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음");
                return ResponseEntity.notFound().build();
            }

            if (e.getMessage().contains("이미 사용 중인")) {
                logger.warn("📤 [400 BAD REQUEST] 중복된 이메일");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", e.getMessage());

                return ResponseEntity.badRequest().body(errorResponse);
            }

            logger.error("❌ [500 ERROR] 사용자 수정 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 수정 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 사용자 역할 변경 (Role ID 목록 기반)
     *
     * 【v2.3 수정】 changeUserRole(Long, String) → updateUserRoles(Long, List<Long>)
     *
     * UserService에는 changeUserRole(Long, String) 메서드가 없고,
     * updateUserRoles(Long userId, List<Long> roleIds) 메서드만 존재합니다.
     *
     * [API 명세]
     *
     * - Method: PUT
     * - URL: /api/users/{id}/roles
     * - Path Variable: id (사용자 ID)
     * - Request Body: { "roleIds": [1, 2, 3] }
     * - Response: 역할이 변경된 사용자 정보
     * - Status: 200 OK / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN: 역할 변경 가능
     * - 기타: 접근 불가
     *
     * @param id 사용자 ID
     * @param request roleIds 목록을 담은 요청 객체
     * @return 역할이 변경된 사용자 정보
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {

        logger.info("📥 [PUT /api/users/{}/roles] 역할 변경 요청", id);

        try {
            List<Long> roleIds = request.get("roleIds");

            if (roleIds == null || roleIds.isEmpty()) {
                logger.warn("📤 [400 BAD REQUEST] roleIds가 비어 있음");
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "roleIds는 필수입니다.");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            User updatedUser = userService.updateUserRoles(id, roleIds);

            logger.info("📤 [200 OK] 역할 변경 성공");

            return ResponseEntity.ok(updatedUser);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                logger.warn("📤 [404 NOT FOUND] 사용자 또는 역할을 찾을 수 없음");
                return ResponseEntity.notFound().build();
            }

            logger.error("❌ [500 ERROR] 역할 변경 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 변경 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /*
     * =========================================================================
     * 【v2.3 제거】 changeUserRole 메서드 (UserService에 해당 메서드 없음)
     * =========================================================================
     *
     * 기존 changeUserRole(Long, String) 메서드는 UserService에 존재하지 않습니다.
     * 대신 updateUserRoles(Long userId, List<Long> roleIds)를 사용하세요.
     *
     * 프론트엔드에서 roleName으로 역할을 변경해야 하는 경우:
     * 1. GET /api/roles로 역할 목록 조회
     * 2. roleName으로 roleId 찾기
     * 3. PUT /api/users/{id}/roles로 역할 변경
     *
     * 또는 UserService에 changeUserRole(Long userId, String roleName) 메서드를
     * 추가하면 이 엔드포인트를 다시 활성화할 수 있습니다.
     *
     * @PutMapping("/{id}/role")
     * @PreAuthorize("hasRole('ADMIN')")
     * public ResponseEntity<Object> changeUserRole(
     *         @PathVariable Long id,
     *         @RequestBody Map<String, String> request) {
     *     String roleName = request.get("roleName");
     *     User updatedUser = userService.changeUserRole(id, roleName);
     *     return ResponseEntity.ok(updatedUser);
     * }
     * =========================================================================
     */

    /**
     * 사용자 잠금 상태 변경
     *
     * [API 명세]
     *
     * - Method: PUT
     * - URL: /api/users/{id}/locked
     * - Path Variable: id (사용자 ID)
     * - Request Body: { "locked": true/false }
     * - Response: 상태가 변경된 사용자 정보
     * - Status: 200 OK / 404 Not Found
     *
     * [권한] 【2일차 수정】
     *
     * - ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
     *
     * @param id 사용자 ID
     * @param request 잠금 상태를 담은 요청 객체
     * @return 상태가 변경된 사용자 정보
     */
    @PutMapping("/{id}/locked")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")  // 【2일차 수정】 MANAGER → BUSINESS_SUPPORT
    public ResponseEntity<Object> toggleUserLocked(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        logger.info("📥 [PUT /api/users/{}/locked] 잠금 상태 변경 요청", id);

        try {
            Boolean locked = request.get("locked");
            User updatedUser = userService.toggleUserLocked(id, locked);

            logger.info("📤 [200 OK] 잠금 상태 변경 성공");

            return ResponseEntity.ok(updatedUser);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음");
                return ResponseEntity.notFound().build();
            }

            logger.error("❌ [500 ERROR] 잠금 상태 변경 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "잠금 상태 변경 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // ================================
    // 삭제 API (Delete Operations)
    // ================================

    /**
     * 사용자 삭제 (소프트 삭제)
     *
     * [API 명세]
     *
     * - Method: DELETE
     * - URL: /api/users/{id}
     * - Path Variable: id (사용자 ID)
     * - Response: 204 No Content / 404 Not Found
     * - Status: 204 No Content (성공)
     *
     * [권한]
     *
     * - ROLE_ADMIN: 사용자 삭제 가능
     * - 기타: 접근 불가
     *
     * [소프트 삭제]
     *
     * 실제로 데이터를 삭제하지 않고 isActive를 false로 설정:
     * - 데이터 보존
     * - 감사 추적 가능
     * - 복구 가능
     *
     * @param id 삭제할 사용자 ID
     * @return 204 No Content 또는 404 Not Found
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> deleteUser(@PathVariable Long id) {

        logger.info("📥 [DELETE /api/users/{}] 사용자 삭제 요청", id);

        try {
            userService.deleteUser(id);

            logger.info("📤 [204 NO CONTENT] 사용자 삭제 성공");

            return ResponseEntity.noContent().build();

        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음");
                return ResponseEntity.notFound().build();
            }

            logger.error("❌ [500 ERROR] 사용자 삭제 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 삭제 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // ================================
    // 통계 및 유틸리티 API
    // ================================

    /**
     * 활성 사용자 수 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/stats/active-count
     * - Response: { "count": 50 }
     * - Status: 200 OK
     *
     * [권한] 【2일차 수정】
     *
     * - ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
     *
     * @return 활성 사용자 수
     */
    @GetMapping("/stats/active-count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")  // 【2일차 수정】 MANAGER → BUSINESS_SUPPORT
    public ResponseEntity<Map<String, Object>> getActiveUserCount() {

        logger.info("📥 [GET /api/users/stats/active-count] 활성 사용자 수 조회");

        try {
            long count = userService.getActiveUserCount();

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);

            logger.info("📤 [200 OK] 활성 사용자 수: {}", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 활성 사용자 수 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 전체 사용자 수 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/stats/total-count
     * - Response: { "count": 100 }
     * - Status: 200 OK
     *
     * [권한] 【2일차 수정】
     *
     * - ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
     *
     * @return 전체 사용자 수
     */
    @GetMapping("/stats/total-count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")  // 【2일차 수정】 MANAGER → BUSINESS_SUPPORT
    public ResponseEntity<Map<String, Object>> getTotalUserCount() {

        logger.info("📥 [GET /api/users/stats/total-count] 전체 사용자 수 조회");

        try {
            long count = userService.getTotalUserCount();

            Map<String, Object> response = new HashMap<>();
            response.put("count", count);

            logger.info("📤 [200 OK] 전체 사용자 수: {}", count);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 전체 사용자 수 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자명 중복 확인
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/check/username?username={username}
     * - Query Parameter: username (확인할 사용자명)
     * - Response: { "exists": true/false }
     * - Status: 200 OK
     *
     * [사용 예시]
     *
     * 회원가입 폼에서 실시간으로 사용자명 중복 확인:
     * ```javascript
     * const checkUsername = async (username) => {
     *   const response = await axios.get(
     *     `/api/users/check/username?username=${username}`
     *   );
     *   return response.data.exists;
     * };
     * ```
     *
     * @param username 확인할 사용자명
     * @return 중복 여부 (exists: true/false)
     */
    @GetMapping("/check/username")
    public ResponseEntity<Map<String, Object>> checkUsername(
            @RequestParam String username) {

        logger.info("📥 [GET /api/users/check/username] 사용자명 중복 확인: {}", username);

        try {
            boolean exists = userService.isUsernameExists(username);

            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            logger.info("📤 [200 OK] 중복 확인 결과: {}", exists ? "중복" : "사용 가능");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 사용자명 중복 확인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 이메일 중복 확인
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/check/email?email={email}
     * - Query Parameter: email (확인할 이메일)
     * - Response: { "exists": true/false }
     * - Status: 200 OK
     *
     * @param email 확인할 이메일
     * @return 중복 여부 (exists: true/false)
     */
    @GetMapping("/check/email")
    public ResponseEntity<Map<String, Object>> checkEmail(
            @RequestParam String email) {

        logger.info("📥 [GET /api/users/check/email] 이메일 중복 확인: {}", email);

        try {
            boolean exists = userService.isEmailExists(email);

            Map<String, Object> response = new HashMap<>();
            response.put("exists", exists);

            logger.info("📤 [200 OK] 중복 확인 결과: {}", exists ? "중복" : "사용 가능");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 이메일 중복 확인 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}