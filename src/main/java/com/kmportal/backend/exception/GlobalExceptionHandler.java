package com.kmportal.backend.exception;

import com.kmportal.backend.dto.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 전역 예외 처리기 (Global Exception Handler)
 *
 * 이 클래스의 역할:
 * 1. 애플리케이션 전체에서 발생하는 모든 예외를 일관된 형태로 처리
 * 2. 클라이언트에게 명확하고 유용한 오류 메시지 제공
 * 3. 보안상 민감한 정보는 숨기고, 개발자를 위한 로그는 별도로 기록
 * 4. HTTP 상태 코드를 적절히 설정하여 RESTful API 표준 준수
 *
 * @RestControllerAdvice:
 * - @ControllerAdvice + @ResponseBody의 조합
 * - 모든 컨트롤러에서 발생하는 예외를 처리
 * - 응답을 JSON 형태로 변환
 *
 * @Slf4j:
 * - Lombok이 제공하는 로깅 어노테이션
 * - log.error(), log.warn() 등의 메서드 사용 가능
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ====== 커스텀 예외들 (향후 생성될 예외 클래스들) ======

    /**
     * 비즈니스 로직 예외 처리
     *
     * BusinessException은 향후 생성할 커스텀 예외 클래스
     * 비즈니스 규칙 위반이나 잘못된 요청에 사용
     *
     * 예시 상황:
     * - 이미 존재하는 이메일로 회원가입 시도
     * - 권한이 없는 리소스 접근
     * - 잘못된 파일 형식 업로드
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(
            BusinessException ex, HttpServletRequest request) {

        log.warn("비즈니스 예외 발생 - URL: {}, 메시지: {}",
                request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(ex.getMessage(), 400);
        return ResponseEntity.badRequest().body(response);
    }

    // ====== 인증/인가 관련 예외들 ======

    /**
     * 인증 실패 예외 처리 (로그인 실패, 잘못된 토큰 등)
     *
     * AuthenticationException: Spring Security에서 제공하는 인증 예외
     * JWT 토큰이 유효하지 않거나, 로그인 정보가 틀렸을 때 발생
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, HttpServletRequest request) {

        log.warn("인증 실패 - URL: {}, 메시지: {}",
                request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.unauthorized("인증에 실패했습니다. 로그인을 다시 시도해주세요.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 잘못된 인증 정보 예외 처리 (틀린 아이디/비밀번호)
     *
     * BadCredentialsException: 로그인 시 아이디나 비밀번호가 틀렸을 때
     * 보안을 위해 구체적인 정보는 제공하지 않음
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadCredentialsException(
            BadCredentialsException ex, HttpServletRequest request) {

        log.warn("로그인 실패 - URL: {}, IP: {}",
                request.getRequestURI(), request.getRemoteAddr());

        ApiResponse<Void> response = ApiResponse.unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    /**
     * 접근 권한 없음 예외 처리
     *
     * AccessDeniedException: 인증은 되었지만 권한이 없는 리소스에 접근할 때
     * 예: 일반 사용자가 관리자 기능에 접근하는 경우
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest request) {

        log.warn("접근 권한 없음 - URL: {}, 메시지: {}",
                request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.forbidden("해당 리소스에 접근할 권한이 없습니다.");
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    // ====== 데이터 검증 관련 예외들 ======

    /**
     * 요청 본문 검증 실패 예외 처리 (@Valid 어노테이션 사용시)
     *
     * MethodArgumentNotValidException: @RequestBody에 @Valid를 사용했을 때
     * 검증에 실패한 필드들의 상세 정보를 제공
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleValidationException(
            MethodArgumentNotValidException ex) {

        log.warn("요청 데이터 검증 실패: {}", ex.getMessage());

        List<String> errors = new ArrayList<>();

        // 각 검증 실패 필드에 대한 오류 메시지 수집
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            String errorMessage = String.format("%s: %s (입력값: %s)",
                    fieldError.getField(),
                    fieldError.getDefaultMessage(),
                    fieldError.getRejectedValue());
            errors.add(errorMessage);
        }

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                .success(false)
                .message("입력 데이터 검증에 실패했습니다.")
                .data(errors)
                .statusCode(400)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 폼 데이터 바인딩 실패 예외 처리
     *
     * BindException: 폼 데이터나 쿼리 파라미터 검증 실패시
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleBindException(BindException ex) {

        log.warn("데이터 바인딩 실패: {}", ex.getMessage());

        List<String> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                .success(false)
                .message("요청 데이터가 올바르지 않습니다.")
                .data(errors)
                .statusCode(400)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 제약 조건 위반 예외 처리
     *
     * ConstraintViolationException: Bean Validation 제약 조건 위반시
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<List<String>>> handleConstraintViolationException(
            ConstraintViolationException ex) {

        log.warn("제약 조건 위반: {}", ex.getMessage());

        List<String> errors = new ArrayList<>();
        Set<ConstraintViolation<?>> violations = ex.getConstraintViolations();

        for (ConstraintViolation<?> violation : violations) {
            errors.add(violation.getPropertyPath() + ": " + violation.getMessage());
        }

        ApiResponse<List<String>> response = ApiResponse.<List<String>>builder()
                .success(false)
                .message("데이터 제약 조건을 위반했습니다.")
                .data(errors)
                .statusCode(400)
                .build();

        return ResponseEntity.badRequest().body(response);
    }

    // ====== 데이터베이스 관련 예외들 ======

    /**
     * 엔티티를 찾을 수 없음 예외 처리
     *
     * EntityNotFoundException: JPA에서 엔티티를 찾을 수 없을 때
     * 예: 존재하지 않는 사용자 ID로 조회할 때
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleEntityNotFoundException(
            EntityNotFoundException ex, HttpServletRequest request) {

        log.warn("엔티티 없음 - URL: {}, 메시지: {}",
                request.getRequestURI(), ex.getMessage());

        ApiResponse<Void> response = ApiResponse.notFound("요청한 리소스를 찾을 수 없습니다.");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    /**
     * 데이터 무결성 위반 예외 처리
     *
     * DataIntegrityViolationException: DB 제약 조건 위반시
     * 예: 중복된 이메일로 사용자 생성, 외래키 제약 위반 등
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("데이터 무결성 위반 - URL: {}, 메시지: {}",
                request.getRequestURI(), ex.getMessage());

        // 사용자에게는 일반적인 메시지, 개발자에게는 상세 로그
        String userMessage = "데이터 처리 중 오류가 발생했습니다. 입력 정보를 확인해주세요.";

        // 중복 키 오류인 경우 더 명확한 메시지 제공
        if (ex.getMessage() != null && ex.getMessage().toLowerCase().contains("duplicate")) {
            userMessage = "이미 존재하는 데이터입니다. 다른 값을 입력해주세요.";
        }

        ApiResponse<Void> response = ApiResponse.error(userMessage, 409);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
    }

    // ====== 파일 업로드 관련 예외들 ======

    /**
     * 파일 크기 초과 예외 처리
     *
     * MaxUploadSizeExceededException: 업로드 파일 크기가 설정된 최대값을 초과할 때
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex) {

        log.warn("파일 크기 초과: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                "업로드 파일 크기가 제한을 초과했습니다. 파일 크기를 확인해주세요.", 413);

        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(response);
    }

    // ====== HTTP 요청 관련 예외들 ======

    /**
     * 지원하지 않는 HTTP 메서드 예외 처리
     *
     * HttpRequestMethodNotSupportedException: GET만 지원하는 URL에 POST 요청을 보낼 때
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpRequestMethodNotSupportedException(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        log.warn("지원하지 않는 HTTP 메서드 - URL: {}, 메서드: {}",
                request.getRequestURI(), request.getMethod());

        String message = String.format("'%s' 메서드는 지원하지 않습니다. 지원되는 메서드: %s",
                ex.getMethod(), String.join(", ", ex.getSupportedMethods()));

        ApiResponse<Void> response = ApiResponse.error(message, 405);
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(response);
    }

    /**
     * 필수 요청 파라미터 누락 예외 처리
     *
     * MissingServletRequestParameterException: @RequestParam(required = true) 파라미터가 없을 때
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingServletRequestParameterException(
            MissingServletRequestParameterException ex) {

        log.warn("필수 파라미터 누락: {}", ex.getMessage());

        String message = String.format("필수 파라미터가 누락되었습니다: %s (%s)",
                ex.getParameterName(), ex.getParameterType());

        ApiResponse<Void> response = ApiResponse.error(message, 400);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * 잘못된 파라미터 타입 예외 처리
     *
     * MethodArgumentTypeMismatchException: 숫자 파라미터에 문자열을 보낼 때
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex) {

        log.warn("파라미터 타입 불일치: {}", ex.getMessage());

        String message = String.format("파라미터 '%s'의 값 '%s'이(가) 올바르지 않습니다. 예상 타입: %s",
                ex.getName(), ex.getValue(), ex.getRequiredType().getSimpleName());

        ApiResponse<Void> response = ApiResponse.error(message, 400);
        return ResponseEntity.badRequest().body(response);
    }

    /**
     * JSON 파싱 오류 예외 처리
     *
     * HttpMessageNotReadableException: 잘못된 JSON 형식일 때
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException ex) {

        log.warn("JSON 파싱 오류: {}", ex.getMessage());

        ApiResponse<Void> response = ApiResponse.error(
                "요청 본문이 올바른 JSON 형식이 아닙니다. 데이터 형식을 확인해주세요.", 400);

        return ResponseEntity.badRequest().body(response);
    }

    // ====== 기본 예외 처리 ======

    /**
     * 처리되지 않은 모든 예외에 대한 기본 처리
     *
     * Exception: 위에서 처리하지 않은 모든 예외
     * 이 메서드는 맨 마지막에 실행되어야 하므로 순서가 중요
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(
            Exception ex, HttpServletRequest request) {

        // 예상하지 못한 오류는 ERROR 레벨로 로깅
        log.error("예상하지 못한 오류 발생 - URL: {}, 예외 타입: {}, 메시지: {}",
                request.getRequestURI(), ex.getClass().getSimpleName(), ex.getMessage(), ex);

        // 사용자에게는 일반적인 오류 메시지만 제공 (보안상 상세 정보 숨김)
        ApiResponse<Void> response = ApiResponse.error(
                "서버에서 오류가 발생했습니다. 잠시 후 다시 시도해주세요.", 500);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}