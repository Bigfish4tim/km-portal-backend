package com.kmportal.backend.service;

import com.kmportal.backend.dto.RoleDto;
import com.kmportal.backend.entity.Role;
import com.kmportal.backend.repository.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 역할 관리 서비스 (RoleService)
 *
 * 이 클래스는 역할 관리와 관련된 모든 비즈니스 로직을 처리합니다.
 * Controller는 HTTP 요청/응답만 처리하고, 실제 비즈니스 로직은
 * 이 Service 계층에서 처리됩니다.
 *
 * [왜 Service 계층이 필요한가?]
 *
 * 1. 관심사의 분리 (Separation of Concerns)
 *    - Controller: HTTP 요청/응답 처리
 *    - Service: 비즈니스 로직 처리
 *    - Repository: 데이터베이스 접근
 *
 * 2. 재사용성
 *    - 여러 Controller에서 같은 비즈니스 로직을 재사용 가능
 *    - UserService에서도 RoleService를 호출하여 역할 관리 가능
 *
 * 3. 테스트 용이성
 *    - Service만 따로 단위 테스트 가능
 *    - HTTP 없이 비즈니스 로직만 테스트
 *
 * 4. 트랜잭션 관리
 *    - @Transactional로 여러 DB 작업을 하나의 트랜잭션으로 묶음
 *    - 중간에 오류 발생 시 자동 롤백
 *
 * [Service 계층의 책임]
 *
 * - 비즈니스 규칙 검증 (예: 시스템 역할 삭제 방지)
 * - 여러 Repository 조합하여 복잡한 작업 수행
 * - 도메인 로직 처리 (예: 역할 우선순위 관리)
 * - 트랜잭션 경계 설정
 * - 데이터 변환 및 가공 (Entity ↔ DTO)
 *
 * [v2.0 업데이트 - 2026-01-29]
 * - DTO 패턴 적용: 순환 참조 문제 해결
 * - getAllActiveRolesAsDto() 등 DTO 반환 메서드 추가
 *
 * @author KM Portal Dev Team
 * @version 2.0
 * @since 2025-11-12
 */
@Service
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 트랜잭션
public class RoleService {

    /**
     * 로깅을 위한 Logger 인스턴스
     *
     * Service 계층에서는 비즈니스 로직의 실행 흐름과
     * 중요한 의사결정 과정을 로깅합니다.
     */
    private static final Logger logger = LoggerFactory.getLogger(RoleService.class);

    // ================================
    // 의존성 주입 (Dependency Injection)
    // ================================

    /**
     * 역할 데이터 액세스를 위한 Repository
     *
     * Repository 패턴:
     * - 데이터 저장소(DB)에 대한 추상화 계층
     * - Service는 어떤 DB를 사용하는지 몰라도 됨
     * - 테스트 시 Mock Repository로 쉽게 대체 가능
     */
    private final RoleRepository roleRepository;

    /**
     * 생성자 기반 의존성 주입
     *
     * [생성자 주입의 장점]
     *
     * 1. 불변성 (Immutability)
     *    - final 필드로 선언 가능
     *    - 객체 생성 후 의존성 변경 불가
     *
     * 2. 테스트 용이성
     *    - 생성자로 Mock 객체 주입 가능
     *    - @Autowired 없이 순수 Java 테스트 가능
     *
     * 3. 순환 참조 방지
     *    - 순환 참조 시 컴파일 타임에 에러 발생
     *    - @Autowired는 런타임에야 발견됨
     *
     * 4. 명시성
     *    - 필요한 의존성이 명확히 드러남
     *    - 의존성이 많으면 리팩토링 신호
     *
     * @param roleRepository 역할 Repository
     */
    @Autowired
    public RoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;

        logger.info("✅ RoleService 초기화 완료 (v2.0 - DTO 패턴 적용)");
        logger.debug("   - RoleRepository: {}", roleRepository.getClass().getSimpleName());
    }

    // ================================
    // DTO 반환 메서드 (순환 참조 해결)
    // ================================

    /**
     * 모든 활성 역할 목록을 DTO로 조회 (우선순위 순)
     *
     * [순환 참조 해결]
     * - Role 엔티티 대신 RoleDto 반환
     * - users 컬렉션 대신 userCount 숫자만 포함
     * - JSON 직렬화 시 무한 루프 방지
     *
     * [사용 예시]
     * ```java
     * // Controller에서 호출
     * List<RoleDto> roles = roleService.getAllActiveRolesAsDto();
     * return ResponseEntity.ok(roles);
     * ```
     *
     * @return 우선순위 순으로 정렬된 모든 활성 역할 DTO 목록
     */
    public List<RoleDto> getAllActiveRolesAsDto() {
        logger.info("📋 전체 활성 역할 목록 조회 시작 (DTO 반환)");

        try {
            // 1. Repository에서 엔티티 조회
            List<Role> roles = roleRepository.findByIsActiveTrueOrderByPriorityAsc();

            // 2. Entity → DTO 변환 (Stream API 사용)
            List<RoleDto> roleDtos = roles.stream()
                    .map(RoleDto::from)  // Role → RoleDto 변환
                    .collect(Collectors.toList());

            logger.info("✅ 전체 활성 역할 목록 조회 성공 (DTO)");
            logger.info("   - 조회된 역할 수: {}", roleDtos.size());

            return roleDtos;

        } catch (Exception e) {
            logger.error("❌ 전체 활성 역할 목록 조회 실패 (DTO)", e);
            throw new RuntimeException("역할 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 시스템 역할만 DTO로 조회
     *
     * @return 시스템 역할 DTO 목록
     */
    public List<RoleDto> getSystemRolesAsDto() {
        logger.info("📋 시스템 역할 목록 조회 시작 (DTO 반환)");

        try {
            List<Role> systemRoles = roleRepository.findByIsSystemRoleTrueAndIsActiveTrue();

            List<RoleDto> roleDtos = systemRoles.stream()
                    .map(RoleDto::from)
                    .collect(Collectors.toList());

            logger.info("✅ 시스템 역할 목록 조회 성공 (DTO)");
            logger.info("   - 조회된 시스템 역할 수: {}", roleDtos.size());

            return roleDtos;

        } catch (Exception e) {
            logger.error("❌ 시스템 역할 목록 조회 실패 (DTO)", e);
            throw new RuntimeException("시스템 역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 정의 역할만 DTO로 조회
     *
     * @return 사용자 정의 역할 DTO 목록
     */
    public List<RoleDto> getCustomRolesAsDto() {
        logger.info("📋 사용자 정의 역할 목록 조회 시작 (DTO 반환)");

        try {
            List<Role> customRoles = roleRepository.findByIsSystemRoleFalseAndIsActiveTrue();

            List<RoleDto> roleDtos = customRoles.stream()
                    .map(RoleDto::from)
                    .collect(Collectors.toList());

            logger.info("✅ 사용자 정의 역할 목록 조회 성공 (DTO)");
            logger.info("   - 조회된 사용자 정의 역할 수: {}", roleDtos.size());

            return roleDtos;

        } catch (Exception e) {
            logger.error("❌ 사용자 정의 역할 목록 조회 실패 (DTO)", e);
            throw new RuntimeException("사용자 정의 역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 역할을 DTO로 조회
     *
     * @param id 역할 ID
     * @return 역할 DTO (없으면 null)
     */
    public RoleDto getRoleByIdAsDto(Long id) {
        logger.info("🔍 역할 상세 조회 시작 (DTO 반환) - ID: {}", id);

        try {
            Optional<Role> roleOptional = roleRepository.findById(id);

            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();
                RoleDto dto = RoleDto.from(role);
                logger.info("✅ 역할 상세 조회 성공 (DTO) - 역할명: {}", role.getRoleName());
                return dto;
            } else {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                return null;
            }

        } catch (Exception e) {
            logger.error("❌ 역할 상세 조회 실패 (DTO) - ID: {}", id, e);
            throw new RuntimeException("역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할명으로 역할을 DTO로 조회
     *
     * @param roleName 역할명
     * @return 역할 DTO (없으면 null)
     */
    public RoleDto getRoleByNameAsDto(String roleName) {
        logger.info("🔍 역할명으로 조회 시작 (DTO 반환) - 역할명: {}", roleName);

        try {
            Optional<Role> roleOptional = roleRepository.findByRoleName(roleName);

            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();
                RoleDto dto = RoleDto.from(role);
                logger.info("✅ 역할명으로 조회 성공 (DTO) - ID: {}", role.getRoleId());
                return dto;
            } else {
                logger.warn("⚠️ 역할을 찾을 수 없음 - 역할명: {}", roleName);
                return null;
            }

        } catch (Exception e) {
            logger.error("❌ 역할명으로 조회 실패 (DTO) - 역할명: {}", roleName, e);
            throw new RuntimeException("역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할 검색 결과를 DTO로 반환
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 DTO 목록
     */
    public List<RoleDto> searchRolesAsDto(String keyword) {
        logger.info("🔍 역할 검색 시작 (DTO 반환) - 키워드: {}", keyword);

        try {
            List<Role> roles = roleRepository.findByDisplayNameContainingIgnoreCase(keyword);

            List<RoleDto> roleDtos = roles.stream()
                    .map(RoleDto::from)
                    .collect(Collectors.toList());

            logger.info("✅ 역할 검색 성공 (DTO)");
            logger.info("   - 검색 결과 수: {}", roleDtos.size());

            return roleDtos;

        } catch (Exception e) {
            logger.error("❌ 역할 검색 실패 (DTO) - 키워드: {}", keyword, e);
            throw new RuntimeException("역할 검색 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 조회 메서드 (Read Operations) - 기존 유지
    // ================================

    /**
     * 모든 활성 역할 목록 조회 (우선순위 순)
     *
     * [비즈니스 규칙]
     *
     * - 활성 역할만 조회 (isActive = true)
     * - 우선순위 오름차순 정렬 (낮은 숫자 = 높은 권한이 먼저)
     * - 시스템 역할과 사용자 정의 역할 모두 포함
     *
     * @return 우선순위 순으로 정렬된 모든 활성 역할 목록
     * @deprecated DTO 버전 사용 권장: {@link #getAllActiveRolesAsDto()}
     */
    @Deprecated
    public List<Role> getAllActiveRoles() {
        logger.info("📋 전체 활성 역할 목록 조회 시작");

        try {
            // Repository를 통해 데이터 조회
            List<Role> roles = roleRepository.findByIsActiveTrueOrderByPriorityAsc();

            logger.info("✅ 전체 활성 역할 목록 조회 성공");
            logger.info("   - 조회된 역할 수: {}", roles.size());

            return roles;

        } catch (Exception e) {
            logger.error("❌ 전체 활성 역할 목록 조회 실패", e);
            throw new RuntimeException("역할 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 시스템 역할만 조회
     *
     * @return 시스템 역할 목록
     * @deprecated DTO 버전 사용 권장: {@link #getSystemRolesAsDto()}
     */
    @Deprecated
    public List<Role> getSystemRoles() {
        logger.info("📋 시스템 역할 목록 조회 시작");

        try {
            List<Role> systemRoles = roleRepository.findByIsSystemRoleTrueAndIsActiveTrue();

            logger.info("✅ 시스템 역할 목록 조회 성공");
            logger.info("   - 조회된 시스템 역할 수: {}", systemRoles.size());

            return systemRoles;

        } catch (Exception e) {
            logger.error("❌ 시스템 역할 목록 조회 실패", e);
            throw new RuntimeException("시스템 역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 정의 역할만 조회
     *
     * @return 사용자 정의 역할 목록
     * @deprecated DTO 버전 사용 권장: {@link #getCustomRolesAsDto()}
     */
    @Deprecated
    public List<Role> getCustomRoles() {
        logger.info("📋 사용자 정의 역할 목록 조회 시작");

        try {
            List<Role> customRoles = roleRepository.findByIsSystemRoleFalseAndIsActiveTrue();

            logger.info("✅ 사용자 정의 역할 목록 조회 성공");
            logger.info("   - 조회된 사용자 정의 역할 수: {}", customRoles.size());

            return customRoles;

        } catch (Exception e) {
            logger.error("❌ 사용자 정의 역할 목록 조회 실패", e);
            throw new RuntimeException("사용자 정의 역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 역할 ID로 상세 정보 조회
     *
     * @param id 역할 ID
     * @return 역할 정보 (없으면 null)
     */
    public Role getRoleById(Long id) {
        logger.info("🔍 역할 상세 조회 시작 - ID: {}", id);

        try {
            Optional<Role> roleOptional = roleRepository.findById(id);

            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();
                logger.info("✅ 역할 상세 조회 성공 - 역할명: {}", role.getRoleName());
                return role;
            } else {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                return null;
            }

        } catch (Exception e) {
            logger.error("❌ 역할 상세 조회 실패 - ID: {}", id, e);
            throw new RuntimeException("역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할명으로 역할 조회
     *
     * @param roleName 역할명
     * @return 역할 정보 (없으면 null)
     */
    public Role getRoleByName(String roleName) {
        logger.info("🔍 역할명으로 조회 시작 - 역할명: {}", roleName);

        try {
            Optional<Role> roleOptional = roleRepository.findByRoleName(roleName);

            if (roleOptional.isPresent()) {
                Role role = roleOptional.get();
                logger.info("✅ 역할명으로 조회 성공 - ID: {}", role.getRoleId());
                return role;
            } else {
                logger.warn("⚠️ 역할을 찾을 수 없음 - 역할명: {}", roleName);
                return null;
            }

        } catch (Exception e) {
            logger.error("❌ 역할명으로 조회 실패 - 역할명: {}", roleName, e);
            throw new RuntimeException("역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 검색 및 필터링 메서드
    // ================================

    /**
     * 역할 검색 (표시명 기준)
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 역할 목록
     */
    public List<Role> searchRoles(String keyword) {
        logger.info("🔍 역할 검색 시작 - 키워드: {}", keyword);

        try {
            List<Role> roles = roleRepository.findByDisplayNameContainingIgnoreCase(keyword);

            logger.info("✅ 역할 검색 성공");
            logger.info("   - 검색 결과 수: {}", roles.size());

            return roles;

        } catch (Exception e) {
            logger.error("❌ 역할 검색 실패 - 키워드: {}", keyword, e);
            throw new RuntimeException("역할 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 우선순위 범위로 역할 조회
     *
     * @param minPriority 최소 우선순위
     * @param maxPriority 최대 우선순위
     * @return 해당 우선순위 범위의 역할 목록
     */
    public List<Role> getRolesByPriorityRange(int minPriority, int maxPriority) {
        logger.info("🔍 우선순위 범위 조회 시작 - 범위: {} ~ {}", minPriority, maxPriority);

        try {
            List<Role> roles = roleRepository.findRolesByPriorityRange(
                    minPriority, maxPriority);

            logger.info("✅ 우선순위 범위 조회 성공");
            logger.info("   - 조회된 역할 수: {}", roles.size());

            return roles;

        } catch (Exception e) {
            logger.error("❌ 우선순위 범위 조회 실패", e);
            throw new RuntimeException("역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 생성/수정/삭제 메서드 (Write Operations)
    // ================================

    /**
     * 새로운 역할 생성
     *
     * [비즈니스 규칙]
     *
     * 1. 역할명 중복 검사
     *    - 같은 역할명이 이미 존재하면 생성 불가
     *
     * 2. 역할명 형식 검증
     *    - "ROLE_"로 시작해야 함
     *    - 대문자와 언더스코어만 사용 가능
     *
     * 3. 우선순위 검증
     *    - 1~999 범위 내여야 함
     *
     * @param role 생성할 역할 정보
     * @return 생성된 역할
     */
    @Transactional
    public Role createRole(Role role) {
        logger.info("➕ 새 역할 생성 시작 - 역할명: {}", role.getRoleName());

        try {
            // 1. 역할명 중복 검사
            if (roleRepository.existsByRoleName(role.getRoleName())) {
                logger.warn("⚠️ 역할명 중복 - 역할명: {}", role.getRoleName());
                throw new IllegalArgumentException("이미 존재하는 역할명입니다: " + role.getRoleName());
            }

            // 2. 역할명 형식 검증 (ROLE_로 시작)
            if (!role.getRoleName().startsWith("ROLE_")) {
                logger.warn("⚠️ 역할명 형식 오류 - 역할명: {}", role.getRoleName());
                throw new IllegalArgumentException("역할명은 'ROLE_'로 시작해야 합니다.");
            }

            // 3. 우선순위 검증
            if (role.getPriority() == null || role.getPriority() < 1 || role.getPriority() > 999) {
                logger.warn("⚠️ 우선순위 범위 오류 - 우선순위: {}", role.getPriority());
                throw new IllegalArgumentException("우선순위는 1~999 사이여야 합니다.");
            }

            // 4. 기본값 설정
            if (role.getIsSystemRole() == null) {
                role.setIsSystemRole(false);  // 사용자 정의 역할로 기본 설정
            }
            if (role.getIsActive() == null) {
                role.setIsActive(true);  // 활성 상태로 기본 설정
            }

            // 5. 저장
            Role savedRole = roleRepository.save(role);

            logger.info("✅ 새 역할 생성 성공 - ID: {}, 역할명: {}",
                    savedRole.getRoleId(), savedRole.getRoleName());

            return savedRole;

        } catch (IllegalArgumentException e) {
            logger.error("❌ 역할 생성 실패 (유효성 검증) - {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("❌ 역할 생성 실패 (시스템 오류) - 역할명: {}", role.getRoleName(), e);
            throw new RuntimeException("역할 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할 정보 수정
     *
     * [비즈니스 규칙]
     *
     * 1. 시스템 역할의 핵심 정보 수정 제한
     *    - roleName 변경 불가
     *    - isSystemRole 변경 불가
     *
     * 2. 수정 가능 항목
     *    - displayName (표시명)
     *    - description (설명)
     *    - priority (우선순위) - 주의 필요
     *
     * @param id 수정할 역할 ID
     * @param updatedRole 수정할 정보
     * @return 수정된 역할
     */
    @Transactional
    public Role updateRole(Long id, Role updatedRole) {
        logger.info("✏️ 역할 정보 수정 시작 - ID: {}", id);

        try {
            // 1. 역할 존재 확인
            Optional<Role> existingRoleOptional = roleRepository.findById(id);
            if (!existingRoleOptional.isPresent()) {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
            }

            Role existingRole = existingRoleOptional.get();

            // 2. 시스템 역할 수정 제한 검사
            if (existingRole.getIsSystemRole()) {
                // 시스템 역할의 roleName은 변경 불가
                if (!existingRole.getRoleName().equals(updatedRole.getRoleName())) {
                    logger.warn("⚠️ 시스템 역할 이름 변경 시도 차단 - 역할: {}", existingRole.getRoleName());
                    throw new IllegalArgumentException("시스템 역할의 역할명은 변경할 수 없습니다.");
                }
            }

            // 3. 수정 가능 필드 업데이트
            if (updatedRole.getDisplayName() != null) {
                existingRole.setDisplayName(updatedRole.getDisplayName());
            }
            if (updatedRole.getDescription() != null) {
                existingRole.setDescription(updatedRole.getDescription());
            }
            if (updatedRole.getPriority() != null) {
                if (updatedRole.getPriority() < 1 || updatedRole.getPriority() > 999) {
                    throw new IllegalArgumentException("우선순위는 1~999 사이여야 합니다.");
                }
                existingRole.setPriority(updatedRole.getPriority());
            }

            // 4. 저장
            Role savedRole = roleRepository.save(existingRole);

            logger.info("✅ 역할 정보 수정 성공 - ID: {}", id);

            return savedRole;

        } catch (IllegalArgumentException e) {
            logger.error("❌ 역할 수정 실패 (유효성 검증) - ID: {}, {}", id, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("❌ 역할 수정 실패 (시스템 오류) - ID: {}", id, e);
            throw new RuntimeException("역할 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할 삭제
     *
     * [비즈니스 규칙]
     *
     * 1. 시스템 역할 삭제 불가
     *    - ROLE_ADMIN, ROLE_MANAGER 등 삭제 방지
     *
     * 2. 사용자가 할당된 역할 삭제 불가
     *    - 먼저 사용자의 역할을 변경해야 함
     *
     * 3. 비활성화 권장
     *    - 실제 삭제보다 비활성화 권장
     *    - 감사(Audit) 추적 가능
     *
     * @param id 삭제할 역할 ID
     */
    @Transactional
    public void deleteRole(Long id) {
        logger.info("🗑️ 역할 삭제 시작 - ID: {}", id);

        try {
            // 1. 역할 존재 확인
            Optional<Role> roleOptional = roleRepository.findById(id);
            if (!roleOptional.isPresent()) {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
            }

            Role role = roleOptional.get();

            // 2. 시스템 역할 삭제 방지
            if (role.getIsSystemRole()) {
                logger.warn("⚠️ 시스템 역할 삭제 시도 차단 - 역할: {}", role.getRoleName());
                throw new IllegalArgumentException("시스템 역할은 삭제할 수 없습니다.");
            }

            // 3. 사용자 할당 여부 확인
            long userCount = roleRepository.countUsersByRoleId(id);
            if (userCount > 0) {
                logger.warn("⚠️ 사용자가 할당된 역할 삭제 시도 - 역할: {}, 사용자 수: {}",
                        role.getRoleName(), userCount);
                throw new IllegalArgumentException(
                        "이 역할에 할당된 사용자가 " + userCount + "명 있습니다. 먼저 사용자의 역할을 변경해주세요.");
            }

            // 4. 삭제 실행
            roleRepository.deleteById(id);

            logger.info("✅ 역할 삭제 성공 - ID: {}, 역할명: {}", id, role.getRoleName());

        } catch (IllegalArgumentException e) {
            logger.error("❌ 역할 삭제 실패 (비즈니스 규칙) - ID: {}, {}", id, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("❌ 역할 삭제 실패 (시스템 오류) - ID: {}", id, e);
            throw new RuntimeException("역할 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할 비활성화
     *
     * [비즈니스 규칙]
     *
     * - 시스템 역할도 비활성화 가능 (단, 권장하지 않음)
     * - 비활성화된 역할은 새로 할당할 수 없음
     * - 기존에 할당된 사용자는 유지됨
     *
     * @param id 비활성화할 역할 ID
     */
    @Transactional
    public void deactivateRole(Long id) {
        logger.info("🔒 역할 비활성화 시작 - ID: {}", id);

        try {
            // 1. 역할 존재 여부 확인
            Optional<Role> roleOptional = roleRepository.findById(id);

            if (!roleOptional.isPresent()) {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
            }

            Role role = roleOptional.get();

            // 2. 시스템 역할 비활성화 경고
            if (role.getIsSystemRole()) {
                logger.warn("⚠️ 시스템 역할 비활성화 - 역할: {} - 이 작업은 시스템에 영향을 줄 수 있습니다.",
                        role.getRoleName());
            }

            // 3. 이미 비활성화된 경우
            if (!role.getIsActive()) {
                logger.info("ℹ️ 이미 비활성화된 역할 - ID: {}", id);
                return;
            }

            // 4. 비활성화 실행
            int updatedRows = roleRepository.deactivateRole(id);

            if (updatedRows > 0) {
                logger.info("✅ 역할 비활성화 성공 - ID: {}, 역할명: {}", id, role.getRoleName());
            } else {
                logger.error("❌ 역할 비활성화 실패 - 업데이트된 행 수: {}", updatedRows);
                throw new RuntimeException("역할 비활성화에 실패했습니다.");
            }

        } catch (IllegalArgumentException e) {
            logger.error("❌ 역할 비활성화 실패 (비즈니스 규칙) - ID: {}, {}", id, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("❌ 역할 비활성화 실패 (시스템 오류) - ID: {}", id, e);
            throw new RuntimeException("역할 비활성화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할 활성화
     *
     * [비즈니스 규칙]
     *
     * - 비활성화된 역할을 다시 활성화
     * - 사용자에게 할당 가능한 상태로 복원
     *
     * @param id 활성화할 역할 ID
     */
    @Transactional
    public void activateRole(Long id) {
        logger.info("🔓 역할 활성화 시작 - ID: {}", id);

        try {
            // 1. 역할 존재 여부 확인
            Optional<Role> roleOptional = roleRepository.findById(id);

            if (!roleOptional.isPresent()) {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
            }

            Role role = roleOptional.get();

            // 2. 이미 활성화된 경우
            if (role.getIsActive()) {
                logger.info("ℹ️ 이미 활성화된 역할 - ID: {}", id);
                return;
            }

            // 3. 활성화 실행
            int updatedRows = roleRepository.activateRole(id);

            if (updatedRows > 0) {
                logger.info("✅ 역할 활성화 성공 - ID: {}, 역할명: {}", id, role.getRoleName());
            } else {
                logger.error("❌ 역할 활성화 실패 - 업데이트된 행 수: {}", updatedRows);
                throw new RuntimeException("역할 활성화에 실패했습니다.");
            }

        } catch (IllegalArgumentException e) {
            logger.error("❌ 역할 활성화 실패 (비즈니스 규칙) - ID: {}, {}", id, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("❌ 역할 활성화 실패 (시스템 오류) - ID: {}", id, e);
            throw new RuntimeException("역할 활성화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할 우선순위 업데이트
     *
     * [비즈니스 규칙]
     *
     * 1. 우선순위 범위 검증
     *    - 1~999 사이여야 함
     *    - 낮은 숫자 = 높은 권한
     *
     * 2. 시스템 역할 우선순위 변경 주의
     *    - 시스템 역할의 우선순위 변경은 가능하지만 권장하지 않음
     *    - 권한 체계가 변경될 수 있음
     *
     * @param id 역할 ID
     * @param newPriority 새로운 우선순위
     */
    @Transactional
    public void updateRolePriority(Long id, Integer newPriority) {
        logger.info("🔄 역할 우선순위 업데이트 시작 - ID: {}, 새 우선순위: {}", id, newPriority);

        try {
            // 1. 우선순위 범위 검증
            if (newPriority == null || newPriority < 1 || newPriority > 999) {
                logger.warn("⚠️ 우선순위 범위 오류 - 값: {}", newPriority);
                throw new IllegalArgumentException("우선순위는 1~999 사이여야 합니다.");
            }

            // 2. 역할 존재 확인
            Optional<Role> roleOptional = roleRepository.findById(id);
            if (!roleOptional.isPresent()) {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
            }

            Role role = roleOptional.get();

            // 3. 시스템 역할 우선순위 변경 경고
            if (role.getIsSystemRole()) {
                logger.warn("⚠️ 시스템 역할 우선순위 변경 - 역할명: {}, 이전: {}, 이후: {}",
                        role.getRoleName(), role.getPriority(), newPriority);
            }

            // 4. 우선순위 업데이트 실행
            int updatedRows = roleRepository.updateRolePriority(id, newPriority);

            if (updatedRows > 0) {
                logger.info("✅ 역할 우선순위 업데이트 성공 - ID: {}", id);
            } else {
                throw new RuntimeException("우선순위 업데이트에 실패했습니다.");
            }

        } catch (IllegalArgumentException e) {
            logger.error("❌ 우선순위 업데이트 실패 (유효성 검증) - {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("❌ 우선순위 업데이트 실패 (시스템 오류) - ID: {}", id, e);
            throw new RuntimeException("우선순위 업데이트 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 통계 및 분석 메서드
    // ================================

    /**
     * 역할 통계 정보 조회
     *
     * [제공 정보]
     *
     * - totalRoles: 전체 역할 수
     * - activeRoles: 활성 역할 수
     * - inactiveRoles: 비활성 역할 수
     * - systemRoles: 시스템 역할 수
     * - customRoles: 사용자 정의 역할 수
     * - roleUserStats: 역할별 사용자 수 통계
     * - emptyRoles: 사용자가 없는 역할 목록 (DTO로 변환)
     *
     * @return 역할 통계 정보 Map
     */
    public Map<String, Object> getRoleStatistics() {
        logger.info("📊 역할 통계 정보 조회 시작");

        try {
            // 1. 기본 통계 수집
            long totalRoles = roleRepository.count();
            long activeRoles = roleRepository.countByIsActiveTrue();
            long systemRoles = roleRepository.countByIsSystemRoleTrue();
            long customRoles = roleRepository.countByIsSystemRoleFalse();

            // 2. 역할별 사용자 수 통계
            List<Object[]> roleUserStats = roleRepository.getRoleUserStatistics();

            // 3. 사용자가 없는 역할 조회 (DTO로 변환하여 순환 참조 방지)
            List<Role> emptyRolesEntity = roleRepository.findRolesWithoutUsers();
            List<RoleDto> emptyRoles = emptyRolesEntity.stream()
                    .map(RoleDto::simpleFrom)
                    .collect(Collectors.toList());

            // 4. 통계 정보 구성
            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalRoles", totalRoles);
            statistics.put("activeRoles", activeRoles);
            statistics.put("inactiveRoles", totalRoles - activeRoles);
            statistics.put("systemRoles", systemRoles);
            statistics.put("customRoles", customRoles);
            statistics.put("roleUserStats", roleUserStats);
            statistics.put("emptyRolesCount", emptyRoles.size());
            statistics.put("emptyRoles", emptyRoles);

            logger.info("✅ 역할 통계 정보 조회 성공");
            logger.info("   - 전체: {}, 활성: {}, 시스템: {}, 사용자정의: {}",
                    totalRoles, activeRoles, systemRoles, customRoles);

            return statistics;

        } catch (Exception e) {
            logger.error("❌ 역할 통계 정보 조회 실패", e);
            throw new RuntimeException("통계 정보 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 역할을 가진 사용자 수 조회
     *
     * [비즈니스 규칙]
     *
     * - 해당 역할을 가진 사용자 수를 정확히 계산
     * - 역할이 존재하지 않으면 예외 발생
     *
     * @param id 역할 ID
     * @return 사용자 수 정보 Map
     */
    public Map<String, Object> getRoleUserCount(Long id) {
        logger.info("📊 역할별 사용자 수 조회 시작 - 역할 ID: {}", id);

        try {
            // 1. 역할 존재 확인
            Optional<Role> roleOptional = roleRepository.findById(id);

            if (!roleOptional.isPresent()) {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
            }

            Role role = roleOptional.get();

            // 2. 사용자 수 조회
            long userCount = roleRepository.countUsersByRoleId(id);

            // 3. 결과 구성
            Map<String, Object> result = new HashMap<>();
            result.put("roleId", id);
            result.put("roleName", role.getDisplayName());
            result.put("userCount", userCount);

            logger.info("✅ 역할별 사용자 수 조회 완료 - 역할: {}, 사용자 수: {}명",
                    role.getDisplayName(), userCount);

            return result;

        } catch (IllegalArgumentException e) {
            logger.error("❌ 사용자 수 조회 실패 (역할 없음) - ID: {}", id);
            throw e;

        } catch (Exception e) {
            logger.error("❌ 사용자 수 조회 실패 (시스템 오류) - ID: {}", id, e);
            throw new RuntimeException("사용자 수 조회 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 유틸리티 메서드
    // ================================

    /**
     * 역할명 중복 확인
     *
     * [비즈니스 규칙]
     *
     * - 역할명이 이미 존재하면 사용 불가
     * - 새 역할 생성 시 반드시 확인해야 함
     *
     * @param roleName 확인할 역할명
     * @return 중복 여부 정보 Map
     */
    public Map<String, Object> checkRoleNameDuplicate(String roleName) {
        logger.info("🔍 역할명 중복 확인 - 역할명: {}", roleName);

        try {
            boolean exists = roleRepository.existsByRoleName(roleName);

            Map<String, Object> result = new HashMap<>();
            result.put("roleName", roleName);
            result.put("exists", exists);
            result.put("available", !exists);

            logger.info("✅ 역할명 중복 확인 완료 - 역할명: {}, 사용가능: {}", roleName, !exists);

            return result;

        } catch (Exception e) {
            logger.error("❌ 역할명 중복 확인 실패 - 역할명: {}", roleName, e);
            throw new RuntimeException("역할명 확인 중 오류가 발생했습니다.", e);
        }
    }
}