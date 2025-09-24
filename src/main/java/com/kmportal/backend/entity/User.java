package com.kmportal.backend.entity;

import com.kmportal.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * 사용자 엔티티
 * - KM 포털 시스템의 사용자 정보를 저장하는 테이블
 * - Spring Security와 연동하여 인증/인가에 사용
 * - 약 400명의 사용자를 관리할 예정
 *
 * 테이블명: users (user는 일부 DB에서 예약어이므로 복수형 사용)
 *
 * @author KM Portal Team
 * @since 2025-09-23 (3일차)
 */
@Entity                             // JPA 엔티티 선언
@Table(name = "users")              // 테이블명 지정
@Getter @Setter                     // Lombok: getter/setter 자동 생성
@NoArgsConstructor                  // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor                 // Lombok: 모든 필드 생성자 자동 생성
@Builder                            // Lombok: 빌더 패턴 지원
@ToString(exclude = {"password", "roles"})  // Lombok: toString (비밀번호와 역할은 제외)
public class User extends BaseEntity {

    /**
     * 사용자 ID (Primary Key)
     * - 자동 증가되는 고유 식별자
     * - Long 타입 사용으로 대용량 사용자 지원
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto Increment 사용
    @Column(name = "user_id")
    private Long userId;

    /**
     * 사용자명 (로그인 ID)
     * - 로그인시 사용되는 고유한 사용자명
     * - 영문, 숫자, 언더스코어만 허용 예정
     * - 중복 불허, 필수 입력
     */
    @Column(name = "username", unique = true, nullable = false, length = 50)
    private String username;

    /**
     * 비밀번호
     * - Spring Security의 PasswordEncoder로 암호화되어 저장
     * - BCrypt 알고리즘 사용 예정
     * - 평문으로 저장하지 않음
     */
    @Column(name = "password", nullable = false, length = 100)
    private String password;

    /**
     * 이메일 주소
     * - 사용자 연락 및 인증용
     * - 중복 불허, 필수 입력
     * - 이메일 형식 검증 필요
     */
    @Column(name = "email", unique = true, nullable = false, length = 100)
    private String email;

    /**
     * 사용자 실명
     * - 시스템 내 표시용 이름
     * - 한글, 영문 모두 지원
     */
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    /**
     * 부서명
     * - 사용자가 소속된 부서
     * - 권한 관리 및 필터링에 사용
     */
    @Column(name = "department", length = 100)
    private String department;

    /**
     * 직급/직책
     * - 사용자의 직급 정보
     * - 권한 레벨 결정에 참고
     */
    @Column(name = "position", length = 50)
    private String position;

    /**
     * 전화번호
     * - 연락처 정보
     * - 선택 입력
     */
    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    /**
     * 계정 활성화 상태
     * - true: 활성화된 사용자 (로그인 가능)
     * - false: 비활성화된 사용자 (로그인 불가)
     * - 관리자가 사용자 계정을 일시적으로 중지할 때 사용
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default  // 빌더 패턴에서 기본값 설정
    private Boolean isActive = true;

    /**
     * 계정 잠금 상태
     * - true: 계정 잠금 (로그인 불가)
     * - false: 정상 상태
     * - 로그인 실패 횟수 초과시 자동 잠금 예정
     */
    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    /**
     * 계정 만료 여부
     * - true: 계정 만료
     * - false: 정상 상태
     * - 임시 계정이나 기간 제한 계정에 사용
     */
    @Column(name = "is_expired", nullable = false)
    @Builder.Default
    private Boolean isExpired = false;

    /**
     * 마지막 로그인 시간
     * - 사용자의 마지막 로그인 일시 기록
     * - 활동 통계 및 휴면 계정 관리에 사용
     */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /**
     * 로그인 실패 횟수
     * - 연속된 로그인 실패 횟수 기록
     * - 일정 횟수 초과시 계정 잠금
     * - 성공적인 로그인시 0으로 리셋
     */
    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    /**
     * 사용자 역할 (다대다 관계)
     * - 한 사용자는 여러 역할을 가질 수 있음
     * - 예: 일반사용자 + 게시판관리자
     * - LAZY 로딩으로 성능 최적화
     */
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(
            name = "user_roles",                    // 연결 테이블명
            joinColumns = @JoinColumn(name = "user_id"),        // 현재 엔티티 컬럼
            inverseJoinColumns = @JoinColumn(name = "role_id")  // 대상 엔티티 컬럼
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    /**
     * 비즈니스 메서드: 역할 추가
     * - 사용자에게 새로운 역할을 추가
     * - 중복 추가 방지
     *
     * @param role 추가할 역할
     */
    public void addRole(Role role) {
        if (role != null) {
            this.roles.add(role);
            role.getUsers().add(this);  // 양방향 관계 동기화
        }
    }

    /**
     * 비즈니스 메서드: 역할 제거
     * - 사용자에게서 특정 역할을 제거
     *
     * @param role 제거할 역할
     */
    public void removeRole(Role role) {
        if (role != null) {
            this.roles.remove(role);
            role.getUsers().remove(this);  // 양방향 관계 동기화
        }
    }

    /**
     * 비즈니스 메서드: 특정 역할 보유 여부 확인
     * - 사용자가 특정 역할을 가지고 있는지 확인
     *
     * @param roleName 확인할 역할명
     * @return 역할 보유 여부
     */
    public boolean hasRole(String roleName) {
        return this.roles.stream()
                .anyMatch(role -> role.getRoleName().equals(roleName));
    }

    /**
     * 비즈니스 메서드: 계정 잠금
     * - 사용자 계정을 잠금 상태로 변경
     */
    public void lockAccount() {
        this.isLocked = true;
    }

    /**
     * 비즈니스 메서드: 계정 잠금 해제
     * - 사용자 계정의 잠금을 해제하고 실패 횟수 초기화
     */
    public void unlockAccount() {
        this.isLocked = false;
        this.failedLoginAttempts = 0;
    }

    /**
     * 비즈니스 메서드: 로그인 실패 기록
     * - 로그인 실패시 호출하여 실패 횟수 증가
     * - 5회 실패시 자동으로 계정 잠금
     */
    public void recordLoginFailure() {
        this.failedLoginAttempts++;
        if (this.failedLoginAttempts >= 5) {
            this.lockAccount();
        }
    }

    /**
     * 비즈니스 메서드: 로그인 성공 기록
     * - 성공적인 로그인시 호출
     * - 실패 횟수 초기화 및 마지막 로그인 시간 업데이트
     */
    public void recordLoginSuccess() {
        this.failedLoginAttempts = 0;
        this.lastLoginAt = LocalDateTime.now();
    }

    /**
     * 비즈니스 메서드: 계정 사용 가능 여부 확인
     * - Spring Security에서 사용
     *
     * @return 계정 사용 가능 여부
     */
    public boolean isAccountNonExpired() {
        return !this.isExpired;
    }

    /**
     * 비즈니스 메서드: 계정 잠금 상태 확인
     * - Spring Security에서 사용
     *
     * @return 계정 잠금 여부 (true: 잠금되지 않음)
     */
    public boolean isAccountNonLocked() {
        return !this.isLocked;
    }

    /**
     * 비즈니스 메서드: 계정 활성화 상태 확인
     * - Spring Security에서 사용
     *
     * @return 계정 활성화 여부
     */
    public boolean isEnabled() {
        return this.isActive;
    }
}