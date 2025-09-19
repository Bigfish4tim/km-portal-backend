package com.kmportal.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 설정 클래스 (기본 구성)
 *
 * 현재는 2일차 기본 구성으로, 모든 요청을 허용하고 기본 설정만 제공합니다.
 * 향후 6일차(JWT 설정)에서 본격적인 인증/인가 설정을 추가할 예정입니다.
 *
 * 현재 설정의 목적:
 * 1. Spring Security 기본 구조 마련
 * 2. 비밀번호 인코더 설정
 * 3. CORS 및 기본 보안 헤더 설정
 * 4. 개발 환경에서의 편의성 제공 (모든 요청 허용)
 *
 * @Configuration: 이 클래스가 Spring 설정 클래스임을 나타냄
 * @EnableWebSecurity: Spring Security 웹 보안 활성화
 * @EnableMethodSecurity: 메서드 레벨 보안 활성화 (향후 @PreAuthorize 등 사용)
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // @PreAuthorize, @PostAuthorize 활성화
public class SecurityConfig {

    /**
     * 비밀번호 인코더 빈 등록
     *
     * BCryptPasswordEncoder를 사용하여 안전한 비밀번호 해싱 제공
     * BCrypt는 다음과 같은 장점이 있습니다:
     * 1. Salt 자동 생성으로 레인보우 테이블 공격 방지
     * 2. 적응형 해싱으로 시간이 지나도 보안 강도 유지 가능
     * 3. Spring Security에서 권장하는 표준 방식
     *
     * @return BCryptPasswordEncoder 인스턴스
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security 필터 체인 설정 (2일차 간소화 버전)
     *
     * 2일차에서는 기본 동작에 집중하고, 복잡한 보안 설정은 6일차에 추가
     *
     * @param http HttpSecurity 설정 객체
     * @return SecurityFilterChain 보안 필터 체인
     * @throws Exception 설정 중 발생할 수 있는 예외
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                // ====== CSRF 비활성화 (REST API용) ======
                .csrf(csrf -> csrf.disable())

                // ====== 세션 비활성화 (JWT 사용 예정) ======
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                // ====== CORS 활성화 ======
                .cors(cors -> cors
                        .configurationSource(corsConfigurationSource())
                )

                // ====== 2일차 개발 단계: 모든 요청 허용 ======
                .authorizeHttpRequests(authz -> authz
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * CORS 설정 정의
     *
     * 프론트엔드(Vue.js)와 백엔드(Spring Boot) 간의 크로스 오리진 요청 허용
     * 개발 환경에서는 모든 오리진 허용, 운영 환경에서는 특정 도메인만 허용
     *
     * @return CorsConfigurationSource CORS 설정 소스
     */
    @Bean
    public org.springframework.web.cors.CorsConfigurationSource corsConfigurationSource() {
        org.springframework.web.cors.CorsConfiguration configuration =
                new org.springframework.web.cors.CorsConfiguration();

        // 허용할 오리진 설정
        if ("development".equals(System.getProperty("spring.profiles.active", "dev"))) {
            // 개발 환경: 로컬 Vue.js 개발 서버 허용
            configuration.addAllowedOrigin("http://localhost:8080");     // Vue CLI 개발 서버
            configuration.addAllowedOrigin("http://localhost:3000");     // 추가 개발 포트
            configuration.addAllowedOrigin("http://127.0.0.1:8080");
        } else {
            // 운영 환경: 실제 도메인만 허용 (향후 설정)
            // configuration.addAllowedOrigin("https://your-domain.com");
            configuration.addAllowedOriginPattern("*"); // 임시로 모든 오리진 허용
        }

        // 허용할 HTTP 메서드
        configuration.addAllowedMethod("GET");
        configuration.addAllowedMethod("POST");
        configuration.addAllowedMethod("PUT");
        configuration.addAllowedMethod("PATCH");
        configuration.addAllowedMethod("DELETE");
        configuration.addAllowedMethod("OPTIONS");

        // 허용할 헤더
        configuration.addAllowedHeader("*"); // 모든 헤더 허용

        // 인증 정보 포함 허용 (JWT 토큰 사용시 필요)
        configuration.setAllowCredentials(false); // JWT 사용시 false로 설정

        // preflight 요청 캐시 시간 (초)
        configuration.setMaxAge(3600L);

        org.springframework.web.cors.UrlBasedCorsConfigurationSource source =
                new org.springframework.web.cors.UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}

/*
 * ====== 향후 추가될 설정들 (6일차 JWT 구현시) ======
 *
 * 1. JWT 인증 필터:
 *    @Bean
 *    public JwtAuthenticationFilter jwtAuthenticationFilter() {
 *        return new JwtAuthenticationFilter();
 *    }
 *
 * 2. AuthenticationManager 설정:
 *    @Bean
 *    public AuthenticationManager authenticationManager(
 *            AuthenticationConfiguration config) throws Exception {
 *        return config.getAuthenticationManager();
 *    }
 *
 * 3. UserDetailsService 구현:
 *    @Bean
 *    public UserDetailsService userDetailsService() {
 *        return new CustomUserDetailsService();
 *    }
 *
 * 4. 상세 권한 설정:
 *    .authorizeHttpRequests(authz -> authz
 *        .requestMatchers("/api/admin/**").hasRole("ADMIN")
 *        .requestMatchers("/api/manager/**").hasAnyRole("ADMIN", "MANAGER")
 *        .requestMatchers(HttpMethod.GET, "/api/boards/**").hasRole("USER")
 *        .requestMatchers(HttpMethod.POST, "/api/boards/**").hasRole("USER")
 *        .anyRequest().authenticated()
 *    )
 *
 * 5. 예외 처리:
 *    .exceptionHandling(exceptions -> exceptions
 *        .authenticationEntryPoint(new JwtAuthenticationEntryPoint())
 *        .accessDeniedHandler(new JwtAccessDeniedHandler())
 *    )
 */