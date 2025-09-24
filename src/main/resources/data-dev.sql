-- KM 포털 개발환경 초기 데이터
-- 이 파일은 개발 환경에서 애플리케이션 시작 시 자동으로 실행됩니다.
-- 테스트를 위한 사용자, 역할 데이터를 포함합니다.

-- ================================
-- 역할(Role) 테이블 초기 데이터
-- ================================

-- 시스템 관리자 역할
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_ADMIN', '시스템 관리자', '시스템 전체에 대한 모든 권한을 가진 최고 관리자', 1, true, true, NOW(), NOW());

-- 부서 관리자 역할
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_MANAGER', '부서 관리자', '부서 내 사용자 및 콘텐츠 관리 권한을 가진 관리자', 10, true, true, NOW(), NOW());

-- 게시판 관리자 역할
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_BOARD_ADMIN', '게시판 관리자', '게시판 콘텐츠 관리 및 모니터링 권한을 가진 관리자', 20, true, true, NOW(), NOW());

-- 일반 사용자 역할
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_USER', '일반 사용자', '기본적인 시스템 이용 권한을 가진 일반 사용자', 100, true, true, NOW(), NOW());

-- ================================
-- 사용자(User) 테이블 초기 데이터
-- ================================

-- 시스템 관리자 계정
-- 비밀번호: admin123 (BCrypt로 암호화된 해시)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKXIGAcPNi4hUMwrCI2GcaGYIv1i', 'admin@kmportal.com', '시스템관리자', 'IT부', '시스템관리자', '010-1234-5678', true, false, false, 0, NOW(), NOW());

-- 부서 관리자 계정
-- 비밀번호: manager123 (BCrypt로 암호화된 해시)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('manager', '$2a$10$8K8mQZ2kuDKZjQDiPYEeje6FuKQs8VdvHkh7q.zBTyNjQqKHHtCu.', 'manager@kmportal.com', '김부장', '영업부', '부장', '010-2345-6789', true, false, false, 0, NOW(), NOW());

-- 게시판 관리자 계정
-- 비밀번호: board123 (BCrypt로 암호화된 해시)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('board_admin', '$2a$10$7P2L9QDxGkWwM1vN.oFTG.zLXC.8rHUGN1kOqTyI0eKQOiV8GjQdW', 'board@kmportal.com', '박과장', '기획부', '과장', '010-3456-7890', true, false, false, 0, NOW(), NOW());

-- 일반 사용자 계정들 (테스트용)
-- 비밀번호: user123 (BCrypt로 암호화된 해시)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES
('user01', '$2a$10$5H4Q6B7xYzQdLKfT.mNUVeQg8HtSj2nYKdOcRvPq7WnElCpS8jXYu', 'user01@kmportal.com', '이대리', '영업부', '대리', '010-4567-8901', true, false, false, 0, NOW(), NOW()),
('user02', '$2a$10$5H4Q6B7xYzQdLKfT.mNUVeQg8HtSj2nYKdOcRvPq7WnElCpS8jXYu', 'user02@kmportal.com', '최주임', '마케팅부', '주임', '010-5678-9012', true, false, false, 0, NOW(), NOW()),
('user03', '$2a$10$5H4Q6B7xYzQdLKfT.mNUVeQg8HtSj2nYKdOcRvPq7WnElCpS8jXYu', 'user03@kmportal.com', '정사원', 'HR부', '사원', '010-6789-0123', true, false, false, 0, NOW(), NOW()),
('user04', '$2a$10$5H4Q6B7xYzQdLKfT.mNUVeQg8HtSj2nYKdOcRvPq7WnElCpS8jXYu', 'user04@kmportal.com', '한사원', '개발부', '사원', '010-7890-1234', true, false, false, 0, NOW(), NOW());

-- 비활성 계정 (테스트용)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('inactive_user', '$2a$10$5H4Q6B7xYzQdLKfT.mNUVeQg8HtSj2nYKdOcRvPq7WnElCpS8jXYu', 'inactive@kmportal.com', '비활성사용자', '기타', '사원', '010-8901-2345', false, false, false, 0, NOW(), NOW());

-- 잠금된 계정 (테스트용)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('locked_user', '$2a$10$5H4Q6B7xYzQdLKfT.mNUVeQg8HtSj2nYKdOcRvPq7WnElCpS8jXYu', 'locked@kmportal.com', '잠금사용자', '기타', '사원', '010-9012-3456', true, true, false, 5, NOW(), NOW());

-- ================================
-- 사용자-역할 매핑 테이블 데이터
-- ================================

-- admin 사용자에게 ROLE_ADMIN 역할 할당
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ROLE_ADMIN';

-- manager 사용자에게 ROLE_MANAGER 역할 할당
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'manager' AND r.role_name = 'ROLE_MANAGER';

-- board_admin 사용자에게 ROLE_BOARD_ADMIN 역할 할당
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'board_admin' AND r.role_name = 'ROLE_BOARD_ADMIN';

-- 일반 사용자들에게 ROLE_USER 역할 할당
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username IN ('user01', 'user02', 'user03', 'user04', 'inactive_user', 'locked_user')
AND r.role_name = 'ROLE_USER';

-- manager 사용자에게 추가로 ROLE_USER 권한도 부여 (다중 역할 테스트)
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'manager' AND r.role_name = 'ROLE_USER';

-- ================================
-- 테스트 데이터 검증 쿼리 (주석으로 남겨둠)
-- ================================

/*
-- 생성된 역할 확인
SELECT role_id, role_name, display_name, priority, is_system_role, is_active
FROM roles
ORDER BY priority;

-- 생성된 사용자 확인
SELECT user_id, username, email, full_name, department, position, is_active, is_locked
FROM users
ORDER BY user_id;

-- 사용자-역할 매핑 확인
SELECT
    u.username,
    u.full_name,
    r.role_name,
    r.display_name,
    r.priority
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
ORDER BY u.username, r.priority;

-- 부서별 사용자 수 확인
SELECT department, COUNT(*) as user_count
FROM users
WHERE is_active = true
GROUP BY department
ORDER BY user_count DESC;

-- 역할별 사용자 수 확인
SELECT
    r.display_name,
    COUNT(ur.user_id) as user_count
FROM roles r
LEFT JOIN user_roles ur ON r.role_id = ur.role_id
GROUP BY r.role_id, r.display_name
ORDER BY r.priority;
*/

-- ================================
-- 개발환경 전용 추가 데이터
-- ================================

-- 개발 모드임을 알리는 시스템 설정
-- (실제 시스템 설정 테이블이 구현되면 사용)
/*
INSERT INTO system_settings (setting_key, setting_value, description, created_at, updated_at)
VALUES
('ENVIRONMENT', 'DEVELOPMENT', '현재 환경 설정', NOW(), NOW()),
('DEMO_MODE', 'true', '데모 모드 활성화 여부', NOW(), NOW()),
('DEBUG_MODE', 'true', '디버그 모드 활성화 여부', NOW(), NOW());
*/

-- 개발용 알림 데이터 (알림 테이블 구현 시 사용)
/*
INSERT INTO notifications (title, content, type, target_user_id, is_read, created_at)
SELECT
    '시스템 시작 알림',
    'KM 포털 시스템이 정상적으로 시작되었습니다.',
    'SYSTEM',
    user_id,
    false,
    NOW()
FROM users
WHERE username = 'admin';
*/

-- ================================
-- 패스워드 정보 (개발용 참고사항)
-- ================================

/*
테스트 계정 로그인 정보:

1. 시스템 관리자
   - ID: admin
   - PW: admin123
   - 권한: 시스템 전체 관리

2. 부서 관리자
   - ID: manager
   - PW: manager123
   - 권한: 부서 관리 + 일반 사용

3. 게시판 관리자
   - ID: board_admin
   - PW: board123
   - 권한: 게시판 관리

4. 일반 사용자 (user01~user04)
   - ID: user01, user02, user03, user04
   - PW: user123
   - 권한: 기본 사용자

5. 테스트 계정
   - inactive_user: 비활성 계정 (로그인 불가)
   - locked_user: 잠금 계정 (로그인 불가)

주의: 운영환경에서는 반드시 기본 비밀번호를 변경하세요!
*/