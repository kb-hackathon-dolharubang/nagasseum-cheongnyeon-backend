# nagasseum-cheongnyeon (backend)

MZ세대의 독립 준비를 위한 자산 관리 플랫폼 **나갔음 청년**의 백엔드 레포지토리입니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Framework 5.3 |
| SQL Mapper | MyBatis |
| Build | Maven (WAR Packaging) |
| WAS | Tomcat 9.0.118 |
| DB | MySQL 8.0 |
| Cache | Redis 7 |
| Auth | Kakao OAuth2 + JWT |
| CI/CD | GitHub Actions → Docker Hub |

---

## 로컬 실행

### 사전 준비

- Java 17
- Docker
- Apache Tomcat 9.0.x
- `.env` 파일 생성 (`.env.example` 참고)

### 실행 순서

```bash
# 1. MySQL, Redis 실행
docker compose up -d

# 2. 빌드
./mvnw clean package -DskipTests

# 3. IntelliJ Tomcat 실행 설정
# VM options: -Dspring.profiles.active=local,api -Duser.dir={프로젝트_루트_경로}
# Deployment: target/nagasseum-cheongnyeon-backend.war → Application context: /
```
