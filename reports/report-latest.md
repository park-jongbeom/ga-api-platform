# 프로젝트 진행 상황 보고서

**프로젝트**: Go Almond Matching  
**보고일**: 2026-02-03  
**작업물 확인**: [https://go-almond.ddnsfree.com/](https://go-almond.ddnsfree.com/)

---

## 진행도 요약

| 구분 | 건수 |
|------|------|
| 전체 이슈 | 208 |
| 완료 | 47 |
| 진행 중 | 1 |
| 남은 작업 | 160 |

| 백엔드 | 121 |
| 프론트엔드 | 87 |

**진행률**: `████░░░░░░░░░░░░░░░░` **22.6%**

---

## 전체 일정 (Epic 타임라인)

### 백엔드

- **GAM-1** Mock API 및 Swagger 문서 구축 ~ 2026-01-27
- **GAM-2** 사용자 프로필 및 유학 목표 관리 ~ 2026-02-03
- **GAM-3** Rule-based AI 매칭 알고리즘 구현 ~ 2026-02-10
- **GAM-4** 학교/프로그램 관리 및 매칭 API 완성 ~ 2026-02-17
- **GAM-5** 지원 현황 관리 및 문서 업로드 ~ 2026-02-24
- **GAM-6** 보안 강화 및 모니터링 (배포는 CI/CD 자동화) ~ 2026-03-03

### 프론트엔드

- **GAM-142** 사용자 인증 및 프로필 입력 화면 구현 ~ 2026-02-06
- **GAM-143** AI 매칭 실행 및 결과 시각화 ~ 2026-02-20
- **GAM-144** 지원 관리 기능 및 대시보드 완성 ~ 2026-03-06

---

## 백엔드

### 완료된 작업

- **GAM-1** [에픽] Mock API 및 Swagger 문서 구축 (기한: 2026-01-27)
- **GAM-27** [작업] 단일 모듈 구조 확인 (ga-api-platform 루트)
- **GAM-28** [작업] MockMatchingController.kt 생성
- **GAM-29** [작업] MatchingResponse DTO 정의
- **GAM-30** [작업] SchoolResponse DTO 정의
- **GAM-31** [작업] ProgramResponse DTO 정의 (기한: 2026-02-11)
- **GAM-32** [작업] Mock 데이터 생성 (3가지 시나리오) (기한: 2026-02-12)
- **GAM-33** [작업] docs/api/ API.md (공통 형식, API 목록) (기한: 2026-02-12)
- **GAM-34** [작업] docs/api/ 엔드포인트별 Markdown (matching, programs, schools)
- **GAM-35** [작업] docs/FRONTEND_HANDOFF.md 작성 (링크 공유 방식, Base URL, 일정)
- **GAM-36** [작업] docs/04_FRONTEND_COOPERATION.md 참조 또는 통합
- **GAM-37** [작업] README "프론트엔드 협업"에 FRONTEND_HANDOFF.md 링크 추가
- **GAM-38** [작업] build.gradle.kts에 JPA, PostgreSQL, Flyway 의존성 추가
- **GAM-39** [작업] application.yml 프로파일 및 Flyway 기본 설정
- **GAM-40** [작업] application-lightsail.yml, application-local.yml 작성
- **GAM-41** [작업] V1__create_schema_from_doc.sql 작성 및 db/migration 배치 (기한: 2026-02-18)
- **GAM-46** [작업] Flyway 설정 확인 (application.yml, 이미 GAM-13에서 설정됨)
- **GAM-49** [작업] User Entity 생성 (GAM-13 V1 마이그레이션의 users 테이블 매핑)
- **GAM-52** [작업] UserRepository, AcademicProfileRepository, UserPreferenceRep (기한: 2026-02-26)
- **GAM-53** [작업] UserProfileService 구현 (기한: 2026-02-26)
- **GAM-54** [작업] UserProfileController 구현 (기한: 2026-02-26)
- **GAM-57** [작업] UserProfileServiceTest.kt 작성
- **GAM-58** [작업] UserProfileControllerTest.kt 작성
- **GAM-78** [작업] WeightConfig.kt 설정 클래스
- **GAM-85** [작업] School.kt Entity 생성
- **GAM-86** [작업] Program.kt Entity 생성
- **GAM-125** [작업] E2E 테스트 작성
- **GAM-127** [작업] 테스트 데이터 준비
- **GAM-132** [작업] CORS 설정
- **GAM-134** [작업] Actuator 설정
- **GAM-136** [작업] 로그 포맷 설정 (JSON)

### 진행 중인 작업

- **GAM-2** [에픽] 사용자 프로필 및 유학 목표 관리 (기한: 2026-02-03)

### 남은 작업

- **GAM-3** [에픽] Rule-based AI 매칭 알고리즘 구현 (기한: 2026-02-10)
- **GAM-4** [에픽] 학교/프로그램 관리 및 매칭 API 완성 (기한: 2026-02-17)
- **GAM-5** [에픽] 지원 현황 관리 및 문서 업로드 (기한: 2026-02-24)
- **GAM-6** [에픽] 보안 강화 및 모니터링 (배포는 CI/CD 자동화) (기한: 2026-03-03)
- **GAM-42** [작업] JIRA 백로그(GAM-13) 설명·AC·Tasks·DoD 반영 (기한: 2026-02-19)
- **GAM-43** [작업] V2__create_matching_tables.sql 작성 (GAM-13의 V1 이후) (기한: 2026-02-19)
- **GAM-44** [작업] 테이블 스키마 정의 (컬럼, 타입, 제약조건) (기한: 2026-02-19)
- **GAM-45** [작업] 인덱스 생성 쿼리 작성
- **GAM-47** [작업] 마이그레이션 테스트 (로컬 DB)
- **GAM-48** [작업] ERD 다이어그램 작성 (docs/erd.png)
- **GAM-50** [작업] AcademicProfile Entity 생성 (academic_profiles 테이블 매핑)
- **GAM-51** [작업] UserPreference Entity 생성 (user_preferences 테이블 매핑) (기한: 2026-02-25)
- **GAM-55** [작업] Request/Response DTO 생성 (기한: 2026-02-26)
- **GAM-56** [작업] Validation 로직 추가
- **GAM-59** [작업] MockK 설정 및 Mocking
- **GAM-60** [작업] 성공 시나리오 테스트
- **GAM-61** [작업] 실패 시나리오 테스트 (기한: 2026-03-04)
- **GAM-62** [작업] 테스트 커버리지 측정 (기한: 2026-03-05)
- **GAM-63** [작업] MatchingEngineService.kt 생성 (기한: 2026-03-05)
- **GAM-64** [작업] HardFilterService.kt 생성
- **GAM-65** [작업] 예산 초과 필터 구현
- **GAM-66** [작업] 비자 요건 필터 구현
- **GAM-67** [작업] 영어 점수 필터 구현
- **GAM-68** [작업] 입학 시기 필터 구현
- **GAM-69** [작업] 영어 점수 환산 유틸리티
- **GAM-70** [작업] FilterResult 모델 정의 (기한: 2026-02-12)
- **GAM-71** [작업] ScoringService.kt 생성 (기한: 2026-02-12)
- **GAM-72** [작업] 학업 적합도 계산 로직
- **GAM-73** [작업] 영어 적합도 계산 로직
- **GAM-74** [작업] 예산 적합도 계산 로직
- **GAM-75** [작업] 지역 선호 계산 로직
- **GAM-76** [작업] 기간 적합도 계산 로직
- **GAM-77** [작업] 진로 연계성 계산 로직
- **GAM-79** [작업] GPA 정규화 유틸리티
- **GAM-80** [작업] PathOptimizationService.kt 생성
- **GAM-81** [작업] 경로 최적화 시나리오 구현
- **GAM-82** [작업] 리스크 패널티 로직 구현
- **GAM-83** [작업] 추천 유형 분류 로직
- **GAM-84** [작업] 최종 점수 계산 및 검증
- **GAM-87** [작업] SchoolRepository 생성
- **GAM-88** [작업] ProgramRepository 생성
- **GAM-89** [작업] seed_schools.sql 작성 (20개 학교)
- **GAM-90** [작업] seed_programs.sql 작성 (40개 프로그램)
- **GAM-91** [작업] SchoolService 구현
- **GAM-92** [작업] ProgramController 구현
- **GAM-93** [작업] MatchingResult.kt Entity
- **GAM-94** [작업] MatchingResultRepository
- **GAM-95** [작업] MatchingController.kt
- **GAM-96** [작업] 전체 매칭 파이프라인 구현
- **GAM-97** [작업] 결과 저장 로직
- **GAM-98** [작업] 결과 조회 로직
- **GAM-99** [작업] 성능 최적화 (캐싱, 인덱스)
- **GAM-100** [작업] ExplanationService.kt 생성
- **GAM-101** [작업] 설명 템플릿 정의
- **GAM-102** [작업] 점수 기여도 분석 로직
- **GAM-103** [작업] 긍정/부정 요소 추출
- **GAM-104** [작업] 설명 문구 생성 로직
- **GAM-105** [작업] Mock/실제 스키마 비교 체크리스트 작성
- **GAM-106** [작업] 프론트 호출 성공 기준 문서화
- **GAM-107** [작업] E2E 호출 시나리오 테스트
- **GAM-108** [작업] Application.kt Entity
- **GAM-109** [작업] ApplicationRepository
- **GAM-110** [작업] ApplicationService
- **GAM-111** [작업] ApplicationController
- **GAM-112** [작업] ApplicationStatus Enum
- **GAM-113** [작업] Progress 계산 로직
- **GAM-114** [작업] Document.kt Entity
- **GAM-115** [작업] DocumentRepository
- **GAM-116** [작업] DocumentService
- **GAM-117** [작업] FileStorageService (S3 or Local)
- **GAM-118** [작업] DocumentController
- **GAM-119** [작업] 파일 검증 로직
- **GAM-120** [작업] 파일 삭제 로직
- **GAM-121** [작업] DashboardController.kt
- **GAM-122** [작업] DashboardService
- **GAM-123** [작업] 집계 쿼리 작성
- **GAM-124** [작업] DashboardResponse DTO
- **GAM-126** [작업] 성능 테스트 스크립트
- **GAM-128** [작업] 테스트 실행 및 결과 분석
- **GAM-129** [작업] JwtAuthenticationFilter 적용
- **GAM-130** [작업] RateLimitingFilter 구현
- **GAM-131** [작업] InputValidator 구현
- **GAM-133** [작업] 보안 체크리스트 검증
- **GAM-135** [작업] Prometheus 메트릭 추가
- **GAM-137** [작업] 커스텀 메트릭 추가
- **GAM-138** [작업] README.md 작성 (단일 모듈 구조, Docker 배포, .env 관리 등)
- **GAM-139** [작업] 04_FRONTEND_COOPERATION.md 업데이트
- **GAM-140** [작업] API 사용 예시 추가
- **GAM-141** [작업] 트러블슈팅 가이드 작성

---

## 프론트엔드

### 완료된 작업

- **GAM-145** [스토리] 회원가입/로그인 (기한: 2026-02-03)
- **GAM-165** [작업] SignupForm.tsx 구현 (BaseInput 활용) - 1.5h (기한: 2026-02-05)
- **GAM-166** [작업] LoginForm.tsx 구현 - 1h (기한: 2026-02-05)
- **GAM-167** [작업] AuthService API 연동 (회원가입/로그인) - 1h (기한: 2026-02-05)
- **GAM-168** [작업] JWT 토큰 localStorage 저장 로직 - 0.5h (기한: 2026-02-05)
- **GAM-169** [작업] ProfileWizard.tsx 구조 생성 (Progress Bar) - 1h (기한: 2026-02-05)
- **GAM-170** [작업] Step1SchoolInfo.tsx 폼 구현 - 2h (기한: 2026-02-05)
- **GAM-171** [작업] Zod 스키마 정의 및 Validation - 0.5h (기한: 2026-02-05)
- **GAM-172** [작업] localStorage 임시 저장 로직 - 0.5h (기한: 2026-02-05)
- **GAM-173** [작업] Step2PersonalInfo.tsx 폼 구현 - 2h (기한: 2026-02-05)
- **GAM-174** [작업] "이전"/"다음" 버튼 로직 - 0.5h (기한: 2026-02-05)
- **GAM-175** [작업] MBTI 선택 UI - 0.5h (기한: 2026-02-05)
- **GAM-176** [작업] Step3StudyPreference.tsx 폼 구현 - 1.5h (기한: 2026-02-05)
- **GAM-177** [작업] BudgetSlider.tsx 구현 - 1h (기한: 2026-02-05)
- **GAM-178** [작업] LocationSelector.tsx 다중 선택 구현 - 1h (기한: 2026-02-05)
- **GAM-179** [작업] 전체 데이터 검증 로직 - 0.5h (기한: 2026-02-05)

### 진행 중인 작업

- 없음

### 남은 작업

- **GAM-142** [에픽] 사용자 인증 및 프로필 입력 화면 구현 (기한: 2026-02-06)
- **GAM-143** [에픽] AI 매칭 실행 및 결과 시각화 (기한: 2026-02-20)
- **GAM-144** [에픽] 지원 관리 기능 및 대시보드 완성 (기한: 2026-03-06)
- **GAM-146** [스토리] 프로필 입력 Step 1 - 학교 정보 (기한: 2026-02-04)
- **GAM-147** [스토리] 프로필 입력 Step 2 - 개인 정보 (기한: 2026-02-05)
- **GAM-148** [스토리] 프로필 입력 Step 3 - 유학 목표 (기한: 2026-02-05)
- **GAM-149** [스토리] 프로필 저장 API 연동 (기한: 2026-02-05)
- **GAM-150** [스토리] Mock API 연동 테스트 (기한: 2026-02-05)
- **GAM-151** [스토리] Form Validation & Error Handling (기한: 2026-02-05)
- **GAM-152** [스토리] 단위 테스트 (Week 1) (기한: 2026-02-05)
- **GAM-153** [스토리] 매칭 실행 UI (기한: 2026-02-17)
- **GAM-154** [스토리] 6대 지표 시각화 (Recharts) (기한: 2026-02-18)
- **GAM-155** [스토리] Top 5 추천 학교 리스트 (기한: 2026-02-19)
- **GAM-156** [스토리] 매칭 API 연동 (기한: 2026-02-19)
- **GAM-157** [스토리] 학교 상세 페이지 (기한: 2026-02-19)
- **GAM-158** [스토리] Explainable AI 설명 표시 (기한: 2026-02-19)
- **GAM-159** [스토리] 보관하기 기능 (기한: 2026-02-19)
- **GAM-160** [스토리] 단위 테스트 (Week 2) (기한: 2026-02-19)
- **GAM-161** [스토리] 지원하기 기능 (기한: 2026-03-03)
- **GAM-162** [스토리] 대시보드 구현 (기한: 2026-03-04)
- **GAM-163** [스토리] 통합 테스트 (E2E) (기한: 2026-03-05)
- **GAM-164** [스토리] 반응형 & 최적화 (기한: 2026-03-05)
- **GAM-180** [작업] UserProfileService.ts API 클라이언트 - 1h (기한: 2026-02-05)
- **GAM-181** [작업] DTO 타입 정의 - 0.5h (기한: 2026-02-05)
- **GAM-182** [작업] 에러 핸들링 및 재시도 로직 - 1h (기한: 2026-02-05)
- **GAM-183** [작업] 로딩 스피너 및 토스트 메시지 - 0.5h (기한: 2026-02-05)
- **GAM-184** [작업] Swagger UI에서 Mock API 테스트 - 0.5h (기한: 2026-02-05)
- **GAM-185** [작업] Axios 인스턴스 설정 확인 - 0.5h (기한: 2026-02-05)
- **GAM-186** [작업] Mock API 응답 타입 정의 - 0.5h (기한: 2026-02-05)
- **GAM-187** [작업] API 호출 테스트 코드 작성 - 0.5h (기한: 2026-02-05)
- **GAM-188** [작업] 전체 Zod 스키마 통합 - 1h (기한: 2026-02-05)
- **GAM-189** [작업] 에러 메시지 UI 구현 - 0.5h (기한: 2026-02-05)
- **GAM-190** [작업] 에러 필드 스크롤 로직 - 0.5h (기한: 2026-02-05)
- **GAM-191** [작업] 인증 컴포넌트 테스트 - 0.5h (기한: 2026-02-05)
- **GAM-192** [작업] 프로필 입력 컴포넌트 테스트 - 1h (기한: 2026-02-05)
- **GAM-193** [작업] Validation 테스트 - 0.5h (기한: 2026-02-05)
- **GAM-194** [작업] MatchingTrigger.tsx 구현 - 1h (기한: 2026-02-19)
- **GAM-195** [작업] LoadingSpinner.tsx 구현 - 0.5h (기한: 2026-02-19)
- **GAM-196** [작업] Mock 매칭 API 호출 로직 - 0.5h (기한: 2026-02-19)
- **GAM-197** [작업] Recharts 설치 및 기본 설정 - 0.5h (기한: 2026-02-19)
- **GAM-198** [작업] MatchingResultCard.tsx 구현 - 1.5h (기한: 2026-02-19)
- **GAM-199** [작업] MatchingRadarChart.tsx 구현 - 2h (기한: 2026-02-19)
- **GAM-200** [작업] RecommendationList.tsx 구현 - 1h (기한: 2026-02-19)
- **GAM-201** [작업] SchoolCard.tsx 구현 - 2h (기한: 2026-02-19)
- **GAM-202** [작업] ProgramFilter.tsx 구현 - 1h (기한: 2026-02-19)
- **GAM-203** [작업] API 엔드포인트 변경 및 환경변수 분리 - 0.5h (기한: 2026-02-19)
- **GAM-204** [작업] DTO 타입 업데이트 - 1h (기한: 2026-02-19)
- **GAM-205** [작업] 에러 핸들링 강화 - 0.5h (기한: 2026-02-19)
- **GAM-206** [작업] SchoolDetail.tsx 레이아웃 - 1h (기한: 2026-02-19)
- **GAM-207** [작업] SchoolInfo.tsx 기본 정보 섹션 - 2h (기한: 2026-02-19)
- **GAM-208** [작업] MatchingExplanation.tsx 설명 섹션 - 2h (기한: 2026-02-19)
- **GAM-209** [작업] SchoolActionBar.tsx 액션 버튼 - 1h (기한: 2026-02-19)
- **GAM-210** [작업] 설명 텍스트 파싱 로직 - 1h (기한: 2026-02-19)
- **GAM-211** [작업] MatchingExplanation.tsx UI 구현 - 1h (기한: 2026-02-19)
- **GAM-212** [작업] BookmarkButton.tsx 구현 - 1h (기한: 2026-02-19)
- **GAM-213** [작업] Bookmark API 연동 - 1h (기한: 2026-02-19)
- **GAM-214** [작업] 매칭 결과 컴포넌트 테스트 - 1h (기한: 2026-02-19)
- **GAM-215** [작업] 학교 상세 컴포넌트 테스트 - 1h (기한: 2026-02-19)
- **GAM-216** [작업] ApplicationModal.tsx 구현 - 1.5h (기한: 2026-03-05)
- **GAM-217** [작업] ApplicationService.ts API 연동 - 1h (기한: 2026-03-05)
- **GAM-218** [작업] 중복 지원 방지 로직 - 0.5h (기한: 2026-03-05)
- **GAM-219** [작업] Dashboard.tsx 리팩토링 - 1h (기한: 2026-03-05)
- **GAM-220** [작업] WelcomeSection.tsx - 0.5h (기한: 2026-03-05)
- **GAM-221** [작업] MatchingStatusCard.tsx - 1.5h (기한: 2026-03-05)
- **GAM-222** [작업] ApplicationStatusCard.tsx - 1.5h (기한: 2026-03-05)
- **GAM-223** [작업] SavedSchoolsCard.tsx - 0.5h (기한: 2026-03-05)
- **GAM-224** [작업] E2E 테스트 시나리오 작성 - 1.5h (기한: 2026-03-05)
- **GAM-225** [작업] 에러 시나리오 테스트 - 0.5h (기한: 2026-03-05)
- **GAM-226** [작업] 반응형 테스트 및 수정 - 1h (기한: 2026-03-05)
- **GAM-227** [작업] 성능 최적화 (코드 스플리팅, Lazy Loading) - 0.5h (기한: 2026-03-05)
- **GAM-228** [작업] Lighthouse 점검 및 개선 - 0.5h (기한: 2026-03-05)

---

**작업물 링크**: [Go Almond](https://go-almond.ddnsfree.com/)
