package com.kmportal.backend.controller;

import com.kmportal.backend.dto.RoleDto;
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
 * 역할 관리 REST API 컨트롤러 (v2.0 - DTO 패턴 적용)
 *
 * [v2.0 업데이트 - 2026-01-29]
 *
 * 순환 참조 문제 해결:
 * - 기존: List<Role> 반환 → Role.users → User.roles → 무한 루프
 * - 변경: List<RoleDto> 반환 → users 필드 제외, userCount만 포함
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
 * @author KM Portal Dev Team
 * @version 2.0 (DTO 패턴 적용)
 * @since 2025-11-12
 */
@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoleController {

    /**
     * 로깅을 위한 Logger 인스턴스
     */
    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    /**
     * 역할 관리 비즈니스 로직을 담당하는 Service
     */
    private final RoleService roleService;

    /**
     * 생성자 기반 의존성 주입
     *
     * @param roleService 역할 관리 서비스
     */
    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
        logger.info("✅ RoleController 초기화 완료 (v2.0 - DTO 패턴 적용)");
        logger.debug("   - RoleService: {}", roleService.getClass().getSimpleName());
    }

    // ================================
    // 조회 API (Read Operations) - DTO 반환
    // ================================

    /**
     * 모든 역할 목록 조회 (우선순위 순) - DTO 반환
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles
     * - Response: 우선순위 순으로 정렬된 모든 활성 역할 DTO 목록
     * - Status: 200 OK
     *
     * [순환 참조 해결]
     *
     * - 기존: List<Role> → JSON 직렬화 시 무한 루프
     * - 변경: List<RoleDto> → users 필드 제외, userCount만 포함
     *
     * [권한]
     *
     * - ROLE_ADMIN: 모든 역할 조회 가능
     *
     * [응답 예시]
     *
     * ```json
     * [
     *   {
     *     "roleId": 1,
     *     "roleName": "ROLE_ADMIN",
     *     "displayName": "관리자",
     *     "priority": 1,
     *     "isSystemRole": true,
     *     "isActive": true,
     *     "userCount": 1
     *   },
     *   {
     *     "roleId": 2,
     *     "roleName": "ROLE_BUSINESS_SUPPORT",
     *     "displayName": "경영지원",
     *     "priority": 5,
     *     "isSystemRole": true,
     *     "isActive": true,
     *     "userCount": 2
     *   }
     * ]
     * ```
     *
     * @return 우선순위 순으로 정렬된 모든 활성 역할 DTO 목록
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        logger.info("📥 [GET /api/roles] 전체 역할 목록 조회 요청 (DTO)");

        try {
            // Service의 DTO 반환 메서드 호출 (순환 참조 해결)
            List<RoleDto> roles = roleService.getAllActiveRolesAsDto();

            logger.info("📤 [GET /api/roles] 응답 성공 - 역할 수: {}개", roles.size());

            return ResponseEntity.ok(roles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles] 전체 역할 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 활성 역할만 조회 (일반 관리자도 접근 가능) - DTO 반환
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/active
     * - Response: 활성 역할 DTO 목록 (우선순위 순)
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN: 접근 가능
     * - ROLE_BUSINESS_SUPPORT: 접근 가능 (경영지원)
     *
     * @return 활성 역할 DTO 목록
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")
    public ResponseEntity<List<RoleDto>> getActiveRoles() {
        logger.info("📥 [GET /api/roles/active] 활성 역할 목록 조회 요청 (DTO)");

        try {
            List<RoleDto> activeRoles = roleService.getAllActiveRolesAsDto();

            logger.info("📤 [GET /api/roles/active] 응답 성공 - 역할 수: {}개", activeRoles.size());

            return ResponseEntity.ok(activeRoles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/active] 활성 역할 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 시스템 역할만 조회 - DTO 반환
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/system
     * - Response: 시스템 역할 DTO 목록
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN만 접근 가능
     *
     * @return 시스템 역할 DTO 목록
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleDto>> getSystemRoles() {
        logger.info("📥 [GET /api/roles/system] 시스템 역할 목록 조회 요청 (DTO)");

        try {
            List<RoleDto> systemRoles = roleService.getSystemRolesAsDto();

            logger.info("📤 [GET /api/roles/system] 응답 성공 - 역할 수: {}개", systemRoles.size());

            return ResponseEntity.ok(systemRoles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/system] 시스템 역할 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자 정의 역할만 조회 - DTO 반환
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/custom
     * - Response: 사용자 정의 역할 DTO 목록
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN만 접근 가능
     *
     * @return 사용자 정의 역할 DTO 목록
     */
    @GetMapping("/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<RoleDto>> getCustomRoles() {
        logger.info("📥 [GET /api/roles/custom] 사용자 정의 역할 목록 조회 요청 (DTO)");

        try {
            List<RoleDto> customRoles = roleService.getCustomRolesAsDto();

            logger.info("📤 [GET /api/roles/custom] 응답 성공 - 역할 수: {}개", customRoles.size());

            return ResponseEntity.ok(customRoles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/custom] 사용자 정의 역할 목록 조회 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 특정 역할 상세 조회 - DTO 반환
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/{id}
     * - Path Variable: id (역할 ID)
     * - Response: 역할 DTO 상세 정보
     * - Status: 200 OK / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN: 접근 가능
     * - ROLE_BUSINESS_SUPPORT: 접근 가능
     *
     * @param id 역할 ID
     * @return 역할 DTO 상세 정보
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")
    public ResponseEntity<RoleDto> getRoleById(@PathVariable Long id) {
        logger.info("📥 [GET /api/roles/{}] 역할 상세 조회 요청 (DTO)", id);

        try {
            RoleDto role = roleService.getRoleByIdAsDto(id);

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
     * 역할명으로 역할 조회 - DTO 반환
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/name/{roleName}
     * - Path Variable: roleName (역할명)
     * - Response: 역할 DTO 상세 정보
     * - Status: 200 OK / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN: 접근 가능
     * - ROLE_BUSINESS_SUPPORT: 접근 가능
     *
     * @param roleName 역할명 (예: ROLE_ADMIN)
     * @return 역할 DTO 상세 정보
     */
    @GetMapping("/name/{roleName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")
    public ResponseEntity<RoleDto> getRoleByName(@PathVariable String roleName) {
        logger.info("📥 [GET /api/roles/name/{}] 역할명으로 조회 요청 (DTO)", roleName);

        try {
            RoleDto role = roleService.getRoleByNameAsDto(roleName);

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

    /**
     * 역할 검색 - DTO 반환
     *
     * [API 명세]
     *
     * - Method: GET
     * - URL: /api/roles/search?keyword=검색어
     * - Query Parameter: keyword (검색 키워드)
     * - Response: 검색 결과 역할 DTO 목록
     * - Status: 200 OK
     *
     * [권한]
     *
     * - ROLE_ADMIN: 접근 가능
     * - ROLE_BUSINESS_SUPPORT: 접근 가능
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 역할 DTO 목록
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")
    public ResponseEntity<List<RoleDto>> searchRoles(@RequestParam String keyword) {
        logger.info("📥 [GET /api/roles/search] 역할 검색 요청 (DTO) - 키워드: {}", keyword);

        try {
            List<RoleDto> roles = roleService.searchRolesAsDto(keyword);

            logger.info("📤 [GET /api/roles/search] 응답 성공 - 검색 결과: {}개", roles.size());

            return ResponseEntity.ok(roles);

        } catch (Exception e) {
            logger.error("❌ [GET /api/roles/search] 역할 검색 실패 - 키워드: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // 생성/수정/삭제 API (Write Operations)
    // ================================

    /**
     * 새로운 역할 생성
     *
     * [API 명세]
     *
     * - Method: POST
     * - URL: /api/roles
     * - Request Body: Role 정보 (JSON)
     * - Response: 생성된 역할 DTO 정보
     * - Status: 201 Created / 400 Bad Request
     *
     * [권한]
     *
     * - ROLE_ADMIN만 역할 생성 가능
     *
     * [요청 예시]
     *
     * ```json
     * {
     *   "roleName": "ROLE_EDITOR",
     *   "displayName": "편집자",
     *   "description": "콘텐츠 편집 권한",
     *   "priority": 50
     * }
     * ```
     *
     * @param role 생성할 역할 정보
     * @return 생성된 역할 DTO 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleDto> createRole(@Valid @RequestBody Role role) {
        logger.info("📥 [POST /api/roles] 새 역할 생성 요청 - 역할명: {}", role.getRoleName());

        try {
            Role createdRole = roleService.createRole(role);
            RoleDto dto = RoleDto.from(createdRole);

            logger.info("📤 [POST /api/roles] 응답 성공 - 생성된 역할 ID: {}", dto.getRoleId());

            return ResponseEntity.status(HttpStatus.CREATED).body(dto);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [POST /api/roles] 유효성 검증 실패 - {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (Exception e) {
            logger.error("❌ [POST /api/roles] 역할 생성 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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
     * - Response: 수정된 역할 DTO 정보
     * - Status: 200 OK / 400 Bad Request / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN만 역할 수정 가능
     *
     * @param id 수정할 역할 ID
     * @param role 수정할 정보
     * @return 수정된 역할 DTO 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoleDto> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody Role role) {

        logger.info("📥 [PUT /api/roles/{}] 역할 정보 수정 요청", id);

        try {
            Role updatedRole = roleService.updateRole(id, role);
            RoleDto dto = RoleDto.from(updatedRole);

            logger.info("📤 [PUT /api/roles/{}] 응답 성공", id);

            return ResponseEntity.ok(dto);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [PUT /api/roles/{}] 유효성 검증 실패 - {}", id, e.getMessage());

            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (Exception e) {
            logger.error("❌ [PUT /api/roles/{}] 역할 수정 실패", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 역할 삭제
     *
     * [API 명세]
     *
     * - Method: DELETE
     * - URL: /api/roles/{id}
     * - Path Variable: id (역할 ID)
     * - Response: 처리 결과 메시지
     * - Status: 200 OK / 400 Bad Request / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN만 역할 삭제 가능
     *
     * [비즈니스 규칙]
     *
     * - 시스템 역할 삭제 불가
     * - 사용자가 할당된 역할 삭제 불가
     *
     * @param id 삭제할 역할 ID
     * @return 처리 결과 메시지
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteRole(@PathVariable Long id) {
        logger.info("📥 [DELETE /api/roles/{}] 역할 삭제 요청", id);

        try {
            roleService.deleteRole(id);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할이 성공적으로 삭제되었습니다.");
            response.put("deletedId", id);

            logger.info("📤 [DELETE /api/roles/{}] 응답 성공", id);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ [DELETE /api/roles/{}] 삭제 실패 - {}", id, e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            if (e.getMessage().contains("찾을 수 없습니다")) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            logger.error("❌ [DELETE /api/roles/{}] 역할 삭제 실패", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 삭제 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 역할 비활성화
     *
     * [API 명세]
     *
     * - Method: POST
     * - URL: /api/roles/{id}/deactivate
     * - Path Variable: id (역할 ID)
     * - Response: 처리 결과 메시지
     * - Status: 200 OK / 404 Not Found
     *
     * [권한]
     *
     * - ROLE_ADMIN만 역할 비활성화 가능
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
            logger.warn("⚠️ [POST /api/roles/{}/deactivate] 역할을 찾을 수 없음", id);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", e.getMessage());

            return ResponseEntity.notFound().build();

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
     *   "totalRoles": 12,
     *   "activeRoles": 12,
     *   "inactiveRoles": 0,
     *   "systemRoles": 12,
     *   "customRoles": 0,
     *   "roleUserStats": [...],
     *   "emptyRolesCount": 0,
     *   "emptyRoles": []
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
     * - ROLE_BUSINESS_SUPPORT: 접근 가능
     *
     * @param id 역할 ID
     * @return 해당 역할을 가진 사용자 수
     */
    @GetMapping("/{id}/user-count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('BUSINESS_SUPPORT')")
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