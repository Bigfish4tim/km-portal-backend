package com.kmportal.backend.controller;

import com.kmportal.backend.entity.Role;
import com.kmportal.backend.service.RoleService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 역할 관리 REST API 컨트롤러 (리팩토링 버전)
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
 * ❌ 리팩토링 전 (765줄):
 * ```java
 * @GetMapping
 * public ResponseEntity<List<Role>> getAllRoles() {
 *     try {
 *         // Repository를 직접 호출
 *         List<Role> roles = roleRepository.findByIsActiveTrueOrderByPriorityAsc();
 *         return ResponseEntity.ok(roles);
 *     } catch (Exception e) {
 *         return ResponseEntity.status(500).build();
 *     }
 * }
 * ```
 *
 * ✅ 리팩토링 후 (약 400줄):
 * ```java
 * @GetMapping
 * public ResponseEntity<List<Role>> getAllRoles() {
 *     try {
 *         // Service에 위임 (비즈니스 로직은 Service가 처리)
 *         List<Role> roles = roleService.getAllActiveRoles();
 *         return ResponseEntity.ok(roles);
 *     } catch (Exception e) {
 *         return buildErrorResponse(e);
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
 *    - UserService에서도 RoleService 호출 가능
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
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoleController {

    /**
     * 로깅을 위한 Logger 인스턴스
     *
     * Controller에서는 주로 다음을 로깅합니다:
     * - HTTP 요청 수신 (요청 파라미터)
     * - Service 호출 성공/실패
     * - HTTP 응답 반환 (상태 코드)
     */
    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    /**
     * 역할 관리 비즈니스 로직을 담당하는 Service
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
    private final RoleService roleService;

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
     * @param roleService 역할 관리 서비스
     */
    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
        logger.info("✅ RoleController 초기화 완료");
        logger.debug("   - RoleService: {}", roleService.getClass().getSimpleName());
    }

    // ================================
    // 조회 API (Read Operations)
    // ================================

    /**
     * 모든 역할 목록 조회 (우선순위 순)
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles
     * - Response: 우선순위 순으로 정렬된 모든 활성 역할 목록
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN: 모든 역할 조회 가능 (시스템 역할 포함)
     * - 기타: 접근 불가 (403 Forbidden)
     *
     * [사용 예시]
     *
     * ```javascript
     * // 프론트엔드에서 호출
     * axios.get('/api/roles')
     *   .then(response => {
     *     console.log('역할 목록:', response.data);
     *   });
     * ```
     *
     * [응답 예시]
     *
     * ```json
     * [
     *   {
     *     "roleId": 1,
     *     "roleName": "ROLE_ADMIN",
     *     "displayName": "시스템 관리자",
     *     "priority": 1,
     *     "isSystemRole": true,
     *     "isActive": true
     *   },
     *   {
     *     "roleId": 2,
     *     "roleName": "ROLE_MANAGER",
     *     "displayName": "부서 관리자",
     *     "priority": 10,
     *     "isSystemRole": true,
     *     "isActive": true
     *   }
     * ]
     * ```
     *
     * @return 우선순위 순으로 정렬된 모든 활성 역할 목록
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getAllRoles() {
        logger.info("📥 [GET /api/roles] 전체 역할 목록 조회 요청");

        try {
            // Service에 비즈니스 로직 위임
            List<Role> roles = roleService.getAllActiveRoles();

            logger.info("📤 [GET /api/roles] 응답 성공 - 역할 수: {}개", roles.size());

            return ResponseEntity.ok(roles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles] 전체 역할 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 활성 역할만 조회 (일반 관리자도 접근 가능)
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/active
     * - Response: 활성 역할 목록 (우선순위 순)
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN: 접근 가능
     * - ROLE_MANAGER: 접근 가능
     * - 기타: 접근 불가 (403 Forbidden)
     *
     * @return 활성 역할 목록
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Role>> getActiveRoles() {
        logger.info("📥 [GET /api/roles/active] 활성 역할 목록 조회 요청");

        try {
            List<Role> activeRoles = roleService.getAllActiveRoles();

            logger.info("📤 [GET /api/roles/active] 응답 성공 - 역할 수: {}개", activeRoles.size());

            return ResponseEntity.ok(activeRoles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/active] 활성 역할 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 시스템 역할만 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/system
     * - Response: 시스템 역할 목록
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN만 접근 가능
     *
     * @return 시스템 역할 목록
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getSystemRoles() {
        logger.info("📥 [GET /api/roles/system] 시스템 역할 목록 조회 요청");

        try {
            List<Role> systemRoles = roleService.getSystemRoles();

            logger.info("📤 [GET /api/roles/system] 응답 성공 - 역할 수: {}개", systemRoles.size());

            return ResponseEntity.ok(systemRoles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/system] 시스템 역할 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자 정의 역할만 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/custom
     * - Response: 사용자 정의 역할 목록
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN만 접근 가능
     *
     * @return 사용자 정의 역할 목록
     */
    @GetMapping("/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getCustomRoles() {
        logger.info("📥 [GET /api/roles/custom] 사용자 정의 역할 목록 조회 요청");

        try {
            List<Role> customRoles = roleService.getCustomRoles();

            logger.info("📤 [GET /api/roles/custom] 응답 성공 - 역할 수: {}개", customRoles.size());

            return ResponseEntity.ok(customRoles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/custom] 사용자 정의 역할 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 특정 역할 상세 정보 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/{id}
     * - Path Variable: id (역할 ID)
     * - Response: 역할 상세 정보
     * - Status: 200 OK / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN: 접근 가능
     * - ROLE_MANAGER: 접근 가능
     * - 기타: 접근 불가
     *
     * @param id 역할 ID
     * @return 역할 상세 정보
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {
        logger.info("📥 [GET /api/roles/{}] 역할 상세 조회 요청", id);

        try {
            Role role = roleService.getRoleById(id);

            if (role != null) {
                logger.info("📤 [GET /api/roles/{}] 응답 성공 - 역할명: {}", id, role.getRoleName());
                return ResponseEntity.ok(role);
            } else {
                logger.warn("⚠️ [GET /api/roles/{}] 역할을 찾을 수 없음", id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/{}] 역할 상세 조회 실패", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 역할명으로 역할 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/name/{roleName}
     * - Path Variable: roleName (역할명, 예: ROLE_ADMIN)
     * - Response: 역할 정보
     * - Status: 200 OK / 404 Not Found
     *
     * @param roleName 역할명
     * @return 역할 정보
     */
    @GetMapping("/name/{roleName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Role> getRoleByName(@PathVariable String roleName) {
        logger.info("📥 [GET /api/roles/name/{}] 역할명으로 조회 요청", roleName);

        try {
            Role role = roleService.getRoleByName(roleName);

            if (role != null) {
                logger.info("📤 [GET /api/roles/name/{}] 응답 성공 - ID: {}", roleName, role.getRoleId());
                return ResponseEntity.ok(role);
            } else {
                logger.warn("⚠️ [GET /api/roles/name/{}] 역할을 찾을 수 없음", roleName);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/name/{}] 역할명으로 조회 실패", roleName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // 검색 및 필터링 API
    // ================================

    /**
     * 역할 검색 (표시명 기준)
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/search?keyword=검색어
     * - Query Parameter: keyword (검색 키워드)
     * - Response: 검색 결과 역할 목록
     * - Status: 200 OK
     *
     * [사용 예시]
     *
     * ```javascript
     * // "관리"라는 단어가 포함된 역할 검색
     * axios.get('/api/roles/search', {
     *   params: { keyword: '관리' }
     * });
     * ```
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 역할 목록
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Role>> searchRoles(@RequestParam String keyword) {
        logger.info("📥 [GET /api/roles/search] 역할 검색 요청 - 키워드: {}", keyword);

        try {
            List<Role> searchResults = roleService.searchRolesByDisplayName(keyword);

            logger.info("📤 [GET /api/roles/search] 응답 성공 - 결과 수: {}개", searchResults.size());

            return ResponseEntity.ok(searchResults);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/search] 역할 검색 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 우선순위 범위로 역할 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/priority-range?min=1&max=50
     * - Query Parameters:
     *   - min: 최소 우선순위
     *   - max: 최대 우선순위
     * - Response: 해당 우선순위 범위의 역할 목록
     * - Status: 200 OK
     *
     * @param minPriority 최소 우선순위
     * @param maxPriority 최대 우선순위
     * @return 해당 우선순위 범위의 역할 목록
     */
    @GetMapping("/priority-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getRolesByPriorityRange(
            @RequestParam Integer minPriority,
            @RequestParam Integer maxPriority) {

        logger.info("📥 [GET /api/roles/priority-range] 우선순위 범위 조회 - 범위: {} ~ {}",
                minPriority, maxPriority);

        try {
            List<Role> roles = roleService.getRolesByPriorityRange(minPriority, maxPriority);

            logger.info("📤 [GET /api/roles/priority-range] 응답 성공 - 결과 수: {}개", roles.size());

            return ResponseEntity.ok(roles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/priority-range] 우선순위 범위 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // 생성 및 수정 API (Write Operations)
    // ================================

    /**
     * 새로운 역할 생성
     *
     * [API 명세]
     *
     * - Method: POST
     * - URL: /api/roles
     * - Request Body: Role 객체 (JSON)
     * - Response: 생성된 역할 정보
     * - Status: 201 Created / 400 Bad Request
     *
     * [권한]
     *
     * - ROLE_ADMIN만 역할 생성 가능
     *
     * [Request Body 예시]
     *
     * ```json
     * {
     *   "roleName": "ROLE_EDITOR",
     *   "displayName": "콘텐츠 편집자",
     *   "description": "게시글 작성 및 수정 권한",
     *   "priority": 50
     * }
     * ```
     *
     * [유효성 검증]
     *
     * - roleName: ROLE_로 시작, 대문자와 언더스코어만
     * - displayName: 필수
     * - priority: 1~999 사이
     * - 역할명 중복 불가
     *
     * @param role 생성할 역할 정보
     * @return 생성된 역할 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createRole(@Valid @RequestBody Role role) {
        logger.info("📥 [POST /api/roles] 역할 생성 요청");
        logger.debug("   - 역할명: {}, 표시명: {}", role.getRoleName(), role.getDisplayName());

        try {
            // Service에 비즈니스 로직 위임
            Role savedRole = roleService.createRole(role);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할이 성공적으로 생성되었습니다.");
            response.put("role", savedRole);

            logger.info("📤 [POST /api/roles] 응답 성공 - 역할 ID: {}", savedRole.getRoleId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            // 비즈니스 규칙 위반 (클라이언트 오류)
            logger.warn("⚠️ [POST /api/roles] 유효성 검증 실패 - {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            // 예상치 못한 오류 (서버 오류)
            logger.error("❌ [POST /api/roles] 역할 생성 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 생성 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 역할 정보 수정
     *
     * [API 명세]
     *
     * - Method: PUT
     * - URL: /api/roles/{id}
     * - Path Variable: id (역할 ID)
     * - Request Body: 수정할 역할 정보 (JSON)
     * - Response: 수정된 역할 정보
     * - Status: 200 OK / 400 Bad Request / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN만 역할 수정 가능
     *
     * [주의사항]
     *
     * - 시스템 역할(isSystemRole=true)은 수정 불가
     * - roleName은 수정 불가 (권한 체계 유지)
     * - displayName, description, priority만 수정 가능
     *
     * @param id 수정할 역할 ID
     * @param role 수정할 정보
     * @return 수정된 역할 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody Role role) {

        logger.info("📥 [PUT /api/roles/{}] 역할 수정 요청", id);

        try {
            Role updatedRole = roleService.updateRole(id, role);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할이 성공적으로 수정되었습니다.");
            response.put("role", updatedRole);

            logger.info("📤 [PUT /api/roles/{}] 응답 성공", id);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [PUT /api/roles/{}] 유효성 검증 실패 - {}", id, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ [PUT /api/roles/{}] 역할 수정 실패", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 수정 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 삭제 및 활성화/비활성화 API
    // ================================

    /**
     * 역할 비활성화 (소프트 삭제)
     *
     * [API 명세]
     *
     * - Method: POST
     * - URL: /api/roles/{id}/deactivate
     * - Path Variable: id (역할 ID)
     * - Response: 처리 결과 메시지
     * - Status: 200 OK / 400 Bad Request / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN만 역할 비활성화 가능
     *
     * [주의사항]
     *
     * - 시스템 역할(isSystemRole=true)은 비활성화 불가
     * - 실제 삭제하지 않고 isActive=false로 설정
     * - 기존 사용자의 역할은 유지됨
     *
     * @param id 비활성화할 역할 ID
     * @return 처리 결과 메시지
     */
    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateRole(@PathVariable Long id) {
        logger.info("📥 [POST /api/roles/{}/deactivate] 역할 비활성화 요청", id);

        try {
            roleService.deactivateRole(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할이 성공적으로 비활성화되었습니다.");

            logger.info("📤 [POST /api/roles/{}/deactivate] 응답 성공", id);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [POST /api/roles/{}/deactivate] 비즈니스 규칙 위반 - {}", id, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ [POST /api/roles/{}/deactivate] 역할 비활성화 실패", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 비활성화 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 역할 활성화
     *
     * [API 명세]
     *
     * - Method: POST
     * - URL: /api/roles/{id}/activate
     * - Path Variable: id (역할 ID)
     * - Response: 처리 결과 메시지
     * - Status: 200 OK / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN만 역할 활성화 가능
     *
     * @param id 활성화할 역할 ID
     * @return 처리 결과 메시지
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activateRole(@PathVariable Long id) {
        logger.info("📥 [POST /api/roles/{}/activate] 역할 활성화 요청", id);

        try {
            roleService.activateRole(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할이 성공적으로 활성화되었습니다.");

            logger.info("📤 [POST /api/roles/{}/activate] 응답 성공", id);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [POST /api/roles/{}/activate] 역할을 찾을 수 없음", id);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("❌ [POST /api/roles/{}/activate] 역할 활성화 실패", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 활성화 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 역할 우선순위 업데이트
     *
     * [API 명세]
     *
     * - Method: PATCH
     * - URL: /api/roles/{id}/priority
     * - Path Variable: id (역할 ID)
     * - Request Body: {"priority": 50}
     * - Response: 처리 결과 메시지
     * - Status: 200 OK / 400 Bad Request / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN만 우선순위 변경 가능
     *
     * @param id 역할 ID
     * @param requestBody 새로운 우선순위 정보
     * @return 처리 결과 메시지
     */
    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRolePriority(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> requestBody) {

        logger.info("📥 [PATCH /api/roles/{}/priority] 우선순위 업데이트 요청", id);

        try {
            Integer newPriority = requestBody.get("priority");

            if (newPriority == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "우선순위 값이 필요합니다.");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            roleService.updateRolePriority(id, newPriority);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할 우선순위가 성공적으로 업데이트되었습니다.");
            response.put("priority", newPriority);

            logger.info("📤 [PATCH /api/roles/{}/priority] 응답 성공", id);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [PATCH /api/roles/{}/priority] 유효성 검증 실패 - {}", id, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ [PATCH /api/roles/{}/priority] 우선순위 업데이트 실패", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "우선순위 업데이트 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 통계 및 분석 API
    // ================================

    /**
     * 역할 통계 정보 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/statistics
     * - Response: 역할 통계 정보
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN만 통계 조회 가능
     *
     * [응답 예시]
     *
     * ```json
     * {
     *   "totalRoles": 5,
     *   "activeRoles": 4,
     *   "inactiveRoles": 1,
     *   "systemRoles": 3,
     *   "customRoles": 2,
     *   "roleUserStats": [...],
     *   "emptyRolesCount": 1,
     *   "emptyRoles": [...]
     * }
     * ```
     *
     * @return 역할 통계 정보
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRoleStatistics() {
        logger.info("📥 [GET /api/roles/statistics] 역할 통계 정보 조회 요청");

        try {
            Map<String, Object> statistics = roleService.getRoleStatistics();

            logger.info("📤 [GET /api/roles/statistics] 응답 성공");

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/statistics] 역할 통계 정보 조회 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "통계 정보를 조회할 수 없습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 특정 역할을 가진 사용자 수 조회
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/{id}/user-count
     * - Path Variable: id (역할 ID)
     * - Response: 해당 역할을 가진 사용자 수
     * - Status: 200 OK / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN: 접근 가능
     * - ROLE_MANAGER: 접근 가능
     *
     * @param id 역할 ID
     * @return 해당 역할을 가진 사용자 수
     */
    @GetMapping("/{id}/user-count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getRoleUserCount(@PathVariable Long id) {
        logger.info("📥 [GET /api/roles/{}/user-count] 역할별 사용자 수 조회 요청", id);

        try {
            Map<String, Object> response = roleService.getRoleUserCount(id);

            logger.info("📤 [GET /api/roles/{}/user-count] 응답 성공", id);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [GET /api/roles/{}/user-count] 역할을 찾을 수 없음", id);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/{}/user-count] 사용자 수 조회 실패", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 수 조회 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 유틸리티 API
    // ================================

    /**
     * 역할명 중복 확인
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/check-name?roleName=역할명
     * - Query Parameter: roleName (확인할 역할명)
     * - Response: 중복 여부 정보
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN만 확인 가능
     *
     * [응답 예시]
     *
     * ```json
     * {
     *   "roleName": "ROLE_EDITOR",
     *   "exists": false,
     *   "available": true
     * }
     * ```
     *
     * @param roleName 확인할 역할명
     * @return 중복 여부 정보
     */
    @GetMapping("/check-name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkRoleName(@RequestParam String roleName) {
        logger.info("📥 [GET /api/roles/check-name] 역할명 중복 확인 요청 - 역할명: {}", roleName);

        try {
            Map<String, Object> response = roleService.checkRoleNameDuplicate(roleName);

            logger.info("📤 [GET /api/roles/check-name] 응답 성공");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/check-name] 역할명 중복 확인 실패", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할명 확인 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}