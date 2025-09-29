package com.kmportal.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS(Cross-Origin Resource Sharing) 설정 클래스
 *
 * Vue.js 프론트엔드(일반적으로 포트 8080)와 Spring Boot 백엔드(포트 8081) 간의
 * 통신을 위한 CORS 정책을 설정합니다.
 *
 * 개발 환경에서는 보안을 완화하여 개발 편의성을 높이고,
 * 프로덕션 환경에서는 보안을 강화할 수 있도록 구성되어 있습니다.
 *
 * @author KM Portal Team
 * @version 1.0
 * @since 2025-09-24
 */
@Configuration
public class CorsConfig {

    /**
     * CORS 설정 소스를 생성하는 Bean
     *
     * 이 메서드는 Spring Security와 함께 동작하여 모든 HTTP 요청에 대해
     * CORS 정책을 적용합니다.
     *
     * @return CorsConfigurationSource CORS 설정이 적용된 소스 객체
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {

        // CORS 설정 객체 생성
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경에서 허용할 Origin들을 설정
        // Vue CLI 개발 서버의 기본 포트들을 포함
        configuration.setAllowedOriginPatterns(Arrays.asList(
                "http://localhost:3000",    // Vue.js 개발 서버 (localhost)
                "http://127.0.0.1:3000"    // Vue.js 개발 서버 (IP 주소)
        ));


        // 허용할 HTTP 메서드들을 설정
        // RESTful API의 모든 기본 메서드를 포함
        configuration.setAllowedMethods(Arrays.asList(
                "GET",      // 데이터 조회
                "POST",     // 데이터 생성
                "PUT",      // 데이터 전체 수정
                "PATCH",    // 데이터 부분 수정
                "DELETE",   // 데이터 삭제
                "OPTIONS"   // Preflight 요청
        ));

        // 허용할 헤더들을 설정
        // JWT 토큰 인증과 JSON 통신에 필요한 헤더들을 포함
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",    // JWT 토큰 헤더
                "Content-Type",     // 요청 본문 타입 (application/json 등)
                "X-Requested-With", // AJAX 요청 식별
                "Accept",           // 허용하는 응답 타입
                "Origin",           // 요청 출처
                "Access-Control-Request-Method",    // Preflight 요청 메서드
                "Access-Control-Request-Headers"    // Preflight 요청 헤더
        ));

        // 인증 정보(쿠키, 인증 헤더)를 포함한 요청을 허용
        // JWT 토큰이 포함된 요청을 처리하기 위해 필요
        configuration.setAllowCredentials(true);

        // Preflight 요청의 캐시 시간을 설정 (1시간)
        // OPTIONS 요청의 결과를 캐시하여 성능을 향상시킴
        configuration.setMaxAge(3600L);

        // 클라이언트에서 접근할 수 있는 응답 헤더를 설정
        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",        // 새로운 토큰 반환시 필요
                "Content-Disposition"   // 파일 다운로드시 필요
        ));

        // URL 기반 CORS 설정 소스 생성
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        // 모든 경로("/**")에 대해 위에서 설정한 CORS 정책을 적용
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}