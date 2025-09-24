package com.kmportal.backend.controller;

import com.kmportal.backend.entity.Role;
import com.kmportal.backend.repository.RoleRepository;
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
import java.util.Optional;

/**
 * 역할 관리 REST API 컨트롤러
 *
 * RBAC(Role-Based Access Control) 시스템의 역할 관리를 위한 컨트롤러입니다.
 * 시스템 역할과 사용자 정의 역할을 구분하여 관리합니다.
 *
 * 주요 기능:
 * - 역할 목록 조회 (전체, 활성, 시스템/사용자정의 구분)
 * - 역할 상세 정보 조회
 * - 사용자 정의 역할 생성/수정
 * - 역할 활성화/비활성화 (시스템 역할 제외)
 * - 역할 검색 및 통계
 * - 우선순위 기반 역할 관리
 *
 * 보안:
 * - 시스템 역할은 ADMIN만 조회 가능
 * - 역할 생성/수정/삭제는 ADMIN만 가능
 * - 시스템 역할은 삭제/수정 불가 (안전성 보장)
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-09-24
 */
@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*", maxAge = 3600)
public class RoleController {

    private static final Logger logger = LoggerFactory.getLogger(RoleController.class);

    @Autowired
    private RoleRepository roleRepository;

    // ================================
    // 조회 API 메서드
    // ================================

    /**
     * 모든 역할 목록 조회 (우선순위 순)
     *
     * GET /api/roles
     *
     * 권한: ADMIN만 접근 가능 (시스템 역할 포함)
     *
     * @return 우선순위 순으로 정렬된 모든 역할 목록
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getAllRoles() {

        try {
            logger.info("전체 역할 목록 조회 요청");

            // 우선순위 오름차순으로 모든 활성 역할 조회
            List<Role> roles = roleRepository.findByIsActiveTrueOrderByPriorityAsc();

            logger.info("전체 역할 목록 조회 성공 - {}개 역할", roles.size());

            return ResponseEntity.ok(roles);

        } catch (Exception e) {
            logger.error("전체 역할 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 활성 역할만 조회 (일반 사용자도 접근 가능)
     *
     * GET /api/roles/active
     *
     * 권한: MANAGER 이상 접근 가능
     *
     * @return 활성 역할 목록 (우선순위 순)
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Role>> getActiveRoles() {

        try {
            logger.info("활성 역할 목록 조회 요청");

            List<Role> activeRoles = roleRepository.findByIsActiveTrueOrderByPriorityAsc();

            logger.info("활성 역할 목록 조회 성공 - {}개 역할", activeRoles.size());

            return ResponseEntity.ok(activeRoles);

        } catch (Exception e) {
            logger.error("활성 역할 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 시스템 역할만 조회
     *
     * GET /api/roles/system
     *
     * @return 시스템 역할 목록
     */
    @GetMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getSystemRoles() {

        try {
            logger.info("시스템 역할 목록 조회 요청");

            List<Role> systemRoles = roleRepository.findByIsSystemRoleTrueAndIsActiveTrue();

            logger.info("시스템 역할 목록 조회 성공 - {}개 역할", systemRoles.size());

            return ResponseEntity.ok(systemRoles);

        } catch (Exception e) {
            logger.error("시스템 역할 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자 정의 역할만 조회
     *
     * GET /api/roles/custom
     *
     * @return 사용자 정의 역할 목록
     */
    @GetMapping("/custom")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getCustomRoles() {

        try {
            logger.info("사용자 정의 역할 목록 조회 요청");

            List<Role> customRoles = roleRepository.findByIsSystemRoleFalseAndIsActiveTrue();

            logger.info("사용자 정의 역할 목록 조회 성공 - {}개 역할", customRoles.size());

            return ResponseEntity.ok(customRoles);

        } catch (Exception e) {
            logger.error("사용자 정의 역할 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 특정 역할 상세 정보 조회
     *
     * GET /api/roles/{id}
     *
     * @param id 역할 ID
     * @return 역할 상세 정보
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Role> getRoleById(@PathVariable Long id) {

        try {
            logger.info("역할 상세 조회 요청 - ID: {}", id);

            Optional<Role> roleOptional = roleRepository.findById(id);

            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();
                logger.info("역할 상세 조회 성공 - 역할명: {}", role.getRoleName());
                return ResponseEntity.ok(role);
            } else {
                logger.warn("역할을 찾을 수 없음 - ID: {}", id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("역할 상세 조회 중 오류 발생 - ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 역할명으로 역할 조회
     *
     * GET /api/roles/name/{roleName}
     *
     * @param roleName 역할명
     * @return 역할 정보
     */
    @GetMapping("/name/{roleName}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Role> getRoleByName(@PathVariable String roleName) {

        try {
            logger.info("역할명으로 조회 요청 - 역할명: {}", roleName);

            Optional<Role> roleOptional = roleRepository.findByRoleName(roleName);

            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();
                logger.info("역할명으로 조회 성공 - ID: {}", role.getRoleId());
                return ResponseEntity.ok(role);
            } else {
                logger.warn("역할을 찾을 수 없음 - 역할명: {}", roleName);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("역할명으로 조회 중 오류 발생 - 역할명: {}", roleName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // 검색 및 필터링 API 메서드
    // ================================

    /**
     * 역할 검색 (표시명 기준)
     *
     * GET /api/roles/search?keyword=검색어
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 역할 목록
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<Role>> searchRoles(@RequestParam String keyword) {

        try {
            logger.info("역할 검색 요청 - 키워드: {}", keyword);

            List<Role> searchResults = roleRepository
                    .findByDisplayNameContainingIgnoreCase(keyword);

            logger.info("역할 검색 완료 - 키워드: {}, 결과: {}개", keyword, searchResults.size());

            return ResponseEntity.ok(searchResults);

        } catch (Exception e) {
            logger.error("역할 검색 중 오류 발생 - 키워드: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 우선순위 범위로 역할 조회
     *
     * GET /api/roles/priority-range?min=1&max=50
     *
     * @param minPriority 최소 우선순위
     * @param maxPriority 최대 우선순위
     * @return 해당 범위의 역할 목록
     */
    @GetMapping("/priority-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Role>> getRolesByPriorityRange(
            @RequestParam Integer minPriority,
            @RequestParam Integer maxPriority) {

        try {
            logger.info("우선순위 범위로 역할 조회 요청 - 범위: {} ~ {}", minPriority, maxPriority);

            List<Role> roles = roleRepository.findRolesByPriorityRange(minPriority, maxPriority);

            logger.info("우선순위 범위 조회 완료 - {}개 역할", roles.size());

            return ResponseEntity.ok(roles);

        } catch (Exception e) {
            logger.error("우선순위 범위 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // 생성/수정 API 메서드
    // ================================

    /**
     * 새로운 사용자 정의 역할 생성
     *
     * POST /api/roles
     * Content-Type: application/json
     *
     * 주의: 시스템 역할은 코드로만 생성 가능
     *
     * @param role 생성할 역할 정보
     * @return 생성된 역할 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createRole(@Valid @RequestBody Role role) {

        try {
            logger.info("새로운 역할 생성 요청 - 역할명: {}", role.getRoleName());

            // 역할명 중복 검사
            if (roleRepository.existsByRoleName(role.getRoleName())) {
                logger.warn("역할명 중복 - 역할명: {}", role.getRoleName());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "역할명이 이미 존재합니다.");
                errorResponse.put("field", "roleName");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // 표시명 중복 검사
            if (roleRepository.existsByDisplayName(role.getDisplayName())) {
                logger.warn("표시명 중복 - 표시명: {}", role.getDisplayName());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "표시명이 이미 존재합니다.");
                errorResponse.put("field", "displayName");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // 역할명 형식 검증 (ROLE_로 시작하는지 확인)
            if (!role.getRoleName().startsWith("ROLE_")) {
                logger.warn("잘못된 역할명 형식 - 역할명: {}", role.getRoleName());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "역할명은 'ROLE_'로 시작해야 합니다.");
                errorResponse.put("field", "roleName");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            // 사용자 정의 역할로 설정 (시스템 역할은 API로 생성 불가)
            role.setIsSystemRole(false);
            role.setIsActive(true);

            // 우선순위가 설정되지 않았다면 기본값 설정
            if (role.getPriority() == null) {
                role.setPriority(200); // 사용자 정의 역할 기본 우선순위
            }

            Role savedRole = roleRepository.save(role);

            logger.info("새로운 역할 생성 성공 - ID: {}, 역할명: {}",
                    savedRole.getRoleId(), savedRole.getRoleName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할이 성공적으로 생성되었습니다.");
            response.put("role", savedRole);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("역할 생성 중 오류 발생 - 역할명: {}", role.getRoleName(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 생성 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 역할 정보 수정
     *
     * PUT /api/roles/{id}
     * Content-Type: application/json
     *
     * 주의: 시스템 역할은 수정 불가
     *
     * @param id 수정할 역할 ID
     * @param roleDetails 수정할 역할 정보
     * @return 수정된 역할 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody Role roleDetails) {

        try {
            logger.info("역할 정보 수정 요청 - ID: {}", id);

            Optional<Role> roleOptional = roleRepository.findById(id);

            if (!roleOptional.isPresent()) {
                logger.warn("수정할 역할을 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "역할을 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

            Role existingRole = roleOptional.get();

            // 시스템 역할 수정 방지
            if (existingRole.getIsSystemRole()) {
                logger.warn("시스템 역할 수정 시도 - ID: {}, 역할명: {}", id, existingRole.getRoleName());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "시스템 역할은 수정할 수 없습니다.");
                errorResponse.put("roleType", "system");

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            // 수정 가능한 필드만 업데이트
            existingRole.setDisplayName(roleDetails.getDisplayName());
            existingRole.setDescription(roleDetails.getDescription());
            existingRole.setPriority(roleDetails.getPriority());

            // 역할명과 시스템 역할 여부는 수정 불가
            // existingRole.setRoleName() - 변경 불가
            // existingRole.setIsSystemRole() - 변경 불가

            Role updatedRole = roleRepository.save(existingRole);

            logger.info("역할 정보 수정 성공 - ID: {}, 역할명: {}",
                    updatedRole.getRoleId(), updatedRole.getRoleName());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할 정보가 성공적으로 수정되었습니다.");
            response.put("role", updatedRole);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("역할 정보 수정 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 정보 수정 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 상태 관리 API 메서드
    // ================================

    /**
     * 역할 비활성화 (소프트 삭제)
     *
     * DELETE /api/roles/{id}
     *
     * 주의: 시스템 역할은 비활성화 불가
     *
     * @param id 비활성화할 역할 ID
     * @return 처리 결과
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateRole(@PathVariable Long id) {

        try {
            logger.info("역할 비활성화 요청 - ID: {}", id);

            Optional<Role> roleOptional = roleRepository.findById(id);

            if (!roleOptional.isPresent()) {
                logger.warn("비활성화할 역할을 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "역할을 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

            Role role = roleOptional.get();

            // 시스템 역할 비활성화 방지
            if (role.getIsSystemRole()) {
                logger.warn("시스템 역할 비활성화 시도 - ID: {}, 역할명: {}", id, role.getRoleName());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "시스템 역할은 비활성화할 수 없습니다.");
                errorResponse.put("roleType", "system");

                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(errorResponse);
            }

            int updatedRows = roleRepository.deactivateRole(id);

            if (updatedRows > 0) {
                logger.info("역할 비활성화 성공 - ID: {}, 역할명: {}", id, role.getRoleName());

                Map<String, Object> response = new HashMap<>();
                response.put("message", "역할이 성공적으로 비활성화되었습니다.");

                return ResponseEntity.ok(response);
            } else {
                logger.error("역할 비활성화 실패 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "역할 비활성화에 실패했습니다.");

                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
            }

        } catch (Exception e) {
            logger.error("역할 비활성화 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 비활성화 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 역할 활성화
     *
     * POST /api/roles/{id}/activate
     *
     * @param id 활성화할 역할 ID
     * @return 처리 결과
     */
    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activateRole(@PathVariable Long id) {

        try {
            logger.info("역할 활성화 요청 - ID: {}", id);

            int updatedRows = roleRepository.activateRole(id);

            if (updatedRows > 0) {
                logger.info("역할 활성화 성공 - ID: {}", id);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "역할이 성공적으로 활성화되었습니다.");

                return ResponseEntity.ok(response);
            } else {
                logger.warn("활성화할 역할을 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "역할을 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("역할 활성화 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 활성화 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 역할 우선순위 업데이트
     *
     * PATCH /api/roles/{id}/priority
     * Content-Type: application/json
     * Body: {"priority": 50}
     *
     * @param id 역할 ID
     * @param requestBody 새로운 우선순위 정보
     * @return 처리 결과
     */
    @PatchMapping("/{id}/priority")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateRolePriority(
            @PathVariable Long id,
            @RequestBody Map<String, Integer> requestBody) {

        try {
            Integer newPriority = requestBody.get("priority");

            if (newPriority == null) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "우선순위 값이 필요합니다.");

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
            }

            logger.info("역할 우선순위 업데이트 요청 - ID: {}, 새 우선순위: {}", id, newPriority);

            int updatedRows = roleRepository.updateRolePriority(id, newPriority);

            if (updatedRows > 0) {
                logger.info("역할 우선순위 업데이트 성공 - ID: {}", id);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "역할 우선순위가 성공적으로 업데이트되었습니다.");
                response.put("priority", newPriority);

                return ResponseEntity.ok(response);
            } else {
                logger.warn("우선순위 업데이트할 역할을 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "역할을 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("역할 우선순위 업데이트 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "우선순위 업데이트 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 통계 및 분석 API 메서드
    // ================================

    /**
     * 역할 통계 정보 조회
     *
     * GET /api/roles/statistics
     *
     * @return 역할 통계 정보
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getRoleStatistics() {

        try {
            logger.info("역할 통계 정보 조회 요청");

            long totalRoles = roleRepository.count();
            long activeRoles = roleRepository.countByIsActiveTrue();
            long systemRoles = roleRepository.countByIsSystemRoleTrue();
            long customRoles = roleRepository.countByIsSystemRoleFalse();

            // 역할별 사용자 수 통계
            List<Object[]> roleUserStats = roleRepository.getRoleUserStatistics();

            // 사용자가 없는 역할 조회
            List<Role> emptyRoles = roleRepository.findRolesWithoutUsers();

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalRoles", totalRoles);
            statistics.put("activeRoles", activeRoles);
            statistics.put("inactiveRoles", totalRoles - activeRoles);
            statistics.put("systemRoles", systemRoles);
            statistics.put("customRoles", customRoles);
            statistics.put("roleUserStats", roleUserStats);
            statistics.put("emptyRolesCount", emptyRoles.size());
            statistics.put("emptyRoles", emptyRoles);

            logger.info("역할 통계 정보 조회 성공 - 전체: {}, 활성: {}, 시스템: {}, 사용자정의: {}",
                    totalRoles, activeRoles, systemRoles, customRoles);

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("역할 통계 정보 조회 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "통계 정보를 조회할 수 없습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 특정 역할을 가진 사용자 수 조회
     *
     * GET /api/roles/{id}/user-count
     *
     * @param id 역할 ID
     * @return 해당 역할을 가진 사용자 수
     */
    @GetMapping("/{id}/user-count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getRoleUserCount(@PathVariable Long id) {

        try {
            logger.info("역할별 사용자 수 조회 요청 - 역할 ID: {}", id);

            long userCount = roleRepository.countUsersByRoleId(id);

            Optional<Role> roleOptional = roleRepository.findById(id);
            String roleName = roleOptional.map(Role::getDisplayName).orElse("알 수 없음");

            Map<String, Object> response = new HashMap<>();
            response.put("roleId", id);
            response.put("roleName", roleName);
            response.put("userCount", userCount);

            logger.info("역할별 사용자 수 조회 완료 - 역할: {}, 사용자 수: {}명", roleName, userCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("역할별 사용자 수 조회 중 오류 발생 - 역할 ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 수 조회 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 유틸리티 API 메서드
    // ================================

    /**
     * 역할명 중복 확인
     *
     * GET /api/roles/check-name?roleName=역할명
     *
     * @param roleName 확인할 역할명
     * @return 중복 여부 정보
     */
    @GetMapping("/check-name")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> checkRoleName(@RequestParam String roleName) {

        try {
            logger.info("역할명 중복 확인 요청 - 역할명: {}", roleName);

            boolean exists = roleRepository.existsByRoleName(roleName);

            Map<String, Object> response = new HashMap<>();
            response.put("roleName", roleName);
            response.put("exists", exists);
            response.put("available", !exists);

            logger.info("역할명 중복 확인 완료 - 역할명: {}, 사용가능: {}", roleName, !exists);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("역할명 중복 확인 중 오류 발생 - 역할명: {}", roleName, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할명 확인 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}