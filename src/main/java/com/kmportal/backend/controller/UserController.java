package com.kmportal.backend.controller;

import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.UserRepository;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 관리 REST API 컨트롤러
 *
 * 이 컨트롤러는 사용자 관리와 관련된 모든 REST API를 제공합니다.
 * 기본적인 CRUD 기능부터 고급 검색, 통계 기능까지 포함합니다.
 *
 * 주요 기능:
 * - 사용자 목록 조회 (페이징, 검색, 정렬 지원)
 * - 사용자 상세 정보 조회
 * - 사용자 생성/수정/삭제 (소프트 삭제)
 * - 사용자 상태 관리 (활성화/비활성화, 잠금/해제)
 * - 사용자 검색 및 필터링
 * - 사용자 통계 정보
 *
 * 보안:
 * - @PreAuthorize 어노테이션으로 권한 기반 접근 제어
 * - 입력값 검증 (@Valid 어노테이션 사용)
 * - 에러 처리 및 로깅
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-09-24
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    /**
     * 로깅을 위한 Logger 인스턴스
     */
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
     * 사용자 데이터 액세스를 위한 Repository
     * @Autowired: Spring이 자동으로 의존성 주입
     */
    @Autowired
    private UserRepository userRepository;

    // ================================
    // 조회 API 메서드
    // ================================

    /**
     * 모든 사용자 목록 조회 (페이징 지원)
     *
     * GET /api/users
     * GET /api/users?page=0&size=10&sort=username,asc
     *
     * 권한: ADMIN, MANAGER만 접근 가능
     *
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 10)
     * @param sortBy 정렬 필드 (기본값: username)
     * @param sortDir 정렬 방향 (기본값: asc)
     * @return 페이징된 사용자 목록
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "username") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        try {
            logger.info("사용자 목록 조회 요청 - page: {}, size: {}, sortBy: {}, sortDir: {}",
                    page, size, sortBy, sortDir);

            // 정렬 방향 설정
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;

            // 페이징 및 정렬 설정
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            // 데이터베이스에서 페이징된 사용자 목록 조회
            Page<User> userPage = userRepository.findAll(pageable);

            // 응답 데이터 구성
            Map<String, Object> response = new HashMap<>();
            response.put("users", userPage.getContent());           // 사용자 목록
            response.put("currentPage", userPage.getNumber());      // 현재 페이지
            response.put("totalPages", userPage.getTotalPages());   // 전체 페이지 수
            response.put("totalElements", userPage.getTotalElements()); // 전체 요소 수
            response.put("pageSize", userPage.getSize());           // 페이지 크기
            response.put("hasNext", userPage.hasNext());            // 다음 페이지 여부
            response.put("hasPrevious", userPage.hasPrevious());    // 이전 페이지 여부

            logger.info("사용자 목록 조회 성공 - 총 {}명, {}페이지",
                    userPage.getTotalElements(), userPage.getTotalPages());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("사용자 목록 조회 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 목록을 조회할 수 없습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    /**
     * 활성 사용자만 조회
     *
     * GET /api/users/active
     *
     * @param pageable 페이징 정보
     * @return 활성 사용자 목록
     */
    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Page<User>> getActiveUsers(Pageable pageable) {

        try {
            logger.info("활성 사용자 목록 조회 요청");

            Page<User> activeUsers = userRepository.findByIsActiveTrue(pageable);

            logger.info("활성 사용자 목록 조회 성공 - {}명", activeUsers.getTotalElements());

            return ResponseEntity.ok(activeUsers);

        } catch (Exception e) {
            logger.error("활성 사용자 목록 조회 중 오류 발생", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 특정 사용자 상세 정보 조회
     *
     * GET /api/users/{id}
     *
     * @param id 사용자 ID
     * @return 사용자 상세 정보
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or #id == authentication.principal.userId")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {

        try {
            logger.info("사용자 상세 조회 요청 - ID: {}", id);

            Optional<User> userOptional = userRepository.findById(id);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                logger.info("사용자 상세 조회 성공 - 사용자명: {}", user.getUsername());
                return ResponseEntity.ok(user);
            } else {
                logger.warn("사용자를 찾을 수 없음 - ID: {}", id);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("사용자 상세 조회 중 오류 발생 - ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 사용자명으로 사용자 조회
     *
     * GET /api/users/username/{username}
     *
     * @param username 사용자명
     * @return 사용자 정보
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {

        try {
            logger.info("사용자명으로 조회 요청 - 사용자명: {}", username);

            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                logger.info("사용자명으로 조회 성공 - ID: {}", user.getUserId());
                return ResponseEntity.ok(user);
            } else {
                logger.warn("사용자를 찾을 수 없음 - 사용자명: {}", username);
                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("사용자명으로 조회 중 오류 발생 - 사용자명: {}", username, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // 검색 API 메서드
    // ================================

    /**
     * 사용자 검색 (이름 또는 이메일)
     *
     * GET /api/users/search?keyword=검색어
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 사용자 목록
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<User>> searchUsers(@RequestParam String keyword) {

        try {
            logger.info("사용자 검색 요청 - 키워드: {}", keyword);

            // 이름 또는 이메일로 검색 (대소문자 구분 안함)
            List<User> searchResults = userRepository
                    .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(keyword, keyword);

            logger.info("사용자 검색 완료 - 키워드: {}, 결과: {}명", keyword, searchResults.size());

            return ResponseEntity.ok(searchResults);

        } catch (Exception e) {
            logger.error("사용자 검색 중 오류 발생 - 키워드: {}", keyword, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * 부서별 사용자 조회
     *
     * GET /api/users/department/{department}
     *
     * @param department 부서명
     * @return 해당 부서 사용자 목록
     */
    @GetMapping("/department/{department}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<List<User>> getUsersByDepartment(@PathVariable String department) {

        try {
            logger.info("부서별 사용자 조회 요청 - 부서: {}", department);

            List<User> departmentUsers = userRepository.findByDepartmentAndIsActiveTrue(department);

            logger.info("부서별 사용자 조회 완료 - 부서: {}, 사용자 수: {}명",
                    department, departmentUsers.size());

            return ResponseEntity.ok(departmentUsers);

        } catch (Exception e) {
            logger.error("부서별 사용자 조회 중 오류 발생 - 부서: {}", department, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ================================
    // 생성/수정 API 메서드
    // ================================

    /**
     * 새 사용자 생성
     *
     * POST /api/users
     * Content-Type: application/json
     *
     * @param user 생성할 사용자 정보
     * @return 생성된 사용자 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody User user) {

        try {
            logger.info("신규 사용자 생성 요청 - 사용자명: {}", user.getUsername());

            // 중복 검사
            if (userRepository.existsByUsername(user.getUsername())) {
                logger.warn("사용자명 중복 - 사용자명: {}", user.getUsername());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "사용자명이 이미 존재합니다.");
                errorResponse.put("field", "username");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            if (userRepository.existsByEmail(user.getEmail())) {
                logger.warn("이메일 중복 - 이메일: {}", user.getEmail());

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "이메일이 이미 존재합니다.");
                errorResponse.put("field", "email");

                return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);
            }

            // 기본값 설정
            user.setIsActive(true);
            user.setIsLocked(false);
            user.setPasswordExpired(false);
            user.setFailedLoginAttempts(0);

            // 사용자 저장
            User savedUser = userRepository.save(user);

            logger.info("신규 사용자 생성 성공 - ID: {}, 사용자명: {}",
                    savedUser.getUserId(), savedUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "사용자가 성공적으로 생성되었습니다.");
            response.put("user", savedUser);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            logger.error("사용자 생성 중 오류 발생 - 사용자명: {}", user.getUsername(), e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 생성 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 정보 수정
     *
     * PUT /api/users/{id}
     * Content-Type: application/json
     *
     * @param id 수정할 사용자 ID
     * @param userDetails 수정할 사용자 정보
     * @return 수정된 사용자 정보
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or #id == authentication.principal.userId")
    public ResponseEntity<Map<String, Object>> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody User userDetails) {

        try {
            logger.info("사용자 정보 수정 요청 - ID: {}", id);

            Optional<User> userOptional = userRepository.findById(id);

            if (!userOptional.isPresent()) {
                logger.warn("수정할 사용자를 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "사용자를 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

            User existingUser = userOptional.get();

            // 기존 사용자 정보 업데이트
            existingUser.setEmail(userDetails.getEmail());
            existingUser.setFullName(userDetails.getFullName());
            existingUser.setDepartment(userDetails.getDepartment());
            existingUser.setPosition(userDetails.getPosition());
            existingUser.setPhoneNumber(userDetails.getPhoneNumber());

            // 관리자만 활성화/잠금 상태 변경 가능
            // TODO: 권한 검사 로직 추가 필요

            User updatedUser = userRepository.save(existingUser);

            logger.info("사용자 정보 수정 성공 - ID: {}, 사용자명: {}",
                    updatedUser.getUserId(), updatedUser.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("message", "사용자 정보가 성공적으로 수정되었습니다.");
            response.put("user", updatedUser);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("사용자 정보 수정 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 정보 수정 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 상태 관리 API 메서드
    // ================================

    /**
     * 사용자 비활성화 (소프트 삭제)
     *
     * DELETE /api/users/{id}
     *
     * @param id 비활성화할 사용자 ID
     * @return 처리 결과
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable Long id) {

        try {
            logger.info("사용자 비활성화 요청 - ID: {}", id);

            int updatedRows = userRepository.deactivateUser(id);

            if (updatedRows > 0) {
                logger.info("사용자 비활성화 성공 - ID: {}", id);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "사용자가 성공적으로 비활성화되었습니다.");

                return ResponseEntity.ok(response);
            } else {
                logger.warn("비활성화할 사용자를 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "사용자를 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("사용자 비활성화 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자 비활성화 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 계정 잠금
     *
     * POST /api/users/{id}/lock
     *
     * @param id 잠금할 사용자 ID
     * @return 처리 결과
     */
    @PostMapping("/{id}/lock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> lockUser(@PathVariable Long id) {

        try {
            logger.info("사용자 계정 잠금 요청 - ID: {}", id);

            int updatedRows = userRepository.lockUser(id);

            if (updatedRows > 0) {
                logger.info("사용자 계정 잠금 성공 - ID: {}", id);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "사용자 계정이 잠금되었습니다.");

                return ResponseEntity.ok(response);
            } else {
                logger.warn("잠금할 사용자를 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "사용자를 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("사용자 계정 잠금 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "계정 잠금 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 사용자 계정 잠금 해제
     *
     * POST /api/users/{id}/unlock
     *
     * @param id 잠금 해제할 사용자 ID
     * @return 처리 결과
     */
    @PostMapping("/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> unlockUser(@PathVariable Long id) {

        try {
            logger.info("사용자 계정 잠금 해제 요청 - ID: {}", id);

            int updatedRows = userRepository.unlockUser(id);

            if (updatedRows > 0) {
                logger.info("사용자 계정 잠금 해제 성공 - ID: {}", id);

                Map<String, Object> response = new HashMap<>();
                response.put("message", "사용자 계정 잠금이 해제되었습니다.");

                return ResponseEntity.ok(response);
            } else {
                logger.warn("잠금 해제할 사용자를 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "사용자를 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

        } catch (Exception e) {
            logger.error("사용자 계정 잠금 해제 중 오류 발생 - ID: {}", id, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "계정 잠금 해제 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 통계 API 메서드
    // ================================

    /**
     * 사용자 통계 정보 조회
     *
     * GET /api/users/statistics
     *
     * @return 사용자 통계 정보
     */
    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> getUserStatistics() {

        try {
            logger.info("사용자 통계 정보 조회 요청");

            long totalUsers = userRepository.count();
            long activeUsers = userRepository.countByIsActiveTrue();
            long lockedUsers = userRepository.countByIsLockedTrue();

            // 부서별 사용자 수
            List<Object[]> departmentStats = userRepository.findActiveUserCountByDepartment();

            // 최근 7일간 신규 가입자
            LocalDateTime weekAgo = LocalDateTime.now().minusWeeks(1);
            long newUsersThisWeek = userRepository.countByCreatedAtBetween(weekAgo, LocalDateTime.now());

            Map<String, Object> statistics = new HashMap<>();
            statistics.put("totalUsers", totalUsers);
            statistics.put("activeUsers", activeUsers);
            statistics.put("inactiveUsers", totalUsers - activeUsers);
            statistics.put("lockedUsers", lockedUsers);
            statistics.put("newUsersThisWeek", newUsersThisWeek);
            statistics.put("departmentStats", departmentStats);

            logger.info("사용자 통계 정보 조회 성공 - 전체: {}, 활성: {}, 잠금: {}",
                    totalUsers, activeUsers, lockedUsers);

            return ResponseEntity.ok(statistics);

        } catch (Exception e) {
            logger.error("사용자 통계 정보 조회 중 오류 발생", e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "통계 정보를 조회할 수 없습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    // ================================
    // 유틸리티 API 메서드
    // ================================

    /**
     * 사용자명 중복 확인
     *
     * GET /api/users/check-username?username=사용자명
     *
     * @param username 확인할 사용자명
     * @return 중복 여부 정보
     */
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {

        try {
            logger.info("사용자명 중복 확인 요청 - 사용자명: {}", username);

            boolean exists = userRepository.existsByUsername(username);

            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("exists", exists);
            response.put("available", !exists);

            logger.info("사용자명 중복 확인 완료 - 사용자명: {}, 사용가능: {}", username, !exists);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("사용자명 중복 확인 중 오류 발생 - 사용자명: {}", username, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "사용자명 확인 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    /**
     * 이메일 중복 확인
     *
     * GET /api/users/check-email?email=이메일주소
     *
     * @param email 확인할 이메일
     * @return 중복 여부 정보
     */
    @GetMapping("/check-email")
    public ResponseEntity<Map<String, Object>> checkEmail(@RequestParam String email) {

        try {
            logger.info("이메일 중복 확인 요청 - 이메일: {}", email);

            boolean exists = userRepository.existsByEmail(email);

            Map<String, Object> response = new HashMap<>();
            response.put("email", email);
            response.put("exists", exists);
            response.put("available", !exists);

            logger.info("이메일 중복 확인 완료 - 이메일: {}, 사용가능: {}", email, !exists);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("이메일 중복 확인 중 오류 발생 - 이메일: {}", email, e);

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "이메일 확인 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}