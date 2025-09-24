-- ==============================================
-- 📁 src/main/resources/data-dev.sql
-- 개발환경 초기 데이터 삽입 스크립트
-- ==============================================

-- 주의사항:
-- 1. H2 Database 문법에 맞춰 작성
-- 2. 개발환경에서만 실행됨 (application-dev.yml 설정)
-- 3. Spring Boot 시작시 자동으로 실행됨
-- 4. 테이블은 JPA가 자동으로 생성하므로 CREATE TABLE 문 불필요

-- ==============================================
-- 역할(Role) 초기 데이터 삽입
-- ==============================================

-- 시스템 관리자 역할
INSERT INTO roles (role_name, display_name, description, is_active, is_system_role, priority, created_at, updated_at)
VALUES ('ROLE_ADMIN', '시스템 관리자', '모든 시스템 기능에 대한 전체 권한을 가진 관리자', true, true, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 부서 관리자 역할
INSERT INTO roles (role_name, display_name, description, is_active, is_system_role, priority, created_at, updated_at)
VALUES ('ROLE_MANAGER', '부서 관리자', '부서 내 사용자 관리 및 게시판 관리 권한', true, true, 10, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 게시판 관리자 역할
INSERT INTO roles (role_name, display_name, description, is_active, is_system_role, priority, created_at, updated_at)
VALUES ('ROLE_BOARD_ADMIN', '게시판 관리자', '게시판 및 댓글 관리 권한', true, true, 20, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 일반 사용자 역할
INSERT INTO roles (role_name, display_name, description, is_active, is_system_role, priority, created_at, updated_at)
VALUES ('ROLE_USER', '일반 사용자', '기본적인 포털 사용 권한 (게시글 작성, 파일 업로드 등)', true, true, 100, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================
-- 사용자(User) 초기 데이터 삽입
-- ==============================================

-- 비밀번호 참고: 모든 계정의 비밀번호는 "password123" 입니다.
-- BCrypt로 암호화된 해시값: $2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.
-- 실제 운영환경에서는 더 복잡한 비밀번호 사용 필요

-- 1. 시스템 관리자 (admin)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'admin@kmportal.com', '시스템 관리자', 'IT팀', '팀장', '02-1234-5678', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 2. IT팀 매니저 (itmanager)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('itmanager', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'itmanager@kmportal.com', '김철수', 'IT팀', '과장', '02-1234-5679', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 3. 인사팀 매니저 (hrmanager)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('hrmanager', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'hrmanager@kmportal.com', '이영희', '인사팀', '과장', '02-1234-5680', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 4. 게시판 관리자 (boardadmin)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('boardadmin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'boardadmin@kmportal.com', '박미영', '기획팀', '대리', '02-1234-5681', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 5. 일반 사용자들 (개발 테스트용)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('user1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'user1@kmportal.com', '최승호', '영업팀', '사원', '02-1234-5682', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('user2', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'user2@kmportal.com', '정수진', '마케팅팀', '사원', '02-1234-5683', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('user3', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'user3@kmportal.com', '강민수', '재무팀', '주임', '02-1234-5684', true, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 6. 비활성화된 사용자 (테스트용)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('inactiveuser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'inactive@kmportal.com', '비활성사용자', '퇴사', '전직원', '', false, false, false, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 7. 잠긴 계정 (테스트용)
INSERT INTO users (username, password, email, full_name, department, position, phone_number, is_active, is_locked, is_expired, failed_login_attempts, created_at, updated_at)
VALUES ('lockeduser', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2uheWG/igi.', 'locked@kmportal.com', '잠긴계정', 'IT팀', '사원', '02-1234-5685', true, true, false, 5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- ==============================================
-- 사용자-역할 연결 (user_roles) 데이터 삽입
-- ==============================================

-- 관리자에게 모든 권한 부여 (ADMIN + MANAGER + BOARD_ADMIN + USER)
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ROLE_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'admin' AND r.role_name = 'ROLE_USER';

-- IT 매니저에게 매니저 + 사용자 권한
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'itmanager' AND r.role_name = 'ROLE_MANAGER';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'itmanager' AND r.role_name = 'ROLE_USER';

-- 인사팀 매니저에게 매니저 + 사용자 권한
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'hrmanager' AND r.role_name = 'ROLE_MANAGER';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'hrmanager' AND r.role_name = 'ROLE_USER';

-- 게시판 관리자에게 게시판 관리 + 사용자 권한
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'boardadmin' AND r.role_name = 'ROLE_BOARD_ADMIN';

INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username = 'boardadmin' AND r.role_name = 'ROLE_USER';

-- 일반 사용자들에게 사용자 권한만 부여
INSERT INTO user_roles (user_id, role_id)
SELECT u.user_id, r.role_id
FROM users u, roles r
WHERE u.username IN ('user1', 'user2', 'user3', 'inactiveuser', 'lockeduser')
AND r.role_name = 'ROLE_USER';

-- ==============================================
-- 데이터 삽입 완료 로그
-- ==============================================

-- H2 Database에서는 주석도 실행되지 않으므로 SELECT 문으로 확인
-- 개발자 콘솔에서 데이터 삽입 결과 확인 가능

-- 삽입된 역할 수 확인
-- SELECT '역할 삽입 완료 - 총 개수:' as message, COUNT(*) as count FROM roles;

-- 삽입된 사용자 수 확인
-- SELECT '사용자 삽입 완료 - 총 개수:' as message, COUNT(*) as count FROM users;

-- 사용자-역할 연결 수 확인
-- SELECT '사용자-역할 연결 완료 - 총 개수:' as message, COUNT(*) as count FROM user_roles;

-- ==============================================
-- 테스트용 계정 정보 요약
-- ==============================================

/*
생성된 테스트 계정 정보:

1. admin / password123
   - 역할: 시스템 관리자
   - 권한: 모든 기능 접근 가능
   - 부서: IT팀

2. itmanager / password123
   - 역할: 부서 관리자
   - 권한: 부서 관리, 사용자 관리
   - 부서: IT팀

3. hrmanager / password123
   - 역할: 부서 관리자
   - 권한: 부서 관리, 사용자 관리
   - 부서: 인사팀

4. boardadmin / password123
   - 역할: 게시판 관리자
   - 권한: 게시판 관리, 댓글 관리
   - 부서: 기획팀

5. user1 / password123
   - 역할: 일반 사용자
   - 권한: 기본 포털 기능
   - 부서: 영업팀

6. user2 / password123
   - 역할: 일반 사용자
   - 부서: 마케팅팀

7. user3 / password123
   - 역할: 일반 사용자
   - 부서: 재무팀

8. inactiveuser / password123
   - 상태: 비활성화 (로그인 불가)
   - 테스트용: 계정 상태 테스트

9. lockeduser / password123
   - 상태: 계정 잠금 (로그인 불가)
   - 테스트용: 계정 잠금 테스트

개발 환경에서 이 계정들로 다양한 권한 레벨 테스트 가능
*/