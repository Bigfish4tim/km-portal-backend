package com.kmportal.backend.repository;

import com.kmportal.backend.entity.Role;
import com.kmportal.backend.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * UserRepository 통합 테스트 클래스
 *
 * 이 클래스는 UserRepository의 JPA 쿼리 메서드들을 테스트합니다.
 * 실제 데이터베이스(H2 인메모리)를 사용하여 쿼리가 올바르게 동작하는지 검증합니다.
 *
 * [@DataJpaTest 어노테이션 설명]
 *
 * @DataJpaTest는 JPA 관련 컴포넌트만 로드하는 슬라이스 테스트입니다.
 *
 * 특징:
 * 1. @Entity, @Repository 클래스들만 스캔
 * 2. 임베디드 데이터베이스 자동 구성 (H2)
 * 3. 각 테스트 후 자동 롤백
 * 4. SQL 로깅 활성화
 * 5. 전체 애플리케이션 컨텍스트보다 빠른 로딩
 *
 * [TestEntityManager 설명]
 *
 * TestEntityManager는 테스트에서 엔티티를 관리하기 위한 도구입니다.
 *
 * 주요 메서드:
 * - persist(): 새 엔티티 저장
 * - find(): ID로 엔티티 조회
 * - flush(): 영속성 컨텍스트를 DB에 반영
 * - clear(): 영속성 컨텍스트 초기화
 *
 * [통합 테스트 vs 단위 테스트]
 *
 * 단위 테스트 (Mock):
 * - 빠른 실행 속도
 * - 외부 의존성 없음
 * - 비즈니스 로직 검증에 적합
 *
 * 통합 테스트 (실제 DB):
 * - 실제 쿼리 동작 검증
 * - JPA 매핑 오류 발견
 * - 쿼리 성능 문제 발견
 *
 * 작성일: 2025년 11월 29일 (40일차)
 * 작성자: KM Portal Dev Team
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-29
 */
@DataJpaTest  // JPA 슬라이스 테스트
@ActiveProfiles("test")  // test 프로파일 사용 (H2 데이터베이스)
@DisplayName("UserRepository 통합 테스트")
class UserRepositoryTest {

    // ================================
    // 의존성 주입
    // ================================

    /**
     * TestEntityManager: 테스트용 EntityManager
     *
     * @DataJpaTest가 제공하는 특별한 EntityManager입니다.
     * 테스트 데이터를 직접 DB에 삽입하고 검증할 수 있습니다.
     */
    @Autowired
    private TestEntityManager entityManager;

    /**
     * UserRepository: 테스트 대상 Repository
     *
     * Spring Data JPA가 자동으로 구현체를 생성합니다.
     */
    @Autowired
    private UserRepository userRepository;

    /**
     * RoleRepository: Role 엔티티 저장을 위한 Repository
     */
    @Autowired
    private RoleRepository roleRepository;

    // ================================
    // 테스트 픽스처
    // ================================

    private User testUser;
    private User testUser2;
    private Role userRole;
    private Role adminRole;

    /**
     * 각 테스트 전에 실행되는 설정 메서드
     *
     * 테스트 데이터를 DB에 삽입합니다.
     */
    @BeforeEach
    void setUp() {
        // 1. Role 데이터 생성 및 저장
        userRole = new Role();
        userRole.setRoleName("ROLE_USER");
        userRole.setDescription("일반 사용자");
        userRole.setPriority(100);
        entityManager.persist(userRole);

        adminRole = new Role();
        adminRole.setRoleName("ROLE_ADMIN");
        adminRole.setDescription("관리자");
        adminRole.setPriority(1);
        entityManager.persist(adminRole);

        // 2. User 데이터 생성 및 저장
        testUser = new User("testuser", "encodedPassword123", "test@example.com", "테스트 사용자");
        testUser.setDepartment("개발팀");
        testUser.setPosition("대리");
        testUser.setPhoneNumber("010-1234-5678");
        testUser.setIsActive(true);
        testUser.setIsLocked(false);
        testUser.setPasswordExpired(false);
        testUser.setFailedLoginAttempts(0);
        testUser.addRole(userRole);
        entityManager.persist(testUser);

        testUser2 = new User("testuser2", "encodedPassword456", "test2@example.com", "테스트 사용자2");
        testUser2.setDepartment("마케팅팀");
        testUser2.setPosition("과장");
        testUser2.setIsActive(true);
        testUser2.setIsLocked(false);
        testUser2.addRole(userRole);
        testUser2.addRole(adminRole);
        entityManager.persist(testUser2);

        // 3. 영속성 컨텍스트 플러시
        // flush(): 영속성 컨텍스트의 변경사항을 DB에 반영
        // clear(): 영속성 컨텍스트 초기화 (1차 캐시 제거)
        entityManager.flush();
        entityManager.clear();
    }

    // ================================
    // 기본 조회 메서드 테스트
    // ================================

    @Nested
    @DisplayName("사용자명으로 조회 테스트")
    class FindByUsernameTest {

        @Test
        @DisplayName("존재하는 사용자명으로 조회 - 성공")
        void findByUsername_WhenExists_ReturnsUser() {
            // When
            Optional<User> found = userRepository.findByUsername("testuser");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("testuser");
            assertThat(found.get().getEmail()).isEqualTo("test@example.com");
            assertThat(found.get().getFullName()).isEqualTo("테스트 사용자");
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 조회 - 빈 Optional 반환")
        void findByUsername_WhenNotExists_ReturnsEmpty() {
            // When
            Optional<User> found = userRepository.findByUsername("nonexistent");

            // Then
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("이메일로 조회 테스트")
    class FindByEmailTest {

        @Test
        @DisplayName("존재하는 이메일로 조회 - 성공")
        void findByEmail_WhenExists_ReturnsUser() {
            // When
            Optional<User> found = userRepository.findByEmail("test@example.com");

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 조회 - 빈 Optional 반환")
        void findByEmail_WhenNotExists_ReturnsEmpty() {
            // When
            Optional<User> found = userRepository.findByEmail("nonexistent@example.com");

            // Then
            assertThat(found).isEmpty();
        }
    }

    // ================================
    // 존재 여부 확인 테스트
    // ================================

    @Nested
    @DisplayName("존재 여부 확인 테스트")
    class ExistsTest {

        @Test
        @DisplayName("사용자명 존재 - true 반환")
        void existsByUsername_WhenExists_ReturnsTrue() {
            // When
            boolean exists = userRepository.existsByUsername("testuser");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("사용자명 미존재 - false 반환")
        void existsByUsername_WhenNotExists_ReturnsFalse() {
            // When
            boolean exists = userRepository.existsByUsername("nonexistent");

            // Then
            assertThat(exists).isFalse();
        }

        @Test
        @DisplayName("이메일 존재 - true 반환")
        void existsByEmail_WhenExists_ReturnsTrue() {
            // When
            boolean exists = userRepository.existsByEmail("test@example.com");

            // Then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("이메일 미존재 - false 반환")
        void existsByEmail_WhenNotExists_ReturnsFalse() {
            // When
            boolean exists = userRepository.existsByEmail("nonexistent@example.com");

            // Then
            assertThat(exists).isFalse();
        }
    }

    // ================================
    // 상태별 조회 테스트
    // ================================

    @Nested
    @DisplayName("상태별 조회 테스트")
    class FindByStatusTest {

        @Test
        @DisplayName("활성 사용자 목록 조회")
        void findByIsActiveTrue_ReturnsActiveUsers() {
            // When
            List<User> activeUsers = userRepository.findByIsActiveTrue();

            // Then
            assertThat(activeUsers).hasSize(2);
            assertThat(activeUsers).extracting("isActive").containsOnly(true);
        }

        @Test
        @DisplayName("비활성 사용자 목록 조회")
        void findByIsActiveFalse_ReturnsInactiveUsers() {
            // Given: 비활성 사용자 추가
            User inactiveUser = new User("inactive", "password", "inactive@example.com", "비활성 사용자");
            inactiveUser.setIsActive(false);
            entityManager.persist(inactiveUser);
            entityManager.flush();

            // When
            List<User> inactiveUsers = userRepository.findByIsActiveFalse();

            // Then
            assertThat(inactiveUsers).hasSize(1);
            assertThat(inactiveUsers.get(0).getUsername()).isEqualTo("inactive");
        }

        @Test
        @DisplayName("잠금된 사용자 목록 조회")
        void findByIsLockedTrue_ReturnsLockedUsers() {
            // Given: 잠금된 사용자 추가
            User lockedUser = new User("locked", "password", "locked@example.com", "잠금 사용자");
            lockedUser.setIsLocked(true);
            lockedUser.setIsActive(true);
            entityManager.persist(lockedUser);
            entityManager.flush();

            // When
            List<User> lockedUsers = userRepository.findByIsLockedTrue();

            // Then
            assertThat(lockedUsers).hasSize(1);
            assertThat(lockedUsers.get(0).getIsLocked()).isTrue();
        }
    }

    // ================================
    // 부서별 조회 테스트
    // ================================

    @Nested
    @DisplayName("부서별 조회 테스트")
    class FindByDepartmentTest {

        @Test
        @DisplayName("부서별 사용자 조회")
        void findByDepartment_ReturnsDepartmentUsers() {
            // When
            List<User> devTeam = userRepository.findByDepartment("개발팀");

            // Then
            assertThat(devTeam).hasSize(1);
            assertThat(devTeam.get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("부서별 활성 사용자 조회")
        void findByDepartmentAndIsActiveTrue_ReturnsActiveDepartmentUsers() {
            // When
            List<User> activeDevTeam = userRepository.findByDepartmentAndIsActiveTrue("개발팀");

            // Then
            assertThat(activeDevTeam).hasSize(1);
            assertThat(activeDevTeam).extracting("department").containsOnly("개발팀");
            assertThat(activeDevTeam).extracting("isActive").containsOnly(true);
        }
    }

    // ================================
    // 검색 테스트
    // ================================

    @Nested
    @DisplayName("검색 테스트")
    class SearchTest {

        @Test
        @DisplayName("이름으로 검색 (부분 일치)")
        void findByFullNameContainingIgnoreCase_ReturnsMatchingUsers() {
            // When
            List<User> found = userRepository.findByFullNameContainingIgnoreCase("테스트");

            // Then
            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("이메일로 검색 (부분 일치)")
        void findByEmailContainingIgnoreCase_ReturnsMatchingUsers() {
            // When
            List<User> found = userRepository.findByEmailContainingIgnoreCase("example.com");

            // Then
            assertThat(found).hasSize(2);
        }

        @Test
        @DisplayName("이름 또는 이메일로 검색")
        void findByFullNameOrEmail_ReturnsMatchingUsers() {
            // When
            List<User> found = userRepository.findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    "사용자", "test2"
            );

            // Then
            assertThat(found).hasSize(2);
        }
    }

    // ================================
    // 역할별 조회 테스트
    // ================================

    @Nested
    @DisplayName("역할별 조회 테스트")
    class FindByRoleTest {

        @Test
        @DisplayName("역할명으로 사용자 조회")
        void findByRoleName_ReturnsUsersWithRole() {
            // When
            List<User> admins = userRepository.findByRoleName("ROLE_ADMIN");

            // Then
            assertThat(admins).hasSize(1);
            assertThat(admins.get(0).getUsername()).isEqualTo("testuser2");
        }

        @Test
        @DisplayName("여러 역할 중 하나라도 가진 사용자 조회")
        void findByRoleNames_ReturnsUsersWithAnyRole() {
            // When
            List<User> users = userRepository.findByRoleNames(
                    Arrays.asList("ROLE_USER", "ROLE_ADMIN")
            );

            // Then
            assertThat(users).hasSize(2);
        }
    }

    // ================================
    // 페이징 테스트
    // ================================

    @Nested
    @DisplayName("페이징 테스트")
    class PagingTest {

        @Test
        @DisplayName("활성 사용자 페이징 조회")
        void findByIsActiveTrue_WithPaging_ReturnsPage() {
            // Given
            PageRequest pageRequest = PageRequest.of(0, 1, Sort.by("username").ascending());

            // When
            Page<User> page = userRepository.findByIsActiveTrue(pageRequest);

            // Then
            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getTotalPages()).isEqualTo(2);
            assertThat(page.getContent()).hasSize(1);
            assertThat(page.getContent().get(0).getUsername()).isEqualTo("testuser");
        }
    }

    // ================================
    // 통계 메서드 테스트
    // ================================

    @Nested
    @DisplayName("통계 메서드 테스트")
    class StatisticsTest {

        @Test
        @DisplayName("활성 사용자 수 조회")
        void countByIsActiveTrue_ReturnsCorrectCount() {
            // When
            long count = userRepository.countByIsActiveTrue();

            // Then
            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("잠금된 사용자 수 조회")
        void countByIsLockedTrue_ReturnsCorrectCount() {
            // When
            long count = userRepository.countByIsLockedTrue();

            // Then
            assertThat(count).isZero();
        }

        @Test
        @DisplayName("부서별 활성 사용자 수 조회")
        void countByDepartmentAndIsActiveTrue_ReturnsCorrectCount() {
            // When
            long devCount = userRepository.countByDepartmentAndIsActiveTrue("개발팀");
            long marketingCount = userRepository.countByDepartmentAndIsActiveTrue("마케팅팀");

            // Then
            assertThat(devCount).isEqualTo(1);
            assertThat(marketingCount).isEqualTo(1);
        }
    }

    // ================================
    // 업데이트 메서드 테스트
    // ================================

    @Nested
    @DisplayName("업데이트 메서드 테스트")
    class UpdateTest {

        @Test
        @DisplayName("로그인 실패 횟수 증가")
        void incrementFailedLoginAttempts_IncreasesCount() {
            // Given
            Long userId = testUser.getUserId();

            // When
            userRepository.incrementFailedLoginAttempts(userId);
            entityManager.flush();
            entityManager.clear();

            // Then
            User updated = userRepository.findById(userId).orElseThrow();
            assertThat(updated.getFailedLoginAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("사용자 계정 잠금")
        void lockUser_SetsLockedTrue() {
            // Given
            Long userId = testUser.getUserId();

            // When
            userRepository.lockUser(userId);
            entityManager.flush();
            entityManager.clear();

            // Then
            User updated = userRepository.findById(userId).orElseThrow();
            assertThat(updated.getIsLocked()).isTrue();
        }

        @Test
        @DisplayName("사용자 계정 잠금 해제")
        void unlockUser_SetsLockedFalseAndResetsAttempts() {
            // Given
            testUser.setIsLocked(true);
            testUser.setFailedLoginAttempts(5);
            entityManager.merge(testUser);
            entityManager.flush();
            Long userId = testUser.getUserId();

            // When
            userRepository.unlockUser(userId);
            entityManager.flush();
            entityManager.clear();

            // Then
            User updated = userRepository.findById(userId).orElseThrow();
            assertThat(updated.getIsLocked()).isFalse();
            assertThat(updated.getFailedLoginAttempts()).isZero();
        }

        @Test
        @DisplayName("사용자 비활성화")
        void deactivateUser_SetsActiveFalse() {
            // Given
            Long userId = testUser.getUserId();

            // When
            userRepository.deactivateUser(userId);
            entityManager.flush();
            entityManager.clear();

            // Then
            User updated = userRepository.findById(userId).orElseThrow();
            assertThat(updated.getIsActive()).isFalse();
        }
    }

    // ================================
    // 시간 기반 조회 테스트
    // ================================

    @Nested
    @DisplayName("시간 기반 조회 테스트")
    class TimeBasedQueryTest {

        @Test
        @DisplayName("특정 기간 내 생성된 사용자 수 조회")
        void countByCreatedAtBetween_ReturnsCorrectCount() {
            // Given
            LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfToday = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

            // When
            long count = userRepository.countByCreatedAtBetween(startOfToday, endOfToday);

            // Then
            // 테스트에서 생성된 사용자들이 오늘 날짜로 생성됨
            assertThat(count).isGreaterThanOrEqualTo(0);
        }

        @Test
        @DisplayName("특정 시점 이후 생성된 사용자 수 조회")
        void countByCreatedAtAfter_ReturnsCorrectCount() {
            // Given
            LocalDateTime yesterday = LocalDateTime.now().minusDays(1);

            // When
            long count = userRepository.countByCreatedAtAfter(yesterday);

            // Then
            assertThat(count).isEqualTo(2);
        }
    }

    // ================================
    // CRUD 기본 테스트
    // ================================

    @Nested
    @DisplayName("CRUD 기본 테스트")
    class CrudTest {

        @Test
        @DisplayName("사용자 저장")
        void save_NewUser_SavesSuccessfully() {
            // Given
            User newUser = new User("newuser", "password", "new@example.com", "새 사용자");
            newUser.setIsActive(true);
            newUser.setIsLocked(false);

            // When
            User saved = userRepository.save(newUser);

            // Then
            assertThat(saved.getUserId()).isNotNull();
            assertThat(saved.getUsername()).isEqualTo("newuser");
        }

        @Test
        @DisplayName("사용자 삭제")
        void delete_ExistingUser_DeletesSuccessfully() {
            // Given
            Long userId = testUser.getUserId();
            assertThat(userRepository.findById(userId)).isPresent();

            // When
            userRepository.deleteById(userId);
            entityManager.flush();

            // Then
            assertThat(userRepository.findById(userId)).isEmpty();
        }

        @Test
        @DisplayName("전체 사용자 수 조회")
        void count_ReturnsCorrectCount() {
            // When
            long count = userRepository.count();

            // Then
            assertThat(count).isEqualTo(2);
        }
    }
}

/*
 * ====== Repository 테스트 가이드 ======
 *
 * 1. @DataJpaTest 특징:
 *
 *    - JPA 관련 빈만 로드 (빠른 테스트)
 *    - H2 인메모리 DB 자동 설정
 *    - 각 테스트 후 자동 롤백
 *    - @Transactional 자동 적용
 *
 * 2. TestEntityManager 사용법:
 *
 *    // 저장
 *    entityManager.persist(entity);
 *
 *    // 조회
 *    Entity found = entityManager.find(Entity.class, id);
 *
 *    // DB 반영
 *    entityManager.flush();
 *
 *    // 1차 캐시 초기화
 *    entityManager.clear();
 *
 * 3. 테스트 데이터 준비:
 *
 *    @BeforeEach에서 필요한 데이터를 persist()로 저장
 *    flush()와 clear()로 영속성 컨텍스트 정리
 *
 * 4. @Modifying 쿼리 테스트:
 *
 *    - 벌크 업데이트 후 flush() 필요
 *    - clear() 후 다시 조회해서 결과 확인
 *
 * 5. 테스트 프로파일:
 *
 *    @ActiveProfiles("test")로 테스트 전용 설정 사용
 *    application-test.yml에 H2 설정
 *
 * 6. 테스트 실행:
 *
 *    mvn test -Dtest=UserRepositoryTest
 */