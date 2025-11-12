package com.kmportal.backend.repository;

import com.kmportal.backend.entity.File;
import com.kmportal.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * FileRepository
 *
 * File 엔티티를 위한 JPA Repository 인터페이스입니다.
 * Spring Data JPA가 자동으로 구현체를 생성합니다.
 *
 * 주요 기능:
 * 1. 기본 CRUD 메서드 (JpaRepository에서 상속)
 * 2. 사용자별 파일 조회
 * 3. 파일명 검색
 * 4. 카테고리별 조회
 * 5. 삭제되지 않은 파일만 조회 (Soft Delete)
 *
 * 작성일: 2025년 11월 12일 (18일차)
 * 작성자: 18일차 개발 담당자
 */
@Repository
public interface FileRepository extends JpaRepository<File, Long> {

    /**
     * 삭제되지 않은 모든 파일 조회 (페이징)
     * Soft Delete 방식을 사용하여 isDeleted가 false인 파일만 조회합니다.
     *
     * @param pageable 페이징 정보 (페이지 번호, 페이지 크기, 정렬)
     * @return 삭제되지 않은 파일 목록 (페이징)
     */
    Page<File> findByIsDeletedFalse(Pageable pageable);

    /**
     * 특정 사용자가 업로드한 파일 조회 (페이징)
     * 삭제되지 않은 파일만 조회합니다.
     *
     * @param uploadedBy 업로드한 사용자
     * @param pageable 페이징 정보
     * @return 해당 사용자가 업로드한 파일 목록 (페이징)
     */
    Page<File> findByUploadedByAndIsDeletedFalse(User uploadedBy, Pageable pageable);

    /**
     * 파일명으로 검색 (페이징)
     * originalName에 검색어가 포함된 파일을 찾습니다.
     * 대소문자를 구분하지 않고, 삭제되지 않은 파일만 조회합니다.
     *
     * @param keyword 검색어
     * @param pageable 페이징 정보
     * @return 검색 결과 파일 목록 (페이징)
     */
    Page<File> findByOriginalNameContainingIgnoreCaseAndIsDeletedFalse(
            String keyword,
            Pageable pageable
    );

    /**
     * 카테고리별 파일 조회 (페이징)
     * 삭제되지 않은 파일만 조회합니다.
     *
     * @param category 파일 카테고리 (예: "DOCUMENT", "IMAGE")
     * @param pageable 페이징 정보
     * @return 해당 카테고리의 파일 목록 (페이징)
     */
    Page<File> findByCategoryAndIsDeletedFalse(String category, Pageable pageable);

    /**
     * 공개 파일 조회 (페이징)
     * isPublic이 true이고 삭제되지 않은 파일만 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 공개 파일 목록 (페이징)
     */
    Page<File> findByIsPublicTrueAndIsDeletedFalse(Pageable pageable);

    /**
     * 특정 사용자의 파일 개수 조회
     * 삭제되지 않은 파일만 카운트합니다.
     *
     * @param uploadedBy 업로드한 사용자
     * @return 해당 사용자의 파일 개수
     */
    Long countByUploadedByAndIsDeletedFalse(User uploadedBy);

    /**
     * 카테고리별 파일 개수 조회
     * 삭제되지 않은 파일만 카운트합니다.
     *
     * @param category 파일 카테고리
     * @return 해당 카테고리의 파일 개수
     */
    Long countByCategoryAndIsDeletedFalse(String category);

    /**
     * 특정 사용자의 파일 목록 조회 (최근 업로드 순)
     * 삭제되지 않은 파일을 최근 업로드 순으로 조회합니다.
     *
     * @param uploadedBy 업로드한 사용자
     * @return 최근 업로드 순 파일 목록
     */
    List<File> findTop10ByUploadedByAndIsDeletedFalseOrderByCreatedAtDesc(User uploadedBy);

    /**
     * 저장된 파일명으로 파일 조회
     * 삭제되지 않은 파일만 조회합니다.
     *
     * @param storedName 저장된 파일명 (UUID)
     * @return 파일 Optional
     */
    Optional<File> findByStoredNameAndIsDeletedFalse(String storedName);

    /**
     * 복합 검색 쿼리
     * 파일명, 설명, 카테고리를 동시에 검색합니다.
     * 삭제되지 않은 파일만 조회합니다.
     *
     * @param keyword 검색어
     * @param category 카테고리 (null 허용)
     * @param uploadedBy 업로드한 사용자 (null 허용)
     * @param pageable 페이징 정보
     * @return 검색 결과 파일 목록 (페이징)
     *
     * JPQL을 사용한 복합 검색:
     * - :keyword가 파일명 또는 설명에 포함
     * - :category가 null이 아니면 해당 카테고리만
     * - :uploadedBy가 null이 아니면 해당 사용자만
     * - 항상 isDeleted = false인 파일만
     */
    @Query("SELECT f FROM File f WHERE " +
            "f.isDeleted = false AND " +
            "(:keyword IS NULL OR " +
            " LOWER(f.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            " LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
            "(:category IS NULL OR f.category = :category) AND " +
            "(:uploadedBy IS NULL OR f.uploadedBy = :uploadedBy)")
    Page<File> searchFiles(
            @Param("keyword") String keyword,
            @Param("category") String category,
            @Param("uploadedBy") User uploadedBy,
            Pageable pageable
    );

    /**
     * 특정 기간 내 업로드된 파일 조회
     * 삭제되지 않은 파일만 조회합니다.
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @param pageable 페이징 정보
     * @return 해당 기간의 파일 목록 (페이징)
     */
    @Query("SELECT f FROM File f WHERE " +
            "f.isDeleted = false AND " +
            "f.createdAt BETWEEN :startDate AND :endDate")
    Page<File> findFilesCreatedBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * 파일 타입별 개수 조회
     * 삭제되지 않은 파일의 contentType별 개수를 조회합니다.
     * 통계 데이터 생성에 사용됩니다.
     *
     * @return 파일 타입별 개수 리스트 (Object[] 형태: [contentType, count])
     */
    @Query("SELECT f.contentType, COUNT(f) FROM File f WHERE " +
            "f.isDeleted = false " +
            "GROUP BY f.contentType")
    List<Object[]> countByContentType();

    /**
     * 전체 파일 크기 합계 조회
     * 삭제되지 않은 파일의 총 크기를 바이트 단위로 반환합니다.
     *
     * @return 전체 파일 크기 (bytes)
     */
    @Query("SELECT SUM(f.fileSize) FROM File f WHERE f.isDeleted = false")
    Long getTotalFileSize();

    /**
     * 특정 사용자의 파일 크기 합계 조회
     * 삭제되지 않은 파일의 총 크기를 바이트 단위로 반환합니다.
     *
     * @param uploadedBy 업로드한 사용자
     * @return 해당 사용자의 파일 크기 합계 (bytes)
     */
    @Query("SELECT SUM(f.fileSize) FROM File f WHERE " +
            "f.uploadedBy = :uploadedBy AND f.isDeleted = false")
    Long getTotalFileSizeByUser(@Param("uploadedBy") User uploadedBy);

    /**
     * 다운로드 횟수가 많은 파일 조회 (Top N)
     * 삭제되지 않은 파일 중 다운로드가 많은 순으로 조회합니다.
     *
     * @param pageable 페이징 정보 (주로 상위 10개 정도)
     * @return 다운로드 횟수가 많은 파일 목록
     */
    Page<File> findByIsDeletedFalseOrderByDownloadCountDesc(Pageable pageable);
}