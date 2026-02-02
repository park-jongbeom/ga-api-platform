# 백엔드 완료 항목 코드 검증 결과

**검증일**: 2026-02-02  
**기준**: [report-latest.md](report-latest.md) 완료된 백엔드 작업

---

## 원인 (GAM-55, GAM-70, GAM-71 불일치)

- **리포트 생성**: `reports/jira-generate-report.py`가 **JIRA API**로 이슈 상태를 조회해 `report-latest.md`·날짜별 보고서를 생성합니다.
- **원인**: GAM-55, GAM-70, GAM-71이 JIRA에서 **Done**으로 되어 있어 리포트 "완료된 작업"에 포함되었으나, **실제 코드는 미구현** 상태였습니다. (JIRA 상태와 코드베이스 불일치)
- **조치**: 리포트 파일을 수정해 위 3건을 "완료된 작업"에서 제거하고 "남은 작업"으로 옮겼습니다. 진행도 요약(완료 24, 남은 97, 진행률 19.8%)도 갱신했습니다.
- **권장**: JIRA에서 GAM-55, GAM-70, GAM-71을 **To Do**(또는 해당 워크플로)로 되돌려 두면, 다음에 `./reports/jira-report-local.sh` 실행 시에도 리포트가 올바르게 생성됩니다.

---

## 검증 요약

| 리포트 완료 항목 | 코드 존재 | 검증 결과 |
|------------------|-----------|-----------|
| GAM-1 Mock API 및 API 문서 구축 | ✅ | 통과 |
| GAM-11 Mock API 명세 구현 | ✅ | 통과 |
| GAM-12 API 문서 & 프론트 협업 가이드 | ✅ | 통과 |
| GAM-13 DB 인프라 구축 | ✅ | 통과 |
| GAM-20 Auth API (회원가입/로그인) | ✅ | 통과 |
| GAM-21 DB 스키마 설계 & 마이그레이션 | ✅ | 통과 |
| GAM-22 User Profile API 구현 | ✅ | 통과 |
| GAM-23 단위 테스트 & 통합 테스트 | ✅ | 통과 |
| GAM-55 Bookmark API | ❌ | **코드 없음** |
| GAM-70 FilterResult 모델 정의 | ❌ | **코드 없음** |
| GAM-71 ScoringService.kt 생성 | ❌ | **코드 없음** |

**전체 테스트**: `./gradlew test` **통과** (exit code 0)

---

## 상세 검증

### ✅ GAM-1, GAM-11 — Mock API

- **컨트롤러**: `src/main/kotlin/com/goalmond/api/controller/MockMatchingController.kt`
- **엔드포인트**:  
  - `POST /api/v1/matching/run`  
  - `GET /api/v1/matching/result`  
  - `GET /api/v1/programs?type=...`  
  - `GET /api/v1/schools/{schoolId}`

### ✅ GAM-12 — API 문서

- **위치**: `docs/api/`
- **파일**: `API.md`, `README.md`, `matching.md`, `programs.md`, `schools.md`, `auth.md`, `user-profile.md`
- **프론트 공유**: `docs/FRONTEND_HANDOFF.md` 존재

### ✅ GAM-13 — DB 인프라

- Spring Data JPA, Flyway, PostgreSQL 설정
- `application.yml` (default 제외), `application-local.yml`, `application-lightsail.yml`

### ✅ GAM-20 — Auth API

- **컨트롤러**: `AuthController.kt` — `POST /api/v1/auth/signup`, `POST /api/v1/auth/login`
- **서비스**: `AuthService.kt` (회원가입/로그인, BCrypt, JWT 발급)
- **DTO**: `SignupRequest`, `LoginRequest`, `AuthResponse` 등

### ✅ GAM-21 — DB 스키마 & 마이그레이션

- **마이그레이션**: `V1__create_schema_from_doc.sql`, `V3__add_matching_tables_and_columns.sql`
- **엔티티**: `User`, `AcademicProfile`, `UserPreference` (그 외 스키마 문서 기준 테이블)

### ✅ GAM-22 — User Profile API

- **컨트롤러**: `UserProfileController.kt`
  - `PUT /api/v1/user/profile`
  - `POST /api/v1/user/education`
  - `POST /api/v1/user/preference`
- **서비스**: `UserProfileService.kt`
- **DTO**: `ProfileUpdateRequest`, `EducationRequest`, `PreferenceRequest`

### ✅ GAM-23 — 단위/통합 테스트

- **단위**: `AuthServiceTest.kt`, `AuthControllerTest.kt`, `UserProfileServiceTest.kt`, `UserProfileControllerTest.kt`
- **통합**: `AuthAndUserProfileIntegrationTest.kt` (실제 DB 연동, 시드 계정 로그인·프로필 API 검증)

### ❌ GAM-55 — Bookmark API

- **상태**: 컨트롤러/서비스/엔드포인트 **코드 없음**
- **리포트**: 완료로 표기됨 → **미구현**

### ❌ GAM-70 — FilterResult 모델 정의

- **상태**: `FilterResult` 클래스/모델 **코드 없음**
- **리포트**: 완료로 표기됨 → **미구현**

### ❌ GAM-71 — ScoringService.kt 생성

- **상태**: `ScoringService.kt` **파일 없음**
- **리포트**: 완료로 표기됨 → **미구현**

---

## 결론

- **GAM-1, 11, 12, 13, 20, 21, 22, 23**  
  → 코드 존재·테스트 통과로 **완료 검증됨**.

- **GAM-55, GAM-70, GAM-71**  
  → 리포지토리 내 구현 없음.  
  → JIRA/리포트를 **미완료**로 수정하거나, 해당 항목 구현 후 다시 검증하는 것이 필요함.

---

## GAM-70, GAM-71 일정 검증 (2026-02-02)

- **검증 결과**: 코드 미구현(FilterResult 모델, ScoringService.kt 없음). 리포지토리 내 해당 파일·클래스 없음.
- **일정**: 보고서에 기한이 없었음 → **기한: 2026-02-12** 로 명시함 (GAM-32 Base Score·GAM-33 경로 최적화와 동일 주차, 매칭 엔진 관련 작업으로 가정).
- **리포트 반영**: `report-latest.md`, `report-2026-02-02.md` 남은 작업 내 GAM-70, GAM-71에 **(기한: 2026-02-12, 미구현)** 표기.

---

## GAM-7, GAM-8, GAM-9, GAM-10 (2026-02-02)

- 사용자 확인: 해당 스토리(Mock API 명세, API 문서, DB 인프라, DB 스키마)는 이미 완료된 범주로 봄.
- **리포트 반영**: 위 4건을 "완료된 작업"으로 옮기고, "남은 작업"에서 제거. 진행도 요약 갱신(완료 28, 남은 93, 진행률 23.1%).

---

## 리포트 vs JIRA 불일치 (GAM-51 등)

### 왜 보고서의 "남은 작업"과 JIRA가 다르게 보이는가

1. **리포트는 “한 번 생성된 뒤 수동 수정”된 상태**
   - `report-latest.md`는 **JIRA API로 생성**된 뒤, GAM-55/70/71 이동, GAM-7~10 완료 반영 등 **수동 편집**이 가해진 파일입니다.
   - JIRA에서 이슈를 완료/재오픈해도 **리포트 파일은 자동으로 갱신되지 않습니다.**

2. **JIRA와 리포트를 맞추는 방법**
   - **`./reports/jira-report-local.sh`** 를 실행하면 **현재 JIRA 상태**를 기준으로 `report-latest.md`·날짜별 보고서가 **덮어쓰기**됩니다.
   - 실행 후에는 “완료/남은 작업” 구분이 JIRA와 일치합니다. (단, 수동으로 넣었던 GAM-7~10 완료·GAM-55/70/71 남은 작업 등은 JIRA 상태에 따라 다시 바뀔 수 있음.)

### GAM-51 (Application API 구현)의 경우

- **JIRA**: GAM-51이 **완료(Done)** 로 보이고, 기한 2월 25일로 표시됨.
- **현재 리포트**: GAM-51이 **남은 작업**에 있음 → 위 1번 때문에, **예전 JIRA 스냅샷 + 수동 수정** 기준이라 JIRA에서 Done으로 바뀐 뒤 리포트를 다시 만들지 않았기 때문.
- **코드 검증**: **Application API**(지원 현황 등) 관련 컨트롤러/서비스는 **리포지토리에 없음**. (GAM-55 Bookmark API와 같은 “JIRA만 Done, 코드 미구현” 유형.)

**선택지**

- **A) JIRA를 실제 작업 상태에 맞추고 싶을 때**  
  GAM-51을 JIRA에서 **To Do(또는 해당 상태)** 로 되돌린 뒤, `./reports/jira-report-local.sh` 로 리포트 재생성 → 리포트에는 계속 “남은 작업”으로 나옴.
- **B) JIRA를 기준으로 리포트만 맞추고 싶을 때**  
  `./reports/jira-report-local.sh` 만 실행 → JIRA에서 GAM-51이 Done이면 리포트 “완료된 작업”에 포함됨. (코드 미구현은 별도 이슈로 관리.)
