package com.kmportal.backend.service;

import com.kmportal.backend.entity.Role;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.RoleRepository;
import com.kmportal.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * UserService 단위 테스트 클래스
 *
 * 이 클래스는 UserService의 비즈니스 로직을 테스트합니다.
 * Mockito를 사용하여 의존성(Repository, PasswordEncoder)을 Mock 객체로 대체하고,
 * Service 로직만 독립적으로 테스트합니다.
 *
 * [테스트 구조 설명]
 *
 * 1. @ExtendWith(MockitoExtension.class)
 *    - JUnit 5에서 Mockito를 사용하기 위한 설정
 *    - @Mock, @InjectMocks 어노테이션 활성화
 *
 * 2. @Mock
 *    - 가짜 객체(Mock)를 생성
 *    - 실제 DB 연결 없이 테스트 가능
 *
 * 3. @InjectMocks
 *    - @Mock으로 생성된 객체들을 자동으로 주입
 *    - 테스트 대상 클래스에 의존성 주입
 *
 * [테스트 명명 규칙]
 *
 * 메서드명_조건_기대결과 형식 사용
 * 예: getUserById_WhenExists_ReturnsUser
 *
 * 작성일: 2025년 11월 29일 (40일차)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-29
 */
@ExtendWith(MockitoExtension.class)  // Mockito 확장 사용
@DisplayName("UserService 단위 테스트")  // 테스트 클래스 이름 지정
class UserServiceTest {

    // ================================
    // Mock 객체 선언
    // ================================

    /**
     * UserRepository Mock 객체
     *
     * 실제 데이터베이스에 접근하지 않고,
     * 미리 정의된 값을 반환하도록 설정할 수 있습니다.
     */
    @Mock
    private UserRepository userRepository;

    /**
     * RoleRepository Mock 객체
     *
     * 역할 관련 테스트에 사용됩니다.
     */
    @Mock
    private RoleRepository roleRepository;

    /**
     * PasswordEncoder Mock 객체
     *
     * 비밀번호 암호화 로직을 Mock으로 처리합니다.
     */
    @Mock
    private PasswordEncoder passwordEncoder;

    /**
     * 테스트 대상: UserService
     *
     * @InjectMocks는 위에서 @Mock으로 선언된 객체들을
     * 자동으로 UserService 생성자에 주입합니다.
     */
    @InjectMocks
    private UserService userService;

    // ================================
    // 테스트 픽스처 (Test Fixtures)
    // ================================

    /**
     * 테스트에서 공통으로 사용할 User 객체
     */
    private User testUser;

    /**
     * 테스트에서 공통으로 사용할 Role 객체
     */
    private Role testRole;

    /**
     * 각 테스트 메서드 실행 전에 호출되는 설정 메서드
     *
     * @BeforeEach: JUnit 5에서 각 테스트 전에 실행
     * - 테스트 데이터 초기화
     * - 공통 설정 수행
     */
    @BeforeEach
    void setUp() {
        // 테스트용 Role 생성
        testRole = new Role();
        testRole.setRoleId(1L);
        testRole.setRoleName("ROLE_USER");
        testRole.setDescription("일반 사용자");
        testRole.setPriority(100);

        // 테스트용 User 생성
        testUser = new User("testuser", "encodedPassword", "test@example.com", "테스트 사용자");
        testUser.setUserId(1L);
        testUser.setDepartment("개발팀");
        testUser.setPosition("대리");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setIsActive(true);
        testUser.setIsLocked(false);
        testUser.setPasswordExpired(false);
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // 역할 추가
        Set<Role> roles = new HashSet<>();
        roles.add(testRole);
        testUser.setRoles(roles);
    }

    // ================================
    // 사용자 조회 테스트
    // ================================

    /**
     * @Nested: 관련 테스트들을 그룹화
     *
     * 장점:
     * - 테스트 구조를 계층적으로 표현
     * - 관련 테스트를 논리적으로 묶음
     * - 테스트 결과 가독성 향상
     */
    @Nested
    @DisplayName("사용자 ID로 조회 테스트")
    class GetUserByIdTest {

        @Test
        @DisplayName("존재하는 사용자 조회 - 성공")
        void getUserById_WhenUserExists_ReturnsUser() {
            // ========== Given (준비) ==========
            // Mock 객체의 동작을 정의합니다.
            // userRepository.findById(1L)이 호출되면 testUser를 Optional로 감싸서 반환
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // ========== When (실행) ==========
            // 테스트 대상 메서드를 실행합니다.
            Optional<User> result = userService.getUserById(1L);

            // ========== Then (검증) ==========
            // 결과를 검증합니다.

            // 1. Optional이 비어있지 않은지 확인
            assertThat(result).isPresent();

            // 2. 반환된 User의 속성들이 올바른지 확인
            assertThat(result.get().getUserId()).isEqualTo(1L);
            assertThat(result.get().getUsername()).isEqualTo("testuser");
            assertThat(result.get().getEmail()).isEqualTo("test@example.com");
            assertThat(result.get().getFullName()).isEqualTo("테스트 사용자");

            // 3. Repository 메서드가 정확히 1번 호출되었는지 확인
            then(userRepository).should(times(1)).findById(1L);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회 - Optional.empty 반환")
        void getUserById_WhenUserNotExists_ReturnsEmpty() {
            // Given
            // 존재하지 않는 ID로 조회 시 빈 Optional 반환
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When
            Optional<User> result = userService.getUserById(999L);

            // Then
            assertThat(result).isEmpty();
            then(userRepository).should(times(1)).findById(999L);
        }
    }

    // ================================
    // 사용자 목록 조회 테스트
    // ================================

    @Nested
    @DisplayName("사용자 목록 조회 테스트")
    class GetAllUsersTest {

        @Test
        @DisplayName("페이징된 사용자 목록 조회 - 성공")
        void getAllUsers_WithPaging_ReturnsPagedUsers() {
            // Given
            // 테스트용 사용자 목록 생성
            List<User> userList = Arrays.asList(testUser);
            Page<User> userPage = new PageImpl<>(
                    userList,
                    PageRequest.of(0, 10),
                    1
            );

            // Mock 설정: findAll 호출 시 Page 객체 반환
            given(userRepository.findAll(any(Pageable.class))).willReturn(userPage);

            // When
            Page<User> result = userService.getAllUsers(0, 10, "createdAt", "desc");

            // Then
            // 1. 페이지 정보 확인
            assertThat(result).isNotNull();
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.getContent()).hasSize(1);

            // 2. 첫 번째 사용자 정보 확인
            assertThat(result.getContent().get(0).getUsername()).isEqualTo("testuser");

            // 3. Repository 메서드 호출 검증
            then(userRepository).should(times(1)).findAll(any(Pageable.class));
        }

        @Test
        @DisplayName("빈 사용자 목록 조회 - 빈 Page 반환")
        void getAllUsers_WhenNoUsers_ReturnsEmptyPage() {
            // Given
            Page<User> emptyPage = new PageImpl<>(
                    Collections.emptyList(),
                    PageRequest.of(0, 10),
                    0
            );
            given(userRepository.findAll(any(Pageable.class))).willReturn(emptyPage);

            // When
            Page<User> result = userService.getAllUsers(0, 10, "createdAt", "desc");

            // Then
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
    }

    // ================================
    // 활성 사용자 조회 테스트
    // ================================

    @Nested
    @DisplayName("활성 사용자 조회 테스트")
    class GetActiveUsersTest {

        @Test
        @DisplayName("활성 사용자 목록 조회 - 성공")
        void getActiveUsers_ReturnsOnlyActiveUsers() {
            // Given
            List<User> activeUsers = Arrays.asList(testUser);
            Page<User> activePage = new PageImpl<>(activeUsers, PageRequest.of(0, 10), 1);

            given(userRepository.findByIsActiveTrue(any(Pageable.class))).willReturn(activePage);

            // When
            Page<User> result = userService.getActiveUsers(PageRequest.of(0, 10));

            // Then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getIsActive()).isTrue();
            then(userRepository).should().findByIsActiveTrue(any(Pageable.class));
        }
    }

    // ================================
    // 사용자 삭제 테스트 (Soft Delete)
    // ================================

    @Nested
    @DisplayName("사용자 삭제 테스트")
    class DeleteUserTest {

        @Test
        @DisplayName("사용자 소프트 삭제 - 성공")
        void deleteUser_WhenUserExists_SetsInactive() {
            // Given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            userService.deleteUser(1L);

            // Then
            // 1. 사용자가 비활성화 되었는지 확인
            assertThat(testUser.getIsActive()).isFalse();

            // 2. save 메서드가 호출되었는지 확인
            then(userRepository).should(times(1)).save(testUser);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 삭제 - 예외 발생")
        void deleteUser_WhenUserNotExists_ThrowsException() {
            // Given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            // assertThatThrownBy: 예외가 발생하는지 검증
            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    // ================================
    // 사용자 통계 테스트
    // ================================

    @Nested
    @DisplayName("사용자 통계 테스트")
    class UserStatisticsTest {

        @Test
        @DisplayName("활성 사용자 수 조회 - 성공")
        void getActiveUserCount_ReturnsCorrectCount() {
            // Given
            given(userRepository.countByIsActiveTrue()).willReturn(100L);

            // When
            long count = userService.getActiveUserCount();

            // Then
            assertThat(count).isEqualTo(100L);
            then(userRepository).should().countByIsActiveTrue();
        }

        @Test
        @DisplayName("전체 사용자 수 조회 - 성공")
        void getTotalUserCount_ReturnsCorrectCount() {
            // Given
            given(userRepository.count()).willReturn(150L);

            // When
            long count = userService.getTotalUserCount();

            // Then
            assertThat(count).isEqualTo(150L);
        }
    }

    // ================================
    // 중복 확인 테스트
    // ================================

    @Nested
    @DisplayName("중복 확인 테스트")
    class DuplicationCheckTest {

        @Test
        @DisplayName("사용자명 중복 - true 반환")
        void isUsernameExists_WhenDuplicate_ReturnsTrue() {
            // Given
            given(userRepository.existsByUsername("existingUser")).willReturn(true);

            // When
            boolean exists = userService.isUsernameExists("existingUser");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("사용자명 중복 아님 - false 반환")
        void isUsernameExists_WhenNotDuplicate_ReturnsFalse() {
            // Given
            given(userRepository.existsByUsername("newUser")).willReturn(false);

            // When
            boolean exists = userService.isUsernameExists("newUser");

            // Then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("이메일 중복 확인 - true 반환")
        void isEmailExists_WhenDuplicate_ReturnsTrue() {
            // Given
            given(userRepository.existsByEmail("existing@example.com")).willReturn(true);

            // When
            boolean exists = userService.isEmailExists("existing@example.com");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("이메일 중복 아님 - false 반환")
        void isEmailExists_WhenNotDuplicate_ReturnsFalse() {
            // Given
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);

            // When
            boolean exists = userService.isEmailExists("new@example.com");

            // Then
            assertThat(exists).isFalse();
        }
    }

    // ================================
    // 역할별 사용자 수 조회 테스트
    // ================================

    @Nested
    @DisplayName("역할별 사용자 수 조회 테스트")
    class GetUserCountByRoleTest {

        @Test
        @DisplayName("역할별 사용자 수 조회 - 성공")
        void getUserCountByRole_ReturnsCorrectCount() {
            // Given
            List<User> adminUsers = Arrays.asList(testUser, testUser);
            given(userRepository.findByRoleName("ROLE_ADMIN")).willReturn(adminUsers);

            // When
            long count = userService.getUserCountByRole("ROLE_ADMIN");

            // Then
            assertThat(count).isEqualTo(2);
            then(userRepository).should().findByRoleName("ROLE_ADMIN");
        }

        @Test
        @DisplayName("역할에 해당하는 사용자 없음 - 0 반환")
        void getUserCountByRole_WhenNoUsers_ReturnsZero() {
            // Given
            given(userRepository.findByRoleName("ROLE_SUPER_ADMIN")).willReturn(Collections.emptyList());

            // When
            long count = userService.getUserCountByRole("ROLE_SUPER_ADMIN");

            // Then
            assertThat(count).isZero();
        }
    }
}

/*
 * ====== 테스트 작성 가이드 ======
 *
 * 1. 테스트 구조 (Given-When-Then 패턴):
 *
 *    Given (준비):
 *    - 테스트에 필요한 데이터와 Mock 설정
 *    - given(mock.method()).willReturn(value)
 *
 *    When (실행):
 *    - 테스트 대상 메서드 실행
 *    - 결과를 변수에 저장
 *
 *    Then (검증):
 *    - 결과가 예상과 일치하는지 확인
 *    - assertThat(result).isEqualTo(expected)
 *    - then(mock).should().method()
 *
 * 2. Mock 사용법:
 *
 *    // 반환값 설정
 *    given(mockRepository.findById(1L)).willReturn(Optional.of(user));
 *
 *    // 예외 발생 설정
 *    given(mockService.method()).willThrow(new RuntimeException());
 *
 *    // 호출 검증
 *    then(mockRepository).should().save(any(User.class));
 *    then(mockRepository).should(times(2)).findById(anyLong());
 *    then(mockRepository).should(never()).delete(any());
 *
 * 3. AssertJ 주요 메서드:
 *
 *    assertThat(result).isNotNull();
 *    assertThat(result).isEqualTo(expected);
 *    assertThat(list).hasSize(3);
 *    assertThat(list).isEmpty();
 *    assertThat(list).contains(item);
 *    assertThat(optional).isPresent();
 *    assertThat(optional).isEmpty();
 *    assertThatThrownBy(() -> method()).isInstanceOf(Exception.class);
 *
 * 4. 테스트 실행 방법:
 *
 *    IDE:
 *    - 클래스 또는 메서드 옆의 재생 버튼 클릭
 *
 *    Maven:
 *    - mvn test (전체 테스트)
 *    - mvn test -Dtest=UserServiceTest (특정 테스트 클래스)
 *    - mvn test -Dtest=UserServiceTest#getUserById_WhenExists_ReturnsUser (특정 메서드)
 */