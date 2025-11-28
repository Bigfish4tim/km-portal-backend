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
 * ==== 37일차 업데이트: 쿼리 최적화 ====
 * 1. 사용자별 파일 조회 최적화 인덱스
 * 2. 파일 검색 최적화 인덱스
 * 3. 카테고리/타입별 필터링 최적화
 *
 * 작성일: 2025년 11월 12일 (18일차)
 * 수정일: 2025년 11월 28일 (37일차 - 인덱스 최적화)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.1 (37일차 인덱스 최적화)
 * @since 2025-11-12
 */
@Entity
@Table(name = "files",
        indexes = {
                // ======================================
                // 기존 단일 컬럼 인덱스
                // ======================================

                /**
                 * 업로더별 파일 조회 인덱스
                 */
                @Index(name = "idx_file_uploader", columnList = "user_id"),

                /**
                 * 원본 파일명 검색 인덱스
                 */
                @Index(name = "idx_file_original_name", columnList = "original_name"),

                // ======================================
                // 37일차 추가: 복합 인덱스 (쿼리 패턴 최적화)
                // ======================================

                /**
                 * 복합 인덱스 1: 삭제되지 않은 파일 + 생성일시 (기본 목록)
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM files
                 * WHERE is_deleted = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 높음 (파일 목록 기본 조회)
                 */
                @Index(name = "idx_file_active_created",
                        columnList = "is_deleted, created_at DESC"),

                /**
                 * 복합 인덱스 2: 사용자별 삭제되지 않은 파일
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM files
                 * WHERE user_id = ? AND is_deleted = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 높음 (내 파일 목록)
                 */
                @Index(name = "idx_file_user_active_created",
                        columnList = "user_id, is_deleted, created_at DESC"),

                /**
                 * 복합 인덱스 3: 카테고리별 삭제되지 않은 파일
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM files
                 * WHERE category = ? AND is_deleted = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 중간 (카테고리별 필터링)
                 */
                @Index(name = "idx_file_category_active",
                        columnList = "category, is_deleted, created_at DESC"),

                /**
                 * 복합 인덱스 4: MIME 타입별 파일
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM files
                 * WHERE content_type LIKE 'image/%' AND is_deleted = false
                 *
                 * 사용 빈도: 중간 (이미지 파일만 필터링 등)
                 */
                @Index(name = "idx_file_content_type",
                        columnList = "content_type, is_deleted"),

                /**
                 * 복합 인덱스 5: 공개 파일 목록
                 *
                 * 최적화 대상 쿼리:
                 * SELECT * FROM files
                 * WHERE is_public = true AND is_deleted = false
                 * ORDER BY created_at DESC
                 *
                 * 사용 빈도: 중간 (공개 파일 조회)
                 */
                @Index(name = "idx_file_public_active",
                        columnList = "is_public, is_deleted, created_at DESC")
        })
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class File extends BaseEntity {

    /**
     * 파일 ID (Primary Key)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 원본 파일명
     * NULL 허용 안 함, 최대 255자
     */
    @Column(nullable = false, length = 255)
    private String originalName;

    /**
     * 저장된 파일명 (UUID)
     * NULL 허용 안 함, 최대 255자
     */
    @Column(nullable = false, length = 255)
    private String storedName;

    /**
     * 파일 경로
     * NULL 허용 안 함, 최대 500자
     */
    @Column(nullable = false, length = 500)
    private String filePath;

    /**
     * 파일 크기 (bytes)
     * NULL 허용 안 함
     */
    @Column(nullable = false)
    private Long fileSize;

    /**
     * MIME 타입 (Content Type)
     * NULL 허용 안 함, 최대 100자
     */
    @Column(nullable = false, length = 100)
    private String contentType;

    /**
     * 파일 카테고리
     * NULL 허용, 최대 50자
     */
    @Column(length = 50)
    private String category;

    /**
     * 파일 설명
     * NULL 허용, 최대 500자
     */
    @Column(length = 500)
    private String description;

    /**
     * 업로드한 사용자
     * LAZY 로딩
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User uploadedBy;

    /**
     * 다운로드 횟수
     * 기본값 0
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer downloadCount = 0;

    /**
     * 공개 여부
     * 기본값 false
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = false;

    /**
     * 삭제 여부
     * 기본값 false
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    // ====== 비즈니스 메서드 ======

    /**
     * 파일 확장자 추출 메서드
     */
    public String getFileExtension() {
        if (originalName == null || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf(".") + 1).toLowerCase();
    }

    /**
     * 파일 크기를 읽기 쉬운 형식으로 반환
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
     * 이미지 파일 여부 확인
     */
    public boolean isImage() {
        return contentType != null && contentType.startsWith("image/");
    }

    /**
     * PDF 파일 여부 확인
     */
    public boolean isPdf() {
        return contentType != null && contentType.equals("application/pdf");
    }

    /**
     * 다운로드 횟수 증가
     */
    public void increaseDownloadCount() {
        this.downloadCount += 1;
    }

    /**
     * 파일 삭제 처리 (Soft Delete)
     */
    public void softDelete() {
        this.isDeleted = true;
        super.softDelete();
    }

    /**
     * 파일 복구
     */
    public void restore() {
        this.isDeleted = false;
        super.restore();
    }

    /**
     * 업로더 ID 조회 편의 메서드
     */
    public Long getUploaderId() {
        return this.uploadedBy != null ? this.uploadedBy.getUserId() : null;
    }

    /**
     * 업로더 이름 조회 편의 메서드
     */
    public String getUploaderName() {
        return this.uploadedBy != null ? this.uploadedBy.getFullName() : "알 수 없음";
    }
}

/*
 * ====== 37일차 파일 시스템 쿼리 최적화 가이드 ======
 *
 * 1. 파일 시스템 특성:
 *    - 파일 메타데이터만 DB에 저장 (실제 파일은 파일 시스템)
 *    - 검색 빈도: 목록 조회 > 상세 조회 > 다운로드
 *    - 사용자별 파일 분리 (내 파일 / 공개 파일)
 *
 * 2. 인덱스 설계 고려사항:
 *    - 파일명 LIKE 검색: 인덱스 효과 제한적
 *    - MIME 타입 필터링: 접두어 검색에는 효과적
 *    - 대용량 파일 목록: 페이징 필수
 *
 * 3. 성능 최적화 팁:
 *    - 파일 목록 조회 시 필요한 컬럼만 SELECT
 *    - 썸네일 표시 시 별도 썸네일 경로 저장 고려
 *    - 파일 크기 합계: SUM(file_size) 집계 쿼리 주의
 *
 * 4. 추가 최적화 고려사항:
 *    - 파일 카테고리 ENUM 타입 변환 (문자열 → 숫자)
 *    - 파일 해시값 저장 (중복 파일 탐지)
 *    - 업로드 일시 파티셔닝 (대용량 데이터 시)
 */