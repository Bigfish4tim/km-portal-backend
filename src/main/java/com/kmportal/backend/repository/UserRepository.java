package com.kmportal.backend.repository;

import com.kmportal.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 사용자 데이터 액세스 Repository
 *
 * Spring Data JPA의 JpaRepository를 상속받아 기본적인 CRUD 기능과
 * 사용자 도메인에 특화된 쿼리 메서드들을 제공합니다.
 *
 * JpaRepository<User, Long>:
 * - User: 관리할 엔티티 타입
 * - Long: 엔티티의 ID 타입 (@Id 필드 타입)
 *
 * 기본 제공 메서드:
 * - save(User): 사용자 저장/수정
 * - findById(Long): ID로 사용자 조회
 * - findAll(): 모든 사용자 조회
 * - deleteById(Long): ID로 사용자 삭제
 * - count(): 전체 사용자 수
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-09-24
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // ================================
    // 기본 조회 메서드
    // ================================

    /**
     * 사용자명으로 사용자 조회 (로그인 시 사용)
     * Spring Security에서 인증 시 주로 사용되는 메서드
     *
     * 메서드명 규칙: findBy + 필드명 + Optional
     * - findBy: 조회를 의미
     * - Username: User 엔티티의 username 필드
     * - Optional: 결과가 없을 수 있음을 명시
     *
     * @param username 로그인 아이디
     * @return 사용자 정보 (Optional로 감싼 결과)
     */
    Optional<User> findByUsername(String username);

    /**
     * 이메일로 사용자 조회
     * 회원가입 시 이메일 중복 체크나 비밀번호 찾기 등에 사용
     *
     * @param email 이메일 주소
     * @return 사용자 정보 (Optional로 감싼 결과)
     */
    Optional<User> findByEmail(String email);

    /**
     * 사용자명 존재 여부 확인
     * 회원가입 시 사용자명 중복 체크에 사용
     *
     * 메서드명 규칙: existsBy + 필드명
     * - existsBy: 존재 여부 확인
     * - Username: 확인할 필드
     * - boolean 반환: 존재하면 true, 없으면 false
     *
     * @param username 확인할 사용자명
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByUsername(String username);

    /**
     * 이메일 존재 여부 확인
     * 회원가입 시 이메일 중복 체크에 사용
     *
     * @param email 확인할 이메일
     * @return 존재하면 true, 없으면 false
     */
    boolean existsByEmail(String email);

    // ================================
    // 상태별 조회 메서드
    // ================================

    /**
     * 활성 사용자 목록 조회
     * 활성화된 사용자만 조회 (isActive = true)
     *
     * 메서드명 규칙: findBy + 필드명 + 조건
     * - IsActiveTrue: isActive가 true인 조건
     *
     * @return 활성 사용자 목록
     */
    List<User> findByIsActiveTrue();

    /**
     * 비활성 사용자 목록 조회
     * 비활성화된 사용자만 조회 (isActive = false)
     *
     * @return 비활성 사용자 목록
     */
    List<User> findByIsActiveFalse();

    /**
     * 잠금된 사용자 목록 조회
     * 보안상 잠금된 계정들을 관리하기 위해 사용
     *
     * @return 잠금된 사용자 목록
     */
    List<User> findByIsLockedTrue();

    /**
     * 활성 사용자 목록 조회 (페이징 지원)
     * 사용자가 많을 때 페이징 처리를 위해 사용
     *
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 페이징된 활성 사용자 목록
     */
    Page<User> findByIsActiveTrue(Pageable pageable);

    // ================================
    // 부서/직책별 조회 메서드
    // ================================

    /**
     * 부서별 사용자 조회
     * 조직도나 부서별 통계에 사용
     *
     * @param department 부서명
     * @return 해당 부서 사용자 목록
     */
    List<User> findByDepartment(String department);

    /**
     * 부서별 활성 사용자 조회
     * 부서별 활성 사용자만 필터링
     *
     * 메서드명에서 And로 조건 연결:
     * - Department: 부서 조건
     * - And: 그리고
     * - IsActiveTrue: 활성 상태 조건
     *
     * @param department 부서명
     * @return 해당 부서의 활성 사용자 목록
     */
    List<User> findByDepartmentAndIsActiveTrue(String department);

    /**
     * 직책별 사용자 조회
     *
     * @param position 직책
     * @return 해당 직책 사용자 목록
     */
    List<User> findByPosition(String position);

    // ================================
    // 검색 메서드 (Like 조건 사용)
    // ================================

    /**
     * 실명으로 사용자 검색 (부분 일치)
     * 사용자 관리 화면에서 이름으로 검색할 때 사용
     *
     * 메서드명 규칙: findBy + 필드명 + Containing + IgnoreCase
     * - Containing: LIKE '%값%' 조건 (부분 일치)
     * - IgnoreCase: 대소문자 구분 안함
     *
     * @param fullName 검색할 이름 (부분 일치)
     * @return 이름에 해당 문자가 포함된 사용자 목록
     */
    List<User> findByFullNameContainingIgnoreCase(String fullName);

    /**
     * 이메일로 사용자 검색 (부분 일치)
     *
     * @param email 검색할 이메일 (부분 일치)
     * @return 이메일에 해당 문자가 포함된 사용자 목록
     */
    List<User> findByEmailContainingIgnoreCase(String email);

    /**
     * 다중 필드 검색 (이름 또는 이메일)
     * 통합 검색 기능에서 사용
     *
     * Or 조건으로 여러 필드 검색
     *
     * @param fullName 검색할 이름
     * @param email 검색할 이메일
     * @return 이름 또는 이메일이 일치하는 사용자 목록
     */
    List<User> findByFullNameContainingIgnoreCaseOrEmailContainingIgnoreCase(
            String fullName, String email);

    // ================================
    // 시간 기반 조회 메서드
    // ================================

    /**
     * 특정 기간 이후 생성된 사용자 조회
     * 신규 가입자 통계나 최근 가입자 조회에 사용
     *
     * 메서드명 규칙: findBy + 필드명 + After
     * - After: 특정 시점 이후 조건 (> 연산)
     *
     * @param createdAt 기준 시간
     * @return 해당 시간 이후 생성된 사용자 목록
     */
    List<User> findByCreatedAtAfter(LocalDateTime createdAt);

    /**
     * 특정 기간 이후 마지막 로그인한 사용자 조회
     * 최근 활동 사용자 분석에 사용
     *
     * @param lastLoginAt 기준 시간
     * @return 해당 시간 이후 로그인한 사용자 목록
     */
    List<User> findByLastLoginAtAfter(LocalDateTime lastLoginAt);

    /**
     * 특정 기간 이전에 마지막 로그인한 사용자 조회
     * 비활성 사용자나 휴면 계정 관리에 사용
     *
     * 메서드명 규칙: findBy + 필드명 + Before
     * - Before: 특정 시점 이전 조건 (< 연산)
     *
     * @param lastLoginAt 기준 시간
     * @return 해당 시간 이전에 로그인한 사용자 목록
     */
    List<User> findByLastLoginAtBefore(LocalDateTime lastLoginAt);

    // ================================
    // 커스텀 쿼리 메서드 (@Query 사용)
    // ================================

    /**
     * 특정 역할을 가진 사용자 조회
     * @Query 어노테이션으로 JPQL 쿼리 직접 작성
     *
     * JPQL (Java Persistence Query Language):
     * - 엔티티 기반 쿼리 언어 (테이블이 아닌 객체 기준)
     * - u: User 엔티티의 별칭
     * - JOIN: 연관 관계 조인
     * - r: Role 엔티티의 별칭
     * - :roleName: 파라미터 바인딩
     *
     * @param roleName 역할명 (예: "ROLE_ADMIN")
     * @return 해당 역할을 가진 사용자 목록
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * 여러 역할 중 하나라도 가진 사용자 조회
     * IN 조건으로 여러 역할 중 하나만 만족하면 조회
     *
     * @param roleNames 역할명 목록
     * @return 해당 역할들 중 하나라도 가진 사용자 목록
     */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.roleName IN :roleNames")
    List<User> findByRoleNames(@Param("roleNames") List<String> roleNames);

    /**
     * 부서별 활성 사용자 수 조회
     * 통계나 대시보드에서 사용
     *
     * COUNT(): 개수 집계 함수
     * GROUP BY: 부서별로 그룹화
     *
     * @return 부서별 사용자 수 (부서명, 사용자 수)
     */
    @Query("SELECT u.department, COUNT(u) FROM User u WHERE u.isActive = true " +
            "GROUP BY u.department ORDER BY COUNT(u) DESC")
    List<Object[]> findActiveUserCountByDepartment();

    /**
     * 로그인 실패 횟수가 특정 값 이상인 사용자 조회
     * 보안 관리를 위한 쿼리
     *
     * 메서드명 규칙: findBy + 필드명 + GreaterThanEqual
     * - GreaterThanEqual: >= 연산 (이상)
     *
     * @param attempts 기준 실패 횟수
     * @return 실패 횟수가 기준 이상인 사용자 목록
     */
    List<User> findByFailedLoginAttemptsGreaterThanEqual(Integer attempts);

    // ================================
    // 업데이트 메서드 (@Modifying 사용)
    // ================================

    /**
     * 사용자의 마지막 로그인 시간 업데이트
     * @Modifying: UPDATE, DELETE 쿼리임을 명시
     * @Query: 커스텀 UPDATE 쿼리 작성
     *
     * 주의사항:
     * - @Modifying 어노테이션 필수
     * - 트랜잭션 내에서만 실행 가능
     * - 영속성 컨텍스트와 동기화 이슈 주의
     *
     * @param userId 사용자 ID
     * @param lastLoginAt 마지막 로그인 시간
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt, u.failedLoginAttempts = 0 " +
            "WHERE u.userId = :userId")
    int updateLastLoginAt(@Param("userId") Long userId,
                          @Param("lastLoginAt") LocalDateTime lastLoginAt);

    /**
     * 로그인 실패 횟수 증가
     * 로그인 실패 시 호출되는 메서드
     *
     * @param userId 사용자 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE User u SET u.failedLoginAttempts = u.failedLoginAttempts + 1 " +
            "WHERE u.userId = :userId")
    int incrementFailedLoginAttempts(@Param("userId") Long userId);

    /**
     * 사용자 계정 잠금
     * 보안 정책에 따른 계정 잠금 처리
     *
     * @param userId 사용자 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE User u SET u.isLocked = true WHERE u.userId = :userId")
    int lockUser(@Param("userId") Long userId);

    /**
     * 사용자 계정 잠금 해제
     *
     * @param userId 사용자 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE User u SET u.isLocked = false, u.failedLoginAttempts = 0 " +
            "WHERE u.userId = :userId")
    int unlockUser(@Param("userId") Long userId);

    /**
     * 사용자 비활성화
     * 계정 삭제 대신 비활성화 처리 (소프트 삭제)
     *
     * @param userId 사용자 ID
     * @return 업데이트된 레코드 수
     */
    @Modifying
    @Query("UPDATE User u SET u.isActive = false WHERE u.userId = :userId")
    int deactivateUser(@Param("userId") Long userId);

    // ================================
    // 통계 및 집계 메서드
    // ================================

    /**
     * 활성 사용자 수 조회
     *
     * @return 활성 사용자 수
     */
    long countByIsActiveTrue();

    /**
     * 잠금된 사용자 수 조회
     *
     * @return 잠금된 사용자 수
     */
    long countByIsLockedTrue();

    /**
     * 특정 기간 내 신규 가입자 수
     *
     * 메서드명 규칙: countBy + 필드명 + Between
     * - Between: 특정 범위 내 조건
     *
     * @param startDate 시작 날짜
     * @param endDate 종료 날짜
     * @return 해당 기간 내 가입자 수
     */
    long countByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 부서별 사용자 수 조회 (활성 사용자만)
     *
     * @param department 부서명
     * @return 해당 부서의 활성 사용자 수
     */
    long countByDepartmentAndIsActiveTrue(String department);
}