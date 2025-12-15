package com.kmportal.backend.service;

import com.kmportal.backend.controller.AuthController;
import com.kmportal.backend.entity.Role;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.RoleRepository;
import com.kmportal.backend.repository.UserRepository;
import com.kmportal.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

/**
 * AuthService 단위 테스트 클래스
 *
 * 이 클래스는 AuthService의 인증 관련 비즈니스 로직을 테스트합니다.
 *
 * [주요 테스트 항목]
 *
 * 1. 로그인 인증 (authenticate)
 *    - 성공 케이스
 *    - 실패 케이스 (존재하지 않는 사용자, 비밀번호 불일치 등)
 *
 * 2. 계정 상태 검증
 *    - 비활성 계정
 *    - 잠금 계정
 *    - 로그인 실패 횟수 초과
 *
 * 3. 회원가입 (registerUser)
 *    - 성공 케이스
 *    - 실패 케이스 (중복 사용자명, 중복 이메일 등)
 *
 * [테스트 전략]
 *
 * - Mock 객체를 사용하여 외부 의존성 격리
 * - 각 시나리오별 명확한 테스트 케이스 작성
 * - 예외 상황에 대한 철저한 테스트
 *
 * 작성일: 2025년 11월 29일 (40일차)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-29
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 단위 테스트")
class AuthServiceTest {

    // ================================
    // Mock 객체 선언
    // ================================

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private Environment environment;

    @InjectMocks
    private AuthService authService;

    // ================================
    // 테스트 픽스처
    // ================================

    private User testUser;
    private Role userRole;

    @BeforeEach
    void setUp() {
        // 테스트용 Role 생성
        userRole = new Role();
        userRole.setRoleId(1L);
        userRole.setRoleName("ROLE_USER");
        userRole.setDescription("일반 사용자");
        userRole.setPriority(100);

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
        roles.add(userRole);
        testUser.setRoles(roles);
    }

    // ================================
    // 로그인 인증 테스트
    // ================================

    @Nested
    @DisplayName("로그인 인증 테스트")
    class AuthenticateTest {

        @Test
        @DisplayName("로그인 성공 - 토큰 발급")
        void authenticate_WithValidCredentials_ReturnsSuccess() {
            // Given
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("password123", "encodedPassword")).willReturn(true);

            // JwtUtil.generateToken(String username, String fullName, String email, String department, List<String> roles)
            // 실제 JwtUtil 메서드 시그니처에 맞춰 Mock 설정
            given(jwtUtil.generateToken(
                    anyString(),  // username
                    anyString(),  // fullName
                    anyString(),  // email
                    anyString(),  // department
                    anyList()     // roles
            )).willReturn("access-token-123");

            given(jwtUtil.generateRefreshToken(anyString())).willReturn("refresh-token-456");
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            AuthService.LoginResponse result = authService.authenticate("testuser", "password123");

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getAccessToken()).isEqualTo("access-token-123");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token-456");
            assertThat(result.getUserInfo()).isNotNull();
            assertThat(result.getUserInfo().getUsername()).isEqualTo("testuser");

            // 마지막 로그인 시간 업데이트 및 실패 횟수 초기화 확인
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("로그인 실패 - 존재하지 않는 사용자")
        void authenticate_WithNonExistentUser_ReturnsFailure() {
            // Given
            given(userRepository.findByUsername("nonexistent")).willReturn(Optional.empty());

            // When
            AuthService.LoginResponse result = authService.authenticate("nonexistent", "password");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("존재하지 않는 사용자");
            assertThat(result.getAccessToken()).isNull();
        }

        @Test
        @DisplayName("로그인 실패 - 비밀번호 불일치")
        void authenticate_WithWrongPassword_ReturnsFailure() {
            // Given
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(passwordEncoder.matches("wrongPassword", "encodedPassword")).willReturn(false);
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            AuthService.LoginResponse result = authService.authenticate("testuser", "wrongPassword");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("비밀번호가 일치하지 않습니다");

            // 실패 횟수 증가 확인
            assertThat(testUser.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("로그인 실패 - 비활성 계정")
        void authenticate_WithInactiveAccount_ReturnsFailure() {
            // Given
            testUser.setIsActive(false);
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            // When
            AuthService.LoginResponse result = authService.authenticate("testuser", "password123");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("비활성화된 계정");
        }

        @Test
        @DisplayName("로그인 실패 - 잠금된 계정")
        void authenticate_WithLockedAccount_ReturnsFailure() {
            // Given
            testUser.setIsLocked(true);
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));

            // When
            AuthService.LoginResponse result = authService.authenticate("testuser", "password123");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("잠긴 계정");
        }

        @Test
        @DisplayName("로그인 실패 횟수 초과 - 계정 잠금")
        void authenticate_WithExceededFailedAttempts_LocksAccount() {
            // Given
            testUser.setFailedLoginAttempts(5);  // 이미 5회 실패
            given(userRepository.findByUsername("testuser")).willReturn(Optional.of(testUser));
            given(userRepository.save(any(User.class))).willReturn(testUser);

            // When
            AuthService.LoginResponse result = authService.authenticate("testuser", "password123");

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("로그인 실패 횟수");
            assertThat(testUser.getIsLocked()).isTrue();
        }
    }

    // ================================
    // 회원가입 테스트
    // ================================

    @Nested
    @DisplayName("회원가입 테스트")
    class RegisterUserTest {

        private AuthController.RegisterRequest createRegisterRequest() {
            AuthController.RegisterRequest request = new AuthController.RegisterRequest();
            request.setUsername("newuser");
            request.setPassword("password123");
            request.setEmail("new@example.com");
            request.setFullName("신규 사용자");
            request.setDepartment("마케팅팀");
            request.setPosition("사원");
            request.setPhoneNumber("010-9876-5432");
            return request;
        }

        @Test
        @DisplayName("회원가입 성공 - 개발 환경")
        void registerUser_WithValidInput_InDevEnv_ReturnsSuccess() {
            // Given
            AuthController.RegisterRequest request = createRegisterRequest();

            given(userRepository.existsByUsername("newuser")).willReturn(false);
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
            given(roleRepository.findByRoleName("ROLE_USER")).willReturn(Optional.of(userRole));
            given(environment.getActiveProfiles()).willReturn(new String[]{"dev"});

            User savedUser = new User("newuser", "encodedPassword", "new@example.com", "신규 사용자");
            savedUser.setUserId(2L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // When
            AuthController.RegisterResponse result = authService.registerUser(request);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).contains("바로 로그인");

            // 비밀번호 암호화 확인
            then(passwordEncoder).should().encode("password123");
            // 저장 확인
            then(userRepository).should().save(any(User.class));
        }

        @Test
        @DisplayName("회원가입 성공 - 운영 환경 (관리자 승인 필요)")
        void registerUser_WithValidInput_InProdEnv_ReturnsSuccess() {
            // Given
            AuthController.RegisterRequest request = createRegisterRequest();

            given(userRepository.existsByUsername("newuser")).willReturn(false);
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
            given(roleRepository.findByRoleName("ROLE_USER")).willReturn(Optional.of(userRole));
            given(environment.getActiveProfiles()).willReturn(new String[]{"prod"});

            User savedUser = new User("newuser", "encodedPassword", "new@example.com", "신규 사용자");
            savedUser.setUserId(2L);
            given(userRepository.save(any(User.class))).willReturn(savedUser);

            // When
            AuthController.RegisterResponse result = authService.registerUser(request);

            // Then
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getMessage()).contains("관리자 승인");
        }

        @Test
        @DisplayName("회원가입 실패 - 사용자명 중복")
        void registerUser_WithDuplicateUsername_ReturnsFailure() {
            // Given
            AuthController.RegisterRequest request = createRegisterRequest();
            given(userRepository.existsByUsername("newuser")).willReturn(true);

            // When
            AuthController.RegisterResponse result = authService.registerUser(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("이미 사용 중인 사용자명");

            // 저장되지 않았는지 확인
            then(userRepository).should(never()).save(any(User.class));
        }

        @Test
        @DisplayName("회원가입 실패 - 이메일 중복")
        void registerUser_WithDuplicateEmail_ReturnsFailure() {
            // Given
            AuthController.RegisterRequest request = createRegisterRequest();
            given(userRepository.existsByUsername("newuser")).willReturn(false);
            given(userRepository.existsByEmail("new@example.com")).willReturn(true);

            // When
            AuthController.RegisterResponse result = authService.registerUser(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("이미 사용 중인 이메일");
        }

        @Test
        @DisplayName("회원가입 실패 - 기본 역할을 찾을 수 없음")
        void registerUser_WhenDefaultRoleNotFound_ReturnsFailure() {
            // Given
            AuthController.RegisterRequest request = createRegisterRequest();
            given(userRepository.existsByUsername("newuser")).willReturn(false);
            given(userRepository.existsByEmail("new@example.com")).willReturn(false);
            given(passwordEncoder.encode("password123")).willReturn("encodedPassword");
            given(roleRepository.findByRoleName("ROLE_USER")).willReturn(Optional.empty());
            given(environment.getActiveProfiles()).willReturn(new String[]{"dev"});

            // When
            AuthController.RegisterResponse result = authService.registerUser(request);

            // Then
            assertThat(result.isSuccess()).isFalse();
            assertThat(result.getMessage()).contains("오류가 발생");
        }
    }

    // ================================
    // 로그인 응답 클래스 테스트
    // ================================

    @Nested
    @DisplayName("LoginResponse 클래스 테스트")
    class LoginResponseTest {

        @Test
        @DisplayName("성공 응답 생성")
        void loginResponse_Success_HasCorrectFields() {
            // Given
            AuthService.LoginResponse response = AuthService.LoginResponse.success(
                    "access-token",
                    "refresh-token",
                    testUser
            );

            // Then
            assertThat(response.isSuccess()).isTrue();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getUserInfo()).isNotNull();
            assertThat(response.getUserInfo().getUserId()).isEqualTo(1L);
            assertThat(response.getUserInfo().getUsername()).isEqualTo("testuser");
            assertThat(response.getUserInfo().getRoles()).contains("ROLE_USER");
        }

        @Test
        @DisplayName("실패 응답 생성")
        void loginResponse_Failure_HasCorrectMessage() {
            // Given
            AuthService.LoginResponse response = AuthService.LoginResponse.failure("로그인 실패");

            // Then
            assertThat(response.isSuccess()).isFalse();
            assertThat(response.getMessage()).isEqualTo("로그인 실패");
            assertThat(response.getAccessToken()).isNull();
            assertThat(response.getRefreshToken()).isNull();
            assertThat(response.getUserInfo()).isNull();
        }
    }

    // ================================
    // UserInfo 클래스 테스트
    // ================================

    @Nested
    @DisplayName("UserInfo 클래스 테스트")
    class UserInfoTest {

        @Test
        @DisplayName("UserInfo 생성 및 필드 확인")
        void userInfo_Creation_HasCorrectFields() {
            // Given
            List<String> roles = Arrays.asList("ROLE_USER", "ROLE_MANAGER");

            AuthService.UserInfo userInfo = new AuthService.UserInfo(
                    1L,
                    "testuser",
                    "테스트 사용자",
                    "test@example.com",
                    "개발팀",
                    roles
            );

            // Then
            assertThat(userInfo.getUserId()).isEqualTo(1L);
            assertThat(userInfo.getUsername()).isEqualTo("testuser");
            assertThat(userInfo.getFullName()).isEqualTo("테스트 사용자");
            assertThat(userInfo.getEmail()).isEqualTo("test@example.com");
            assertThat(userInfo.getDepartment()).isEqualTo("개발팀");
            assertThat(userInfo.getRoles()).containsExactly("ROLE_USER", "ROLE_MANAGER");
        }
    }
}

/*
 * ====== AuthService 테스트 가이드 ======
 *
 * 1. 인증 테스트 시나리오:
 *
 *    성공 케이스:
 *    - 올바른 사용자명과 비밀번호
 *    - 활성화된 계정
 *    - 잠금되지 않은 계정
 *
 *    실패 케이스:
 *    - 존재하지 않는 사용자명
 *    - 잘못된 비밀번호
 *    - 비활성 계정
 *    - 잠금 계정
 *    - 로그인 실패 횟수 초과
 *
 * 2. 회원가입 테스트 시나리오:
 *
 *    성공 케이스:
 *    - 유효한 정보 입력 (개발 환경)
 *    - 유효한 정보 입력 (운영 환경)
 *
 *    실패 케이스:
 *    - 중복 사용자명
 *    - 중복 이메일
 *    - 기본 역할 없음
 *
 * 3. PasswordEncoder Mock:
 *
 *    // 암호화
 *    given(passwordEncoder.encode("plain")).willReturn("encoded");
 *
 *    // 비교
 *    given(passwordEncoder.matches("plain", "encoded")).willReturn(true);
 *
 * 4. JwtUtil Mock:
 *
 *    given(jwtUtil.generateAccessToken(username, userId, roles))
 *        .willReturn("access-token");
 *
 *    given(jwtUtil.generateRefreshToken(username))
 *        .willReturn("refresh-token");
 *
 * 5. Environment Mock:
 *
 *    // 개발 환경
 *    given(environment.getActiveProfiles()).willReturn(new String[]{"dev"});
 *
 *    // 운영 환경
 *    given(environment.getActiveProfiles()).willReturn(new String[]{"prod"});
 */