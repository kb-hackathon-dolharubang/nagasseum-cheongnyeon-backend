# 나갔음청년 — Backend

청년의 주거 독립을 자산, 소득, 정부 정책, 전문 상담까지 하나로 잇는 **데이터 기반 청년 자립 지원 플랫폼**입니다.
실제 금융 데이터로 *"언제, 어떤 조건으로 독립할 수 있는가"* 를 진단하고, 받을 수 있는 정책을 실데이터로 판정하며, 남는 고민은 선배나 전문가 상담으로 연결합니다.

---

## 핵심 흐름

```
① 진단 (자산 연동, 주거 목표 진단, 또래 비교) -> ② 정책 판정 (정책 사전 자격 판정) -> ③ 상담 연결 (전문가, 멘토 예약, 상담 요약 생성)
```

---

## 기능 구성

| 기능 | 설명                                                 | 담당 도메인 |
|---|----------------------------------------------------|---|
| **자산 연동** | CODEF 마이데이터로 은행, 카드, 대출 실계좌 연동 및 순자산 집계            | `asset` |
| **주거 목표 진단** | 목표 보증금과 현재 자산을 비교해 달성 가능성 판정                       | `goal` |
| **몬테카를로 시뮬레이션** | 국토부 실거래가의 변동성을 반영해 목표 시점 달성 확률 산출                  | `goal` |
| **주거 추천 알고리즘** | Realistic, Preference, HoldOut 3가지 알고리즘으로 대안 카드 제시 | `goal` |
| **부동산 시세 수집** | 국토부 실거래가 API로 전월세, 매매 수집 및 중위시세 제공                 | `property` |
| **또래 비교** | 동일 연령, 소득 구간 사용자의 자산, 목표와 익명 비교                    | `compare` |
| **정책 사전 자격 판정** | 실 자산, 소득 데이터로 청년 정책 수혜 여부를 사전 판정                   | `ai/eligibility` |
| **AI 상담 응대** | LLM 기반 정책 Q&A 및 전문 판단이 필요한 경우 상담자에게 전달             | `ai/summary` |
| **상담 요약 자동 생성** | AI가 대화 맥락을 요약해 전문가, 멘토에게 전달                        | `ai/summary` |
| **전문가·멘토 연결** | 선배 청년 멘토 및 금융 전문가 상담 예약                            | `consultation` |

---

## 정책 사전 자격 판정 대상

코드 기반 핵심 요건 판정(Rule) + LLM 보조 판정 2단계 구조로 구현됩니다.
정책이 늘어도 판정 오케스트레이션 코드는 변경 없이 `PolicyDefinition` 구현체만 추가합니다.

| 정책명 | 소관 | 판정 방식 |
|---|---|---|
| 청년 전용 버팀목 전세자금대출 | 국토교통부 | Rule + LLM |
| 내집마련 디딤돌 대출 | 국토교통부 | Rule + LLM |

---

## 기술 스택

| 구분 | 기술                          |
|---|-----------------------------|
| Language | Java 17                     |
| Framework | Spring Framework 5.3        |
| SQL Mapper | MyBatis                     |
| Build | Maven                       |
| WAS | Tomcat 9.0.118              |
| DB | MySQL 8.0                   |
| Cache | Redis 7                     |
| Auth | Kakao OAuth2 + JWT          |

---

## 외부 데이터 연동

| 연동 대상 | 용도                                 |
|---|------------------------------------|
| **CODEF API** | 은행, 카드, 대출 계좌 연동          |
| **국토교통부 실거래가 API** | 아파트, 연립다세대, 오피스텔, 단독다가구 전월세, 매매 수집 |
| **온통청년 오픈API** | 청년 정책 데이터 조회                       |
| **LLM API** | 정책 판정 보조 및 상담 요약 자동 생성             |
| **Kakao OAuth2** | 소셜 로그인                             |

---

## 도메인 구조

```
com.team.independence
├── member        회원 관리
├── auth          Kakao OAuth2 + JWT 인증
├── asset         자산 연동 (CODEF API), 순자산 집계
├── goal          목표 설정, 몬테카를로 시뮬레이션, 주거 추천 알고리즘
├── property      국토부 실거래가 수집 (전월세, 매매), 중위시세 조회
├── policy        공공 정책 목록 조회
├── compare       또래 비교, 월별 스냅샷
├── ai
│   ├── eligibility   정책 사전 자격 판정 (Rule + LLM)
│   └── summary       AI 상담 응대 및 상담 요약 자동 생성
├── consultation  전문가, 멘토 상담 예약
├── scheduler     배치 스케줄러 (batch 프로파일 전용)
├── external      외부 API 클라이언트 (CODEF, MOLIT, 온통청년)
├── common        공통 응답, 예외, 보안
└── config        설정
```

---

## 프로파일

같은 WAR 파일을 두 역할로 나눠 실행합니다. 서비스 및 Mapper는 두 프로파일 모두에서 로드됩니다.

| 프로파일 | 용도 |
|---|---|
| `api` | 사용자 요청 처리 (Controller 활성화) |
| `batch` | 야간 배치 작업 (Scheduler 활성화, `@Scheduled` 동작) |

---

## 로컬 실행

### 사전 준비

- Java 17
- Docker
- Apache Tomcat 9.0.118
- `.env` 파일 생성 (`.env.example` 참고)

### 실행 순서

```bash
# 1. DB · Redis 실행
docker compose up -d

# 2. DB 스키마 초기화 (최초 1회)
# sql/ 디렉터리의 파일을 순서대로 실행
# 01_full_schema.sql
# 02_institution_master.sql
# 03_region_initialize.sql
# 04_mock_compare_data.sql
# 05_region_dong_initialize.sql

# 3. 빌드
./mvnw clean package -DskipTests

# 4. IntelliJ Tomcat 실행 설정
# VM options : -Dspring.profiles.active=local,api
# Deployment : target/independence-backend.war → Application context: /
```

### 헬스 체크

```
GET /api/v1/members/health
→ {"success": true, "data": "OK", "error": null}
```

---

## 응답 형식

```json
// 성공
{ "success": true,  "data": { ... }, "error": null }

// 실패
{ "success": false, "data": null,    "error": { "code": "GOAL_001", "message": "..." } }
```
