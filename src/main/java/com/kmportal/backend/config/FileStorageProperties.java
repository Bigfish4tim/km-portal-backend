package com.kmportal.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * FileStorageProperties
 *
 * application.yml의 파일 관련 설정을 읽어오는 설정 클래스입니다.
 * @ConfigurationProperties를 사용하여 YAML 설정을 자바 객체로 매핑합니다.
 *
 * 설정 경로: file.storage.*
 *
 * 사용 방법:
 * 1. 다른 클래스에서 @Autowired로 주입
 * 2. fileStorageProperties.getPath() 형태로 사용
 *
 * 예시:
 * <code>
 * @Autowired
 * private FileStorageProperties fileStorageProperties;
 *
 * String uploadPath = fileStorageProperties.getPath();
 * List<String> allowedExtensions = fileStorageProperties.getAllowedExtensions();
 * </code>
 *
 * 작성일: 2025년 11월 12일 (18일차)
 * 작성자: 18일차 개발 담당자
 * 수정일: 2025년 11월 12일 (Lambda 에러 수정)
 */
@Configuration
@ConfigurationProperties(prefix = "file.storage")
@Data
public class FileStorageProperties {

    /**
     * 파일 저장 경로
     *
     * application.yml의 file.storage.path 값
     * 예: ${user.home}/km-portal-files
     *
     * Windows: C:\Users\사용자명\km-portal-files
     * Linux/Mac: /home/사용자명/km-portal-files
     */
    private String path;

    /**
     * 허용된 파일 확장자 목록
     *
     * application.yml의 file.storage.allowed-extensions 값
     * 예: [jpg, jpeg, png, gif, pdf, doc, docx, xls, xlsx]
     *
     * 파일 업로드 시 이 목록에 있는 확장자만 허용합니다.
     * 대소문자 구분 없이 비교합니다.
     */
    private List<String> allowedExtensions;

    /**
     * 개별 파일 최대 크기
     *
     * application.yml의 file.storage.max-size 값
     * 예: "50MB"
     *
     * Spring Boot는 자동으로 문자열을 바이트로 변환하지 않으므로
     * 실제 사용 시에는 파싱이 필요합니다.
     */
    private String maxSize;

    /**
     * 사용자당 전체 파일 크기 제한
     *
     * application.yml의 file.storage.max-total-size 값
     * 예: "500MB"
     */
    private String maxTotalSize;

    /**
     * 디렉토리 자동 생성 여부
     *
     * application.yml의 file.storage.auto-create-directory 값
     * 기본값: true
     *
     * true이면 파일 저장 경로가 없을 때 자동으로 생성합니다.
     */
    private Boolean autoCreateDirectory = true;

    /**
     * 파일명 생성 방식
     *
     * application.yml의 file.storage.filename-strategy 값
     * 가능한 값: uuid, timestamp, original
     * 기본값: uuid
     *
     * - uuid: UUID를 사용하여 고유한 파일명 생성 (권장)
     * - timestamp: 현재 시간을 사용하여 파일명 생성
     * - original: 원본 파일명 그대로 사용 (중복 가능성 있음)
     */
    private String filenameStrategy = "uuid";

    /**
     * 중복 파일 처리 방식
     *
     * application.yml의 file.storage.duplicate-strategy 값
     * 가능한 값: rename, overwrite, error
     * 기본값: rename
     *
     * - rename: 중복 시 파일명 뒤에 숫자 추가 (예: file(1).pdf)
     * - overwrite: 기존 파일 덮어쓰기
     * - error: 중복 시 에러 발생
     */
    private String duplicateStrategy = "rename";

    /**
     * 파일 크기 문자열을 바이트로 변환하는 유틸리티 메서드
     *
     * @param sizeStr 파일 크기 문자열 (예: "50MB", "10GB")
     * @return 바이트 단위의 파일 크기
     * @throws IllegalArgumentException 잘못된 형식의 문자열인 경우
     */
    public long parseSize(String sizeStr) {
        if (sizeStr == null || sizeStr.trim().isEmpty()) {
            throw new IllegalArgumentException("파일 크기 문자열이 비어있습니다.");
        }

        sizeStr = sizeStr.trim().toUpperCase();

        // 숫자와 단위 분리
        String numericPart = sizeStr.replaceAll("[^0-9.]", "");
        String unitPart = sizeStr.replaceAll("[0-9.]", "");

        if (numericPart.isEmpty()) {
            throw new IllegalArgumentException("숫자가 포함되지 않은 파일 크기 문자열입니다: " + sizeStr);
        }

        double value = Double.parseDouble(numericPart);

        // 단위에 따라 바이트로 변환
        switch (unitPart) {
            case "B":
            case "":
                return (long) value;
            case "KB":
                return (long) (value * 1024);
            case "MB":
                return (long) (value * 1024 * 1024);
            case "GB":
                return (long) (value * 1024 * 1024 * 1024);
            default:
                throw new IllegalArgumentException("지원하지 않는 파일 크기 단위입니다: " + unitPart);
        }
    }

    /**
     * 최대 파일 크기를 바이트로 반환
     *
     * @return 최대 파일 크기 (bytes)
     */
    public long getMaxSizeInBytes() {
        return parseSize(maxSize);
    }

    /**
     * 최대 전체 파일 크기를 바이트로 반환
     *
     * @return 최대 전체 파일 크기 (bytes)
     */
    public long getMaxTotalSizeInBytes() {
        return parseSize(maxTotalSize);
    }

    /**
     * 파일 확장자가 허용되는지 확인
     * 대소문자 구분 없이 비교합니다.
     *
     * @param extension 확인할 파일 확장자 (점 없이, 예: "jpg", "PDF")
     * @return 허용되면 true, 아니면 false
     */
    public boolean isAllowedExtension(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return false;
        }

        // 확장자 정규화: 소문자 변환 및 앞의 점(.) 제거
        String normalizedExtension = extension.toLowerCase().trim();

        // 점(.)이 포함되어 있으면 제거
        if (normalizedExtension.startsWith(".")) {
            normalizedExtension = normalizedExtension.substring(1);
        }

        // Lambda 표현식에서 사용할 final 변수 생성
        // Java의 Lambda는 final 또는 effectively final 변수만 캡처 가능
        final String finalExtension = normalizedExtension;

        return allowedExtensions.stream()
                .anyMatch(allowed -> allowed.equalsIgnoreCase(finalExtension));
    }

    /**
     * 설정 값 검증 메서드
     * 애플리케이션 시작 시 설정이 올바른지 확인합니다.
     *
     * @return 검증 성공 시 true
     * @throws IllegalStateException 설정이 올바르지 않은 경우
     */
    public boolean validate() {
        // 파일 저장 경로 확인
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalStateException("파일 저장 경로(file.storage.path)가 설정되지 않았습니다.");
        }

        // 허용 확장자 확인
        if (allowedExtensions == null || allowedExtensions.isEmpty()) {
            throw new IllegalStateException("허용된 파일 확장자(file.storage.allowed-extensions)가 설정되지 않았습니다.");
        }

        // 파일 크기 설정 확인
        if (maxSize == null || maxSize.trim().isEmpty()) {
            throw new IllegalStateException("최대 파일 크기(file.storage.max-size)가 설정되지 않았습니다.");
        }

        // 파일 크기 파싱 테스트
        try {
            getMaxSizeInBytes();
        } catch (Exception e) {
            throw new IllegalStateException("최대 파일 크기 설정이 올바르지 않습니다: " + maxSize, e);
        }

        // 전체 파일 크기 설정 확인
        if (maxTotalSize != null && !maxTotalSize.trim().isEmpty()) {
            try {
                getMaxTotalSizeInBytes();
            } catch (Exception e) {
                throw new IllegalStateException("최대 전체 파일 크기 설정이 올바르지 않습니다: " + maxTotalSize, e);
            }
        }

        return true;
    }

    /**
     * 설정 정보를 문자열로 반환 (디버깅용)
     *
     * @return 설정 정보 문자열
     */
    @Override
    public String toString() {
        return "FileStorageProperties{" +
                "path='" + path + '\'' +
                ", allowedExtensions=" + allowedExtensions +
                ", maxSize='" + maxSize + '\'' +
                ", maxTotalSize='" + maxTotalSize + '\'' +
                ", autoCreateDirectory=" + autoCreateDirectory +
                ", filenameStrategy='" + filenameStrategy + '\'' +
                ", duplicateStrategy='" + duplicateStrategy + '\'' +
                '}';
    }
}