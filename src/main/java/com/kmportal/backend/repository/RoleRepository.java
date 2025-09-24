package com.kmportal.backend.repository;

import com.kmportal.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 역할 데이터 액세스 Repository
 *
 * RBAC(Role-Based Access Control) 시스템의 역할 관리를 위한
 * 데이터 액세스 계층입니다.
 *
 * 주요 기능:
 * - 역할 CRUD 기본 기능
 * - 시스템 역할과 사용자 정의 역할 구분 관리
 * - 우선순위 기반 역할 조회
 * - 역할별 통계 정보 제공
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-09-24
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // ================================
    // 기본 조회 메서드
    // ================================

    /**
     * 역할명으로 역할 조회
     * Spring Security에서 권한 확인 시 주로 사용
     *
     * @param roleName 역할명 (예: "ROLE_ADMIN")
     * @return 역할 정보 (Optional로 감싼 결과)
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * 표시명으로 역할 조회
     * 관리 화면에서 사용자 친화적인 이름으로 조회
     *
     * @param displayName 표시명 (예: "관리자")
     * @return 역할 정보 (Optional로 감싼 결과)
     */
    Optional<Role> findByDisplayName(String displayName);

    /**
     * 역할명 존재 여부 확인
     * 새로운 역할 생성 시 중복 체크에 사용
     *
     * @param roleName 확인할 역할명
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByRoleName(String roleName);

    /**
     * 표시명 존재 여부 확인
     *
     * @param displayName 확인할 표시명
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByDisplayName(String displayName);

    // ================================
    // 상태별 조회 메서드
    // ================================

    /**
     * 활성화된 역할 목록 조회
     * 사용자에게 할당 가능한 역할들만 조회
     *
     * @return 활성 역할 목록
     */
    List<Role> findByIsActiveTrue();

    /**
     * 비활성화된 역할 목록 조회
     * 관리자가 비활성 역할을 관리할 때 사용
     *
     * @return 비활성 역할 목록
     */
    List<Role> findByIsActiveFalse();

    /**
     * 시스템 역할 목록 조회
     * 삭제/수정 불가능한 기본 시스템 역할들
     *
     * @return 시스템 역할 목록
     */
    List<Role> findByIsSystemRoleTrue();

    /**
     * 사용자 정의 역할 목록 조회
     * 관리자가 생성한 커스텀 역할들
     *
     * @return 사용자 정의 역할 목록
     */
    List<Role> findByIsSystemRoleFalse();

    /**
     * 활성화된 시스템 역할 조회
     * 시스템 초기화나 기본 권한 설정에 사용
     *
     * @return 활성화된 시스템 역할 목록
     */
    List<Role> findByIsSystemRoleTrueAndIsActiveTrue();

    /**
     * 활성화된 사용자 정의 역할 조회
     * 관리자가 사용자에게 할당 가능한 커스텀 역할들
     *
     * @return 활성화된 사용자 정의 역할 목록
     */
    List<Role> findByIsSystemRoleFalseAndIsActiveTrue();

    // ================================
    // 우선순위 기반 조회 메서드
    // ================================

    /**
     * 우선순위 오름차순으로 모든 활성 역할 조회
     * 권한 레벨 순으로 정렬 (낮은 숫자 = 높은 권한이 먼저)
     *
     * 메서드명 규칙: findBy + 조건 + OrderBy + 필드명 + 정렬방향
     * - OrderBy: 정렬 조건
     * - Priority: 정렬할 필드
     * - Asc: 오름차순 (Ascending)
     *
     * @return 우선순위 순으로 정렬된 활성 역할 목록
     */
    List<Role> findByIsActiveTrueOrderByPriorityAsc();

    /**
     * 특정 우선순위 이하의 역할들 조회
     * 현재 사용자의 권한 이하의 역할만 조회할 때 사용
     *
     * 메서드명 규칙: findBy + 필드명 + LessThanEqual
     * - LessThanEqual: <= 연산 (이하)
     *
     * @param priority 기준 우선순위
     * @return 해당 우선순위 이하의 역할 목록
     */
    List<Role> findByPriorityLessThanEqualAndIsActiveTrueOrderByPriorityAsc(Integer priority);

    /**
     * 특정 우선순위 이상의 역할들 조회
     * 현재 사용자보다 낮은 권한의 역할만 조회할 때 사용
     *
     * 메서드명 규칙: findBy + 필드명 + GreaterThanEqual
     * - GreaterThanEqual: >= 연산 (이상)
     *
     * @param priority 기준 우선순위
     * @return 해당 우선순위 이상의 역할 목록
     */
    List<Role> findByPriorityGreaterThanEqualAndIsActiveTrueOrderByPriorityAsc(Integer priority);

    /**
     * 최고 권한 역할 조회
     * 우선순위가 가장 낮은(권한이 가장 높은) 역할
     *
     * @return 최고 권한 역할
     */
    Optional<Role> findTopByIsActiveTrueOrderByPriorityAsc();

    // ================================
    // 검색 메서드 (Like 조건 사용)
    // ================================

    /**
     * 표시명으로 역할 검색 (부분 일치)
     * 관리 화면에서 역할 검색 시 사용
     *
     * @param displayName 검색할 표시명 (부분 일치)
     * @return 표시명에 해당 문자가 포함된 역할 목록
     */
    List<Role> findByDisplayNameContainingIgnoreCase(String displayName);

    /**
     * 설명으로 역할 검색 (부분 일치)
     * 역할의 기능을 설명으로 찾을 때 사용
     *
     * @param description 검색할 설명 (부분 일치)
     * @return 설명에 해당 문자가 포함된 역할 목록
     */
    List<Role> findByDescriptionContainingIgnoreCase(String description);

    // ================================
    // 커스텀 쿼리 메서드 (@Query 사용)
    // ================================

    /**
     * 특정 사용자가 가진 역할들 조회
     * 사용자의 권한을 확인할 때 사용
     *
     * @param userId 사용자 ID
     * @return 해당 사용자가 가진 역할 목록
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.userId = :userId")
    List<Role> findByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자가 가진 활성 역할들 조회
     * 실제 권한 확인 시 사용 (비활성 역할 제외)
     *
     * @param userId 사용자 ID
     * @return 해당 사용자가 가진 활성 역할 목록
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.userId = :userId AND r.isActive = true " +
            "ORDER BY r.priority ASC")
    List<Role> findActiveRolesByUserId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 최고 권한 역할 조회
     * 사용자의 최고 권한을 확인할 때 사용
     *
     * @param userId 사용자 ID
     * @return 해당 사용자의 최고 권한 역할 (우선순위가 가장 낮은 것)
     */
    @Query("SELECT r FROM Role r JOIN r.users u WHERE u.userId = :userId AND r.isActive = true " +
            "ORDER BY r.priority ASC LIMIT 1")
    Optional<Role> findHighestPriorityRoleByUserId(@Param("userId") Long userId);

    /**
     * 사용자 수가 많은 역할들 조회 (인기 역할)
     * 시스템 통계나 분석에 사용
     *
     * @return 사용자 수가 많은 순으로 정렬된 역할 목록
     */
    @Query("SELECT r, COUNT(u) as userCount FROM Role r LEFT JOIN r.users u " +
            "WHERE r.isActive = true GROUP BY r ORDER BY userCount DESC")
    List<Object[]> findRolesWithUserCount();

    /**
     * 사용자가 없는 역할들 조회
     * 불필요한 역할 정리나 관리에 사용
     *
     * @return 사용자가 할당되지 않은 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.users IS EMPTY AND r.isSystemRole = false")
    List<Role> findRolesWithoutUsers();

    /**
     * 특정 우선순위 범위의 역할들 조회
     * 권한 레벨별 역할 관리에 사용
     *
     * @param minPriority 최소 우선순위
     * @param maxPriority 최대 우선순위
     * @return 해당 우선순위 범위의 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.priority BETWEEN :minPriority AND :maxPriority " +
            "AND r.isActive = true ORDER BY r.priority ASC")
    List<Role> findRolesByPriorityRange(@Param("minPriority") Integer minPriority,
                                        @Param("maxPriority") Integer maxPriority);

    // ================================
    // 업데이트 메서드 (@Modifying 사용)
    // ================================

    /**
     * 역할 비활성화
     * 역할 삭제 대신 비활성화 처리 (소프트 삭제)
     *
     * @param roleId 역할 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Role r SET r.isActive = false WHERE r.roleId = :roleId")
    int deactivateRole(@Param("roleId") Long roleId);

    /**
     * 역할 활성화
     *
     * @param roleId 역할 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Role r SET r.isActive = true WHERE r.roleId = :roleId")
    int activateRole(@Param("roleId") Long roleId);

    /**
     * 역할 우선순위 업데이트
     * 권한 레벨 조정에 사용
     *
     * @param roleId 역할 ID
     * @param priority 새로운 우선순위
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Role r SET r.priority = :priority WHERE r.roleId = :roleId")
    int updateRolePriority(@Param("roleId") Long roleId, @Param("priority") Integer priority);

    /**
     * 역할 설명 업데이트
     * 역할의 책임이나 권한이 변경될 때 사용
     *
     * @param roleId 역할 ID
     * @param description 새로운 설명
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE Role r SET r.description = :description WHERE r.roleId = :roleId")
    int updateRoleDescription(@Param("roleId") Long roleId, @Param("description") String description);

    // ================================
    // 통계 및 집계 메서드
    // ================================

    /**
     * 활성 역할 수 조회
     *
     * @return 활성 역할 수
     */
    long countByIsActiveTrue();

    /**
     * 시스템 역할 수 조회
     *
     * @return 시스템 역할 수
     */
    long countByIsSystemRoleTrue();

    /**
     * 사용자 정의 역할 수 조회
     *
     * @return 사용자 정의 역할 수
     */
    long countByIsSystemRoleFalse();

    /**
     * 특정 우선순위 이하 활성 역할 수 조회
     *
     * @param priority 기준 우선순위
     * @return 해당 우선순위 이하의 활성 역할 수
     */
    long countByPriorityLessThanEqualAndIsActiveTrue(Integer priority);

    /**
     * 특정 역할을 가진 사용자 수 조회
     * 역할별 사용자 통계에 사용
     *
     * @param roleId 역할 ID
     * @return 해당 역할을 가진 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.roleId = :roleId")
    long countUsersByRoleId(@Param("roleId") Long roleId);

    /**
     * 각 역할별 사용자 수 통계
     * 관리 대시보드나 통계 화면에서 사용
     *
     * @return 역할별 사용자 수 (역할명, 사용자 수)
     */
    @Query("SELECT r.displayName, COUNT(u) FROM Role r LEFT JOIN r.users u " +
            "WHERE r.isActive = true GROUP BY r.roleId, r.displayName " +
            "ORDER BY COUNT(u) DESC")
    List<Object[]> getRoleUserStatistics();
}