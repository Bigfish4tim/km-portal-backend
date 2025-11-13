package com.kmportal.backend.service;

import com.kmportal.backend.config.FileStorageProperties;
import com.kmportal.backend.entity.File;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.exception.FileSizeExceededException;
import com.kmportal.backend.exception.FileStorageException;
import com.kmportal.backend.exception.FileTypeNotAllowedException;
import com.kmportal.backend.repository.FileRepository;
import com.kmportal.backend.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 파일 관리 서비스
 *
 * 파일 업로드, 다운로드, 조회, 삭제 등의 비즈니스 로직을 처리합니다.
 *
 * 주요 기능:
 * - 파일 업로드 (로컬 파일 시스템에 저장)
 * - 파일 메타데이터 저장 (데이터베이스)
 * - 파일 검증 (크기, 확장자)
 * - 파일 조회 및 검색
 * - 파일 삭제 (Soft Delete)
 *
 * @author KM Portal Team
 * @version 1.0
 * @since 2025-11-13 (19일차)
 */
@Service
@Transactional
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Autowired
    private FileStorageProperties fileStorageProperties;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * 애플리케이션 시작 시 자동으로 실행되는 초기화 메서드
     *
     * @PostConstruct 어노테이션:
     * - 스프링이 이 클래스의 Bean을 생성한 직후 자동으로 실행
     * - 생성자 다음에 실행됨
     * - 파일 저장 디렉토리를 미리 생성하여 RuntimeException 방지
     *
     * 작업 내용:
     * 1. FileStorageProperties 설정 검증
     * 2. 파일 저장 루트 디렉토리 생성
     * 3. 연도/월별 하위 디렉토리 생성 (선택)
     */
    @PostConstruct
    public void init() {
        try {
            System.out.println("=================================");
            System.out.println("📁 파일 저장소 초기화 시작");
            System.out.println("=================================");

            // 1. FileStorageProperties 설정 검증
            fileStorageProperties.validate();
            System.out.println("✅ 파일 저장소 설정 검증 완료");

            // 2. 파일 저장 루트 디렉토리 생성
            Path uploadPath = Paths.get(fileStorageProperties.getPath());

            // 디렉토리가 없으면 생성
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("📂 파일 저장 디렉토리 생성: " + uploadPath.toAbsolutePath());
            } else {
                System.out.println("📂 파일 저장 디렉토리 이미 존재: " + uploadPath.toAbsolutePath());
            }

            // 3. 현재 연도/월 디렉토리도 미리 생성 (선택사항)
            LocalDate now = LocalDate.now();
            Path currentMonthPath = uploadPath.resolve(
                    String.format("%d/%02d", now.getYear(), now.getMonthValue())
            );

            if (!Files.exists(currentMonthPath)) {
                Files.createDirectories(currentMonthPath);
                System.out.println("📅 현재 월 디렉토리 생성: " + currentMonthPath);
            }

            System.out.println("\n=================================");
            System.out.println("📁 파일 저장소 초기화 완료");
            System.out.println("=================================");
            System.out.println("📍 저장 경로: " + uploadPath.toAbsolutePath());
            System.out.println("📏 최대 파일 크기: " + (fileStorageProperties.getMaxSizeInBytes() / 1024 / 1024) + " MB");
            System.out.println("📋 허용 확장자: " + String.join(", ", fileStorageProperties.getAllowedExtensions()));
            System.out.println("=================================\n");

        } catch (IOException e) {
            logger.error("❌ 파일 저장소 초기화 실패", e);
            throw new FileStorageException("파일 저장소 초기화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 파일 업로드 메인 메서드
     *
     * 작업 흐름:
     * 1. 파일 검증 (크기, 확장자, NULL 체크)
     * 2. 고유한 파일명 생성 (UUID)
     * 3. 저장 경로 생성 (연도/월별 자동 분류)
     * 4. 실제 파일 저장 (로컬 파일 시스템)
     * 5. 파일 메타데이터 저장 (데이터베이스)
     * 6. 업로드한 사용자 정보 연결
     *
     * @param file MultipartFile - 업로드된 파일 (Spring이 자동으로 변환)
     * @param userId Long - 파일을 업로드한 사용자의 ID
     * @return File - 저장된 파일의 엔티티 (메타데이터 포함)
     * @throws IllegalArgumentException 파일 검증 실패 시
     * @throws RuntimeException 파일 저장 실패 시
     */
    public File uploadFile(MultipartFile file, Long userId) {
        System.out.println("=================================");
        System.out.println("📤 파일 업로드 시작");
        System.out.println("=================================");
        System.out.println("📋 입력 데이터:");
        System.out.println("   - 원본 파일명: " + file.getOriginalFilename());
        System.out.println("   - 파일 크기: " + file.getSize() + " bytes (" +
                String.format("%.2f", file.getSize() / 1024.0) + " KB)");
        System.out.println("   - Content Type: " + file.getContentType());
        System.out.println("   - 업로드 사용자 ID: " + userId);

        try {
            // 1. 파일 검증
            System.out.println("\n🔍 파일 검증 시작...");
            validateFile(file);
            System.out.println("✅ 파일 검증 완료");

            // 2. 고유한 파일명 생성 (UUID 사용)
            String storedFileName = generateFileName(file.getOriginalFilename());
            System.out.println("🔑 생성된 파일명: " + storedFileName);

            // 3. 저장 경로 생성 (연도/월별 자동 분류)
            String relativePath = buildFilePath(storedFileName);
            Path targetPath = Paths.get(fileStorageProperties.getPath()).resolve(relativePath);
            System.out.println("📂 저장 경로: " + targetPath.toAbsolutePath());

            // 3-1. 디렉토리가 없으면 생성
            Files.createDirectories(targetPath.getParent());

            // 4. 실제 파일 저장
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            System.out.println("✅ 파일 저장 완료: " + targetPath);

            // 5. 업로드한 사용자 조회
            User uploader = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("❌ 사용자를 찾을 수 없습니다: " + userId));
            System.out.println("👤 업로드 사용자: " + uploader.getUsername());

            // 6. 파일 메타데이터 생성 및 저장
            File fileEntity = File.builder()
                    .originalName(file.getOriginalFilename())        // 원본 파일명
                    .storedName(storedFileName)                       // UUID 파일명
                    .filePath(relativePath)                           // 상대 경로
                    .fileSize(file.getSize())                         // 파일 크기 (bytes)
                    .contentType(file.getContentType())               // MIME 타입
                    .uploadedBy(uploader)                             // 업로더 정보
                    .build();

            File savedFile = fileRepository.save(fileEntity);
            System.out.println("💾 파일 메타데이터 저장 완료");
            System.out.println("   - File ID: " + savedFile.getId());
            System.out.println("   - 원본명: " + savedFile.getOriginalName());
            System.out.println("   - 저장명: " + savedFile.getStoredName());

            System.out.println("=================================");
            System.out.println("✅ 파일 업로드 완료");
            System.out.println("=================================\n");

            return savedFile;

        } catch (IOException e) {
            logger.error("❌ 파일 저장 중 오류 발생", e);
            System.err.println("❌ 파일 저장 실패: " + e.getMessage());
            throw new FileStorageException("파일 저장 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 업로드된 파일의 유효성을 검증합니다.
     *
     * 검증 항목:
     * 1. 파일이 NULL이 아닌지
     * 2. 파일이 비어있지 않은지
     * 3. 파일 크기가 제한을 초과하지 않는지
     * 4. 파일 확장자가 허용된 형식인지
     *
     * @param file MultipartFile - 검증할 파일
     * @throws IllegalArgumentException 검증 실패 시 예외 발생
     */
    private void validateFile(MultipartFile file) {
        // 1. NULL 체크
        if (file == null) {
            System.err.println("❌ 검증 실패: 파일이 전송되지 않았습니다");
            throw new IllegalArgumentException("파일이 전송되지 않았습니다.");
        }

        // 2. 빈 파일 체크
        if (file.isEmpty()) {
            System.err.println("❌ 검증 실패: 빈 파일입니다");
            throw new IllegalArgumentException("빈 파일은 업로드할 수 없습니다.");
        }

        // 3. 파일 크기 체크
        long maxSize = fileStorageProperties.getMaxSizeInBytes();
        if (file.getSize() > maxSize) {
            long maxSizeMB = maxSize / 1024 / 1024;
            System.err.println("❌ 검증 실패: 파일 크기 초과");
            System.err.println("   - 최대 허용: " + maxSizeMB + " MB");
            System.err.println("   - 현재 파일: " + String.format("%.2f", file.getSize() / 1024.0 / 1024.0) + " MB");
            throw new FileSizeExceededException(
                    String.format("파일 크기가 제한을 초과했습니다. (최대: %dMB, 현재: %.2fMB)",
                            maxSizeMB,
                            file.getSize() / 1024.0 / 1024.0)
            );
        }

        // 4. 파일 확장자 체크
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            System.err.println("❌ 검증 실패: 파일명이 없습니다");
            throw new IllegalArgumentException("파일명이 없습니다.");
        }

        String extension = getFileExtension(originalFilename);
        if (!fileStorageProperties.isAllowedExtension(extension)) {
            System.err.println("❌ 검증 실패: 허용되지 않은 파일 형식");
            System.err.println("   - 현재 확장자: " + extension);
            System.err.println("   - 허용 확장자: " + String.join(", ", fileStorageProperties.getAllowedExtensions()));
            throw new FileTypeNotAllowedException(
                    String.format("허용되지 않은 파일 형식입니다. (현재: %s, 허용: %s)",
                            extension,
                            String.join(", ", fileStorageProperties.getAllowedExtensions()))
            );
        }
    }

    /**
     * UUID를 사용하여 고유한 파일명을 생성합니다.
     *
     * 원본 파일명을 그대로 사용하면 발생할 수 있는 문제:
     * - 파일명 중복 (덮어쓰기 위험)
     * - 특수문자로 인한 오류
     * - 보안 위험 (경로 탐색 공격 등)
     *
     * UUID 사용의 장점:
     * - 전세계적으로 고유한 식별자 보장
     * - 특수문자 없음 (안전)
     * - 예측 불가능 (보안)
     *
     * 형식: UUID + 확장자
     * 예: "a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
     *
     * @param originalFilename String - 원본 파일명
     * @return String - UUID 기반의 새로운 파일명
     */
    private String generateFileName(String originalFilename) {
        // UUID 생성 (36자리 고유 문자열)
        String uuid = UUID.randomUUID().toString();

        // 원본 파일명에서 확장자 추출
        String extension = getFileExtension(originalFilename);

        // UUID + 확장자 조합
        String newFileName = uuid + "." + extension;

        logger.debug("파일명 생성: {} -> {}", originalFilename, newFileName);
        return newFileName;
    }

    /**
     * 연도/월별로 자동 분류된 파일 경로를 생성합니다.
     *
     * 파일을 날짜별로 분류하는 이유:
     * - 디렉토리 관리 편의성 (한 폴더에 수천 개의 파일 방지)
     * - 백업 및 관리 용이
     * - 성능 향상 (파일 시스템 검색 속도)
     *
     * 경로 형식: YYYY/MM/파일명
     * 예: "2025/11/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg"
     *
     * @param storedFileName String - UUID 기반 파일명
     * @return String - 연도/월이 포함된 상대 경로
     */
    private String buildFilePath(String storedFileName) {
        // 현재 날짜 가져오기
        LocalDate now = LocalDate.now();

        // 경로 생성: YYYY/MM/파일명
        String filePath = String.format("%d/%02d/%s",
                now.getYear(),              // 연도 (4자리)
                now.getMonthValue(),        // 월 (2자리, 앞에 0 패딩)
                storedFileName
        );

        logger.debug("파일 경로 생성: {}", filePath);
        return filePath;
    }

    /**
     * 파일명에서 확장자를 추출합니다.
     *
     * 예시:
     * - "document.pdf" → "pdf"
     * - "image.JPG" → "jpg" (소문자로 변환)
     * - "archive.tar.gz" → "gz" (마지막 확장자만)
     *
     * @param filename String - 파일명
     * @return String - 확장자 (소문자, 점 제외)
     */
    private String getFileExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return "";
        }

        // 마지막 점(.)의 위치 찾기
        int lastIndexOf = filename.lastIndexOf(".");

        if (lastIndexOf == -1) {
            // 확장자가 없는 경우
            return "";
        }

        // 확장자 추출 및 소문자로 변환
        return filename.substring(lastIndexOf + 1).toLowerCase();
    }

    /**
     * 삭제되지 않은 모든 파일의 목록을 페이징하여 조회합니다.
     *
     * @param pageable Pageable - 페이징 정보
     * @return Page<File> - 파일 목록 (페이징 정보 포함)
     */
    @Transactional(readOnly = true)
    public Page<File> getFiles(Pageable pageable) {
        logger.info("📄 파일 목록 조회: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());
        return fileRepository.findByIsDeletedFalse(pageable);
    }

    /**
     * 특정 사용자가 업로드한 파일 목록을 조회합니다.
     *
     * @param userId Long - 사용자 ID
     * @param pageable Pageable - 페이징 정보
     * @return Page<File> - 파일 목록
     */
    @Transactional(readOnly = true)
    public Page<File> getFilesByUser(Long userId, Pageable pageable) {
        logger.info("📄 사용자별 파일 목록 조회: userId={}", userId);

        // User 객체를 먼저 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        return fileRepository.findByUploadedByAndIsDeletedFalse(user, pageable);
    }

    /**
     * 파일 ID로 파일 정보를 조회합니다.
     *
     * @param id Long - 파일 ID
     * @return File - 파일 엔티티
     * @throws IllegalArgumentException 파일을 찾을 수 없거나 삭제된 경우
     */
    @Transactional(readOnly = true)
    public File getFileById(Long id) {
        logger.info("🔍 파일 상세 조회: id={}", id);

        File file = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("파일을 찾을 수 없습니다: " + id));

        // 삭제된 파일인지 확인
        if (file.getIsDeleted()) {
            throw new IllegalArgumentException("삭제된 파일입니다: " + id);
        }

        return file;
    }

    /**
     * 파일을 Soft Delete 방식으로 삭제합니다.
     *
     * Soft Delete:
     * - 실제 파일이나 데이터베이스 레코드를 삭제하지 않음
     * - isDeleted 플래그를 true로 설정
     * - 복구 가능
     * - 히스토리 유지
     *
     * @param id Long - 파일 ID
     * @throws IllegalArgumentException 파일을 찾을 수 없는 경우
     */
    public void deleteFile(Long id) {
        System.out.println("=================================");
        System.out.println("🗑️ 파일 삭제 시작");
        System.out.println("=================================");
        System.out.println("📋 삭제 요청 파일 ID: " + id);

        File file = fileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("❌ 파일을 찾을 수 없습니다: " + id));

        // 이미 삭제된 파일인지 확인
        if (file.getIsDeleted()) {
            logger.warn("⚠️ 이미 삭제된 파일입니다: id={}", id);
            System.out.println("⚠️ 이미 삭제된 파일입니다");
            return;
        }

        // Soft Delete 수행
        file.setIsDeleted(true);
        fileRepository.save(file);

        System.out.println("✅ 파일 삭제 완료");
        System.out.println("   - File ID: " + id);
        System.out.println("   - 원본명: " + file.getOriginalName());
        System.out.println("=================================\n");

        logger.info("🗑️ 파일 삭제 완료: id={}, originalName={}", id, file.getOriginalName());
    }

    /**
     * 전체 파일 통계 정보를 조회합니다.
     *
     * @return Map<String, Object> - 통계 정보
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getFileStatistics() {
        logger.info("📊 파일 통계 조회");

        long totalCount = fileRepository.countByIsDeletedFalse();
        Long totalSize = fileRepository.sumFileSizeByIsDeletedFalse();

        // totalSize가 null일 경우 0으로 처리
        if (totalSize == null) {
            totalSize = 0L;
        }

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalCount", totalCount);
        statistics.put("totalSize", totalSize);
        statistics.put("totalSizeMB", totalSize / 1024.0 / 1024.0);

        logger.info("📊 파일 통계: totalCount={}, totalSize={}bytes", totalCount, totalSize);

        return statistics;
    }
}