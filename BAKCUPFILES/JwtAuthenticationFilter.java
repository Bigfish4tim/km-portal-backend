package com.kmportal.backend.filter;

import com.kmportal.backend.util.JwtUtil;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * JWT 토큰 기반 인증을 처리하는 Spring Security 필터
 *
 * 이 필터는 모든 HTTP 요청을 가로채서 JWT 토큰의 유효성을 검증하고,
 * 유효한 토큰인 경우 Spring Security Context에 인증 정보를 설정합니다.
 *
 * 처리 과정:
 * 1. Authorization 헤더에서 JWT 토큰 추출
 * 2. 토큰 유효성 검증 (서명, 만료시간 등)
 * 3. 토큰에서 사용자 정보 및 권한 추출
 * 4. Spring Security Authentication 객체 생성
 * 5. Security Context에 인증 정보 설정
 *
 * OncePerRequestFilter를 상속하여 요청당 한 번만 실행되도록 보장합니다.
 *
 * @author KM Portal Team
 * @version 1.0
 * @since 2025-09-24
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    /**
     * JWT 유틸리티를 주입받는 생성자
     *
     * @param jwtUtil JWT 토큰 처리를 위한 유틸리티 객체
     */
    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * 필터의 메인 로직을 수행하는 메서드
     *
     * 모든 HTTP 요청에 대해 JWT 토큰을 검증하고 인증 정보를 설정합니다.
     * 토큰이 없거나 유효하지 않은 경우 인증 없이 다음 필터로 진행합니다.
     *
     * @param request HTTP 요청 객체
     * @param response HTTP 응답 객체
     * @param filterChain 다음 필터 체인
     * @throws ServletException 서블릿 예외
     * @throws IOException 입출력 예외
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // 1. Authorization 헤더에서 JWT 토큰 추출
            String jwt = extractTokenFromRequest(request);

            // 2. 토큰이 존재하고 현재 Security Context에 인증 정보가 없는 경우 처리
            if (jwt != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // 3. JWT 토큰에서 사용자명 추출 및 검증
                String username = jwtUtil.extractUsername(jwt);

                if (username != null) {

                    // 4. 토큰 유효성 검증
                    if (jwtUtil.validateToken(jwt, username)) {

                        // 5. 토큰에서 사용자 권한 정보 추출
                        List<String> roles = jwtUtil.extractRoles(jwt);

                        // 6. Spring Security 권한 객체로 변환
                        List<GrantedAuthority> authorities = roles.stream()
                                .map(SimpleGrantedAuthority::new)
                                .collect(Collectors.toList());

                        // 7. 인증 토큰 생성 (사용자명, null, 권한목록)
                        // 비밀번호는 null로 설정 (JWT 기반 인증이므로 불필요)
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        username,           // Principal (사용자명)
                                        null,              // Credentials (비밀번호 - JWT에서는 불필요)
                                        authorities        // Authorities (권한 목록)
                                );

                        // 8. 인증 상세 정보 설정 (IP 주소, 세션 ID 등)
                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        // 9. Security Context에 인증 정보 설정
                        SecurityContextHolder.getContext().setAuthentication(authToken);

                        // 10. 로그 기록 (개발/디버깅용)
                        if (logger.isDebugEnabled()) {
                            logger.debug(String.format(
                                    "[JWT Filter] 사용자 인증 성공: %s, 권한: %s",
                                    username,
                                    roles.toString()
                            ));
                        }

                    } else {
                        // 토큰이 유효하지 않은 경우
                        logger.warn(String.format(
                                "[JWT Filter] 유효하지 않은 토큰: 사용자=%s, IP=%s",
                                username,
                                getClientIpAddress(request)
                        ));
                    }
                }
            }

        } catch (Exception e) {
            // JWT 처리 중 예외 발생시 로그 기록
            logger.error(String.format(
                    "[JWT Filter] 토큰 처리 중 오류 발생: %s, IP=%s",
                    e.getMessage(),
                    getClientIpAddress(request)
            ), e);

            // 예외가 발생해도 필터 체인은 계속 진행
            // Security Context는 비어있는 상태로 유지되어 비인증 상태가 됨
        }

        // 다음 필터로 요청 전달
        filterChain.doFilter(request, response);
    }

    /**
     * HTTP 요청에서 JWT 토큰을 추출하는 메서드
     *
     * Authorization 헤더에서 "Bearer " 접두사를 제거하고 토큰만 추출합니다.
     *
     * @param request HTTP 요청 객체
     * @return 추출된 JWT 토큰 문자열, 없으면 null
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        // Authorization 헤더 값 가져오기
        String bearerToken = request.getHeader("Authorization");

        // Bearer 토큰 형식인지 확인하고 토큰 부분만 추출
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // "Bearer " 제거
        }

        return null;
    }

    /**
     * 클라이언트의 실제 IP 주소를 가져오는 메서드
     *
     * 프록시나 로드밸런서를 거치는 경우를 고려하여
     * 다양한 헤더에서 실제 클라이언트 IP를 찾습니다.
     *
     * @param request HTTP 요청 객체
     * @return 클라이언트 IP 주소
     */
    private String getClientIpAddress(HttpServletRequest request) {
        // 프록시를 통해 전달된 실제 IP 확인 (우선순위 순)
        String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED"
        };

        // 각 헤더에서 IP 주소 찾기
        for (String header : ipHeaders) {
            String ip = request.getHeader(header);

            if (StringUtils.hasText(ip) &&
                    !"unknown".equalsIgnoreCase(ip) &&
                    !"127.0.0.1".equals(ip) &&
                    !"0:0:0:0:0:0:0:1".equals(ip)) {

                // 여러 IP가 콤마로 구분된 경우 첫 번째 IP 사용
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }

                return ip;
            }
        }

        // 헤더에서 찾지 못한 경우 기본 원격 주소 사용
        return request.getRemoteAddr();
    }

    /**
     * 특정 URL 패턴에 대해 필터를 건너뛸지 결정하는 메서드
     *
     * 공개 API나 정적 리소스에 대해서는 JWT 검증을 수행하지 않도록
     * 성능 최적화를 위해 사용할 수 있습니다.
     *
     * @param request HTTP 요청 객체
     * @return 필터를 건너뛸 경우 true, 실행할 경우 false
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // 필터를 건너뛸 경로 패턴들
        String[] skipPatterns = {
                "/api/auth/login",          // 로그인 API
                "/api/auth/refresh",        // 토큰 갱신 API
                "/api/public/",             // 공개 API
                "/actuator/health",         // 헬스체크
                "/actuator/info",           // 정보 조회
                "/h2-console/",             // H2 콘솔 (개발용)
                "/swagger-ui/",             // API 문서 (개발용)
                "/v3/api-docs/",           // API 스펙 (개발용)
                "/css/",                   // CSS 정적 리소스
                "/js/",                    // JavaScript 정적 리소스
                "/images/",                // 이미지 정적 리소스
                "/favicon.ico"             // 파비콘
        };

        // 경로가 건너뛸 패턴 중 하나와 일치하는지 확인
        for (String pattern : skipPatterns) {
            if (path.startsWith(pattern)) {
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format(
                            "[JWT Filter] 필터 건너뛰기: %s (패턴: %s)",
                            path,
                            pattern
                    ));
                }
                return true;
            }
        }

        return false;
    }
}