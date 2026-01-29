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
 * KM 손해사정 포털 역할(Role) 엔티티
 *
 * ============================================
 * 📌 1일차 수정 (2025-01-21)
 * ============================================
 * - 기존 4개 Role → 12개 Role로 확장
 * - 손해사정 업무에 맞는 새로운 Role 구조 적용
 * - 1종/4종 손해사정사 업무 구분 지원
 * ============================================
 *
 * 이 클래스는 RBAC(Role-Based Access Control) 시스템의 핵심으로,
 * 사용자의 권한을 역할 단위로 관리합니다.
 *
 * [신규 역할 시스템 - 12개]
 *
 * 관리 역할:
 * - ROLE_ADMIN: 관리자 (우선순위: 1) - 시스템 전체 관리
 * - ROLE_BUSINESS_SUPPORT: 경영지원 (우선순위: 5) - 접수/배당/전송 담당
 *
 * 임원 역할:
 * - ROLE_EXECUTIVE_ALL: 임원(1/4종) (우선순위: 10) - 1종+4종 전체 업무
 * - ROLE_EXECUTIVE_TYPE1: 임원(1종) (우선순위: 11) - 1종 전체 업무
 * - ROLE_EXECUTIVE_TYPE4: 임원(4종) (우선순위: 12) - 4종 전체 업무
 *
 * 팀장 역할:
 * - ROLE_TEAM_LEADER_ALL: 팀장(1/4종) (우선순위: 20) - 자기 팀 1종+4종 업무
 * - ROLE_TEAM_LEADER_TYPE1: 팀장(1종) (우선순위: 21) - 자기 팀 1종 업무
 * - ROLE_TEAM_LEADER_TYPE4: 팀장(4종) (우선순위: 22) - 자기 팀 4종 업무
 *
 * 조사자 역할:
 * - ROLE_INVESTIGATOR_ALL: 조사자(1/4종) (우선순위: 30) - 자기 배당 1종+4종 업무
 * - ROLE_INVESTIGATOR_TYPE1: 조사자(1종) (우선순위: 31) - 자기 배당 1종 업무
 * - ROLE_INVESTIGATOR_TYPE4: 조사자(4종) (우선순위: 32) - 자기 배당 4종 업무
 *
 * 일반 역할:
 * - ROLE_EMPLOYEE: 일반사원 (우선순위: 100) - 권한 없음 (분류용)
 *
 * 우선순위가 낮을수록(숫자가 작을수록) 높은 권한을 의미합니다.
 *
 * @author KM Portal Dev Team
 * @version 2.0 (1일차 수정)
 * @since 2025-01-21
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
     *
     * 제약사항 (ROL-CON-001, ROL-CON-002):
     * - 반드시 ROLE_ 접두사로 시작
     * - 대문자와 언더스코어만 사용 가능
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    @NotBlank(message = "역할명은 필수 입력 항목입니다.")
    @Pattern(regexp = "^ROLE_[A-Z][A-Z_0-9]*$",
            message = "역할명은 ROLE_로 시작하고 대문자, 숫자, 언더스코어만 사용할 수 있습니다.")
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
     *
     * 제약사항 (ROL-CON-003): 1~999 범위 내 정수
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
     *
     * 제약사항 (ROL-CON-004): 모든 Role은 isSystemRole = true로 설정
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
     * ADMIN이나 BUSINESS_SUPPORT 역할인지 체크
     *
     * [1일차 수정] 기존 MANAGER → BUSINESS_SUPPORT로 변경
     *
     * @return 관리자급이면 true
     */
    public boolean isAdminLevel() {
        return hasRoleName("ROLE_ADMIN") || hasRoleName("ROLE_BUSINESS_SUPPORT");
    }

    /**
     * 1종 업무 접근 가능 여부 확인
     *
     * [1일차 신규] 1종 손해사정 업무 접근 권한 체크
     *
     * @return 1종 업무 접근 가능하면 true
     */
    public boolean canAccessType1() {
        // 관리자, 경영지원은 모든 업무 접근 가능
        if (hasRoleName("ROLE_ADMIN") || hasRoleName("ROLE_BUSINESS_SUPPORT")) {
            return true;
        }
        // 1/4종 통합 역할은 1종 접근 가능
        if (hasRoleName("ROLE_EXECUTIVE_ALL") ||
                hasRoleName("ROLE_TEAM_LEADER_ALL") ||
                hasRoleName("ROLE_INVESTIGATOR_ALL")) {
            return true;
        }
        // 1종 전용 역할은 1종 접근 가능
        if (hasRoleName("ROLE_EXECUTIVE_TYPE1") ||
                hasRoleName("ROLE_TEAM_LEADER_TYPE1") ||
                hasRoleName("ROLE_INVESTIGATOR_TYPE1")) {
            return true;
        }
        return false;
    }

    /**
     * 4종 업무 접근 가능 여부 확인
     *
     * [1일차 신규] 4종 손해사정 업무 접근 권한 체크
     *
     * @return 4종 업무 접근 가능하면 true
     */
    public boolean canAccessType4() {
        // 관리자, 경영지원은 모든 업무 접근 가능
        if (hasRoleName("ROLE_ADMIN") || hasRoleName("ROLE_BUSINESS_SUPPORT")) {
            return true;
        }
        // 1/4종 통합 역할은 4종 접근 가능
        if (hasRoleName("ROLE_EXECUTIVE_ALL") ||
                hasRoleName("ROLE_TEAM_LEADER_ALL") ||
                hasRoleName("ROLE_INVESTIGATOR_ALL")) {
            return true;
        }
        // 4종 전용 역할은 4종 접근 가능
        if (hasRoleName("ROLE_EXECUTIVE_TYPE4") ||
                hasRoleName("ROLE_TEAM_LEADER_TYPE4") ||
                hasRoleName("ROLE_INVESTIGATOR_TYPE4")) {
            return true;
        }
        return false;
    }

    /**
     * 임원급 권한인지 확인
     *
     * [1일차 신규] 임원 이상 권한 체크 (검토 권한 등에 사용)
     *
     * @return 임원급 이상이면 true
     */
    public boolean isExecutiveLevel() {
        return hasRoleName("ROLE_ADMIN") ||
                hasRoleName("ROLE_EXECUTIVE_ALL") ||
                hasRoleName("ROLE_EXECUTIVE_TYPE1") ||
                hasRoleName("ROLE_EXECUTIVE_TYPE4");
    }

    /**
     * 팀장급 권한인지 확인
     *
     * [1일차 신규] 팀장 이상 권한 체크 (조사/보고서/검토 권한 등에 사용)
     *
     * @return 팀장급 이상이면 true
     */
    public boolean isTeamLeaderLevel() {
        return isExecutiveLevel() ||
                hasRoleName("ROLE_BUSINESS_SUPPORT") ||
                hasRoleName("ROLE_TEAM_LEADER_ALL") ||
                hasRoleName("ROLE_TEAM_LEADER_TYPE1") ||
                hasRoleName("ROLE_TEAM_LEADER_TYPE4");
    }

    /**
     * 조사자급 권한인지 확인
     *
     * [1일차 신규] 조사자 이상 권한 체크 (조사/보고서 작성 권한 등에 사용)
     *
     * @return 조사자급 이상이면 true
     */
    public boolean isInvestigatorLevel() {
        return isTeamLeaderLevel() ||
                hasRoleName("ROLE_INVESTIGATOR_ALL") ||
                hasRoleName("ROLE_INVESTIGATOR_TYPE1") ||
                hasRoleName("ROLE_INVESTIGATOR_TYPE4");
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
    // [1일차 수정] 기존 4개 → 12개로 확장
    // ================================

    /**
     * [1] 관리자 역할 생성
     *
     * 시스템 전체에 대한 모든 권한을 가진 최고 관리자
     * - 우선순위: 1 (최고 권한)
     * - 1종/4종: 모두 접근 가능
     * - 업무 권한: 모든 업무
     */
    public static Role createAdminRole() {
        return new Role(
                "ROLE_ADMIN",
                "관리자",
                "시스템 전체에 대한 모든 권한을 가진 최고 관리자",
                1
        );
    }

    /**
     * [2] 경영지원 역할 생성
     *
     * [1일차 신규] 접수/배당/전송 업무를 담당하는 경영지원 역할
     * - 우선순위: 5
     * - 1종/4종: 모두 접근 가능
     * - 업무 권한: 접수, 배당, 전송
     */
    public static Role createBusinessSupportRole() {
        return new Role(
                "ROLE_BUSINESS_SUPPORT",
                "경영지원",
                "보험사 의뢰 접수, 조사자 배당, 보고서 전송 업무를 담당하는 경영지원 역할",
                5
        );
    }

    /**
     * [3] 임원(1/4종) 역할 생성
     *
     * [1일차 신규] 1종과 4종 모든 업무를 총괄하는 임원 역할
     * - 우선순위: 10
     * - 1종/4종: 모두 접근 가능
     * - 업무 권한: 검토
     */
    public static Role createExecutiveAllRole() {
        return new Role(
                "ROLE_EXECUTIVE_ALL",
                "임원(1/4종)",
                "1종과 4종 모든 손해사정 업무를 총괄하고 보고서를 검토하는 임원",
                10
        );
    }

    /**
     * [4] 임원(1종) 역할 생성
     *
     * [1일차 신규] 1종 업무만 담당하는 임원 역할
     * - 우선순위: 11
     * - 1종: 접근 가능 / 4종: 접근 불가
     * - 업무 권한: 검토 (1종만)
     */
    public static Role createExecutiveType1Role() {
        return new Role(
                "ROLE_EXECUTIVE_TYPE1",
                "임원(1종)",
                "1종 손해사정 업무를 총괄하고 보고서를 검토하는 임원",
                11
        );
    }

    /**
     * [5] 임원(4종) 역할 생성
     *
     * [1일차 신규] 4종 업무만 담당하는 임원 역할
     * - 우선순위: 12
     * - 1종: 접근 불가 / 4종: 접근 가능
     * - 업무 권한: 검토 (4종만)
     */
    public static Role createExecutiveType4Role() {
        return new Role(
                "ROLE_EXECUTIVE_TYPE4",
                "임원(4종)",
                "4종 손해사정 업무를 총괄하고 보고서를 검토하는 임원",
                12
        );
    }

    /**
     * [6] 팀장(1/4종) 역할 생성
     *
     * [1일차 신규] 1종과 4종 모든 업무를 처리하는 팀장 역할
     * - 우선순위: 20
     * - 1종/4종: 모두 접근 가능
     * - 업무 권한: 조사, 보고서, 검토 (자기 팀)
     */
    public static Role createTeamLeaderAllRole() {
        return new Role(
                "ROLE_TEAM_LEADER_ALL",
                "팀장(1/4종)",
                "1종과 4종 손해사정 업무를 수행하고 팀원 보고서를 검토하는 팀장",
                20
        );
    }

    /**
     * [7] 팀장(1종) 역할 생성
     *
     * [1일차 신규] 1종 업무만 담당하는 팀장 역할
     * - 우선순위: 21
     * - 1종: 접근 가능 / 4종: 접근 불가
     * - 업무 권한: 조사, 보고서, 검토 (자기 팀 1종만)
     */
    public static Role createTeamLeaderType1Role() {
        return new Role(
                "ROLE_TEAM_LEADER_TYPE1",
                "팀장(1종)",
                "1종 손해사정 업무를 수행하고 팀원 보고서를 검토하는 팀장",
                21
        );
    }

    /**
     * [8] 팀장(4종) 역할 생성
     *
     * [1일차 신규] 4종 업무만 담당하는 팀장 역할
     * - 우선순위: 22
     * - 1종: 접근 불가 / 4종: 접근 가능
     * - 업무 권한: 조사, 보고서, 검토 (자기 팀 4종만)
     */
    public static Role createTeamLeaderType4Role() {
        return new Role(
                "ROLE_TEAM_LEADER_TYPE4",
                "팀장(4종)",
                "4종 손해사정 업무를 수행하고 팀원 보고서를 검토하는 팀장",
                22
        );
    }

    /**
     * [9] 조사자(1/4종) 역할 생성
     *
     * [1일차 신규] 1종과 4종 모든 조사 업무를 수행하는 조사자 역할
     * - 우선순위: 30
     * - 1종/4종: 모두 접근 가능
     * - 업무 권한: 조사, 보고서 (자기 배당 건)
     */
    public static Role createInvestigatorAllRole() {
        return new Role(
                "ROLE_INVESTIGATOR_ALL",
                "조사자(1/4종)",
                "1종과 4종 손해사정 현장 조사 및 보고서 작성을 담당하는 조사자",
                30
        );
    }

    /**
     * [10] 조사자(1종) 역할 생성
     *
     * [1일차 신규] 1종 조사 업무만 수행하는 조사자 역할
     * - 우선순위: 31
     * - 1종: 접근 가능 / 4종: 접근 불가
     * - 업무 권한: 조사, 보고서 (자기 배당 1종 건)
     */
    public static Role createInvestigatorType1Role() {
        return new Role(
                "ROLE_INVESTIGATOR_TYPE1",
                "조사자(1종)",
                "1종 손해사정 현장 조사 및 보고서 작성을 담당하는 조사자",
                31
        );
    }

    /**
     * [11] 조사자(4종) 역할 생성
     *
     * [1일차 신규] 4종 조사 업무만 수행하는 조사자 역할
     * - 우선순위: 32
     * - 1종: 접근 불가 / 4종: 접근 가능
     * - 업무 권한: 조사, 보고서 (자기 배당 4종 건)
     */
    public static Role createInvestigatorType4Role() {
        return new Role(
                "ROLE_INVESTIGATOR_TYPE4",
                "조사자(4종)",
                "4종 손해사정 현장 조사 및 보고서 작성을 담당하는 조사자",
                32
        );
    }

    /**
     * [12] 일반사원 역할 생성
     *
     * [1일차 신규] 특별한 업무 권한이 없는 일반사원 역할
     * - 우선순위: 100 (최저 권한)
     * - 1종/4종: 접근 불가
     * - 업무 권한: 없음 (분류용)
     */
    public static Role createEmployeeRole() {
        return new Role(
                "ROLE_EMPLOYEE",
                "일반사원",
                "업무 권한이 아직 부여되지 않은 일반 사원 (분류 및 대기용)",
                100
        );
    }

    // ================================
    // [1일차 삭제] 기존 팩토리 메서드 제거
    // ================================
    // - createManagerRole() → createBusinessSupportRole()로 대체
    // - createBoardAdminRole() → 삭제 (손해사정 업무에 불필요)
    // - createUserRole() → createEmployeeRole()로 대체
    // ================================

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