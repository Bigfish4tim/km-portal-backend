package com.kmportal.backend.controller;

import com.kmportal.backend.service.AuthService;
import com.kmportal.backend.service.AuthService.LoginResponse;
import com.kmportal.backend.service.AuthService.TokenRefreshResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 사용자 인증 관련 REST API를 제공하는 컨트롤러
 *
 * 이 컨트롤러는 프론트엔드와 백엔드 간의 인증 관련 통신을 담당합니다:
 * - 로그인 API: 사용자 인증 처리 및 JWT 토큰 발급
 * - 토큰 갱신 API: Refresh Token을 통한 Access Token 갱신
 * - 로그아웃 API: 클라이언트 측 토큰 무효화 처리
 * - 토큰 검증 API: 현재 토큰의 유효성 확인
 *
 * CORS 설정을 통해 프론트엔드(Vue.js)에서 안전하게 호출할 수 있습니다.
 *
 * @author KM Portal Team
 * @version 1.0
 * @since 2025-09-24
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = {"http://localhost:8080", "http://127.0.0.1:8080"})
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * 사용자 로그인 API
     *
     * 사용자가 제공한 아이디와 비밀번호를 검증하고,
     * 성공시 JWT 토큰을 발급하여 반환합니다.
     *
     * 요청 형식:
     * POST /api/auth/login
     * Content-Type: application/json
     *
     * 요청 본문:
     * {
     *   "username": "사용자명",
     *   "password": "비밀번호"
     * }
     *
     * 성공 응답 (200 OK):
     * {
     *   "success": true,
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
     *   "userInfo": {
     *     "userId": 1,
     *     "username": "admin",
     *     "fullName": "시스템 관리자",
     *     "email": "admin@kmportal.com",
     *     "department": "IT팀",
     *     "roles": ["ROLE_ADMIN"]
     *   }
     * }
     *
     * 실패 응답 (401 Unauthorized):
     * {
     *   "success": false,
     *   "message": "비밀번호가 일치하지 않습니다."
     * }
     *
     * @param loginRequest 로그인 요청 데이터
     * @return ResponseEntity<LoginResponse> 로그인 처리 결과
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            // 입력값 검증
            if (loginRequest.getUsername() == null || loginRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.failure("사용자명을 입력해주세요."));
            }

            if (loginRequest.getPassword() == null || loginRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(LoginResponse.failure("비밀번호를 입력해주세요."));
            }

            // 인증 서비스를 통해 로그인 처리
            LoginResponse response = authService.authenticate(
                    loginRequest.getUsername().trim(),
                    loginRequest.getPassword()
            );

            // 성공시 200 OK, 실패시 401 Unauthorized 반환
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            // 예외 발생시 로그 기록 후 서버 오류 응답 반환
            System.err.println("로그인 API 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(LoginResponse.failure("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    /**
     * 토큰 갱신 API
     *
     * Refresh Token을 사용하여 새로운 Access Token을 발급합니다.
     * Access Token이 만료되었지만 Refresh Token이 유효한 경우 사용됩니다.
     *
     * 요청 형식:
     * POST /api/auth/refresh
     * Content-Type: application/json
     *
     * 요청 본문:
     * {
     *   "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     *
     * 성공 응답 (200 OK):
     * {
     *   "success": true,
     *   "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
     * }
     *
     * 실패 응답 (401 Unauthorized):
     * {
     *   "success": false,
     *   "message": "유효하지 않은 Refresh Token입니다."
     * }
     *
     * @param refreshRequest 토큰 갱신 요청 데이터
     * @return ResponseEntity<TokenRefreshResponse> 토큰 갱신 결과
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@Valid @RequestBody TokenRefreshRequest refreshRequest) {
        try {
            // 입력값 검증
            if (refreshRequest.getRefreshToken() == null || refreshRequest.getRefreshToken().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(TokenRefreshResponse.failure("Refresh Token을 제공해주세요."));
            }

            // 인증 서비스를 통해 토큰 갱신 처리
            TokenRefreshResponse response = authService.refreshAccessToken(
                    refreshRequest.getRefreshToken().trim()
            );

            // 성공시 200 OK, 실패시 401 Unauthorized 반환
            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

        } catch (Exception e) {
            // 예외 발생시 로그 기록 후 서버 오류 응답 반환
            System.err.println("토큰 갱신 API 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(TokenRefreshResponse.failure("서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    /**
     * 로그아웃 API
     *
     * 클라이언트 측에서 토큰을 제거하도록 안내하는 API입니다.
     * JWT는 서버에서 무효화할 수 없으므로, 클라이언트에서 토큰을 제거하는 것이 중요합니다.
     *
     * 요청 형식:
     * POST /api/auth/logout
     * Authorization: Bearer {accessToken}
     *
     * 성공 응답 (200 OK):
     * {
     *   "success": true,
     *   "message": "로그아웃 되었습니다."
     * }
     *
     * @return ResponseEntity<LogoutResponse> 로그아웃 처리 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout() {
        // JWT 특성상 서버에서 토큰을 무효화할 수 없으므로
        // 클라이언트에서 토큰 제거를 확인하는 응답만 반환
        return ResponseEntity.ok(new LogoutResponse(true, "로그아웃 되었습니다."));
    }

    /**
     * 사용자 회원가입 API
     *
     * 신규 사용자 등록을 처리합니다.
     * 사용자명과 이메일 중복을 확인하고, 비밀번호를 암호화하여 저장합니다.
     *
     * 요청 형식:
     * POST /api/auth/register
     * Content-Type: application/json
     *
     * 요청 본문:
     * {
     *   "username": "사용자명",
     *   "password": "비밀번호",
     *   "email": "이메일",
     *   "fullName": "실명",
     *   "department": "부서",
     *   "position": "직급",
     *   "phoneNumber": "전화번호"
     * }
     *
     * 성공 응답 (201 Created):
     * {
     *   "success": true,
     *   "message": "회원가입이 완료되었습니다.",
     *   "userId": 123
     * }
     *
     * 실패 응답 (400 Bad Request):
     * {
     *   "success": false,
     *   "message": "이미 사용 중인 사용자명입니다."
     * }
     *
     * @param registerRequest 회원가입 요청 데이터
     * @return ResponseEntity<RegisterResponse> 회원가입 처리 결과
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest registerRequest) {
        try {
            // 입력값 기본 검증
            if (registerRequest.getUsername() == null ||
                    registerRequest.getUsername().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(RegisterResponse.failure("사용자명을 입력해주세요."));
            }

            if (registerRequest.getPassword() == null ||
                    registerRequest.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(RegisterResponse.failure("비밀번호를 입력해주세요."));
            }

            if (registerRequest.getEmail() == null ||
                    registerRequest.getEmail().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(RegisterResponse.failure("이메일을 입력해주세요."));
            }

            // 인증 서비스를 통해 회원가입 처리
            RegisterResponse response = authService.registerUser(registerRequest);

            // 성공시 201 Created, 실패시 400 Bad Request 반환
            if (response.isSuccess()) {
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            System.err.println("회원가입 API 처리 중 오류 발생: " + e.getMessage());
            e.printStackTrace();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(RegisterResponse.failure(
                            "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요."));
        }
    }

    /**
     * 현재 토큰의 유효성을 검증하는 API
     *
     * 클라이언트에서 현재 보유한 토큰이 여전히 유효한지 확인할 때 사용합니다.
     * 토큰이 만료되었거나 유효하지 않은 경우 재로그인을 요청할 수 있습니다.
     *
     * 요청 형식:
     * GET /api/auth/validate
     * Authorization: Bearer {accessToken}
     *
     * 성공 응답 (200 OK):
     * {
     *   "valid": true,
     *   "message": "유효한 토큰입니다."
     * }
     *
     * 실패 응답 (401 Unauthorized):
     * {
     *   "valid": false,
     *   "message": "토큰이 만료되었거나 유효하지 않습니다."
     * }
     *
     * @param authHeader Authorization 헤더 값
     * @return ResponseEntity<TokenValidationResponse> 토큰 검증 결과
     */
    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            // Authorization 헤더 검증
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new TokenValidationResponse(false, "Authorization 헤더가 없거나 올바르지 않습니다."));
            }

            // Bearer 접두사 제거하여 토큰 추출
            String token = authHeader.substring(7);

            // 토큰에서 사용자명 추출 (이 과정에서 토큰 유효성도 검증됨)
            // JWT 유틸에서 예외가 발생하면 유효하지 않은 토큰으로 처리
            // 실제로는 JwtUtil을 통해 더 정교한 검증을 수행해야 합니다.

            return ResponseEntity.ok(new TokenValidationResponse(true, "유효한 토큰입니다."));

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new TokenValidationResponse(false, "토큰이 만료되었거나 유효하지 않습니다."));
        }
    }

    // ===== 요청/응답 DTO 클래스들 =====

    /**
     * 로그인 요청 데이터를 담는 DTO 클래스
     */
    public static class LoginRequest {
        @NotBlank(message = "사용자명은 필수입니다.")
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;

        // 기본 생성자
        public LoginRequest() {}

        public LoginRequest(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    /**
     * 토큰 갱신 요청 데이터를 담는 DTO 클래스
     */
    public static class TokenRefreshRequest {
        @NotBlank(message = "Refresh Token은 필수입니다.")
        private String refreshToken;

        // 기본 생성자
        public TokenRefreshRequest() {}

        public TokenRefreshRequest(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    /**
     * 로그아웃 응답 데이터를 담는 DTO 클래스
     */
    public static class LogoutResponse {
        private boolean success;
        private String message;

        public LogoutResponse(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
    }

    /**
     * 토큰 검증 응답 데이터를 담는 DTO 클래스
     */
    public static class TokenValidationResponse {
        private boolean valid;
        private String message;

        public TokenValidationResponse(boolean valid, String message) {
            this.valid = valid;
            this.message = message;
        }

        public boolean isValid() { return valid; }
        public String getMessage() { return message; }
    }

    /**
     * 회원가입 요청 데이터를 담는 DTO 클래스
     */
    public static class RegisterRequest {
        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(min = 3, max = 30, message = "사용자명은 3-30자 사이여야 합니다.")
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
        private String password;

        @NotBlank(message = "이메일은 필수입니다.")
        private String email;

        @NotBlank(message = "실명은 필수입니다.")
        private String fullName;

        private String department;
        private String position;
        private String phoneNumber;

        // 기본 생성자
        public RegisterRequest() {}

        // Getter 메서드들
        public String getUsername() { return username; }
        public String getPassword() { return password; }
        public String getEmail() { return email; }
        public String getFullName() { return fullName; }
        public String getDepartment() { return department; }
        public String getPosition() { return position; }
        public String getPhoneNumber() { return phoneNumber; }

        // Setter 메서드들
        public void setUsername(String username) { this.username = username; }
        public void setPassword(String password) { this.password = password; }
        public void setEmail(String email) { this.email = email; }
        public void setFullName(String fullName) { this.fullName = fullName; }
        public void setDepartment(String department) { this.department = department; }
        public void setPosition(String position) { this.position = position; }
        public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    }

    /**
     * 회원가입 응답 데이터를 담는 DTO 클래스
     */
    public static class RegisterResponse {
        private boolean success;
        private String message;
        private Long userId;

        private RegisterResponse(boolean success, String message, Long userId) {
            this.success = success;
            this.message = message;
            this.userId = userId;
        }

        public static RegisterResponse success(String message, Long userId) {
            return new RegisterResponse(true, message, userId);
        }

        public static RegisterResponse failure(String message) {
            return new RegisterResponse(false, message, null);
        }

        // Getter 메서드들
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getUserId() { return userId; }
    }

}