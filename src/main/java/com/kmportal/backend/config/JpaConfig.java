package com.kmportal.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Optional;

/**
 * JPA 설정 클래스
 *
 * 이 설정 클래스의 주요 기능들:
 * 1. JPA Auditing 활성화 - BaseEntity의 생성일/수정일 자동 관리
 * 2. JPA Repository 스캔 설정
 * 3. 트랜잭션 관리 활성화
 * 4. 현재 사용자 정보를 자동으로 엔티티에 저장하는 AuditorAware 구현
 *
 * @Configuration: 이 클래스가 Spring 설정 클래스임을 나타냄
 * @EnableJpaAuditing: JPA Auditing 기능을 활성화
 * @EnableJpaRepositories: JPA Repository 자동 스캔 및 생성 활성화
 * @EnableTransactionManagement: 트랜잭션 관리 기능 활성화
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorProvider")  // auditorProvider 빈을 감사자 제공자로 사용
@EnableJpaRepositories(basePackages = "com.kmportal.backend.repository")  // Repository 위치 지정
@EnableTransactionManagement  // @Transactional 어노테이션 처리 활성화
public class JpaConfig {

    /**
     * AuditorAware 구현체를 빈으로 등록
     *
     * AuditorAware는 Spring Data JPA에서 제공하는 인터페이스로,
     * 엔티티의 생성자(createdBy)와 수정자(updatedBy) 정보를 자동으로 설정할 때 사용
     *
     * BaseEntity의 @CreatedBy, @LastModifiedBy 어노테이션과 연동되어
     * 엔티티가 생성/수정될 때 자동으로 현재 사용자 정보를 저장
     *
     * @return AuditorAware<String> 구현체
     */
    @Bean
    public AuditorAware<String> auditorProvider() {
        return new SpringSecurityAuditorAware();
    }

    /**
     * Spring Security를 이용한 감사자 제공 클래스
     *
     * 현재 인증된 사용자의 정보를 반환하여 엔티티의 생성자/수정자 필드에 저장
     *
     * 작동 방식:
     * 1. SecurityContextHolder에서 현재 인증 정보 획득
     * 2. 인증된 사용자가 있으면 사용자명 반환
     * 3. 인증되지 않았으면 "system" 반환 (시스템 작업의 경우)
     *
     * 향후 JWT 토큰 기반 인증 시스템이 구축되면 자동으로 사용자 정보가 설정됨
     */
    public static class SpringSecurityAuditorAware implements AuditorAware<String> {

        /**
         * 현재 감사자(사용자) 정보를 반환하는 메서드
         *
         * @return Optional<String> 현재 사용자명 또는 "system"
         *
         * 반환 값 설명:
         * - 로그인한 사용자: 사용자의 username (보통 이메일)
         * - 로그인하지 않은 경우: "system" (배치 작업, 초기 데이터 등)
         * - 인증 정보가 없는 경우: Optional.empty() (auditing 필드가 null로 설정됨)
         */
        @Override
        public Optional<String> getCurrentAuditor() {

            // Spring Security Context에서 현재 인증 정보 획득
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 인증 정보가 없거나 인증되지 않은 경우
            if (authentication == null || !authentication.isAuthenticated()) {
                // 시스템 작업으로 간주 (예: 스케줄러, 초기 데이터 로딩 등)
                return Optional.of("system");
            }

            // "anonymousUser"는 Spring Security의 익명 사용자
            if ("anonymousUser".equals(authentication.getPrincipal())) {
                return Optional.of("anonymous");
            }

            // 실제 사용자 정보 반환
            String username = authentication.getName();

            // 사용자명이 없는 경우 예외 처리
            if (username == null || username.trim().isEmpty()) {
                return Optional.of("unknown");
            }

            return Optional.of(username);
        }
    }
}

/*
 * ====== JPA 설정 추가 옵션들 ======
 *
 * 향후 필요에 따라 추가할 수 있는 설정들:
 *
 * 1. 데이터베이스 방언 명시적 설정:
 *    @Bean
 *    public LocalContainerEntityManagerFactoryBean entityManagerFactory() {
 *        // SQL Server 방언 등 명시적 설정
 *    }
 *
 * 2. 쿼리 로깅 설정:
 *    spring.jpa.show-sql=true
 *    spring.jpa.properties.hibernate.format_sql=true
 *    (application.yml에서 설정)
 *
 * 3. 배치 처리 최적화:
 *    spring.jpa.properties.hibernate.jdbc.batch_size=20
 *    spring.jpa.properties.hibernate.order_inserts=true
 *
 * 4. 2차 캐시 설정:
 *    spring.jpa.properties.hibernate.cache.use_second_level_cache=true
 *    spring.jpa.properties.hibernate.cache.region.factory_class=org.hibernate.cache.jcache.JCacheRegionFactory
 *
 * 5. 연결 풀 설정:
 *    spring.datasource.hikari.maximum-pool-size=20
 *    spring.datasource.hikari.minimum-idle=5
 *
 * 6. 검증 모드 설정:
 *    spring.jpa.hibernate.ddl-auto=validate  (운영환경)
 *    spring.jpa.hibernate.ddl-auto=update   (개발환경)
 */