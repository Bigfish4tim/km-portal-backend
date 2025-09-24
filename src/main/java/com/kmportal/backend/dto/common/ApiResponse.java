package com.kmportal.backend.dto.common;

import lombok.*;

import java.time.LocalDateTime;

/**
 * API 응답 공통 포맷 클래스
 * - 모든 REST API의 응답을 표준화하여 일관성 제공
 * - 성공/실패 구분을 명확히 하고 클라이언트에서 처리하기 쉽도록 구성
 * - 제네릭 타입 지원으로 다양한 데이터 타입 대응
 *
 * 응답 형식:
 * {
 *   "success": true/false,
 *   "message": "응답 메시지",
 *   "data": { 실제 데이터 },
 *   "errorCode": "ERROR_CODE", (실패시만)
 *   "timestamp": "2025-09-23T..."
 * }
 *
 * @param <T> 응답 데이터의 타입
 * @author KM Portal Team
 * @since 2025-09-23 (3일차)
 */
@Getter @Setter                     // Lombok: getter/setter 자동 생성
@NoArgsConstructor                  // Lombok: 기본 생성자 자동 생성
@AllArgsConstructor                 // Lombok: 모든 필드 생성자 자동 생성
@Builder                            // Lombok: 빌더 패턴 지원
@ToString                           // Lombok: toString 메서드 자동 생성
public class ApiResponse<T> {

    /**
     * 성공 여부
     * - true: 요청 처리 성공
     * - false: 요청 처리 실패
     */
    private boolean success;

    /**
     * 응답 메시지
     * - 사용자에게 표시할 친화적인 메시지
     * - 성공 시: 작업 완료 안내
     * - 실패 시: 오류 원인 설명
     */
    private String message;

    /**
     * 응답 데이터
     * - 성공 시: 실제 반환할 데이터
     * - 실패 시: null 또는 오류 관련 정보
     * - 제네릭 타입으로 다양한 데이터 지원
     */
    private T data;

    /**
     * 오류 코드 (실패 시에만 사용)
     * - 클라이언트에서 오류 타입을 구분할 수 있도록 제공
     * - 예: "VALIDATION_ERROR", "NOT_FOUND", "UNAUTHORIZED"
     */
    private String errorCode;

    /**
     * 응답 생성 시간
     * - API 응답이 생성된 시각
     * - 로깅 및 디버깅 용도
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    // ==============================================
    // 정적 팩토리 메서드 - 성공 응답 생성
    // ==============================================

    /**
     * 성공 응답 생성 (데이터 포함)
     * - 처리 성공 시 데이터와 함께 응답 생성
     *
     * @param data 응답할 데이터
     * @param message 성공 메시지
     * @param <T> 데이터 타입
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (데이터만)
     * - 기본 성공 메시지로 응답 생성
     *
     * @param data 응답할 데이터
     * @param <T> 데이터 타입
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, "요청이 성공적으로 처리되었습니다.");
    }

    /**
     * 성공 응답 생성 (메시지만)
     * - 데이터 없이 성공 메시지만으로 응답 생성
     * - 삭제, 업데이트 등 결과 데이터가 없는 작업에 사용
     *
     * @param message 성공 메시지
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success(String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 성공 응답 생성 (기본)
     * - 기본 성공 메시지로 응답 생성
     *
     * @return 성공 응답 객체
     */
    public static <T> ApiResponse<T> success() {
        return success("요청이 성공적으로 처리되었습니다.");
    }

    // ==============================================
    // 정적 팩토리 메서드 - 실패 응답 생성
    // ==============================================

    /**
     * 실패 응답 생성 (오류 코드 + 메시지)
     * - 처리 실패 시 오류 코드와 메시지로 응답 생성
     *
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param <T> 데이터 타입
     * @return 실패 응답 객체
     */
    public static <T> ApiResponse<T> failure(String errorCode, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .data(null)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성 (오류 코드 + 메시지 + 데이터)
     * - 실패 시에도 일부 데이터를 포함해야 하는 경우
     * - 예: 유효성 검사 실패 시 오류 필드 정보 포함
     *
     * @param errorCode 오류 코드
     * @param message 오류 메시지
     * @param data 오류 관련 데이터
     * @param <T> 데이터 타입
     * @return 실패 응답 객체
     */
    public static <T> ApiResponse<T> failure(String errorCode, String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .errorCode(errorCode)
                .data(data)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * 실패 응답 생성 (메시지만)
     * - 간단한 오류 응답 생성
     * - 기본 오류 코드 "GENERAL_ERROR" 사용
     *
     * @param message 오류 메시지
     * @param <T> 데이터 타입
     * @return 실패 응답 객체
     */
    public static <T> ApiResponse<T> failure(String message) {
        return failure("GENERAL_ERROR", message);
    }

    // ==============================================
    // 특정 상황별 응답 생성 메서드
    // ==============================================

    /**
     * 404 Not Found 응답 생성
     * - 요청한 리소스를 찾을 수 없는 경우
     *
     * @param resourceName 찾을 수 없는 리소스 이름
     * @param <T> 데이터 타입
     * @return 404 응답 객체
     */
    public static <T> ApiResponse<T> notFound(String resourceName) {
        return failure("NOT_FOUND", resourceName + "을(를) 찾을 수 없습니다.");
    }

    /**
     * 400 Bad Request 응답 생성
     * - 잘못된 요청인 경우
     *
     * @param message 오류 메시지
     * @param <T> 데이터 타입
     * @return 400 응답 객체
     */
    public static <T> ApiResponse<T> badRequest(String message) {
        return failure("BAD_REQUEST", message);
    }

    /**
     * 401 Unauthorized 응답 생성
     * - 인증이 필요한 경우
     *
     * @param <T> 데이터 타입
     * @return 401 응답 객체
     */
    public static <T> ApiResponse<T> unauthorized() {
        return failure("UNAUTHORIZED", "인증이 필요합니다.");
    }

    /**
     * 403 Forbidden 응답 생성
     * - 권한이 부족한 경우
     *
     * @param <T> 데이터 타입
     * @return 403 응답 객체
     */
    public static <T> ApiResponse<T> forbidden() {
        return failure("FORBIDDEN", "접근 권한이 없습니다.");
    }

    /**
     * 422 Validation Error 응답 생성
     * - 유효성 검사 실패인 경우
     *
     * @param message 검증 오류 메시지
     * @param <T> 데이터 타입
     * @return 422 응답 객체
     */
    public static <T> ApiResponse<T> validationError(String message) {
        return failure("VALIDATION_ERROR", message);
    }

    /**
     * 500 Internal Server Error 응답 생성
     * - 서버 내부 오류인 경우
     *
     * @param <T> 데이터 타입
     * @return 500 응답 객체
     */
    public static <T> ApiResponse<T> internalError() {
        return failure("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");
    }

    /**
     * 500 Internal Server Error 응답 생성 (메시지 포함)
     * - 서버 내부 오류인 경우 (상세 메시지 포함)
     *
     * @param message 상세 오류 메시지
     * @param <T> 데이터 타입
     * @return 500 응답 객체
     */
    public static <T> ApiResponse<T> internalError(String message) {
        return failure("INTERNAL_ERROR", message);
    }

    // ==============================================
    // 유틸리티 메서드
    // ==============================================

    /**
     * 성공 여부 확인
     *
     * @return 성공 시 true, 실패 시 false
     */
    public boolean isSuccess() {
        return this.success;
    }

    /**
     * 실패 여부 확인
     *
     * @return 실패 시 true, 성공 시 false
     */
    public boolean isFailure() {
        return !this.success;
    }

    /**
     * 오류 코드 존재 여부 확인
     *
     * @return 오류 코드가 있으면 true
     */
    public boolean hasErrorCode() {
        return this.errorCode != null && !this.errorCode.trim().isEmpty();
    }

    /**
     * 데이터 존재 여부 확인
     *
     * @return 데이터가 있으면 true
     */
    public boolean hasData() {
        return this.data != null;
    }

    /**
     * 응답 객체를 JSON 형태의 문자열로 변환
     * - 로깅 및 디버깅 용도
     *
     * @return JSON 형태의 문자열
     */
    public String toJsonString() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"success\":").append(success).append(",");
        json.append("\"message\":\"").append(message != null ? message : "").append("\",");
        json.append("\"data\":").append(data != null ? data.toString() : "null").append(",");
        if (hasErrorCode()) {
            json.append("\"errorCode\":\"").append(errorCode).append("\",");
        }
        json.append("\"timestamp\":\"").append(timestamp).append("\"");
        json.append("}");
        return json.toString();
    }
}