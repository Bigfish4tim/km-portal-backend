package com.kmportal.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

/**
 * 개발 환경 전용 유틸리티 컨트롤러
 */
@RestController
@RequestMapping("/api/dev")
public class DevController {

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 비밀번호 해시 생성 (개발용)
     */
    @PostMapping("/generate-hash")
    public HashResponse generateHash(@RequestBody HashRequest request) {
        String hash = passwordEncoder.encode(request.getPassword());

        System.out.println("=================================");
        System.out.println("🔐 비밀번호 해시 생성");
        System.out.println("=================================");
        System.out.println("평문 비밀번호: " + request.getPassword());
        System.out.println("생성된 해시: " + hash);
        System.out.println("=================================");

        return new HashResponse(request.getPassword(), hash);
    }

    static class HashRequest {
        private String password;
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    static class HashResponse {
        private String plainPassword;
        private String hash;

        public HashResponse(String plainPassword, String hash) {
            this.plainPassword = plainPassword;
            this.hash = hash;
        }

        public String getPlainPassword() { return plainPassword; }
        public String getHash() { return hash; }
    }
}