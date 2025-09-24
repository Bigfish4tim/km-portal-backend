package com.kmportal.backend.exception;

import com.kmportal.backend.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 전역 예외 처리기
 * - 애플리케이션 전체에서 발생하는 예외를 일관성 있게 처리
 * - 사용자에게 친화적인 오류 메시지 제공
 * - 개발자를 위한 상세한 로깅 제공
 * - Spring Boot의 ResponseEntityExceptionHandler를 상속하여 기본 예외도 처리
 *
 * @author KM Portal Team
 * @since 2025-09-23 (3일차)
 */
@Slf4j                          // Lombok을 통한 로깅 기능
@RestControllerAdvice           // 전역 예외 처리 및 자동 JSON 응답
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // ==============================================
    // 커스텀 비즈니스 예외 처리
    // ==============================================

    /**
     * BusinessException 처리
     * - 애플리케이션에서 정의한 비즈니스 로직 예외
     * - 사용자에게 직접적으로 표시할 수 있는 메시지
     *
     * @param ex BusinessException 객체
     * @param request 웹 요청 정보
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Object>> handleBusinessException(
            BusinessException ex, WebRequest request) {

        log.warn("비즈니스 예외 발생: {}", ex.getMessage());
        log.debug("예외 상세 정보", ex);

        ApiResponse<Object> response = ApiResponse.failure(
                ex.getErrorCode() != null ? ex.getErrorCode() : "BUSINESS_ERROR",
                ex.getMessage()
        );

        return ResponseEntity.badRequest().body(response);
    }

    // ==============================================
    // 인증 및 권한 관련 예외 처리
    // ==============================================

    /**
     * 인증 예외 처리
     * - 로그인 실패, 토큰 만료 등 인증 관련 예외
     *
     * @param ex AuthenticationException 객체
     * @param request 웹 요청 정보
     * @return 401 Unauthorized 응답
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {

        log.warn("인증 실패: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.failure(
                "AUTHENTICATION_FAILED",
                "인증에 실패했습니다. 로그인 정보를 확인해주세요."
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 자격증명 오류 처리
     * - 잘못된 사용자명/비밀번호
     *
     * @param ex BadCredentialsException 객체
     * @param request 웹 요청 정보
     * @return 401 Unauthorized 응답
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {

        log.warn("잘못된 자격증명: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.failure(
                "INVALID_CREDENTIALS",
                "사용자명 또는 비밀번호가 올바르지 않습니다."
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 권한 부족 예외 처리
     * - 접근 권한이 없는 리소스에 대한 요청
     *
     * @param ex AccessDeniedException 객체
     * @param request 웹 요청 정보
     * @return 403 Forbidden 응답
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {

        log.warn("접근 권한 부족: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.failure(
                "ACCESS_DENIED",
                "해당 리소스에 접근할 권한이 없습니다."
        );

        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ==============================================
    // 유효성 검사 예외 처리
    // ==============================================

    /**
     * @Valid 어노테이션을 통한 유효성 검사 실패 처리
     * - Request Body 유효성 검사 실패
     *
     * @param ex MethodArgumentNotValidException 객체
     * @param request 웹 요청 정보
     * @return 422 Unprocessable Entity 응답
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex, WebRequest request) {

        log.warn("유효성 검사 실패: {}", ex.getMessage());

        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
            } else {
                errors.add(error.getDefaultMessage());
            }
        });

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                .success(false)
                .message("입력값 유효성 검사에 실패했습니다.")
                .errorCode("VALIDATION_FAILED")
                .data(errors)
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * @ModelAttribute 바인딩 예외 처리
     * - Form 데이터 바인딩 실패
     *
     * @param ex BindException 객체
     * @param request 웹 요청 정보
     * @return 422 Unprocessable Entity 응답
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleBindException(
            BindException ex, WebRequest request) {

        log.warn("바인딩 예외 발생: {}", ex.getMessage());

        List<String> errors = new ArrayList<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            if (error instanceof FieldError) {
                FieldError fieldError = (FieldError) error;
                errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
            } else {
                errors.add(error.getDefaultMessage());
            }
        });

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                .success(false)
                .message("데이터 바인딩에 실패했습니다.")
                .errorCode("BINDING_FAILED")
                .data(errors)
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    /**
     * Bean Validation 제약조건 위반 처리
     * - @NotNull, @Size 등 제약조건 위반
     *
     * @param ex ConstraintViolationException 객체
     * @param request 웹 요청 정보
     * @return 422 Unprocessable Entity 응답
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolationException(
            ConstraintViolationException ex, WebRequest request) {

        log.warn("제약조건 위반: {}", ex.getMessage());

        List<String> errors = new ArrayList<>();
        ex.getConstraintViolations().forEach(violation -> {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        });

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                .success(false)
                .message("제약조건을 위반했습니다.")
                .errorCode("CONSTRAINT_VIOLATION")
                .data(errors)
                .build();

        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(response);
    }

    // ==============================================
    // 데이터베이스 관련 예외 처리
    // ==============================================

    /**
     * 데이터 무결성 위반 처리
     * - 중복 키, 외래키 제약조건 위반 등
     *
     * @param ex DataIntegrityViolationException 객체
     * @param request 웹 요청 정보
     * @return 409 Conflict 응답
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {

        log.error("데이터 무결성 위반: {}", ex.getMessage());

        String message = "데이터 처리 중 오류가 발생했습니다.";
        String errorCode = "DATA_INTEGRITY_VIOLATION";

        // 구체적인 오류 메시지 추출
        if (ex.getMessage().contains("Duplicate entry")) {
            message = "이미 존재하는 데이터입니다.";
            errorCode = "DUPLICATE_DATA";
        } else if (ex.getMessage().contains("foreign key constraint")) {
            message = "참조 무결성 제약조건을 위반했습니다.";
            errorCode = "FOREIGN_KEY_CONSTRAINT";
        }

        ApiResponse<Object> response = ApiResponse.failure(errorCode, message);

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ==============================================
    // 일반적인 예외 처리
    // ==============================================

    /**
     * 요소를 찾을 수 없는 예외 처리
     * - Optional.get() 실패, 존재하지 않는 리소스 접근
     *
     * @param ex NoSuchElementException 객체
     * @param request 웹 요청 정보
     * @return 404 Not Found 응답
     */
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoSuchElementException(
            NoSuchElementException ex, WebRequest request) {

        log.warn("요소를 찾을 수 없음: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.failure(
                "NOT_FOUND",
                "요청한 리소스를 찾을 수 없습니다."
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * IllegalArgumentException 처리
     * - 잘못된 매개변수 전달
     *
     * @param ex IllegalArgumentException 객체
     * @param request 웹 요청 정보
     * @return 400 Bad Request 응답
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, WebRequest request) {

        log.warn("잘못된 매개변수: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.failure(
                "ILLEGAL_ARGUMENT",
                "잘못된 요청 매개변수입니다: " + ex.getMessage()
        );

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * IllegalStateException 처리
     * - 잘못된 상태에서의 메서드 호출
     *
     * @param ex IllegalStateException 객체
     * @param request 웹 요청 정보
     * @return 409 Conflict 응답
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalStateException(
            IllegalStateException ex, WebRequest request) {

        log.warn("잘못된 상태: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.failure(
                "ILLEGAL_STATE",
                "현재 상태에서는 해당 작업을 수행할 수 없습니다."
        );

        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    /**
     * NullPointerException 처리
     * - null 참조 예외
     *
     * @param ex NullPointerException 객체
     * @param request 웹 요청 정보
     * @return 500 Internal Server Error 응답
     */
    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ApiResponse<Object>> handleNullPointerException(
            NullPointerException ex, WebRequest request) {

        log.error("널 포인터 예외 발생", ex);

        ApiResponse<Object> response = ApiResponse.failure(
                "NULL_POINTER_ERROR",
                "서버에서 예상치 못한 오류가 발생했습니다."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * RuntimeException 처리
     * - 예상치 못한 런타임 예외
     *
     * @param ex RuntimeException 객체
     * @param request 웹 요청 정보
     * @return 500 Internal Server Error 응답
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Object>> handleRuntimeException(
            RuntimeException ex, WebRequest request) {

        log.error("런타임 예외 발생", ex);

        ApiResponse<Object> response = ApiResponse.failure(
                "RUNTIME_ERROR",
                "서버에서 처리 중 오류가 발생했습니다."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    /**
     * 모든 예외의 최종 처리기
     * - 위에서 처리되지 않은 모든 예외
     *
     * @param ex Exception 객체
     * @param request 웹 요청 정보
     * @return 500 Internal Server Error 응답
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex, WebRequest request) {

        log.error("예상치 못한 예외 발생", ex);

        ApiResponse<Object> response = ApiResponse.failure(
                "INTERNAL_SERVER_ERROR",
                "서버 내부 오류가 발생했습니다. 관리자에게 문의하세요."
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }

    // ==============================================
    // 유틸리티 메서드
    // ==============================================

    /**
     * 요청 URI 추출
     * - 로깅 및 디버깅용
     *
     * @param request 웹 요청
     * @return 요청 URI
     */
    private String getRequestUri(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }

    /**
     * 클라이언트 IP 추출
     * - 로깅 및 보안용
     *
     * @param request 웹 요청
     * @return 클라이언트 IP 주소
     */
    private String getClientIpAddress(WebRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return "unknown";
    }
}