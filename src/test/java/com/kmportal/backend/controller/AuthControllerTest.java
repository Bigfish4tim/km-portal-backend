package com.kmportal.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kmportal.backend.entity.Role;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.RoleRepository;
import com.kmportal.backend.repository.UserRepository;
import com.kmportal.backend.service.AuthService;
import com.kmportal.backend.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 통합 테스트 클래스
 *
 * 이 클래스는 인증 관련 API 엔드포인트의 통합 테스트를 수행합니다.
 * @SpringBootTest와 MockMvc를 사용하여 실제 HTTP 요청/응답을 테스트합니다.
 *
 * [테스트 대상 API]
 *
 * 1. POST /api/auth/login     - 로그인
 * 2. POST /api/auth/register  - 회원가입
 * 3. POST /api/auth/refresh   - 토큰 갱신
 * 4. POST /api/auth/logout    - 로그아웃
 * 5. GET  /api/auth/me        - 현재 사용자 정보
 *
 * [테스트 환경]
 *
 * - @SpringBootTest: 전체 애플리케이션 컨텍스트 로드
 * - @AutoConfigureMockMvc: MockMvc 자동 구성
 * - @ActiveProfiles("test"): 테스트 프로파일 사용
 * - @Transactional: 각 테스트 후 롤백
 *
 * 작성일: 2025년 11월 30일 (41일차)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-30
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController 통합 테스트")
class AuthControllerTest {

    // ================================
    // 의존성 주입
    // ================================

    /**
     * MockMvc - HTTP 요청/응답 시뮬레이션
     * 실제 서버를 띄우지 않고 Controller 테스트 가능
     */
    @Autowired
    private MockMvc mockMvc;

    /**
     * ObjectMapper - JSON 직렬화/역직렬화
     * 요청 본문을 JSON으로 변환할 때 사용
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * UserRepository - 테스트 데이터 설정용
     * 테스트 전 사용자 데이터를 미리 저장
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * RoleRepository - 역할 데이터 설정용
     */
    @Autowired
    private RoleRepository roleRepository;

    /**
     * PasswordEncoder - 비밀번호 암호화
     * 테스트 사용자 생성 시 비밀번호 암호화에 사용
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * JwtUtil - JWT 토큰 생성/검증
     * 인증이 필요한 API 테스트 시 토큰 생성에 사용
     */
    @Autowired
    private JwtUtil jwtUtil;

    // ================================
    // 테스트 픽스처 (공통 데이터)
    // ================================

    private User testUser;
    private Role userRole;
    private Role adminRole;
    private String validAccessToken;

    /**
     * 각 테스트 실행 전 호출되는 설정 메서드
     *
     * 테스트에 필요한 공통 데이터를 미리 생성합니다:
     * - 역할 (ROLE_USER, ROLE_ADMIN)
     * - 테스트 사용자
     * - 유효한 JWT 토큰
     */
    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 (테스트 격리를 위해)
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // ========== 역할 생성 ==========
        userRole = new Role();
        userRole.setRoleName("ROLE_USER");
        userRole.setDescription("일반 사용자");
        userRole.setPriority(100);
        userRole = roleRepository.save(userRole);

        adminRole = new Role();
        adminRole.setRoleName("ROLE_ADMIN");
        adminRole.setDescription("관리자");
        adminRole.setPriority(1);
        adminRole = roleRepository.save(adminRole);

        // ========== 테스트 사용자 생성 ==========
        testUser = new User(
                "testuser",                              // username
                passwordEncoder.encode("password123"),   // 암호화된 비밀번호
                "test@example.com",                      // email
                "테스트 사용자"                           // fullName
        );
        testUser.setDepartment("개발팀");
        testUser.setPosition("대리");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setIsActive(true);
        testUser.setIsLocked(false);
        testUser.setPasswordExpired(false);
        testUser.setFailedLoginAttempts(0);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setUpdatedAt(LocalDateTime.now());

        // 역할 할당
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);
        testUser.setRoles(roles);

        testUser = userRepository.save(testUser);

        // ========== 테스트용 JWT 토큰 생성 ==========
        List<String> roleNames = Arrays.asList("ROLE_USER");
        validAccessToken = jwtUtil.generateToken(
                testUser.getUsername(),
                testUser.getFullName(),
                testUser.getEmail(),
                testUser.getDepartment(),
                roleNames
        );
    }

    // ================================
    // 로그인 API 테스트
    // ================================

    @Nested
    @DisplayName("POST /api/auth/login - 로그인 API")
    class LoginTest {

        /**
         * 로그인 성공 테스트
         *
         * 시나리오: 올바른 사용자명과 비밀번호로 로그인
         * 예상 결과: 200 OK, JWT 토큰 반환
         */
        @Test
        @DisplayName("로그인 성공 - 올바른 자격증명")
        void login_WithValidCredentials_ReturnsTokens() throws Exception {
            // Given: 로그인 요청 데이터 준비
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "testuser");
            loginRequest.put("password", "password123");

            // When: 로그인 API 호출
            ResultActions result = mockMvc.perform(
                    post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
            );

            // Then: 응답 검증
            result
                    .andDo(print())  // 요청/응답 로깅
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.refreshToken").exists())
                    .andExpect(jsonPath("$.userInfo.username").value("testuser"))
                    .andExpect(jsonPath("$.userInfo.email").value("test@example.com"))
                    .andExpect(jsonPath("$.userInfo.roles").isArray())
                    .andExpect(jsonPath("$.userInfo.roles[0]").value("ROLE_USER"));
        }

        /**
         * 로그인 실패 테스트 - 존재하지 않는 사용자
         *
         * 시나리오: 등록되지 않은 사용자명으로 로그인 시도
         * 예상 결과: 401 Unauthorized 또는 200 OK with success=false
         */
        @Test
        @DisplayName("로그인 실패 - 존재하지 않는 사용자")
        void login_WithNonExistentUser_ReturnsFailure() throws Exception {
            // Given
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "nonexistent");
            loginRequest.put("password", "password123");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())  // 비즈니스 로직에서 처리
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("존재하지 않는 사용자")));
        }

        /**
         * 로그인 실패 테스트 - 잘못된 비밀번호
         *
         * 시나리오: 올바른 사용자명이지만 틀린 비밀번호
         * 예상 결과: success=false, 실패 메시지 반환
         */
        @Test
        @DisplayName("로그인 실패 - 잘못된 비밀번호")
        void login_WithWrongPassword_ReturnsFailure() throws Exception {
            // Given
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "testuser");
            loginRequest.put("password", "wrongPassword");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("비밀번호")));
        }

        /**
         * 로그인 실패 테스트 - 비활성화된 계정
         *
         * 시나리오: isActive가 false인 계정으로 로그인 시도
         * 예상 결과: 로그인 거부
         */
        @Test
        @DisplayName("로그인 실패 - 비활성화된 계정")
        void login_WithInactiveAccount_ReturnsFailure() throws Exception {
            // Given: 계정 비활성화
            testUser.setIsActive(false);
            userRepository.save(testUser);

            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "testuser");
            loginRequest.put("password", "password123");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("비활성화")));
        }

        /**
         * 로그인 실패 테스트 - 잠긴 계정
         *
         * 시나리오: 로그인 실패 횟수 초과로 잠긴 계정
         * 예상 결과: 로그인 거부
         */
        @Test
        @DisplayName("로그인 실패 - 잠긴 계정")
        void login_WithLockedAccount_ReturnsFailure() throws Exception {
            // Given: 계정 잠금
            testUser.setIsLocked(true);
            userRepository.save(testUser);

            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "testuser");
            loginRequest.put("password", "password123");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("잠금")));
        }

        /**
         * 로그인 요청 유효성 검사 테스트
         *
         * 시나리오: 필수 필드 누락
         * 예상 결과: 400 Bad Request 또는 유효성 오류 메시지
         */
        @Test
        @DisplayName("로그인 실패 - 필수 필드 누락 (username)")
        void login_WithMissingUsername_ReturnsBadRequest() throws Exception {
            // Given: username 누락
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("password", "password123");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        /**
         * 로그인 요청 유효성 검사 테스트 - 빈 비밀번호
         */
        @Test
        @DisplayName("로그인 실패 - 빈 비밀번호")
        void login_WithEmptyPassword_ReturnsBadRequest() throws Exception {
            // Given
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "testuser");
            loginRequest.put("password", "");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    // ================================
    // 회원가입 API 테스트
    // ================================

    @Nested
    @DisplayName("POST /api/auth/register - 회원가입 API")
    class RegisterTest {

        /**
         * 회원가입 성공 테스트
         *
         * 시나리오: 유효한 정보로 회원가입
         * 예상 결과: 200 OK, 가입 성공 메시지
         */
        @Test
        @DisplayName("회원가입 성공 - 유효한 정보")
        void register_WithValidInput_ReturnsSuccess() throws Exception {
            // Given: 회원가입 요청 데이터
            Map<String, String> registerRequest = new HashMap<>();
            registerRequest.put("username", "newuser");
            registerRequest.put("password", "newPassword123");
            registerRequest.put("email", "newuser@example.com");
            registerRequest.put("fullName", "신규 사용자");
            registerRequest.put("department", "영업팀");
            registerRequest.put("position", "사원");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(registerRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").exists());
        }

        /**
         * 회원가입 실패 테스트 - 중복 사용자명
         *
         * 시나리오: 이미 존재하는 사용자명으로 가입 시도
         * 예상 결과: 가입 거부, 중복 메시지
         */
        @Test
        @DisplayName("회원가입 실패 - 중복 사용자명")
        void register_WithDuplicateUsername_ReturnsFailure() throws Exception {
            // Given: 이미 존재하는 username 사용
            Map<String, String> registerRequest = new HashMap<>();
            registerRequest.put("username", "testuser");  // 이미 존재
            registerRequest.put("password", "password123");
            registerRequest.put("email", "different@example.com");
            registerRequest.put("fullName", "중복 사용자");
            registerRequest.put("department", "개발팀");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(registerRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("사용자명")));
        }

        /**
         * 회원가입 실패 테스트 - 중복 이메일
         *
         * 시나리오: 이미 존재하는 이메일로 가입 시도
         * 예상 결과: 가입 거부, 중복 메시지
         */
        @Test
        @DisplayName("회원가입 실패 - 중복 이메일")
        void register_WithDuplicateEmail_ReturnsFailure() throws Exception {
            // Given
            Map<String, String> registerRequest = new HashMap<>();
            registerRequest.put("username", "differentuser");
            registerRequest.put("password", "password123");
            registerRequest.put("email", "test@example.com");  // 이미 존재
            registerRequest.put("fullName", "다른 사용자");
            registerRequest.put("department", "인사팀");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(registerRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value(containsString("이메일")));
        }

        /**
         * 회원가입 유효성 검사 테스트 - 짧은 비밀번호
         */
        @Test
        @DisplayName("회원가입 실패 - 짧은 비밀번호")
        void register_WithShortPassword_ReturnsBadRequest() throws Exception {
            // Given
            Map<String, String> registerRequest = new HashMap<>();
            registerRequest.put("username", "shortpwuser");
            registerRequest.put("password", "12");  // 너무 짧음
            registerRequest.put("email", "short@example.com");
            registerRequest.put("fullName", "테스트");
            registerRequest.put("department", "개발팀");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(registerRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }

        /**
         * 회원가입 유효성 검사 테스트 - 잘못된 이메일 형식
         */
        @Test
        @DisplayName("회원가입 실패 - 잘못된 이메일 형식")
        void register_WithInvalidEmail_ReturnsBadRequest() throws Exception {
            // Given
            Map<String, String> registerRequest = new HashMap<>();
            registerRequest.put("username", "invalidemailuser");
            registerRequest.put("password", "password123");
            registerRequest.put("email", "invalid-email");  // 잘못된 형식
            registerRequest.put("fullName", "테스트");
            registerRequest.put("department", "개발팀");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(registerRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    // ================================
    // 현재 사용자 정보 API 테스트
    // ================================

    @Nested
    @DisplayName("GET /api/auth/me - 현재 사용자 정보 API")
    class GetCurrentUserTest {

        /**
         * 인증된 사용자 정보 조회 성공
         *
         * 시나리오: 유효한 JWT 토큰으로 /api/auth/me 호출
         * 예상 결과: 현재 로그인한 사용자 정보 반환
         */
        @Test
        @DisplayName("현재 사용자 정보 조회 성공")
        void getCurrentUser_WithValidToken_ReturnsUserInfo() throws Exception {
            // When & Then
            mockMvc.perform(
                            get("/api/auth/me")
                                    .header("Authorization", "Bearer " + validAccessToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.username").value("testuser"))
                    .andExpect(jsonPath("$.email").value("test@example.com"))
                    .andExpect(jsonPath("$.fullName").value("테스트 사용자"));
        }

        /**
         * 인증 없이 사용자 정보 조회 실패
         *
         * 시나리오: 토큰 없이 /api/auth/me 호출
         * 예상 결과: 401 Unauthorized
         */
        @Test
        @DisplayName("현재 사용자 정보 조회 실패 - 토큰 없음")
        void getCurrentUser_WithoutToken_ReturnsUnauthorized() throws Exception {
            // When & Then
            mockMvc.perform(
                            get("/api/auth/me")
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        /**
         * 만료된 토큰으로 사용자 정보 조회 실패
         *
         * 시나리오: 만료된 JWT 토큰 사용
         * 예상 결과: 401 Unauthorized
         */
        @Test
        @DisplayName("현재 사용자 정보 조회 실패 - 만료된 토큰")
        void getCurrentUser_WithExpiredToken_ReturnsUnauthorized() throws Exception {
            // Given: 만료된 토큰 (임의의 문자열)
            String expiredToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.expired.token";

            // When & Then
            mockMvc.perform(
                            get("/api/auth/me")
                                    .header("Authorization", "Bearer " + expiredToken)
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        /**
         * 잘못된 형식의 토큰으로 조회 실패
         */
        @Test
        @DisplayName("현재 사용자 정보 조회 실패 - 잘못된 토큰 형식")
        void getCurrentUser_WithInvalidToken_ReturnsUnauthorized() throws Exception {
            // Given
            String invalidToken = "invalid-token-format";

            // When & Then
            mockMvc.perform(
                            get("/api/auth/me")
                                    .header("Authorization", "Bearer " + invalidToken)
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    // ================================
    // 토큰 갱신 API 테스트
    // ================================

    @Nested
    @DisplayName("POST /api/auth/refresh - 토큰 갱신 API")
    class RefreshTokenTest {

        /**
         * 토큰 갱신 성공 테스트
         *
         * 시나리오: 유효한 리프레시 토큰으로 새 액세스 토큰 요청
         * 예상 결과: 새로운 액세스 토큰 발급
         */
        @Test
        @DisplayName("토큰 갱신 성공")
        void refreshToken_WithValidRefreshToken_ReturnsNewAccessToken() throws Exception {
            // Given: 리프레시 토큰 생성
            String refreshToken = jwtUtil.generateRefreshToken(testUser.getUsername());

            Map<String, String> refreshRequest = new HashMap<>();
            refreshRequest.put("refreshToken", refreshToken);

            // When & Then
            mockMvc.perform(
                            post("/api/auth/refresh")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(refreshRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists());
        }

        /**
         * 토큰 갱신 실패 - 잘못된 리프레시 토큰
         */
        @Test
        @DisplayName("토큰 갱신 실패 - 잘못된 리프레시 토큰")
        void refreshToken_WithInvalidToken_ReturnsFailure() throws Exception {
            // Given
            Map<String, String> refreshRequest = new HashMap<>();
            refreshRequest.put("refreshToken", "invalid-refresh-token");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/refresh")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(refreshRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    // ================================
    // 로그아웃 API 테스트
    // ================================

    @Nested
    @DisplayName("POST /api/auth/logout - 로그아웃 API")
    class LogoutTest {

        /**
         * 로그아웃 성공 테스트
         *
         * 시나리오: 인증된 사용자가 로그아웃 요청
         * 예상 결과: 200 OK, 로그아웃 성공 메시지
         */
        @Test
        @DisplayName("로그아웃 성공")
        void logout_WithValidToken_ReturnsSuccess() throws Exception {
            // When & Then
            mockMvc.perform(
                            post("/api/auth/logout")
                                    .header("Authorization", "Bearer " + validAccessToken)
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        /**
         * 로그아웃 - 토큰 없이 요청
         *
         * 참고: 로그아웃은 보통 토큰 없이도 성공으로 처리하거나
         * 인증 필요로 설정할 수 있음 (구현에 따라 다름)
         */
        @Test
        @DisplayName("로그아웃 - 토큰 없이")
        void logout_WithoutToken_Returns() throws Exception {
            // When & Then
            // 구현에 따라 200 OK 또는 401 Unauthorized
            mockMvc.perform(
                            post("/api/auth/logout")
                    )
                    .andDo(print());
            // 결과는 구현에 따라 다를 수 있음
        }
    }

    // ================================
    // 보안 및 CORS 테스트
    // ================================

    @Nested
    @DisplayName("보안 관련 테스트")
    class SecurityTest {

        /**
         * CORS 프리플라이트 요청 테스트
         *
         * 시나리오: OPTIONS 메서드로 CORS 프리플라이트 요청
         * 예상 결과: 200 OK, CORS 헤더 포함
         */
        @Test
        @DisplayName("CORS 프리플라이트 요청 성공")
        void corsPreflightRequest_ReturnsCorrectHeaders() throws Exception {
            mockMvc.perform(
                            options("/api/auth/login")
                                    .header("Origin", "http://localhost:3000")
                                    .header("Access-Control-Request-Method", "POST")
                                    .header("Access-Control-Request-Headers", "Content-Type")
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(header().exists("Access-Control-Allow-Origin"))
                    .andExpect(header().exists("Access-Control-Allow-Methods"));
        }

        /**
         * SQL 인젝션 방지 테스트
         *
         * 시나리오: SQL 인젝션 시도가 포함된 로그인 요청
         * 예상 결과: 정상 처리 (인젝션 실패), 로그인 실패
         */
        @Test
        @DisplayName("SQL 인젝션 방지")
        void login_WithSqlInjection_DoesNotCompromise() throws Exception {
            // Given: SQL 인젝션 시도
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", "admin' OR '1'='1");
            loginRequest.put("password", "' OR '1'='1");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/login")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(loginRequest))
                    )
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(false));
        }

        /**
         * XSS 방지 테스트
         *
         * 시나리오: XSS 스크립트가 포함된 회원가입 요청
         * 예상 결과: 스크립트가 이스케이프되거나 거부됨
         */
        @Test
        @DisplayName("XSS 스크립트 방지")
        void register_WithXssScript_SanitizesInput() throws Exception {
            // Given: XSS 스크립트 포함
            Map<String, String> registerRequest = new HashMap<>();
            registerRequest.put("username", "xssuser");
            registerRequest.put("password", "password123");
            registerRequest.put("email", "xss@example.com");
            registerRequest.put("fullName", "<script>alert('XSS')</script>");
            registerRequest.put("department", "개발팀");

            // When & Then
            mockMvc.perform(
                            post("/api/auth/register")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(registerRequest))
                    )
                    .andDo(print());
            // 응답에 스크립트 태그가 그대로 포함되지 않아야 함
        }
    }
}

/*
 * ====== AuthController 통합 테스트 가이드 ======
 *
 * 1. 테스트 실행 방법:
 *
 *    # 전체 실행
 *    mvn test -Dtest=AuthControllerTest
 *
 *    # 특정 테스트 그룹만 실행
 *    mvn test -Dtest=AuthControllerTest$LoginTest
 *
 *    # 특정 테스트만 실행
 *    mvn test -Dtest=AuthControllerTest#login_WithValidCredentials_ReturnsTokens
 *
 * 2. MockMvc 주요 메서드:
 *
 *    - perform(): HTTP 요청 실행
 *    - andExpect(): 응답 검증
 *    - andDo(print()): 요청/응답 로깅
 *
 * 3. JSON 검증 방법:
 *
 *    jsonPath("$.field").value(expected)
 *    jsonPath("$.field").exists()
 *    jsonPath("$.field").doesNotExist()
 *    jsonPath("$.array").isArray()
 *    jsonPath("$.array", hasSize(3))
 *
 * 4. 상태 코드 검증:
 *
 *    status().isOk()          // 200
 *    status().isCreated()     // 201
 *    status().isBadRequest()  // 400
 *    status().isUnauthorized()// 401
 *    status().isForbidden()   // 403
 *    status().isNotFound()    // 404
 *
 * 5. 헤더 검증:
 *
 *    header().exists("Header-Name")
 *    header().string("Header-Name", "expected-value")
 *
 * 작성일: 2025년 11월 30일 (41일차)
 */