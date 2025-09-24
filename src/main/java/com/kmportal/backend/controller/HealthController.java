package com.kmportal.backend.controller;

import com.kmportal.backend.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 헬스체크 컨트롤러
 * - API 서버 상태 확인용 기본 컨트롤러
 * - 프론트엔드와 백엔드 간 연결 테스트용
 * - 개발 초기 단계에서 통신 확인 목적
 *
 * @author KM Portal Team
 * @since 2025-09-23 (3일차)
 */
@Slf4j                          // Lombok을 통한 로그 기능 자동 생성
@RestController                 // REST API 컨트롤러 선언
@RequestMapping("/api/health")  // 기본 경로: /api/health
@CrossOrigin(origins = "*")     // 개발 단계: 모든 오리진 허용 (나중에 보안 강화 예정)
public class HealthController {

    /**
     * 기본 헬스체크 API
     * GET /api/health
     *
     * 목적:
     * - 서버가 정상적으로 실행되고 있는지 확인
     * - 프론트엔드에서 백엔드 연결 상태 체크
     *
     * @return 서버 상태 정보
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {

        // 로그 출력 (개발 단계에서 요청 확인용)
        log.info("헬스체크 API 요청 수신 - 시간: {}", LocalDateTime.now());

        try {
            // 응답 데이터 구성
            Map<String, Object> healthData = new HashMap<>();
            healthData.put("status", "UP");                    // 서버 상태
            healthData.put("timestamp", LocalDateTime.now());  // 현재 시간
            healthData.put("service", "KM Portal Backend");    // 서비스 이름
            healthData.put("version", "1.0.0");               // 버전 정보
            healthData.put("environment", "development");      // 환경 정보

            // 성공 응답 반환 (ApiResponse 공통 포맷 사용)
            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                    healthData,
                    "서버가 정상적으로 동작 중입니다."
            );

            log.info("헬스체크 응답 성공");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            // 예외 발생 시 에러 로그 및 실패 응답
            log.error("헬스체크 처리 중 오류 발생: ", e);

            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.failure(
                    "HEALTH_CHECK_ERROR",
                    "헬스체크 처리 중 오류가 발생했습니다."
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 상세 상태 정보 API
     * GET /api/health/detail
     *
     * 목적:
     * - 더 자세한 서버 상태 정보 제공
     * - 시스템 리소스 및 설정 정보 확인
     *
     * @return 상세 서버 상태 정보
     */
    @GetMapping("/detail")
    public ResponseEntity<ApiResponse<Map<String, Object>>> detailHealthCheck() {

        log.info("상세 헬스체크 API 요청 수신");

        try {
            // Java 시스템 정보 수집
            Runtime runtime = Runtime.getRuntime();

            Map<String, Object> detailData = new HashMap<>();

            // 기본 상태 정보
            detailData.put("status", "UP");
            detailData.put("timestamp", LocalDateTime.now());

            // 서비스 정보
            Map<String, Object> serviceInfo = new HashMap<>();
            serviceInfo.put("name", "KM Portal Backend");
            serviceInfo.put("version", "1.0.0");
            serviceInfo.put("environment", "development");
            serviceInfo.put("java_version", System.getProperty("java.version"));
            serviceInfo.put("spring_boot_version", "3.5.5");
            detailData.put("service", serviceInfo);

            // 시스템 리소스 정보
            Map<String, Object> systemInfo = new HashMap<>();
            systemInfo.put("total_memory_mb", runtime.totalMemory() / 1024 / 1024);
            systemInfo.put("free_memory_mb", runtime.freeMemory() / 1024 / 1024);
            systemInfo.put("used_memory_mb", (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024);
            systemInfo.put("max_memory_mb", runtime.maxMemory() / 1024 / 1024);
            systemInfo.put("available_processors", runtime.availableProcessors());
            detailData.put("system", systemInfo);

            // 데이터베이스 연결 상태 (추후 구현 예정)
            Map<String, Object> dbInfo = new HashMap<>();
            dbInfo.put("status", "NOT_CONFIGURED_YET");
            dbInfo.put("message", "데이터베이스 연결 설정이 아직 완료되지 않았습니다.");
            detailData.put("database", dbInfo);

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                    detailData,
                    "상세 헬스체크 정보를 성공적으로 조회했습니다."
            );

            log.info("상세 헬스체크 응답 성공");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("상세 헬스체크 처리 중 오류 발생: ", e);

            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.failure(
                    "DETAIL_HEALTH_CHECK_ERROR",
                    "상세 헬스체크 처리 중 오류가 발생했습니다."
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * 개발용 테스트 API
     * POST /api/health/test
     *
     * 목적:
     * - POST 요청 테스트
     * - 요청 데이터 처리 확인
     *
     * @param testData 테스트용 요청 데이터
     * @return 처리 결과
     */
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<Map<String, Object>>> testApi(
            @RequestBody(required = false) Map<String, Object> testData) {

        log.info("테스트 API 요청 수신 - 데이터: {}", testData);

        try {
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "POST 요청이 성공적으로 처리되었습니다.");
            responseData.put("received_data", testData);
            responseData.put("timestamp", LocalDateTime.now());

            ApiResponse<Map<String, Object>> response = ApiResponse.success(
                    responseData,
                    "테스트 API 호출이 성공했습니다."
            );

            log.info("테스트 API 응답 성공");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("테스트 API 처리 중 오류 발생: ", e);

            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.failure(
                    "TEST_API_ERROR",
                    "테스트 API 처리 중 오류가 발생했습니다."
            );

            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}