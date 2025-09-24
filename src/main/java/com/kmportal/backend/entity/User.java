package com.kmportal.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * KM 포털 사용자 엔티티
 *
 * 이 클래스는 시스템의 모든 사용자 정보를 담고 있으며,
 * Spring Security의 UserDetails 인터페이스를 구현하여
 * 인증 및 권한 처리에 직접 사용할 수 있습니다.
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-09-24
 */
@Entity
@Table(name = "users", // 테이블명을 명시적으로 지정 (user는 예약어인 경우가 많음)
        indexes = {
                @Index(name = "idx_user_username", columnList = "username", unique = true),
                @Index(name = "idx_user_email", columnList = "email", unique = true),
                @Index(name = "idx_user_department", columnList = "department"),
                @Index(name = "idx_user_active", columnList = "is_active")
        })
public class User implements UserDetails {

    /**
     * 사용자 고유 ID (Primary Key)
     * GenerationType.IDENTITY: 데이터베이스의 AUTO_INCREMENT 기능 사용
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long userId;

    /**
     * 로그인 아이디 (필수, 유일값)
     * 3-30자의 영문, 숫자, 언더스코어만 허용
     */
    @Column(name = "username", nullable = false, unique = true, length = 30)
    @NotBlank(message = "사용자명은 필수 입력 항목입니다.")
    @Size(min = 3, max = 30, message = "사용자명은 3-30자 사이여야 합니다.")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
            message = "사용자명은 영문, 숫자, 언더스코어만 사용할 수 있습니다.")
    private String username;

    /**
     * 암호화된 비밀번호
     * BCrypt 등으로 암호화하여 저장
     */
    @Column(name = "password", nullable = false, length = 255)
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    private String password;

    /**
     * 이메일 주소 (필수, 유일값)
     */
    @Column(name = "email", nullable = false, unique = true, length = 100)
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    /**
     * 사용자 실명 (한글명)
     */
    @Column(name = "full_name", nullable = false, length = 50)
    @NotBlank(message = "성명은 필수 입력 항목입니다.")
    @Size(max = 50, message = "성명은 50자를 초과할 수 없습니다.")
    private String fullName;

    /**
     * 소속 부서명
     */
    @Column(name = "department", length = 100)
    @Size(max = 100, message = "부서명은 100자를 초과할 수 없습니다.")
    private String department;

    /**
     * 직급/직책
     */
    @Column(name = "position", length = 50)
    @Size(max = 50, message = "직책은 50자를 초과할 수 없습니다.")
    private String position;

    /**
     * 연락처 (휴대폰 번호)
     */
    @Column(name = "phone_number", length = 20)
    @Pattern(regexp = "^[0-9\\-+()\\s]*$",
            message = "연락처는 숫자, 하이픈, 괄호, 플러스 기호만 사용할 수 있습니다.")
    private String phoneNumber;

    /**
     * 계정 활성화 상태
     * true: 활성 계정, false: 비활성 계정
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 계정 잠금 상태
     * true: 잠금된 계정, false: 정상 계정
     * 보안 정책에 의해 일시적으로 접근을 차단할 때 사용
     */
    @Column(name = "is_locked", nullable = false)
    private Boolean isLocked = false;

    /**
     * 비밀번호 만료 여부
     * true: 비밀번호 변경 필요, false: 정상
     */
    @Column(name = "password_expired", nullable = false)
    private Boolean passwordExpired = false;

    /**
     * 마지막 로그인 시간
     * 사용자 활동 추적 및 보안 모니터링에 활용
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 로그인 실패 횟수
     * 보안 정책에 따른 계정 잠금 처리에 사용
     */
    @Column(name = "failed_login_attempts", nullable = false)
    private Integer failedLoginAttempts = 0;

    /**
     * 계정 생성 시간 (자동 설정)
     * Hibernate가 엔티티 저장 시 자동으로 현재 시간 설정
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 계정 최종 수정 시간 (자동 설정)
     * Hibernate가 엔티티 수정 시 자동으로 현재 시간 갱신
     */
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 사용자-역할 다대다 관계 매핑
     *
     * fetch = FetchType.EAGER: 사용자 조회 시 역할 정보도 함께 조회
     * cascade = CascadeType.MERGE: 사용자 수정 시 역할 연관관계도 함께 처리
     *
     * 중간 테이블(user_roles) 설정:
     * - joinColumns: 현재 엔티티(User)의 외래키
     * - inverseJoinColumns: 상대 엔티티(Role)의 외래키
     */
    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    // ================================
    // 기본 생성자 및 편의 생성자
    // ================================

    /**
     * 기본 생성자 (JPA 필수)
     */
    public User() {}

    /**
     * 필수 필드 생성자
     * 사용자 생성 시 반드시 필요한 정보들을 매개변수로 받습니다.
     *
     * @param username 로그인 아이디
     * @param password 암호화된 비밀번호
     * @param email 이메일 주소
     * @param fullName 사용자 실명
     */
    public User(String username, String password, String email, String fullName) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.fullName = fullName;
        this.isActive = true;
        this.isLocked = false;
        this.passwordExpired = false;
        this.failedLoginAttempts = 0;
    }

    // ================================
    // 비즈니스 메서드
    // ================================

    /**
     * 사용자에게 역할 추가
     *
     * @param role 추가할 역할
     */
    public void addRole(Role role) {
        this.roles.add(role);
        role.getUsers().add(this);
    }

    /**
     * 사용자에게서 역할 제거
     *
     * @param role 제거할 역할
     */
    public void removeRole(Role role) {
        this.roles.remove(role);
        role.getUsers().remove(this);
    }

    /**
     * 특정 역할 보유 여부 확인
     *
     * @param roleName 역할명 (예: "ROLE_ADMIN")
     * @return 역할 보유 시 true, 아니면 false
     */
    public boolean hasRole(String roleName) {
        return this.roles.stream()
                .anyMatch(role -> role.getRoleName().equals(roleName));
    }

    /**
     * 최고 권한 역할 확인
     * 우선순위가 가장 낮은 숫자(=가장 높은 권한)를 가진 역할을 반환
     *
     * @return 최고 권한 역할, 역할이 없으면 null
     */
    public Role getHighestPriorityRole() {
        return this.roles.stream()
                .min(Comparator.comparing(Role::getPriority))
                .orElse(null);
    }

    /**
     * 로그인 성공 처리
     * 마지막 로그인 시간 갱신 및 실패 횟수 초기화
     */
    public void onLoginSuccess() {
        this.lastLoginAt = LocalDateTime.now();
        this.failedLoginAttempts = 0;
    }

    /**
     * 로그인 실패 처리
     * 실패 횟수 증가 및 정책에 따른 계정 잠금 처리
     *
     * @param maxAttempts 최대 허용 실패 횟수
     */
    public void onLoginFailure(int maxAttempts) {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= maxAttempts) {
            this.isLocked = true;
        }
    }

    /**
     * 계정 잠금 해제
     */
    public void unlock() {
        this.isLocked = false;
        this.failedLoginAttempts = 0;
    }

    // ================================
    // UserDetails 인터페이스 구현
    // Spring Security 인증에 필요한 메서드들
    // ================================

    /**
     * 사용자 권한 목록 반환
     * Spring Security에서 권한 검증에 사용
     *
     * @return 권한 목록
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
                .collect(Collectors.toList());
    }

    /**
     * 계정 만료 여부
     *
     * @return 만료되지 않았으면 true
     */
    @Override
    public boolean isAccountNonExpired() {
        return true; // 현재는 계정 만료 정책이 없음
    }

    /**
     * 계정 잠금 여부
     *
     * @return 잠금되지 않았으면 true
     */
    @Override
    public boolean isAccountNonLocked() {
        return !this.isLocked;
    }

    /**
     * 비밀번호 만료 여부
     *
     * @return 만료되지 않았으면 true
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return !this.passwordExpired;
    }

    /**
     * 계정 활성화 여부
     *
     * @return 활성화되어 있으면 true
     */
    @Override
    public boolean isEnabled() {
        return this.isActive;
    }

    // ================================
    // Getter 및 Setter 메서드
    // ================================

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }

    public Boolean getIsLocked() { return isLocked; }
    public void setIsLocked(Boolean isLocked) { this.isLocked = isLocked; }

    public Boolean getPasswordExpired() { return passwordExpired; }
    public void setPasswordExpired(Boolean passwordExpired) { this.passwordExpired = passwordExpired; }

    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
    public void setLastLoginAt(LocalDateTime lastLoginAt) { this.lastLoginAt = lastLoginAt; }

    public Integer getFailedLoginAttempts() { return failedLoginAttempts; }
    public void setFailedLoginAttempts(Integer failedLoginAttempts) { this.failedLoginAttempts = failedLoginAttempts; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<Role> getRoles() { return roles; }
    public void setRoles(Set<Role> roles) { this.roles = roles; }

    // ================================
    // Object 클래스 메서드 오버라이드
    // ================================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return Objects.equals(userId, user.userId) &&
                Objects.equals(username, user.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, username);
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", fullName='" + fullName + '\'' +
                ", department='" + department + '\'' +
                ", isActive=" + isActive +
                ", isLocked=" + isLocked +
                '}';
    }
}