package com.kmportal.backend.exception;

/**
 * 파일 저장 예외 클래스
 *
 * 파일 저장 중 발생하는 모든 예외를 처리합니다.
 *
 * 발생 상황:
 * 1. 파일 저장 디렉토리 생성 실패
 * 2. 파일 저장 중 I/O 오류
 * 3. 디스크 공간 부족
 * 4. 권한 오류
 *
 * 사용 예시:
 * throw new FileStorageException("파일 저장 실패", e);
 *
 * @author KM Portal Team
 * @since 2025-11-13 (19일차)
 */
public class FileStorageException extends RuntimeException {

    /**
     * 메시지만으로 예외 생성
     *
     * @param message String - 예외 메시지
     */
    public FileStorageException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인(cause)으로 예외 생성
     *
     * 원인(cause)을 포함하면:
     * 1. 원래 예외의 스택 트레이스를 보존
     * 2. 디버깅이 쉬워짐
     * 3. 예외 체인을 추적 가능
     *
     * @param message String - 예외 메시지
     * @param cause Throwable - 원인이 된 예외
     */
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}