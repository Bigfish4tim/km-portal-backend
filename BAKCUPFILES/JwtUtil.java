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
 * JWT 토큰 생성, 검증, 파싱을 담당하는 유틸리티 클래스 (0.12.6 호환)
 *
 * @author KM Portal Team
 * @version 1.0
 * @since 2025-09-24
 */
@Component
public class JwtUtil {

    private static final long JWT_EXPIRATION = 86400000; // 24시간
    private static final long REFRESH_EXPIRATION = 604800000; // 7일

    private String secret = "myVerySecretKeyForKMPortal2025!@#$%^&*()_+1234567890";
    private SecretKey secretKey;

    private SecretKey getSigningKey() {
        if (secretKey == null) {
            secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        }
        return secretKey;
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username, JWT_EXPIRATION);
    }

    public String generateToken(String username, String fullName, String email,
                                String department, List<String> roles) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("fullName", fullName);
        claims.put("email", email);
        claims.put("department", department);
        claims.put("roles", roles);
        claims.put("tokenType", "access");
        return createToken(claims, username, JWT_EXPIRATION);
    }

    public String generateRefreshToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("tokenType", "refresh");
        return createToken(claims, username, REFRESH_EXPIRATION);
    }

    private String createToken(Map<String, Object> claims, String subject, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .claims(claims)      // 수정: setClaims -> claims
                .subject(subject)    // 수정: setSubject -> subject
                .issuedAt(now)      // 수정: setIssuedAt -> issuedAt
                .expiration(expiryDate) // 수정: setExpiration -> expiration
                .signWith(getSigningKey()) // 수정: SignatureAlgorithm 제거
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())    // 수정: setSigningKey -> verifyWith
                    .build()
                    .parseSignedClaims(token)       // 수정: parseClaimsJws -> parseSignedClaims
                    .getPayload();                  // 수정: getBody -> getPayload
        } catch (Exception e) {
            throw new JwtException("토큰 파싱 실패: " + e.getMessage(), e);
        }
    }

    public Boolean isTokenExpired(String token) {
        final Date expiration = extractExpiration(token);
        return expiration.before(new Date());
    }

    public Boolean validateToken(String token, String username) {
        try {
            final String extractedUsername = extractUsername(token);
            return (extractedUsername.equals(username) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public List<String> extractRoles(String token) {
        return extractClaim(token, claims -> (List<String>) claims.get("roles"));
    }

    public String extractFullName(String token) {
        return extractClaim(token, claims -> (String) claims.get("fullName"));
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> (String) claims.get("email"));
    }

    public String extractDepartment(String token) {
        return extractClaim(token, claims -> (String) claims.get("department"));
    }
}