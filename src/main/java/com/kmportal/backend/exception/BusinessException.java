package com.kmportal.backend.exception;

/**
 * 비즈니스 로직 예외 클래스
 *
 * 이 예외는 다음과 같은 상황에서 사용됩니다:
 * 1. 비즈니스 규칙 위반 (예: 이미 존재하는 이메일로 회원가입)
 * 2. 잘못된 요청 데이터 (예: 지원하지 않는 파일 형식)
 * 3. 권한 관련 문제 (예: 다른 사용자의 게시글 수정 시도)
 * 4. 상태 관련 문제 (예: 이미 삭제된 게시글 수정 시도)
 *
 * RuntimeException을 상속받아 언체크드 예외로 구현
 * - 컴파일러가 강제하지 않음
 * - Spring의 트랜잭션 롤백 정책에 적합 (기본적으로 RuntimeException에서만 롤백)
 *
 * 사용 예시:
 * throw new BusinessException("이미 사용 중인 이메일입니다.");
 * throw new BusinessException("파일 크기는 10MB를 초과할 수 없습니다.");
 */
public class BusinessException extends RuntimeException {

    /**
     * 에러 코드 (선택적)
     * - API 응답에서 구체적인 에러 타입을 구분하기 위한 코드
     * - 예: "USER_ALREADY_EXISTS", "INVALID_FILE_TYPE" 등
     */
    private final String errorCode;

    /**
     * 메시지만 포함하는 기본 생성자
     *
     * @param message 예외 메시지 (사용자에게 표시될 메시지)
     *
     * 사용 예시:
     * throw new BusinessException("이미 존재하는 사용자입니다.");
     */
    public BusinessException(String message) {
        super(message);
        this.errorCode = null;
    }

    /**
     * 에러 코드와 메시지를 포함하는 생성자
     *
     * @param errorCode 에러 코드
     * @param message 예외 메시지
     *
     * 사용 예시:
     * throw new BusinessException("USER_ALREADY_EXISTS", "이미 존재하는 사용자입니다.");
     */
    public BusinessException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * 메시지와 원인 예외를 포함하는 생성자
     *
     * @param message 예외 메시지
     * @param cause 원인이 된 예외 (다른 예외를 래핑할 때 사용)
     *
     * 사용 예시:
     * try {
     *     fileService.upload(file);
     * } catch (IOException e) {
     *     throw new BusinessException("파일 업로드에 실패했습니다.", e);
     * }
     */
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
    }

    /**
     * 에러 코드, 메시지, 원인 예외를 모두 포함하는 생성자
     *
     * @param errorCode 에러 코드
     * @param message 예외 메시지
     * @param cause 원인이 된 예외
     */
    public BusinessException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * 원인 예외만 포함하는 생성자
     *
     * @param cause 원인이 된 예외
     *
     * 사용 예시:
     * throw new BusinessException(someCheckedException);
     */
    public BusinessException(Throwable cause) {
        super(cause);
        this.errorCode = null;
    }

    /**
     * 에러 코드 반환
     *
     * @return 에러 코드 (없으면 null)
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * 에러 코드 존재 여부 확인
     *
     * @return 에러 코드가 있으면 true
     */
    public boolean hasErrorCode() {
        return errorCode != null && !errorCode.trim().isEmpty();
    }
}

/**
 * 인증 관련 예외 클래스
 *
 * JWT 토큰, 로그인/로그아웃 등 인증 관련 예외에 특화
 * BusinessException의 하위 클래스로 구현하여 일관성 유지
 *
 * 사용 상황:
 * 1. JWT 토큰이 만료되었을 때
 * 2. 잘못된 토큰 형식일 때
 * 3. 토큰 서명이 유효하지 않을 때
 * 4. 로그인 시도 횟수 초과할 때
 */
class AuthException extends BusinessException {

    /**
     * 인증 예외 기본 생성자
     *
     * @param message 인증 관련 오류 메시지
     */
    public AuthException(String message) {
        super("AUTH_ERROR", message);
    }

    /**
     * 원인 예외와 함께 인증 예외 생성
     *
     * @param message 인증 관련 오류 메시지
     * @param cause 원인이 된 예외
     */
    public AuthException(String message, Throwable cause) {
        super("AUTH_ERROR", message, cause);
    }

    /**
     * 커스텀 에러 코드와 함께 인증 예외 생성
     *
     * @param errorCode 커스텀 에러 코드
     * @param message 인증 관련 오류 메시지
     */
    public AuthException(String errorCode, String message) {
        super(errorCode, message);
    }
}

/**
 * 파일 처리 관련 예외 클래스
 *
 * 파일 업로드, 다운로드, 처리 중 발생하는 예외에 특화
 *
 * 사용 상황:
 * 1. 지원하지 않는 파일 형식일 때
 * 2. 파일 크기가 제한을 초과할 때
 * 3. 파일 저장/읽기 중 오류가 발생할 때
 * 4. 바이러스가 검출되었을 때 (향후 추가 가능)
 */
class FileException extends BusinessException {

    /**
     * 파일 예외 기본 생성자
     *
     * @param message 파일 관련 오류 메시지
     */
    public FileException(String message) {
        super("FILE_ERROR", message);
    }

    /**
     * 원인 예외와 함께 파일 예외 생성
     *
     * @param message 파일 관련 오류 메시지
     * @param cause 원인이 된 예외 (보통 IOException)
     */
    public FileException(String message, Throwable cause) {
        super("FILE_ERROR", message, cause);
    }

    /**
     * 커스텀 에러 코드와 함께 파일 예외 생성
     *
     * @param errorCode 커스텀 에러 코드
     * @param message 파일 관련 오류 메시지
     */
    public FileException(String errorCode, String message) {
        super(errorCode, message);
    }
}

/**
 * 데이터 검증 관련 예외 클래스
 *
 * Bean Validation 외에 추가적인 비즈니스 검증에서 사용
 *
 * 사용 상황:
 * 1. 복잡한 비즈니스 규칙 검증 실패
 * 2. 데이터 간 일관성 검증 실패
 * 3. 외부 API 호출 결과에 대한 검증 실패
 */
class ValidationException extends BusinessException {

    /**
     * 검증 예외 기본 생성자
     *
     * @param message 검증 관련 오류 메시지
     */
    public ValidationException(String message) {
        super("VALIDATION_ERROR", message);
    }

    /**
     * 여러 검증 오류를 하나의 메시지로 합성
     *
     * @param fieldName 검증 실패한 필드명
     * @param value 검증 실패한 값
     * @param constraint 위반된 제약 조건
     */
    public ValidationException(String fieldName, Object value, String constraint) {
        super("FIELD_VALIDATION_ERROR",
                String.format("필드 '%s'의 값 '%s'이(가) 제약 조건을 위반했습니다: %s",
                        fieldName, value, constraint));
    }
}

/*
 * ====== 향후 추가 가능한 예외 클래스들 ======
 *
 * 1. 리소스 관련 예외:
 *    class ResourceNotFoundException extends BusinessException
 *    class ResourceAlreadyExistsException extends BusinessException
 *
 * 2. 권한 관련 예외:
 *    class PermissionDeniedException extends BusinessException
 *    class RoleNotFoundException extends BusinessException
 *
 * 3. 외부 서비스 관련 예외:
 *    class ExternalServiceException extends BusinessException
 *    class TimeoutException extends BusinessException
 *
 * 4. 상태 관련 예외:
 *    class InvalidStateException extends BusinessException
 *    class StateTransitionException extends BusinessException
 */