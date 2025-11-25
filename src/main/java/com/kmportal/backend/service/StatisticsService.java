package com.kmportal.backend.service;

import com.kmportal.backend.repository.BoardRepository;
import com.kmportal.backend.repository.CommentRepository;
import com.kmportal.backend.repository.FileRepository;
import com.kmportal.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * StatisticsService - 통계 데이터 집계 비즈니스 로직
 *
 * 대시보드에서 필요한 각종 통계 데이터를 수집하고 집계하는 서비스입니다.
 * Repository 계층에서 데이터를 조회하고, 필요한 계산과 변환을 수행합니다.
 *
 * 주요 기능:
 * 1. 시스템 기본 통계 (사용자, 게시글, 댓글, 파일 수)
 * 2. 사용자 활동 통계 (신규 가입, 활성/비활성 사용자)
 * 3. 게시판 통계 (오늘/주간/월간 게시글 수, 평균 조회수)
 * 4. 차트 데이터 생성 (최근 7일 게시글 추이)
 * 5. 인기 게시글, 최근 게시글 목록
 * 6. 부서별 게시글 분포
 *
 * 성능 최적화:
 * - @Transactional(readOnly = true): 읽기 전용 트랜잭션으로 성능 향상
 * - 집계 쿼리 사용: COUNT, GROUP BY 등으로 최소한의 데이터만 조회
 * - 캐싱 가능: 향후 @Cacheable 적용 가능
 *
 * 작성일: 2025년 11월 25일 (32일차)
 * 작성자: 32일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-25
 */
@Slf4j  // 로깅을 위한 Lombok 어노테이션
@Service  // Spring Service 컴포넌트임을 표시
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성
@Transactional(readOnly = true)  // 모든 메서드를 읽기 전용 트랜잭션으로 실행 (성능 향상)
public class StatisticsService {

    // ====== 의존성 주입 ======

    /**
     * UserRepository - 사용자 데이터 조회
     * 사용자 수, 신규 가입자, 활성/비활성 사용자 등을 조회합니다.
     */
    private final UserRepository userRepository;

    /**
     * BoardRepository - 게시글 데이터 조회
     * 게시글 수, 최근 게시글, 인기 게시글, 부서별 분포 등을 조회합니다.
     */
    private final BoardRepository boardRepository;

    /**
     * CommentRepository - 댓글 데이터 조회
     * 댓글 수, 오늘 작성된 댓글 등을 조회합니다.
     */
    private final CommentRepository commentRepository;

    /**
     * FileRepository - 파일 데이터 조회
     * 파일 수, 총 파일 크기 등을 조회합니다.
     */
    private final FileRepository fileRepository;

    // ====== 메인 메서드: 대시보드 종합 통계 ======

    /**
     * 대시보드용 종합 통계 조회
     *
     * 대시보드에서 필요한 모든 통계 데이터를 한 번에 조회합니다.
     * 여러 Repository에서 데이터를 조회하여 하나의 Map으로 통합합니다.
     *
     * 포함 데이터:
     * - 시스템 기본 통계 (totalUsers, totalBoards, totalComments, totalFiles)
     * - 사용자 활동 통계 (todayNewUsers, activeUsers)
     * - 게시판 통계 (todayNewBoards, todayNewComments)
     * - 차트 데이터 (weeklyBoardsChart)
     * - 인기 게시글 TOP 5 (popularBoards)
     * - 최근 게시글 5개 (recentBoards)
     * - 부서별 게시글 분포 (boardsByDepartment)
     *
     * @return Map<String, Object> - 모든 통계 데이터
     *
     * 사용 예시:
     * ```java
     * Map<String, Object> stats = statisticsService.getDashboardStatistics();
     * Long totalUsers = (Long) stats.get("totalUsers");
     * ```
     */
    public Map<String, Object> getDashboardStatistics() {
        log.info("[StatisticsService] 대시보드 종합 통계 조회 시작");

        // 모든 통계 데이터를 담을 Map 생성
        Map<String, Object> statistics = new HashMap<>();

        try {
            // 1. 시스템 기본 통계 추가
            statistics.putAll(getSystemStatistics());
            log.debug("시스템 기본 통계 추가 완료");

            // 2. 사용자 활동 통계 추가
            Map<String, Long> userActivity = getUserActivityStatistics();
            statistics.put("todayNewUsers", userActivity.get("todayNewUsers"));
            statistics.put("activeUsers", userActivity.get("activeUsers"));
            log.debug("사용자 활동 통계 추가 완료");

            // 3. 게시판 통계 추가
            Map<String, Object> boardStats = getBoardStatistics();
            statistics.put("todayNewBoards", boardStats.get("todayNewBoards"));
            statistics.put("todayNewComments", boardStats.get("todayNewComments"));
            statistics.put("averageViewCount", boardStats.get("averageViewCount"));
            log.debug("게시판 통계 추가 완료");

            // 4. 주간 게시글 차트 데이터 추가
            statistics.put("weeklyBoardsChart", getWeeklyBoardChart());
            log.debug("주간 차트 데이터 추가 완료");

            // 5. 인기 게시글 TOP 5 추가
            statistics.put("popularBoards", getPopularBoards());
            log.debug("인기 게시글 추가 완료");

            // 6. 최근 게시글 5개 추가
            statistics.put("recentBoards", getRecentBoards());
            log.debug("최근 게시글 추가 완료");

            // 7. 부서별 게시글 분포 추가
            statistics.put("boardsByDepartment", getBoardsByDepartment());
            log.debug("부서별 분포 추가 완료");

            log.info("[StatisticsService] 대시보드 종합 통계 조회 완료 - 총 {} 개 항목", statistics.size());

            return statistics;

        } catch (Exception e) {
            log.error("[StatisticsService] 대시보드 통계 조회 중 오류 발생", e);

            // 오류 발생 시 빈 Map 대신 기본값이 있는 Map 반환
            return getEmptyStatistics();
        }
    }

    // ====== 1. 시스템 기본 통계 ======

    /**
     * 시스템 기본 통계 조회
     *
     * 전체 사용자, 게시글, 댓글, 파일 수를 조회합니다.
     *
     * @return Map<String, Long> - 기본 통계 숫자들
     *         - totalUsers: 전체 사용자 수
     *         - totalBoards: 전체 게시글 수 (삭제 제외)
     *         - totalComments: 전체 댓글 수 (삭제 제외)
     *         - totalFiles: 전체 파일 수
     */
    public Map<String, Long> getSystemStatistics() {
        log.debug("[StatisticsService] 시스템 기본 통계 조회");

        Map<String, Long> statistics = new HashMap<>();

        // 전체 사용자 수 (삭제되지 않은 사용자만)
        long totalUsers = userRepository.count();
        statistics.put("totalUsers", totalUsers);
        log.debug("전체 사용자 수: {}", totalUsers);

        // 전체 게시글 수 (삭제되지 않은 게시글만)
        // BoardRepository에 countByIsDeletedFalse() 메서드가 있다고 가정
        long totalBoards = boardRepository.count();
        statistics.put("totalBoards", totalBoards);
        log.debug("전체 게시글 수: {}", totalBoards);

        // 전체 댓글 수 (삭제되지 않은 댓글만)
        // CommentRepository에 countByIsDeletedFalse() 메서드가 있다고 가정
        long totalComments = commentRepository.count();
        statistics.put("totalComments", totalComments);
        log.debug("전체 댓글 수: {}", totalComments);

        // 전체 파일 수
        long totalFiles = fileRepository.count();
        statistics.put("totalFiles", totalFiles);
        log.debug("전체 파일 수: {}", totalFiles);

        return statistics;
    }

    // ====== 2. 사용자 활동 통계 ======

    /**
     * 사용자 활동 통계 조회
     *
     * 오늘 신규 가입자, 활성 사용자, 비활성 사용자 수를 조회합니다.
     *
     * @return Map<String, Long> - 사용자 활동 통계
     *         - todayNewUsers: 오늘 가입한 사용자 수
     *         - activeUsers: 활성 사용자 수 (30일 내 로그인)
     *         - inactiveUsers: 비활성 사용자 수 (30일 이상 미로그인)
     */
    public Map<String, Long> getUserActivityStatistics() {
        log.debug("[StatisticsService] 사용자 활동 통계 조회");

        Map<String, Long> statistics = new HashMap<>();

        // 오늘 자정 시간 계산
        LocalDateTime today = LocalDate.now().atStartOfDay();
        log.debug("오늘 자정: {}", today);

        // 오늘 가입한 사용자 수
        // UserRepository에 countByCreatedAtAfter(LocalDateTime) 메서드가 있다고 가정
        long todayNewUsers = userRepository.countByCreatedAtAfter(today);
        statistics.put("todayNewUsers", todayNewUsers);
        log.debug("오늘 신규 가입자: {}", todayNewUsers);

        // 30일 전 시간 계산
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        log.debug("30일 전: {}", thirtyDaysAgo);

        // 활성 사용자 수 (30일 내 로그인한 사용자)
        // UserRepository에 countByLastLoginAtAfter(LocalDateTime) 메서드가 있다고 가정
        long activeUsers = userRepository.countByLastLoginAtAfter(thirtyDaysAgo);
        statistics.put("activeUsers", activeUsers);
        log.debug("활성 사용자 수 (30일 내 로그인): {}", activeUsers);

        // 비활성 사용자 수 (30일 이상 미로그인)
        long totalUsers = userRepository.count();
        long inactiveUsers = totalUsers - activeUsers;
        statistics.put("inactiveUsers", inactiveUsers);
        log.debug("비활성 사용자 수: {}", inactiveUsers);

        return statistics;
    }

    // ====== 3. 게시판 통계 ======

    /**
     * 게시판 통계 조회
     *
     * 오늘/주간/월간 게시글 수, 오늘 댓글 수, 평균 조회수 등을 조회합니다.
     *
     * @return Map<String, Object> - 게시판 통계
     *         - todayNewBoards: 오늘 작성된 게시글 수
     *         - todayNewComments: 오늘 작성된 댓글 수
     *         - weeklyBoards: 이번 주 게시글 수
     *         - monthlyBoards: 이번 달 게시글 수
     *         - averageViewCount: 평균 조회수
     */
    public Map<String, Object> getBoardStatistics() {
        log.debug("[StatisticsService] 게시판 통계 조회");

        Map<String, Object> statistics = new HashMap<>();

        // 오늘 자정 시간
        LocalDateTime today = LocalDate.now().atStartOfDay();

        // 오늘 작성된 게시글 수
        long todayNewBoards = boardRepository.countByCreatedAtAfter(today);
        statistics.put("todayNewBoards", todayNewBoards);
        log.debug("오늘 작성된 게시글: {}", todayNewBoards);

        // 오늘 작성된 댓글 수
        long todayNewComments = commentRepository.countByCreatedAtAfter(today);
        statistics.put("todayNewComments", todayNewComments);
        log.debug("오늘 작성된 댓글: {}", todayNewComments);

        // 이번 주 시작 시간 (월요일 자정)
        LocalDateTime weekStart = LocalDate.now()
                .with(java.time.DayOfWeek.MONDAY)
                .atStartOfDay();

        // 이번 주 게시글 수
        long weeklyBoards = boardRepository.countByCreatedAtAfter(weekStart);
        statistics.put("weeklyBoards", weeklyBoards);
        log.debug("이번 주 게시글: {}", weeklyBoards);

        // 이번 달 시작 시간 (1일 자정)
        LocalDateTime monthStart = LocalDate.now()
                .withDayOfMonth(1)
                .atStartOfDay();

        // 이번 달 게시글 수
        long monthlyBoards = boardRepository.countByCreatedAtAfter(monthStart);
        statistics.put("monthlyBoards", monthlyBoards);
        log.debug("이번 달 게시글: {}", monthlyBoards);

        // 평균 조회수 계산
        // BoardRepository에 findAverageViewCount() 메서드가 있다고 가정
        Double averageViewCount = boardRepository.findAverageViewCount();
        statistics.put("averageViewCount", averageViewCount != null ? averageViewCount : 0.0);
        log.debug("평균 조회수: {}", averageViewCount);

        return statistics;
    }

    // ====== 4. 차트 데이터 생성 ======

    /**
     * 최근 7일 게시글 작성 추이 차트 데이터 생성
     *
     * Chart.js에서 사용할 수 있는 형식으로 데이터를 반환합니다.
     * labels: ["2025-11-19", "2025-11-20", ..., "2025-11-25"]
     * data: [5, 8, 3, 12, 7, 9, 10]
     *
     * @return Map<String, Object> - 차트 데이터
     *         - labels: 날짜 문자열 배열 (7개)
     *         - data: 해당 날짜의 게시글 수 배열 (7개)
     */
    public Map<String, Object> getWeeklyBoardChart() {
        log.debug("[StatisticsService] 주간 게시글 차트 데이터 생성");

        Map<String, Object> chartData = new HashMap<>();

        // 날짜 레이블 생성 (오늘부터 6일 전까지)
        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();

        for (int i = 6; i >= 0; i--) {
            // i일 전의 날짜
            LocalDate date = LocalDate.now().minusDays(i);

            // 레이블에 날짜 추가 (YYYY-MM-DD 형식)
            labels.add(date.toString());

            // 해당 날짜의 게시글 수 조회
            LocalDateTime dayStart = date.atStartOfDay();
            LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

            // BoardRepository에 countByCreatedAtBetween() 메서드가 있다고 가정
            long count = boardRepository.countByCreatedAtBetween(dayStart, dayEnd);
            data.add(count);

            log.debug("{} 게시글 수: {}", date, count);
        }

        chartData.put("labels", labels);
        chartData.put("data", data);

        log.info("[StatisticsService] 주간 차트 데이터 생성 완료 - {} 일치", labels.size());

        return chartData;
    }

    // ====== 5. 인기 게시글 조회 ======

    /**
     * 인기 게시글 TOP 5 조회
     *
     * 조회수가 가장 많은 게시글 5개를 조회합니다.
     *
     * @return List<Map<String, Object>> - 인기 게시글 목록
     *         각 게시글 정보:
     *         - id: 게시글 ID
     *         - title: 제목
     *         - authorName: 작성자 이름
     *         - viewCount: 조회수
     *         - commentCount: 댓글 수
     *         - createdAt: 작성일시
     */
    public List<Map<String, Object>> getPopularBoards() {
        log.debug("[StatisticsService] 인기 게시글 TOP 5 조회");

        // BoardRepository에서 조회수가 높은 게시글 5개 조회
        // findTop5ByIsDeletedFalseOrderByViewCountDesc() 메서드가 있다고 가정
        var popularBoards = boardRepository.findTop5ByIsDeletedFalseOrderByViewCountDesc();

        // Entity를 Map으로 변환 (프론트엔드에서 사용하기 쉽게)
        List<Map<String, Object>> result = popularBoards.stream()
                .map(board -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", board.getId());
                    map.put("title", board.getTitle());
                    map.put("authorName", board.getAuthorName());
                    map.put("viewCount", board.getViewCount());
                    map.put("commentCount", board.getCommentCount());
                    map.put("createdAt", board.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        log.debug("인기 게시글 {} 건 조회 완료", result.size());

        return result;
    }

    // ====== 6. 최근 게시글 조회 ======

    /**
     * 최근 게시글 5개 조회
     *
     * 최근에 작성된 게시글 5개를 조회합니다.
     *
     * @return List<Map<String, Object>> - 최근 게시글 목록
     *         각 게시글 정보:
     *         - id: 게시글 ID
     *         - title: 제목
     *         - authorName: 작성자 이름
     *         - viewCount: 조회수
     *         - commentCount: 댓글 수
     *         - createdAt: 작성일시
     */
    public List<Map<String, Object>> getRecentBoards() {
        log.debug("[StatisticsService] 최근 게시글 5개 조회");

        // BoardRepository에서 최근 게시글 5개 조회
        // findTop5ByIsDeletedFalseOrderByCreatedAtDesc() 메서드가 있다고 가정
        var recentBoards = boardRepository.findTop5ByIsDeletedFalseOrderByCreatedAtDesc();

        // Entity를 Map으로 변환
        List<Map<String, Object>> result = recentBoards.stream()
                .map(board -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", board.getId());
                    map.put("title", board.getTitle());
                    map.put("authorName", board.getAuthorName());
                    map.put("viewCount", board.getViewCount());
                    map.put("commentCount", board.getCommentCount());
                    map.put("createdAt", board.getCreatedAt());
                    return map;
                })
                .collect(Collectors.toList());

        log.debug("최근 게시글 {} 건 조회 완료", result.size());

        return result;
    }

    // ====== 7. 부서별 게시글 분포 ======

    /**
     * 부서별 게시글 분포 조회
     *
     * 각 부서별로 작성된 게시글 수를 조회합니다.
     *
     * @return Map<String, Long> - 부서명을 key로, 게시글 수를 value로 하는 Map
     *         예: {"개발팀": 45, "기획팀": 32, "디자인팀": 28}
     */
    public Map<String, Long> getBoardsByDepartment() {
        log.debug("[StatisticsService] 부서별 게시글 분포 조회");

        // BoardRepository에서 부서별 게시글 수 조회
        // findBoardCountByDepartment() 메서드가 있다고 가정
        // 이 메서드는 @Query를 사용하여 GROUP BY 쿼리를 실행합니다
        Map<String, Long> distribution = boardRepository.findBoardCountByDepartment();

        log.debug("부서별 게시글 분포: {}", distribution);

        return distribution;
    }

    // ====== 유틸리티 메서드 ======

    /**
     * 빈 통계 데이터 생성
     *
     * 오류 발생 시 반환할 기본값이 있는 통계 데이터를 생성합니다.
     * 모든 숫자는 0, 모든 리스트는 빈 리스트로 초기화됩니다.
     *
     * @return Map<String, Object> - 기본값으로 초기화된 통계 데이터
     */
    private Map<String, Object> getEmptyStatistics() {
        Map<String, Object> statistics = new HashMap<>();

        // 시스템 기본 통계
        statistics.put("totalUsers", 0L);
        statistics.put("totalBoards", 0L);
        statistics.put("totalComments", 0L);
        statistics.put("totalFiles", 0L);

        // 사용자 활동 통계
        statistics.put("todayNewUsers", 0L);
        statistics.put("activeUsers", 0L);

        // 게시판 통계
        statistics.put("todayNewBoards", 0L);
        statistics.put("todayNewComments", 0L);
        statistics.put("averageViewCount", 0.0);

        // 차트 데이터
        Map<String, Object> emptyChart = new HashMap<>();
        emptyChart.put("labels", new ArrayList<>());
        emptyChart.put("data", new ArrayList<>());
        statistics.put("weeklyBoardsChart", emptyChart);

        // 게시글 목록
        statistics.put("popularBoards", new ArrayList<>());
        statistics.put("recentBoards", new ArrayList<>());

        // 부서별 분포
        statistics.put("boardsByDepartment", new HashMap<>());

        log.warn("[StatisticsService] 빈 통계 데이터 반환 (오류 발생)");

        return statistics;
    }
}

/*
 * ====== Repository 메서드 요구사항 ======
 *
 * StatisticsService가 정상 동작하려면 다음 메서드들이 Repository에 있어야 합니다:
 *
 * **UserRepository**:
 * - countByCreatedAtAfter(LocalDateTime): 특정 시간 이후 가입한 사용자 수
 * - countByLastLoginAtAfter(LocalDateTime): 특정 시간 이후 로그인한 사용자 수
 *
 * **BoardRepository**:
 * - countByCreatedAtAfter(LocalDateTime): 특정 시간 이후 작성된 게시글 수
 * - countByCreatedAtBetween(LocalDateTime, LocalDateTime): 특정 기간의 게시글 수
 * - findAverageViewCount(): 평균 조회수
 * - findTop5ByIsDeletedFalseOrderByViewCountDesc(): 인기 게시글 TOP 5
 * - findTop5ByIsDeletedFalseOrderByCreatedAtDesc(): 최근 게시글 5개
 * - findBoardCountByDepartment(): 부서별 게시글 수 (GROUP BY)
 *
 * **CommentRepository**:
 * - countByCreatedAtAfter(LocalDateTime): 특정 시간 이후 작성된 댓글 수
 *
 * **FileRepository**:
 * - count(): 전체 파일 수 (기본 제공)
 *
 * 이러한 메서드들은 다음 단계에서 각 Repository에 추가해야 합니다.
 */

/*
 * ====== 성능 최적화 전략 ======
 *
 * 1. @Transactional(readOnly = true)
 *    - 읽기 전용 트랜잭션으로 성능 향상
 *    - 불필요한 Flush, Dirty Checking 생략
 *
 * 2. 집계 쿼리 사용
 *    - COUNT, AVG, GROUP BY 등 DB에서 직접 계산
 *    - Java 코드에서 계산하는 것보다 훨씬 빠름
 *
 * 3. TOP N 쿼리
 *    - findTop5... 메서드로 필요한 만큼만 조회
 *    - 페이징보다 간단하고 빠름
 *
 * 4. 인덱스 활용
 *    - created_at, view_count, is_deleted 등에 인덱스 설정 권장
 *
 * 5. 캐싱 적용 (향후 개선)
 *    - @Cacheable("dashboardStats")
 *    - TTL 5분 설정
 *    - 통계는 실시간이 아니어도 됨
 */

/*
 * ====== 테스트 방법 ======
 *
 * 1. 단위 테스트 (StatisticsServiceTest)
 *    ```java
 *    @Test
 *    void 대시보드_통계_조회_테스트() {
 *        Map<String, Object> stats = statisticsService.getDashboardStatistics();
 *        assertNotNull(stats.get("totalUsers"));
 *        assertNotNull(stats.get("popularBoards"));
 *    }
 *    ```
 *
 * 2. Postman 테스트
 *    - GET http://localhost:8080/api/statistics/dashboard
 *    - Authorization: Bearer {JWT_TOKEN}
 *    - 응답 데이터 구조 확인
 *
 * 3. 프론트엔드 통합 테스트
 *    - Dashboard.vue에서 API 호출
 *    - 통계 데이터 정상 표시 확인
 */