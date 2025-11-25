package com.kmportal.backend.config;

import com.kmportal.backend.filter.JwtAuthenticationFilter;
import com.kmportal.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * Spring Security 보안 설정 클래스
 *
 * 이 클래스는 애플리케이션의 모든 보안 정책을 정의합니다:
 * - JWT 기반 인증 시스템 구성
 * - API 엔드포인트별 접근 권한 설정
 * - CORS 정책 적용
 * - 세션 정책 (Stateless) 설정
 * - 비밀번호 암호화 설정
 * - 메서드 레벨 보안 활성화
 *
 * JWTを 사용하므로 전통적인 세션 기반 인증은 비활성화하고,
 * 모든 요청은 토큰을 통해 인증됩니다.
 *
 * 🆕 30일차 수정: 댓글 API 권한 설정 추가
 *
 * @author KM Portal Team
 * @version 1.1
 * @since 2025-09-24
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(
        prePostEnabled = true,  // @PreAuthorize, @PostAuthorize 활성화
        securedEnabled = true,  // @Secured 활성화
        jsr250Enabled = true    // @RolesAllowed 활성화
)
public class SecurityConfig {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private CorsConfigurationSource corsConfigurationSource;

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder Bean 생성
     *
     * BCrypt 알고리즘을 사용하여 비밀번호를 안전하게 해시화합니다.
     * BCrypt는 salt를 자동으로 생성하고 적응형 함수로 설계되어
     * 무차별 대입 공격에 강한 저항성을 가집니다.
     *
     * @return BCryptPasswordEncoder 비밀번호 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        // strength 12: 충분히 안전하면서도 성능을 고려한 설정
        return new BCryptPasswordEncoder(12);
    }

    /**
     * JWT 인증 필터 Bean 생성
     *
     * 모든 HTTP 요청에서 JWT 토큰을 검증하고 인증 정보를 설정하는
     * 커스텀 필터를 생성합니다.
     *
     * @return JwtAuthenticationFilter JWT 인증 필터 인스턴스
     */
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtUtil);
    }

    /**
     * Spring Security 메인 설정
     *
     * 모든 HTTP 보안 정책을 정의하는 SecurityFilterChain을 구성합니다.
     * JWT 기반 인증, CORS, 세션 관리, 권한 설정 등을 포함합니다.
     *
     * @param http HttpSecurity 설정 객체
     * @return SecurityFilterChain 보안 필터 체인
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF 보호 비활성화 (JWT 사용으로 불필요)
                .csrf(csrf -> csrf.disable())

                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                // 세션 정책: STATELESS (세션 사용하지 않음)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // HTTP 요청별 인증 규칙 설정
                .authorizeHttpRequests(authz -> authz

                        // ===== 공개 접근 허용 엔드포인트 =====

                        // 인증 관련 API (로그인, 토큰 갱신 등)
                        .requestMatchers("/api/auth/**").permitAll()

                        // 공개 정보 조회 API
                        .requestMatchers("/api/public/**").permitAll()

                        // 개발/운영 모니터링 엔드포인트
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/info").permitAll()

                        // 개발용 H2 데이터베이스 콘솔 (프로덕션에서는 제거)
                        .requestMatchers("/h2-console/**").permitAll()

                        // 정적 리소스 (CSS, JS, 이미지 등)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // API 문서 (Swagger 등, 개발 환경에서만)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        // ===== 관리자 전용 엔드포인트 =====

                        // 사용자 관리 API (관리자만 접근 가능)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")

                        // 시스템 관리 API
                        .requestMatchers("/api/system/**").hasRole("ADMIN")

                        // 역할 관리 API
                        .requestMatchers("/api/roles/**").hasAnyRole("ADMIN", "MANAGER")

                        // ===== 매니저 이상 권한 필요 엔드포인트 =====

                        // 사용자 관리 API (매니저 이상)
                        .requestMatchers("/api/users/create").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/users/*/lock").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/users/*/unlock").hasAnyRole("ADMIN", "MANAGER")
                        .requestMatchers("/api/users/*/roles").hasAnyRole("ADMIN", "MANAGER")

                        // 게시판 관리 API
                        .requestMatchers("/api/boards/*/admin/**").hasAnyRole("ADMIN", "BOARD_ADMIN")

                        // 게시글 상단 고정/해제 (관리자만)
                        .requestMatchers("/api/boards/*/pin").hasRole("ADMIN")

                        // 게시판 통계 (관리자 또는 매니저)
                        .requestMatchers("/api/boards/statistics").hasAnyRole("ADMIN", "MANAGER")

                        // ===== 인증된 사용자 접근 가능 엔드포인트 =====

                        // 기본 사용자 정보 조회/수정
                        .requestMatchers("/api/users/me").authenticated()
                        .requestMatchers("/api/users/profile").authenticated()

                        // 일반 사용자 조회 (읽기 전용)
                        .requestMatchers("/api/users").hasAnyRole("ADMIN", "MANAGER", "USER")
                        .requestMatchers("/api/users/*").hasAnyRole("ADMIN", "MANAGER", "USER")

                        // 파일 관리 API
                        .requestMatchers("/api/files/upload").authenticated()
                        .requestMatchers("/api/files/download/*").authenticated()
                        .requestMatchers("/api/files/delete/*").authenticated()

                        // ===== 🆕 30일차 추가: 댓글 API 권한 설정 =====
                        // 댓글 작성/수정/삭제 - 로그인 필요
                        // 댓글 API는 /api/boards/{boardId}/comments/** 형태
                        // /api/boards/** 패턴에 포함되지만 명시적으로 설정

                        // 댓글 목록 조회 - 인증된 사용자
                        .requestMatchers("/api/boards/*/comments").authenticated()

                        // 댓글 작성 - 인증된 사용자
                        .requestMatchers("/api/boards/*/comments/**").authenticated()

                        // ===== 기존 게시판 API =====
                        // 게시판 API (댓글 API 포함)
                        .requestMatchers("/api/boards/**").authenticated()
                        .requestMatchers("/api/posts/**").authenticated()
                        .requestMatchers("/api/comments/**").authenticated()

                        // 대시보드 및 통계 API
                        .requestMatchers("/api/dashboard/**").authenticated()
                        .requestMatchers("/api/statistics/**").authenticated()

                        // 알림 API
                        .requestMatchers("/api/notifications/**").authenticated()

                        // ===== 기타 모든 API 요청 =====

                        // 위에 정의되지 않은 모든 API는 인증 필요
                        .requestMatchers("/api/**").authenticated()

                        // 그 외 모든 요청은 허용 (프론트엔드 라우팅)
                        .anyRequest().permitAll()
                )

                // ===== HTTP 보안 헤더 설정 =====
                .headers(headers -> headers
                        // X-Frame-Options: H2 콘솔을 위해 sameOrigin으로 설정
                        .frameOptions(frame -> frame.sameOrigin())

                        // X-Content-Type-Options: MIME 타입 스니핑 방지
                        .contentTypeOptions(contentType -> {})
                );

        // ===== 커스텀 JWT 인증 필터 추가 =====

        // UsernamePasswordAuthenticationFilter 앞에 JWT 필터 추가
        // 이렇게 하면 모든 요청이 JWT 필터를 먼저 거치게 됩니다
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}

/*
 * ====== 30일차 수정 내용 ======
 *
 * 댓글 API 경로:
 * - POST   /api/boards/{boardId}/comments              - 댓글 작성
 * - POST   /api/boards/{boardId}/comments/{id}/replies - 대댓글 작성
 * - GET    /api/boards/{boardId}/comments              - 댓글 목록 조회
 * - GET    /api/boards/{boardId}/comments/{id}/replies - 대댓글 목록 조회
 * - PUT    /api/boards/{boardId}/comments/{id}         - 댓글 수정
 * - DELETE /api/boards/{boardId}/comments/{id}         - 댓글 삭제
 * - GET    /api/boards/{boardId}/comments/count        - 댓글 수 조회
 *
 * 권한 정책:
 * - 모든 댓글 API는 로그인한 사용자만 접근 가능
 * - 댓글 수정/삭제는 본인 또는 관리자만 가능 (Service 레이어에서 체크)
 */