package com.kmportal.backend.service;

import com.kmportal.backend.entity.Role;
import com.kmportal.backend.entity.User;
import com.kmportal.backend.repository.RoleRepository;
import com.kmportal.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 사용자 관리 서비스 (UserService)
 *
 * 이 클래스는 사용자 관리와 관련된 모든 비즈니스 로직을 처리합니다.
 * Controller는 HTTP 요청/응답만 처리하고, 실제 비즈니스 로직은
 * 이 Service 계층에서 처리됩니다.
 *
 * [왜 Service 계층이 필요한가?]
 *
 * 1. 관심사의 분리 (Separation of Concerns)
 *    - Controller: HTTP 요청/응답 처리
 *    - Service: 비즈니스 로직 처리
 *    - Repository: 데이터베이스 접근
 *
 * 2. 재사용성
 *    - 여러 Controller에서 같은 비즈니스 로직을 재사용 가능
 *    - 스케줄러, 배치 작업 등에서도 Service 재사용 가능
 *
 * 3. 테스트 용이성
 *    - Service만 따로 단위 테스트 가능
 *    - HTTP 없이 비즈니스 로직만 테스트
 *
 * 4. 트랜잭션 관리
 *    - @Transactional로 여러 DB 작업을 하나의 트랜잭션으로 묶음
 *    - 중간에 오류 발생 시 자동 롤백
 *
 * [Service 계층의 책임]
 *
 * - 비즈니스 규칙 검증 (예: 사용자명 중복 확인)
 * - 여러 Repository 조합하여 복잡한 작업 수행
 * - 도메인 로직 처리 (예: 비밀번호 암호화)
 * - 트랜잭션 경계 설정
 * - 데이터 변환 및 가공
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-12
 */
@Service
@Transactional(readOnly = true)  // 기본적으로 읽기 전용 트랜잭션
public class UserService {

    /**
     * 로깅을 위한 Logger 인스턴스
     *
     * Service 계층에서는 비즈니스 로직의 실행 흐름과
     * 중요한 의사결정 과정을 로깅합니다.
     */
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    // ================================
    // 의존성 주입 (Dependency Injection)
    // ================================

    /**
     * 사용자 데이터 액세스를 위한 Repository
     *
     * Repository 패턴:
     * - 데이터 저장소(DB)에 대한 추상화 계층
     * - Service는 어떤 DB를 사용하는지 몰라도 됨
     * - 테스트 시 Mock Repository로 쉽게 대체 가능
     */
    private final UserRepository userRepository;

    /**
     * 역할(Role) 데이터 액세스를 위한 Repository
     *
     * 사용 목적:
     * - 사용자에게 역할 할당
     * - 역할 정보 조회
     * - 권한 변경 작업
     */
    private final RoleRepository roleRepository;

    /**
     * 비밀번호 암호화를 위한 PasswordEncoder
     *
     * 보안의 핵심:
     * - 비밀번호를 평문으로 저장하면 안 됨
     * - BCrypt 알고리즘으로 단방향 암호화
     * - 같은 비밀번호도 매번 다른 암호화 결과 생성 (Salt)
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * 생성자 기반 의존성 주입
     *
     * [생성자 주입의 장점]
     *
     * 1. 불변성 (Immutability)
     *    - final 필드로 선언 가능
     *    - 객체 생성 후 의존성 변경 불가
     *
     * 2. 테스트 용이성
     *    - 생성자로 Mock 객체 주입 가능
     *    - @Autowired 없이 순수 Java 테스트 가능
     *
     * 3. 순환 참조 방지
     *    - 순환 참조 시 컴파일 타임에 에러 발생
     *    - @Autowired는 런타임에야 발견됨
     *
     * 4. 명시성
     *    - 필요한 의존성이 명확히 드러남
     *    - 의존성이 많으면 리팩토링 신호
     *
     * @param userRepository 사용자 Repository
     * @param roleRepository 역할 Repository
     * @param passwordEncoder 비밀번호 암호화기
     */
    @Autowired
    public UserService(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;

        logger.info("✅ UserService 초기화 완료");
        logger.debug("   - UserRepository: {}", userRepository.getClass().getSimpleName());
        logger.debug("   - RoleRepository: {}", roleRepository.getClass().getSimpleName());
        logger.debug("   - PasswordEncoder: {}", passwordEncoder.getClass().getSimpleName());
    }

    // ================================
    // 조회 메서드 (Read Operations)
    // ================================

    /**
     * 모든 사용자 목록 조회 (페이징 지원)
     *
     * [메서드 설계 원칙]
     *
     * 1. 입력: 페이징 정보 (페이지 번호, 크기, 정렬)
     * 2. 처리: Repository에서 페이징된 데이터 조회
     * 3. 출력: Page 객체 (데이터 + 메타정보)
     *
     * [Page 객체란?]
     *
     * Spring Data JPA가 제공하는 페이징 결과 컨테이너
     * - getContent(): 실제 데이터 리스트
     * - getTotalElements(): 전체 데이터 개수
     * - getTotalPages(): 전체 페이지 수
     * - getNumber(): 현재 페이지 번호
     * - hasNext(): 다음 페이지 존재 여부
     *
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기 (한 페이지당 데이터 개수)
     * @param sortBy 정렬 기준 필드
     * @param sortDir 정렬 방향 (asc 또는 desc)
     * @return 페이징된 사용자 목록
     */
    public Page<User> getAllUsers(int page, int size, String sortBy, String sortDir) {
        logger.info("📋 사용자 목록 조회 시작");
        logger.debug("   - 페이지: {}, 크기: {}", page, size);
        logger.debug("   - 정렬: {} {}", sortBy, sortDir);

        try {
            // 정렬 방향 설정
            Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;

            // 페이징 및 정렬 설정
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            // Repository를 통해 데이터 조회
            Page<User> userPage = userRepository.findAll(pageable);

            logger.info("✅ 사용자 목록 조회 성공");
            logger.info("   - 조회된 사용자 수: {}", userPage.getContent().size());
            logger.info("   - 전체 사용자 수: {}", userPage.getTotalElements());
            logger.info("   - 전체 페이지 수: {}", userPage.getTotalPages());

            return userPage;

        } catch (Exception e) {
            logger.error("❌ 사용자 목록 조회 실패", e);
            throw new RuntimeException("사용자 목록 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 활성 사용자만 조회
     *
     * [비즈니스 규칙]
     *
     * - isActive = true인 사용자만 조회
     * - 비활성 사용자는 시스템에서 제외
     * - 통계나 분석에서 활성 사용자만 카운트
     *
     * @param pageable 페이징 정보
     * @return 활성 사용자 목록
     */
    public Page<User> getActiveUsers(Pageable pageable) {
        logger.info("📋 활성 사용자 목록 조회 시작");

        try {
            Page<User> activeUsers = userRepository.findByIsActiveTrue(pageable);

            logger.info("✅ 활성 사용자 조회 성공: {}명", activeUsers.getTotalElements());

            return activeUsers;

        } catch (Exception e) {
            logger.error("❌ 활성 사용자 조회 실패", e);
            throw new RuntimeException("활성 사용자 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * ID로 특정 사용자 조회
     *
     * [Optional 패턴]
     *
     * - Java 8부터 도입된 null 안전 컨테이너
     * - null 체크 코드를 줄이고 명시적으로 "없을 수 있음"을 표현
     * - orElse(), orElseThrow() 등으로 안전하게 처리
     *
     * [사용 예시]
     *
     * ```java
     * Optional<User> userOpt = getUserById(1L);
     *
     * // 방법 1: 값이 있으면 처리
     * userOpt.ifPresent(user -> {
     *     System.out.println(user.getUsername());
     * });
     *
     * // 방법 2: 없으면 기본값 사용
     * User user = userOpt.orElse(new User());
     *
     * // 방법 3: 없으면 예외 발생
     * User user = userOpt.orElseThrow(() ->
     *     new RuntimeException("사용자를 찾을 수 없습니다"));
     * ```
     *
     * @param id 사용자 ID
     * @return 사용자 정보 (Optional로 감싼 결과)
     */
    public Optional<User> getUserById(Long id) {
        logger.info("🔍 사용자 조회 시작 - ID: {}", id);

        try {
            Optional<User> userOptional = userRepository.findById(id);

            if (userOptional.isPresent()) {
                User user = userOptional.get();
                logger.info("✅ 사용자 조회 성공");
                logger.debug("   - 사용자명: {}", user.getUsername());
                logger.debug("   - 이메일: {}", user.getEmail());
                logger.debug("   - 역할 수: {}", user.getRoles().size());
            } else {
                logger.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", id);
            }

            return userOptional;

        } catch (Exception e) {
            logger.error("❌ 사용자 조회 실패 - ID: {}", id, e);
            throw new RuntimeException("사용자 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자명으로 사용자 조회
     *
     * [용도]
     *
     * - 로그인 처리
     * - 사용자명 중복 확인
     * - 사용자 검색
     *
     * @param username 사용자명
     * @return 사용자 정보 (Optional로 감싼 결과)
     */
    public Optional<User> getUserByUsername(String username) {
        logger.info("🔍 사용자 조회 시작 - 사용자명: {}", username);

        try {
            Optional<User> userOptional = userRepository.findByUsername(username);

            if (userOptional.isPresent()) {
                logger.info("✅ 사용자 조회 성공");
            } else {
                logger.warn("⚠️ 사용자를 찾을 수 없음 - 사용자명: {}", username);
            }

            return userOptional;

        } catch (Exception e) {
            logger.error("❌ 사용자 조회 실패 - 사용자명: {}", username, e);
            throw new RuntimeException("사용자 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 부서별 사용자 조회
     *
     * [비즈니스 활용]
     *
     * - 조직도 표시
     * - 부서별 통계
     * - 부서 관리자에게 소속 사용자 목록 제공
     *
     * @param department 부서명
     * @return 해당 부서 사용자 목록
     */
    public List<User> getUsersByDepartment(String department) {
        logger.info("🔍 부서별 사용자 조회 - 부서: {}", department);

        try {
            List<User> users = userRepository.findByDepartmentAndIsActiveTrue(department);

            logger.info("✅ 부서별 사용자 조회 성공: {}명", users.size());

            return users;

        } catch (Exception e) {
            logger.error("❌ 부서별 사용자 조회 실패 - 부서: {}", department, e);
            throw new RuntimeException("부서별 사용자 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 검색 (이름 또는 이메일)
     *
     * [검색 로직]
     *
     * - 입력된 키워드가 이름 또는 이메일에 포함되면 검색 결과에 포함
     * - 대소문자 구분 없음 (IgnoreCase)
     * - 부분 일치 검색 (Containing)
     *
     * [예시]
     *
     * 키워드: "kim"
     * 검색 결과:
     * - 김철수 (이름에 "kim"이 포함)
     * - 홍길동 (kim@example.com - 이메일에 "kim"이 포함)
     *
     * @param keyword 검색 키워드
     * @return 검색 결과 사용자 목록
     */
    public List<User> searchUsers(String keyword) {
        logger.info("🔍 사용자 검색 시작 - 키워드: {}", keyword);

        try {
            List<User> users = userRepository
                    .findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                            keyword, keyword);

            logger.info("✅ 사용자 검색 성공: {}명", users.size());

            return users;

        } catch (Exception e) {
            logger.error("❌ 사용자 검색 실패 - 키워드: {}", keyword, e);
            throw new RuntimeException("사용자 검색 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 생성 메서드 (Create Operations)
    // ================================

    /**
     * 새 사용자 생성
     *
     * [트랜잭션 처리]
     *
     * @Transactional:
     * - 메서드 실행 전 트랜잭션 시작
     * - 정상 완료 시 자동 커밋
     * - 예외 발생 시 자동 롤백
     * - 여러 DB 작업을 하나의 단위로 처리
     *
     * [비즈니스 규칙]
     *
     * 1. 사용자명 중복 확인 (필수)
     * 2. 이메일 중복 확인 (필수)
     * 3. 비밀번호 암호화 (보안 필수)
     * 4. 기본 역할 할당 (ROLE_USER)
     * 5. 초기 상태 설정 (활성화, 잠금 해제 등)
     *
     * [에러 처리]
     *
     * - 중복 발생 시: IllegalArgumentException
     * - 역할 없음 시: RuntimeException
     * - 기타 오류 시: RuntimeException
     *
     * @param user 생성할 사용자 정보
     * @return 생성된 사용자 (ID 포함)
     * @throws IllegalArgumentException 사용자명 또는 이메일 중복
     * @throws RuntimeException 기본 역할 없음 또는 생성 실패
     */
    @Transactional  // 쓰기 작업이므로 readOnly = false (기본값)
    public User createUser(User user) {
        logger.info("➕ 새 사용자 생성 시작");
        logger.debug("   - 사용자명: {}", user.getUsername());
        logger.debug("   - 이메일: {}", user.getEmail());
        logger.debug("   - 실명: {}", user.getFullName());

        try {
            // ===== 1단계: 중복 확인 =====

            // 사용자명 중복 확인
            if (userRepository.existsByUsername(user.getUsername())) {
                logger.warn("⚠️ 사용자명 중복 - {}", user.getUsername());
                throw new IllegalArgumentException("이미 사용 중인 사용자명입니다.");
            }

            // 이메일 중복 확인
            if (userRepository.existsByEmail(user.getEmail())) {
                logger.warn("⚠️ 이메일 중복 - {}", user.getEmail());
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }

            logger.info("✅ 중복 확인 통과");

            // ===== 2단계: 비밀번호 암호화 =====

            /**
             * [비밀번호 암호화 과정]
             *
             * 입력: "admin123" (평문)
             * ↓
             * BCrypt 알고리즘 적용
             * - Salt 자동 생성 (랜덤 값)
             * - 여러 번 해싱 (기본 12 rounds)
             * ↓
             * 출력: "$2a$12$abcdefgh..." (암호화된 문자열)
             *
             * [특징]
             *
             * - 단방향 암호화: 복호화 불가능
             * - Salt 내장: 같은 비밀번호도 매번 다른 결과
             * - 시간 조절 가능: rounds 값으로 보안 강도 조절
             */
            String rawPassword = user.getPassword();
            String encodedPassword = passwordEncoder.encode(rawPassword);
            user.setPassword(encodedPassword);

            logger.info("✅ 비밀번호 암호화 완료");
            logger.debug("   - 원본 길이: {} 문자", rawPassword.length());
            logger.debug("   - 암호화 길이: {} 문자", encodedPassword.length());

            // ===== 3단계: 초기 상태 설정 =====

            /**
             * [사용자 초기 상태]
             *
             * 활성화: true (바로 로그인 가능)
             * 잠금: false (정상 상태)
             * 비밀번호 만료: false (정상 사용 가능)
             * 실패 횟수: 0 (초기값)
             *
             * [환경별 다른 설정]
             *
             * 개발 환경: 바로 활성화 (빠른 테스트)
             * 운영 환경: 관리자 승인 필요 (보안 강화)
             */
            user.setIsActive(true);
            user.setIsLocked(false);
            user.setPasswordExpired(false);
            user.setFailedLoginAttempts(0);

            logger.info("✅ 초기 상태 설정 완료");

            // ===== 4단계: 기본 역할 할당 =====

            /**
             * [역할 할당 로직]
             *
             * 1. DB에서 "ROLE_USER" 역할 조회
             * 2. 역할이 없으면 RuntimeException 발생
             * 3. user.addRole()로 양방향 관계 설정
             *
             * [양방향 관계란?]
             *
             * User → Role: user.getRoles()로 접근
             * Role → User: role.getUsers()로 접근
             *
             * addRole() 메서드가 양쪽 모두 설정:
             * - user.getRoles().add(role)
             * - role.getUsers().add(user)
             */
            Role userRole = roleRepository.findByRoleName("ROLE_USER")
                    .orElseThrow(() -> {
                        logger.error("❌ 기본 역할(ROLE_USER)을 찾을 수 없음");
                        return new RuntimeException("기본 역할(ROLE_USER)을 찾을 수 없습니다.");
                    });

            user.addRole(userRole);

            logger.info("✅ 기본 역할 할당 완료");
            logger.debug("   - 역할: {} ({})", userRole.getDisplayName(), userRole.getRoleName());

            // ===== 5단계: 데이터베이스 저장 =====

            /**
             * [저장 과정]
             *
             * userRepository.save(user) 호출 시:
             *
             * 1. JPA가 User 엔티티를 영속성 컨텍스트에 추가
             * 2. INSERT 쿼리 생성 및 실행
             *    - users 테이블에 사용자 정보 삽입
             *    - user_roles 테이블에 역할 연결 정보 삽입
             * 3. 생성된 ID를 User 객체에 자동 설정
             * 4. 트랜잭션 커밋 (메서드 정상 종료 시)
             *
             * [자동 생성되는 것들]
             *
             * - userId: AUTO_INCREMENT로 자동 생성
             * - createdAt: @CreationTimestamp로 현재 시간 설정
             * - updatedAt: @UpdateTimestamp로 현재 시간 설정
             */
            User savedUser = userRepository.save(user);

            logger.info("🎉 사용자 생성 성공!");
            logger.info("   - 사용자 ID: {}", savedUser.getUserId());
            logger.info("   - 사용자명: {}", savedUser.getUsername());
            logger.info("   - 이메일: {}", savedUser.getEmail());
            logger.info("   - 생성 시간: {}", savedUser.getCreatedAt());

            return savedUser;

        } catch (IllegalArgumentException e) {
            // 중복 등 비즈니스 규칙 위반 (클라이언트 오류)
            logger.warn("⚠️ 사용자 생성 실패: {}", e.getMessage());
            throw e;

        } catch (RuntimeException e) {
            // 시스템 오류 (서버 오류)
            logger.error("❌ 사용자 생성 중 오류 발생", e);
            throw new RuntimeException("사용자 생성 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 수정 메서드 (Update Operations)
    // ================================

    /**
     * 사용자 정보 수정
     *
     * [수정 가능한 필드]
     *
     * - 이메일
     * - 실명 (fullName)
     * - 부서 (department)
     * - 직책 (position)
     * - 연락처 (phoneNumber)
     *
     * [수정 불가능한 필드]
     *
     * - 사용자명 (username): 변경 불가
     * - 비밀번호 (password): 별도 메서드로 변경
     * - 역할 (roles): 별도 메서드로 변경
     * - ID, 생성일 등: 자동 관리 필드
     *
     * [JPA 변경 감지]
     *
     * 1. userRepository.findById()로 엔티티 조회
     *    → 영속성 컨텍스트에 엔티티 저장
     *
     * 2. 엔티티의 필드 값 변경 (setter 호출)
     *    → JPA가 변경사항 추적
     *
     * 3. 트랜잭션 커밋 시
     *    → JPA가 자동으로 UPDATE 쿼리 실행
     *    → save() 호출 불필요!
     *
     * 하지만 명시성을 위해 save()를 호출하기도 함.
     *
     * @param id 수정할 사용자 ID
     * @param updatedUser 수정할 정보가 담긴 User 객체
     * @return 수정된 사용자 정보
     * @throws RuntimeException 사용자를 찾을 수 없거나 수정 실패
     */
    @Transactional
    public User updateUser(Long id, User updatedUser) {
        logger.info("✏️ 사용자 정보 수정 시작 - ID: {}", id);

        try {
            // ===== 1단계: 기존 사용자 조회 =====

            User existingUser = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", id);
                        return new RuntimeException("사용자를 찾을 수 없습니다.");
                    });

            logger.info("✅ 기존 사용자 조회 성공");
            logger.debug("   - 기존 이메일: {}", existingUser.getEmail());

            // ===== 2단계: 이메일 중복 확인 (이메일 변경 시) =====

            /**
             * [이메일 변경 시 중복 확인 로직]
             *
             * 1. 이메일이 변경되었는지 확인
             *    - 기존 이메일 ≠ 새 이메일
             *
             * 2. 변경된 경우에만 중복 확인
             *    - 다른 사용자가 사용 중인지 체크
             *    - 본인 것은 제외 (existsByEmail만으로는 부족)
             *
             * 3. 중복이면 예외 발생
             */
            if (!existingUser.getEmail().equals(updatedUser.getEmail())) {
                logger.info("📧 이메일 변경 감지: {} → {}",
                        existingUser.getEmail(), updatedUser.getEmail());

                if (userRepository.existsByEmail(updatedUser.getEmail())) {
                    logger.warn("⚠️ 이메일 중복 - {}", updatedUser.getEmail());
                    throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                }

                logger.info("✅ 이메일 중복 확인 통과");
            }

            // ===== 3단계: 필드별 업데이트 =====

            /**
             * [Null 체크가 중요한 이유]
             *
             * 클라이언트가 일부 필드만 보내는 경우:
             * {
             *   "email": "new@example.com"
             *   // department, position 등은 null
             * }
             *
             * null 체크 없이 업데이트하면:
             * - 의도치 않게 필드가 null로 변경됨
             * - 데이터 손실 발생
             *
             * 올바른 처리:
             * - null이 아닌 필드만 업데이트
             * - 나머지 필드는 기존 값 유지
             */

            // 이메일 업데이트
            if (updatedUser.getEmail() != null) {
                existingUser.setEmail(updatedUser.getEmail());
                logger.debug("   ✓ 이메일 업데이트: {}", updatedUser.getEmail());
            }

            // 실명 업데이트
            if (updatedUser.getFullName() != null) {
                existingUser.setFullName(updatedUser.getFullName());
                logger.debug("   ✓ 실명 업데이트: {}", updatedUser.getFullName());
            }

            // 부서 업데이트
            if (updatedUser.getDepartment() != null) {
                existingUser.setDepartment(updatedUser.getDepartment());
                logger.debug("   ✓ 부서 업데이트: {}", updatedUser.getDepartment());
            }

            // 직책 업데이트
            if (updatedUser.getPosition() != null) {
                existingUser.setPosition(updatedUser.getPosition());
                logger.debug("   ✓ 직책 업데이트: {}", updatedUser.getPosition());
            }

            // 연락처 업데이트
            if (updatedUser.getPhoneNumber() != null) {
                existingUser.setPhoneNumber(updatedUser.getPhoneNumber());
                logger.debug("   ✓ 연락처 업데이트: {}", updatedUser.getPhoneNumber());
            }

            // ===== 4단계: 저장 (명시적 호출) =====

            /**
             * [save() 호출의 의미]
             *
             * JPA의 변경 감지(Dirty Checking)로 인해
             * 트랜잭션 커밋 시 자동으로 UPDATE 쿼리가 실행되므로
             * 사실 save()를 호출하지 않아도 됩니다.
             *
             * 하지만 명시적으로 save()를 호출하는 이유:
             * 1. 가독성: 저장 시점이 명확함
             * 2. 관습: Spring Data JPA 사용 패턴
             * 3. 안전성: 영속성 컨텍스트 동기화 보장
             */
            User savedUser = userRepository.save(existingUser);

            logger.info("🎉 사용자 정보 수정 완료!");
            logger.info("   - 사용자 ID: {}", savedUser.getUserId());
            logger.info("   - 수정 시간: {}", savedUser.getUpdatedAt());

            return savedUser;

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ 사용자 수정 실패: {}", e.getMessage());
            throw e;

        } catch (RuntimeException e) {
            logger.error("❌ 사용자 수정 중 오류 발생", e);
            throw new RuntimeException("사용자 정보 수정 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 역할 변경
     *
     * [역할 변경 프로세스]
     *
     * 1. 사용자 조회 및 존재 확인
     * 2. 새로운 역할 ID 목록 검증
     * 3. 기존 역할 모두 제거
     * 4. 새 역할 할당
     * 5. 데이터베이스 저장
     *
     * [다대다 관계 관리]
     *
     * User ↔ Role은 다대다 관계:
     * - 한 사용자는 여러 역할을 가질 수 있음
     * - 한 역할은 여러 사용자에게 할당될 수 있음
     * - 중간 테이블(user_roles)로 관계 관리
     *
     * 역할 변경 시:
     * 1. user.getRoles().clear()
     *    → user_roles 테이블에서 해당 사용자의 모든 연결 삭제
     *
     * 2. user.addRole(role)
     *    → user_roles 테이블에 새 연결 삽입
     *
     * @param userId 사용자 ID
     * @param roleIds 새로 할당할 역할 ID 목록
     * @return 업데이트된 사용자 정보 (역할 포함)
     * @throws RuntimeException 사용자 또는 역할을 찾을 수 없거나 변경 실패
     */
    @Transactional
    public User updateUserRoles(Long userId, List<Long> roleIds) {
        logger.info("🔄 사용자 역할 변경 시작 - 사용자 ID: {}", userId);
        logger.debug("   - 새 역할 ID 목록: {}", roleIds);

        try {
            // ===== 1단계: 사용자 조회 =====

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> {
                        logger.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", userId);
                        return new RuntimeException("사용자를 찾을 수 없습니다.");
                    });

            logger.info("✅ 사용자 조회 성공");
            logger.debug("   - 사용자명: {}", user.getUsername());
            logger.debug("   - 현재 역할 수: {}", user.getRoles().size());

            // ===== 2단계: 역할 ID 목록 검증 =====

            if (roleIds == null || roleIds.isEmpty()) {
                logger.warn("⚠️ 역할 ID 목록이 비어있음");
                throw new IllegalArgumentException("최소 1개 이상의 역할을 선택해야 합니다.");
            }

            logger.info("✅ 역할 ID 목록 검증 통과 - 개수: {}", roleIds.size());

            // ===== 3단계: 기존 역할 모두 제거 =====

            /**
             * [clear() 메서드의 동작]
             *
             * Set<Role> roles = user.getRoles();
             * roles.clear();
             *
             * 실행 결과:
             * 1. Set에서 모든 Role 제거
             * 2. JPA가 이를 감지
             * 3. 트랜잭션 커밋 시:
             *    DELETE FROM user_roles WHERE user_id = ?
             *
             * 주의:
             * - Role 엔티티 자체는 삭제되지 않음
             * - 단지 User와의 연결만 제거
             */
            int oldRoleCount = user.getRoles().size();
            user.getRoles().clear();

            logger.info("✅ 기존 역할 {}개 제거 완료", oldRoleCount);

            // ===== 4단계: 새 역할 할당 =====

            int assignedCount = 0;     // 성공적으로 할당된 역할 수
            int notFoundCount = 0;     // 찾을 수 없는 역할 ID 수

            for (Long roleId : roleIds) {
                logger.debug("   처리 중: 역할 ID {}", roleId);

                Optional<Role> roleOptional = roleRepository.findById(roleId);

                if (roleOptional.isPresent()) {
                    Role role = roleOptional.get();

                    /**
                     * [양방향 관계 설정]
                     *
                     * user.addRole(role) 메서드 내부:
                     *
                     * public void addRole(Role role) {
                     *     this.roles.add(role);           // User → Role
                     *     role.getUsers().add(this);      // Role → User
                     * }
                     *
                     * 양쪽 모두 설정해야 하는 이유:
                     * - JPA 영속성 컨텍스트의 일관성 유지
                     * - 양방향 탐색 가능
                     * - 데이터베이스 동기화 보장
                     */
                    user.addRole(role);

                    assignedCount++;
                    logger.debug("     ✅ 역할 추가 성공: {} ({})",
                            role.getDisplayName(), role.getRoleName());
                } else {
                    notFoundCount++;
                    logger.warn("     ⚠️ 역할을 찾을 수 없음 - ID: {}", roleId);
                }
            }

            logger.info("📊 역할 할당 결과:");
            logger.info("   - 성공: {}개", assignedCount);
            logger.info("   - 실패: {}개", notFoundCount);

            // 유효한 역할이 하나도 없는 경우 에러
            if (assignedCount == 0) {
                logger.warn("⚠️ 할당된 역할이 없음");
                throw new IllegalArgumentException("유효한 역할을 찾을 수 없습니다.");
            }

            // ===== 5단계: 저장 =====

            User updatedUser = userRepository.save(user);

            logger.info("🎉 사용자 역할 변경 완료!");
            logger.info("   - 사용자 ID: {}", userId);
            logger.info("   - 사용자명: {}", updatedUser.getUsername());
            logger.info("   - 새 역할 수: {}", updatedUser.getRoles().size());

            return updatedUser;

        } catch (IllegalArgumentException e) {
            logger.warn("⚠️ 역할 변경 실패: {}", e.getMessage());
            throw e;

        } catch (RuntimeException e) {
            logger.error("❌ 역할 변경 중 오류 발생", e);
            throw new RuntimeException("역할 변경 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 활성화/비활성화
     *
     * [소프트 삭제 패턴]
     *
     * 실제로 데이터를 삭제하지 않고 상태만 변경:
     * - 활성화 (isActive = true): 로그인 가능
     * - 비활성화 (isActive = false): 로그인 불가
     *
     * [소프트 삭제의 장점]
     *
     * 1. 데이터 보존
     *    - 과거 기록 유지
     *    - 감사(Audit) 추적 가능
     *    - 필요 시 복구 가능
     *
     * 2. 참조 무결성
     *    - 외래키 제약 위반 없음
     *    - 연관 데이터 영향 최소화
     *
     * 3. 통계 분석
     *    - 전체 사용자 수 집계 가능
     *    - 이탈률 분석 가능
     *
     * @param id 사용자 ID
     * @param active 활성화 상태 (true: 활성화, false: 비활성화)
     * @return 상태가 변경된 사용자 정보
     * @throws RuntimeException 사용자를 찾을 수 없거나 상태 변경 실패
     */
    @Transactional
    public User toggleUserActive(Long id, boolean active) {
        logger.info("🔄 사용자 활성화 상태 변경 시작 - ID: {}", id);
        logger.debug("   - 새 상태: {}", active ? "활성화" : "비활성화");

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", id);
                        return new RuntimeException("사용자를 찾을 수 없습니다.");
                    });

            boolean oldStatus = user.getIsActive();
            user.setIsActive(active);

            User updatedUser = userRepository.save(user);

            logger.info("🎉 사용자 활성화 상태 변경 완료!");
            logger.info("   - 사용자 ID: {}", id);
            logger.info("   - 사용자명: {}", updatedUser.getUsername());
            logger.info("   - 이전 상태: {}", oldStatus ? "활성" : "비활성");
            logger.info("   - 새 상태: {}", active ? "활성" : "비활성");

            return updatedUser;

        } catch (RuntimeException e) {
            logger.error("❌ 활성화 상태 변경 중 오류 발생", e);
            throw new RuntimeException("활성화 상태 변경 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 계정 잠금/해제
     *
     * [보안 정책]
     *
     * 계정 잠금 사유:
     * 1. 로그인 실패 횟수 초과
     * 2. 의심스러운 활동 감지
     * 3. 관리자의 수동 잠금
     * 4. 장기간 미사용
     *
     * 잠금 해제 방법:
     * 1. 일정 시간 경과 후 자동 해제
     * 2. 관리자의 수동 해제
     * 3. 본인 인증 후 해제
     *
     * @param id 사용자 ID
     * @param locked 잠금 상태 (true: 잠금, false: 해제)
     * @return 상태가 변경된 사용자 정보
     * @throws RuntimeException 사용자를 찾을 수 없거나 상태 변경 실패
     */
    @Transactional
    public User toggleUserLocked(Long id, boolean locked) {
        logger.info("🔒 사용자 잠금 상태 변경 시작 - ID: {}", id);
        logger.debug("   - 새 상태: {}", locked ? "잠금" : "해제");

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", id);
                        return new RuntimeException("사용자를 찾을 수 없습니다.");
                    });

            boolean oldStatus = user.getIsLocked();

            if (locked) {
                // 잠금 설정
                user.setIsLocked(true);
                user.setLockedAt(LocalDateTime.now());
            } else {
                // 잠금 해제
                user.setIsLocked(false);
                user.setLockedAt(null);
                user.setFailedLoginAttempts(0);  // 실패 횟수도 초기화
            }

            User updatedUser = userRepository.save(user);

            logger.info("🎉 사용자 잠금 상태 변경 완료!");
            logger.info("   - 사용자 ID: {}", id);
            logger.info("   - 사용자명: {}", updatedUser.getUsername());
            logger.info("   - 이전 상태: {}", oldStatus ? "잠금" : "정상");
            logger.info("   - 새 상태: {}", locked ? "잠금" : "정상");

            if (locked) {
                logger.info("   - 잠금 시간: {}", updatedUser.getLockedAt());
            }

            return updatedUser;

        } catch (RuntimeException e) {
            logger.error("❌ 잠금 상태 변경 중 오류 발생", e);
            throw new RuntimeException("잠금 상태 변경 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 삭제 메서드 (Delete Operations)
    // ================================

    /**
     * 사용자 삭제 (소프트 삭제)
     *
     * [소프트 삭제 vs 하드 삭제]
     *
     * 소프트 삭제 (Soft Delete):
     * - 실제로 데이터를 삭제하지 않음
     * - isActive를 false로 설정
     * - 데이터는 DB에 남아있음
     * - 복구 가능
     *
     * 하드 삭제 (Hard Delete):
     * - 실제로 데이터를 DB에서 삭제
     * - DELETE 쿼리 실행
     * - 복구 불가능
     * - 외래키 제약 위반 가능
     *
     * [언제 하드 삭제를 사용하나?]
     *
     * - 개인정보 보호법: GDPR 등의 삭제 요청
     * - 테스트 데이터 정리
     * - 스팸/악의적 사용자 완전 제거
     *
     * @param id 삭제할 사용자 ID
     * @throws RuntimeException 사용자를 찾을 수 없거나 삭제 실패
     */
    @Transactional
    public void deleteUser(Long id) {
        logger.info("🗑️ 사용자 삭제(비활성화) 시작 - ID: {}", id);

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", id);
                        return new RuntimeException("사용자를 찾을 수 없습니다.");
                    });

            // 소프트 삭제: 비활성화 처리
            user.setIsActive(false);
            userRepository.save(user);

            logger.info("🎉 사용자 삭제(비활성화) 완료!");
            logger.info("   - 사용자 ID: {}", id);
            logger.info("   - 사용자명: {}", user.getUsername());
            logger.info("   - 처리 시간: {}", LocalDateTime.now());

        } catch (RuntimeException e) {
            logger.error("❌ 사용자 삭제 중 오류 발생", e);
            throw new RuntimeException("사용자 삭제 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 완전 삭제 (하드 삭제)
     *
     * [주의사항]
     *
     * ⚠️ 이 메서드는 데이터를 완전히 삭제합니다!
     *
     * 1. 복구 불가능
     *    - 삭제된 데이터는 영구적으로 사라짐
     *    - 백업이 없으면 복구 방법 없음
     *
     * 2. 참조 무결성 위반 가능
     *    - 다른 테이블에서 이 사용자를 참조하는 경우
     *    - 외래키 제약 조건 오류 발생 가능
     *
     * 3. 감사 추적 불가
     *    - 과거 기록이 사라져 추적 불가
     *    - 규정 준수 문제 발생 가능
     *
     * [사용 시나리오]
     *
     * - 개인정보 삭제 요청 (법적 의무)
     * - 테스트 데이터 정리
     * - 스팸 계정 완전 제거
     *
     * @param id 완전 삭제할 사용자 ID
     * @throws RuntimeException 사용자를 찾을 수 없거나 삭제 실패
     */
    @Transactional
    public void permanentlyDeleteUser(Long id) {
        logger.warn("⚠️⚠️⚠️ 사용자 완전 삭제(하드 삭제) 시작 - ID: {}", id);
        logger.warn("⚠️ 이 작업은 되돌릴 수 없습니다!");

        try {
            User user = userRepository.findById(id)
                    .orElseThrow(() -> {
                        logger.warn("⚠️ 사용자를 찾을 수 없음 - ID: {}", id);
                        return new RuntimeException("사용자를 찾을 수 없습니다.");
                    });

            String username = user.getUsername();
            String email = user.getEmail();

            // 실제 삭제 (DELETE 쿼리 실행)
            userRepository.delete(user);

            logger.warn("🗑️ 사용자 완전 삭제 완료!");
            logger.warn("   - 삭제된 사용자 ID: {}", id);
            logger.warn("   - 삭제된 사용자명: {}", username);
            logger.warn("   - 삭제된 이메일: {}", email);
            logger.warn("   - 처리 시간: {}", LocalDateTime.now());

        } catch (RuntimeException e) {
            logger.error("❌ 사용자 완전 삭제 중 오류 발생", e);
            throw new RuntimeException("사용자 완전 삭제 중 오류가 발생했습니다.", e);
        }
    }

    // ================================
    // 통계 및 유틸리티 메서드
    // ================================

    /**
     * 활성 사용자 수 조회
     *
     * [사용 예시]
     *
     * - 대시보드 통계
     * - 시스템 모니터링
     * - 리포트 생성
     *
     * @return 활성 사용자 수
     */
    public long getActiveUserCount() {
        logger.debug("📊 활성 사용자 수 조회");

        try {
            long count = userRepository.countByIsActiveTrue();

            logger.debug("✅ 활성 사용자 수: {}", count);

            return count;

        } catch (Exception e) {
            logger.error("❌ 활성 사용자 수 조회 실패", e);
            throw new RuntimeException("활성 사용자 수 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 전체 사용자 수 조회
     *
     * @return 전체 사용자 수 (활성 + 비활성)
     */
    public long getTotalUserCount() {
        logger.debug("📊 전체 사용자 수 조회");

        try {
            long count = userRepository.count();

            logger.debug("✅ 전체 사용자 수: {}", count);

            return count;

        } catch (Exception e) {
            logger.error("❌ 전체 사용자 수 조회 실패", e);
            throw new RuntimeException("전체 사용자 수 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 특정 역할을 가진 사용자 수 조회
     *
     * @param roleName 역할명 (예: "ROLE_ADMIN")
     * @return 해당 역할을 가진 사용자 수
     */
    public long getUserCountByRole(String roleName) {
        logger.debug("📊 역할별 사용자 수 조회 - 역할: {}", roleName);

        try {
            List<User> users = userRepository.findByRoleName(roleName);
            long count = users.size();

            logger.debug("✅ 역할별 사용자 수: {}", count);

            return count;

        } catch (Exception e) {
            logger.error("❌ 역할별 사용자 수 조회 실패 - 역할: {}", roleName, e);
            throw new RuntimeException("역할별 사용자 수 조회 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자명 중복 확인
     *
     * [사용 목적]
     *
     * - 회원가입 시 실시간 중복 확인
     * - 사용자명 변경 시 중복 확인
     *
     * @param username 확인할 사용자명
     * @return 중복이면 true, 아니면 false
     */
    public boolean isUsernameExists(String username) {
        logger.debug("🔍 사용자명 중복 확인 - {}", username);

        try {
            boolean exists = userRepository.existsByUsername(username);

            logger.debug("   결과: {}", exists ? "중복" : "사용 가능");

            return exists;

        } catch (Exception e) {
            logger.error("❌ 사용자명 중복 확인 실패 - {}", username, e);
            throw new RuntimeException("사용자명 중복 확인 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일
     * @return 중복이면 true, 아니면 false
     */
    public boolean isEmailExists(String email) {
        logger.debug("🔍 이메일 중복 확인 - {}", email);

        try {
            boolean exists = userRepository.existsByEmail(email);

            logger.debug("   결과: {}", exists ? "중복" : "사용 가능");

            return exists;

        } catch (Exception e) {
            logger.error("❌ 이메일 중복 확인 실패 - {}", email, e);
            throw new RuntimeException("이메일 중복 확인 중 오류가 발생했습니다.", e);
        }
    }
}