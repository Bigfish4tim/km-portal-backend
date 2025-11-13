package com.kmportal.backend.exception;

/**
 * 파일 크기 초과 예외 클래스
 *
 * 업로드하려는 파일의 크기가 최대 허용 크기를 초과할 때 발생합니다.
 *
 * 발생 상황:
 * - 파일 크기 > application.yml의 file.storage.max-size
 *
 * 사용 예시:
 * throw new FileSizeExceededException("파일 크기가 50MB를 초과했습니다.");
 *
 * @author KM Portal Team
 * @since 2025-11-13 (19일차)
 */
public class FileSizeExceededException extends RuntimeException {

    /**
     * 메시지만으로 예외 생성
     *
     * @param message String - 예외 메시지
     */
    public FileSizeExceededException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인으로 예외 생성
     *
     * @param message String - 예외 메시지
     * @param cause Throwable - 원인이 된 예외
     */
    public FileSizeExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}