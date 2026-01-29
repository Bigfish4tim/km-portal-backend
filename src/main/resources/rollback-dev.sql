-- ============================================
-- KM 포털 Role 시스템 롤백 스크립트
-- ============================================
-- 📌 1일차 롤백용 (2025-01-21)
-- ============================================
--
-- 이 스크립트는 12개 Role 시스템으로의 마이그레이션이 실패했을 때
-- 기존 4개 Role 시스템으로 복원하기 위한 스크립트입니다.
--
-- ⚠️ 주의사항:
-- 1. 이 스크립트 실행 전 반드시 현재 데이터를 백업하세요.
-- 2. 12개 Role 시스템으로 생성된 신규 사용자 데이터는 손실됩니다.
-- 3. 롤백 후에는 기존 테스트 계정만 사용 가능합니다.
-- ============================================

-- ================================
-- [STEP 1] 기존 데이터 삭제
-- ================================

-- user_roles 매핑 테이블 데이터 삭제
DELETE FROM user_roles;

-- users 테이블 데이터 삭제
DELETE FROM users;

-- roles 테이블 데이터 삭제
DELETE FROM roles;

-- Identity 시드 초기화 (MSSQL)
DBCC CHECKIDENT ('roles', RESEED, 0);
DBCC CHECKIDENT ('users', RESEED, 0);

-- ================================
-- [STEP 2] 기존 역할(Role) 테이블 복원 (4개)
-- ================================

-- 시스템 관리자 역할
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_ADMIN', '시스템 관리자', '시스템 전체에 대한 모든 권한을 가진 최고 관리자', 1, 1, 1, GETDATE(), GETDATE());

-- 부서 관리자 역할
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_MANAGER', '부서 관리자', '부서 내 사용자 및 콘텐츠 관리 권한을 가진 관리자', 10, 1, 1, GETDATE(), GETDATE());

-- 게시판 관리자 역할
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_BOARD_ADMIN', '게시판 관리자', '게시판 콘텐츠 관리 및 모니터링 권한을 가진 관리자', 20, 1, 1, GETDATE(), GETDATE());

-- 일반 사용자 역할
INSERT INTO roles (role_name, display_name, description, priority, is_system_role, is_active, created_at, updated_at)
VALUES ('ROLE_USER', '일반 사용자', '기본적인 시스템 이용 권한을 가진 일반 사용자', 100, 1, 1, GETDATE(), GETDATE());

-- ================================
-- [STEP 3] 기존 사용자(User) 테이블 복원 (8명)
-- ================================

-- 시스템 관리자 계정 (비밀번호: admin123)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('admin', '$2a$12$IJEDBGWZIANqlnflc7MCZOwh1nZ0hOuRkwa.74kwELqUQCWkuLIUa', 'admin@kmportal.com', '시스템관리자', 'IT부', '시스템관리자', '010-1234-5678', 1, 0, 0, 0, GETDATE(), GETDATE());

-- 부서 관리자 계정 (비밀번호: manager123)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('manager', '$2a$12$AbAg6bJdaqjAKdXmuV2F/.D3h3wRjZc0ai3MXdG4Z5OD.j2R8/wP2', 'manager@kmportal.com', '김부장', '영업부', '부장', '010-2345-6789', 1, 0, 0, 0, GETDATE(), GETDATE());

-- 게시판 관리자 계정 (비밀번호: board123)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('board_admin', '$2a$12$aM2kyiFvui3sifrUpNN/meHjMbbxSi.pntRteL8tLg3lM9FZyVcHe', 'board@kmportal.com', '박과장', '기획부', '과장', '010-3456-7890', 1, 0, 0, 0, GETDATE(), GETDATE());

-- 일반 사용자 계정들 (비밀번호: user123)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES
('user01', '$2a$12$CscDysCO0P5T0RFkVrsP0.s4aRdr/U70OlMHonPlXxZADDIUK3ZEu', 'user01@kmportal.com', '이대리', '영업부', '대리', '010-4567-8901', 1, 0, 0, 0, GETDATE(), GETDATE()),
('user02', '$2a$12$CscDysCO0P5T0RFkVrsP0.s4aRdr/U70OlMHonPlXxZADDIUK3ZEu', 'user02@kmportal.com', '최주임', '마케팅부', '주임', '010-5678-9012', 1, 0, 0, 0, GETDATE(), GETDATE()),
('user03', '$2a$12$CscDysCO0P5T0RFkVrsP0.s4aRdr/U70OlMHonPlXxZADDIUK3ZEu', 'user03@kmportal.com', '정사원', 'HR부', '사원', '010-6789-0123', 1, 0, 0, 0, GETDATE(), GETDATE()),
('user04', '$2a$12$CscDysCO0P5T0RFkVrsP0.s4aRdr/U70OlMHonPlXxZADDIUK3ZEu', 'user04@kmportal.com', '한사원', '개발부', '사원', '010-7890-1234', 1, 0, 0, 0, GETDATE(), GETDATE());

-- 비활성 계정 (테스트용)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('inactive_user', '$2a$10$5H4Q6B7xYzQdLKfT.mNUVeQg8HtSj2nYKdOcRvPq7WnElCpS8jXYu', 'inactive@kmportal.com', '비활성사용자', '기타', '사원', '010-8901-2345', 0, 0, 0, 0, GETDATE(), GETDATE());

-- 잠금된 계정 (테스트용)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, password_expired, failed_login_attempts, created_at, updated_at)
VALUES ('locked_user', '$2a$10$5H4Q6B7xYzQdLKfT.mNUVeQg8HtSj2nYKdOcRvPq7WnElCpS8jXYu', 'locked@kmportal.com', '잠금사용자', '기타', '사원', '010-9012-3456', 1, 1, 0, 5, GETDATE(), GETDATE());

-- ================================
-- [STEP 4] 사용자-역할 매핑 복원
-- ================================

-- admin → ROLE_ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ROLE_ADMIN';

-- manager → ROLE_MANAGER
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'manager' AND r.role_name = 'ROLE_MANAGER';

-- board_admin → ROLE_BOARD_ADMIN
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'board_admin' AND r.role_name = 'ROLE_BOARD_ADMIN';

-- 일반 사용자들 → ROLE_USER
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username IN ('user01', 'user02', 'user03', 'user04', 'inactive_user', 'locked_user')
AND r.role_name = 'ROLE_USER';

-- manager에게 ROLE_USER 추가 (다중 역할 테스트)
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'manager' AND r.role_name = 'ROLE_USER';

-- ================================
-- 롤백 완료 확인 쿼리
-- ================================

-- 역할 확인 (4개여야 함)
SELECT role_id, role_name, display_name, priority FROM roles ORDER BY priority;

-- 사용자 확인 (8명이어야 함)
SELECT user_id, username, full_name FROM users ORDER BY user_id;

-- 매핑 확인
SELECT u.username, r.role_name
FROM users u
JOIN user_roles ur ON u.user_id = ur.user_id
JOIN roles r ON ur.role_id = r.role_id
ORDER BY u.username;

/*
============================================
📌 롤백 후 테스트 계정
============================================
1. admin / admin123 - 시스템 관리자
2. manager / manager123 - 부서 관리자
3. board_admin / board123 - 게시판 관리자
4. user01~04 / user123 - 일반 사용자
============================================
*/