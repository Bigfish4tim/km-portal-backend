// ==============================================
// 📁 load-tests/load-test-basic.js
// K6 기본 부하 테스트 스크립트
// 42일차 - 부하 테스트 설정 및 기본 테스트
// ==============================================

/**
 * K6 기본 부하 테스트
 * 
 * 이 스크립트는 KM 포털의 기본 API 엔드포인트에 대한
 * 부하 테스트를 수행합니다.
 * 
 * 테스트 대상:
 * - 헬스 체크 API
 * - Actuator 메트릭 API
 * - 게시판 목록 API (인증 없이 접근 가능한 경우)
 * 
 * 실행 방법:
 * 
 * 1. 연기 테스트 (Smoke Test) - 빠른 확인:
 *    k6 run load-test-basic.js
 * 
 * 2. 부하 테스트 (Load Test) - 400명 기준:
 *    k6 run --config load-test-basic.js -e TEST_TYPE=load
 * 
 * 3. 스트레스 테스트 (Stress Test) - 한계 확인:
 *    k6 run load-test-basic.js -e TEST_TYPE=stress
 * 
 * 4. HTML 리포트 생성:
 *    k6 run --out json=results.json load-test-basic.js
 */

import http from 'k6/http';
import { sleep, check, group } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// 설정 파일 임포트
import {
    BASE_URL,
    API_URL,
    SMOKE_TEST,
    LOAD_TEST,
    STRESS_TEST,
    SPIKE_TEST,
    DEFAULT_PARAMS,
    randomThinkTime,
    validateResponse,
    parseJsonResponse,
    logTestStart,
    logTestEnd,
    apiSuccessRate,
    apiErrorCount
} from './load-test-config.js';

// ==============================================
// 커스텀 메트릭 정의
// ==============================================

// API별 응답 시간 추적
const healthCheckDuration = new Trend('health_check_duration', true);
const actuatorDuration = new Trend('actuator_duration', true);
const publicApiDuration = new Trend('public_api_duration', true);

// 성공/실패 카운터
const healthCheckSuccess = new Counter('health_check_success');
const healthCheckFail = new Counter('health_check_fail');

// ==============================================
// 테스트 옵션 설정
// ==============================================

/**
 * 테스트 유형에 따른 옵션 선택
 * 환경변수 TEST_TYPE으로 제어:
 * - smoke (기본값): 연기 테스트
 * - load: 부하 테스트
 * - stress: 스트레스 테스트
 * - spike: 스파이크 테스트
 */
const testType = __ENV.TEST_TYPE || 'smoke';

// 테스트 유형별 옵션 매핑
const testOptions = {
    smoke: SMOKE_TEST,
    load: LOAD_TEST,
    stress: STRESS_TEST,
    spike: SPIKE_TEST
};

// 선택된 테스트 옵션
export const options = {
    // 기본 옵션 (smoke test가 기본)
    ...testOptions[testType],
    
    // 공통 임계값 (모든 테스트 유형에 적용)
    thresholds: {
        ...testOptions[testType].thresholds,
        
        // 헬스 체크 API는 항상 빨라야 함
        'health_check_duration': ['p(95)<200', 'avg<100'],
        
        // Actuator API 응답 시간
        'actuator_duration': ['p(95)<500', 'avg<300'],
        
        // 전체 API 성공률
        'api_success_rate': ['rate>0.95'],
        
        // 에러 카운트
        'api_error_count': ['count<100']
    },
    
    // 시나리오 정의 (더 정교한 테스트를 위해)
    scenarios: testType === 'smoke' ? undefined : {
        // 시나리오 1: 헬스 체크 (지속적)
        health_check: {
            executor: 'constant-vus',
            vus: 2,
            duration: '5m',
            exec: 'healthCheckScenario',
            tags: { scenario: 'health_check' }
        },
        
        // 시나리오 2: 일반 API 접근 (램프업)
        general_api: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: testOptions[testType].stages || [
                { duration: '1m', target: 10 },
                { duration: '2m', target: 10 },
                { duration: '1m', target: 0 }
            ],
            exec: 'generalApiScenario',
            tags: { scenario: 'general_api' }
        }
    },
    
    // 태그 (리포트에서 구분용)
    tags: {
        testType: testType,
        project: 'km-portal',
        environment: 'development'
    },
    
    // 출력 설정
    summaryTrendStats: ['avg', 'min', 'med', 'max', 'p(90)', 'p(95)', 'p(99)'],
    
    // 콘솔 출력 설정
    noConnectionReuse: false,
    userAgent: 'K6-LoadTest/1.0 (KM-Portal)',
    
    // DNS 캐싱 (성능 향상)
    dns: {
        ttl: '5m',
        select: 'random',
        policy: 'preferIPv4'
    }
};

// ==============================================
// 셋업 함수 (테스트 시작 전 1회 실행)
// ==============================================

/**
 * 테스트 시작 전 실행되는 setup 함수
 * - 서버 연결 확인
 * - 테스트 데이터 준비
 * - 전역 상태 초기화
 * 
 * @returns {object} 테스트에서 사용할 데이터
 */
export function setup() {
    logTestStart(`기본 부하 테스트 (${testType})`);
    
    console.log(`\n📋 테스트 설정:`);
    console.log(`   - 테스트 유형: ${testType}`);
    console.log(`   - 대상 URL: ${BASE_URL}`);
    console.log(`   - API URL: ${API_URL}`);
    
    // 서버 연결 확인
    console.log(`\n🔍 서버 연결 확인 중...`);
    
    const healthResponse = http.get(`${BASE_URL}/actuator/health`, {
        timeout: '10s',
        tags: { name: 'setup_health_check' }
    });
    
    if (healthResponse.status !== 200) {
        console.error(`❌ 서버 연결 실패! 상태: ${healthResponse.status}`);
        console.error(`   응답: ${healthResponse.body}`);
        throw new Error('서버에 연결할 수 없습니다. 서버가 실행 중인지 확인하세요.');
    }
    
    const healthData = parseJsonResponse(healthResponse);
    console.log(`✅ 서버 연결 성공!`);
    console.log(`   - 상태: ${healthData?.status || 'UNKNOWN'}`);
    
    // API 엔드포인트 목록 확인
    console.log(`\n📌 테스트할 API 엔드포인트:`);
    console.log(`   - GET ${BASE_URL}/actuator/health`);
    console.log(`   - GET ${BASE_URL}/actuator/info`);
    console.log(`   - GET ${BASE_URL}/actuator/metrics`);
    console.log(`   - GET ${API_URL}/health (애플리케이션 헬스)`);
    
    console.log(`\n${'='.repeat(60)}\n`);
    
    // 테스트에서 사용할 데이터 반환
    return {
        startTime: Date.now(),
        testType: testType,
        endpoints: {
            health: `${BASE_URL}/actuator/health`,
            info: `${BASE_URL}/actuator/info`,
            metrics: `${BASE_URL}/actuator/metrics`,
            appHealth: `${API_URL}/health`
        }
    };
}

// ==============================================
// 메인 테스트 함수 (VU당 반복 실행)
// ==============================================

/**
 * 기본 테스트 함수
 * 각 가상 사용자(VU)가 반복 실행
 * 
 * @param {object} data - setup에서 반환한 데이터
 */
export default function(data) {
    // 시나리오가 정의되지 않은 경우 (smoke test)
    // 모든 테스트를 순차적으로 실행
    
    // 그룹 1: 헬스 체크
    group('헬스 체크', () => {
        testHealthCheck(data);
    });
    
    // Think time (사용자 대기 시간 시뮬레이션)
    sleep(randomThinkTime(0.5, 1));
    
    // 그룹 2: Actuator 엔드포인트
    group('Actuator 메트릭', () => {
        testActuatorEndpoints(data);
    });
    
    // Think time
    sleep(randomThinkTime(1, 2));
}

// ==============================================
// 시나리오별 함수
// ==============================================

/**
 * 헬스 체크 시나리오
 * 별도 시나리오로 실행될 때 사용
 */
export function healthCheckScenario(data) {
    testHealthCheck(data);
    sleep(randomThinkTime(1, 2));
}

/**
 * 일반 API 시나리오
 * 별도 시나리오로 실행될 때 사용
 */
export function generalApiScenario(data) {
    testActuatorEndpoints(data);
    sleep(randomThinkTime(2, 4));
}

// ==============================================
// 테스트 함수들
// ==============================================

/**
 * 헬스 체크 API 테스트
 * @param {object} data - 테스트 데이터
 */
function testHealthCheck(data) {
    const startTime = Date.now();
    
    // Actuator 헬스 체크
    const response = http.get(
        data?.endpoints?.health || `${BASE_URL}/actuator/health`,
        {
            ...DEFAULT_PARAMS,
            tags: { name: 'health_check', type: 'actuator' }
        }
    );
    
    const duration = Date.now() - startTime;
    healthCheckDuration.add(duration);
    
    // 응답 검증
    const isSuccess = check(response, {
        '헬스 체크 상태 200': (r) => r.status === 200,
        '헬스 체크 응답 시간 < 200ms': (r) => r.timings.duration < 200,
        '응답에 status 필드 존재': (r) => {
            const body = parseJsonResponse(r);
            return body && body.status !== undefined;
        },
        '상태가 UP': (r) => {
            const body = parseJsonResponse(r);
            return body && body.status === 'UP';
        }
    });
    
    // 메트릭 기록
    if (isSuccess) {
        healthCheckSuccess.add(1);
        apiSuccessRate.add(1);
    } else {
        healthCheckFail.add(1);
        apiSuccessRate.add(0);
        apiErrorCount.add(1);
        
        console.warn(`⚠️ 헬스 체크 실패: status=${response.status}, duration=${duration}ms`);
    }
}

/**
 * Actuator 엔드포인트 테스트
 * @param {object} data - 테스트 데이터
 */
function testActuatorEndpoints(data) {
    // 1. Info 엔드포인트
    const infoResponse = http.get(
        data?.endpoints?.info || `${BASE_URL}/actuator/info`,
        {
            ...DEFAULT_PARAMS,
            tags: { name: 'actuator_info', type: 'actuator' }
        }
    );
    
    actuatorDuration.add(infoResponse.timings.duration);
    
    check(infoResponse, {
        'Info API 상태 200': (r) => r.status === 200,
        'Info 응답 시간 < 500ms': (r) => r.timings.duration < 500
    });
    
    if (infoResponse.status === 200) {
        apiSuccessRate.add(1);
    } else {
        apiSuccessRate.add(0);
        apiErrorCount.add(1);
    }
    
    sleep(randomThinkTime(0.3, 0.7));
    
    // 2. Metrics 목록 엔드포인트
    const metricsResponse = http.get(
        data?.endpoints?.metrics || `${BASE_URL}/actuator/metrics`,
        {
            ...DEFAULT_PARAMS,
            tags: { name: 'actuator_metrics', type: 'actuator' }
        }
    );
    
    actuatorDuration.add(metricsResponse.timings.duration);
    
    const metricsSuccess = check(metricsResponse, {
        'Metrics API 상태 200': (r) => r.status === 200,
        'Metrics 응답 시간 < 500ms': (r) => r.timings.duration < 500,
        'Metrics 목록 존재': (r) => {
            const body = parseJsonResponse(r);
            return body && body.names && body.names.length > 0;
        }
    });
    
    if (metricsSuccess) {
        apiSuccessRate.add(1);
        
        // 몇 가지 중요한 메트릭 상세 조회
        const importantMetrics = [
            'jvm.memory.used',
            'http.server.requests',
            'system.cpu.usage'
        ];
        
        // 랜덤하게 하나의 메트릭만 상세 조회 (과도한 요청 방지)
        const randomMetric = importantMetrics[Math.floor(Math.random() * importantMetrics.length)];
        
        const metricDetailResponse = http.get(
            `${BASE_URL}/actuator/metrics/${randomMetric}`,
            {
                ...DEFAULT_PARAMS,
                tags: { name: 'actuator_metric_detail', type: 'actuator', metric: randomMetric }
            }
        );
        
        publicApiDuration.add(metricDetailResponse.timings.duration);
        
        check(metricDetailResponse, {
            '메트릭 상세 조회 성공': (r) => r.status === 200
        });
        
        if (metricDetailResponse.status === 200) {
            apiSuccessRate.add(1);
        } else {
            apiSuccessRate.add(0);
        }
    } else {
        apiSuccessRate.add(0);
        apiErrorCount.add(1);
    }
}

// ==============================================
// 정리 함수 (테스트 종료 후 1회 실행)
// ==============================================

/**
 * 테스트 종료 후 실행되는 teardown 함수
 * - 테스트 결과 요약
 * - 리소스 정리
 * - 최종 리포트 출력
 * 
 * @param {object} data - setup에서 반환한 데이터
 */
export function teardown(data) {
    const endTime = Date.now();
    const totalDuration = ((endTime - (data?.startTime || endTime)) / 1000).toFixed(2);
    
    console.log(`\n${'='.repeat(60)}`);
    console.log(`📊 테스트 완료 요약`);
    console.log(`${'='.repeat(60)}`);
    console.log(`   - 테스트 유형: ${data?.testType || testType}`);
    console.log(`   - 총 소요 시간: ${totalDuration}초`);
    console.log(`   - 종료 시간: ${new Date().toISOString()}`);
    console.log(`${'='.repeat(60)}\n`);
    
    logTestEnd(`기본 부하 테스트 (${data?.testType || testType})`);
}

// ==============================================
// 결과 요약 커스터마이징
// ==============================================

/**
 * handleSummary 함수
 * 테스트 결과 요약을 커스터마이징
 * JSON, HTML 등 다양한 형식으로 출력 가능
 * 
 * @param {object} data - K6 테스트 결과 데이터
 * @returns {object} 출력 형식별 결과
 */
export function handleSummary(data) {
    // 콘솔 출력 커스터마이징
    console.log('\n');
    console.log('╔══════════════════════════════════════════════════════════╗');
    console.log('║           KM 포털 기본 부하 테스트 결과                    ║');
    console.log('╠══════════════════════════════════════════════════════════╣');
    
    // 주요 메트릭 출력
    const metrics = data.metrics;
    
    if (metrics.http_req_duration) {
        const duration = metrics.http_req_duration.values;
        console.log(`║ HTTP 요청 응답 시간:                                      ║`);
        console.log(`║   - 평균: ${duration.avg?.toFixed(2) || 'N/A'}ms                                      `);
        console.log(`║   - 중앙값: ${duration.med?.toFixed(2) || 'N/A'}ms                                    `);
        console.log(`║   - P95: ${duration['p(95)']?.toFixed(2) || 'N/A'}ms                                  `);
        console.log(`║   - P99: ${duration['p(99)']?.toFixed(2) || 'N/A'}ms                                  `);
    }
    
    if (metrics.http_reqs) {
        console.log(`║ 총 요청 수: ${metrics.http_reqs.values.count || 0}                                    `);
        console.log(`║ 초당 요청 수: ${metrics.http_reqs.values.rate?.toFixed(2) || 'N/A'}/s                  `);
    }
    
    if (metrics.http_req_failed) {
        const failRate = (metrics.http_req_failed.values.rate * 100).toFixed(2);
        console.log(`║ 실패율: ${failRate}%                                          `);
    }
    
    console.log('╚══════════════════════════════════════════════════════════╝');
    console.log('\n');
    
    // 결과 파일 생성
    const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
    
    return {
        // 표준 텍스트 출력
        'stdout': textSummary(data, { indent: '  ', enableColors: true }),
        
        // JSON 결과 파일 (상세 분석용)
        [`results/basic-test-${timestamp}.json`]: JSON.stringify(data, null, 2),
        
        // HTML 리포트 (시각화용)
        [`results/basic-test-${timestamp}.html`]: generateHtmlReport(data)
    };
}

/**
 * 텍스트 요약 생성
 * @param {object} data - 테스트 결과 데이터
 * @param {object} options - 출력 옵션
 * @returns {string} 텍스트 요약
 */
function textSummary(data, options = {}) {
    // K6 기본 텍스트 요약 사용
    // 실제로는 k6/experimental/jslib에서 textSummary를 import하여 사용
    return JSON.stringify(data.metrics, null, 2);
}

/**
 * HTML 리포트 생성
 * @param {object} data - 테스트 결과 데이터
 * @returns {string} HTML 문자열
 */
function generateHtmlReport(data) {
    const metrics = data.metrics;
    const httpDuration = metrics.http_req_duration?.values || {};
    const httpReqs = metrics.http_reqs?.values || {};
    const httpFailed = metrics.http_req_failed?.values || {};
    
    return `
<!DOCTYPE html>
<html lang="ko">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>KM 포털 부하 테스트 결과</title>
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
        h1 { 
            color: #333;
            margin-bottom: 10px;
            font-size: 28px;
        }
        .subtitle {
            color: #666;
            margin-bottom: 30px;
        }
        .metrics-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
        }
        .metric-card {
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            padding: 20px;
            border-radius: 12px;
            text-align: center;
        }
        .metric-value {
            font-size: 36px;
            font-weight: bold;
            color: #333;
        }
        .metric-label {
            color: #666;
            margin-top: 5px;
        }
        .metric-card.success { background: linear-gradient(135deg, #a8edea 0%, #fed6e3 100%); }
        .metric-card.warning { background: linear-gradient(135deg, #ffecd2 0%, #fcb69f 100%); }
        .metric-card.danger { background: linear-gradient(135deg, #ff9a9e 0%, #fecfef 100%); }
        .timestamp {
            text-align: right;
            color: #999;
            font-size: 12px;
            margin-top: 20px;
        }
        table {
            width: 100%;
            border-collapse: collapse;
            margin-top: 20px;
        }
        th, td {
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #eee;
        }
        th { background: #f8f9fa; font-weight: 600; }
        tr:hover { background: #f8f9fa; }
    </style>
</head>
<body>
    <div class="container">
        <div class="card">
            <h1>🚀 KM 포털 부하 테스트 결과</h1>
            <p class="subtitle">42일차 - 기본 부하 테스트 (${testType})</p>
            
            <div class="metrics-grid">
                <div class="metric-card success">
                    <div class="metric-value">${httpReqs.count || 0}</div>
                    <div class="metric-label">총 요청 수</div>
                </div>
                <div class="metric-card ${(httpFailed.rate || 0) < 0.05 ? 'success' : 'danger'}">
                    <div class="metric-value">${((httpFailed.rate || 0) * 100).toFixed(2)}%</div>
                    <div class="metric-label">실패율</div>
                </div>
                <div class="metric-card ${(httpDuration.avg || 0) < 500 ? 'success' : 'warning'}">
                    <div class="metric-value">${(httpDuration.avg || 0).toFixed(0)}ms</div>
                    <div class="metric-label">평균 응답 시간</div>
                </div>
                <div class="metric-card">
                    <div class="metric-value">${(httpReqs.rate || 0).toFixed(1)}/s</div>
                    <div class="metric-label">초당 요청 수</div>
                </div>
            </div>
            
            <h2 style="margin-top: 30px; margin-bottom: 15px;">📊 응답 시간 분포</h2>
            <table>
                <thead>
                    <tr>
                        <th>지표</th>
                        <th>값</th>
                        <th>목표</th>
                        <th>결과</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>최소값</td>
                        <td>${(httpDuration.min || 0).toFixed(2)}ms</td>
                        <td>-</td>
                        <td>-</td>
                    </tr>
                    <tr>
                        <td>평균</td>
                        <td>${(httpDuration.avg || 0).toFixed(2)}ms</td>
                        <td>&lt; 500ms</td>
                        <td>${(httpDuration.avg || 0) < 500 ? '✅ 통과' : '❌ 실패'}</td>
                    </tr>
                    <tr>
                        <td>중앙값 (P50)</td>
                        <td>${(httpDuration.med || 0).toFixed(2)}ms</td>
                        <td>-</td>
                        <td>-</td>
                    </tr>
                    <tr>
                        <td>P90</td>
                        <td>${(httpDuration['p(90)'] || 0).toFixed(2)}ms</td>
                        <td>-</td>
                        <td>-</td>
                    </tr>
                    <tr>
                        <td>P95</td>
                        <td>${(httpDuration['p(95)'] || 0).toFixed(2)}ms</td>
                        <td>&lt; 1000ms</td>
                        <td>${(httpDuration['p(95)'] || 0) < 1000 ? '✅ 통과' : '❌ 실패'}</td>
                    </tr>
                    <tr>
                        <td>P99</td>
                        <td>${(httpDuration['p(99)'] || 0).toFixed(2)}ms</td>
                        <td>&lt; 2000ms</td>
                        <td>${(httpDuration['p(99)'] || 0) < 2000 ? '✅ 통과' : '❌ 실패'}</td>
                    </tr>
                    <tr>
                        <td>최대값</td>
                        <td>${(httpDuration.max || 0).toFixed(2)}ms</td>
                        <td>&lt; 5000ms</td>
                        <td>${(httpDuration.max || 0) < 5000 ? '✅ 통과' : '❌ 실패'}</td>
                    </tr>
                </tbody>
            </table>
            
            <p class="timestamp">생성 시간: ${new Date().toISOString()}</p>
        </div>
    </div>
</body>
</html>
    `;
}
