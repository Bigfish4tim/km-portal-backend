// src/main/java/com/kmportal/backend/KmPortalBackendApplication.java
package com.kmportal.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * KM 포털 백엔드 애플리케이션 메인 클래스
 *
 * @SpringBootApplication 어노테이션이 다음 3개를 포함:
 * 1. @Configuration - 설정 클래스임을 표시
 * 2. @EnableAutoConfiguration - Spring Boot 자동 설정 활성화
 * 3. @ComponentScan - 컴포넌트 스캔 활성화
 */
@SpringBootApplication
public class KmPortalBackendApplication {

	/**
	 * 애플리케이션 시작점
	 * @param args 명령줄 인수
	 */
	public static void main(String[] args) {
		SpringApplication.run(KmPortalBackendApplication.class, args);
	}
}