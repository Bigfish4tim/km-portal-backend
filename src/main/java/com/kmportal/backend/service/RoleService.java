package com.kmportal.backend.service;

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
 * - 데이터 변환 및 가공
 *
 * @author KM Portal Dev Team
 * @version 1.0
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

        logger.info("✅ RoleService 초기화 완료");
        logger.debug("   - RoleRepository: {}", roleRepository.getClass().getSimpleName());
    }

    // ================================
    // 조회 메서드 (Read Operations)
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
     */
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
     * [비즈니스 규칙]
     *
     * - 시스템 역할만 조회 (isSystemRole = true)
     * - 활성 역할만 조회 (isActive = true)
     * - ROLE_ADMIN, ROLE_MANAGER, ROLE_USER 등
     *
     * @return 시스템 역할 목록
     */
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
     * [비즈니스 규칙]
     *
     * - 사용자 정의 역할만 조회 (isSystemRole = false)
     * - 활성 역할만 조회 (isActive = true)
     * - 관리자가 생성한 커스텀 역할들
     *
     * @return 사용자 정의 역할 목록
     */
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
     * [비즈니스 규칙]
     *
     * - ID에 해당하는 역할이 없으면 null 반환
     * - Optional을 사용하여 안전한 null 처리
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
     * [비즈니스 규칙]
     *
     * - 역할명 (예: "ROLE_ADMIN")으로 조회
     * - 대소문자 구분 (정확히 일치해야 함)
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
     * [비즈니스 규칙]
     *
     * - 표시명(displayName)에서 키워드 검색
     * - 부분 일치 검색 (LIKE '%keyword%')
     * - 대소문자 구분 안함
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 역할 목록
     */
    public List<Role> searchRolesByDisplayName(String keyword) {
        logger.info("🔍 역할 검색 시작 - 키워드: {}", keyword);

        try {
            List<Role> searchResults = roleRepository
                    .findByDisplayNameContainingIgnoreCase(keyword);

            logger.info("✅ 역할 검색 완료 - 결과 수: {}개", searchResults.size());

            return searchResults;

        } catch (Exception e) {
            logger.error("❌ 역할 검색 실패 - 키워드: {}", keyword, e);
            throw new RuntimeException("역할 검색 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 우선순위 범위로 역할 조회
     *
     * [비즈니스 규칙]
     *
     * - 최소 우선순위 ~ 최대 우선순위 범위 내의 역할 조회
     * - 활성 역할만 조회
     * - 예: priority BETWEEN 1 AND 50
     *
     * @param minPriority 최소 우선순위
     * @param maxPriority 최대 우선순위
     * @return 해당 우선순위 범위의 역할 목록
     */
    public List<Role> getRolesByPriorityRange(Integer minPriority, Integer maxPriority) {
        logger.info("🔍 우선순위 범위로 역할 조회 시작 - 범위: {} ~ {}", minPriority, maxPriority);

        try {
            List<Role> roles = roleRepository.findRolesByPriorityRange(minPriority, maxPriority);

            logger.info("✅ 우선순위 범위 조회 완료 - 결과 수: {}개", roles.size());

            return roles;

        } catch (Exception e) {
            logger.error("❌ 우선순위 범위 조회 실패", e);
            throw new RuntimeException("역할 조회 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 생성 및 수정 메서드 (Write Operations)
    // ================================

    /**
     * 새로운 역할 생성
     *
     * [비즈니스 규칙]
     *
     * 1. 역할명 중복 확인 (필수)
     *    - 역할명이 이미 존재하면 예외 발생
     *
     * 2. 역할명 형식 검증
     *    - "ROLE_"로 시작해야 함 (Spring Security 규칙)
     *    - 대문자와 언더스코어만 사용
     *
     * 3. 우선순위 검증
     *    - 1~999 범위 내여야 함
     *
     * 4. 기본값 설정
     *    - isActive = true (생성 시 활성 상태)
     *    - isSystemRole = false (사용자 정의 역할)
     *
     * @param role 생성할 역할 정보
     * @return 저장된 역할 정보
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
    @Transactional  // 쓰기 작업은 트랜잭션 필요
    public Role createRole(Role role) {
        logger.info("➕ 역할 생성 시작");
        logger.debug("   - 역할명: {}", role.getRoleName());
        logger.debug("   - 표시명: {}", role.getDisplayName());
        logger.debug("   - 우선순위: {}", role.getPriority());

        try {
            // 1. 역할명 중복 확인 (비즈니스 규칙)
            if (roleRepository.existsByRoleName(role.getRoleName())) {
                logger.warn("⚠️ 역할명 중복 - 역할명: {}", role.getRoleName());
                throw new IllegalArgumentException("이미 존재하는 역할명입니다: " + role.getRoleName());
            }

            // 2. 역할명 형식 검증 (비즈니스 규칙)
            if (!role.getRoleName().startsWith("ROLE_")) {
                logger.warn("⚠️ 역할명 형식 오류 - 역할명: {}", role.getRoleName());
                throw new IllegalArgumentException("역할명은 ROLE_로 시작해야 합니다.");
            }

            // 3. 우선순위 검증 (비즈니스 규칙)
            if (role.getPriority() == null || role.getPriority() < 1 || role.getPriority() > 999) {
                logger.warn("⚠️ 우선순위 범위 오류 - 우선순위: {}", role.getPriority());
                throw new IllegalArgumentException("우선순위는 1~999 사이여야 합니다.");
            }

            // 4. 기본값 설정
            if (role.getIsActive() == null) {
                role.setIsActive(true);
            }
            if (role.getIsSystemRole() == null) {
                role.setIsSystemRole(false);  // 사용자 정의 역할
            }

            // 5. 역할 저장
            Role savedRole = roleRepository.save(role);

            logger.info("✅ 역할 생성 성공");
            logger.info("   - 역할 ID: {}", savedRole.getRoleId());
            logger.info("   - 역할명: {}", savedRole.getRoleName());

            return savedRole;

        } catch (IllegalArgumentException e) {
            // 비즈니스 규칙 위반 (클라이언트 오류)
            logger.error("❌ 역할 생성 실패 (유효성 검증) - {}", e.getMessage());
            throw e;

        } catch (Exception e) {
            // 예상치 못한 오류 (서버 오류)
            logger.error("❌ 역할 생성 실패 (시스템 오류)", e);
            throw new RuntimeException("역할 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 역할 정보 수정
     *
     * [비즈니스 규칙]
     *
     * 1. 시스템 역할 수정 제한
     *    - 시스템 역할(isSystemRole=true)은 수정 불가
     *    - ROLE_ADMIN, ROLE_MANAGER 등 보호
     *
     * 2. 수정 가능 필드
     *    - displayName (표시명)
     *    - description (설명)
     *    - priority (우선순위)
     *
     * 3. 수정 불가 필드
     *    - roleName (역할명) - 변경하면 권한 체계 붕괴
     *    - isSystemRole (시스템 역할 여부) - 보안상 변경 불가
     *
     * @param id 수정할 역할 ID
     * @param updateInfo 수정할 정보
     * @return 수정된 역할 정보
     * @throws IllegalArgumentException 유효성 검증 실패 시
     */
    @Transactional
    public Role updateRole(Long id, Role updateInfo) {
        logger.info("✏️ 역할 수정 시작 - ID: {}", id);

        try {
            // 1. 기존 역할 조회
            Optional<Role> existingRoleOptional = roleRepository.findById(id);

            if (!existingRoleOptional.isPresent()) {
                logger.warn("⚠️ 역할을 찾을 수 없음 - ID: {}", id);
                throw new IllegalArgumentException("역할을 찾을 수 없습니다.");
            }

            Role existingRole = existingRoleOptional.get();

            // 2. 시스템 역할 수정 방지 (비즈니스 규칙)
            if (existingRole.getIsSystemRole()) {
                logger.warn("⚠️ 시스템 역할 수정 시도 - 역할명: {}", existingRole.getRoleName());
                throw new IllegalArgumentException("시스템 역할은 수정할 수 없습니다.");
            }

            // 3. 수정 가능한 필드만 업데이트
            if (updateInfo.getDisplayName() != null) {
                existingRole.setDisplayName(updateInfo.getDisplayName());
            }
            if (updateInfo.getDescription() != null) {
                existingRole.setDescription(updateInfo.getDescription());
            }
            if (updateInfo.getPriority() != null) {
                // 우선순위 범위 검증
                if (updateInfo.getPriority() < 1 || updateInfo.getPriority() > 999) {
                    throw new IllegalArgumentException("우선순위는 1~999 사이여야 합니다.");
                }
                existingRole.setPriority(updateInfo.getPriority());
            }

            // 4. 저장 (JPA가 자동으로 UPDATE 쿼리 실행)
            Role updatedRole = roleRepository.save(existingRole);

            logger.info("✅ 역할 수정 성공 - ID: {}", id);

            return updatedRole;

        } catch (IllegalArgumentException e) {
            logger.error("❌ 역할 수정 실패 (유효성 검증) - ID: {}, {}", id, e.getMessage());
            throw e;

        } catch (Exception e) {
            logger.error("❌ 역할 수정 실패 (시스템 오류) - ID: {}", id, e);
            throw new RuntimeException("역할 수정 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 활성화/비활성화 메서드
    // ================================

    /**
     * 역할 비활성화 (소프트 삭제)
     *
     * [비즈니스 규칙]
     *
     * 1. 시스템 역할 비활성화 금지
     *    - 시스템 역할은 비활성화 불가
     *    - 시스템 안정성 보장
     *
     * 2. 소프트 삭제 방식
     *    - 실제로 삭제하지 않고 isActive = false로 설정
     *    - 기존 사용자의 역할 정보는 유지
     *    - 새로운 사용자에게는 할당 불가
     *
     * 3. 연관된 사용자 확인
     *    - 해당 역할을 가진 사용자가 있어도 비활성화 가능
     *    - 단, 경고 로그 출력
     *
     * @param id 비활성화할 역할 ID
     * @throws IllegalArgumentException 비즈니스 규칙 위반 시
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

            // 2. 시스템 역할 비활성화 방지 (비즈니스 규칙)
            if (role.getIsSystemRole()) {
                logger.warn("⚠️ 시스템 역할 비활성화 시도 - 역할명: {}", role.getRoleName());
                throw new IllegalArgumentException("시스템 역할은 비활성화할 수 없습니다.");
            }

            // 3. 이미 비활성화된 경우
            if (!role.getIsActive()) {
                logger.info("ℹ️ 이미 비활성화된 역할 - ID: {}", id);
                return;  // 중복 처리 방지
            }

            // 4. 연관된 사용자 수 확인 (경고 로그)
            long userCount = roleRepository.countUsersByRoleId(id);
            if (userCount > 0) {
                logger.warn("⚠️ 사용자가 할당된 역할을 비활성화합니다 - 사용자 수: {}명", userCount);
            }

            // 5. 비활성화 실행
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
     * - emptyRoles: 사용자가 없는 역할 목록
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

            // 3. 사용자가 없는 역할 조회
            List<Role> emptyRoles = roleRepository.findRolesWithoutUsers();

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