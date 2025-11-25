package com.kmportal.backend.controller;

import com.kmportal.backend.dto.common.ApiResponse;
import com.kmportal.backend.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * StatisticsController - 통계 데이터 제공 컨트롤러
 *
 * 대시보드에서 필요한 각종 통계 데이터를 제공하는 REST API 컨트롤러입니다.
 * 모든 API는 인증된 사용자만 호출할 수 있으며, 일부 API는 관리자 권한이 필요합니다.
 *
 * 주요 기능:
 * 1. 시스템 전체 통계 제공 (사용자, 게시글, 댓글, 파일)
 * 2. 대시보드용 종합 통계 제공 (한 번에 모든 데이터 조회)
 * 3. 사용자 활동 통계 제공
 * 4. 게시판 통계 제공
 * 5. 부서별 통계 제공
 *
 * 성능 최적화:
 * - 한 번의 API 호출로 여러 통계를 한꺼번에 조회 (GET /api/statistics/dashboard)
 * - 집계 쿼리 최적화 (COUNT, GROUP BY)
 * - 캐싱 적용 가능 (향후 @Cacheable 추가 가능)
 *
 * 작성일: 2025년 11월 25일 (32일차)
 * 작성자: 32일차 개발 담당자
 *
 * @author KM Portal Dev Team
 * @version 1.0
 * @since 2025-11-25
 */
@Slf4j  // 로깅을 위한 Lombok 어노테이션
@RestController  // REST API 컨트롤러임을 표시
@RequestMapping("/api/statistics")  // 모든 엔드포인트는 /api/statistics로 시작
@RequiredArgsConstructor  // final 필드에 대한 생성자 자동 생성 (의존성 주입)
public class StatisticsController {

    // ====== 의존성 주입 ======

    /**
     * StatisticsService - 통계 비즈니스 로직 처리
     *
     * @RequiredArgsConstructor 어노테이션에 의해 자동으로 생성자 주입됩니다.
     */
    private final StatisticsService statisticsService;

    // ====== API 엔드포인트들 ======

    /**
     * 대시보드용 종합 통계 조회 API
     *
     * GET /api/statistics/dashboard
     *
     * 대시보드에서 필요한 모든 통계 데이터를 한 번에 조회합니다.
     * 여러 개의 API를 호출하는 것보다 성능이 우수합니다.
     *
     * 포함 데이터:
     * - totalUsers: 전체 사용자 수
     * - totalBoards: 전체 게시글 수
     * - totalComments: 전체 댓글 수
     * - totalFiles: 전체 파일 수
     * - todayNewUsers: 오늘 신규 가입자 수
     * - todayNewBoards: 오늘 작성된 게시글 수
     * - todayNewComments: 오늘 작성된 댓글 수
     * - activeUsers: 활성 사용자 수 (30일 내 로그인)
     * - recentBoards: 최근 게시글 5개 (제목, 작성자, 작성일)
     * - popularBoards: 인기 게시글 5개 (조회수 기준)
     * - boardsByDepartment: 부서별 게시글 수
     * - weeklyBoardsChart: 최근 7일간 게시글 작성 추이
     *
     * 권한: 모든 인증된 사용자 (USER, MANAGER, ADMIN)
     *
     * @return ApiResponse<Map<String, Object>> - 모든 통계 데이터를 담은 Map
     *
     * 사용 예시 (프론트엔드):
     * ```javascript
     * const response = await axios.get('/api/statistics/dashboard')
     * const stats = response.data.data
     * console.log('전체 사용자:', stats.totalUsers)
     * console.log('전체 게시글:', stats.totalBoards)
     * ```
     */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboardStatistics() {
        log.info("[Statistics API] 대시보드 종합 통계 조회 요청");

        try {
            // StatisticsService에서 모든 통계 데이터를 한 번에 조회
            Map<String, Object> statistics = statisticsService.getDashboardStatistics();

            log.info("[Statistics API] 대시보드 통계 조회 성공 - 항목 수: {}", statistics.size());

            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .message("대시보드 통계 조회 성공")
                            .data(statistics)
                            .build()
            );

        } catch (Exception e) {
            log.error("[Statistics API] 대시보드 통계 조회 실패", e);

            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 시스템 기본 통계 조회 API
     *
     * GET /api/statistics/system
     *
     * 시스템의 기본 통계 데이터만 조회합니다.
     * (사용자, 게시글, 댓글, 파일의 전체 개수)
     *
     * 포함 데이터:
     * - totalUsers: 전체 사용자 수
     * - totalBoards: 전체 게시글 수
     * - totalComments: 전체 댓글 수
     * - totalFiles: 전체 파일 수
     *
     * 권한: 모든 인증된 사용자
     *
     * @return ApiResponse<Map<String, Long>> - 기본 통계 숫자들
     */
    @GetMapping("/system")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getSystemStatistics() {
        log.info("[Statistics API] 시스템 기본 통계 조회 요청");

        try {
            Map<String, Long> statistics = statisticsService.getSystemStatistics();

            log.info("[Statistics API] 시스템 통계 조회 성공");

            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Long>>builder()
                            .success(true)
                            .message("시스템 통계 조회 성공")
                            .data(statistics)
                            .build()
            );

        } catch (Exception e) {
            log.error("[Statistics API] 시스템 통계 조회 실패", e);

            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, Long>>builder()
                            .success(false)
                            .message("통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 사용자 활동 통계 조회 API
     *
     * GET /api/statistics/user-activity
     *
     * 사용자들의 활동 관련 통계 데이터를 조회합니다.
     *
     * 포함 데이터:
     * - todayNewUsers: 오늘 신규 가입자 수
     * - activeUsers: 활성 사용자 수 (30일 내 로그인)
     * - inactiveUsers: 비활성 사용자 수 (30일 이상 미로그인)
     *
     * 권한: MANAGER, ADMIN만 조회 가능 (USER 권한은 조회 불가)
     *
     * @return ApiResponse<Map<String, Long>> - 사용자 활동 통계
     */
    @GetMapping("/user-activity")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUserActivityStatistics() {
        log.info("[Statistics API] 사용자 활동 통계 조회 요청");

        try {
            Map<String, Long> statistics = statisticsService.getUserActivityStatistics();

            log.info("[Statistics API] 사용자 활동 통계 조회 성공");

            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Long>>builder()
                            .success(true)
                            .message("사용자 활동 통계 조회 성공")
                            .data(statistics)
                            .build()
            );

        } catch (Exception e) {
            log.error("[Statistics API] 사용자 활동 통계 조회 실패", e);

            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, Long>>builder()
                            .success(false)
                            .message("통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 게시판 통계 조회 API
     *
     * GET /api/statistics/boards
     *
     * 게시판 관련 통계 데이터를 조회합니다.
     *
     * 포함 데이터:
     * - todayNewBoards: 오늘 작성된 게시글 수
     * - todayNewComments: 오늘 작성된 댓글 수
     * - weeklyBoards: 이번 주 작성된 게시글 수
     * - monthlyBoards: 이번 달 작성된 게시글 수
     * - averageViewCount: 평균 조회수
     *
     * 권한: 모든 인증된 사용자
     *
     * @return ApiResponse<Map<String, Object>> - 게시판 통계
     */
    @GetMapping("/boards")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBoardStatistics() {
        log.info("[Statistics API] 게시판 통계 조회 요청");

        try {
            Map<String, Object> statistics = statisticsService.getBoardStatistics();

            log.info("[Statistics API] 게시판 통계 조회 성공");

            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .message("게시판 통계 조회 성공")
                            .data(statistics)
                            .build()
            );

        } catch (Exception e) {
            log.error("[Statistics API] 게시판 통계 조회 실패", e);

            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 최근 7일 게시글 추이 조회 API
     *
     * GET /api/statistics/boards/weekly-chart
     *
     * 최근 7일간 날짜별 게시글 작성 추이를 조회합니다.
     * Chart.js에서 사용할 수 있는 형식으로 반환됩니다.
     *
     * 반환 형식:
     * {
     *   "labels": ["2025-11-19", "2025-11-20", ..., "2025-11-25"],
     *   "data": [5, 8, 3, 12, 7, 9, 10]
     * }
     *
     * 권한: 모든 인증된 사용자
     *
     * @return ApiResponse<Map<String, Object>> - 차트 데이터
     */
    @GetMapping("/boards/weekly-chart")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getWeeklyBoardChart() {
        log.info("[Statistics API] 주간 게시글 차트 데이터 조회 요청");

        try {
            Map<String, Object> chartData = statisticsService.getWeeklyBoardChart();

            log.info("[Statistics API] 주간 게시글 차트 데이터 조회 성공");

            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(true)
                            .message("주간 게시글 차트 데이터 조회 성공")
                            .data(chartData)
                            .build()
            );

        } catch (Exception e) {
            log.error("[Statistics API] 주간 게시글 차트 데이터 조회 실패", e);

            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, Object>>builder()
                            .success(false)
                            .message("차트 데이터 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 부서별 게시글 분포 조회 API
     *
     * GET /api/statistics/boards/by-department
     *
     * 부서별로 작성된 게시글 수를 조회합니다.
     *
     * 반환 형식:
     * {
     *   "개발팀": 45,
     *   "기획팀": 32,
     *   "디자인팀": 28,
     *   ...
     * }
     *
     * 권한: MANAGER, ADMIN만 조회 가능
     *
     * @return ApiResponse<Map<String, Long>> - 부서별 게시글 수
     */
    @GetMapping("/boards/by-department")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getBoardsByDepartment() {
        log.info("[Statistics API] 부서별 게시글 분포 조회 요청");

        try {
            Map<String, Long> statistics = statisticsService.getBoardsByDepartment();

            log.info("[Statistics API] 부서별 게시글 분포 조회 성공");

            return ResponseEntity.ok(
                    ApiResponse.<Map<String, Long>>builder()
                            .success(true)
                            .message("부서별 게시글 분포 조회 성공")
                            .data(statistics)
                            .build()
            );

        } catch (Exception e) {
            log.error("[Statistics API] 부서별 게시글 분포 조회 실패", e);

            return ResponseEntity.internalServerError().body(
                    ApiResponse.<Map<String, Long>>builder()
                            .success(false)
                            .message("통계 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 인기 게시글 TOP 5 조회 API
     *
     * GET /api/statistics/boards/popular
     *
     * 조회수가 가장 많은 게시글 5개를 조회합니다.
     *
     * 반환 데이터 구조:
     * [
     *   {
     *     "id": 1,
     *     "title": "게시글 제목",
     *     "authorName": "작성자 이름",
     *     "viewCount": 123,
     *     "commentCount": 45,
     *     "createdAt": "2025-11-25T10:30:00"
     *   },
     *   ...
     * ]
     *
     * 권한: 모든 인증된 사용자
     *
     * @return ApiResponse<List<Map<String, Object>>> - 인기 게시글 목록
     */
    @GetMapping("/boards/popular")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getPopularBoards() {
        log.info("[Statistics API] 인기 게시글 TOP 5 조회 요청");

        try {
            var popularBoards = statisticsService.getPopularBoards();

            log.info("[Statistics API] 인기 게시글 조회 성공 - {} 건", popularBoards.size());

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("인기 게시글 조회 성공")
                            .data(popularBoards)
                            .build()
            );

        } catch (Exception e) {
            log.error("[Statistics API] 인기 게시글 조회 실패", e);

            return ResponseEntity.internalServerError().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("인기 게시글 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }

    /**
     * 최근 게시글 5개 조회 API
     *
     * GET /api/statistics/boards/recent
     *
     * 최근에 작성된 게시글 5개를 조회합니다.
     *
     * 반환 데이터 구조:
     * [
     *   {
     *     "id": 10,
     *     "title": "최신 게시글 제목",
     *     "authorName": "작성자 이름",
     *     "viewCount": 10,
     *     "commentCount": 2,
     *     "createdAt": "2025-11-25T14:00:00"
     *   },
     *   ...
     * ]
     *
     * 권한: 모든 인증된 사용자
     *
     * @return ApiResponse<List<Map<String, Object>>> - 최근 게시글 목록
     */
    @GetMapping("/boards/recent")
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMIN')")
    public ResponseEntity<ApiResponse<?>> getRecentBoards() {
        log.info("[Statistics API] 최근 게시글 5개 조회 요청");

        try {
            var recentBoards = statisticsService.getRecentBoards();

            log.info("[Statistics API] 최근 게시글 조회 성공 - {} 건", recentBoards.size());

            return ResponseEntity.ok(
                    ApiResponse.builder()
                            .success(true)
                            .message("최근 게시글 조회 성공")
                            .data(recentBoards)
                            .build()
            );

        } catch (Exception e) {
            log.error("[Statistics API] 최근 게시글 조회 실패", e);

            return ResponseEntity.internalServerError().body(
                    ApiResponse.builder()
                            .success(false)
                            .message("최근 게시글 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .build()
            );
        }
    }
}

/*
 * ====== API 엔드포인트 정리 ======
 *
 * 1. GET /api/statistics/dashboard
 *    - 설명: 대시보드용 종합 통계 (한 번에 모든 데이터)
 *    - 권한: USER, MANAGER, ADMIN
 *    - 반환: Map<String, Object> (모든 통계 포함)
 *
 * 2. GET /api/statistics/system
 *    - 설명: 시스템 기본 통계 (사용자/게시글/댓글/파일 수)
 *    - 권한: USER, MANAGER, ADMIN
 *    - 반환: Map<String, Long>
 *
 * 3. GET /api/statistics/user-activity
 *    - 설명: 사용자 활동 통계
 *    - 권한: MANAGER, ADMIN
 *    - 반환: Map<String, Long>
 *
 * 4. GET /api/statistics/boards
 *    - 설명: 게시판 통계
 *    - 권한: USER, MANAGER, ADMIN
 *    - 반환: Map<String, Object>
 *
 * 5. GET /api/statistics/boards/weekly-chart
 *    - 설명: 주간 게시글 작성 추이 (차트용)
 *    - 권한: USER, MANAGER, ADMIN
 *    - 반환: Map<String, Object> (labels, data)
 *
 * 6. GET /api/statistics/boards/by-department
 *    - 설명: 부서별 게시글 분포
 *    - 권한: MANAGER, ADMIN
 *    - 반환: Map<String, Long>
 *
 * 7. GET /api/statistics/boards/popular
 *    - 설명: 인기 게시글 TOP 5
 *    - 권한: USER, MANAGER, ADMIN
 *    - 반환: List<Map<String, Object>>
 *
 * 8. GET /api/statistics/boards/recent
 *    - 설명: 최근 게시글 5개
 *    - 권한: USER, MANAGER, ADMIN
 *    - 반환: List<Map<String, Object>>
 */

/*
 * ====== 성능 최적화 전략 ======
 *
 * 1. 한 번에 모든 통계 조회
 *    - /api/statistics/dashboard 엔드포인트 사용
 *    - 여러 API를 호출하는 것보다 효율적
 *
 * 2. 집계 쿼리 최적화
 *    - COUNT, GROUP BY 사용
 *    - 인덱스 활용 (created_at, author_id 등)
 *
 * 3. 캐싱 적용 (향후 개선)
 *    - @Cacheable("dashboardStats")
 *    - TTL 5분 설정
 *    - 통계는 실시간이 아니어도 됨
 *
 * 4. 페이징 미사용
 *    - TOP 5, 최근 5개 등 작은 데이터만 조회
 *    - 페이징 오버헤드 없음
 */

/*
 * ====== 보안 고려사항 ======
 *
 * 1. 권한 분리
 *    - 일반 통계: 모든 사용자
 *    - 사용자 활동: MANAGER, ADMIN만
 *    - 부서별 분포: MANAGER, ADMIN만
 *
 * 2. 민감 정보 제외
 *    - 비밀번호, 이메일 전체 등 제외
 *    - 통계 숫자만 제공
 *
 * 3. SQL Injection 방지
 *    - JPA Query Method 사용
 *    - 직접 쿼리 작성 시 @Param 사용
 */