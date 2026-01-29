# Go Almond AI 매칭 MVP - JIRA Backlog

**프로젝트**: Go Almond AI Matching System  
**버전**: MVP 1.0  
**예상 기간**: 6주 (주당 10시간)  
**작성일**: 2026-01-26 (업데이트: 실제 작업 시간 반영)

---

## Story Points 산정 기준

| Story Points | 예상 시간 | 복잡도 |
|-------------|---------|--------|
| 1 SP | 1시간 | 매우 낮음 |
| 2 SP | 2시간 | 낮음 |
| 3 SP | 3시간 | 보통 |
| 4 SP | 4시간 | 높음 |
| 5 SP | 5시간 | 높음 |
| 6 SP | 6시간 | 매우 높음 |

**주당 가용 SP**: 10 SP (월~목, 19:00~21:30, 일 2.5시간)  
**실제 작업 속도**: 31시간에 AI 컨설턴트 서비스 완성 (실적 기반)

---

## 운영 원칙 및 산출물 기준

### 1) 산출물 기준 Story 규칙
- Story는 “완료된 결과물” 기준으로 정의
  - 예: “매칭 실행 API가 API 문서(Markdown) 예시/실응답으로 프론트 호출 성공”
- “작업 활동”은 Task로만 관리

### 2) 공통 Definition of Done (DoD) 템플릿
- [ ] API 문서(Markdown) 예시/스키마가 실제 응답과 일치
- [ ] 프론트에서 로컬 호출 성공 (CORS 포함)
- [ ] 에러 응답 표준(아래 5) 적용 확인
- [ ] 관련 문서/예시 업데이트 완료

### 3) Mock → 실제 전환 정책
- Mock 스키마 = 실제 스키마 (항상 동일 유지)
- 스키마 변경 시:
  - docs/api Markdown 예시/스키마 동시 수정
  - 프론트 영향 범위 기록 및 공지
- 전환 기준일을 Story로 명시 (Week 4)

### 4) 일정 보정 룰
- 1~2주 실제 작업 후 SP/시간 재보정
- 주당 10h 기준 15~20% 버퍼 포함
- 보정 결과는 Sprint 계획에 반영

### 5) 에러 응답 표준
- 모든 API는 다음 구조를 유지:
  - `success=false`, `code`, `message`, `timestamp`
- API 문서(Markdown) 예시에서 에러 응답 포함

---

## Epic 1: API 인프라 & Mock 데이터 제공

**Epic ID**: GAM-1  
**Epic Name**: Mock API 및 API 문서(Markdown) 구축  
**Business Value**: 프론트엔드 개발자가 1주차부터 작업 시작 가능  
**Target Sprint**: Week 1  
**Total Story Points**: 12 SP

---

### Story GAM-11: Mock API 명세 구현

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 6 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 1  
**Labels**: `mock-api`, `week1`, `frontend-blocker`

**Description**:
프론트엔드 개발자가 실제 로직 완성을 기다리지 않고 작업할 수 있도록 Mock API를 구현합니다.

**User Story**:
```
AS A 프론트엔드 개발자
I WANT Mock API를 통해 예상 응답 구조를 확인하고
SO THAT 백엔드 로직 완성 전에 UI를 구현할 수 있다
```

**Acceptance Criteria**:
- [ ] `POST /api/v1/matching/run` 엔드포인트 구현
  - Request: `{ "user_id": "uuid" }`
  - Response: 매칭 점수 및 Top 5 학교 반환
- [ ] `GET /api/v1/matching/result` 엔드포인트 구현
  - Response: 저장된 최신 매칭 결과 반환
- [ ] `GET /api/v1/programs?type={type}` 엔드포인트 구현
  - type: `university`, `community_college`, `vocational`
  - Response: 프로그램 목록 (10개 샘플)
- [ ] `GET /api/v1/schools/{schoolId}` 엔드포인트 구현
  - Response: 학교 상세 정보
- [ ] 3가지 시나리오 Mock 데이터 제공
  - 안정권 (매칭 점수 85+)
  - 도전권 (매칭 점수 70-84)
  - 전략 경로 (매칭 점수 60-69)

**Tasks**:
- [ ] GAM-11-1: ga-matching-service 모듈 생성 (Spring Boot)
- [ ] GAM-11-2: MockMatchingController.kt 생성
- [ ] GAM-11-3: MatchingResponse DTO 정의
- [ ] GAM-11-4: SchoolResponse DTO 정의
- [ ] GAM-11-5: ProgramResponse DTO 정의
- [ ] GAM-11-6: Mock 데이터 생성 (3가지 시나리오)

**Technical Notes**:
```kotlin
// 파일 위치
ga-matching-service/src/main/kotlin/com/goalmond/matching/
├── controller/MockMatchingController.kt
├── domain/dto/MatchingResponse.kt
├── domain/dto/SchoolResponse.kt
└── domain/dto/ProgramResponse.kt
```

**Definition of Done**:
- 모든 엔드포인트가 API 문서(docs/api) 기준으로 확인·테스트 가능
- Mock 응답이 API 명세서와 일치
- 공통 DoD 템플릿 충족

---

### Story GAM-12: API 문서(Markdown) & 프론트엔드 협업 가이드

**Story Type**: Documentation  
**Priority**: Critical  
**Story Points**: 2 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 1  
**Labels**: `documentation`, `frontend-cooperation`, `week1`

**Description**:
프론트엔드 개발자가 API 프로젝트 데이터 없이 **전달받은 링크만** 사용해 작업할 수 있도록, Markdown API 문서와 협업 가이드를 작성하고 링크로 공유합니다.

**User Story**:
```
AS A 프론트엔드 개발자
I WANT API 문서(Markdown)와 전달 링크 기반 협업 가이드를
SO THAT 백엔드 레포를 clone 하지 않고도 통합 작업을 진행할 수 있다
```

**Acceptance Criteria**:
- [ ] `docs/api/` Markdown API 문서 작성 (API.md, matching.md, programs.md, schools.md)
  - 요청/응답 예시, TypeScript 타입 예시, cURL/JavaScript Fetch 예시
  - 에러 응답 형식 설명
- [ ] `docs/FRONTEND_HANDOFF.md` 작성
  - 전달받는 링크: API Base URL, API 문서 링크(예: GitHub docs/api)
  - Cursor에서 전달받은 문서 링크로 스펙 확인 후 Base URL로 호출하는 방식 안내
- [ ] Mock 응답 예시 포함
- [ ] 인증 필요 시 방법 설명 (현재 Mock 단계는 생략 가능)

**Tasks**:
- [ ] GAM-12-1: docs/api/ API.md (공통 형식, API 목록)
- [ ] GAM-12-2: docs/api/ 엔드포인트별 Markdown (matching, programs, schools)
- [ ] GAM-12-3: docs/FRONTEND_HANDOFF.md 작성 (링크 공유 방식, Base URL, 일정)
- [ ] GAM-12-4: docs/04_FRONTEND_COOPERATION.md 참조 또는 통합
- [ ] GAM-12-5: README "프론트엔드 협업"에 FRONTEND_HANDOFF.md 링크 추가

**Definition of Done**:
- API 문서는 Markdown만 제공되며, 전달 링크로 공유 가능
- 프론트 개발자가 전달받은 링크(문서 + Base URL)만으로 개발 시작 가능
- 모든 API에 요청/응답 예시 및 TypeScript 타입 예시 포함

---

### Story GAM-13: 신규 서비스 모듈 구조 생성

**Story Type**: Task  
**Priority**: Critical  
**Story Points**: 2 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 1  
**Labels**: `infrastructure`, `setup`, `week1`

**Description**:
ga-matching-service 모듈을 생성하고 기본 인프라를 구축합니다.

**Acceptance Criteria**:
- [ ] ga-matching-service 모듈 생성
- [ ] build.gradle.kts 의존성 설정
  - Spring Boot 3.4+
  - Spring Data JPA
  - PostgreSQL Driver
  - SpringDoc OpenAPI
  - ga-common 모듈
- [ ] application.yml 설정 (dev, lightsail 프로파일)
- [ ] Docker Compose에 서비스 추가 (Port 8084)
- [ ] Health Check 엔드포인트 구현
- [ ] Gradle 빌드 성공

**Tasks**:
- [ ] GAM-13-1: settings.gradle.kts에 모듈 추가
- [ ] GAM-13-2: build.gradle.kts 작성
- [ ] GAM-13-3: MatchingServiceApplication.kt 생성
- [ ] GAM-13-4: application.yml 설정
- [ ] GAM-13-5: docker-compose.yml 업데이트
- [ ] GAM-13-6: HealthCheckController.kt 구현

**Definition of Done**:
- `./gradlew :ga-matching-service:bootRun` 정상 실행
- Health Check API 응답 확인
- Docker Compose로 전체 서비스 실행 가능

---

## Epic 2: User Profile & Preference

**Epic ID**: GAM-2  
**Epic Name**: 사용자 프로필 및 유학 목표 관리  
**Business Value**: 매칭 엔진에 필요한 사용자 데이터 수집  
**Target Sprint**: Week 2  
**Total Story Points**: 10 SP

---

### Story GAM-21: DB 스키마 설계 & 마이그레이션

**Story Type**: Story  
**Priority**: High  
**Story Points**: 4 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 2  
**Labels**: `database`, `migration`, `week2`

**Description**:
AI 매칭에 필요한 전체 데이터베이스 스키마를 설계하고 Flyway 마이그레이션을 작성합니다.

**User Story**:
```
AS A 시스템 관리자
I WANT 안정적인 데이터베이스 스키마를
SO THAT 사용자 데이터와 매칭 결과를 안전하게 저장할 수 있다
```

**Acceptance Criteria**:
- [ ] 8개 테이블 생성
  - `users` (기본 정보)
  - `user_education` (학력 정보)
  - `user_preference` (유학 목표)
  - `schools` (학교 마스터)
  - `programs` (프로그램 마스터)
  - `matching_results` (매칭 결과)
  - `applications` (지원 현황)
  - `documents` (문서 관리)
- [ ] 외래 키 제약 조건 설정
- [ ] 인덱스 설계
  - `user_id`, `tenant_id` 복합 인덱스
  - `school_id`, `program_id` 인덱스
  - `created_at` 인덱스 (최신 데이터 조회용)
- [ ] Enum 타입 정의
  - `education_level`: 초/중/고/대/대학원/검정
  - `school_type`: University/CC/Vocational/Elementary
  - `application_status`: 준비/제출/합격/거절
- [ ] Flyway 마이그레이션 성공

**Tasks**:
- [ ] GAM-21-1: V1__create_matching_tables.sql 작성
- [ ] GAM-21-2: 테이블 스키마 정의 (컬럼, 타입, 제약조건)
- [ ] GAM-21-3: 인덱스 생성 쿼리 작성
- [ ] GAM-21-4: Flyway 설정 (application.yml)
- [ ] GAM-21-5: 마이그레이션 테스트 (로컬 DB)
- [ ] GAM-21-6: ERD 다이어그램 작성 (docs/erd.png)

**Technical Notes**:
```sql
-- 주요 테이블 구조
CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(50) NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    birth_date DATE,
    nationality VARCHAR(50),
    mbti VARCHAR(4),
    personality_tags JSONB,
    bio TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_tenant ON users(tenant_id);
```

**Definition of Done**:
- Flyway 마이그레이션 성공적으로 실행
- 모든 테이블 및 인덱스 생성 확인
- ERD 문서 작성 완료

---

### Story GAM-22: User Profile API 구현

**Story Type**: Story  
**Priority**: High  
**Story Points**: 4 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 2  
**Labels**: `api`, `user-profile`, `week2`

**Description**:
사용자 프로필, 학력 정보, 유학 목표를 저장하는 API를 구현합니다.

**User Story**:
```
AS A 학생 사용자
I WANT 내 프로필과 유학 목표를 저장하고
SO THAT AI 매칭을 받을 수 있다
```

**Acceptance Criteria**:
- [ ] `PUT /api/v1/user/profile` - 기본 정보 저장/수정
  - MBTI, 성향 태그, 자기소개
- [ ] `POST /api/v1/user/education` - 학력 정보 입력
  - 학교명, 학교 지역, GPA, 영어 점수
- [ ] `POST /api/v1/user/preference` - 유학 목표 설정
  - 목표 프로그램, 희망 전공, 예산, 선호 지역
- [ ] JWT 인증 필터 적용
- [ ] Bean Validation 적용 (@NotNull, @Email, @Min, @Max)
- [ ] 에러 응답 표준화

**Tasks**:
- [ ] GAM-22-1: User Entity 생성
- [ ] GAM-22-2: UserEducation Entity 생성
- [ ] GAM-22-3: UserPreference Entity 생성
- [ ] GAM-22-4: UserRepository 인터페이스 생성
- [ ] GAM-22-5: UserProfileService 구현
- [ ] GAM-22-6: UserProfileController 구현
- [ ] GAM-22-7: Request/Response DTO 생성
- [ ] GAM-22-8: Validation 로직 추가

**Definition of Done**:
- 모든 API 엔드포인트 정상 동작
- API 문서(docs/api) 기준으로 테스트 가능
- Validation 에러 적절히 처리

---

### Story GAM-23: 단위 테스트 & 통합 테스트

**Story Type**: Task  
**Priority**: Medium  
**Story Points**: 4 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 2  
**Labels**: `testing`, `week2`

**Description**:
User Profile 기능에 대한 단위 테스트 및 통합 테스트를 작성합니다.

**Acceptance Criteria**:
- [ ] UserProfileServiceTest 작성 (MockK)
- [ ] UserProfileControllerTest 작성 (WebMvcTest)
- [ ] 테스트 커버리지 80% 이상
- [ ] 모든 테스트 통과
- [ ] Edge Case 테스트
  - 중복 저장 시도
  - 필수 필드 누락
  - 유효하지 않은 데이터

**Tasks**:
- [ ] GAM-23-1: UserProfileServiceTest.kt 작성
- [ ] GAM-23-2: UserProfileControllerTest.kt 작성
- [ ] GAM-23-3: MockK 설정 및 Mocking
- [ ] GAM-23-4: 성공 시나리오 테스트
- [ ] GAM-23-5: 실패 시나리오 테스트
- [ ] GAM-23-6: 테스트 커버리지 측정

**Definition of Done**:
- 전체 테스트 통과
- 테스트 커버리지 보고서 생성
- 주요 비즈니스 로직 테스트 완료

---

## Epic 3: AI 매칭 엔진 핵심 로직

**Epic ID**: GAM-3  
**Epic Name**: Rule-based AI 매칭 알고리즘 구현  
**Business Value**: Go Almond의 핵심 가치 제공  
**Target Sprint**: Week 3  
**Total Story Points**: 10 SP

---

### Story GAM-31: 매칭 엔진 Hard Filter 구현

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 4 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 3  
**Labels**: `matching-engine`, `core-logic`, `week3`

**Description**:
지원 불가능한 프로그램을 사전에 제거하는 Hard Filter를 구현합니다.

**User Story**:
```
AS A AI 매칭 시스템
I WANT 사용자가 지원 불가능한 프로그램을 필터링하고
SO THAT 현실적인 추천만 제공할 수 있다
```

**Acceptance Criteria**:
- [ ] 4가지 Hard Filter 구현
  1. 예산 초과 필터: `program.tuition + living_cost > user.budget_max`
  2. 비자 요건 필터: 비자 타입 호환성 확인
  3. 영어 점수 필터: TOEFL/IELTS/Duolingo 환산 후 비교
  4. 입학 시기 필터: 희망 입학 시기와 프로그램 입학 일정 매칭
- [ ] 필터링 사유 로깅
- [ ] 필터링된 프로그램 수 반환
- [ ] 영어 점수 환산 로직
  - IELTS → TOEFL 변환
  - Duolingo → TOEFL 변환

**Tasks**:
- [ ] GAM-31-1: MatchingEngineService.kt 생성
- [ ] GAM-31-2: HardFilterService.kt 생성
- [ ] GAM-31-3: 예산 초과 필터 구현
- [ ] GAM-31-4: 비자 요건 필터 구현
- [ ] GAM-31-5: 영어 점수 필터 구현
- [ ] GAM-31-6: 입학 시기 필터 구현
- [ ] GAM-31-7: 영어 점수 환산 유틸리티
- [ ] GAM-31-8: FilterResult 모델 정의

**Technical Notes**:
```kotlin
// 영어 점수 환산 로직
fun convertToToefl(score: Double, testType: TestType): Double {
    return when (testType) {
        TestType.TOEFL -> score
        TestType.IELTS -> score * 13.33 // IELTS 7.0 = TOEFL 94
        TestType.DUOLINGO -> score * 0.8 // Duolingo 120 = TOEFL 96
        else -> 0.0
    }
}
```

**Definition of Done**:
- 4가지 필터 모두 정상 동작
- 단위 테스트 작성 완료
- 필터링 로직 문서화

---

### Story GAM-32: Base Score 계산 (6대 지표)

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 5 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 3  
**Labels**: `matching-engine`, `scoring`, `week3`

**Description**:
6대 매칭 지표를 기반으로 Base Score를 계산합니다.

**User Story**:
```
AS A AI 매칭 시스템
I WANT 사용자와 프로그램의 적합성을 정량화하고
SO THAT 객관적인 매칭 점수를 제공할 수 있다
```

**Acceptance Criteria**:
- [ ] 6대 지표 계산 구현
  1. 학업 적합도 (20%) - GPA vs 입학 기준
  2. 영어 적합도 (15%) - 점수 여유
  3. 예산 적합도 (15%) - 여유도
  4. 지역 선호 (10%) - 일치도
  5. 기간 적합도 (10%) - 목표 기간
  6. 진로 연계성 (30%) - 전공/직업
- [ ] 총점 100점 계산
- [ ] 0-100 범위 검증
- [ ] 각 지표별 점수 반환
- [ ] GPA 정규화 (한국 내신 → 4.0 스케일)
- [ ] 가중치 설정 클래스

**Tasks**:
- [ ] GAM-32-1: ScoringService.kt 생성
- [ ] GAM-32-2: 학업 적합도 계산 로직
- [ ] GAM-32-3: 영어 적합도 계산 로직
- [ ] GAM-32-4: 예산 적합도 계산 로직
- [ ] GAM-32-5: 지역 선호 계산 로직
- [ ] GAM-32-6: 기간 적합도 계산 로직
- [ ] GAM-32-7: 진로 연계성 계산 로직
- [ ] GAM-32-8: WeightConfig.kt 설정 클래스
- [ ] GAM-32-9: GPA 정규화 유틸리티

**Technical Notes**:
```kotlin
data class MatchingScore(
    val totalScore: Double,
    val academicScore: Double,     // 20%
    val englishScore: Double,      // 15%
    val budgetScore: Double,       // 15%
    val locationScore: Double,     // 10%
    val durationScore: Double,     // 10%
    val careerScore: Double        // 30%
)

// 가중치 검증
fun validateWeights() {
    require(weights.sum() == 100.0) { "Total weight must be 100%" }
}
```

**Definition of Done**:
- 6대 지표 계산 정상 동작
- 가중치 합 100% 검증
- 단위 테스트 작성 완료

---

### Story GAM-33: 경로 최적화 & 리스크 패널티

**Story Type**: Story  
**Priority**: High  
**Story Points**: 1 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 3  
**Labels**: `matching-engine`, `optimization`, `week3`

**Description**:
사용자 상황에 맞는 최적 경로를 추천하고 리스크 요소에 패널티를 적용합니다.

**User Story**:
```
AS A AI 매칭 시스템
I WANT 사용자의 현실적 제약을 고려한 최적 경로를 제안하고
SO THAT 성공 가능성이 높은 추천을 제공할 수 있다
```

**Acceptance Criteria**:
- [ ] 경로 최적화 시나리오 4가지 구현
  1. GPA 낮음 + 예산 제한 → CC 경로 +10점
  2. 영어 없음 + 취업 목표 → Vocational +15점
  3. 편입 목표 + 높은 편입률 → CC +10점
  4. OPT 의사 + OPT 가능 → +5점
- [ ] 리스크 패널티 4가지 구현
  1. 입학 경쟁률 < 30% → -15점
  2. 영어 점수 임계 (차이 < 5) → -10점
  3. 예산 임계 (여유 < $5000) → -10점
  4. 체류 의사 불명확 → -5점
- [ ] 최종 점수 > 0 검증
- [ ] 추천 유형 분류 (안정권/도전권/전략)

**Tasks**:
- [ ] GAM-33-1: PathOptimizationService.kt 생성
- [ ] GAM-33-2: 경로 최적화 시나리오 구현
- [ ] GAM-33-3: 리스크 패널티 로직 구현
- [ ] GAM-33-4: 추천 유형 분류 로직
- [ ] GAM-33-5: 최종 점수 계산 및 검증

**Definition of Done**:
- 모든 시나리오 정상 동작
- 리스크 패널티 반영 확인
- 단위 테스트 작성

---

## Epic 4: Program 관리 & 매칭 실행

**Epic ID**: GAM-4  
**Epic Name**: 학교/프로그램 관리 및 매칭 API 완성  
**Business Value**: 실제 매칭 결과 제공  
**Target Sprint**: Week 4  
**Total Story Points**: 10 SP

---

### Story GAM-41: School & Program Master Data

**Story Type**: Story  
**Priority**: High  
**Story Points**: 3 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 4  
**Labels**: `master-data`, `week4`

**Description**:
학교 및 프로그램 마스터 데이터를 생성하고 관리 API를 구현합니다.

**User Story**:
```
AS A 시스템 관리자
I WANT 학교와 프로그램 데이터를 관리하고
SO THAT AI 매칭에 사용할 수 있다
```

**Acceptance Criteria**:
- [ ] School Entity 구현
- [ ] Program Entity 구현
- [ ] Mock 학교 데이터 20개 생성
  - University (5개)
  - Community College (10개)
  - Vocational School (5개)
- [ ] 시드 데이터 SQL 스크립트
- [ ] `GET /api/v1/programs?type={type}` 구현
- [ ] `GET /api/v1/schools/{schoolId}` 구현

**Tasks**:
- [ ] GAM-41-1: School.kt Entity 생성
- [ ] GAM-41-2: Program.kt Entity 생성
- [ ] GAM-41-3: SchoolRepository 생성
- [ ] GAM-41-4: ProgramRepository 생성
- [ ] GAM-41-5: seed_schools.sql 작성 (20개 학교)
- [ ] GAM-41-6: seed_programs.sql 작성 (40개 프로그램)
- [ ] GAM-41-7: SchoolService 구현
- [ ] GAM-41-8: ProgramController 구현

**Definition of Done**:
- 20개 학교 데이터 삽입 완료
- API 정상 동작
- 필터링 및 페이징 적용

---

### Story GAM-42: 매칭 실행 API 구현

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 5 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 4  
**Labels**: `matching-api`, `core-feature`, `week4`

**Description**:
사용자 요청에 따라 AI 매칭을 실행하고 결과를 반환하는 API를 구현합니다.

**User Story**:
```
AS A 학생 사용자
I WANT AI 매칭을 실행하고
SO THAT 나에게 맞는 학교 추천을 받을 수 있다
```

**Acceptance Criteria**:
- [ ] `POST /api/v1/matching/run` 구현
  - User Profile 조회
  - User Preference 조회
  - Hard Filter 실행
  - Base Score 계산
  - 경로 최적화 적용
  - 리스크 패널티 적용
  - Top 5 결과 반환
- [ ] `GET /api/v1/matching/result` 구현
  - 저장된 매칭 결과 조회
- [ ] MatchingResult Entity 구현 (결과 저장)
- [ ] 6대 지표 시각화 데이터 포함
- [ ] 추천 유형 분류 포함
- [ ] 매칭 실행 시간 < 3초

**Tasks**:
- [ ] GAM-42-1: MatchingResult.kt Entity
- [ ] GAM-42-2: MatchingResultRepository
- [ ] GAM-42-3: MatchingController.kt
- [ ] GAM-42-4: 전체 매칭 파이프라인 구현
- [ ] GAM-42-5: 결과 저장 로직
- [ ] GAM-42-6: 결과 조회 로직
- [ ] GAM-42-7: 성능 최적화 (캐싱, 인덱스)

**Technical Notes**:
```kotlin
// 매칭 파이프라인
fun executeMatching(userId: UUID): MatchingResult {
    val userProfile = userProfileService.getProfile(userId)
    val userPreference = userProfileService.getPreference(userId)
    val allPrograms = programRepository.findAll()
    
    // Step 1: Hard Filter
    val filteredPrograms = hardFilterService.filter(allPrograms, userProfile, userPreference)
    
    // Step 2: Base Score
    val scoredPrograms = scoringService.calculate(filteredPrograms, userProfile, userPreference)
    
    // Step 3: Path Optimization
    val optimizedScores = pathOptimizationService.optimize(scoredPrograms, userProfile)
    
    // Step 4: Risk Penalty
    val finalScores = riskPenaltyService.apply(optimizedScores)
    
    // Step 5: Top 5
    val topResults = finalScores.sortedByDescending { it.totalScore }.take(5)
    
    // Save and return
    return matchingResultRepository.save(MatchingResult(...))
}
```

**Definition of Done**:
- 매칭 API 정상 동작
- 결과 DB에 저장
- 성능 요구사항 충족 (< 3초)

---

### Story GAM-43: Explainable AI 설명 생성

**Story Type**: Story  
**Priority**: Medium  
**Story Points**: 2 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 4  
**Labels**: `explainable-ai`, `week4`

**Description**:
각 매칭 결과에 대한 설명을 자동으로 생성합니다.

**User Story**:
```
AS A 학생 사용자
I WANT 왜 이 학교가 추천되었는지 이해하고
SO THAT 더 신뢰할 수 있는 결정을 내릴 수 있다
```

**Acceptance Criteria**:
- [ ] 템플릿 기반 설명 생성
- [ ] 점수 기여도 상위 3개 지표 추출
- [ ] 긍정적 요소 강조
  - "예산 여유", "영어 점수 충분"
- [ ] 부정적 요소 경고
  - "경쟁률 높음", "예산 임계"
- [ ] 설명 최대 길이 200자
- [ ] 다국어 지원 준비 (i18n)

**Tasks**:
- [ ] GAM-43-1: ExplanationService.kt 생성
- [ ] GAM-43-2: 설명 템플릿 정의
- [ ] GAM-43-3: 점수 기여도 분석 로직
- [ ] GAM-43-4: 긍정/부정 요소 추출
- [ ] GAM-43-5: 설명 문구 생성 로직

**Definition of Done**:
- 모든 매칭 결과에 설명 포함
- 설명 내용이 점수와 일치
- 읽기 쉬운 문구

---

### Story GAM-44: E2E 통합 시나리오 검증

**Story Type**: Story  
**Priority**: High  
**Story Points**: 2 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 4  
**Labels**: `e2e`, `week4`, `frontend-blocker`

**Description**:
Mock → 실제 API 전환 전후의 E2E 흐름을 검증하고 프론트 호출 기준을 확정합니다.

**User Story**:
```
AS A 프론트엔드 개발자
I WANT 매칭 실행의 전체 흐름이 실제 API로 동작함을 확인하고
SO THAT Mock에서 실제 API로 안전하게 전환할 수 있다
```

**Acceptance Criteria**:
- [ ] 프로필 입력 → 매칭 실행 → 결과 조회 E2E 동작 확인
- [ ] API 문서(Markdown) 예시와 실제 응답 스키마 일치
- [ ] 프론트 로컬 호출 성공 (CORS 포함)
- [ ] 에러 응답 표준 적용 확인

**Tasks**:
- [ ] GAM-44-1: Mock/실제 스키마 비교 체크리스트 작성
- [ ] GAM-44-2: 프론트 호출 성공 기준 문서화
- [ ] GAM-44-3: E2E 호출 시나리오 테스트

**Definition of Done**:
- E2E 시나리오 통과
- Mock → 실제 전환 기준 확정

---

## Epic 5: Application 관리 & 문서

**Epic ID**: GAM-5  
**Epic Name**: 지원 현황 관리 및 문서 업로드  
**Business Value**: 사용자가 지원 과정을 추적할 수 있음  
**Target Sprint**: Week 5  
**Total Story Points**: 10 SP

---

### Story GAM-51: Application API 구현

**Story Type**: Story  
**Priority**: Medium  
**Story Points**: 4 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 5  
**Labels**: `application`, `week5`

**Description**:
학교 지원 현황을 관리하는 API를 구현합니다.

**User Story**:
```
AS A 학생 사용자
I WANT 내 지원 현황을 추적하고
SO THAT 진행 상황을 관리할 수 있다
```

**Acceptance Criteria**:
- [ ] `POST /api/v1/applications` - 지원 생성
- [ ] `GET /api/v1/applications` - 지원 현황 조회
- [ ] `PATCH /api/v1/applications/{id}/status` - 상태 변경
- [ ] Application Entity 구현
- [ ] 지원 상태 Enum (준비/제출/합격/거절)
- [ ] Progress % 자동 계산

**Tasks**:
- [ ] GAM-51-1: Application.kt Entity
- [ ] GAM-51-2: ApplicationRepository
- [ ] GAM-51-3: ApplicationService
- [ ] GAM-51-4: ApplicationController
- [ ] GAM-51-5: ApplicationStatus Enum
- [ ] GAM-51-6: Progress 계산 로직

**Definition of Done**:
- 모든 API 정상 동작
- 상태 변경 이력 관리
- 단위 테스트 작성

---

### Story GAM-52: Document 업로드

**Story Type**: Story  
**Priority**: Medium  
**Story Points**: 3 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 5  
**Labels**: `document`, `file-upload`, `week5`

**Description**:
지원 서류를 업로드하고 관리하는 기능을 구현합니다.

**User Story**:
```
AS A 학생 사용자
I WANT 지원 서류를 업로드하고
SO THAT 지원 과정을 완료할 수 있다
```

**Acceptance Criteria**:
- [ ] `POST /api/v1/documents/upload` - 파일 업로드
- [ ] `GET /api/v1/documents` - 문서 리스트
- [ ] `DELETE /api/v1/documents/{id}` - 문서 삭제
- [ ] Multipart 파일 업로드 처리
- [ ] S3 또는 로컬 스토리지 연동
- [ ] 파일 타입 검증 (PDF, JPG, PNG)
- [ ] 최대 파일 크기 제한 (10MB)

**Tasks**:
- [ ] GAM-52-1: Document.kt Entity
- [ ] GAM-52-2: DocumentRepository
- [ ] GAM-52-3: DocumentService
- [ ] GAM-52-4: FileStorageService (S3 or Local)
- [ ] GAM-52-5: DocumentController
- [ ] GAM-52-6: 파일 검증 로직
- [ ] GAM-52-7: 파일 삭제 로직

**Definition of Done**:
- 파일 업로드 정상 동작
- 파일 타입 및 크기 검증
- 업로드된 파일 다운로드 가능

---

### Story GAM-53: Dashboard API

**Story Type**: Story  
**Priority**: Low  
**Story Points**: 2 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 5  
**Labels**: `dashboard`, `week5`

**Description**:
사용자 대시보드에 표시할 요약 데이터를 제공하는 API를 구현합니다.

**User Story**:
```
AS A 학생 사용자
I WANT 대시보드에서 한눈에 현황을 파악하고
SO THAT 다음 단계를 계획할 수 있다
```

**Acceptance Criteria**:
- [ ] `GET /api/v1/dashboard` 구현
- [ ] 매칭 결과 개수
- [ ] 진행 중인 지원 현황
- [ ] 최근 활동 (최근 7일)
- [ ] 집계 쿼리 최적화

**Tasks**:
- [ ] GAM-53-1: DashboardController.kt
- [ ] GAM-53-2: DashboardService
- [ ] GAM-53-3: 집계 쿼리 작성
- [ ] GAM-53-4: DashboardResponse DTO

**Definition of Done**:
- Dashboard API 정상 동작
- 쿼리 성능 최적화 (< 500ms)

---

### Story GAM-54: 통합 테스트

**Story Type**: Task  
**Priority**: High  
**Story Points**: 1 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 5  
**Labels**: `testing`, `integration`, `week5`

**Description**:
전체 플로우에 대한 E2E 통합 테스트를 작성합니다.

**Acceptance Criteria**:
- [ ] E2E 테스트 시나리오 작성
  - 회원가입 → 프로필 입력 → 매칭 실행 → 지원 생성
- [ ] 성능 테스트
  - 매칭 실행 시간 < 3초
  - 동시 사용자 10명 처리
- [ ] 부하 테스트 (선택)

**Tasks**:
- [ ] GAM-54-1: E2E 테스트 작성
- [ ] GAM-54-2: 성능 테스트 스크립트
- [ ] GAM-54-3: 테스트 데이터 준비
- [ ] GAM-54-4: 테스트 실행 및 결과 분석

**Definition of Done**:
- E2E 테스트 통과
- 성능 요구사항 충족

---

## Epic 6: 보안 & 모니터링

**Epic ID**: GAM-6  
**Epic Name**: 보안 강화 및 모니터링 (배포는 CI/CD 자동화)  
**Business Value**: 프로덕션 준비 완료  
**Target Sprint**: Week 6  
**Total Story Points**: 7 SP

---

### Story GAM-61: 보안 강화

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 3 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 6  
**Labels**: `security`, `week6`

**Description**:
프로덕션 배포 전 보안을 강화합니다.

**Acceptance Criteria**:
- [ ] JWT 인증 필터 적용 (모든 API)
- [ ] Rate Limiting (분당 10회)
- [ ] Input Validation & Sanitization
- [ ] SQL Injection 방어 확인
- [ ] CORS 설정 (프론트 도메인만 허용)
- [ ] XSS 방어

**Tasks**:
- [ ] GAM-61-1: JwtAuthenticationFilter 적용
- [ ] GAM-61-2: RateLimitingFilter 구현
- [ ] GAM-61-3: InputValidator 구현
- [ ] GAM-61-4: CORS 설정
- [ ] GAM-61-5: 보안 체크리스트 검증

**Definition of Done**:
- 모든 보안 항목 적용
- 보안 테스트 통과

---

### Story GAM-62: 모니터링 & 로깅

**Story Type**: Task  
**Priority**: High  
**Story Points**: 2 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 6  
**Labels**: `monitoring`, `logging`, `week6`

**Description**:
모니터링 및 로깅 시스템을 구축합니다.

**Acceptance Criteria**:
- [ ] Actuator Health Check
- [ ] Prometheus Metrics 연동
- [ ] 구조화된 로깅 (JSON)
- [ ] 에러 로그 수집
- [ ] 주요 메트릭 수집
  - 매칭 실행 횟수
  - 응답 시간
  - 에러율

**Tasks**:
- [ ] GAM-62-1: Actuator 설정
- [ ] GAM-62-2: Prometheus 메트릭 추가
- [ ] GAM-62-3: 로그 포맷 설정 (JSON)
- [ ] GAM-62-4: 커스텀 메트릭 추가

**Definition of Done**:
- Health Check 정상 동작
- 메트릭 수집 확인

---

### Story GAM-63: 최종 문서화

**Story Type**: Documentation  
**Priority**: Medium  
**Story Points**: 2 SP  
**Assignee**: Backend Developer  
**Sprint**: Week 6  
**Labels**: `documentation`, `week6`

**Description**:
프로젝트 문서를 최종 정리합니다.

**Acceptance Criteria**:
- [ ] ga-matching-service/README.md 작성
- [ ] docs/04_FRONTEND_COOPERATION.md 업데이트
- [ ] API 사용 가이드
- [ ] 트러블슈팅 가이드
- [ ] Cursor 협업 가이드

**Tasks**:
- [ ] GAM-63-1: README.md 작성
- [ ] GAM-63-2: 04_FRONTEND_COOPERATION.md 업데이트
- [ ] GAM-63-3: API 사용 예시 추가
- [ ] GAM-63-4: 트러블슈팅 가이드 작성

**Definition of Done**:
- 모든 문서 작성 완료
- 프론트 개발자가 문서만으로 통합 가능

**Note**: Dockerfile, CI/CD 파이프라인은 Week 1~5 개발 중 자동으로 작성되며, Push 시 GitHub Actions가 자동 배포 처리

---

## Sprint 요약

### Sprint 1 (Week 1): Mock API & Infrastructure
- **Goal**: 프론트엔드 개발 시작 가능
- **Stories**: GAM-11, GAM-12, GAM-13
- **Total SP**: 10 SP
- **Deliverables**: Mock API, API 문서(Markdown), 협업 가이드(링크 제공)

### Sprint 2 (Week 2): User Profile & DB
- **Goal**: 사용자 데이터 수집
- **Stories**: GAM-21, GAM-22, GAM-23
- **Total SP**: 10 SP
- **Deliverables**: DB 스키마, User Profile API

### Sprint 3 (Week 3): Matching Engine Core
- **Goal**: AI 매칭 알고리즘 구현
- **Stories**: GAM-31, GAM-32, GAM-33
- **Total SP**: 10 SP
- **Deliverables**: Hard Filter, Scoring, Optimization

### Sprint 4 (Week 4): Matching API Complete
- **Goal**: 실제 매칭 결과 제공 및 E2E 전환 기준 확정
- **Stories**: GAM-41, GAM-42, GAM-43, GAM-44
- **Total SP**: 12 SP
- **Deliverables**: 매칭 API, 학교 데이터, 설명 생성, E2E 전환 기준

### Sprint 5 (Week 5): Application & Document
- **Goal**: 지원 관리 기능
- **Stories**: GAM-51, GAM-52, GAM-53, GAM-54
- **Total SP**: 10 SP
- **Deliverables**: Application API, Document Upload, Dashboard

### Sprint 6 (Week 6): Security & Monitoring
- **Goal**: 보안/모니터링 마무리 (배포는 CI/CD 자동 처리)
- **Stories**: GAM-61, GAM-62, GAM-63
- **Total SP**: 7 SP
- **Deliverables**: 보안, 모니터링, 문서화

---

## JIRA 등록 가이드

### Epic 생성
1. Project → Create Issue → Epic
2. Epic Name 입력 (예: "Mock API 및 API 문서(Markdown) 구축")
3. Business Value 명시
4. Target Sprint 설정

### Story 생성
1. Epic 하위에 Story 생성
2. Story Points 설정 (Fibonacci: 1, 2, 3, 5, 8)
3. Acceptance Criteria 명시
4. Tasks로 상세 작업 쪼개기

### Labels 활용
- `week1`, `week2`, ..., `week6`: 주차별 구분
- `mock-api`, `matching-engine`, `security`: 기능별 구분
- `frontend-blocker`: 프론트엔드 의존성
- `critical`, `high`, `medium`, `low`: 우선순위

### Workflow
1. **To Do**: 계획됨
2. **In Progress**: 진행 중
3. **Code Review**: 리뷰 요청
4. **Testing**: 테스트 중
5. **Done**: 완료

---

## 프론트엔드 협업 타임라인

| 시점 | 백엔드 상태 | 프론트엔드 작업 가능 범위 |
|------|-----------|---------------------|
| **Week 1 종료** | Mock API 제공 | ✅ 전체 UI 개발 시작 (Mock 데이터) |
| **Week 2 종료** | User Profile API 완성 | ✅ 프로필 입력 화면 연동 |
| **Week 3 종료** | 매칭 엔진 완성 | 매칭 로직 대기 |
| **Week 4 종료** | 매칭 API 완성 | ✅ 실제 매칭 결과 연동 |
| **Week 5 종료** | Application API 완성 | ✅ 지원 관리 화면 연동 |
| **Week 6 종료** | 보안/모니터링 마무리 | ✅ 통합 테스트 및 QA |

---

## 다음 단계 (백로그)

### Phase 2 (차기 버전)
- **GAM-7**: 실제 학교 데이터 크롤링/수집
- **GAM-8**: ML 기반 매칭 개선 (딥러닝)
- **GAM-9**: 실시간 합격률 피드백 반영
- **GAM-10**: 국가별 유저 패턴 학습
- **GAM-11**: 결제 시스템 통합
- **GAM-12**: 파트너(학교/에이전트) 포털

---

**문서 버전**: 1.0  
**마지막 업데이트**: 2026-01-26  
**작성자**: Go Almond Backend Team
