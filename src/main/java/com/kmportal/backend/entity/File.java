package com.kmportal.backend.entity;

import com.kmportal.backend.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * File 엔티티
 *
 * 파일 시스템의 메타데이터를 저장하는 JPA 엔티티입니다.
 * 실제 파일은 로컬 파일 시스템에 저장되고, 이 엔티티는 파일 정보만 관리합니다.
 *
 * 주요 기능:
 * 1. 파일 메타데이터 저장 (파일명, 크기, 타입 등)
 * 2. 업로드한 사용자 추적 (User와 ManyToOne 관계)
 * 3. 파일 카테고리 관리
 * 4. 생성일시/수정일시 자동 관리 (BaseEntity 상속)
 *
 * 작성일: 2025년 11월 12일 (18일차)
 * 수정일: 2025년 11월 13일 (19일차) - @EqualsAndHashCode 추가
 * 작성자: 18일차 개발 담당자
 */
@Entity
@Table(name = "files")
@Data
@EqualsAndHashCode(callSuper = false)  // ✅ 추가: BaseEntity 상속 시 경고 제거
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File extends BaseEntity {

    /**
     * 파일 ID (Primary Key)
     * 자동 증가 방식으로 생성됩니다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원본 파일명
     * 사용자가 업로드한 파일의 원래 이름입니다.
     * 예: "프로젝트_계획서.pdf", "회의록_2025_11.docx"
     *
     * NULL 허용 안 함, 최대 255자
     */
    @Column(nullable = false, length = 255)
    private String originalName;

    /**
     * 저장된 파일명
     * 서버에 실제로 저장되는 파일명입니다.
     * 파일명 중복을 방지하기 위해 UUID를 사용합니다.
     * 예: "a3f2b9c1-4d5e-6f7g-8h9i-0j1k2l3m4n5o.pdf"
     *
     * NULL 허용 안 함, 최대 255자
     */
    @Column(nullable = false, length = 255)
    private String storedName;

    /**
     * 파일 경로
     * 파일이 저장된 전체 경로입니다.
     * 연도/월별로 자동 분류됩니다.
     * 예: "/uploads/2025/11/a3f2b9c1-4d5e-6f7g-8h9i-0j1k2l3m4n5o.pdf"
     *
     * NULL 허용 안 함, 최대 500자
     */
    @Column(nullable = false, length = 500)
    private String filePath;

    /**
     * 파일 크기 (bytes)
     * 파일의 바이트 크기를 저장합니다.
     * Long 타입을 사용하여 큰 파일도 지원합니다.
     *
     * 예시:
     * - 1KB = 1,024 bytes
     * - 1MB = 1,048,576 bytes
     * - 10MB = 10,485,760 bytes
     *
     * NULL 허용 안 함
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * MIME 타입 (Content Type)
     * 파일의 형식을 나타냅니다.
     * 다운로드 시 브라우저가 파일을 올바르게 처리하는 데 필요합니다.
     *
     * 예시:
     * - "image/jpeg" : JPG 이미지
     * - "image/png" : PNG 이미지
     * - "application/pdf" : PDF 문서
     * - "application/vnd.ms-excel" : Excel 파일
     * - "application/msword" : Word 문서
     *
     * NULL 허용 안 함, 최대 100자
     */
    @Column(nullable = false, length = 100)
    private String contentType;

    /**
     * 파일 카테고리
     * 파일을 분류하기 위한 카테고리입니다.
     *
     * 예시:
     * - "DOCUMENT" : 문서 파일
     * - "IMAGE" : 이미지 파일
     * - "SPREADSHEET" : 스프레드시트
     * - "PRESENTATION" : 프레젠테이션
     * - "ETC" : 기타
     *
     * NULL 허용, 최대 50자
     */
    @Column(length = 50)
    private String category;

    /**
     * 파일 설명
     * 파일에 대한 간단한 설명이나 메모입니다.
     * 사용자가 직접 입력할 수 있습니다.
     *
     * NULL 허용, 최대 500자
     */
    @Column(length = 500)
    private String description;

    /**
     * 업로드한 사용자
     * 이 파일을 업로드한 사용자입니다.
     * User 엔티티와 다대일(ManyToOne) 관계입니다.
     *
     * 지연 로딩(LAZY)을 사용하여 성능을 최적화합니다.
     * - LAZY: 파일 정보를 조회할 때 사용자 정보는 실제로 필요할 때만 조회
     * - EAGER: 파일 정보를 조회할 때 사용자 정보도 함께 조회 (비권장)
     *
     * NULL 허용 안 함
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User uploadedBy;

    /**
     * 다운로드 횟수
     * 파일이 다운로드된 횟수를 추적합니다.
     * 기본값은 0입니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

    /**
     * 공개 여부
     * 파일의 공개/비공개 상태를 나타냅니다.
     * - true: 모든 사용자가 다운로드 가능
     * - false: 업로드한 사용자와 관리자만 다운로드 가능
     *
     * 기본값은 false (비공개)입니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * 삭제 여부
     * 파일의 삭제 상태를 나타냅니다.
     * Soft Delete 방식을 사용합니다.
     * - true: 삭제됨 (실제 파일은 남아있지만 목록에 표시 안 됨)
     * - false: 정상 상태
     *
     * 기본값은 false (정상)입니다.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * 파일 확장자 추출 메서드
     * originalName에서 확장자를 추출합니다.
     *
     * @return 파일 확장자 (예: "pdf", "jpg", "docx")
     *         확장자가 없으면 빈 문자열 반환
     */
    public String getFileExtension() {
        if (originalName == null || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 파일 크기를 읽기 쉬운 형식으로 반환하는 메서드
     *
     * @return 사람이 읽기 쉬운 파일 크기 (예: "1.5 MB", "320 KB")
     */
    public String getReadableFileSize() {
        if (fileSize == null) {
            return "0 B";
        }

        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }

    /**
     * 이미지 파일 여부 확인 메서드
     *
     * @return 이미지 파일이면 true, 아니면 false
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * PDF 파일 여부 확인 메서드
     *
     * @return PDF 파일이면 true, 아니면 false
     */
    public boolean isPdf() {
        return contentType != null && contentType.equals("application/pdf");
    }
}