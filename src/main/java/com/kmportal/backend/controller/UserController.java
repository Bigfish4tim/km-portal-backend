package com.kmportal.backend.controller;

import com.kmportal.backend.entity.Role;  // 🔥 신규 추가: 역할(Role) 엔티티 임포트
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.RoleRepository;  // 🔥 신규 추가: 역할 Repository 임포트
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
import org.springframework.security.crypto.password.PasswordEncoder;  // 🔥 신규 추가: 비밀번호 암호화 임포트
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자 관리 REST API 컨트롤러 (16일차 개선 버전)
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
 * - 🔥 [신규] 사용자 권한(역할) 변경
 * - 🔥 [개선] 비밀번호 암호화 저장
 *
 * 보안:
 * - @PreAuthorize 어노테이션으로 권한 기반 접근 제어
 * - 입력값 검증 (@Valid 어노테이션 사용)
 * - 에러 처리 및 로깅
 *
 * 16일차 개선 사항:
 * 1. RoleRepository 의존성 추가 - 역할 관리 기능 지원
 * 2. PasswordEncoder 의존성 추가 - 비밀번호 안전 저장
 * 3. updateUserRoles() 메서드 추가 - 사용자 권한 변경 API
 * 4. createUser() 메서드 개선 - 비밀번호 암호화 적용
 *
 * @author KM Portal Dev Team
 * @version 2.0 (16일차 개선)
 * @since 2025-11-11
 */
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", maxAge = 3600)
public class UserController {

    /**
     * 로깅을 위한 Logger 인스턴스
     *
     * Logger는 애플리케이션의 실행 상황을 기록하는 도구입니다.
     * 디버깅, 모니터링, 문제 해결에 필수적입니다.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    /**
     * 사용자 데이터 액세스를 위한 Repository
     *
     * @Autowired: Spring이 자동으로 UserRepository 인스턴스를 주입합니다.
     * 이를 "의존성 주입(Dependency Injection)"이라고 합니다.
     */
    @Autowired
    private UserRepository userRepository;

    // ================================
    // 🔥 16일차 신규 추가 시작
    // ================================

    /**
     * 역할(Role) 데이터 액세스를 위한 Repository
     *
     * [추가 목적]
     * 사용자에게 역할을 할당하거나 변경할 때 필요합니다.
     * 예: 일반 사용자를 관리자로 승격
     *
     * [사용 위치]
     * - updateUserRoles() 메서드: 사용자 권한 변경 시
     *
     * @Autowired: Spring이 자동으로 RoleRepository 인스턴스를 주입
     */
    @Autowired
    private RoleRepository roleRepository;

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder
     *
     * [추가 목적]
     * 사용자 비밀번호를 평문이 아닌 암호화된 형태로 저장하기 위함입니다.
     * 이는 보안의 가장 기본이자 필수 요소입니다.
     *
     * [암호화 방식]
     * BCrypt 알고리즘을 사용합니다. (SecurityConfig에서 설정)
     * - 단방향 암호화: 암호화된 값을 원래 값으로 되돌릴 수 없음
     * - Salt 자동 생성: 같은 비밀번호도 매번 다른 암호화 결과 생성
     * - 강도 조절 가능: rounds 값으로 보안 강도 조절 (기본 12)
     *
     * [예시]
     * 원본 비밀번호: "admin123"
     * 암호화 결과: "$2a$12$abcdefghijklmnop..." (약 60자)
     *
     * [사용 위치]
     * - createUser() 메서드: 신규 사용자 생성 시 비밀번호 암호화
     * - (향후) updatePassword() 메서드: 비밀번호 변경 시
     *
     * @Autowired: Spring이 자동으로 PasswordEncoder 인스턴스를 주입
     */
    @Autowired
    private PasswordEncoder passwordEncoder;

    // ================================
    // 🔥 16일차 신규 추가 끝
    // ================================

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
     * 새 사용자 생성 (16일차 개선 버전)
     *
     * POST /api/users
     * Content-Type: application/json
     *
     * 🔥 16일차 개선 사항:
     * - 비밀번호 암호화 로직 추가 (BCrypt 알고리즘 사용)
     * - 원본 비밀번호는 데이터베이스에 저장되지 않음
     * - 암호화된 비밀번호만 저장되어 보안 강화
     *
     * [요청 예시]
     * {
     *   "username": "newuser",
     *   "password": "password123",  ← 평문 비밀번호 (암호화되어 저장됨)
     *   "email": "user@example.com",
     *   "fullName": "홍길동",
     *   "department": "개발팀",
     *   "position": "개발자"
     * }
     *
     * [저장되는 데이터]
     * {
     *   "username": "newuser",
     *   "password": "$2a$12$XYZ...",  ← 암호화된 비밀번호
     *   "email": "user@example.com",
     *   ...
     * }
     *
     * @param user 생성할 사용자 정보
     * @return 생성된 사용자 정보
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> createUser(@Valid @RequestBody User user) {

        try {
            logger.info("신규 사용자 생성 요청 - 사용자명: {}", user.getUsername());

            // ===== 1단계: 중복 검사 =====
            // 이미 존재하는 사용자명이나 이메일인지 확인합니다.
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

            // ================================
            // 🔥 16일차 신규 추가: 비밀번호 암호화
            // ================================

            /**
             * [비밀번호 암호화 과정 상세 설명]
             *
             * 1. 원본 비밀번호 추출
             *    - user.getPassword()로 평문 비밀번호를 가져옵니다
             *    - 예: "admin123"
             *
             * 2. BCrypt 암호화 수행
             *    - passwordEncoder.encode() 메서드 호출
             *    - BCrypt 알고리즘이 자동으로:
             *      a) Salt 생성 (랜덤 값)
             *      b) 비밀번호 + Salt를 조합하여 해시 생성
             *      c) 결과를 Base64로 인코딩
             *    - 예: "$2a$12$abcdefghijklmnop..."
             *
             * 3. 암호화된 비밀번호로 교체
             *    - user.setPassword()로 원본을 암호화된 값으로 대체
             *    - 이제 user 객체는 암호화된 비밀번호를 가지고 있습니다
             *
             * 4. 데이터베이스 저장
             *    - 암호화된 비밀번호가 저장됩니다
             *    - 원본 비밀번호는 어디에도 저장되지 않습니다
             *
             * [보안 장점]
             * - 데이터베이스가 유출되어도 원본 비밀번호는 알 수 없음
             * - 같은 비밀번호도 매번 다른 암호화 결과 생성 (Salt 덕분)
             * - 관리자도 사용자의 실제 비밀번호를 알 수 없음
             *
             * [로그인 시 검증 방법]
             * - 로그인 시: passwordEncoder.matches(입력비밀번호, 저장된암호화비밀번호)
             * - BCrypt가 자동으로 Salt를 추출하여 비교
             * - 일치하면 true, 불일치하면 false 반환
             */

            logger.info("🔐 비밀번호 암호화 시작...");

            // 원본 비밀번호 (로깅용 - 실제로는 로그에 비밀번호를 남기면 안 됩니다)
            // 아래 로그는 개발 단계에서만 사용하고, 프로덕션에서는 제거해야 합니다.
            String rawPassword = user.getPassword();
            logger.debug("원본 비밀번호 길이: {}", rawPassword.length());

            // BCrypt 암호화 수행
            String encodedPassword = passwordEncoder.encode(user.getPassword());

            // 암호화된 비밀번호로 교체
            user.setPassword(encodedPassword);

            logger.info("✅ 비밀번호 암호화 완료 (암호화 길이: {}자)", encodedPassword.length());

            // ================================
            // 🔥 비밀번호 암호화 끝
            // ================================

            // ===== 2단계: 기본값 설정 =====
            // 새로 생성되는 사용자의 초기 상태를 설정합니다.
            user.setIsActive(true);              // 계정 활성화
            user.setIsLocked(false);             // 계정 잠금 해제
            user.setPasswordExpired(false);      // 비밀번호 만료 안됨
            user.setFailedLoginAttempts(0);      // 로그인 실패 횟수 0으로 초기화

            // ===== 3단계: 사용자 저장 =====
            // 데이터베이스에 사용자 정보를 저장합니다.
            User savedUser = userRepository.save(user);

            logger.info("신규 사용자 생성 성공 - ID: {}, 사용자명: {}",
                    savedUser.getUserId(), savedUser.getUsername());

            // ===== 4단계: 성공 응답 반환 =====
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

    // ================================
    // 🔥 16일차 신규 추가: 권한 관리 API
    // ================================

    /**
     * 사용자 권한(역할) 변경 (16일차 신규 추가)
     *
     * PUT /api/users/{id}/roles
     * Content-Type: application/json
     *
     * [기능 설명]
     * 특정 사용자에게 할당된 역할(권한)을 변경합니다.
     * 기존 역할은 모두 제거되고, 새로운 역할로 대체됩니다.
     *
     * [사용 시나리오]
     * 1. 관리자가 사용자 관리 페이지에서 "권한 변경" 버튼 클릭
     * 2. 역할 선택 다이얼로그가 표시됨
     * 3. 원하는 역할들을 다중 선택 (예: ADMIN, USER)
     * 4. "저장" 버튼 클릭
     * 5. 이 API가 호출되어 사용자 권한 변경
     *
     * [권한]
     * - ADMIN 또는 MANAGER만 접근 가능
     *
     * [요청 형식]
     * {
     *   "roleIds": [1, 2, 3]  // 할당할 역할 ID 목록
     * }
     *
     * [응답 형식 - 성공]
     * {
     *   "message": "역할이 성공적으로 변경되었습니다.",
     *   "user": {
     *     "userId": 5,
     *     "username": "user01",
     *     "roles": [
     *       {
     *         "roleId": 1,
     *         "roleName": "ROLE_ADMIN",
     *         "displayName": "시스템 관리자"
     *       },
     *       {
     *         "roleId": 2,
     *         "roleName": "ROLE_USER",
     *         "displayName": "일반 사용자"
     *       }
     *     ],
     *     ...
     *   },
     *   "assignedCount": 2,
     *   "notFoundCount": 0
     * }
     *
     * [응답 형식 - 실패]
     * {
     *   "error": "사용자를 찾을 수 없습니다."
     * }
     *
     * [주의사항]
     * 1. 기존 역할은 모두 제거되고 새로운 역할로 대체됩니다
     * 2. 최소 1개 이상의 역할을 할당해야 합니다
     * 3. 존재하지 않는 역할 ID는 무시됩니다
     * 4. 역할 ID 목록이 비어있으면 에러가 발생합니다
     *
     * [에러 케이스]
     * - 사용자를 찾을 수 없음: 404 Not Found
     * - 역할 목록이 비어있음: 400 Bad Request
     * - 유효한 역할이 없음: 400 Bad Request
     * - 서버 오류: 500 Internal Server Error
     *
     * [프론트엔드 연동 예시 (Vue.js)]
     * ```javascript
     * async updateUserRoles(userId, roleIds) {
     *   try {
     *     const response = await axios.put(
     *       `http://localhost:8080/api/users/${userId}/roles`,
     *       { roleIds: roleIds },
     *       { headers: { Authorization: `Bearer ${token}` } }
     *     );
     *     console.log('권한 변경 성공:', response.data.message);
     *   } catch (error) {
     *     console.error('권한 변경 실패:', error.response.data.error);
     *   }
     * }
     * ```
     *
     * @param id 권한을 변경할 사용자 ID
     * @param request 새로 할당할 역할 ID 목록을 담은 요청 객체
     * @return 변경된 사용자 정보와 처리 결과
     */
    @PutMapping("/{id}/roles")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<Map<String, Object>> updateUserRoles(
            @PathVariable Long id,
            @RequestBody Map<String, List<Long>> request) {

        try {
            logger.info("🔄 사용자 역할 변경 요청 시작 - 사용자 ID: {}", id);

            // ===== 1단계: 사용자 조회 =====
            // 역할을 변경할 사용자가 존재하는지 확인합니다.
            Optional<User> userOptional = userRepository.findById(id);

            if (!userOptional.isPresent()) {
                logger.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", id);

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "사용자를 찾을 수 없습니다.");

                return ResponseEntity.notFound().build();
            }

            User user = userOptional.get();
            logger.info("✅ 사용자 조회 성공");
            logger.info("   - 사용자명: {}", user.getUsername());
            logger.info("   - 이메일: {}", user.getEmail());
            logger.info("   - 현재 역할 수: {}", user.getRoles().size());

            // ===== 2단계: 요청에서 역할 ID 목록 추출 =====
            // 클라이언트가 보낸 역할 ID 목록을 추출합니다.
            List<Long> roleIds = request.get("roleIds");

            if (roleIds == null || roleIds.isEmpty()) {
                logger.warn("⚠️ 역할 ID 목록이 비어있음");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "최소 1개 이상의 역할을 선택해야 합니다.");

                return ResponseEntity.badRequest().body(errorResponse);
            }

            logger.info("📋 변경할 역할 ID 목록: {}", roleIds);
            logger.info("📋 역할 개수: {}", roleIds.size());

            // ===== 3단계: 기존 역할 모두 제거 =====
            /**
             * [역할 제거 방법 설명]
             *
             * user.getRoles().clear()를 호출하면:
             * 1. User 객체의 roles 컬렉션이 비워집니다
             * 2. JPA가 이를 감지하여 중간 테이블(user_roles)의 관련 레코드를 삭제합니다
             * 3. 실제 Role 엔티티는 삭제되지 않습니다 (참조만 제거됨)
             *
             * [왜 clear()를 사용하나?]
             * - 기존 역할을 하나씩 제거하는 것보다 효율적
             * - 코드가 간결하고 명확
             * - JPA가 최적화된 DELETE 쿼리를 생성
             */
            logger.info("🗑️ 기존 역할 제거 중...");
            int oldRoleCount = user.getRoles().size();

            // 기존 역할 모두 제거
            user.getRoles().clear();

            logger.info("✅ 기존 역할 {}개 제거 완료", oldRoleCount);

            // ===== 4단계: 새 역할 할당 =====
            /**
             * [역할 할당 과정]
             *
             * 1. 각 역할 ID에 대해 반복
             * 2. roleRepository에서 역할 조회
             * 3. 역할이 존재하면:
             *    - user.addRole()로 역할 추가
             *    - 양방향 관계 설정 (User ↔ Role)
             *    - 할당 성공 카운트 증가
             * 4. 역할이 없으면:
             *    - 경고 로그 출력
             *    - 실패 카운트 증가
             *    - 다음 역할로 계속 진행
             */
            logger.info("➕ 새 역할 할당 중...");
            int assignedCount = 0;     // 성공적으로 할당된 역할 수
            int notFoundCount = 0;     // 찾을 수 없는 역할 ID 수

            for (Long roleId : roleIds) {
                logger.info("   처리 중: 역할 ID {}", roleId);

                // 역할 조회
                Optional<Role> roleOptional = roleRepository.findById(roleId);

                if (roleOptional.isPresent()) {
                    // 역할이 존재하는 경우
                    Role role = roleOptional.get();

                    // User 엔티티의 헬퍼 메서드를 사용하여 역할 추가
                    // 이 메서드는 양방향 관계를 자동으로 설정합니다
                    user.addRole(role);

                    assignedCount++;
                    logger.info("     ✅ 역할 추가 성공: {} ({})",
                            role.getDisplayName(), role.getRoleName());
                } else {
                    // 역할이 존재하지 않는 경우
                    notFoundCount++;
                    logger.warn("     ⚠️ 역할을 찾을 수 없음 - ID: {}", roleId);
                }
            }

            // 할당 결과 요약 로그
            logger.info("📊 역할 할당 결과:");
            logger.info("   - 성공: {}개", assignedCount);
            logger.info("   - 실패: {}개", notFoundCount);

            // 유효한 역할이 하나도 없는 경우 에러 반환
            if (assignedCount == 0) {
                logger.warn("⚠️ 할당된 역할이 없음 - 모든 역할 ID가 유효하지 않음");

                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "유효한 역할을 찾을 수 없습니다.");
                errorResponse.put("notFoundCount", notFoundCount);

                return ResponseEntity.badRequest().body(errorResponse);
            }

            // ===== 5단계: 데이터베이스에 저장 =====
            /**
             * [저장 과정]
             *
             * userRepository.save(user)를 호출하면:
             * 1. JPA가 User 엔티티의 변경사항을 감지
             * 2. 중간 테이블(user_roles)의 변경사항 계산:
             *    - 제거된 역할: DELETE 쿼리 실행
             *    - 추가된 역할: INSERT 쿼리 실행
             * 3. 트랜잭션 커밋 시 모든 변경사항이 데이터베이스에 반영
             *
             * [트랜잭션 보장]
             * - @Transactional 어노테이션이 없어도 JPA가 자동으로 트랜잭션 처리
             * - 중간에 오류 발생 시 모든 변경사항 롤백
             * - All or Nothing: 모두 성공하거나 모두 실패
             */
            logger.info("💾 데이터베이스 저장 중...");
            User updatedUser = userRepository.save(user);

            logger.info("🎉 사용자 역할 변경 완료!");
            logger.info("   - 사용자 ID: {}", id);
            logger.info("   - 사용자명: {}", updatedUser.getUsername());
            logger.info("   - 새 역할 수: {}", updatedUser.getRoles().size());
            logger.info("   - 할당 성공: {}개", assignedCount);
            logger.info("   - 할당 실패: {}개", notFoundCount);

            // ===== 6단계: 성공 응답 반환 =====
            /**
             * [응답 구조]
             *
             * {
             *   "message": "성공 메시지",
             *   "user": {
             *     // 변경된 사용자 정보 (역할 포함)
             *   },
             *   "assignedCount": 2,    // 성공한 역할 수
             *   "notFoundCount": 0     // 실패한 역할 수
             * }
             *
             * [프론트엔드 활용]
             * - message: 사용자에게 표시할 성공 메시지
             * - user: 업데이트된 사용자 정보로 UI 갱신
             * - assignedCount/notFoundCount: 상세 결과 표시
             */
            Map<String, Object> response = new HashMap<>();
            response.put("message", "역할이 성공적으로 변경되었습니다.");
            response.put("user", updatedUser);
            response.put("assignedCount", assignedCount);
            response.put("notFoundCount", notFoundCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            /**
             * [에러 처리]
             *
             * 예상 가능한 에러:
             * 1. 데이터베이스 연결 오류
             * 2. 제약 조건 위반 (예: 역할 ID가 너무 큼)
             * 3. 메모리 부족
             * 4. 트랜잭션 타임아웃
             *
             * 모든 에러는 로그에 기록되고, 클라이언트에게는
             * 일반적인 에러 메시지를 반환합니다.
             */
            logger.error("❌ 사용자 역할 변경 중 오류 발생", e);
            logger.error("   - 사용자 ID: {}", id);
            logger.error("   - 에러 타입: {}", e.getClass().getSimpleName());
            logger.error("   - 에러 메시지: {}", e.getMessage());

            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "역할 변경 중 오류가 발생했습니다.");
            errorResponse.put("message", e.getMessage());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorResponse);
        }
    }

    // ================================
    // 🔥 16일차 신규 추가 끝
    // ================================
}