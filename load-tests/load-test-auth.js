// ==============================================
// 📁 load-tests/load-test-auth.js
// K6 인증 API 부하 테스트 스크립트
// 42일차 - 부하 테스트 설정 및 기본 테스트
// ==============================================

/**
 * K6 인증 API 부하 테스트
 * 
 * 이 스크립트는 KM 포털의 인증 관련 API에 대한
 * 부하 테스트를 수행합니다.
 * 
 * 테스트 대상:
 * - POST /api/auth/login (로그인)
 * - POST /api/auth/register (회원가입)
 * - POST /api/auth/refresh (토큰 갱신)
 * - GET /api/auth/me (현재 사용자 정보)
 * - POST /api/auth/logout (로그아웃)
 * 
 * 실행 방법:
 * 
 * 1. 연기 테스트 (Smoke Test):
 *    k6 run load-test-auth.js
 * 
 * 2. 부하 테스트 (Load Test) - 동시 로그인 시뮬레이션:
 *    k6 run load-test-auth.js -e TEST_TYPE=load
 * 
 * 3. 특정 VU 수로 실행:
 *    k6 run --vus 100 --duration 5m load-test-auth.js
 * 
 * 4. 결과를 JSON으로 저장:
 *    k6 run --out json=auth-results.json load-test-auth.js
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
    validateResponse,
    parseJsonResponse,
    logTestStart,
    logTestEnd,
    loginSuccessCount,
    loginFailCount,
    loginSuccessRate,
    loginDuration,
    apiSuccessRate,
    apiErrorCount
} from './load-test-config.js';

// ==============================================
// 테스트 데이터 준비
// ==============================================

/**
 * 테스트 사용자 목록 (SharedArray로 VU간 공유)
 * 실제 테스트 환경에서는 별도 JSON 파일에서 로드
 */
const testUsers = new SharedArray('users', function() {
    // 테스트용 사용자 100명 생성
    const users = [];
    
    // 기본 테스트 계정
    users.push(TEST_USERS.user);
    users.push(TEST_USERS.admin);
    
    // 추가 테스트 계정 (실제 환경에서는 DB에 미리 생성 필요)
    for (let i = 1; i <= 98; i++) {
        users.push({
            username: `testuser${i}`,
            password: 'password123'
        });
    }
    
    return users;
});

// ==============================================
// 커스텀 메트릭 정의
// ==============================================

// 인증 관련 응답 시간 추적
const registerDuration = new Trend('register_duration', true);
const refreshDuration = new Trend('refresh_duration', true);
const meDuration = new Trend('me_duration', true);
const logoutDuration = new Trend('logout_duration', true);

// 인증 작업별 성공/실패 카운터
const registerSuccess = new Counter('register_success');
const registerFail = new Counter('register_fail');
const refreshSuccess = new Counter('refresh_success');
const refreshFail = new Counter('refresh_fail');

// 토큰 만료 관련
const tokenExpiredCount = new Counter('token_expired_count');

// ==============================================
// 테스트 옵션 설정
// ==============================================

const testType = __ENV.TEST_TYPE || 'smoke';

// 테스트 유형별 옵션
const testConfigs = {
    smoke: {
        vus: 1,
        duration: '1m',
        thresholds: {
            'login_duration': ['p(95)<1000'],
            'login_success_rate': ['rate>0.95'],
            'http_req_failed': ['rate<0.05']
        }
    },
    load: {
        stages: [
            { duration: '1m', target: 50 },    // 50명으로 램프업
            { duration: '3m', target: 100 },   // 100명으로 증가
            { duration: '5m', target: 200 },   // 200명 유지 (동시 로그인 시뮬레이션)
            { duration: '2m', target: 100 },   // 100명으로 감소
            { duration: '1m', target: 0 }      // 종료
        ],
        thresholds: {
            'login_duration': ['p(95)<2000', 'avg<1000'],
            'login_success_rate': ['rate>0.90'],
            'http_req_failed': ['rate<0.10'],
            'http_req_duration': ['p(95)<3000']
        }
    },
    stress: {
        stages: [
            { duration: '2m', target: 100 },
            { duration: '3m', target: 300 },
            { duration: '3m', target: 500 },   // 동시 500명 로그인 시도
            { duration: '2m', target: 100 },
            { duration: '1m', target: 0 }
        ],
        thresholds: {
            'login_duration': ['p(95)<5000'],
            'login_success_rate': ['rate>0.80'],
            'http_req_failed': ['rate<0.20']
        }
    }
};

export const options = {
    ...testConfigs[testType],
    
    // 공통 임계값
    thresholds: {
        ...testConfigs[testType].thresholds,
        
        // 회원가입 API (상대적으로 느림 허용)
        'register_duration': ['p(95)<3000'],
        
        // 토큰 갱신 API (빨라야 함)
        'refresh_duration': ['p(95)<500'],
        
        // 사용자 정보 조회 (빨라야 함)
        'me_duration': ['p(95)<300']
    },
    
    // 태그
    tags: {
        testType: testType,
        testTarget: 'auth-api',
        project: 'km-portal'
    },
    
    // 시나리오 (load/stress 테스트에서만)
    scenarios: testType !== 'smoke' ? {
        // 시나리오 1: 일반 로그인 흐름
        normal_login_flow: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: testConfigs[testType].stages,
            exec: 'normalLoginFlow',
            tags: { scenario: 'normal_login' }
        },
        
        // 시나리오 2: 빠른 로그인/로그아웃 반복
        rapid_auth: {
            executor: 'constant-arrival-rate',
            rate: 10,                // 초당 10회
            timeUnit: '1s',
            duration: '5m',
            preAllocatedVUs: 20,
            maxVUs: 50,
            exec: 'rapidAuthCycle',
            startTime: '2m',         // 2분 후 시작
            tags: { scenario: 'rapid_auth' }
        }
    } : undefined
};

// ==============================================
// 셋업 함수
// ==============================================

export function setup() {
    logTestStart(`인증 API 부하 테스트 (${testType})`);
    
    console.log(`\n📋 테스트 설정:`);
    console.log(`   - 테스트 유형: ${testType}`);
    console.log(`   - 대상 URL: ${API_URL}`);
    console.log(`   - 테스트 사용자 수: ${testUsers.length}명`);
    
    // API 연결 확인
    console.log(`\n🔍 인증 API 연결 확인 중...`);
    
    // 로그인 테스트
    const loginResponse = http.post(
        `${API_URL}/auth/login`,
        JSON.stringify(TEST_USERS.user),
        DEFAULT_PARAMS
    );
    
    if (loginResponse.status !== 200) {
        const body = parseJsonResponse(loginResponse);
        
        // success: false 응답도 API는 정상 작동
        if (body && body.success === false) {
            console.log(`⚠️ 테스트 계정 로그인 실패 (계정이 없을 수 있음)`);
            console.log(`   - 메시지: ${body.message || 'Unknown'}`);
        } else {
            console.error(`❌ 로그인 API 오류: ${loginResponse.status}`);
            throw new Error('인증 API에 연결할 수 없습니다.');
        }
    } else {
        const body = parseJsonResponse(loginResponse);
        if (body && body.success) {
            console.log(`✅ 인증 API 연결 성공!`);
            console.log(`   - 테스트 계정 로그인 확인됨`);
        }
    }
    
    console.log(`\n📌 테스트할 API 엔드포인트:`);
    console.log(`   - POST ${API_URL}/auth/login`);
    console.log(`   - POST ${API_URL}/auth/register`);
    console.log(`   - POST ${API_URL}/auth/refresh`);
    console.log(`   - GET  ${API_URL}/auth/me`);
    console.log(`   - POST ${API_URL}/auth/logout`);
    
    console.log(`\n${'='.repeat(60)}\n`);
    
    return {
        startTime: Date.now(),
        testType: testType,
        userCount: testUsers.length
    };
}

// ==============================================
// 메인 테스트 함수
// ==============================================

/**
 * 기본 테스트 함수 (smoke test용)
 */
export default function(data) {
    // 전체 인증 흐름 테스트
    normalLoginFlow(data);
}

/**
 * 일반 로그인 흐름 시나리오
 * 사용자가 로그인 → 활동 → 로그아웃하는 전체 흐름
 */
export function normalLoginFlow(data) {
    // VU별로 다른 사용자 선택
    const userIndex = (__VU - 1) % testUsers.length;
    const user = testUsers[userIndex];
    
    let accessToken = null;
    let refreshToken = null;
    
    // 그룹 1: 로그인
    group('1. 로그인', () => {
        const result = performLogin(user.username, user.password);
        
        if (result.success) {
            accessToken = result.accessToken;
            refreshToken = result.refreshToken;
        }
    });
    
    // 로그인 실패시 조기 종료
    if (!accessToken) {
        console.warn(`VU ${__VU}: 로그인 실패로 테스트 중단`);
        return;
    }
    
    sleep(randomThinkTime(1, 2));
    
    // 그룹 2: 사용자 정보 조회
    group('2. 사용자 정보 조회', () => {
        getUserInfo(accessToken);
    });
    
    sleep(randomThinkTime(2, 4));
    
    // 그룹 3: 토큰 갱신 (50% 확률)
    if (Math.random() < 0.5) {
        group('3. 토큰 갱신', () => {
            const result = refreshAccessToken(refreshToken);
            if (result.success) {
                accessToken = result.accessToken;
            }
        });
        
        sleep(randomThinkTime(1, 2));
    }
    
    // 그룹 4: 로그아웃
    group('4. 로그아웃', () => {
        performLogout(accessToken);
    });
}

/**
 * 빠른 인증 사이클 시나리오
 * 로그인/로그아웃을 빠르게 반복
 */
export function rapidAuthCycle(data) {
    const userIndex = (__VU - 1) % testUsers.length;
    const user = testUsers[userIndex];
    
    // 빠른 로그인
    const loginResult = performLogin(user.username, user.password);
    
    if (loginResult.success) {
        // 최소한의 대기
        sleep(0.5);
        
        // 즉시 로그아웃
        performLogout(loginResult.accessToken);
    }
    
    sleep(randomThinkTime(0.5, 1));
}

// ==============================================
// API 호출 함수들
// ==============================================

/**
 * 로그인 수행
 * @param {string} username - 사용자명
 * @param {string} password - 비밀번호
 * @returns {object} 로그인 결과 { success, accessToken, refreshToken }
 */
function performLogin(username, password) {
    const startTime = Date.now();
    
    const payload = JSON.stringify({
        username: username,
        password: password
    });
    
    const response = http.post(
        `${API_URL}/auth/login`,
        payload,
        {
            ...DEFAULT_PARAMS,
            tags: { name: 'login', type: 'auth' }
        }
    );
    
    const duration = Date.now() - startTime;
    loginDuration.add(duration);
    
    // 응답 검증
    const body = parseJsonResponse(response);
    
    const checks = check(response, {
        '로그인 상태 200': (r) => r.status === 200,
        '로그인 응답 시간 < 2초': (r) => r.timings.duration < 2000,
        '응답에 success 필드': () => body && 'success' in body
    });
    
    // 로그인 성공 여부 확인
    const isSuccess = response.status === 200 && body && body.success === true;
    
    if (isSuccess) {
        loginSuccessCount.add(1);
        loginSuccessRate.add(1);
        apiSuccessRate.add(1);
        
        // 토큰 유효성 검증
        check(body, {
            'accessToken 존재': (b) => b.accessToken && b.accessToken.length > 0,
            'refreshToken 존재': (b) => b.refreshToken && b.refreshToken.length > 0,
            'user 정보 존재': (b) => b.user && b.user.username
        });
        
        return {
            success: true,
            accessToken: body.accessToken,
            refreshToken: body.refreshToken,
            user: body.user
        };
    } else {
        loginFailCount.add(1);
        loginSuccessRate.add(0);
        apiSuccessRate.add(0);
        
        // 실패 원인 로깅
        if (body) {
            console.warn(`로그인 실패 [${username}]: ${body.message || 'Unknown error'}`);
        }
        
        return {
            success: false,
            error: body?.message || 'Login failed'
        };
    }
}

/**
 * 회원가입 수행
 * @param {object} userData - 사용자 데이터
 * @returns {object} 회원가입 결과
 */
function performRegister(userData) {
    const startTime = Date.now();
    
    const payload = JSON.stringify({
        username: userData.username || `user_${randomString(8)}`,
        password: userData.password || 'password123',
        email: userData.email || `test_${randomString(8)}@test.com`,
        name: userData.name || `테스트유저_${randomNumber(1, 1000)}`
    });
    
    const response = http.post(
        `${API_URL}/auth/register`,
        payload,
        {
            ...DEFAULT_PARAMS,
            tags: { name: 'register', type: 'auth' }
        }
    );
    
    const duration = Date.now() - startTime;
    registerDuration.add(duration);
    
    const body = parseJsonResponse(response);
    
    const isSuccess = response.status === 200 && body && body.success === true;
    
    if (isSuccess) {
        registerSuccess.add(1);
        apiSuccessRate.add(1);
    } else {
        registerFail.add(1);
        apiSuccessRate.add(0);
    }
    
    check(response, {
        '회원가입 상태 200': (r) => r.status === 200,
        '회원가입 응답 시간 < 3초': (r) => r.timings.duration < 3000
    });
    
    return {
        success: isSuccess,
        message: body?.message
    };
}

/**
 * 토큰 갱신
 * @param {string} refreshToken - 리프레시 토큰
 * @returns {object} 갱신 결과
 */
function refreshAccessToken(refreshToken) {
    if (!refreshToken) {
        console.warn('refreshToken이 없어 갱신 불가');
        return { success: false };
    }
    
    const startTime = Date.now();
    
    const payload = JSON.stringify({
        refreshToken: refreshToken
    });
    
    const response = http.post(
        `${API_URL}/auth/refresh`,
        payload,
        {
            ...DEFAULT_PARAMS,
            tags: { name: 'refresh', type: 'auth' }
        }
    );
    
    const duration = Date.now() - startTime;
    refreshDuration.add(duration);
    
    const body = parseJsonResponse(response);
    
    const isSuccess = response.status === 200 && body && body.accessToken;
    
    check(response, {
        '토큰 갱신 상태 200': (r) => r.status === 200,
        '토큰 갱신 응답 시간 < 500ms': (r) => r.timings.duration < 500,
        '새 accessToken 존재': () => body && body.accessToken
    });
    
    if (isSuccess) {
        refreshSuccess.add(1);
        apiSuccessRate.add(1);
        
        return {
            success: true,
            accessToken: body.accessToken
        };
    } else {
        refreshFail.add(1);
        apiSuccessRate.add(0);
        
        // 토큰 만료 체크
        if (response.status === 401 || (body && body.message?.includes('expired'))) {
            tokenExpiredCount.add(1);
        }
        
        return {
            success: false,
            error: body?.message || 'Token refresh failed'
        };
    }
}

/**
 * 현재 사용자 정보 조회
 * @param {string} accessToken - 액세스 토큰
 * @returns {object} 사용자 정보
 */
function getUserInfo(accessToken) {
    if (!accessToken) {
        return { success: false };
    }
    
    const startTime = Date.now();
    
    const response = http.get(
        `${API_URL}/auth/me`,
        {
            ...getAuthParams(accessToken),
            tags: { name: 'get_me', type: 'auth' }
        }
    );
    
    const duration = Date.now() - startTime;
    meDuration.add(duration);
    
    const body = parseJsonResponse(response);
    
    const isSuccess = response.status === 200;
    
    check(response, {
        '사용자 정보 조회 상태 200': (r) => r.status === 200,
        '사용자 정보 응답 시간 < 300ms': (r) => r.timings.duration < 300,
        'username 존재': () => body && body.username
    });
    
    if (isSuccess) {
        apiSuccessRate.add(1);
        return {
            success: true,
            user: body
        };
    } else {
        apiSuccessRate.add(0);
        
        // 토큰 만료 체크
        if (response.status === 401) {
            tokenExpiredCount.add(1);
        }
        
        return {
            success: false
        };
    }
}

/**
 * 로그아웃 수행
 * @param {string} accessToken - 액세스 토큰
 * @returns {object} 로그아웃 결과
 */
function performLogout(accessToken) {
    if (!accessToken) {
        return { success: false };
    }
    
    const startTime = Date.now();
    
    const response = http.post(
        `${API_URL}/auth/logout`,
        null,
        {
            ...getAuthParams(accessToken),
            tags: { name: 'logout', type: 'auth' }
        }
    );
    
    const duration = Date.now() - startTime;
    logoutDuration.add(duration);
    
    // 로그아웃은 200 또는 204 둘 다 성공
    const isSuccess = response.status === 200 || response.status === 204;
    
    check(response, {
        '로그아웃 성공': (r) => r.status === 200 || r.status === 204,
        '로그아웃 응답 시간 < 500ms': (r) => r.timings.duration < 500
    });
    
    if (isSuccess) {
        apiSuccessRate.add(1);
    } else {
        apiSuccessRate.add(0);
    }
    
    return {
        success: isSuccess
    };
}

// ==============================================
// 정리 함수
// ==============================================

export function teardown(data) {
    const endTime = Date.now();
    const totalDuration = ((endTime - (data?.startTime || endTime)) / 1000).toFixed(2);
    
    console.log(`\n${'='.repeat(60)}`);
    console.log(`📊 인증 API 부하 테스트 완료`);
    console.log(`${'='.repeat(60)}`);
    console.log(`   - 테스트 유형: ${data?.testType || testType}`);
    console.log(`   - 총 소요 시간: ${totalDuration}초`);
    console.log(`   - 테스트 사용자 수: ${data?.userCount || 0}명`);
    console.log(`${'='.repeat(60)}\n`);
    
    logTestEnd(`인증 API 부하 테스트 (${data?.testType || testType})`);
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
    console.log('║           KM 포털 인증 API 부하 테스트 결과               ║');
    console.log('╠══════════════════════════════════════════════════════════╣');
    
    // 로그인 관련 메트릭
    if (metrics.login_duration) {
        const loginDur = metrics.login_duration.values;
        console.log(`║ 로그인 응답 시간:                                        ║`);
        console.log(`║   - 평균: ${loginDur.avg?.toFixed(2) || 'N/A'}ms                                      `);
        console.log(`║   - P95: ${loginDur['p(95)']?.toFixed(2) || 'N/A'}ms                                   `);
    }
    
    if (metrics.login_success_rate) {
        const rate = (metrics.login_success_rate.values.rate * 100).toFixed(2);
        console.log(`║ 로그인 성공률: ${rate}%                                    `);
    }
    
    if (metrics.login_success_count && metrics.login_fail_count) {
        const success = metrics.login_success_count.values.count || 0;
        const fail = metrics.login_fail_count.values.count || 0;
        console.log(`║ 로그인 성공/실패: ${success} / ${fail}                             `);
    }
    
    console.log('╚══════════════════════════════════════════════════════════╝');
    console.log('\n');
    
    return {
        'stdout': JSON.stringify(data.metrics, null, 2),
        [`results/auth-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
        [`results/auth-test-${timestamp}.html`]: generateAuthHtmlReport(data)
    };
}

/**
 * HTML 리포트 생성
 */
function generateAuthHtmlReport(data) {
    const metrics = data.metrics;
    const loginDur = metrics.login_duration?.values || {};
    const loginRate = metrics.login_success_rate?.values || {};
    const refreshDur = metrics.refresh_duration?.values || {};
    
    return `
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KM 포털 인증 API 부하 테스트 결과</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body { 
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            min-height: 100vh;
            padding: 20px;
        }
        .container { max-width: 1200px; margin: 0 auto; }
        .card {
            background: white;
            border-radius: 16px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            padding: 30px;
            margin-bottom: 20px;
        }
        h1 { color: #333; margin-bottom: 10px; }
        h2 { color: #555; margin: 25px 0 15px; border-bottom: 2px solid #eee; padding-bottom: 10px; }
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 15px;
            margin-bottom: 20px;
        }
        .metric-card {
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            padding: 20px;
            border-radius: 12px;
            text-align: center;
        }
        .metric-value { font-size: 32px; font-weight: bold; color: #333; }
        .metric-label { color: #666; margin-top: 5px; font-size: 14px; }
        .metric-card.success { background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%); }
        .metric-card.warning { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); }
        .metric-card.danger { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); }
        table { width: 100%; border-collapse: collapse; margin-top: 15px; }
        th, td { padding: 12px; text-align: left; border-bottom: 1px solid #eee; }
        th { background: #f8f9fa; }
        .badge { 
            padding: 4px 12px; 
            border-radius: 20px; 
            font-size: 12px; 
            font-weight: bold;
        }
        .badge-success { background: #d4edda; color: #155724; }
        .badge-danger { background: #f8d7da; color: #721c24; }
    </style>
</head>
<body>
    <div class="container">
        <div class="card">
            <h1>🔐 KM 포털 인증 API 부하 테스트 결과</h1>
            <p style="color: #666; margin-bottom: 25px;">42일차 - 인증 API 성능 테스트 (${testType})</p>
            
            <h2>📊 로그인 API 성능</h2>
            <div class="metrics-grid">
                <div class="metric-card ${(loginRate.rate || 0) > 0.9 ? 'success' : 'danger'}">
                    <div class="metric-value">${((loginRate.rate || 0) * 100).toFixed(1)}%</div>
                    <div class="metric-label">로그인 성공률</div>
                </div>
                <div class="metric-card ${(loginDur.avg || 0) < 1000 ? 'success' : 'warning'}">
                    <div class="metric-value">${(loginDur.avg || 0).toFixed(0)}ms</div>
                    <div class="metric-label">평균 응답 시간</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${(loginDur['p(95)'] || 0).toFixed(0)}ms</div>
                    <div class="metric-label">P95 응답 시간</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${(loginDur.max || 0).toFixed(0)}ms</div>
                    <div class="metric-label">최대 응답 시간</div>
                </div>
            </div>
            
            <h2>🔄 토큰 갱신 API 성능</h2>
            <div class="metrics-grid">
                <div class="metric-card ${(refreshDur.avg || 0) < 500 ? 'success' : 'warning'}">
                    <div class="metric-value">${(refreshDur.avg || 0).toFixed(0)}ms</div>
                    <div class="metric-label">평균 응답 시간</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${(refreshDur['p(95)'] || 0).toFixed(0)}ms</div>
                    <div class="metric-label">P95 응답 시간</div>
                </div>
            </div>
            
            <h2>📋 API별 상세 결과</h2>
            <table>
                <thead>
                    <tr>
                        <th>API</th>
                        <th>평균</th>
                        <th>P95</th>
                        <th>목표</th>
                        <th>결과</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>POST /auth/login</td>
                        <td>${(loginDur.avg || 0).toFixed(2)}ms</td>
                        <td>${(loginDur['p(95)'] || 0).toFixed(2)}ms</td>
                        <td>P95 &lt; 2000ms</td>
                        <td><span class="badge ${(loginDur['p(95)'] || 0) < 2000 ? 'badge-success' : 'badge-danger'}">${(loginDur['p(95)'] || 0) < 2000 ? '통과' : '실패'}</span></td>
                    </tr>
                    <tr>
                        <td>POST /auth/refresh</td>
                        <td>${(refreshDur.avg || 0).toFixed(2)}ms</td>
                        <td>${(refreshDur['p(95)'] || 0).toFixed(2)}ms</td>
                        <td>P95 &lt; 500ms</td>
                        <td><span class="badge ${(refreshDur['p(95)'] || 0) < 500 ? 'badge-success' : 'badge-danger'}">${(refreshDur['p(95)'] || 0) < 500 ? '통과' : '실패'}</span></td>
                    </tr>
                    <tr>
                        <td>GET /auth/me</td>
                        <td>${(metrics.me_duration?.values.avg || 0).toFixed(2)}ms</td>
                        <td>${(metrics.me_duration?.values['p(95)'] || 0).toFixed(2)}ms</td>
                        <td>P95 &lt; 300ms</td>
                        <td><span class="badge ${(metrics.me_duration?.values['p(95)'] || 0) < 300 ? 'badge-success' : 'badge-danger'}">${(metrics.me_duration?.values['p(95)'] || 0) < 300 ? '통과' : '실패'}</span></td>
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
