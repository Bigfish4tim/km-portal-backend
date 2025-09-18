// src/main/java/com/kmportal/backend/controller/TestController.java
package com.kmportal.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 애플리케이션 동작 확인을 위한 테스트 컨트롤러
 */
@RestController
@RequestMapping("/test")
public class TestController {

    /**
     * 서버 상태 확인용 엔드포인트
     * @return 상태 메시지
     */
    @GetMapping("/health")
    public String healthCheck() {
        return "KM Portal Backend is running successfully!";
    }
}