package com.kmportal.backend.service;

import com.kmportal.backend.entity.User;
import com.kmportal.backend.entity.Role;
import com.kmportal.backend.repository.UserRepository;
import com.kmportal.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 사용자 인증 관련 비즈니스 로직을 처리하는 서비스 클래스
 *
 * 이 서비스는 사용자의 로그인, 토큰 발급, 계정 상태 확인 등
 * 인증과 관련된 모든 핵심 기능을 담당합니다.
 *
 * 주요 기능:
 * - 사용자 로그인 검증
 * - JWT 토큰 생성 및 관리
 * - 계정 상태 확인 (활성/비활성, 잠금 상태)
 * - 로그인 실패 횟수 관리
 * - Refresh Token을 통한 토큰 갱신
 *
 * @author KM Portal Team
 * @version 1.0
 * @since 2025-09-24
 */
@Service
@Transactional
public class AuthService {

    // 최대 로그인 실패 허용 횟수
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 사용자 로그인을 처리하는 메서드
     *
     * 사용자명과 비밀번호를 검증하고, 성공시 JWT 토큰을 포함한
     * 로그인 응답 객체를 반환합니다. 실패시 적절한 에러 메시지를 반환합니다.
     *
     * @param username 로그인할 사용자명
     * @param password 사용자가 입력한 비밀번호 (평문)
     * @return LoginResponse 로그인 결과 정보 (토큰, 사용자 정보, 성공/실패 여부)
     */
    public LoginResponse authenticate(String username, String password) {
        try {
            System.out.println("=================================");
            System.out.println("🔐 로그인 인증 시작");
            System.out.println("=================================");

            // 입력값 로깅
            System.out.println("📋 입력 데이터:");
            System.out.println("   - Username: [" + username + "]");
            System.out.println("   - Username 길이: " + username.length());
            System.out.println("   - Password: [" + password + "]");  // ⚠️ 개발 환경에서만!
            System.out.println("   - Password 길이: " + password.length());
            System.out.println("   - Password 첫 문자 ASCII: " + (int) password.charAt(0));
            System.out.println("   - Password 마지막 문자 ASCII: " + (int) password.charAt(password.length() - 1));

            // 1. 사용자 존재 여부 확인
            System.out.println("\n📂 사용자 조회 중...");
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                System.out.println("❌ 사용자 없음: " + username);
                return LoginResponse.failure("존재하지 않는 사용자입니다.");
            }

            User user = userOpt.get();
            System.out.println("✅ 사용자 조회 성공");
            System.out.println("   - User ID: " + user.getUserId());
            System.out.println("   - Username: " + user.getUsername());
            System.out.println("   - Is Active: " + user.getIsActive());
            System.out.println("   - Is Locked: " + user.getIsLocked());
            System.out.println("   - Failed Attempts: " + user.getFailedLoginAttempts());
            System.out.println("   - Stored Password Hash: " + user.getPassword());

            // 2. 계정 상태 검증
            System.out.println("\n🔍 계정 상태 검증 중...");
            AccountStatusCheck statusCheck = checkAccountStatus(user);
            if (!statusCheck.isValid()) {
                System.out.println("❌ 계정 상태 오류: " + statusCheck.getMessage());
                return LoginResponse.failure(statusCheck.getMessage());
            }
            System.out.println("✅ 계정 상태 정상");

            // 3. 비밀번호 검증
            System.out.println("\n🔐 비밀번호 검증 중...");
            System.out.println("   - 입력 비밀번호 (평문): [" + password + "]");
            System.out.println("   - 저장된 해시: " + user.getPassword());

            boolean matches = passwordEncoder.matches(password, user.getPassword());
            System.out.println("   - 비밀번호 일치 여부: " + matches);

            if (!matches) {
                System.out.println("❌ 비밀번호 불일치!");

                // 실패 횟수 증가
                incrementFailedAttempts(user);
                System.out.println("   - 실패 횟수 증가됨: " + (user.getFailedLoginAttempts() + 1));

                return LoginResponse.failure("비밀번호가 일치하지 않습니다.");
            }

            System.out.println("✅ 비밀번호 검증 성공!");

            // 4. 로그인 성공 처리
            System.out.println("\n🎉 로그인 성공 처리 중...");
            handleSuccessfulLogin(user);

            // 5. JWT 토큰 생성
            String accessToken = generateAccessToken(user);
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            System.out.println("✅ 토큰 생성 완료");
            System.out.println("=================================");
            System.out.println("🎉 로그인 인증 완료!");
            System.out.println("=================================\n");

            // 6. 성공 응답 생성
            return LoginResponse.success(accessToken, refreshToken, user);

        } catch (Exception e) {
            System.err.println("❌ 로그인 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return LoginResponse.failure("시스템 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    /**
     * 계정 상태를 확인하는 내부 메서드
     *
     * 사용자 계정이 로그인 가능한 상태인지 다음 항목들을 검증:
     * - 계정 활성화 상태
     * - 계정 잠금 상태
     * - 로그인 실패 횟수 초과 여부
     *
     * @param user 검증할 사용자 객체
     * @return AccountStatusCheck 계정 상태 검증 결과
     */
    private AccountStatusCheck checkAccountStatus(User user) {
        // 비활성 계정 확인
        if (!user.getIsActive()) {
            return AccountStatusCheck.invalid("비활성화된 계정입니다. 관리자에게 문의하세요.");
        }

        // 잠긴 계정 확인
        if (user.getIsLocked()) {
            return AccountStatusCheck.invalid("잠긴 계정입니다. 관리자에게 문의하세요.");
        }

        // 로그인 실패 횟수 초과 확인
        if (user.getFailedLoginAttempts() >= MAX_LOGIN_ATTEMPTS) {
            // 자동으로 계정을 잠금 처리
            user.setIsLocked(true);
            user.setLockedAt(LocalDateTime.now());
            userRepository.save(user);

            return AccountStatusCheck.invalid(
                    String.format("로그인 실패 횟수가 %d회를 초과하여 계정이 잠겼습니다.", MAX_LOGIN_ATTEMPTS)
            );
        }

        return AccountStatusCheck.valid();
    }

    /**
     * 로그인 실패 횟수를 증가시키는 메서드
     *
     * @param user 실패 횟수를 증가시킜 사용자
     */
    private void incrementFailedAttempts(User user) {
        int newAttempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(newAttempts);

        // 최대 허용 횟수 도달시 계정 잠금
        if (newAttempts >= MAX_LOGIN_ATTEMPTS) {
            user.setIsLocked(true);
            user.setLockedAt(LocalDateTime.now());
        }

        userRepository.save(user);
    }

    /**
     * 로그인 성공시 후처리를 담당하는 메서드
     *
     * - 로그인 실패 횟수 초기화
     * - 마지막 로그인 시간 업데이트
     *
     * @param user 로그인에 성공한 사용자
     */
    private void handleSuccessfulLogin(User user) {
        // 로그인 실패 횟수 초기화
        user.setFailedLoginAttempts(0);

        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());

        userRepository.save(user);
    }

    /**
     * 사용자 정보를 기반으로 Access Token을 생성
     *
     * @param user 토큰을 생성할 사용자
     * @return String 생성된 Access Token
     */
    private String generateAccessToken(User user) {
        // 사용자의 역할 목록을 문자열 리스트로 변환
        List<String> roleNames = user.getRoles().stream()
                .map(Role::getRoleName)
                .collect(Collectors.toList());

        return jwtUtil.generateToken(
                user.getUsername(),
                user.getFullName(),
                user.getEmail(),
                user.getDepartment(),
                roleNames
        );
    }

    /**
     * Refresh Token을 사용하여 새로운 Access Token을 발급
     *
     * @param refreshToken 갱신용 토큰
     * @return TokenRefreshResponse 토큰 갱신 결과
     */
    public TokenRefreshResponse refreshAccessToken(String refreshToken) {
        try {
            // Refresh Token에서 사용자명 추출
            String username = jwtUtil.extractUsername(refreshToken);

            // 사용자 정보 조회
            Optional<User> userOpt = userRepository.findByUsername(username);
            if (!userOpt.isPresent()) {
                return TokenRefreshResponse.failure("사용자를 찾을 수 없습니다.");
            }

            User user = userOpt.get();

            // 계정 상태 재확인
            AccountStatusCheck statusCheck = checkAccountStatus(user);
            if (!statusCheck.isValid()) {
                return TokenRefreshResponse.failure(statusCheck.getMessage());
            }

            // Refresh Token 유효성 검증
            if (!jwtUtil.validateToken(refreshToken, username)) {
                return TokenRefreshResponse.failure("유효하지 않은 Refresh Token입니다.");
            }

            // 새로운 Access Token 생성
            String newAccessToken = generateAccessToken(user);

            return TokenRefreshResponse.success(newAccessToken);

        } catch (Exception e) {
            System.err.println("토큰 갱신 중 오류 발생: " + e.getMessage());
            return TokenRefreshResponse.failure("토큰 갱신에 실패했습니다.");
        }
    }

    // ===== 내부 클래스들 =====

    /**
     * 계정 상태 검증 결과를 담는 내부 클래스
     */
    private static class AccountStatusCheck {
        private final boolean valid;
        private final String message;

        private AccountStatusCheck(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public static AccountStatusCheck valid() {
            return new AccountStatusCheck(true, null);
        }

        public static AccountStatusCheck invalid(String message) {
            return new AccountStatusCheck(false, message);
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    /**
     * 로그인 응답을 담는 정적 클래스
     */
    public static class LoginResponse {
        private boolean success;
        private String message;
        private String accessToken;
        private String refreshToken;
        private UserInfo userInfo;

        // 생성자들
        private LoginResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        private LoginResponse(boolean success, String accessToken, String refreshToken, UserInfo userInfo) {
            this.success = success;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.userInfo = userInfo;
        }

        public static LoginResponse success(String accessToken, String refreshToken, User user) {
            UserInfo userInfo = new UserInfo(
                    user.getUserId(),
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getDepartment(),
                    user.getRoles().stream().map(Role::getRoleName).collect(Collectors.toList())
            );
            return new LoginResponse(true, accessToken, refreshToken, userInfo);
        }

        public static LoginResponse failure(String message) {
            return new LoginResponse(false, message);
        }

        // Getter 메서드들
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getAccessToken() { return accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public UserInfo getUserInfo() { return userInfo; }
    }

    /**
     * 토큰 갱신 응답을 담는 정적 클래스
     */
    public static class TokenRefreshResponse {
        private boolean success;
        private String message;
        private String accessToken;

        private TokenRefreshResponse(boolean success, String message, String accessToken) {
            this.success = success;
            this.message = message;
            this.accessToken = accessToken;
        }

        public static TokenRefreshResponse success(String accessToken) {
            return new TokenRefreshResponse(true, null, accessToken);
        }

        public static TokenRefreshResponse failure(String message) {
            return new TokenRefreshResponse(false, message, null);
        }

        // Getter 메서드들
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getAccessToken() { return accessToken; }
    }

    /**
     * 사용자 정보를 담는 정적 클래스
     */
    public static class UserInfo {
        private Long userId;
        private String username;
        private String fullName;
        private String email;
        private String department;
        private List<String> roles;

        public UserInfo(Long userId, String username, String fullName, String email,
                        String department, List<String> roles) {
            this.userId = userId;
            this.username = username;
            this.fullName = fullName;
            this.email = email;
            this.department = department;
            this.roles = roles;
        }

        // Getter 메서드들
        public Long getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getFullName() { return fullName; }
        public String getEmail() { return email; }
        public String getDepartment() { return department; }
        public List<String> getRoles() { return roles; }
    }
}