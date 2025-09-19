package com.kmportal.backend.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 모든 API 응답에서 사용되는 공통 응답 DTO 클래스
 *
 * 이 클래스는 다음과 같은 장점을 제공합니다:
 * 1. 일관된 API 응답 형식 유지
 * 2. 성공/실패 상태를 명확히 구분
 * 3. 클라이언트에서 예상 가능한 응답 구조
 * 4. 디버깅을 위한 타임스탬프 제공
 *
 * @param <T> 응답 데이터의 타입 (제네릭)
 *
 * 사용 예시:
 * - 성공: ApiResponse.success(data)
 * - 실패: ApiResponse.error("오류 메시지")
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) // null 값인 필드는 JSON에서 제외
public class ApiResponse<T> {

    /**
     * API 호출 성공 여부
     * true: 성공, false: 실패
     */
    private boolean success;

    /**
     * 응답 메시지
     * 성공시: 성공 메시지 또는 null
     * 실패시: 오류 메시지
     */
    private String message;

    /**
     * 실제 응답 데이터
     * 성공시: 요청된 데이터 객체
     * 실패시: null 또는 오류 상세 정보
     */
    private T data;

    /**
     * API 응답 생성 시간
     * 디버깅 및 로깅 목적으로 사용
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    /**
     * HTTP 상태 코드
     * RESTful API 표준을 따르기 위해 포함
     */
    private int statusCode;

    // ====== 정적 메서드들 (팩토리 메서드 패턴) ======

    /**
     * 성공 응답을 생성하는 메서드 (데이터 포함)
     *
     * @param data 응답할 데이터
     * @param <T> 데이터의 타입
     * @return 성공 응답 객체
     *
     * 사용 예시:
     * return ApiResponse.success(userList);
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .message("요청이 성공적으로 처리되었습니다.")
                .data(data)
                .statusCode(200)
                .build();
    }

    /**
     * 성공 응답을 생성하는 메서드 (커스텀 메시지 포함)
     *
     * @param data 응답할 데이터
     * @param message 커스텀 성공 메시지
     * @param <T> 데이터의 타입
     * @return 성공 응답 객체
     *
     * 사용 예시:
     * return ApiResponse.success(savedUser, "사용자가 성공적으로 등록되었습니다.");
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .message(message)
                .data(data)
                .statusCode(200)
                .build();
    }

    /**
     * 데이터 없이 성공 응답을 생성하는 메서드
     *
     * @param message 성공 메시지
     * @return 성공 응답 객체
     *
     * 사용 예시:
     * return ApiResponse.success("파일이 성공적으로 삭제되었습니다.");
     */
    public static ApiResponse<Void> success(String message) {
        return ApiResponse.<Void>builder()
                .success(true)
                .message(message)
                .statusCode(200)
                .build();
    }

    /**
     * 오류 응답을 생성하는 메서드
     *
     * @param message 오류 메시지
     * @return 오류 응답 객체
     *
     * 사용 예시:
     * return ApiResponse.error("사용자를 찾을 수 없습니다.");
     */
    public static ApiResponse<Void> error(String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .statusCode(500)
                .build();
    }

    /**
     * 커스텀 상태 코드와 함께 오류 응답을 생성하는 메서드
     *
     * @param message 오류 메시지
     * @param statusCode HTTP 상태 코드
     * @return 오류 응답 객체
     *
     * 사용 예시:
     * return ApiResponse.error("잘못된 요청입니다.", 400);
     */
    public static ApiResponse<Void> error(String message, int statusCode) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .statusCode(statusCode)
                .build();
    }

    /**
     * 인증 실패 응답을 생성하는 메서드
     * JWT 토큰 관련 오류에서 자주 사용될 예정
     *
     * @param message 인증 오류 메시지
     * @return 인증 실패 응답 객체
     */
    public static ApiResponse<Void> unauthorized(String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .statusCode(401)
                .build();
    }

    /**
     * 권한 없음 응답을 생성하는 메서드
     * 권한 기반 접근 제어에서 사용될 예정
     *
     * @param message 권한 오류 메시지
     * @return 권한 없음 응답 객체
     */
    public static ApiResponse<Void> forbidden(String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .statusCode(403)
                .build();
    }

    /**
     * 리소스를 찾을 수 없음 응답을 생성하는 메서드
     *
     * @param message 리소스 없음 메시지
     * @return 리소스 없음 응답 객체
     */
    public static ApiResponse<Void> notFound(String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .statusCode(404)
                .build();
    }
}