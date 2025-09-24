package com.kmportal.backend.entity;

import com.kmportal.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

/**
 * 역할 엔티티
 * - KM 포털 시스템의 사용자 역할(권한)을 정의하는 테이블
 * - RBAC (Role-Based Access Control) 시스템의 핵심
 * - 사용자와 다대다 관계로 연결됨
 *
 * 예상 역할:
 * - ROLE_ADMIN: 시스템 관리자
 * - ROLE_USER: 일반 사용자
 * - ROLE_MANAGER: 부서 관리자
 * - ROLE_BOARD_ADMIN: 게시판 관리자
 *
 * @author KM Portal Team
 * @since 2025-09-23 (3일차)
 */
@Entity                             // JPA 엔티티 선언
@Table(name = "roles")              // 테이블명 지정
@Getter @Setter                     // Lombok: getter/setter 자동 생성
@NoArgsConstructor                  // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor                 // Lombok: 모든 필드 생성자 자동 생성
@Builder                            // Lombok: 빌더 패턴 지원
@ToString(exclude = "users")        // Lombok: toString (사용자 목록은 제외 - 순환참조 방지)
public class Role extends BaseEntity {

    /**
     * 역할 ID (Primary Key)
     * - 자동 증가되는 고유 식별자
     * - Long 타입으로 확장성 고려
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)  // Auto Increment 사용
    @Column(name = "role_id")
    private Long roleId;

    /**
     * 역할명
     * - Spring Security 규칙에 따라 'ROLE_' 접두사 사용
     * - 예: ROLE_ADMIN, ROLE_USER, ROLE_MANAGER
     * - 중복 불허, 필수 입력
     * - 대소문자 구분함
     */
    @Column(name = "role_name", unique = true, nullable = false, length = 50)
    private String roleName;

    /**
     * 역할 표시명
     * - 사용자에게 보여지는 친숙한 이름
     * - 예: "시스템 관리자", "일반 사용자", "부서 관리자"
     * - 다국어 지원 시 이 필드를 변경
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * 역할 설명
     * - 역할의 상세한 설명 및 권한 범위
     * - 관리자가 역할을 이해하기 위한 도움말
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 역할 활성화 상태
     * - true: 활성화된 역할 (사용자에게 할당 가능)
     * - false: 비활성화된 역할 (사용 중단)
     * - 기존 할당된 사용자에게는 계속 적용됨
     */
    @Column(name = "is_active", nullable = false)
    @Builder.Default  // 빌더 패턴에서 기본값 설정
    private Boolean isActive = true;

    /**
     * 시스템 역할 여부
     * - true: 시스템에서 기본 제공하는 역할 (삭제 불가)
     * - false: 관리자가 생성한 커스텀 역할 (삭제 가능)
     * - ROLE_ADMIN, ROLE_USER 등은 시스템 역할
     */
    @Column(name = "is_system_role", nullable = false)
    @Builder.Default
    private Boolean isSystemRole = false;

    /**
     * 역할 우선순위
     * - 숫자가 낮을수록 높은 우선순위
     * - 권한 충돌시 우선순위가 높은 역할 적용
     * - 예: ADMIN(1) > MANAGER(10) > USER(100)
     */
    @Column(name = "priority", nullable = false)
    @Builder.Default
    private Integer priority = 100;

    /**
     * 이 역할을 가진 사용자들 (다대다 관계)
     * - 역할과 사용자는 다대다 관계
     * - LAZY 로딩으로 성능 최적화
     * - mappedBy로 User 엔티티의 roles 필드와 연결
     */
    @ManyToMany(mappedBy = "roles", fetch = FetchType.LAZY)
    @Builder.Default
    private Set<User> users = new HashSet<>();

    /**
     * 생성자: 기본 역할 생성용
     * - 시스템 초기화시 기본 역할 생성에 사용
     *
     * @param roleName 역할명
     * @param displayName 표시명
     * @param description 설명
     */
    public Role(String roleName, String displayName, String description) {
        this.roleName = roleName;
        this.displayName = displayName;
        this.description = description;
        this.isActive = true;
        this.isSystemRole = true;  // 기본 역할은 시스템 역할로 설정
        this.users = new HashSet<>();
    }

    /**
     * 생성자: 우선순위를 포함한 역할 생성
     *
     * @param roleName 역할명
     * @param displayName 표시명
     * @param description 설명
     * @param priority 우선순위
     */
    public Role(String roleName, String displayName, String description, Integer priority) {
        this(roleName, displayName, description);
        this.priority = priority;
    }

    /**
     * 비즈니스 메서드: 사용자 추가
     * - 이 역할에 사용자를 추가
     * - 중복 추가 방지
     *
     * @param user 추가할 사용자
     */
    public void addUser(User user) {
        if (user != null) {
            this.users.add(user);
            user.getRoles().add(this);  // 양방향 관계 동기화
        }
    }

    /**
     * 비즈니스 메서드: 사용자 제거
     * - 이 역할에서 사용자를 제거
     *
     * @param user 제거할 사용자
     */
    public void removeUser(User user) {
        if (user != null) {
            this.users.remove(user);
            user.getRoles().remove(this);  // 양방향 관계 동기화
        }
    }

    /**
     * 비즈니스 메서드: 역할 비활성화
     * - 역할을 비활성화 상태로 변경
     * - 새로운 할당은 불가능하지만 기존 할당은 유지
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 비즈니스 메서드: 역할 활성화
     * - 역할을 활성화 상태로 변경
     */
    public void activate() {
        this.isActive = true;
    }

    /**
     * 비즈니스 메서드: 시스템 역할 여부 확인
     * - 삭제 가능한 역할인지 확인할 때 사용
     *
     * @return 시스템 역할 여부
     */
    public boolean isDeletable() {
        return !this.isSystemRole;
    }

    /**
     * 비즈니스 메서드: 할당된 사용자 수 조회
     * - 이 역할을 가진 사용자 수 반환
     *
     * @return 사용자 수
     */
    public int getUserCount() {
        return this.users != null ? this.users.size() : 0;
    }

    /**
     * 비즈니스 메서드: 특정 사용자가 이 역할을 가지고 있는지 확인
     *
     * @param user 확인할 사용자
     * @return 역할 보유 여부
     */
    public boolean hasUser(User user) {
        return this.users != null && this.users.contains(user);
    }

    /**
     * 정적 메서드: 기본 역할 생성
     * - 시스템 초기화시 사용되는 기본 역할들을 생성
     *
     * @return 기본 역할 배열
     */
    public static Role[] createDefaultRoles() {
        return new Role[]{
                new Role("ROLE_ADMIN", "시스템 관리자",
                        "모든 시스템 기능에 대한 전체 권한을 가진 관리자", 1),
                new Role("ROLE_MANAGER", "부서 관리자",
                        "부서 내 사용자 관리 및 게시판 관리 권한", 10),
                new Role("ROLE_BOARD_ADMIN", "게시판 관리자",
                        "게시판 및 댓글 관리 권한", 20),
                new Role("ROLE_USER", "일반 사용자",
                        "기본적인 포털 사용 권한 (게시글 작성, 파일 업로드 등)", 100)
        };
    }

    /**
     * 정적 메서드: 역할명에서 접두사 제거
     * - 화면 표시용으로 ROLE_ 접두사를 제거한 이름 반환
     *
     * @param roleName 전체 역할명
     * @return 접두사가 제거된 역할명
     */
    public static String getSimpleRoleName(String roleName) {
        if (roleName != null && roleName.startsWith("ROLE_")) {
            return roleName.substring(5);  // "ROLE_" 제거
        }
        return roleName;
    }

    /**
     * equals 메서드 오버라이드
     * - 역할명을 기준으로 동등성 비교
     * - Set 컬렉션에서 중복 제거에 사용
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Role role = (Role) obj;
        return roleName != null && roleName.equals(role.roleName);
    }

    /**
     * hashCode 메서드 오버라이드
     * - 역할명을 기준으로 해시코드 생성
     * - equals와 함께 Set 컬렉션에서 사용
     */
    @Override
    public int hashCode() {
        return roleName != null ? roleName.hashCode() : 0;
    }
}