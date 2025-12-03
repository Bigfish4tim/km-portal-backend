# 파일 상태 확인
git status

# 모든 파일을 스테이징 영역에 추가
git add .

# 초기 커밋
git commit -m "feat: Initial Spring Boot 3.5.5 project setup

- Add Spring Boot 3.5.5 with Maven configuration
- Configure H2 database for development
- Add JWT authentication dependencies
- Setup Spring Security and JPA
- Configure application properties for dev/prod environments
- Add Actuator for monitoring
- Create HealthController for status check"



# git push cmd
git push -u origin main


# 매일 작업 시작 전
git status       
// 현재 상태 확인
git pull origin main
// 최신 코드 받기

# 작업 중
git add .
// 변경사항 스테이징
git commit -m "feat: Add login functionality"
// 커밋

# 작업 완료 후
git push origin main
// 원격 저장소에 푸시


# 백업 및 불러오기 #

# 커밋 히스토리 확인
git log --oneline

# 특정 커밋으로 되돌리기 (변경사항 유지)
git reset --soft [커밋 해시]

# 특정 커밋으로 완전히 되돌리기 (변경사항 삭제)
git reset --hard [커밋 해시]

# 예시
git reset --hard abc1234


# 빌드 명령어 #

.\mvnw.cmd spring-boot:run