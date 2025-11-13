package com.kmportal.backend.exception;

/**
 * 허용되지 않은 파일 타입 예외 클래스
 *
 * 업로드하려는 파일의 확장자가 허용 목록에 없을 때 발생합니다.
 *
 * 발생 상황:
 * - 파일 확장자가 application.yml의 file.storage.allowed-extensions에 없음
 *
 * 예시:
 * - 허용: jpg, png, pdf, docx 등
 * - 차단: exe, sh, bat 등 실행 파일
 *
 * 사용 예시:
 * throw new FileTypeNotAllowedException("exe 파일은 업로드할 수 없습니다.");
 *
 * @author KM Portal Team
 * @since 2025-11-13 (19일차)
 */
public class FileTypeNotAllowedException extends RuntimeException {

    /**
     * 메시지만으로 예외 생성
     *
     * @param message String - 예외 메시지
     */
    public FileTypeNotAllowedException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인으로 예외 생성
     *
     * @param message String - 예외 메시지
     * @param cause Throwable - 원인이 된 예외
     */
    public FileTypeNotAllowedException(String message, Throwable cause) {
        super(message, cause);
    }
}