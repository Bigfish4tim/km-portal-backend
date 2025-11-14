package com.kmportal.backend.controller;

import com.kmportal.backend.entity.File;
import com.kmportal.backend.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

/**
 * 파일 관리 컨트롤러
 *
 * 파일 업로드, 다운로드, 조회, 삭제 API를 제공합니다.
 *
 * API 엔드포인트:
 * - POST   /api/files              : 파일 업로드
 * - GET    /api/files              : 파일 목록 조회 (페이징)
 * - GET    /api/files/search       : 파일 검색 (21일차 추가) ✨
 * - GET    /api/files/my           : 내가 업로드한 파일 목록
 * - GET    /api/files/statistics   : 파일 통계 정보
 * - GET    /api/files/{id}         : 파일 상세 조회
 * - GET    /api/files/{id}/download: 파일 다운로드
 * - DELETE /api/files/{id}         : 파일 삭제
 *
 * @author KM Portal Team
 * @version 1.1
 * @since 2025-11-13 (19일차)
 * 수정일: 2025-11-14 (21일차) - 파일 검색 API 추가
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private static final Logger logger = LoggerFactory.getLogger(FileController.class);

    @Autowired
    private FileService fileService;

    /**
     * 파일 업로드 API
     *
     * HTTP Method: POST
     * URL: /api/files
     * Content-Type: multipart/form-data
     *
     * 요청 파라미터:
     * - file: 업로드할 파일 (필수)
     * - description: 파일 설명 (선택)
     *
     * 응답:
     * - 200 OK: 업로드 성공, File 엔티티 반환
     * - 400 Bad Request: 파일 검증 실패 (크기, 확장자)
     * - 401 Unauthorized: 인증되지 않은 사용자
     * - 500 Internal Server Error: 서버 오류
     *
     * Postman 테스트 방법:
     * 1. Method: POST
     * 2. URL: http://localhost:8080/api/files
     * 3. Headers: Authorization: Bearer {access_token}
     * 4. Body > form-data:
     *    - Key: file, Type: File, Value: (파일 선택)
     *    - Key: description, Type: Text, Value: "테스트 파일"
     *
     * @param file MultipartFile - 업로드할 파일
     * @param description String - 파일 설명 (선택)
     * @return ResponseEntity<File> - 업로드된 파일 정보
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<File> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "description", required = false) String description) {

        System.out.println("=================================");
        System.out.println("📤 파일 업로드 API 호출");
        System.out.println("=================================");
        System.out.println("📋 요청 정보:");
        System.out.println("   - 파일명: " + file.getOriginalFilename());
        System.out.println("   - 파일 크기: " + file.getSize() + " bytes");
        System.out.println("   - 설명: " + (description != null ? description : "없음"));

        try {
            // 1. 현재 로그인한 사용자 ID 가져오기
            Long currentUserId = getCurrentUserId();
            System.out.println("👤 업로드 사용자 ID: " + currentUserId);

            // 2. 파일 업로드 처리
            File uploadedFile = fileService.uploadFile(file, currentUserId);

            // 3. description이 있으면 저장 (추후 File 엔티티에 description 필드 추가 예정)
            // uploadedFile.setDescription(description);

            System.out.println("✅ 파일 업로드 API 성공");
            System.out.println("   - File ID: " + uploadedFile.getId());
            System.out.println("   - 원본명: " + uploadedFile.getOriginalName());
            System.out.println("=================================\n");

            logger.info("✅ 파일 업로드 성공: ID={}, 원본명={}",
                    uploadedFile.getId(), uploadedFile.getOriginalName());

            return ResponseEntity.ok(uploadedFile);

        } catch (IllegalArgumentException e) {
            // 파일 검증 실패 (크기, 확장자 등)
            logger.warn("⚠️ 파일 업로드 실패 (검증 오류): {}", e.getMessage());
            System.err.println("❌ 파일 검증 실패: " + e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            // 서버 오류
            logger.error("❌ 파일 업로드 중 오류 발생", e);
            System.err.println("❌ 파일 업로드 중 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 전체 파일 목록 조회 API (페이징)
     *
     * HTTP Method: GET
     * URL: /api/files
     *
     * 쿼리 파라미터:
     * - page: 페이지 번호 (0부터 시작, 기본값: 0)
     * - size: 페이지당 항목 수 (기본값: 10)
     * - sort: 정렬 기준 (기본값: createdAt,desc)
     *
     * 예시:
     * GET /api/files?page=0&size=10&sort=createdAt,desc
     *
     * 응답:
     * - 200 OK: Page<File> - 페이징된 파일 목록
     * - 401 Unauthorized: 인증되지 않은 사용자
     *
     * @param pageable Pageable - 페이징 정보 (자동 바인딩)
     * @return ResponseEntity<Page<File>> - 파일 목록
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<File>> getFiles(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        logger.info("📄 파일 목록 조회 API 호출: page={}, size={}",
                pageable.getPageNumber(), pageable.getPageSize());

        Page<File> files = fileService.getFiles(pageable);

        logger.info("✅ 파일 목록 조회 완료: 총 {}건, 현재 페이지 {}건",
                files.getTotalElements(), files.getNumberOfElements());

        return ResponseEntity.ok(files);
    }

    /**
     * ✨ 21일차 추가: 파일 검색 API
     *
     * HTTP Method: GET
     * URL: /api/files/search
     *
     * 쿼리 파라미터:
     * - keyword: 검색 키워드 (파일명, 설명에서 검색)
     * - category: 파일 카테고리 (DOCUMENT, IMAGE 등)
     * - userId: 특정 사용자가 업로드한 파일만 검색
     * - startDate: 검색 시작 날짜 (YYYY-MM-DDTHH:mm:ss 형식)
     * - endDate: 검색 종료 날짜 (YYYY-MM-DDTHH:mm:ss 형식)
     * - page: 페이지 번호 (0부터 시작, 기본값: 0)
     * - size: 페이지당 항목 수 (기본값: 10)
     * - sort: 정렬 기준 (기본값: createdAt,desc)
     *
     * 예시:
     * GET /api/files/search?keyword=회의록&category=DOCUMENT&page=0&size=10
     * GET /api/files/search?startDate=2025-11-01T00:00:00&endDate=2025-11-14T23:59:59
     * GET /api/files/search?userId=1&keyword=보고서
     *
     * 응답:
     * - 200 OK: Page<File> - 검색된 파일 목록
     * - 401 Unauthorized: 인증되지 않은 사용자
     *
     * @param keyword String - 검색 키워드 (선택)
     * @param category String - 파일 카테고리 (선택)
     * @param userId Long - 사용자 ID (선택)
     * @param startDate LocalDateTime - 검색 시작 날짜 (선택)
     * @param endDate LocalDateTime - 검색 종료 날짜 (선택)
     * @param pageable Pageable - 페이징 정보
     * @return ResponseEntity<Page<File>> - 검색된 파일 목록
     *
     * @since 2025-11-14 (21일차)
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<File>> searchFiles(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "startDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(value = "endDate", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        System.out.println("=================================");
        System.out.println("🔍 파일 검색 API 호출");
        System.out.println("=================================");
        System.out.println("📋 검색 조건:");
        System.out.println("   - 키워드: " + (keyword != null ? keyword : "없음"));
        System.out.println("   - 카테고리: " + (category != null ? category : "없음"));
        System.out.println("   - 사용자 ID: " + (userId != null ? userId : "없음"));
        System.out.println("   - 시작 날짜: " + (startDate != null ? startDate : "없음"));
        System.out.println("   - 종료 날짜: " + (endDate != null ? endDate : "없음"));
        System.out.println("   - 페이지: " + pageable.getPageNumber());
        System.out.println("   - 크기: " + pageable.getPageSize());

        logger.info("🔍 파일 검색 API: keyword={}, category={}, userId={}, startDate={}, endDate={}",
                keyword, category, userId, startDate, endDate);

        // 서비스 메서드 호출
        Page<File> files = fileService.searchFiles(
                keyword,
                category,
                userId,
                startDate,
                endDate,
                pageable
        );

        System.out.println("✅ 파일 검색 API 완료");
        System.out.println("   - 검색 결과: " + files.getTotalElements() + "건");
        System.out.println("=================================\n");

        logger.info("✅ 파일 검색 완료: 총 {}건", files.getTotalElements());

        return ResponseEntity.ok(files);
    }

    /**
     * 현재 로그인한 사용자가 업로드한 파일 목록 조회
     *
     * HTTP Method: GET
     * URL: /api/files/my
     *
     * @param pageable Pageable - 페이징 정보
     * @return ResponseEntity<Page<File>> - 내 파일 목록
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<File>> getMyFiles(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long currentUserId = getCurrentUserId();
        logger.info("📄 내 파일 목록 조회 API 호출: userId={}", currentUserId);

        Page<File> files = fileService.getFilesByUser(currentUserId, pageable);

        logger.info("✅ 내 파일 목록 조회 완료: 총 {}건", files.getTotalElements());

        return ResponseEntity.ok(files);
    }

    /**
     * 파일 통계 정보 조회 API
     *
     * HTTP Method: GET
     * URL: /api/files/statistics
     *
     * 응답 예시:
     * {
     *   "totalCount": 150,
     *   "totalSize": 1048576000,
     *   "totalSizeMB": 1000.0
     * }
     *
     * @return ResponseEntity<Map<String, Object>> - 통계 정보
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<Map<String, Object>> getFileStatistics() {
        logger.info("📊 파일 통계 조회 API 호출");

        Map<String, Object> statistics = fileService.getFileStatistics();

        logger.info("✅ 파일 통계 조회 완료: {}", statistics);

        return ResponseEntity.ok(statistics);
    }

    /**
     * 파일 상세 정보 조회 API
     *
     * HTTP Method: GET
     * URL: /api/files/{id}
     *
     * 경로 변수:
     * - id: 파일 ID
     *
     * 응답:
     * - 200 OK: File - 파일 상세 정보
     * - 404 Not Found: 파일을 찾을 수 없음
     *
     * @param id Long - 파일 ID
     * @return ResponseEntity<File> - 파일 정보
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<File> getFile(@PathVariable Long id) {
        logger.info("🔍 파일 상세 조회 API 호출: id={}", id);

        try {
            File file = fileService.getFileById(id);
            logger.info("✅ 파일 상세 조회 완료: {}", file.getOriginalName());
            return ResponseEntity.ok(file);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ 파일을 찾을 수 없음: id={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 파일 다운로드 API
     *
     * HTTP Method: GET
     * URL: /api/files/{id}/download
     *
     * 응답:
     * - 200 OK: 파일 데이터 (바이너리)
     * - 404 Not Found: 파일을 찾을 수 없음
     * - 500 Internal Server Error: 파일 읽기 오류
     *
     * 브라우저에서 테스트:
     * http://localhost:8080/api/files/123/download
     *
     * Postman 테스트:
     * 1. Method: GET
     * 2. URL: http://localhost:8080/api/files/{id}/download
     * 3. Headers: Authorization: Bearer {access_token}
     * 4. Send 클릭 후 "Save Response" > "Save to a file"로 저장
     *
     * @param id Long - 파일 ID
     * @return ResponseEntity<Resource> - 파일 데이터
     */
    @GetMapping("/{id}/download")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        System.out.println("=================================");
        System.out.println("📥 파일 다운로드 API 호출");
        System.out.println("=================================");
        System.out.println("📋 요청 정보:");
        System.out.println("   - File ID: " + id);

        try {
            // 1. 파일 정보 조회
            File file = fileService.getFileById(id);
            System.out.println("✅ 파일 정보 조회 완료");
            System.out.println("   - 원본명: " + file.getOriginalName());
            System.out.println("   - 경로: " + file.getFilePath());

            // 2. 실제 파일 경로 생성
            Path filePath = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            // 3. 파일 존재 여부 확인
            if (!resource.exists() || !resource.isReadable()) {
                logger.error("❌ 파일을 읽을 수 없음: {}", filePath);
                System.err.println("❌ 파일을 읽을 수 없음: " + filePath);
                return ResponseEntity.notFound().build();
            }

            // 4. 다운로드 응답 헤더 설정
            String contentDisposition = "attachment; filename=\"" + file.getOriginalName() + "\"";

            System.out.println("✅ 파일 다운로드 시작");
            System.out.println("=================================\n");

            logger.info("📥 파일 다운로드 시작: {}", file.getOriginalName());

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ 파일을 찾을 수 없음: id={}", id);
            System.err.println("❌ 파일을 찾을 수 없음: ID=" + id);
            return ResponseEntity.notFound().build();

        } catch (MalformedURLException e) {
            logger.error("❌ 파일 경로 오류: id={}", id, e);
            System.err.println("❌ 파일 경로 오류: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 파일 삭제 API (Soft Delete)
     *
     * HTTP Method: DELETE
     * URL: /api/files/{id}
     *
     * 권한:
     * - 파일을 업로드한 사용자
     * - 또는 ADMIN, MANAGER 역할
     *
     * 응답:
     * - 200 OK: 삭제 성공
     * - 403 Forbidden: 권한 없음
     * - 404 Not Found: 파일을 찾을 수 없음
     *
     * @param id Long - 파일 ID
     * @return ResponseEntity<Map<String, String>> - 결과 메시지
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> deleteFile(@PathVariable Long id) {
        System.out.println("=================================");
        System.out.println("🗑️ 파일 삭제 API 호출");
        System.out.println("=================================");
        System.out.println("📋 요청 정보:");
        System.out.println("   - File ID: " + id);

        try {
            // 1. 파일 정보 조회
            File file = fileService.getFileById(id);

            // 2. 권한 확인: 업로드한 사용자 또는 관리자만 삭제 가능
            Long currentUserId = getCurrentUserId();
            boolean isOwner = file.getUploadedBy().getUserId().equals(currentUserId);
            boolean isAdmin = hasRole("ROLE_ADMIN") || hasRole("ROLE_MANAGER");

            System.out.println("🔐 권한 확인:");
            System.out.println("   - 현재 사용자 ID: " + currentUserId);
            System.out.println("   - 파일 소유자 ID: " + file.getUploadedBy().getUserId());
            System.out.println("   - 소유자 일치: " + isOwner);
            System.out.println("   - 관리자 권한: " + isAdmin);

            if (!isOwner && !isAdmin) {
                logger.warn("⚠️ 파일 삭제 권한 없음: userId={}, fileId={}", currentUserId, id);
                System.err.println("❌ 파일 삭제 권한 없음");
                System.out.println("=================================\n");

                Map<String, String> error = new HashMap<>();
                error.put("message", "파일을 삭제할 권한이 없습니다.");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
            }

            // 3. 파일 삭제
            fileService.deleteFile(id);

            System.out.println("✅ 파일 삭제 API 완료");
            System.out.println("   - File ID: " + id);
            System.out.println("   - 원본명: " + file.getOriginalName());
            System.out.println("=================================\n");

            logger.info("🗑️ 파일 삭제 완료: id={}, originalName={}", id, file.getOriginalName());

            Map<String, String> response = new HashMap<>();
            response.put("message", "파일이 삭제되었습니다.");
            response.put("fileName", file.getOriginalName());

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ 파일을 찾을 수 없음: id={}", id);
            System.err.println("❌ 파일을 찾을 수 없음: ID=" + id);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * ✨ 22일차 추가: 파일 대량 삭제 API
     * DELETE /api/files/batch
     */
    @DeleteMapping("/batch")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> deleteMultipleFiles(
            @RequestBody Map<String, Object> request) {

        System.out.println("🗑️ 파일 대량 삭제 API 호출");

        try {
            @SuppressWarnings("unchecked")
            List<Object> fileIdsObj = (List<Object>) request.get("fileIds");

            if (fileIdsObj == null || fileIdsObj.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "파일 ID 목록이 비어있습니다"));
            }

            List<Long> fileIds = new java.util.ArrayList<>();
            for (Object obj : fileIdsObj) {
                if (obj instanceof Number) {
                    fileIds.add(((Number) obj).longValue());
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();

            int deletedCount = fileService.deleteMultipleFiles(fileIds);

            System.out.println("✅ 파일 대량 삭제 완료: " + deletedCount + "개");

            Map<String, Object> response = new HashMap<>();
            response.put("message", "파일이 성공적으로 삭제되었습니다");
            response.put("deleted", deletedCount);
            response.put("requested", fileIds.size());

            logger.info("🗑️ 파일 대량 삭제: user={}, deleted={}", currentUsername, deletedCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("❌ 파일 대량 삭제 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "파일 삭제 중 오류 발생: " + e.getMessage()));
        }
    }

    /**
     * ✨ 22일차 추가: 파일 대량 다운로드 API (ZIP)
     * POST /api/files/download-multiple
     */
    @PostMapping("/download-multiple")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Resource> downloadMultipleFiles(
            @RequestBody Map<String, Object> request) {

        System.out.println("📦 파일 대량 다운로드 API 호출");

        try {
            @SuppressWarnings("unchecked")
            List<Object> fileIdsObj = (List<Object>) request.get("fileIds");

            if (fileIdsObj == null || fileIdsObj.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }

            List<Long> fileIds = new java.util.ArrayList<>();
            for (Object obj : fileIdsObj) {
                if (obj instanceof Number) {
                    fileIds.add(((Number) obj).longValue());
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();

            byte[] zipData = fileService.downloadMultipleFiles(fileIds);

            System.out.println("✅ ZIP 파일 생성 완료: " + (zipData.length / 1024) + "KB");

            org.springframework.core.io.ByteArrayResource resource =
                    new org.springframework.core.io.ByteArrayResource(zipData);

            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"files_" + System.currentTimeMillis() + ".zip\"");
            headers.add(HttpHeaders.CONTENT_TYPE, "application/zip");

            logger.info("📦 파일 대량 다운로드: user={}, count={}", currentUsername, fileIds.size());

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentLength(zipData.length)
                    .body(resource);

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ 파일 대량 다운로드 실패: {}", e.getMessage());
            return ResponseEntity.badRequest().build();

        } catch (Exception e) {
            logger.error("❌ 파일 대량 다운로드 실패", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 현재 로그인한 사용자의 ID를 가져옵니다.
     *
     * Spring Security의 SecurityContextHolder를 사용하여
     * 현재 인증된 사용자의 정보를 가져옵니다.
     *
     * @return Long - 현재 사용자 ID
     * @throws IllegalStateException 인증 정보가 없는 경우
     */
    private Long getCurrentUserId() {
        // SecurityContext에서 Authentication 객체 가져오기
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 인증되지 않은 경우 예외 발생
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증된 사용자가 아닙니다.");
        }

        // Principal에서 사용자 이름(username) 가져오기
        String username = authentication.getName();

        // username을 Long 타입으로 변환
        // 실제로는 username이 문자열이므로, UserDetails에서 ID를 가져오는 것이 더 적절합니다.
        // 여기서는 간단하게 username을 ID로 사용한다고 가정합니다.
        // TODO: UserDetails를 사용하여 실제 사용자 ID를 가져오도록 수정 필요
        try {
            return Long.parseLong(username);
        } catch (NumberFormatException e) {
            // username이 숫자가 아닌 경우, 사용자 조회 후 ID 반환
            // 여기서는 임시로 1을 반환 (실제 프로젝트에서는 UserService를 주입받아 조회)
            logger.warn("⚠️ username이 숫자가 아님: {}", username);
            return 1L; // 임시 값
        }
    }

    /**
     * 현재 사용자가 특정 역할(Role)을 가지고 있는지 확인합니다.
     *
     * @param role String - 확인할 역할 (예: "ROLE_ADMIN")
     * @return boolean - 역할 보유 여부
     */
    private boolean hasRole(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(role));
    }
}