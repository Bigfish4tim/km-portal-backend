package com.kmportal.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * KM 포털 역할(Role) 엔티티
 *
 * 이 클래스는 RBAC(Role-Based Access Control) 시스템의 핵심으로,
 * 사용자의 권한을 역할 단위로 관리합니다.
 *
 * 역할 시스템:
 * - ROLE_ADMIN: 시스템 전체 관리자 (우선순위: 1)
 * - ROLE_MANAGER: 부서 관리자 (우선순위: 10)
 * - ROLE_BOARD_ADMIN: 게시판 관리자 (우선순위: 20)
 * - ROLE_USER: 일반 사용자 (우선순위: 100)
 *
 * 우선순위가 낮을수록(숫자가 작을수록) 높은 권한을 의미합니다.
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-09-24
 */
@Entity
@Table(name = "roles",
        indexes = {
                @Index(name = "idx_role_name", columnList = "role_name", unique = true),
                @Index(name = "idx_role_priority", columnList = "priority"),
                @Index(name = "idx_role_system", columnList = "is_system_role")
        })
public class Role {

    /**
     * 역할 고유 ID (Primary Key)
     * GenerationType.IDENTITY: 데이터베이스의 AUTO_INCREMENT 기능 사용
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "role_id")
    private Long roleId;

    /**
     * 역할명 (시스템 내부 사용)
     * Spring Security에서 사용하는 권한명
     * 반드시 "ROLE_" 접두사로 시작해야 함 (Spring Security 규칙)
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    @NotBlank(message = "역할명은 필수 입력 항목입니다.")
    @Pattern(regexp = "^ROLE_[A-Z][A-Z_]*$",
            message = "역할명은 ROLE_로 시작하고 대문자와 언더스코어만 사용할 수 있습니다.")
    private String roleName;

    /**
     * 역할 표시명 (사용자에게 보여지는 한글명)
     * 관리 화면에서 사용자에게 표시되는 이름
     */
    @Column(name = "display_name", nullable = false, length = 100)
    @NotBlank(message = "역할 표시명은 필수 입력 항목입니다.")
    @Size(max = 100, message = "역할 표시명은 100자를 초과할 수 없습니다.")
    private String displayName;

    /**
     * 역할 설명
     * 해당 역할의 권한과 책임에 대한 상세 설명
     */
    @Column(name = "description", length = 500)
    @Size(max = 500, message = "역할 설명은 500자를 초과할 수 없습니다.")
    private String description;

    /**
     * 권한 우선순위
     * 숫자가 낮을수록 높은 권한 (1이 최고 권한)
     * 권한 충돌 시 우선순위로 판단
     */
    @Column(name = "priority", nullable = false)
    @NotNull(message = "우선순위는 필수 입력 항목입니다.")
    @Min(value = 1, message = "우선순위는 1 이상이어야 합니다.")
    @Max(value = 999, message = "우선순위는 999 이하여야 합니다.")
    private Integer priority;

    /**
     * 시스템 역할 여부
     * true: 시스템 기본 역할 (삭제/수정 불가)
     * false: 사용자 정의 역할 (삭제/수정 가능)
     */
    @Column(name = "is_system_role", nullable = false)
    private Boolean isSystemRole = false;

    /**
     * 활성화 상태
     * true: 활성 역할 (할당 가능)
     * false: 비활성 역할 (할당 불가)
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 역할 생성 시간 (자동 설정)
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 역할 최종 수정 시간 (자동 설정)
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 역할을 가진 사용자들과의 다대다 관계
     * mappedBy = "roles": User 엔티티의 roles 필드에 의해 관리됨
     *
     * 주의: toString() 메서드에서 이 필드를 사용하면 순환참조 발생
     * 따라서 toString()에서는 제외해야 함
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    private Set<User> users = new HashSet<>();

    // ================================
    // 기본 생성자 및 편의 생성자
    // ================================

    /**
     * 기본 생성자 (JPA 필수)
     */
    public Role() {}

    /**
     * 시스템 기본 역할 생성자
     * 시스템 초기화 시 기본 역할들을 생성할 때 사용
     *
     * @param roleName 역할명 (ROLE_ADMIN 등)
     * @param displayName 표시명 (관리자 등)
     * @param description 역할 설명
     * @param priority 우선순위
     */
    public Role(String roleName, String displayName, String description, Integer priority) {
        this.roleName = roleName;
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
        this.isSystemRole = true;  // 시스템 기본 역할로 설정
        this.isActive = true;
    }

    /**
     * 사용자 정의 역할 생성자
     * 관리자가 새로운 역할을 생성할 때 사용
     *
     * @param roleName 역할명
     * @param displayName 표시명
     * @param description 역할 설명
     * @param priority 우선순위
     * @param isSystemRole 시스템 역할 여부
     */
    public Role(String roleName, String displayName, String description,
                Integer priority, Boolean isSystemRole) {
        this.roleName = roleName;
        this.displayName = displayName;
        this.description = description;
        this.priority = priority;
        this.isSystemRole = isSystemRole;
        this.isActive = true;
    }

    // ================================
    // 비즈니스 메서드
    // ================================

    /**
     * 시스템 기본 역할인지 확인
     *
     * @return 시스템 역할이면 true, 사용자 정의 역할이면 false
     */
    public boolean isSystemRole() {
        return this.isSystemRole != null && this.isSystemRole;
    }

    /**
     * 역할이 활성화되어 있는지 확인
     *
     * @return 활성화되어 있으면 true
     */
    public boolean isActive() {
        return this.isActive != null && this.isActive;
    }

    /**
     * 다른 역할보다 높은 권한을 가지는지 확인
     * 우선순위 숫자가 낮을수록 높은 권한
     *
     * @param otherRole 비교할 역할
     * @return 현재 역할이 더 높은 권한이면 true
     */
    public boolean hasHigherPriorityThan(Role otherRole) {
        if (otherRole == null || otherRole.getPriority() == null) {
            return true;
        }
        return this.priority != null && this.priority < otherRole.getPriority();
    }

    /**
     * 특정 역할명인지 확인
     *
     * @param roleName 확인할 역할명
     * @return 일치하면 true
     */
    public boolean hasRoleName(String roleName) {
        return this.roleName != null && this.roleName.equals(roleName);
    }

    /**
     * 관리자급 권한인지 확인
     * ADMIN이나 MANAGER 역할인지 체크
     *
     * @return 관리자급이면 true
     */
    public boolean isAdminLevel() {
        return hasRoleName("ROLE_ADMIN") || hasRoleName("ROLE_MANAGER");
    }

    /**
     * 역할에 사용자 추가
     * 양방향 연관관계 동기화
     *
     * @param user 추가할 사용자
     */
    public void addUser(User user) {
        this.users.add(user);
        user.getRoles().add(this);
    }

    /**
     * 역할에서 사용자 제거
     * 양방향 연관관계 동기화
     *
     * @param user 제거할 사용자
     */
    public void removeUser(User user) {
        this.users.remove(user);
        user.getRoles().remove(this);
    }

    /**
     * 역할을 가진 사용자 수 반환
     *
     * @return 이 역할을 가진 사용자 수
     */
    public int getUserCount() {
        return this.users.size();
    }

    /**
     * 역할 비활성화
     * 비활성화된 역할은 새로 할당할 수 없음
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 역할 활성화
     */
    public void activate() {
        this.isActive = true;
    }

    // ================================
    // 정적 팩토리 메서드 (시스템 기본 역할 생성)
    // ================================

    /**
     * 시스템 관리자 역할 생성
     */
    public static Role createAdminRole() {
        return new Role(
                "ROLE_ADMIN",
                "시스템 관리자",
                "시스템 전체에 대한 모든 권한을 가진 최고 관리자",
                1
        );
    }

    /**
     * 부서 관리자 역할 생성
     */
    public static Role createManagerRole() {
        return new Role(
                "ROLE_MANAGER",
                "부서 관리자",
                "부서 내 사용자 및 콘텐츠 관리 권한을 가진 관리자",
                10
        );
    }

    /**
     * 게시판 관리자 역할 생성
     */
    public static Role createBoardAdminRole() {
        return new Role(
                "ROLE_BOARD_ADMIN",
                "게시판 관리자",
                "게시판 콘텐츠 관리 및 모니터링 권한을 가진 관리자",
                20
        );
    }

    /**
     * 일반 사용자 역할 생성
     */
    public static Role createUserRole() {
        return new Role(
                "ROLE_USER",
                "일반 사용자",
                "기본적인 시스템 이용 권한을 가진 일반 사용자",
                100
        );
    }

    // ================================
    // Getter 및 Setter 메서드
    // ================================

    public Long getRoleId() { return roleId; }
    public void setRoleId(Long roleId) { this.roleId = roleId; }

    public String getRoleName() { return roleName; }
    public void setRoleName(String roleName) { this.roleName = roleName; }

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }

    public Boolean getIsSystemRole() { return isSystemRole; }
    public void setIsSystemRole(Boolean isSystemRole) { this.isSystemRole = isSystemRole; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<User> getUsers() { return users; }
    public void setUsers(Set<User> users) { this.users = users; }

    // ================================
    // Object 클래스 메서드 오버라이드
    // ================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Role role = (Role) o;
        return Objects.equals(roleId, role.roleId) &&
                Objects.equals(roleName, role.roleName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(roleId, roleName);
    }

    /**
     * toString 메서드
     *
     * 주의: users 필드는 순환참조를 방지하기 위해 제외
     * 대신 사용자 수만 표시
     */
    @Override
    public String toString() {
        return "Role{" +
                "roleId=" + roleId +
                ", roleName='" + roleName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", priority=" + priority +
                ", isSystemRole=" + isSystemRole +
                ", isActive=" + isActive +
                ", userCount=" + (users != null ? users.size() : 0) +
                '}';
    }
}