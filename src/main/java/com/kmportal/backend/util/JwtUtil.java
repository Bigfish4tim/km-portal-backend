package com.kmportal.backend.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;  // ✅ 추가: stream 사용을 위한 import

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 유틸리티 클래스 (0.12.6 호환)
 *
 * 주요 기능:
 * - Access Token 생성 (24시간 유효)
 * - Refresh Token 생성 (7일 유효)
 * - 토큰 검증 및 파싱
 * - 사용자 정보 추출 (username, roles, email 등)
 * - Spring Security 호환 (ROLE_ 접두사 자동 추가)
 *
 * 수정 사항 (2025-11-20):
 * - roles에 "ROLE_" 접두사 자동 추가 기능 추가
 * - Spring Security와의 호환성 개선
 *
 * @author KM Portal Team
 * @version 1.1
 * @since 2025-09-24
 */
@Component
public class JwtUtil {

    /**
     * Access Token 유효 기간 (밀리초)
     * 24시간 = 24 * 60 * 60 * 1000 = 86400000ms
     */
    private static final long JWT_EXPIRATION = 86400000; // 24시간

    /**
     * Refresh Token 유효 기간 (밀리초)
     * 7일 = 7 * 24 * 60 * 60 * 1000 = 604800000ms
     */
    private static final long REFRESH_EXPIRATION = 604800000; // 7일

    /**
     * JWT 서명에 사용되는 비밀 키
     *
     * ⚠️ 중요: 프로덕션 환경에서는 반드시 application.properties에서 설정하세요!
     * 예: jwt.secret=${JWT_SECRET_KEY}
     */
    private String secret = "myVerySecretKeyForKMPortal2025!@#$%^&*()_+1234567890";

    /**
     * HMAC SHA 암호화에 사용되는 SecretKey 객체
     * 캐시하여 매번 생성하지 않도록 최적화
     */
    private SecretKey secretKey;

    /**
     * SecretKey 생성 및 캐싱
     *
     * @return SecretKey 객체
     */
    private SecretKey getSigningKey() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        }
        return secretKey;
    }

    /**
     * 기본 Access Token 생성 (username만 포함)
     *
     * @param username 사용자 아이디
     * @return JWT 토큰 문자열
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, JWT_EXPIRATION);
    }

    /**
     * 완전한 Access Token 생성 (사용자 정보 포함)
     *
     * ✅ 수정된 부분: roles에 "ROLE_" 접두사 자동 추가
     *
     * 이 메서드는 사용자의 모든 정보를 JWT 토큰에 포함시킵니다.
     * Spring Security가 요구하는 "ROLE_" 접두사를 자동으로 추가하여
     * 권한 검증이 정상적으로 작동하도록 합니다.
     *
     * 예시:
     * - 입력: roles = ["ADMIN", "USER"]
     * - 토큰 저장: roles = ["ROLE_ADMIN", "ROLE_USER"]
     *
     * @param username 사용자 아이디
     * @param fullName 사용자 전체 이름
     * @param email 이메일 주소
     * @param department 부서명
     * @param roles 사용자 권한 목록 (ROLE_ 접두사 없어도 자동 추가됨)
     * @return JWT 토큰 문자열
     */
    public String generateToken(String username, String fullName, String email,
                                String department, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("fullName", fullName);
        claims.put("email", email);
        claims.put("department", department);

        // ========================================
        // ✅ 핵심 수정: ROLE_ 접두사 자동 추가
        // ========================================
        //
        // Spring Security는 권한에 "ROLE_" 접두사를 요구합니다.
        // 예: hasRole("ADMIN")은 실제로는 "ROLE_ADMIN"을 찾습니다.
        //
        // 이 로직은:
        // 1. 이미 "ROLE_"로 시작하는 권한은 그대로 유지
        // 2. "ROLE_"로 시작하지 않는 권한은 앞에 "ROLE_" 추가
        //
        // 예시 변환:
        // - "ADMIN" → "ROLE_ADMIN"
        // - "USER" → "ROLE_USER"
        // - "ROLE_MANAGER" → "ROLE_MANAGER" (이미 있으면 유지)
        //
        List<String> rolesWithPrefix = roles.stream()
                .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                .collect(Collectors.toList());

        claims.put("roles", rolesWithPrefix);  // ✅ 접두사가 추가된 roles 저장
        claims.put("tokenType", "access");

        return createToken(claims, username, JWT_EXPIRATION);
    }

    /**
     * Refresh Token 생성
     *
     * Refresh Token은 Access Token 갱신에만 사용되므로
     * 최소한의 정보만 포함합니다.
     *
     * @param username 사용자 아이디
     * @return Refresh Token 문자열
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "refresh");
        return createToken(claims, username, REFRESH_EXPIRATION);
    }

    /**
     * JWT 토큰 생성 내부 메서드
     *
     * JJWT 0.12.6 버전의 새로운 API를 사용합니다:
     * - setClaims() → claims()
     * - setSubject() → subject()
     * - setIssuedAt() → issuedAt()
     * - setExpiration() → expiration()
     *
     * @param claims 토큰에 포함할 클레임 정보
     * @param subject 토큰 주체 (일반적으로 username)
     * @param expiration 토큰 유효 기간 (밀리초)
     * @return JWT 토큰 문자열
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)              // 클레임 설정
                .subject(subject)            // 주체(username) 설정
                .issuedAt(now)              // 발급 시간
                .expiration(expiryDate)     // 만료 시간
                .signWith(getSigningKey())  // 서명 키로 서명
                .compact();                 // 토큰 생성
    }

    /**
     * 토큰에서 사용자 이름 추출
     *
     * @param token JWT 토큰
     * @return 사용자 아이디
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * 토큰에서 만료 시간 추출
     *
     * @param token JWT 토큰
     * @return 만료 시간
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * 토큰에서 특정 클레임 추출
     *
     * Function을 사용하여 원하는 클레임을 유연하게 추출할 수 있습니다.
     *
     * @param token JWT 토큰
     * @param claimsResolver 클레임 추출 함수
     * @param <T> 반환 타입
     * @return 추출된 클레임 값
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * 토큰에서 모든 클레임 추출
     *
     * JJWT 0.12.6 버전의 새로운 API를 사용합니다:
     * - setSigningKey() → verifyWith()
     * - parseClaimsJws() → parseSignedClaims()
     * - getBody() → getPayload()
     *
     * @param token JWT 토큰
     * @return Claims 객체
     * @throws JwtException 토큰 파싱 실패 시
     */
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())    // 서명 키로 검증
                    .build()
                    .parseSignedClaims(token)       // 서명된 클레임 파싱
                    .getPayload();                  // 페이로드 (클레임) 반환
        } catch (Exception e) {
            throw new JwtException("토큰 파싱 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 토큰 만료 여부 확인
     *
     * @param token JWT 토큰
     * @return 만료되었으면 true, 아니면 false
     */
    public Boolean isTokenExpired(String token) {
        final Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    /**
     * 토큰 유효성 검증
     *
     * 다음 사항을 검증합니다:
     * 1. 토큰에서 추출한 username이 파라미터와 일치하는가?
     * 2. 토큰이 만료되지 않았는가?
     *
     * @param token JWT 토큰
     * @param username 검증할 사용자 아이디
     * @return 유효하면 true, 아니면 false
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 토큰에서 권한 목록 추출
     *
     * ✅ 이미 "ROLE_" 접두사가 포함된 상태로 반환됩니다
     * (generateToken에서 추가했으므로)
     *
     * @param token JWT 토큰
     * @return 권한 목록 (예: ["ROLE_ADMIN", "ROLE_USER"])
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    /**
     * 토큰에서 전체 이름 추출
     *
     * @param token JWT 토큰
     * @return 사용자 전체 이름
     */
    public String extractFullName(String token) {
        return extractClaim(token, claims -> (String) claims.get("fullName"));
    }

    /**
     * 토큰에서 이메일 추출
     *
     * @param token JWT 토큰
     * @return 이메일 주소
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> (String) claims.get("email"));
    }

    /**
     * 토큰에서 부서명 추출
     *
     * @param token JWT 토큰
     * @return 부서명
     */
    public String extractDepartment(String token) {
        return extractClaim(token, claims -> (String) claims.get("department"));
    }
}

/*
 * ====== 수정 내역 ======
 *
 * [2025-11-20] v1.1
 * - generateToken 메서드에 ROLE_ 접두사 자동 추가 로직 추가
 * - Spring Security 호환성 개선
 * - 주석 대폭 추가 (코드 이해도 향상)
 * - import 문 추가 (Collectors)
 *
 * [2025-09-24] v1.0
 * - 초기 버전 생성
 * - JJWT 0.12.6 API 호환
 */