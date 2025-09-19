package com.kmportal.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS 전용 설정 클래스
 *
 * Spring Security의 CORS 설정과 함께 사용되는 추가적인 CORS 설정
 * 주로 Spring MVC 레벨에서의 CORS 처리를 담당
 *
 * SecurityConfig와의 차이점:
 * - SecurityConfig: Spring Security 필터 레벨에서 CORS 처리
 * - CorsConfig: Spring MVC 레벨에서 CORS 처리
 *
 * 두 설정이 함께 작동하여 완전한 CORS 지원 제공
 *
 * @Configuration: Spring 설정 클래스 지정
 */
@Configuration
public class CorsConfig {

    /**
     * 프론트엔드 개발 서버 URL (application.yml에서 주입)
     * 개발 환경에서 동적으로 변경 가능하도록 외부 설정으로 관리
     */
    @Value("${app.cors.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * 허용할 추가 오리진들 (쉼표로 구분)
     * 예: "http://localhost:3000,http://localhost:8081"
     */
    @Value("${app.cors.additional-origins:}")
    private String additionalOrigins;

    /**
     * CORS 최대 캐시 시간 (초)
     */
    @Value("${app.cors.max-age:3600}")
    private Long maxAge;

    /**
     * 개발 모드 여부 (개발 환경에서는 더 관대한 CORS 설정)
     */
    @Value("${app.cors.dev-mode:true}")
    private boolean devMode;

    /**
     * WebMvcConfigurer를 구현한 CORS 설정 빈
     *
     * Spring MVC 레벨에서 CORS 설정을 처리
     * SecurityConfig의 CORS 설정과 함께 작동
     *
     * @return WebMvcConfigurer CORS 설정이 포함된 MVC 설정
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {

                // 모든 API 경로에 대해 CORS 설정 적용
                registry.addMapping("/api/**")

                        // ====== 허용할 오리진 설정 ======
                        .allowedOrigins(getAllowedOrigins())

                        // ====== 허용할 HTTP 메서드 ======
                        .allowedMethods(
                                "GET",      // 데이터 조회
                                "POST",     // 데이터 생성
                                "PUT",      // 전체 데이터 수정
                                "PATCH",    // 부분 데이터 수정
                                "DELETE",   // 데이터 삭제
                                "OPTIONS",  // preflight 요청
                                "HEAD"      // 헤더 정보만 요청
                        )

                        // ====== 허용할 요청 헤더 ======
                        .allowedHeaders(
                                "Origin",
                                "Content-Type",
                                "Accept",
                                "Authorization",        // JWT 토큰용
                                "X-Requested-With",
                                "X-XSRF-TOKEN",
                                "Cache-Control",
                                "Pragma",
                                "Expires",
                                "Last-Modified",
                                "If-Modified-Since"
                        )

                        // ====== 응답에 노출할 헤더 ======
                        .exposedHeaders(
                                "Authorization",        // 새로운 JWT 토큰
                                "Content-Disposition",  // 파일 다운로드용
                                "X-Total-Count",        // 전체 개수 (페이징용)
                                "X-Current-Page",       // 현재 페이지
                                "X-Total-Pages"         // 전체 페이지 수
                        )

                        // ====== 인증 정보 허용 여부 ======
                        // JWT 토큰 사용시에는 false, 쿠키 기반 인증시에는 true
                        .allowCredentials(false)

                        // ====== Preflight 요청 캐시 시간 ======
                        .maxAge(maxAge);

                // ====== 헬스체크 엔드포인트 (더 관대한 설정) ======
                registry.addMapping("/api/health/**")
                        .allowedOrigins("*")           // 모든 오리진 허용
                        .allowedMethods("GET", "OPTIONS")
                        .allowedHeaders("*")
                        .maxAge(300);                  // 5분 캐시

                // ====== 개발 환경용 추가 설정 ======
                if (devMode) {
                    // 개발 도구나 테스트 도구에서의 접근 허용
                    registry.addMapping("/api/dev/**")
                            .allowedOrigins("*")
                            .allowedMethods("*")
                            .allowedHeaders("*")
                            .maxAge(0);                // 캐시 안함
                }
            }
        };
    }

    /**
     * 허용할 오리진 목록을 동적으로 생성
     *
     * application.yml 설정값과 환경에 따라 허용할 오리진을 결정
     * 개발 환경과 운영 환경에서 다른 설정 적용 가능
     *
     * @return String[] 허용할 오리진 배열
     */
    private String[] getAllowedOrigins() {
        // 기본 오리진 목록
        java.util.List<String> origins = new java.util.ArrayList<>();

        // 메인 프론트엔드 URL 추가
        origins.add(frontendUrl);

        // 개발 환경에서 일반적으로 사용하는 포트들 추가
        if (devMode) {
            origins.add("http://localhost:3000");    // Vue CLI 새로운 기본 포트
            origins.add("http://localhost:8080");    // Vue CLI 이전 포트 (백업)
            origins.add("http://localhost:8081");    // 대체 포트
            origins.add("http://127.0.0.1:3000");    // IPv4 루프백 (3000)
            origins.add("http://127.0.0.1:8080");    // IPv4 루프백 (8080)

            // 로컬 IP 주소 (모바일 테스트용)
            try {
                java.net.InetAddress localHost = java.net.InetAddress.getLocalHost();
                String hostAddress = localHost.getHostAddress();
                origins.add("http://" + hostAddress + ":3000");
                origins.add("http://" + hostAddress + ":8080");
            } catch (java.net.UnknownHostException e) {
                // 로컬 IP를 가져올 수 없는 경우 무시
            }
        }

        // 추가 오리진이 설정된 경우 포함
        if (additionalOrigins != null && !additionalOrigins.trim().isEmpty()) {
            String[] additional = additionalOrigins.split(",");
            for (String origin : additional) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !origins.contains(trimmed)) {
                    origins.add(trimmed);
                }
            }
        }

        return origins.toArray(new String[0]);
    }

    /**
     * 현재 CORS 설정 정보를 로그로 출력 (개발 편의용)
     *
     * 애플리케이션 시작시 CORS 설정 상태를 확인할 수 있도록 함
     * 운영 환경에서는 로그 레벨을 조정하여 출력하지 않을 수 있음
     */
    @jakarta.annotation.PostConstruct
    public void logCorsConfiguration() {
        System.out.println("====== CORS Configuration ======");
        System.out.println("Frontend URL: " + frontendUrl);
        System.out.println("Development Mode: " + devMode);
        System.out.println("Max Age: " + maxAge + " seconds");
        System.out.println("Allowed Origins: " + java.util.Arrays.toString(getAllowedOrigins()));

        if (additionalOrigins != null && !additionalOrigins.trim().isEmpty()) {
            System.out.println("Additional Origins: " + additionalOrigins);
        }

        System.out.println("================================");
    }
}

/*
 * ====== application.yml에 추가할 CORS 설정 예시 ======
 *
 * app:
 *   cors:
 *     frontend-url: http://localhost:8080
 *     additional-origins: http://localhost:3000,http://localhost:8081
 *     max-age: 3600
 *     dev-mode: true
 *
 * # 운영 환경 설정 예시 (application-prod.yml)
 * app:
 *   cors:
 *     frontend-url: https://your-domain.com
 *     additional-origins: https://admin.your-domain.com
 *     max-age: 86400  # 24시간
 *     dev-mode: false
 */