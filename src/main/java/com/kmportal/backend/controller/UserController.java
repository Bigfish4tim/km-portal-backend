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
 * 사용자 관리 REST API 컨트롤러 (리팩토링 버전)
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
 * [리팩토링 전후 비교]
 *
 * ❌ 리팩토링 전 (1128줄):
 * ```java
 * @PostMapping
 * public ResponseEntity<?> createUser(@RequestBody User user) {
 *     // 중복 확인 (비즈니스 로직)
 *     if (userRepository.existsByUsername(user.getUsername())) {
 *         return ResponseEntity.badRequest()...;
 *     }
 *
 *     // 비밀번호 암호화 (비즈니스 로직)
 *     String encoded = passwordEncoder.encode(user.getPassword());
 *     user.setPassword(encoded);
 *
 *     // 역할 할당 (비즈니스 로직)
 *     Role userRole = roleRepository.findByRoleName("ROLE_USER")...;
 *     user.addRole(userRole);
 *
 *     // 저장 (비즈니스 로직)
 *     User saved = userRepository.save(user);
 *
 *     return ResponseEntity.ok(saved);
 * }
 * ```
 *
 * ✅ 리팩토링 후 (약 400줄):
 * ```java
 * @PostMapping
 * public ResponseEntity<?> createUser(@RequestBody User user) {
 *     try {
 *         // Service에 위임 (비즈니스 로직은 Service가 처리)
 *         User savedUser = userService.createUser(user);
 *         return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);
 *     } catch (IllegalArgumentException e) {
 *         return ResponseEntity.badRequest().body(...);
 *     }
 * }
 * ```
 *
 * [리팩토링의 장점]
 *
 * 1. 관심사의 분리 (Separation of Concerns)
 *    - Controller: HTTP 처리
 *    - Service: 비즈니스 로직
 *    - Repository: 데이터 액세스
 *
 * 2. 코드 재사용성
 *    - 여러 Controller에서 같은 Service 재사용
 *    - 배치 작업, 스케줄러 등에서도 Service 재사용
 *
 * 3. 테스트 용이성
 *    - Service만 단위 테스트 가능
 *    - Controller는 통합 테스트로 분리
 *
 * 4. 유지보수성
 *    - 각 계층의 책임이 명확
 *    - 변경 시 영향 범위 최소화
 *
 * 5. 가독성
 *    - Controller가 짧고 명확
 *    - HTTP 흐름 파악 용이
 *
 * @author KM Portal Dev Team
 * @version 2.0 (리팩토링)
 * @since 2025-11-12
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
     * [권한]
     *
     * - ROLE_ADMIN: 모든 사용자 조회 가능
     * - ROLE_MANAGER: 모든 사용자 조회 가능
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
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
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/active
     * - Response: 활성 사용자 목록 (페이징 지원)
     * - Status: 200 OK
     *
     * [사용 예시]
     *
     * ```
     * GET /api/users/active?page=0&size=20
     * Authorization: Bearer {token}
     * ```
     *
     * @param pageable 페이징 정보 (Spring이 자동으로 변환)
     * @return 활성 사용자 목록
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Page<User>> getActiveUsers(Pageable pageable) {

        logger.info("📥 [GET /api/users/active] 활성 사용자 조회 요청");

        try {
            Page<User> activeUsers = userService.getActiveUsers(pageable);

            logger.info("📤 [200 OK] 활성 사용자 조회 성공: {}명",
                    activeUsers.getTotalElements());

            return ResponseEntity.ok(activeUsers);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 활성 사용자 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
     * [권한]
     *
     * - ROLE_ADMIN: 모든 사용자 조회 가능
     * - ROLE_MANAGER: 모든 사용자 조회 가능
     * - 본인: 자신의 정보만 조회 가능
     * - 기타: 접근 불가
     *
     * [Spring Security 표현식]
     *
     * `#id == authentication.principal.userId`:
     * - #id: 메서드 파라미터의 id 값
     * - authentication: 현재 인증된 사용자 정보
     * - principal: 인증 주체 (UserDetails 구현체)
     * - userId: User 엔티티의 userId 필드
     *
     * @param id 사용자 ID
     * @return 사용자 상세 정보 또는 404
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or #id == authentication.principal.userId")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {

        logger.info("📥 [GET /api/users/{}] 사용자 조회 요청", id);

        try {
            Optional<User> userOptional = userService.getUserById(id);

            if (userOptional.isPresent()) {
                logger.info("📤 [200 OK] 사용자 조회 성공");
                return ResponseEntity.ok(userOptional.get());
            } else {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 사용자 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자명으로 사용자 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/username/{username}
     * - Path Variable: username (사용자명)
     * - Response: 사용자 정보
     * - Status: 200 OK / 404 Not Found
     *
     * @param username 사용자명
     * @return 사용자 정보 또는 404
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {

        logger.info("📥 [GET /api/users/username/{}] 사용자명으로 조회", username);

        try {
            Optional<User> userOptional = userService.getUserByUsername(username);

            if (userOptional.isPresent()) {
                logger.info("📤 [200 OK] 사용자 조회 성공");
                return ResponseEntity.ok(userOptional.get());
            } else {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음");
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 사용자 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 부서별 사용자 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/department/{department}
     * - Path Variable: department (부서명)
     * - Response: 해당 부서 사용자 목록
     * - Status: 200 OK
     *
     * @param department 부서명
     * @return 해당 부서 사용자 목록
     */
    @GetMapping("/department/{department}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<User>> getUsersByDepartment(@PathVariable String department) {

        logger.info("📥 [GET /api/users/department/{}] 부서별 사용자 조회", department);

        try {
            List<User> users = userService.getUsersByDepartment(department);

            logger.info("📤 [200 OK] 부서별 사용자 조회 성공: {}명", users.size());

            return ResponseEntity.ok(users);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 부서별 사용자 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자 검색 (이름 또는 이메일)
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/users/search?keyword={keyword}
     * - Query Parameter: keyword (검색 키워드)
     * - Response: 검색 결과 사용자 목록
     * - Status: 200 OK
     *
     * [사용 예시]
     *
     * ```
     * GET /api/users/search?keyword=kim
     * Authorization: Bearer {token}
     * ```
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 사용자 목록
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {

        logger.info("📥 [GET /api/users/search] 사용자 검색 요청 - 키워드: {}", keyword);

        try {
            List<User> users = userService.searchUsers(keyword);

            logger.info("📤 [200 OK] 사용자 검색 성공: {}명", users.size());

            return ResponseEntity.ok(users);

        } catch (Exception e) {
            logger.error("❌ [500 ERROR] 사용자 검색 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // 생성 API (Create Operations)
    // ================================

    /**
     * 새 사용자 생성
     *
     * [API 명세]
     *
     * - Method: POST
     * - URL: /api/users
     * - Request Body: User 객체 (JSON)
     * - Response: 생성된 사용자 정보
     * - Status: 201 Created / 400 Bad Request / 500 Internal Server Error
     *
     * [권한]
     *
     * - ROLE_ADMIN: 사용자 생성 가능
     * - 기타: 접근 불가
     *
     * [요청 예시]
     *
     * ```json
     * POST /api/users
     * Authorization: Bearer {token}
     * Content-Type: application/json
     *
     * {
     *   "username": "newuser",
     *   "password": "password123",
     *   "email": "newuser@example.com",
     *   "fullName": "홍길동",
     *   "department": "개발팀",
     *   "position": "사원"
     * }
     * ```
     *
     * [@Valid 어노테이션]
     *
     * Spring의 Bean Validation을 활성화:
     * - User 엔티티의 @NotBlank, @Email 등의 제약 조건 검증
     * - 검증 실패 시 400 Bad Request 자동 반환
     * - 검증 오류는 MethodArgumentNotValidException으로 처리
     *
     * @param user 생성할 사용자 정보
     * @return 생성된 사용자 정보 또는 오류 메시지
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> createUser(@Valid @RequestBody User user) {

        logger.info("📥 [POST /api/users] 사용자 생성 요청");
        logger.debug("   - 사용자명: {}", user.getUsername());
        logger.debug("   - 이메일: {}", user.getEmail());

        try {
            // Service에 비즈니스 로직 위임
            User savedUser = userService.createUser(user);

            logger.info("📤 [201 CREATED] 사용자 생성 성공");
            logger.debug("   - 생성된 사용자 ID: {}", savedUser.getUserId());

            return ResponseEntity.status(HttpStatus.CREATED).body(savedUser);

        } catch (IllegalArgumentException e) {
            // 중복 등 비즈니스 규칙 위반
            logger.warn("📤 [400 BAD REQUEST] 사용자 생성 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (Exception e) {
            // 시스템 오류
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
     * - Request Body: 수정할 사용자 정보 (JSON)
     * - Response: 수정된 사용자 정보
     * - Status: 200 OK / 400 Bad Request / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN: 모든 사용자 수정 가능
     * - ROLE_MANAGER: 모든 사용자 수정 가능
     * - 기타: 접근 불가
     *
     * [요청 예시]
     *
     * ```json
     * PUT /api/users/5
     * Authorization: Bearer {token}
     * Content-Type: application/json
     *
     * {
     *   "email": "updated@example.com",
     *   "fullName": "김철수",
     *   "department": "영업팀",
     *   "position": "과장"
     * }
     * ```
     *
     * @param id 수정할 사용자 ID
     * @param user 수정할 정보가 담긴 User 객체
     * @return 수정된 사용자 정보 또는 오류 메시지
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Object> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User user) {

        logger.info("📥 [PUT /api/users/{}] 사용자 수정 요청", id);

        try {
            User updatedUser = userService.updateUser(id, user);

            logger.info("📤 [200 OK] 사용자 수정 성공");

            return ResponseEntity.ok(updatedUser);

        } catch (IllegalArgumentException e) {
            logger.warn("📤 [400 BAD REQUEST] 사용자 수정 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음");
                return ResponseEntity.notFound().build();
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
     * 사용자 역할 변경
     *
     * [API 명세]
     *
     * - Method: PUT
     * - URL: /api/users/{id}/roles
     * - Path Variable: id (사용자 ID)
     * - Request Body: { "roleIds": [1, 2, 3] }
     * - Response: 역할이 변경된 사용자 정보
     * - Status: 200 OK / 400 Bad Request / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN: 역할 변경 가능
     * - ROLE_MANAGER: 역할 변경 가능
     * - 기타: 접근 불가
     *
     * [요청 예시]
     *
     * ```json
     * PUT /api/users/5/roles
     * Authorization: Bearer {token}
     * Content-Type: application/json
     *
     * {
     *   "roleIds": [1, 2]
     * }
     * ```
     *
     * [응답 예시]
     *
     * ```json
     * {
     *   "message": "역할이 성공적으로 변경되었습니다.",
     *   "user": { ... },
     *   "assignedCount": 2,
     *   "notFoundCount": 0
     * }
     * ```
     *
     * @param id 사용자 ID
     * @param request 역할 ID 목록을 담은 요청 객체
     * @return 변경된 사용자 정보와 처리 결과
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {

        logger.info("📥 [PUT /api/users/{}/roles] 역할 변경 요청", id);
        logger.debug("   - 역할 ID 목록: {}", request.get("roleIds"));

        try {
            List<Long> roleIds = request.get("roleIds");

            // Service에 비즈니스 로직 위임
            User updatedUser = userService.updateUserRoles(id, roleIds);

            // 응답 구성
            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할이 성공적으로 변경되었습니다.");
            response.put("user", updatedUser);
            response.put("assignedCount", roleIds.size());
            response.put("notFoundCount", 0);

            logger.info("📤 [200 OK] 역할 변경 성공");

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("📤 [400 BAD REQUEST] 역할 변경 실패: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(errorResponse);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음");
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

    /**
     * 사용자 활성화/비활성화
     *
     * [API 명세]
     *
     * - Method: PUT
     * - URL: /api/users/{id}/active
     * - Path Variable: id (사용자 ID)
     * - Request Body: { "active": true/false }
     * - Response: 상태가 변경된 사용자 정보
     * - Status: 200 OK / 404 Not Found
     *
     * @param id 사용자 ID
     * @param request 활성화 상태를 담은 요청 객체
     * @return 상태가 변경된 사용자 정보
     */
    @PutMapping("/{id}/active")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> toggleUserActive(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> request) {

        logger.info("📥 [PUT /api/users/{}/active] 활성화 상태 변경 요청", id);

        try {
            Boolean active = request.get("active");
            User updatedUser = userService.toggleUserActive(id, active);

            logger.info("📤 [200 OK] 활성화 상태 변경 성공");

            return ResponseEntity.ok(updatedUser);

        } catch (RuntimeException e) {
            if (e.getMessage().contains("찾을 수 없습니다")) {
                logger.warn("📤 [404 NOT FOUND] 사용자를 찾을 수 없음");
                return ResponseEntity.notFound().build();
            }

            logger.error("❌ [500 ERROR] 활성화 상태 변경 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "활성화 상태 변경 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 사용자 계정 잠금/해제
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
     * @param id 사용자 ID
     * @param request 잠금 상태를 담은 요청 객체
     * @return 상태가 변경된 사용자 정보
     */
    @PutMapping("/{id}/locked")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
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
     * @return 활성 사용자 수
     */
    @GetMapping("/stats/active-count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
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
     * @return 전체 사용자 수
     */
    @GetMapping("/stats/total-count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
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