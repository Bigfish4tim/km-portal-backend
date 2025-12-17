// ==============================================
// 📁 load-tests/load-test-board.js
// K6 게시판 API 부하 테스트 스크립트
// 42일차 - 부하 테스트 설정 및 기본 테스트
// ==============================================

/**
 * K6 게시판 API 부하 테스트
 * 
 * 이 스크립트는 KM 포털의 게시판 관련 API에 대한
 * 부하 테스트를 수행합니다.
 * 
 * 테스트 대상:
 * - GET /api/boards (게시글 목록)
 * - GET /api/boards/:id (게시글 상세)
 * - POST /api/boards (게시글 작성)
 * - PUT /api/boards/:id (게시글 수정)
 * - DELETE /api/boards/:id (게시글 삭제)
 * - GET /api/boards/search (게시글 검색)
 * - POST /api/boards/:id/comments (댓글 작성)
 * 
 * 실행 방법:
 * 
 * 1. 연기 테스트 (Smoke Test):
 *    k6 run load-test-board.js
 * 
 * 2. 부하 테스트 (Load Test) - 400명 사용자 시뮬레이션:
 *    k6 run load-test-board.js -e TEST_TYPE=load
 * 
 * 3. 읽기 집중 테스트 (Read-Heavy):
 *    k6 run load-test-board.js -e TEST_TYPE=read_heavy
 * 
 * 4. 쓰기 집중 테스트 (Write-Heavy):
 *    k6 run load-test-board.js -e TEST_TYPE=write_heavy
 */

import http from 'k6/http';
import { sleep, check, group, fail } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';

// 설정 파일 임포트
import {
    BASE_URL,
    API_URL,
    TEST_USERS,
    SMOKE_TEST,
    LOAD_TEST,
    STRESS_TEST,
    DEFAULT_PARAMS,
    getAuthParams,
    randomThinkTime,
    randomString,
    randomNumber,
    generateBoardData,
    generateCommentData,
    validateResponse,
    parseJsonResponse,
    logTestStart,
    logTestEnd,
    boardListDuration,
    boardDetailDuration,
    boardCreateDuration,
    boardReadCount,
    boardCreateCount,
    apiSuccessRate,
    apiErrorCount
} from './load-test-config.js';

// ==============================================
// 커스텀 메트릭 정의
// ==============================================

// 게시판 API별 응답 시간
const boardUpdateDuration = new Trend('board_update_duration', true);
const boardDeleteDuration = new Trend('board_delete_duration', true);
const boardSearchDuration = new Trend('board_search_duration', true);
const commentCreateDuration = new Trend('comment_create_duration', true);

// 작업별 카운터
const boardUpdateCount = new Counter('board_update_count');
const boardDeleteCount = new Counter('board_delete_count');
const boardSearchCount = new Counter('board_search_count');
const commentCreateCount = new Counter('comment_create_count');

// 실패 카운터
const boardListFail = new Counter('board_list_fail');
const boardCreateFail = new Counter('board_create_fail');
const boardUpdateFail = new Counter('board_update_fail');
const boardDeleteFail = new Counter('board_delete_fail');

// 캐시 히트율 추적 (304 응답)
const cacheHitRate = new Rate('cache_hit_rate');

// ==============================================
// 테스트 옵션 설정
// ==============================================

const testType = __ENV.TEST_TYPE || 'smoke';

// 테스트 유형별 설정
const testConfigs = {
    // 연기 테스트: 빠른 확인
    smoke: {
        vus: 1,
        duration: '1m',
        thresholds: {
            'board_list_duration': ['p(95)<500'],
            'board_detail_duration': ['p(95)<300'],
            'http_req_failed': ['rate<0.05']
        }
    },
    
    // 부하 테스트: 400명 사용자 시뮬레이션
    load: {
        stages: [
            { duration: '2m', target: 100 },   // 워밍업
            { duration: '5m', target: 200 },   // 200명
            { duration: '5m', target: 400 },   // 400명 (목표)
            { duration: '3m', target: 200 },   // 다운
            { duration: '2m', target: 0 }      // 종료
        ],
        thresholds: {
            'board_list_duration': ['p(95)<1000', 'avg<500'],
            'board_detail_duration': ['p(95)<500', 'avg<300'],
            'board_create_duration': ['p(95)<1500'],
            'http_req_failed': ['rate<0.05'],
            'http_reqs': ['rate>100']          // 초당 100 요청 이상
        }
    },
    
    // 읽기 집중 테스트 (80% 읽기, 20% 쓰기)
    read_heavy: {
        stages: [
            { duration: '1m', target: 100 },
            { duration: '5m', target: 300 },
            { duration: '5m', target: 500 },   // 높은 동시 접속
            { duration: '2m', target: 0 }
        ],
        thresholds: {
            'board_list_duration': ['p(95)<800'],
            'board_detail_duration': ['p(95)<400'],
            'http_req_failed': ['rate<0.03']
        }
    },
    
    // 쓰기 집중 테스트 (50% 읽기, 50% 쓰기)
    write_heavy: {
        stages: [
            { duration: '1m', target: 50 },
            { duration: '5m', target: 100 },
            { duration: '5m', target: 200 },
            { duration: '2m', target: 0 }
        ],
        thresholds: {
            'board_create_duration': ['p(95)<2000'],
            'board_update_duration': ['p(95)<1500'],
            'http_req_failed': ['rate<0.10']   // 쓰기 테스트는 실패율 더 허용
        }
    },
    
    // 스트레스 테스트
    stress: STRESS_TEST
};

export const options = {
    ...testConfigs[testType],
    
    // 공통 임계값
    thresholds: {
        ...testConfigs[testType].thresholds,
        
        // 검색 API (조금 느림 허용)
        'board_search_duration': ['p(95)<2000'],
        
        // 댓글 작성
        'comment_create_duration': ['p(95)<1000'],
        
        // 전체 성공률
        'api_success_rate': ['rate>0.90']
    },
    
    // 태그
    tags: {
        testType: testType,
        testTarget: 'board-api',
        project: 'km-portal'
    },
    
    // 시나리오 (load 테스트에서만)
    scenarios: testType === 'load' || testType === 'read_heavy' || testType === 'write_heavy' ? {
        // 시나리오 1: 게시글 브라우징 (가장 많은 비중)
        browsing: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: testConfigs[testType].stages,
            exec: 'browsingScenario',
            tags: { scenario: 'browsing' },
            env: { SCENARIO: 'browsing' }
        },
        
        // 시나리오 2: 게시글 작성 (적은 비중)
        writing: {
            executor: 'constant-arrival-rate',
            rate: testType === 'write_heavy' ? 20 : 5,  // 초당 5-20회 작성
            timeUnit: '1s',
            duration: testType === 'write_heavy' ? '10m' : '15m',
            preAllocatedVUs: 10,
            maxVUs: 30,
            exec: 'writingScenario',
            startTime: '1m',
            tags: { scenario: 'writing' },
            env: { SCENARIO: 'writing' }
        },
        
        // 시나리오 3: 검색 (중간 비중)
        searching: {
            executor: 'constant-vus',
            vus: 10,
            duration: '12m',
            exec: 'searchingScenario',
            startTime: '2m',
            tags: { scenario: 'searching' },
            env: { SCENARIO: 'searching' }
        }
    } : undefined
};

// ==============================================
// 테스트 데이터
// ==============================================

// 검색 키워드 목록
const searchKeywords = new SharedArray('keywords', function() {
    return [
        '공지',
        '안내',
        '업데이트',
        '회의',
        '보고서',
        '프로젝트',
        '일정',
        '자료',
        '교육',
        '시스템',
        '개발',
        '테스트',
        'KM',
        '포털'
    ];
});

// 게시판 카테고리
const categories = ['GENERAL', 'NOTICE', 'QNA', 'FREE'];

// ==============================================
// 셋업 함수
// ==============================================

export function setup() {
    logTestStart(`게시판 API 부하 테스트 (${testType})`);
    
    console.log(`\n📋 테스트 설정:`);
    console.log(`   - 테스트 유형: ${testType}`);
    console.log(`   - 대상 URL: ${API_URL}`);
    
    // 로그인하여 토큰 획득
    console.log(`\n🔐 테스트 계정 로그인 중...`);
    
    const loginResponse = http.post(
        `${API_URL}/auth/login`,
        JSON.stringify(TEST_USERS.user),
        DEFAULT_PARAMS
    );
    
    const loginBody = parseJsonResponse(loginResponse);
    
    if (loginResponse.status !== 200 || !loginBody?.success) {
        console.error(`❌ 로그인 실패: ${loginBody?.message || 'Unknown error'}`);
        console.log(`   테스트를 계속하지만, 인증이 필요한 API는 실패할 수 있습니다.`);
    } else {
        console.log(`✅ 로그인 성공!`);
    }
    
    // 게시글 목록 확인
    console.log(`\n🔍 게시글 목록 확인 중...`);
    
    const boardsResponse = http.get(
        `${API_URL}/boards?page=0&size=10`,
        loginBody?.accessToken ? getAuthParams(loginBody.accessToken) : DEFAULT_PARAMS
    );
    
    if (boardsResponse.status === 200) {
        const boardsBody = parseJsonResponse(boardsResponse);
        console.log(`✅ 게시판 API 연결 성공!`);
        console.log(`   - 현재 게시글 수: ${boardsBody?.totalElements || 'Unknown'}`);
    } else {
        console.log(`⚠️ 게시판 API 접근 실패: ${boardsResponse.status}`);
    }
    
    console.log(`\n📌 테스트할 API 엔드포인트:`);
    console.log(`   - GET  ${API_URL}/boards`);
    console.log(`   - GET  ${API_URL}/boards/:id`);
    console.log(`   - POST ${API_URL}/boards`);
    console.log(`   - PUT  ${API_URL}/boards/:id`);
    console.log(`   - DELETE ${API_URL}/boards/:id`);
    console.log(`   - GET  ${API_URL}/boards/search`);
    
    console.log(`\n${'='.repeat(60)}\n`);
    
    return {
        startTime: Date.now(),
        testType: testType,
        accessToken: loginBody?.accessToken || null,
        refreshToken: loginBody?.refreshToken || null,
        user: loginBody?.user || null
    };
}

// ==============================================
// 메인 테스트 함수
// ==============================================

/**
 * 기본 테스트 함수 (smoke test용)
 */
export default function(data) {
    // 전체 게시판 사용 흐름 테스트
    browsingScenario(data);
}

/**
 * 브라우징 시나리오
 * 게시글 목록 조회 → 상세 조회 → 다음 페이지 반복
 */
export function browsingScenario(data) {
    let accessToken = data?.accessToken;
    
    // 토큰이 없으면 로그인 시도
    if (!accessToken) {
        accessToken = quickLogin();
    }
    
    const authParams = accessToken ? getAuthParams(accessToken) : DEFAULT_PARAMS;
    
    // 1. 게시글 목록 조회
    group('1. 게시글 목록 조회', () => {
        const page = randomNumber(0, 5);  // 0~5 페이지 중 랜덤
        const size = 10;
        
        const listResult = getBoardList(page, size, authParams);
        
        if (listResult.success && listResult.boards.length > 0) {
            // 랜덤하게 2-3개 게시글 상세 조회
            const viewCount = randomNumber(2, 3);
            const selectedBoards = listResult.boards
                .sort(() => Math.random() - 0.5)
                .slice(0, viewCount);
            
            for (const board of selectedBoards) {
                sleep(randomThinkTime(1, 3));
                
                group('2. 게시글 상세 조회', () => {
                    getBoardDetail(board.id, authParams);
                });
            }
        }
    });
    
    sleep(randomThinkTime(3, 6));
}

/**
 * 작성 시나리오
 * 게시글 작성 → (선택적) 수정 → (선택적) 삭제
 */
export function writingScenario(data) {
    let accessToken = data?.accessToken;
    
    if (!accessToken) {
        accessToken = quickLogin();
        if (!accessToken) {
            console.warn('로그인 실패로 쓰기 시나리오 건너뜀');
            return;
        }
    }
    
    const authParams = getAuthParams(accessToken);
    let createdBoardId = null;
    
    // 1. 게시글 작성
    group('1. 게시글 작성', () => {
        const boardData = generateBoardData(__ITER);
        const result = createBoard(boardData, authParams);
        
        if (result.success) {
            createdBoardId = result.boardId;
        }
    });
    
    if (!createdBoardId) {
        return;
    }
    
    sleep(randomThinkTime(2, 4));
    
    // 2. 50% 확률로 수정
    if (Math.random() < 0.5) {
        group('2. 게시글 수정', () => {
            updateBoard(createdBoardId, {
                title: `[수정됨] 부하테스트 게시글 - ${Date.now()}`,
                content: '수정된 내용입니다.\n\n' + randomString(100)
            }, authParams);
        });
        
        sleep(randomThinkTime(1, 2));
    }
    
    // 3. 댓글 작성 (70% 확률)
    if (Math.random() < 0.7) {
        group('3. 댓글 작성', () => {
            createComment(createdBoardId, generateCommentData(), authParams);
        });
        
        sleep(randomThinkTime(1, 2));
    }
    
    // 4. 80% 확률로 삭제 (테스트 데이터 정리)
    if (Math.random() < 0.8) {
        group('4. 게시글 삭제', () => {
            deleteBoard(createdBoardId, authParams);
        });
    }
}

/**
 * 검색 시나리오
 * 다양한 키워드로 게시글 검색
 */
export function searchingScenario(data) {
    let accessToken = data?.accessToken;
    
    if (!accessToken) {
        accessToken = quickLogin();
    }
    
    const authParams = accessToken ? getAuthParams(accessToken) : DEFAULT_PARAMS;
    
    // 랜덤 키워드 선택
    const keyword = searchKeywords[randomNumber(0, searchKeywords.length - 1)];
    
    group('게시글 검색', () => {
        searchBoards(keyword, authParams);
    });
    
    sleep(randomThinkTime(2, 5));
}

// ==============================================
// API 호출 함수들
// ==============================================

/**
 * 빠른 로그인 (VU별로 한 번만)
 */
function quickLogin() {
    const response = http.post(
        `${API_URL}/auth/login`,
        JSON.stringify(TEST_USERS.user),
        {
            ...DEFAULT_PARAMS,
            tags: { name: 'quick_login', type: 'auth' }
        }
    );
    
    const body = parseJsonResponse(response);
    
    if (response.status === 200 && body?.success) {
        return body.accessToken;
    }
    
    return null;
}

/**
 * 게시글 목록 조회
 */
function getBoardList(page = 0, size = 10, params = DEFAULT_PARAMS) {
    const startTime = Date.now();
    
    const response = http.get(
        `${API_URL}/boards?page=${page}&size=${size}&sort=createdAt,desc`,
        {
            ...params,
            tags: { name: 'board_list', type: 'board' }
        }
    );
    
    const duration = Date.now() - startTime;
    boardListDuration.add(duration);
    
    const body = parseJsonResponse(response);
    
    // 캐시 히트 체크 (304 또는 ETag 매칭)
    cacheHitRate.add(response.status === 304 ? 1 : 0);
    
    const isSuccess = check(response, {
        '게시글 목록 상태 200': (r) => r.status === 200 || r.status === 304,
        '게시글 목록 응답 시간 < 1초': (r) => r.timings.duration < 1000,
        '응답에 content 필드 존재': () => body && Array.isArray(body.content)
    });
    
    if (isSuccess && response.status === 200) {
        boardReadCount.add(1);
        apiSuccessRate.add(1);
        
        return {
            success: true,
            boards: body.content || [],
            totalElements: body.totalElements || 0,
            totalPages: body.totalPages || 0
        };
    } else if (response.status === 304) {
        // 캐시 응답도 성공으로 처리
        boardReadCount.add(1);
        apiSuccessRate.add(1);
        return { success: true, boards: [], cached: true };
    } else {
        boardListFail.add(1);
        apiSuccessRate.add(0);
        apiErrorCount.add(1);
        
        return { success: false, boards: [] };
    }
}

/**
 * 게시글 상세 조회
 */
function getBoardDetail(boardId, params = DEFAULT_PARAMS) {
    if (!boardId) {
        return { success: false };
    }
    
    const startTime = Date.now();
    
    const response = http.get(
        `${API_URL}/boards/${boardId}`,
        {
            ...params,
            tags: { name: 'board_detail', type: 'board', boardId: String(boardId) }
        }
    );
    
    const duration = Date.now() - startTime;
    boardDetailDuration.add(duration);
    
    const body = parseJsonResponse(response);
    
    cacheHitRate.add(response.status === 304 ? 1 : 0);
    
    const isSuccess = check(response, {
        '게시글 상세 상태 200': (r) => r.status === 200 || r.status === 304,
        '게시글 상세 응답 시간 < 500ms': (r) => r.timings.duration < 500,
        '게시글 ID 일치': () => body && (body.id === boardId || body.boardId === boardId)
    });
    
    if (isSuccess) {
        boardReadCount.add(1);
        apiSuccessRate.add(1);
        
        return {
            success: true,
            board: body
        };
    } else {
        apiSuccessRate.add(0);
        
        return { success: false };
    }
}

/**
 * 게시글 작성
 */
function createBoard(boardData, params) {
    const startTime = Date.now();
    
    const payload = JSON.stringify(boardData);
    
    const response = http.post(
        `${API_URL}/boards`,
        payload,
        {
            ...params,
            tags: { name: 'board_create', type: 'board' }
        }
    );
    
    const duration = Date.now() - startTime;
    boardCreateDuration.add(duration);
    
    const body = parseJsonResponse(response);
    
    const isSuccess = check(response, {
        '게시글 작성 상태 200/201': (r) => r.status === 200 || r.status === 201,
        '게시글 작성 응답 시간 < 2초': (r) => r.timings.duration < 2000,
        '생성된 게시글 ID 존재': () => body && (body.id || body.boardId || body.success)
    });
    
    if (isSuccess) {
        boardCreateCount.add(1);
        apiSuccessRate.add(1);
        
        return {
            success: true,
            boardId: body.id || body.boardId || body.data?.id
        };
    } else {
        boardCreateFail.add(1);
        apiSuccessRate.add(0);
        apiErrorCount.add(1);
        
        console.warn(`게시글 작성 실패: ${response.status} - ${body?.message || 'Unknown'}`);
        
        return { success: false };
    }
}

/**
 * 게시글 수정
 */
function updateBoard(boardId, updateData, params) {
    if (!boardId) {
        return { success: false };
    }
    
    const startTime = Date.now();
    
    const payload = JSON.stringify(updateData);
    
    const response = http.put(
        `${API_URL}/boards/${boardId}`,
        payload,
        {
            ...params,
            tags: { name: 'board_update', type: 'board', boardId: String(boardId) }
        }
    );
    
    const duration = Date.now() - startTime;
    boardUpdateDuration.add(duration);
    
    const isSuccess = check(response, {
        '게시글 수정 상태 200': (r) => r.status === 200,
        '게시글 수정 응답 시간 < 1.5초': (r) => r.timings.duration < 1500
    });
    
    if (isSuccess) {
        boardUpdateCount.add(1);
        apiSuccessRate.add(1);
        
        return { success: true };
    } else {
        boardUpdateFail.add(1);
        apiSuccessRate.add(0);
        
        return { success: false };
    }
}

/**
 * 게시글 삭제
 */
function deleteBoard(boardId, params) {
    if (!boardId) {
        return { success: false };
    }
    
    const startTime = Date.now();
    
    const response = http.del(
        `${API_URL}/boards/${boardId}`,
        null,
        {
            ...params,
            tags: { name: 'board_delete', type: 'board', boardId: String(boardId) }
        }
    );
    
    const duration = Date.now() - startTime;
    boardDeleteDuration.add(duration);
    
    // 200, 204, 또는 404(이미 삭제됨) 모두 허용
    const isSuccess = check(response, {
        '게시글 삭제 성공': (r) => r.status === 200 || r.status === 204 || r.status === 404,
        '게시글 삭제 응답 시간 < 1초': (r) => r.timings.duration < 1000
    });
    
    if (isSuccess) {
        boardDeleteCount.add(1);
        apiSuccessRate.add(1);
        
        return { success: true };
    } else {
        boardDeleteFail.add(1);
        apiSuccessRate.add(0);
        
        return { success: false };
    }
}

/**
 * 게시글 검색
 */
function searchBoards(keyword, params = DEFAULT_PARAMS) {
    const startTime = Date.now();
    
    const response = http.get(
        `${API_URL}/boards/search?keyword=${encodeURIComponent(keyword)}&page=0&size=20`,
        {
            ...params,
            tags: { name: 'board_search', type: 'board', keyword: keyword }
        }
    );
    
    const duration = Date.now() - startTime;
    boardSearchDuration.add(duration);
    
    const body = parseJsonResponse(response);
    
    const isSuccess = check(response, {
        '게시글 검색 상태 200': (r) => r.status === 200,
        '게시글 검색 응답 시간 < 2초': (r) => r.timings.duration < 2000
    });
    
    if (isSuccess) {
        boardSearchCount.add(1);
        apiSuccessRate.add(1);
        
        return {
            success: true,
            results: body.content || [],
            totalElements: body.totalElements || 0
        };
    } else {
        apiSuccessRate.add(0);
        
        return { success: false, results: [] };
    }
}

/**
 * 댓글 작성
 */
function createComment(boardId, commentData, params) {
    if (!boardId) {
        return { success: false };
    }
    
    const startTime = Date.now();
    
    const payload = JSON.stringify(commentData);
    
    const response = http.post(
        `${API_URL}/boards/${boardId}/comments`,
        payload,
        {
            ...params,
            tags: { name: 'comment_create', type: 'comment', boardId: String(boardId) }
        }
    );
    
    const duration = Date.now() - startTime;
    commentCreateDuration.add(duration);
    
    const isSuccess = check(response, {
        '댓글 작성 상태 200/201': (r) => r.status === 200 || r.status === 201,
        '댓글 작성 응답 시간 < 1초': (r) => r.timings.duration < 1000
    });
    
    if (isSuccess) {
        commentCreateCount.add(1);
        apiSuccessRate.add(1);
        
        return { success: true };
    } else {
        apiSuccessRate.add(0);
        
        return { success: false };
    }
}

// ==============================================
// 정리 함수
// ==============================================

export function teardown(data) {
    const endTime = Date.now();
    const totalDuration = ((endTime - (data?.startTime || endTime)) / 1000).toFixed(2);
    
    console.log(`\n${'='.repeat(60)}`);
    console.log(`📊 게시판 API 부하 테스트 완료`);
    console.log(`${'='.repeat(60)}`);
    console.log(`   - 테스트 유형: ${data?.testType || testType}`);
    console.log(`   - 총 소요 시간: ${totalDuration}초`);
    console.log(`${'='.repeat(60)}\n`);
    
    logTestEnd(`게시판 API 부하 테스트 (${data?.testType || testType})`);
}

// ==============================================
// 결과 요약
// ==============================================

export function handleSummary(data) {
    const metrics = data.metrics;
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    
    // 콘솔 요약 출력
    console.log('\n');
    console.log('╔══════════════════════════════════════════════════════════╗');
    console.log('║           KM 포털 게시판 API 부하 테스트 결과             ║');
    console.log('╠══════════════════════════════════════════════════════════╣');
    
    // 주요 메트릭 출력
    const listDur = metrics.board_list_duration?.values || {};
    const detailDur = metrics.board_detail_duration?.values || {};
    const createDur = metrics.board_create_duration?.values || {};
    const searchDur = metrics.board_search_duration?.values || {};
    
    console.log(`║ 게시글 목록 조회:                                        ║`);
    console.log(`║   - 평균: ${(listDur.avg || 0).toFixed(0)}ms, P95: ${(listDur['p(95)'] || 0).toFixed(0)}ms`);
    console.log(`║ 게시글 상세 조회:                                        ║`);
    console.log(`║   - 평균: ${(detailDur.avg || 0).toFixed(0)}ms, P95: ${(detailDur['p(95)'] || 0).toFixed(0)}ms`);
    console.log(`║ 게시글 작성:                                             ║`);
    console.log(`║   - 평균: ${(createDur.avg || 0).toFixed(0)}ms, P95: ${(createDur['p(95)'] || 0).toFixed(0)}ms`);
    console.log(`║ 게시글 검색:                                             ║`);
    console.log(`║   - 평균: ${(searchDur.avg || 0).toFixed(0)}ms, P95: ${(searchDur['p(95)'] || 0).toFixed(0)}ms`);
    
    // 처리량 정보
    if (metrics.http_reqs) {
        console.log(`║ 초당 요청 수: ${(metrics.http_reqs.values.rate || 0).toFixed(1)}/s`);
    }
    
    // 실패율 정보
    if (metrics.http_req_failed) {
        const failRate = ((metrics.http_req_failed.values.rate || 0) * 100).toFixed(2);
        console.log(`║ 전체 실패율: ${failRate}%`);
    }
    
    console.log('╚══════════════════════════════════════════════════════════╝');
    console.log('\n');
    
    return {
        'stdout': JSON.stringify(metrics, null, 2),
        [`results/board-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
        [`results/board-test-${timestamp}.html`]: generateBoardHtmlReport(data)
    };
}

/**
 * HTML 리포트 생성
 */
function generateBoardHtmlReport(data) {
    const metrics = data.metrics;
    const listDur = metrics.board_list_duration?.values || {};
    const detailDur = metrics.board_detail_duration?.values || {};
    const createDur = metrics.board_create_duration?.values || {};
    const searchDur = metrics.board_search_duration?.values || {};
    const httpReqs = metrics.http_reqs?.values || {};
    const httpFailed = metrics.http_req_failed?.values || {};
    
    return `
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KM 포털 게시판 API 부하 테스트 결과</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container { max-width: 1400px; margin: 0 auto; }
        .card {
            background: white;
            border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            padding: 30px;
            margin-bottom: 20px;
        }
        h1 { color: #333; margin-bottom: 10px; }
        h2 { color: #555; margin: 25px 0 15px; border-bottom: 2px solid #eee; padding-bottom: 10px; }
        .subtitle { color: #666; margin-bottom: 25px; }
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
            gap: 15px;
            margin-bottom: 20px;
        }
        .metric-card {
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            padding: 20px;
            border-radius: 12px;
            text-align: center;
        }
        .metric-value { font-size: 28px; font-weight: bold; color: #333; }
        .metric-label { color: #666; margin-top: 5px; font-size: 13px; }
        .metric-card.success { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); }
        .metric-card.warning { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); }
        .metric-card.danger { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); }
        .api-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
        }
        .api-card {
            background: #f8f9fa;
            border-radius: 12px;
            padding: 20px;
            border-left: 4px solid #11998e;
        }
        .api-card h3 { color: #333; margin-bottom: 15px; font-size: 16px; }
        .api-stat { display: flex; justify-content: space-between; margin: 8px 0; }
        .api-stat-label { color: #666; }
        .api-stat-value { font-weight: bold; color: #333; }
        .badge { 
            display: inline-block;
            padding: 4px 12px; 
            border-radius: 20px; 
            font-size: 12px; 
            font-weight: bold;
        }
        .badge-success { background: #d4edda; color: #155724; }
        .badge-danger { background: #f8d7da; color: #721c24; }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
        th { background: #f8f9fa; }
    </style>
</head>
<body>
    <div class="container">
        <div class="card">
            <h1>📋 KM 포털 게시판 API 부하 테스트 결과</h1>
            <p class="subtitle">42일차 - 게시판 API 성능 테스트 (${testType})</p>
            
            <h2>📊 전체 요약</h2>
            <div class="metrics-grid">
                <div class="metric-card success">
                    <div class="metric-value">${httpReqs.count || 0}</div>
                    <div class="metric-label">총 요청 수</div>
                </div>
                <div class="metric-card ${(httpFailed.rate || 0) < 0.05 ? 'success' : 'danger'}">
                    <div class="metric-value">${((httpFailed.rate || 0) * 100).toFixed(2)}%</div>
                    <div class="metric-label">실패율</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${(httpReqs.rate || 0).toFixed(1)}/s</div>
                    <div class="metric-label">초당 요청</div>
                </div>
                <div class="metric-card ${(metrics.cache_hit_rate?.values.rate || 0) > 0.1 ? 'success' : ''}">
                    <div class="metric-value">${((metrics.cache_hit_rate?.values.rate || 0) * 100).toFixed(1)}%</div>
                    <div class="metric-label">캐시 히트율</div>
                </div>
            </div>
            
            <h2>🔍 API별 성능</h2>
            <div class="api-grid">
                <div class="api-card">
                    <h3>📋 게시글 목록 (GET /boards)</h3>
                    <div class="api-stat">
                        <span class="api-stat-label">평균</span>
                        <span class="api-stat-value">${(listDur.avg || 0).toFixed(0)}ms</span>
                    </div>
                    <div class="api-stat">
                        <span class="api-stat-label">P95</span>
                        <span class="api-stat-value">${(listDur['p(95)'] || 0).toFixed(0)}ms</span>
                    </div>
                    <div class="api-stat">
                        <span class="api-stat-label">목표 (P95 < 1000ms)</span>
                        <span class="badge ${(listDur['p(95)'] || 0) < 1000 ? 'badge-success' : 'badge-danger'}">${(listDur['p(95)'] || 0) < 1000 ? '통과' : '실패'}</span>
                    </div>
                </div>
                
                <div class="api-card">
                    <h3>📄 게시글 상세 (GET /boards/:id)</h3>
                    <div class="api-stat">
                        <span class="api-stat-label">평균</span>
                        <span class="api-stat-value">${(detailDur.avg || 0).toFixed(0)}ms</span>
                    </div>
                    <div class="api-stat">
                        <span class="api-stat-label">P95</span>
                        <span class="api-stat-value">${(detailDur['p(95)'] || 0).toFixed(0)}ms</span>
                    </div>
                    <div class="api-stat">
                        <span class="api-stat-label">목표 (P95 < 500ms)</span>
                        <span class="badge ${(detailDur['p(95)'] || 0) < 500 ? 'badge-success' : 'badge-danger'}">${(detailDur['p(95)'] || 0) < 500 ? '통과' : '실패'}</span>
                    </div>
                </div>
                
                <div class="api-card">
                    <h3>✏️ 게시글 작성 (POST /boards)</h3>
                    <div class="api-stat">
                        <span class="api-stat-label">평균</span>
                        <span class="api-stat-value">${(createDur.avg || 0).toFixed(0)}ms</span>
                    </div>
                    <div class="api-stat">
                        <span class="api-stat-label">P95</span>
                        <span class="api-stat-value">${(createDur['p(95)'] || 0).toFixed(0)}ms</span>
                    </div>
                    <div class="api-stat">
                        <span class="api-stat-label">목표 (P95 < 1500ms)</span>
                        <span class="badge ${(createDur['p(95)'] || 0) < 1500 ? 'badge-success' : 'badge-danger'}">${(createDur['p(95)'] || 0) < 1500 ? '통과' : '실패'}</span>
                    </div>
                </div>
                
                <div class="api-card">
                    <h3>🔎 게시글 검색 (GET /boards/search)</h3>
                    <div class="api-stat">
                        <span class="api-stat-label">평균</span>
                        <span class="api-stat-value">${(searchDur.avg || 0).toFixed(0)}ms</span>
                    </div>
                    <div class="api-stat">
                        <span class="api-stat-label">P95</span>
                        <span class="api-stat-value">${(searchDur['p(95)'] || 0).toFixed(0)}ms</span>
                    </div>
                    <div class="api-stat">
                        <span class="api-stat-label">목표 (P95 < 2000ms)</span>
                        <span class="badge ${(searchDur['p(95)'] || 0) < 2000 ? 'badge-success' : 'badge-danger'}">${(searchDur['p(95)'] || 0) < 2000 ? '통과' : '실패'}</span>
                    </div>
                </div>
            </div>
            
            <h2>📈 응답 시간 분포</h2>
            <table>
                <thead>
                    <tr>
                        <th>API</th>
                        <th>최소</th>
                        <th>평균</th>
                        <th>중앙값</th>
                        <th>P90</th>
                        <th>P95</th>
                        <th>P99</th>
                        <th>최대</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>게시글 목록</td>
                        <td>${(listDur.min || 0).toFixed(0)}ms</td>
                        <td>${(listDur.avg || 0).toFixed(0)}ms</td>
                        <td>${(listDur.med || 0).toFixed(0)}ms</td>
                        <td>${(listDur['p(90)'] || 0).toFixed(0)}ms</td>
                        <td>${(listDur['p(95)'] || 0).toFixed(0)}ms</td>
                        <td>${(listDur['p(99)'] || 0).toFixed(0)}ms</td>
                        <td>${(listDur.max || 0).toFixed(0)}ms</td>
                    </tr>
                    <tr>
                        <td>게시글 상세</td>
                        <td>${(detailDur.min || 0).toFixed(0)}ms</td>
                        <td>${(detailDur.avg || 0).toFixed(0)}ms</td>
                        <td>${(detailDur.med || 0).toFixed(0)}ms</td>
                        <td>${(detailDur['p(90)'] || 0).toFixed(0)}ms</td>
                        <td>${(detailDur['p(95)'] || 0).toFixed(0)}ms</td>
                        <td>${(detailDur['p(99)'] || 0).toFixed(0)}ms</td>
                        <td>${(detailDur.max || 0).toFixed(0)}ms</td>
                    </tr>
                    <tr>
                        <td>게시글 작성</td>
                        <td>${(createDur.min || 0).toFixed(0)}ms</td>
                        <td>${(createDur.avg || 0).toFixed(0)}ms</td>
                        <td>${(createDur.med || 0).toFixed(0)}ms</td>
                        <td>${(createDur['p(90)'] || 0).toFixed(0)}ms</td>
                        <td>${(createDur['p(95)'] || 0).toFixed(0)}ms</td>
                        <td>${(createDur['p(99)'] || 0).toFixed(0)}ms</td>
                        <td>${(createDur.max || 0).toFixed(0)}ms</td>
                    </tr>
                    <tr>
                        <td>게시글 검색</td>
                        <td>${(searchDur.min || 0).toFixed(0)}ms</td>
                        <td>${(searchDur.avg || 0).toFixed(0)}ms</td>
                        <td>${(searchDur.med || 0).toFixed(0)}ms</td>
                        <td>${(searchDur['p(90)'] || 0).toFixed(0)}ms</td>
                        <td>${(searchDur['p(95)'] || 0).toFixed(0)}ms</td>
                        <td>${(searchDur['p(99)'] || 0).toFixed(0)}ms</td>
                        <td>${(searchDur.max || 0).toFixed(0)}ms</td>
                    </tr>
                </tbody>
            </table>
            
            <p style="text-align: right; color: #999; font-size: 12px; margin-top: 20px;">
                생성 시간: ${new Date().toISOString()}
            </p>
        </div>
    </div>
</body>
</html>
    `;
}
