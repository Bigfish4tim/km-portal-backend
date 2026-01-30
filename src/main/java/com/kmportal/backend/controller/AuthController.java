/**
 * =============================================================================
 * 📁 AuthController.java - 인증 API 컨트롤러 (2일차 수정 버전 v2.3)
 * =============================================================================
 *
 * 사용자 인증 관련 API를 제공하는 컨트롤러입니다.
 *
 * 【버전 히스토리】
 * - v2.0 (2일차): RegisterRequest에 roleName 필드 추가, 12개 Role 시스템 반영
 * - v2.1: RegisterResponse 내부 클래스 추가 (AuthService 호환)
 * - v2.2: ApiResponse.error() → failure() 수정, JwtUtil 메서드 호환성 수정
 * - v2.3: 람다 표현식 final 변수 문제 해결
 *
 * ■ API 목록:
 *   - POST /api/auth/login    : 로그인
 *   - POST /api/auth/register : 회원가입 【2일차 수정】
 *   - POST /api/auth/refresh  : 토큰 갱신
 *   - GET  /api/auth/me       : 내 정보 조회
 *   - POST /api/auth/logout   : 로그아웃
 *
 * @author KM Portal Team
 * @version 2.3 (람다 final 변수 문제 해결)
 * @since 2025-09-24
 * @modified 2026-01-30 - 컴파일 오류 해결
 */
package com.kmportal.backend.controller;

import com.kmportal.backend.dto.common.ApiResponse;
import com.kmportal.backend.entity.Role;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.RoleRepository;
import com.kmportal.backend.repository.UserRepository;
import com.kmportal.backend.service.AuthService;
import com.kmportal.backend.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    // =========================================================================
    // 상수 정의
    // =========================================================================

    /**
     * Access Token 유효 시간 (초 단위)
     * JwtUtil에는 getExpirationTime() 메서드가 없으므로 상수로 정의
     * 24시간 = 86400초
     */
    private static final long TOKEN_EXPIRATION_SECONDS = 86400;

    // =========================================================================
    // 12개 Role 시스템 - 유효한 Role 목록 【2일차 추가】
    // =========================================================================

    /**
     * 【2일차 추가】 유효한 Role 이름 목록
     *
     * 회원가입 시 전송된 roleName이 유효한지 검증하는 데 사용됩니다.
     *
     * ■ 12개 Role 구조:
     *   - 관리: ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
     *   - 임원: ROLE_EXECUTIVE_ALL, ROLE_EXECUTIVE_TYPE1, ROLE_EXECUTIVE_TYPE4
     *   - 팀장: ROLE_TEAM_LEADER_ALL, ROLE_TEAM_LEADER_TYPE1, ROLE_TEAM_LEADER_TYPE4
     *   - 조사자: ROLE_INVESTIGATOR_ALL, ROLE_INVESTIGATOR_TYPE1, ROLE_INVESTIGATOR_TYPE4
     *   - 기타: ROLE_EMPLOYEE
     */
    private static final Set<String> VALID_ROLE_NAMES = Set.of(
            "ROLE_ADMIN",
            "ROLE_BUSINESS_SUPPORT",
            "ROLE_EXECUTIVE_ALL",
            "ROLE_EXECUTIVE_TYPE1",
            "ROLE_EXECUTIVE_TYPE4",
            "ROLE_TEAM_LEADER_ALL",
            "ROLE_TEAM_LEADER_TYPE1",
            "ROLE_TEAM_LEADER_TYPE4",
            "ROLE_INVESTIGATOR_ALL",
            "ROLE_INVESTIGATOR_TYPE1",
            "ROLE_INVESTIGATOR_TYPE4",
            "ROLE_EMPLOYEE"
    );

    /**
     * 【2일차 추가】 일반 사용자가 회원가입 시 선택 가능한 Role 목록
     *
     * 관리자/경영지원/임원/팀장 역할은 관리자만 부여할 수 있습니다.
     */
    private static final Set<String> SELF_ASSIGNABLE_ROLES = Set.of(
            "ROLE_INVESTIGATOR_ALL",
            "ROLE_INVESTIGATOR_TYPE1",
            "ROLE_INVESTIGATOR_TYPE4",
            "ROLE_EMPLOYEE"
    );

    // =========================================================================
    // API 엔드포인트
    // =========================================================================

    /**
     * 로그인 API
     *
     * @param request 로그인 요청 (username, password)
     * @return JWT 토큰 및 사용자 정보
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("로그인 시도: {}", request.getUsername());

        try {
            // 인증 시도
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            // 인증 성공 - SecurityContext에 저장
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // 사용자 정보 조회
            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadCredentialsException("사용자를 찾을 수 없습니다."));

            // 계정 상태 확인
            if (!user.getIsActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.failure("비활성화된 계정입니다. 관리자에게 문의하세요."));
            }

            if (user.getIsLocked()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.failure("잠긴 계정입니다. 관리자에게 문의하세요."));
            }

            // JWT 토큰 생성 - JwtUtil의 실제 메서드 시그니처 사용
            // jwtUtil.generateToken(String username, String fullName, String email, String department, List<String> roles)
            List<String> roleNames = user.getRoles().stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toList());

            String accessToken = jwtUtil.generateToken(
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getDepartment(),
                    roleNames
            );
            String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());

            // 마지막 로그인 시간 업데이트
            user.setLastLoginAt(LocalDateTime.now());
            userRepository.save(user);

            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", accessToken);
            responseData.put("refreshToken", refreshToken);
            responseData.put("tokenType", "Bearer");
            responseData.put("expiresIn", TOKEN_EXPIRATION_SECONDS);  // 상수 사용
            responseData.put("user", buildUserResponse(user));

            log.info("로그인 성공: {}", request.getUsername());
            return ResponseEntity.ok(ApiResponse.success(responseData, "로그인 성공"));

        } catch (BadCredentialsException e) {
            log.warn("로그인 실패 - 잘못된 자격증명: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("아이디 또는 비밀번호가 올바르지 않습니다."));

        } catch (AuthenticationException e) {
            log.warn("로그인 실패 - 인증 오류: {} - {}", request.getUsername(), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("인증에 실패했습니다. 다시 시도해주세요."));
        }
    }

    /**
     * 회원가입 API
     *
     * 【2일차 수정】 roleName 필드 추가로 회원가입 시 역할 지정 가능
     *
     * @param request 회원가입 요청 (username, email, password, fullName, department, position, roleName 등)
     * @return 생성된 사용자 정보
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<Map<String, Object>>> register(
            @Valid @RequestBody RegisterRequest request) {

        log.info("회원가입 요청: username={}, email={}, roleName={}",
                request.getUsername(), request.getEmail(), request.getRoleName());

        try {
            // 1. 사용자명 중복 확인
            if (userRepository.existsByUsername(request.getUsername())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.failure("이미 사용 중인 사용자명입니다."));
            }

            // 2. 이메일 중복 확인
            if (userRepository.existsByEmail(request.getEmail())) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(ApiResponse.failure("이미 사용 중인 이메일입니다."));
            }

            // ===============================================================
            // 【2일차 추가】 Role 처리 로직
            // ===============================================================

            // 3. roleName 검증 및 기본값 설정
            String roleName = request.getRoleName();

            // roleName이 없거나 빈 문자열이면 기본값(ROLE_EMPLOYEE) 사용
            if (roleName == null || roleName.trim().isEmpty()) {
                roleName = "ROLE_EMPLOYEE";
                log.info("roleName 미지정 - 기본값 사용: {}", roleName);
            }

            // 4. roleName 유효성 검증
            if (!VALID_ROLE_NAMES.contains(roleName)) {
                log.warn("유효하지 않은 roleName: {}", roleName);
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(ApiResponse.failure("유효하지 않은 역할입니다: " + roleName));
            }

            // 5. 일반 회원가입에서는 상위 역할 제한 (선택적 적용)
            // 참고: 운영 환경에서는 아래 주석을 해제하여 상위 역할 자가 할당 방지
            /*
            if (!SELF_ASSIGNABLE_ROLES.contains(roleName)) {
                log.warn("자가 할당 불가 역할 시도: {}", roleName);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.failure("해당 역할은 관리자만 부여할 수 있습니다: " + roleName));
            }
            */

            // 【v2.3 수정】 람다 표현식에서 사용할 final 변수
            // 람다 내에서 참조하는 변수는 final이거나 effectively final이어야 함
            final String finalRoleName = roleName;

            // 6. Role 엔티티 조회
            Role role = roleRepository.findByRoleName(finalRoleName)
                    .orElseThrow(() -> {
                        log.error("Role 조회 실패: {}", finalRoleName);
                        return new RuntimeException("역할을 찾을 수 없습니다: " + finalRoleName);
                    });

            // ===============================================================
            // 사용자 생성
            // ===============================================================

            // 7. 새 사용자 생성
            User newUser = new User();
            newUser.setUsername(request.getUsername());
            newUser.setEmail(request.getEmail());
            newUser.setPassword(passwordEncoder.encode(request.getPassword()));
            newUser.setFullName(request.getFullName());
            newUser.setDepartment(request.getDepartment());
            newUser.setPosition(request.getPosition());
            newUser.setPhoneNumber(request.getPhoneNumber());

            // 【2일차 수정】 선택된 Role 할당
            newUser.setRoles(new HashSet<>(Collections.singletonList(role)));

            // 기본 상태 설정
            newUser.setIsActive(true);  // 개발 환경에서는 바로 활성화
            newUser.setIsLocked(false);

            // 8. 사용자 저장
            User savedUser = userRepository.save(newUser);

            // 9. 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("userId", savedUser.getUserId());
            responseData.put("username", savedUser.getUsername());
            responseData.put("email", savedUser.getEmail());
            responseData.put("fullName", savedUser.getFullName());
            responseData.put("department", savedUser.getDepartment());
            responseData.put("position", savedUser.getPosition());
            // 【2일차 추가】 할당된 역할 정보
            responseData.put("roleName", finalRoleName);
            responseData.put("roleDisplayName", role.getDisplayName());
            responseData.put("createdAt", savedUser.getCreatedAt());

            log.info("회원가입 완료: username={}, role={}", savedUser.getUsername(), finalRoleName);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(responseData, "회원가입이 완료되었습니다."));

        } catch (Exception e) {
            log.error("회원가입 실패: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("회원가입 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 토큰 갱신 API
     *
     * @param request 갱신 요청 (refreshToken)
     * @return 새로운 액세스 토큰
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {

        log.debug("토큰 갱신 요청");

        try {
            // Refresh 토큰에서 사용자명 추출 (검증 전)
            String refreshToken = request.getRefreshToken();
            String username;

            try {
                username = jwtUtil.extractUsername(refreshToken);
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.failure("유효하지 않은 리프레시 토큰입니다."));
            }

            // Refresh 토큰 검증 - validateToken은 2개 파라미터 필요
            if (!jwtUtil.validateToken(refreshToken, username)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.failure("만료되었거나 유효하지 않은 리프레시 토큰입니다."));
            }

            // 사용자 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 새 액세스 토큰 생성 - JwtUtil의 실제 메서드 사용
            List<String> roleNames = user.getRoles().stream()
                    .map(Role::getRoleName)
                    .collect(Collectors.toList());

            String newAccessToken = jwtUtil.generateToken(
                    user.getUsername(),
                    user.getFullName(),
                    user.getEmail(),
                    user.getDepartment(),
                    roleNames
            );

            // 응답 데이터 구성
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", newAccessToken);
            responseData.put("tokenType", "Bearer");
            responseData.put("expiresIn", TOKEN_EXPIRATION_SECONDS);  // 상수 사용

            return ResponseEntity.ok(ApiResponse.success(responseData, "토큰 갱신 성공"));

        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.failure("토큰 갱신에 실패했습니다."));
        }
    }

    /**
     * 내 정보 조회 API
     *
     * @param request HTTP 요청 (Authorization 헤더에서 토큰 추출)
     * @return 현재 로그인한 사용자 정보
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMyInfo(HttpServletRequest request) {

        try {
            // 인증 정보에서 사용자명 추출
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.failure("인증이 필요합니다."));
            }

            String username = authentication.getName();

            // 사용자 조회
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다."));

            // 응답 데이터 구성
            Map<String, Object> responseData = buildUserResponse(user);

            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (Exception e) {
            log.error("내 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("정보 조회에 실패했습니다."));
        }
    }

    /**
     * 로그아웃 API
     *
     * @return 로그아웃 결과
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {

        try {
            // SecurityContext 초기화
            SecurityContextHolder.clearContext();

            log.info("로그아웃 완료");
            return ResponseEntity.ok(ApiResponse.success(null, "로그아웃 되었습니다."));

        } catch (Exception e) {
            log.error("로그아웃 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.failure("로그아웃에 실패했습니다."));
        }
    }

    // =========================================================================
    // 헬퍼 메서드
    // =========================================================================

    /**
     * 사용자 정보를 Map으로 변환
     *
     * @param user 사용자 엔티티
     * @return 사용자 정보 Map
     */
    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("userId", user.getUserId());
        userMap.put("username", user.getUsername());
        userMap.put("email", user.getEmail());
        userMap.put("fullName", user.getFullName());
        userMap.put("department", user.getDepartment());
        userMap.put("position", user.getPosition());
        userMap.put("phoneNumber", user.getPhoneNumber());
        userMap.put("isActive", user.getIsActive());
        userMap.put("isLocked", user.getIsLocked());
        userMap.put("lastLoginAt", user.getLastLoginAt());
        userMap.put("createdAt", user.getCreatedAt());

        // 【2일차 수정】 역할 정보 포함 (12개 Role 시스템)
        // roles 배열과 함께 주요 역할 정보도 반환
        List<Map<String, Object>> rolesList = user.getRoles().stream()
                .map(role -> {
                    Map<String, Object> roleMap = new HashMap<>();
                    roleMap.put("roleId", role.getRoleId());
                    roleMap.put("roleName", role.getRoleName());
                    roleMap.put("displayName", role.getDisplayName());
                    roleMap.put("priority", role.getPriority());
                    return roleMap;
                })
                .sorted((a, b) -> ((Integer) a.get("priority")).compareTo((Integer) b.get("priority")))
                .collect(Collectors.toList());

        userMap.put("roles", rolesList);

        // 주요 역할 (가장 높은 우선순위)
        if (!rolesList.isEmpty()) {
            userMap.put("primaryRole", rolesList.get(0).get("roleName"));
            userMap.put("primaryRoleDisplayName", rolesList.get(0).get("displayName"));
        }

        return userMap;
    }

    // =========================================================================
    // 요청/응답 DTO 클래스
    // =========================================================================

    /**
     * 로그인 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class LoginRequest {

        @NotBlank(message = "사용자명을 입력해주세요")
        private String username;

        @NotBlank(message = "비밀번호를 입력해주세요")
        private String password;
    }

    /**
     * 회원가입 요청 DTO
     *
     * 【2일차 수정】 roleName 필드 추가
     *
     * ■ 12개 유효한 Role:
     *   - ROLE_ADMIN: 관리자
     *   - ROLE_BUSINESS_SUPPORT: 경영지원
     *   - ROLE_EXECUTIVE_ALL: 임원(1/4종)
     *   - ROLE_EXECUTIVE_TYPE1: 임원(1종)
     *   - ROLE_EXECUTIVE_TYPE4: 임원(4종)
     *   - ROLE_TEAM_LEADER_ALL: 팀장(1/4종)
     *   - ROLE_TEAM_LEADER_TYPE1: 팀장(1종)
     *   - ROLE_TEAM_LEADER_TYPE4: 팀장(4종)
     *   - ROLE_INVESTIGATOR_ALL: 조사자(1/4종)
     *   - ROLE_INVESTIGATOR_TYPE1: 조사자(1종)
     *   - ROLE_INVESTIGATOR_TYPE4: 조사자(4종)
     *   - ROLE_EMPLOYEE: 일반사원 (기본값)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RegisterRequest {

        @NotBlank(message = "사용자명을 입력해주세요")
        @Size(min = 3, max = 50, message = "사용자명은 3-50자여야 합니다")
        private String username;

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        private String email;

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
        private String password;

        @NotBlank(message = "실명을 입력해주세요")
        @Size(min = 2, max = 100, message = "실명은 2-100자여야 합니다")
        private String fullName;

        private String department;

        private String position;

        private String phoneNumber;

        /**
         * 【2일차 추가】 역할 이름
         *
         * 선택사항이며, 미지정 시 ROLE_EMPLOYEE(일반사원)가 할당됩니다.
         *
         * 유효한 값:
         * - ROLE_ADMIN, ROLE_BUSINESS_SUPPORT
         * - ROLE_EXECUTIVE_ALL, ROLE_EXECUTIVE_TYPE1, ROLE_EXECUTIVE_TYPE4
         * - ROLE_TEAM_LEADER_ALL, ROLE_TEAM_LEADER_TYPE1, ROLE_TEAM_LEADER_TYPE4
         * - ROLE_INVESTIGATOR_ALL, ROLE_INVESTIGATOR_TYPE1, ROLE_INVESTIGATOR_TYPE4
         * - ROLE_EMPLOYEE (기본값)
         */
        private String roleName;
    }

    /**
     * 토큰 갱신 요청 DTO
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RefreshTokenRequest {

        @NotBlank(message = "리프레시 토큰을 입력해주세요")
        private String refreshToken;
    }

    // =========================================================================
    // 【추가】 RegisterResponse - AuthService 호환용
    // =========================================================================

    /**
     * 회원가입 응답 DTO
     *
     * AuthService.registerUser()에서 사용됩니다.
     * AuthController.register()에서는 사용하지 않지만,
     * AuthService 호환성을 위해 추가되었습니다.
     */
    @Getter
    @Setter
    @NoArgsConstructor
    public static class RegisterResponse {

        private boolean success;
        private String message;
        private Long userId;

        /**
         * 전체 필드 생성자
         */
        private RegisterResponse(boolean success, String message, Long userId) {
            this.success = success;
            this.message = message;
            this.userId = userId;
        }

        /**
         * 성공 응답 팩토리 메서드
         *
         * @param message 성공 메시지
         * @param userId 생성된 사용자 ID
         * @return 성공 응답 객체
         */
        public static RegisterResponse success(String message, Long userId) {
            return new RegisterResponse(true, message, userId);
        }

        /**
         * 실패 응답 팩토리 메서드
         *
         * @param message 실패 메시지
         * @return 실패 응답 객체
         */
        public static RegisterResponse failure(String message) {
            return new RegisterResponse(false, message, null);
        }

        /**
         * 성공 여부 확인
         *
         * @return 성공 시 true
         */
        public boolean isSuccess() {
            return success;
        }
    }
}