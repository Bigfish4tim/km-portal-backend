
// ==============================================
// 📁 RoleRepository.java  
// 역할 데이터 액세스 레이어
// ==============================================

package com.kmportal.backend.repository;

import com.kmportal.backend.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 역할 레포지토리
 * - Spring Data JPA를 활용한 역할 데이터 액세스
 * - RBAC 시스템의 핵심 데이터 관리
 *
 * @author KM Portal Team
 * @since 2025-09-23 (3일차)
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // ========================================
    // 기본 조회 메서드
    // ========================================

    /**
     * 역할명으로 역할 조회
     * - Spring Security에서 권한 확인 시 사용
     * - 예: "ROLE_ADMIN" 으로 관리자 역할 조회
     *
     * @param roleName 역할명
     * @return 역할 정보
     */
    Optional<Role> findByRoleName(String roleName);

    /**
     * 표시명으로 역할 조회
     * - 관리자 화면에서 친숙한 이름으로 역할 찾기
     *
     * @param displayName 표시명 (예: "시스템 관리자")
     * @return 역할 정보
     */
    Optional<Role> findByDisplayName(String displayName);

    // ========================================
    // 상태별 조회 메서드
    // ========================================

    /**
     * 활성화된 역할 목록 조회
     * - 사용자에게 할당 가능한 역할만 조회
     * - 우선순위 순으로 정렬
     *
     * @return 활성화된 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.isActive = true ORDER BY r.priority ASC")
    List<Role> findActiveRolesOrderByPriority();

    /**
     * 시스템 역할 목록 조회
     * - 삭제할 수 없는 기본 역할들 조회
     *
     * @return 시스템 역할 목록
     */
    List<Role> findByIsSystemRoleTrue();

    /**
     * 커스텀 역할 목록 조회
     * - 관리자가 생성한 삭제 가능한 역할들
     *
     * @param pageable 페이징 정보
     * @return 커스텀 역할 페이지
     */
    Page<Role> findByIsSystemRoleFalse(Pageable pageable);

    // ========================================
    // 검색 메서드
    // ========================================

    /**
     * 역할명 또는 표시명으로 검색
     * - 관리자 화면에서 역할 검색 기능
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 역할 페이지
     */
    @Query("SELECT r FROM Role r WHERE " +
            "LOWER(r.roleName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(r.displayName) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Role> searchRoles(@Param("keyword") String keyword, Pageable pageable);

    // ========================================
    // 우선순위 관련 메서드
    // ========================================

    /**
     * 우선순위 범위 내 역할 조회
     * - 특정 권한 레벨 이상의 역할만 조회
     *
     * @param minPriority 최소 우선순위 (낮은 숫자 = 높은 우선순위)
     * @param maxPriority 최대 우선순위
     * @return 해당 범위의 역할 목록
     */
    @Query("SELECT r FROM Role r WHERE r.priority BETWEEN :minPriority AND :maxPriority ORDER BY r.priority ASC")
    List<Role> findByPriorityBetween(@Param("minPriority") Integer minPriority,
                                     @Param("maxPriority") Integer maxPriority);

    /**
     * 가장 높은 우선순위 조회
     * - 새로운 역할의 우선순위 결정에 사용
     *
     * @return 가장 낮은 우선순위 값 (= 가장 높은 권한)
     */
    @Query("SELECT MIN(r.priority) FROM Role r")
    Optional<Integer> findMinPriority();

    /**
     * 가장 낮은 우선순위 조회
     * - 새로운 역할의 기본 우선순위 결정
     *
     * @return 가장 높은 우선순위 값 (= 가장 낮은 권한)
     */
    @Query("SELECT MAX(r.priority) FROM Role r")
    Optional<Integer> findMaxPriority();

    // ========================================
    // 통계 쿼리 메서드
    // ========================================

    /**
     * 활성화된 역할 수 조회
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
     * 특정 역할을 가진 사용자 수 조회
     * - 역할별 사용자 통계
     *
     * @param roleId 역할 ID
     * @return 해당 역할을 가진 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.roleId = :roleId")
    long countUsersByRoleId(@Param("roleId") Long roleId);

    // ========================================
    // 중복 확인 메서드
    // ========================================

    /**
     * 역할명 중복 확인
     * - 새로운 역할 생성 시 중복 검사
     *
     * @param roleName 확인할 역할명
     * @return 중복 여부
     */
    boolean existsByRoleName(String roleName);

    /**
     * 표시명 중복 확인
     *
     * @param displayName 확인할 표시명
     * @return 중복 여부
     */
    boolean existsByDisplayName(String displayName);

    /**
     * 역할 업데이트 시 중복 확인 (자신 제외)
     *
     * @param roleName 확인할 역할명
     * @param roleId 제외할 역할 ID
     * @return 중복 여부
     */
    boolean existsByRoleNameAndRoleIdNot(String roleName, Long roleId);
}