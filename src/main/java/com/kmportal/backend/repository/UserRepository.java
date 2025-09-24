// ==============================================
// 📁 UserRepository.java
// 사용자 데이터 액세스 레이어
// ==============================================

package com.kmportal.backend.repository;

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
 * 사용자 레포지토리
 * - Spring Data JPA를 활용한 사용자 데이터 액세스
 * - 기본 CRUD 작업 + 커스텀 쿼리 메서드
 * - 약 400명의 사용자 데이터 처리 최적화
 *
 * JpaRepository<T, ID>:
 * - T: 엔티티 클래스 (User)
 * - ID: Primary Key 타입 (Long)
 *
 * @author KM Portal Team
 * @since 2025-09-23 (3일차)
 */
@Repository  // Spring에서 데이터 액세스 계층임을 명시
public interface UserRepository extends JpaRepository<User, Long> {

    // ========================================
    // 기본 조회 메서드
    // ========================================

    /**
     * 사용자명으로 사용자 조회
     * - 로그인 시 사용자 인증에 필요
     * - Spring Security에서 사용
     *
     * @param username 사용자명
     * @return 사용자 정보 (Optional로 null 안전성 보장)
     */
    Optional<User> findByUsername(String username);

    /**
     * 이메일로 사용자 조회
     * - 이메일 중복 확인 및 찾기 기능에 사용
     *
     * @param email 이메일 주소
     * @return 사용자 정보
     */
    Optional<User> findByEmail(String email);

    /**
     * 사용자명과 이메일로 사용자 조회
     * - 비밀번호 재설정 시 본인 확인용
     *
     * @param username 사용자명
     * @param email 이메일 주소
     * @return 사용자 정보
     */
    Optional<User> findByUsernameAndEmail(String username, String email);

    // ========================================
    // 상태별 조회 메서드
    // ========================================

    /**
     * 활성화된 사용자 목록 조회
     * - 관리자 화면에서 활성 사용자만 표시할 때 사용
     * - 페이징 지원으로 대용량 데이터 처리
     *
     * @param pageable 페이징 정보 (페이지 번호, 크기, 정렬)
     * @return 활성화된 사용자 페이지
     */
    Page<User> findByIsActiveTrue(Pageable pageable);

    /**
     * 잠긴 계정 목록 조회
     * - 관리자가 잠긴 계정을 관리할 때 사용
     *
     * @return 잠긴 계정 목록
     */
    List<User> findByIsLockedTrue();

    /**
     * 활성화되고 잠기지 않은 사용자 목록
     * - 정상적으로 로그인 가능한 사용자만 조회
     *
     * @param pageable 페이징 정보
     * @return 정상 사용자 페이지
     */
    Page<User> findByIsActiveTrueAndIsLockedFalse(Pageable pageable);

    // ========================================
    // 부서별 조회 메서드
    // ========================================

    /**
     * 부서별 사용자 조회
     * - 부서 관리자가 소속 직원을 관리할 때 사용
     *
     * @param department 부서명
     * @param pageable 페이징 정보
     * @return 해당 부서 사용자 페이지
     */
    Page<User> findByDepartment(String department, Pageable pageable);

    /**
     * 부서명에 특정 키워드가 포함된 사용자 조회
     * - 부서 검색 기능에 사용
     *
     * @param keyword 검색 키워드
     * @return 사용자 목록
     */
    List<User> findByDepartmentContainingIgnoreCase(String keyword);

    // ========================================
    // 검색 메서드
    // ========================================

    /**
     * 사용자명에 키워드가 포함된 사용자 검색
     * - 관리자 화면에서 사용자 검색 기능
     * - 대소문자 구분 없이 검색
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 사용자 페이지
     */
    Page<User> findByUsernameContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * 실명에 키워드가 포함된 사용자 검색
     * - 한글 이름으로 사용자 찾기
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 사용자 페이지
     */
    Page<User> findByFullNameContainingIgnoreCase(String keyword, Pageable pageable);

    // ========================================
    // 커스텀 쿼리 메서드 (JPQL 사용)
    // ========================================

    /**
     * 통합 검색 쿼리
     * - 사용자명, 실명, 이메일, 부서에서 키워드 검색
     * - 관리자 화면의 통합 검색 기능에 사용
     *
     * @param keyword 검색 키워드
     * @param pageable 페이징 정보
     * @return 검색된 사용자 페이지
     */
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(u.department) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 특정 역할을 가진 사용자 조회
     * - 권한별 사용자 관리에 사용
     * - 예: 관리자 역할을 가진 모든 사용자
     *
     * @param roleName 역할명 (예: "ROLE_ADMIN")
     * @return 해당 역할을 가진 사용자 목록
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName")
    List<User> findByRoleName(@Param("roleName") String roleName);

    /**
     * 활성화된 사용자 중 특정 역할을 가진 사용자 조회
     * - 활성 상태이면서 특정 권한을 가진 사용자만 조회
     *
     * @param roleName 역할명
     * @param pageable 페이징 정보
     * @return 해당 조건의 사용자 페이지
     */
    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.roleName = :roleName AND u.isActive = true")
    Page<User> findActiveUsersByRoleName(@Param("roleName") String roleName, Pageable pageable);

    // ========================================
    // 통계 쿼리 메서드
    // ========================================

    /**
     * 활성화된 사용자 수 조회
     * - 대시보드 통계용
     *
     * @return 활성 사용자 수
     */
    long countByIsActiveTrue();

    /**
     * 잠긴 계정 수 조회
     * - 보안 현황 파악용
     *
     * @return 잠긴 계정 수
     */
    long countByIsLockedTrue();

    /**
     * 부서별 사용자 수 조회
     * - 부서별 통계 정보 제공
     *
     * @param department 부서명
     * @return 해당 부서 사용자 수
     */
    long countByDepartment(String department);

    /**
     * 특정 기간 내 로그인한 사용자 수
     * - 활동 통계 분석용
     *
     * @param startDate 시작 일시
     * @param endDate 종료 일시
     * @return 기간 내 로그인 사용자 수
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt BETWEEN :startDate AND :endDate")
    long countActiveUsersBetween(@Param("startDate") LocalDateTime startDate,
                                 @Param("endDate") LocalDateTime endDate);

    // ========================================
    // 로그인 관련 메서드
    // ========================================

    /**
     * 마지막 로그인이 특정 기간 이전인 사용자 조회
     * - 휴면 계정 관리용
     *
     * @param date 기준 날짜 (이 날짜 이전에 마지막 로그인)
     * @return 휴면 계정 후보 사용자 목록
     */
    List<User> findByLastLoginAtBefore(LocalDateTime date);

    /**
     * 로그인 실패 횟수가 특정 값 이상인 사용자 조회
     * - 보안 위험 계정 모니터링용
     *
     * @param count 실패 횟수 기준
     * @return 해당 조건의 사용자 목록
     */
    List<User> findByFailedLoginAttemptsGreaterThanEqual(Integer count);

    // ========================================
    // 중복 확인 메서드
    // ========================================

    /**
     * 사용자명 중복 확인
     * - 회원가입 시 사용자명 중복 검사
     *
     * @param username 확인할 사용자명
     * @return 중복 여부 (true: 중복됨)
     */
    boolean existsByUsername(String username);

    /**
     * 이메일 중복 확인
     * - 회원가입 시 이메일 중복 검사
     *
     * @param email 확인할 이메일
     * @return 중복 여부 (true: 중복됨)
     */
    boolean existsByEmail(String email);

    /**
     * 사용자 업데이트 시 중복 확인 (자신 제외)
     * - 현재 사용자를 제외하고 사용자명 중복 검사
     *
     * @param username 확인할 사용자명
     * @param userId 제외할 사용자 ID (현재 사용자)
     * @return 중복 여부
     */
    boolean existsByUsernameAndUserIdNot(String username, Long userId);

    /**
     * 사용자 업데이트 시 이메일 중복 확인 (자신 제외)
     *
     * @param email 확인할 이메일
     * @param userId 제외할 사용자 ID
     * @return 중복 여부
     */
    boolean existsByEmailAndUserIdNot(String email, Long userId);
}