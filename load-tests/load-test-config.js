// ==============================================
// 📁 load-tests/load-test-config.js
// K6 부하 테스트 공통 설정 파일
// 42일차 - 성능 테스트 도구 설정
// ==============================================

/**
 * 부하 테스트 공통 설정
 * 
 * K6 설치 방법 (Windows):
 * 1. Chocolatey 사용: choco install k6
 * 2. 또는 https://dl.k6.io/msi/k6-latest-amd64.msi 다운로드
 * 
 * 실행 방법:
 * k6 run load-test-basic.js
 * k6 run --vus 50 --duration 1m load-test-auth.js
 */

// ==============================================
// 환경 설정
// ==============================================

/**
 * 테스트 환경 URL 설정
 * 환경변수 K6_BASE_URL로 오버라이드 가능
 */
export const BASE_URL = __ENV.K6_BASE_URL || 'http://localhost:8080';
export const API_URL = `${BASE_URL}/api`;

/**
 * 테스트 계정 정보
 * 실제 테스트 환경에서는 환경변수로 관리 권장
 */
export const TEST_USERS = {
    // 일반 사용자 계정
    user: {
        username: __ENV.K6_USER || 'testuser',
        password: __ENV.K6_PASSWORD || 'password123'
    },
    // 관리자 계정
    admin: {
        username: __ENV.K6_ADMIN || 'admin',
        password: __ENV.K6_ADMIN_PASSWORD || 'admin123'
    }
};

// ==============================================
// 부하 테스트 시나리오 프리셋
// ==============================================

/**
 * 연기 테스트 (Smoke Test)
 * - 목적: 시스템이 정상 작동하는지 빠르게 확인
 * - 부하: 매우 낮음 (1-2 VUs)
 * - 시간: 1분
 */
export const SMOKE_TEST = {
    vus: 1,
    duration: '1m',
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 95%가 500ms 이하
        http_req_failed: ['rate<0.01']      // 실패율 1% 미만
    }
};

/**
 * 부하 테스트 (Load Test)
 * - 목적: 예상 사용자 수에서의 성능 측정
 * - 부하: 400명 (KM 포털 예상 사용자 수)
 * - 시간: 10분 (램프업 2분 + 유지 5분 + 램프다운 3분)
 */
export const LOAD_TEST = {
    stages: [
        { duration: '2m', target: 100 },   // 2분간 100명으로 증가
        { duration: '3m', target: 200 },   // 3분간 200명으로 증가
        { duration: '5m', target: 400 },   // 5분간 400명 유지 (목표 사용자 수)
        { duration: '2m', target: 200 },   // 2분간 200명으로 감소
        { duration: '1m', target: 0 }      // 1분간 0명으로 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000', 'p(99)<2000'],  // 95%가 1초, 99%가 2초 이하
        http_req_failed: ['rate<0.05'],                   // 실패율 5% 미만
        http_reqs: ['rate>100']                           // 초당 100 요청 이상
    }
};

/**
 * 스트레스 테스트 (Stress Test)
 * - 목적: 시스템 한계점 파악
 * - 부하: 점진적으로 증가 (최대 1000명)
 * - 시간: 15분
 */
export const STRESS_TEST = {
    stages: [
        { duration: '2m', target: 100 },    // 워밍업
        { duration: '3m', target: 300 },    // 증가
        { duration: '3m', target: 500 },    // 더 증가
        { duration: '3m', target: 800 },    // 스트레스 시작
        { duration: '2m', target: 1000 },   // 최대 부하
        { duration: '2m', target: 0 }       // 복구
    ],
    thresholds: {
        http_req_duration: ['p(95)<3000'],  // 95%가 3초 이하
        http_req_failed: ['rate<0.10']      // 실패율 10% 미만
    }
};

/**
 * 스파이크 테스트 (Spike Test)
 * - 목적: 갑작스러운 트래픽 증가 대응 능력 확인
 * - 부하: 급격한 증가 후 감소
 * - 시간: 5분
 */
export const SPIKE_TEST = {
    stages: [
        { duration: '30s', target: 10 },    // 안정 상태
        { duration: '30s', target: 500 },   // 급격한 증가!
        { duration: '1m', target: 500 },    // 유지
        { duration: '30s', target: 10 },    // 급격한 감소
        { duration: '2m', target: 10 }      // 안정화
    ],
    thresholds: {
        http_req_duration: ['p(95)<5000'],  // 스파이크 시 5초까지 허용
        http_req_failed: ['rate<0.15']      // 실패율 15% 미만
    }
};

/**
 * 지속 테스트 (Soak Test / Endurance Test)
 * - 목적: 장시간 운영 시 메모리 누수, 성능 저하 확인
 * - 부하: 중간 수준 유지
 * - 시간: 1시간 이상 권장 (예시는 30분)
 */
export const SOAK_TEST = {
    stages: [
        { duration: '2m', target: 200 },    // 램프업
        { duration: '26m', target: 200 },   // 장시간 유지
        { duration: '2m', target: 0 }       // 램프다운
    ],
    thresholds: {
        http_req_duration: ['p(95)<1500'],
        http_req_failed: ['rate<0.02']
    }
};

// ==============================================
// HTTP 요청 기본 설정
// ==============================================

/**
 * 공통 HTTP 요청 파라미터
 */
export const DEFAULT_PARAMS = {
    headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Accept-Language': 'ko-KR,ko;q=0.9'
    },
    timeout: '30s',
    // 리다이렉트 설정
    redirects: 5,
    // TLS 검증 (개발환경에서는 비활성화)
    insecureSkipTLSVerify: true
};

/**
 * 인증된 요청을 위한 헤더 생성
 * @param {string} token - JWT 액세스 토큰
 * @returns {object} HTTP 요청 파라미터
 */
export function getAuthParams(token) {
    return {
        headers: {
            ...DEFAULT_PARAMS.headers,
            'Authorization': `Bearer ${token}`
        },
        timeout: DEFAULT_PARAMS.timeout,
        redirects: DEFAULT_PARAMS.redirects,
        insecureSkipTLSVerify: DEFAULT_PARAMS.insecureSkipTLSVerify
    };
}

// ==============================================
// 임계값 (Thresholds) 설정
// ==============================================

/**
 * API별 성능 목표 (SLO - Service Level Objectives)
 * 
 * KM 포털 400명 사용자 기준 성능 목표:
 * - 로그인 API: 500ms 이내 (95%)
 * - 게시글 목록: 300ms 이내 (95%)
 * - 게시글 상세: 200ms 이내 (95%)
 * - 게시글 작성: 500ms 이내 (95%)
 * - 파일 업로드: 2초 이내 (95%)
 */
export const API_SLO = {
    auth: {
        login: 500,
        register: 1000,
        refresh: 200
    },
    board: {
        list: 300,
        detail: 200,
        create: 500,
        update: 500,
        delete: 300
    },
    file: {
        upload: 2000,
        download: 1000,
        list: 300
    },
    user: {
        list: 300,
        detail: 200
    }
};

/**
 * 기본 임계값 템플릿
 * 각 테스트 파일에서 확장하여 사용
 */
export const BASE_THRESHOLDS = {
    // HTTP 요청 관련
    http_req_duration: ['p(95)<1000', 'p(99)<2000', 'avg<500', 'max<5000'],
    http_req_failed: ['rate<0.05'],
    http_reqs: ['rate>50'],
    
    // 반복 관련
    iteration_duration: ['p(95)<3000'],
    iterations: ['rate>10'],
    
    // 데이터 전송 관련
    data_received: ['rate>100000'],   // 초당 100KB 이상
    data_sent: ['rate>10000'],        // 초당 10KB 이상
    
    // 가상 사용자 관련
    vus: ['value>0'],
    vus_max: ['value>0']
};

// ==============================================
// 유틸리티 함수
// ==============================================

/**
 * 랜덤 지연 시간 생성 (Think Time 시뮬레이션)
 * 실제 사용자의 행동 패턴을 모방
 * 
 * @param {number} min - 최소 지연 시간 (초)
 * @param {number} max - 최대 지연 시간 (초)
 * @returns {number} 랜덤 지연 시간 (초)
 */
export function randomThinkTime(min = 1, max = 3) {
    return Math.random() * (max - min) + min;
}

/**
 * 랜덤 문자열 생성
 * @param {number} length - 문자열 길이
 * @returns {string} 랜덤 문자열
 */
export function randomString(length = 10) {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}

/**
 * 랜덤 숫자 생성
 * @param {number} min - 최소값
 * @param {number} max - 최대값
 * @returns {number} 랜덤 숫자
 */
export function randomNumber(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * 테스트 데이터 생성 - 게시글
 * @param {number} index - 인덱스 번호
 * @returns {object} 게시글 데이터
 */
export function generateBoardData(index = 0) {
    const timestamp = Date.now();
    return {
        title: `[K6 부하테스트] 게시글 ${index} - ${timestamp}`,
        content: `이 게시글은 K6 부하 테스트에서 자동 생성되었습니다.\n\n` +
                 `생성 시간: ${new Date().toISOString()}\n` +
                 `테스트 인덱스: ${index}\n` +
                 `랜덤 데이터: ${randomString(50)}`,
        category: ['GENERAL', 'NOTICE', 'QNA'][randomNumber(0, 2)],
        isNotice: false,
        isPinned: false
    };
}

/**
 * 테스트 데이터 생성 - 댓글
 * @returns {object} 댓글 데이터
 */
export function generateCommentData() {
    return {
        content: `K6 테스트 댓글 - ${new Date().toISOString()} - ${randomString(20)}`
    };
}

/**
 * 응답 검증 함수
 * @param {object} response - HTTP 응답 객체
 * @param {number} expectedStatus - 예상 상태 코드
 * @param {string} testName - 테스트 이름 (로깅용)
 * @returns {boolean} 검증 성공 여부
 */
export function validateResponse(response, expectedStatus, testName = '') {
    const isValid = response.status === expectedStatus;
    
    if (!isValid) {
        console.error(`[${testName}] 응답 검증 실패: ` +
            `예상=${expectedStatus}, 실제=${response.status}, ` +
            `URL=${response.url}`);
        
        // 에러 응답 본문 로깅 (디버깅용)
        if (response.body) {
            try {
                const body = JSON.parse(response.body);
                console.error(`응답 본문: ${JSON.stringify(body)}`);
            } catch (e) {
                console.error(`응답 본문 (텍스트): ${response.body.substring(0, 200)}`);
            }
        }
    }
    
    return isValid;
}

/**
 * JSON 응답 파싱 함수
 * @param {object} response - HTTP 응답 객체
 * @returns {object|null} 파싱된 JSON 또는 null
 */
export function parseJsonResponse(response) {
    try {
        return JSON.parse(response.body);
    } catch (e) {
        console.error(`JSON 파싱 실패: ${e.message}`);
        return null;
    }
}

// ==============================================
// 커스텀 메트릭 정의
// ==============================================

import { Counter, Gauge, Rate, Trend } from 'k6/metrics';

// 카운터 메트릭
export const loginSuccessCount = new Counter('login_success_count');
export const loginFailCount = new Counter('login_fail_count');
export const boardCreateCount = new Counter('board_create_count');
export const boardReadCount = new Counter('board_read_count');
export const apiErrorCount = new Counter('api_error_count');

// 비율 메트릭
export const loginSuccessRate = new Rate('login_success_rate');
export const apiSuccessRate = new Rate('api_success_rate');

// 트렌드 메트릭 (API별 응답 시간)
export const loginDuration = new Trend('login_duration', true);
export const boardListDuration = new Trend('board_list_duration', true);
export const boardDetailDuration = new Trend('board_detail_duration', true);
export const boardCreateDuration = new Trend('board_create_duration', true);

// 게이지 메트릭 (현재 상태)
export const activeUsers = new Gauge('active_users');

// ==============================================
// 테스트 라이프사이클 함수
// ==============================================

/**
 * 테스트 시작 시 실행 (전역 설정)
 * setup 함수에서 호출
 */
export function logTestStart(testName) {
    console.log('='.repeat(60));
    console.log(`🚀 K6 부하 테스트 시작: ${testName}`);
    console.log(`📅 시작 시간: ${new Date().toISOString()}`);
    console.log(`🌐 대상 서버: ${BASE_URL}`);
    console.log('='.repeat(60));
}

/**
 * 테스트 종료 시 실행 (정리)
 * teardown 함수에서 호출
 */
export function logTestEnd(testName, data = null) {
    console.log('='.repeat(60));
    console.log(`✅ K6 부하 테스트 종료: ${testName}`);
    console.log(`📅 종료 시간: ${new Date().toISOString()}`);
    
    if (data && data.summary) {
        console.log(`📊 요약:`);
        console.log(`   - 총 요청: ${data.summary.totalRequests || 'N/A'}`);
        console.log(`   - 성공률: ${data.summary.successRate || 'N/A'}%`);
        console.log(`   - 평균 응답시간: ${data.summary.avgDuration || 'N/A'}ms`);
    }
    
    console.log('='.repeat(60));
}

// ==============================================
// 사용 예시 (주석)
// ==============================================

/*
 * 사용 예시:
 * 
 * import { 
 *     BASE_URL, 
 *     API_URL, 
 *     TEST_USERS,
 *     LOAD_TEST,
 *     DEFAULT_PARAMS,
 *     getAuthParams,
 *     randomThinkTime,
 *     validateResponse
 * } from './load-test-config.js';
 * 
 * export const options = LOAD_TEST;
 * 
 * export default function() {
 *     const response = http.get(`${API_URL}/boards`, DEFAULT_PARAMS);
 *     validateResponse(response, 200, 'Board List');
 *     sleep(randomThinkTime(1, 3));
 * }
 */

export default {
    BASE_URL,
    API_URL,
    TEST_USERS,
    SMOKE_TEST,
    LOAD_TEST,
    STRESS_TEST,
    SPIKE_TEST,
    SOAK_TEST,
    DEFAULT_PARAMS,
    getAuthParams,
    API_SLO,
    BASE_THRESHOLDS,
    randomThinkTime,
    randomString,
    randomNumber,
    generateBoardData,
    generateCommentData,
    validateResponse,
    parseJsonResponse,
    logTestStart,
    logTestEnd
};
