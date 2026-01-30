package com.kmportal.backend.dto;

import com.kmportal.backend.entity.Role;

import java.time.LocalDateTime;

/**
 * Role 엔티티의 응답용 DTO (Data Transfer Object)
 *
 * [DTO를 사용하는 이유]
 *
 * 1. 순환 참조 방지
 *    - Role ↔ User 간 양방향 관계로 인한 JSON 직렬화 무한 루프 방지
 *    - users 필드 대신 userCount만 포함
 *
 * 2. API 응답 제어
 *    - 클라이언트에 필요한 데이터만 전송
 *    - 민감한 정보 노출 방지
 *
 * 3. 엔티티와 API 분리
 *    - 엔티티 변경이 API에 영향을 주지 않음
 *    - API 버전 관리 용이
 *
 * 4. 성능 최적화
 *    - 불필요한 연관 데이터 로딩 방지
 *    - 네트워크 전송량 감소
 *
 * [사용 예시]
 *
 * ```java
 * // Entity → DTO 변환
 * Role role = roleRepository.findById(1L).orElseThrow();
 * RoleDto dto = RoleDto.from(role);
 *
 * // 또는 사용자 수 포함
 * RoleDto dto = RoleDto.from(role, role.getUsers().size());
 * ```
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2026-01-29
 */
public class RoleDto {

    // ================================
    // 필드 정의
    // ================================

    /**
     * 역할 고유 ID
     */
    private Long roleId;

    /**
     * 역할명 (시스템 내부 사용)
     * 예: ROLE_ADMIN, ROLE_INVESTIGATOR_ALL
     */
    private String roleName;

    /**
     * 역할 표시명 (사용자에게 보여지는 한글명)
     * 예: 관리자, 조사자(1/4종)
     */
    private String displayName;

    /**
     * 역할 설명
     */
    private String description;

    /**
     * 권한 우선순위 (낮을수록 높은 권한)
     */
    private Integer priority;

    /**
     * 시스템 역할 여부
     * true: 시스템 기본 역할 (삭제 불가)
     * false: 사용자 정의 역할
     */
    private Boolean isSystemRole;

    /**
     * 활성화 상태
     * true: 사용자에게 할당 가능
     * false: 할당 불가
     */
    private Boolean isActive;

    /**
     * 역할을 가진 사용자 수
     * (users 컬렉션 대신 숫자만 표시하여 순환 참조 방지)
     */
    private Integer userCount;

    /**
     * 역할 생성 시간
     */
    private LocalDateTime createdAt;

    /**
     * 역할 최종 수정 시간
     */
    private LocalDateTime updatedAt;

    // ================================
    // 생성자
    // ================================

    /**
     * 기본 생성자
     * Jackson JSON 직렬화/역직렬화에 필요
     */
    public RoleDto() {}

    /**
     * 전체 필드 생성자
     *
     * @param roleId 역할 ID
     * @param roleName 역할명
     * @param displayName 표시명
     * @param description 설명
     * @param priority 우선순위
     * @param isSystemRole 시스템 역할 여부
     * @param isActive 활성화 여부
     * @param userCount 사용자 수
     * @param createdAt 생성 시간
     * @param updatedAt 수정 시간
     */
    public RoleDto(Long roleId, String roleName, String displayName, String description,
                   Integer priority, Boolean isSystemRole, Boolean isActive,
                   Integer userCount, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.roleId = roleId;
        this.roleName = roleName;
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
        this.isSystemRole = isSystemRole;
        this.isActive = isActive;
        this.userCount = userCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // ================================
    // 정적 팩토리 메서드 (Entity → DTO 변환)
    // ================================

    /**
     * Role 엔티티를 RoleDto로 변환
     *
     * [변환 규칙]
     * - 모든 기본 필드 복사
     * - users 컬렉션은 size()로 변환하여 userCount에 저장
     *
     * [사용 예시]
     * ```java
     * Role role = roleRepository.findById(1L).orElseThrow();
     * RoleDto dto = RoleDto.from(role);
     * ```
     *
     * @param role 변환할 Role 엔티티
     * @return 변환된 RoleDto (role이 null이면 null 반환)
     */
    public static RoleDto from(Role role) {
        // null 체크
        if (role == null) {
            return null;
        }

        // DTO 생성 및 필드 복사
        RoleDto dto = new RoleDto();
        dto.setRoleId(role.getRoleId());
        dto.setRoleName(role.getRoleName());
        dto.setDisplayName(role.getDisplayName());
        dto.setDescription(role.getDescription());
        dto.setPriority(role.getPriority());
        dto.setIsSystemRole(role.getIsSystemRole());
        dto.setIsActive(role.getIsActive());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());

        // users 컬렉션 → userCount 변환 (순환 참조 방지 핵심!)
        // Lazy Loading 주의: 트랜잭션 내에서 호출해야 함
        try {
            dto.setUserCount(role.getUsers() != null ? role.getUsers().size() : 0);
        } catch (Exception e) {
            // Lazy Loading 실패 시 0으로 설정
            dto.setUserCount(0);
        }

        return dto;
    }

    /**
     * Role 엔티티를 RoleDto로 변환 (사용자 수 직접 지정)
     *
     * [사용 시나리오]
     * - users 컬렉션을 로드하지 않고 별도 쿼리로 사용자 수를 구한 경우
     * - Lazy Loading 문제를 피하고 싶을 때
     *
     * [사용 예시]
     * ```java
     * Role role = roleRepository.findById(1L).orElseThrow();
     * long count = roleRepository.countUsersByRoleId(1L);
     * RoleDto dto = RoleDto.from(role, (int) count);
     * ```
     *
     * @param role 변환할 Role 엔티티
     * @param userCount 사용자 수 (별도로 계산된 값)
     * @return 변환된 RoleDto
     */
    public static RoleDto from(Role role, int userCount) {
        // null 체크
        if (role == null) {
            return null;
        }

        // DTO 생성 및 필드 복사
        RoleDto dto = new RoleDto();
        dto.setRoleId(role.getRoleId());
        dto.setRoleName(role.getRoleName());
        dto.setDisplayName(role.getDisplayName());
        dto.setDescription(role.getDescription());
        dto.setPriority(role.getPriority());
        dto.setIsSystemRole(role.getIsSystemRole());
        dto.setIsActive(role.getIsActive());
        dto.setCreatedAt(role.getCreatedAt());
        dto.setUpdatedAt(role.getUpdatedAt());

        // 직접 지정된 사용자 수 설정
        dto.setUserCount(userCount);

        return dto;
    }

    /**
     * 간단한 정보만 포함하는 DTO 생성
     * (목록 조회 시 성능 최적화용)
     *
     * [포함 필드]
     * - roleId, roleName, displayName, priority, isActive
     *
     * [제외 필드]
     * - description, userCount, createdAt, updatedAt
     *
     * @param role 변환할 Role 엔티티
     * @return 간단한 정보만 포함된 RoleDto
     */
    public static RoleDto simpleFrom(Role role) {
        if (role == null) {
            return null;
        }

        RoleDto dto = new RoleDto();
        dto.setRoleId(role.getRoleId());
        dto.setRoleName(role.getRoleName());
        dto.setDisplayName(role.getDisplayName());
        dto.setPriority(role.getPriority());
        dto.setIsSystemRole(role.getIsSystemRole());
        dto.setIsActive(role.getIsActive());

        return dto;
    }

    // ================================
    // Getter 및 Setter 메서드
    // ================================

    public Long getRoleId() {
        return roleId;
    }

    public void setRoleId(Long roleId) {
        this.roleId = roleId;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsSystemRole() {
        return isSystemRole;
    }

    public void setIsSystemRole(Boolean isSystemRole) {
        this.isSystemRole = isSystemRole;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Integer getUserCount() {
        return userCount;
    }

    public void setUserCount(Integer userCount) {
        this.userCount = userCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // ================================
    // Object 메서드 오버라이드
    // ================================

    @Override
    public String toString() {
        return "RoleDto{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", priority=" + priority +
                ", isSystemRole=" + isSystemRole +
                ", isActive=" + isActive +
                ", userCount=" + userCount +
                '}';
    }
}