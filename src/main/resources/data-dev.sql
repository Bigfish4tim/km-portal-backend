-- ============================================
-- KM 손해사정 포털 개발환경 초기 데이터
-- ============================================
-- 📌 1일차 수정 (2025-01-21)
-- - 기존 4개 Role → 12개 Role로 확장
-- - 기존 8명 사용자 → 16명 테스트 사용자로 변경
-- - 손해사정 업무에 맞는 새로운 데이터 구조 적용
-- ============================================
-- 📌 H2 데이터베이스 호환 버전
-- - MSSQL 전용 명령어(DBCC CHECKIDENT) 제거
-- - H2/MSSQL 공통 SQL 문법 사용
-- ============================================
--
-- 이 파일은 개발 환경에서 애플리케이션 시작 시 자동으로 실행됩니다.
--
-- 마이그레이션 순서 (ROL-MIG-001~003):
-- 1. user_roles 테이블 데이터 삭제
-- 2. users 테이블 데이터 삭제
-- 3. roles 테이블 데이터 삭제
-- 4. 새로운 12개 Role INSERT
-- 5. 새로운 16명 테스트 사용자 INSERT
-- 6. user_roles 매핑 INSERT
-- ============================================

-- ================================
-- [STEP 1~3] 기존 데이터 삭제 (마이그레이션 순서 준수)
-- ================================
-- 외래 키 제약 조건으로 인해 반드시 이 순서로 삭제해야 합니다.
-- 1) user_roles (매핑 테이블 먼저)
-- 2) users (사용자 테이블)
-- 3) roles (역할 테이블)

-- user_roles 매핑 테이블 데이터 삭제
DELETE FROM user_roles;

-- users 테이블 데이터 삭제
DELETE FROM users;

-- roles 테이블 데이터 삭제
DELETE FROM roles;

-- ================================
-- [STEP 4] 역할(Role) 테이블 초기 데이터 (12개)
-- ================================
-- 요구사항 명세서 ROL-ENT-001에 따른 12개 Role 정의
-- 제약사항 ROL-CON-004: 모든 Role은 is_system_role = true

-- [1] 관리자 역할 (우선순위: 1)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_ADMIN', '관리자', '시스템 전체에 대한 모든 권한을 가진 최고 관리자', 1, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [2] 경영지원 역할 (우선순위: 5)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_BUSINESS_SUPPORT', '경영지원', '보험사 의뢰 접수, 조사자 배당, 보고서 전송 업무를 담당하는 경영지원 역할', 5, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [3] 임원(1/4종) 역할 (우선순위: 10)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_EXECUTIVE_ALL', '임원(1/4종)', '1종과 4종 모든 손해사정 업무를 총괄하고 보고서를 검토하는 임원', 10, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [4] 임원(1종) 역할 (우선순위: 11)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_EXECUTIVE_TYPE1', '임원(1종)', '1종 손해사정 업무를 총괄하고 보고서를 검토하는 임원', 11, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [5] 임원(4종) 역할 (우선순위: 12)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_EXECUTIVE_TYPE4', '임원(4종)', '4종 손해사정 업무를 총괄하고 보고서를 검토하는 임원', 12, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [6] 팀장(1/4종) 역할 (우선순위: 20)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_TEAM_LEADER_ALL', '팀장(1/4종)', '1종과 4종 손해사정 업무를 수행하고 팀원 보고서를 검토하는 팀장', 20, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [7] 팀장(1종) 역할 (우선순위: 21)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_TEAM_LEADER_TYPE1', '팀장(1종)', '1종 손해사정 업무를 수행하고 팀원 보고서를 검토하는 팀장', 21, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [8] 팀장(4종) 역할 (우선순위: 22)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_TEAM_LEADER_TYPE4', '팀장(4종)', '4종 손해사정 업무를 수행하고 팀원 보고서를 검토하는 팀장', 22, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [9] 조사자(1/4종) 역할 (우선순위: 30)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_INVESTIGATOR_ALL', '조사자(1/4종)', '1종과 4종 손해사정 현장 조사 및 보고서 작성을 담당하는 조사자', 30, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [10] 조사자(1종) 역할 (우선순위: 31)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_INVESTIGATOR_TYPE1', '조사자(1종)', '1종 손해사정 현장 조사 및 보고서 작성을 담당하는 조사자', 31, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [11] 조사자(4종) 역할 (우선순위: 32)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_INVESTIGATOR_TYPE4', '조사자(4종)', '4종 손해사정 현장 조사 및 보고서 작성을 담당하는 조사자', 32, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [12] 일반사원 역할 (우선순위: 100)
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_EMPLOYEE', '일반사원', '업무 권한이 아직 부여되지 않은 일반 사원 (분류 및 대기용)', 100, true, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ================================
-- [STEP 5] 사용자(User) 테이블 초기 데이터 (16명)
-- ================================
-- 요구사항 명세서 ROL-MIG-003에 따른 16명 테스트 사용자
-- 비밀번호는 BCrypt로 암호화된 해시값 사용

-- ============================================
-- 📌 비밀번호 해시 정보
-- ============================================
-- 모든 테스트 계정 비밀번호: password123
-- BCrypt 해시: $2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi
-- ============================================

-- [1] 관리자 계정 (비밀번호: password123)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('admin', '$2a$12$BnnlnjhGmWb/Gl8jcSElsOZF2U3hfhOq5tifcNqF6./.lKHj5NYny', 'admin@kmportal.com', '관리자', 'IT부', '시스템관리자', '010-1234-5678', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [2-3] 경영지원 계정 2명
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('support01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'support01@kmportal.com', '경영지원1', '경영지원부', '대리', '010-2001-0001', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('support02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'support02@kmportal.com', '경영지원2', '경영지원부', '주임', '010-2001-0002', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [4-6] 임원 계정 3명
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('exec_all', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'exec_all@kmportal.com', '임원(1/4종)', '임원실', '이사', '010-3001-0001', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('exec_type1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'exec_type1@kmportal.com', '임원(1종)', '임원실', '상무', '010-3001-0002', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('exec_type4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'exec_type4@kmportal.com', '임원(4종)', '임원실', '전무', '010-3001-0003', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [7-9] 팀장 계정 3명
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('leader_all', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'leader_all@kmportal.com', '팀장(1/4종)', '조사1팀', '팀장', '010-4001-0001', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('leader_type1', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'leader_type1@kmportal.com', '팀장(1종)', '1종조사팀', '팀장', '010-4001-0002', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('leader_type4', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'leader_type4@kmportal.com', '팀장(4종)', '4종조사팀', '팀장', '010-4001-0003', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [10-15] 조사자 계정 6명
-- 조사자(1/4종) 2명
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('invest_all_01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'invest_all_01@kmportal.com', '조사자A(1/4종)', '조사1팀', '과장', '010-5001-0001', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('invest_all_02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'invest_all_02@kmportal.com', '조사자B(1/4종)', '조사1팀', '대리', '010-5001-0002', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 조사자(1종) 2명
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('invest_type1_01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'invest_type1_01@kmportal.com', '조사자A(1종)', '1종조사팀', '과장', '010-5002-0001', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('invest_type1_02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'invest_type1_02@kmportal.com', '조사자B(1종)', '1종조사팀', '대리', '010-5002-0002', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 조사자(4종) 2명
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('invest_type4_01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'invest_type4_01@kmportal.com', '조사자A(4종)', '4종조사팀', '과장', '010-5003-0001', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('invest_type4_02', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'invest_type4_02@kmportal.com', '조사자B(4종)', '4종조사팀', '대리', '010-5003-0002', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- [16] 일반사원 계정 1명
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('employee01', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi', 'employee01@kmportal.com', '일반사원', '총무부', '사원', '010-6001-0001', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ================================
-- [STEP 6] 사용자-역할 매핑 테이블 데이터
-- ================================
-- 각 사용자에게 해당하는 역할을 할당합니다.

-- [1] admin → ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ROLE_ADMIN';

-- [2-3] support01, support02 → ROLE_BUSINESS_SUPPORT
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'support01' AND r.role_name = 'ROLE_BUSINESS_SUPPORT';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'support02' AND r.role_name = 'ROLE_BUSINESS_SUPPORT';

-- [4-6] 임원 → 각각의 ROLE_EXECUTIVE_* 역할
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'exec_all' AND r.role_name = 'ROLE_EXECUTIVE_ALL';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'exec_type1' AND r.role_name = 'ROLE_EXECUTIVE_TYPE1';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'exec_type4' AND r.role_name = 'ROLE_EXECUTIVE_TYPE4';

-- [7-9] 팀장 → 각각의 ROLE_TEAM_LEADER_* 역할
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'leader_all' AND r.role_name = 'ROLE_TEAM_LEADER_ALL';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'leader_type1' AND r.role_name = 'ROLE_TEAM_LEADER_TYPE1';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'leader_type4' AND r.role_name = 'ROLE_TEAM_LEADER_TYPE4';

-- [10-15] 조사자 → 각각의 ROLE_INVESTIGATOR_* 역할
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'invest_all_01' AND r.role_name = 'ROLE_INVESTIGATOR_ALL';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'invest_all_02' AND r.role_name = 'ROLE_INVESTIGATOR_ALL';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'invest_type1_01' AND r.role_name = 'ROLE_INVESTIGATOR_TYPE1';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'invest_type1_02' AND r.role_name = 'ROLE_INVESTIGATOR_TYPE1';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'invest_type4_01' AND r.role_name = 'ROLE_INVESTIGATOR_TYPE4';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'invest_type4_02' AND r.role_name = 'ROLE_INVESTIGATOR_TYPE4';

-- [16] 일반사원 → ROLE_EMPLOYEE
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'employee01' AND r.role_name = 'ROLE_EMPLOYEE';

-- ================================
-- 패스워드 정보 (개발용 참고사항)
-- ================================

/*
============================================
📌 테스트 계정 로그인 정보 (1일차 수정)
============================================

⭐ 모든 계정 공통 비밀번호: password123

[관리자]
- ID: admin

[경영지원] (2명)
- ID: support01, support02

[임원] (3명)
- ID: exec_all (1/4종), exec_type1 (1종), exec_type4 (4종)

[팀장] (3명)
- ID: leader_all (1/4종), leader_type1 (1종), leader_type4 (4종)

[조사자] (6명)
- ID: invest_all_01, invest_all_02 (1/4종)
- ID: invest_type1_01, invest_type1_02 (1종)
- ID: invest_type4_01, invest_type4_02 (4종)

[일반사원] (1명)
- ID: employee01

============================================
*/