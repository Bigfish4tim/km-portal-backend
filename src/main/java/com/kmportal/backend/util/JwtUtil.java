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

/**
 * JWT 토큰 생성, 검증, 파싱을 담당하는 유틸리티 클래스
 *
 * 이 클래스는 JWT(JSON Web Token)의 전체 생명주기를 관리합니다:
 * - 토큰 생성: 사용자 정보를 기반으로 JWT 토큰 생성
 * - 토큰 검증: 토큰의 유효성, 만료시간, 서명 검증
 * - 토큰 파싱: 토큰에서 사용자 정보 추출
 *
 * 보안을 위해 HS256 알고리즘과 강력한 비밀키를 사용합니다.
 *
 * @author KM Portal Team
 * @version 1.0
 * @since 2025-09-24
 */
@Component
public class JwtUtil {

    // JWT 토큰 만료 시간 (24시간 = 24 * 60 * 60 * 1000 밀리초)
    private static final long JWT_EXPIRATION = 86400000;

    // Refresh Token 만료 시간 (7일 = 7 * 24 * 60 * 60 * 1000 밀리초)
    private static final long REFRESH_EXPIRATION = 604800000;

    // JWT 서명을 위한 비밀키 (application.yml에서 설정)
    private String secret = "myVerySecretKeyForKMPortal2025!@#$%^&*()_+1234567890";

    // 캐시된 SecretKey 객체 (성능 최적화)
    private SecretKey secretKey;

    /**
     * 비밀키를 SecretKey 객체로 변환하여 반환
     *
     * 처음 호출시에만 SecretKey를 생성하고, 이후에는 캐시된 값을 사용하여
     * 성능을 최적화합니다.
     *
     * @return SecretKey JWT 서명에 사용할 비밀키
     */
    private SecretKey getSigningKey() {
        if (secretKey == null) {
            // 비밀키 문자열을 바이트 배열로 변환하여 SecretKey 생성
            secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        }
        return secretKey;
    }

    /**
     * 사용자명을 기반으로 JWT 토큰을 생성
     *
     * 기본적인 사용자명만으로 토큰을 생성하는 간단한 버전입니다.
     * 내부적으로 빈 클레임 맵을 생성하여 상세 버전을 호출합니다.
     *
     * @param username 토큰에 포함할 사용자명
     * @return String 생성된 JWT 토큰
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, JWT_EXPIRATION);
    }

    /**
     * 사용자 정보와 권한을 포함하여 JWT 토큰을 생성
     *
     * 사용자의 상세 정보와 권한 목록을 토큰에 포함시켜
     * 클라이언트에서 별도의 API 호출 없이 권한 확인이 가능합니다.
     *
     * @param username 사용자명
     * @param fullName 사용자 전체 이름
     * @param email 사용자 이메일
     * @param department 사용자 부서
     * @param roles 사용자 권한 목록
     * @return String 생성된 JWT 토큰
     */
    public String generateToken(String username, String fullName, String email,
                                String department, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();

        // 토큰에 사용자 정보를 클레임으로 추가
        claims.put("fullName", fullName);      // 화면 표시용 전체 이름
        claims.put("email", email);            // 이메일 주소
        claims.put("department", department);   // 소속 부서
        claims.put("roles", roles);            // 권한 목록 (배열 형태)
        claims.put("tokenType", "access");     // 토큰 타입 구분

        return createToken(claims, username, JWT_EXPIRATION);
    }

    /**
     * Refresh Token을 생성
     *
     * Access Token보다 긴 만료 시간을 가지며,
     * 새로운 Access Token을 발급받기 위해 사용됩니다.
     *
     * @param username 사용자명
     * @return String 생성된 Refresh Token
     */
    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "refresh");    // Refresh 토큰임을 명시

        return createToken(claims, username, REFRESH_EXPIRATION);
    }

    /**
     * 실제 JWT 토큰을 생성하는 내부 메서드
     *
     * JWT 라이브러리를 사용하여 토큰을 생성합니다.
     * 모든 토큰 생성 메서드가 공통으로 사용하는 핵심 로직입니다.
     *
     * @param claims 토큰에 포함할 사용자 정의 클레임들
     * @param subject 토큰의 주체 (일반적으로 사용자명)
     * @param expiration 토큰 만료 시간 (밀리초)
     * @return String 생성된 JWT 토큰
     */
    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                // 사용자 정의 클레임들을 추가
                .setClaims(claims)
                // 토큰의 주체(subject) 설정 - 일반적으로 사용자명
                .setSubject(subject)
                // 토큰 발급 시간 설정
                .setIssuedAt(now)
                // 토큰 만료 시간 설정
                .setExpiration(expiryDate)
                // HS256 알고리즘으로 서명
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                // 최종 JWT 문자열로 변환
                .compact();
    }

    /**
     * JWT 토큰에서 사용자명을 추출
     *
     * @param token JWT 토큰 문자열
     * @return String 토큰에서 추출한 사용자명
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * JWT 토큰에서 만료 시간을 추출
     *
     * @param token JWT 토큰 문자열
     * @return Date 토큰 만료 시간
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * JWT 토큰에서 특정 클레임을 추출하는 제네릭 메서드
     *
     * Function 인터페이스를 사용하여 다양한 클레임 추출 로직을
     * 하나의 메서드로 처리할 수 있습니다.
     *
     * @param <T> 반환할 클레임의 타입
     * @param token JWT 토큰 문자열
     * @param claimsResolver 클레임에서 원하는 값을 추출하는 함수
     * @return T 추출된 클레임 값
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * JWT 토큰에서 모든 클레임을 추출
     *
     * 토큰을 파싱하여 모든 클레임 정보를 Claims 객체로 반환합니다.
     * 토큰이 유효하지 않거나 만료된 경우 예외가 발생합니다.
     *
     * @param token JWT 토큰 문자열
     * @return Claims 토큰의 모든 클레임 정보
     * @throws JwtException 토큰이 유효하지 않은 경우
     */
    private Claims extractAllClaims(String token) {
        try {
            // 수정: parserBuilder() -> parser() (JWT 0.11+ 버전 호환)
            return Jwts.parser()
                    // 서명 검증용 비밀키 설정
                    .setSigningKey(getSigningKey())
                    .build()
                    // JWT 토큰 파싱 및 클레임 추출
                    .parseClaimsJws(token)
                    .getBody();
        } catch (ExpiredJwtException e) {
            // 토큰이 만료된 경우
            throw new JwtException("토큰이 만료되었습니다.", e);
        } catch (UnsupportedJwtException e) {
            // 지원되지 않는 JWT 토큰인 경우
            throw new JwtException("지원되지 않는 토큰 형식입니다.", e);
        } catch (MalformedJwtException e) {
            // 잘못된 형식의 JWT 토큰인 경우
            throw new JwtException("올바르지 않은 토큰 형식입니다.", e);
        } catch (SecurityException e) {
            // 서명이 올바르지 않은 경우
            throw new JwtException("토큰 서명이 올바르지 않습니다.", e);
        } catch (IllegalArgumentException e) {
            // 토큰이 null이거나 빈 문자열인 경우
            throw new JwtException("토큰이 비어있습니다.", e);
        }
    }

    /**
     * JWT 토큰이 만료되었는지 확인
     *
     * @param token JWT 토큰 문자열
     * @return boolean 만료된 경우 true, 아직 유효한 경우 false
     */
    private Boolean isTokenExpired(String token) {
        final Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    /**
     * JWT 토큰의 유효성을 검증
     *
     * 토큰에서 추출한 사용자명이 전달받은 사용자명과 일치하고,
     * 토큰이 만료되지 않았는지 확인합니다.
     *
     * @param token JWT 토큰 문자열
     * @param username 검증할 사용자명
     * @return boolean 토큰이 유효한 경우 true, 무효한 경우 false
     */
    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            // 사용자명 일치 여부와 토큰 만료 여부를 모두 확인
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (JwtException e) {
            // JWT 파싱 중 예외가 발생한 경우 무효한 토큰으로 처리
            return false;
        }
    }

    /**
     * JWT 토큰에서 사용자 권한 목록을 추출
     *
     * @param token JWT 토큰 문자열
     * @return List<String> 사용자 권한 목록
     */
    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    /**
     * JWT 토큰에서 사용자 전체 이름을 추출
     *
     * @param token JWT 토큰 문자열
     * @return String 사용자 전체 이름
     */
    public String extractFullName(String token) {
        return extractClaim(token, claims -> (String) claims.get("fullName"));
    }

    /**
     * JWT 토큰에서 사용자 이메일을 추출
     *
     * @param token JWT 토큰 문자열
     * @return String 사용자 이메일
     */
    public String extractEmail(String token) {
        return extractClaim(token, claims -> (String) claims.get("email"));
    }

    /**
     * JWT 토큰에서 사용자 부서를 추출
     *
     * @param token JWT 토큰 문자열
     * @return String 사용자 부서
     */
    public String extractDepartment(String token) {
        return extractClaim(token, claims -> (String) claims.get("department"));
    }
}