# JIRA 완료 상태 vs 실제 코드 검증 (엄격 모드) 보고서

**검증 원칙**: Entity/Service/Controller/Repository/DTO는 src/ 내 파일+클래스 정의 필수, 키워드는 src/만 검색.

## 요약

- JIRA 완료 Task: **85개**
- 실제 구현됨 (엄격): **43개** (50%)
- 미구현(불일치): **42개**

## Epic별 검증 결과

| Epic | 완료 Task | 실제 구현 | 불일치 |
|------|----------|----------|--------|
| GAM-1 | 17개 | 14개 | 3개 |
| GAM-2 | 9개 | 3개 | 6개 |
| GAM-3 | 13개 | 9개 | 4개 |
| GAM-4 | 18개 | 8개 | 10개 |
| GAM-5 | 19개 | 6개 | 13개 |
| GAM-6 | 9개 | 3개 | 6개 |

## 미구현 Task 상세 (JIRA 완료 ↔ 코드 없음)

- **GAM-55** (GAM-2): Request/Response DTO 생성
  - 사유: src/에 다음 클래스 없음: ['UserProfileRequest.kt']

- **GAM-78** (GAM-1): WeightConfig.kt 설정 클래스
  - 사유: src/에 다음 클래스 없음: ['WeightConfig.kt']

- **GAM-79** (GAM-3): GPA 정규화 유틸리티
  - 사유: src/ 내 키워드 없음: ['GPA', '정규화']

- **GAM-80** (GAM-3): PathOptimizationService.kt 생성
  - 사유: src/에 다음 클래스 없음: ['PathOptimizationService.kt']

- **GAM-82** (GAM-3): 리스크 패널티 로직 구현
  - 사유: src/ 내 키워드 없음: ['리스크', '패널티']

- **GAM-85** (GAM-1): School.kt Entity 생성
  - 사유: src/에 다음 클래스 없음: ['School.kt']

- **GAM-86** (GAM-1): Program.kt Entity 생성
  - 사유: src/에 다음 클래스 없음: ['Program.kt']

- **GAM-87** (GAM-2): SchoolRepository 생성
  - 사유: src/에 다음 클래스 없음: ['SchoolRepository.kt']

- **GAM-88** (GAM-2): ProgramRepository 생성
  - 사유: src/에 다음 클래스 없음: ['ProgramRepository.kt']

- **GAM-91** (GAM-4): SchoolService 구현
  - 사유: src/에 다음 클래스 없음: ['SchoolService.kt']

- **GAM-92** (GAM-4): ProgramController 구현
  - 사유: src/에 다음 클래스 없음: ['ProgramController.kt']

- **GAM-93** (GAM-4): MatchingResult.kt Entity
  - 사유: src/에 다음 클래스 없음: ['MatchingResult.kt']

- **GAM-94** (GAM-2): MatchingResultRepository
  - 사유: src/에 다음 클래스 없음: ['MatchingResultRepository.kt']

- **GAM-95** (GAM-4): MatchingController.kt
  - 사유: src/에 다음 클래스 없음: ['MatchingController.kt']

- **GAM-99** (GAM-4): 성능 최적화 (캐싱, 인덱스)
  - 사유: src/ 내 키워드 없음: ['캐싱', '인덱스', 'Cache']

- **GAM-100** (GAM-4): ExplanationService.kt 생성
  - 사유: src/에 다음 클래스 없음: ['ExplanationService.kt']

- **GAM-101** (GAM-4): 설명 템플릿 정의
  - 사유: src/ 내 키워드 없음: ['설명', '템플릿']

- **GAM-103** (GAM-4): 긍정/부정 요소 추출
  - 사유: src/ 내 키워드 없음: ['긍정', '부정', '요소']

- **GAM-105** (GAM-4): Mock/실제 스키마 비교 체크리스트 작성
  - 사유: src/ 내 키워드 없음: ['체크리스트', '스키마']

- **GAM-106** (GAM-6): 프론트 호출 성공 기준 문서화
  - 사유: 검증 매핑 없음

- **GAM-107** (GAM-4): E2E 호출 시나리오 테스트
  - 사유: 검증 매핑 없음

- **GAM-108** (GAM-5): Application.kt Entity
  - 사유: src/에 다음 클래스 없음: ['Application.kt']

- **GAM-109** (GAM-2): ApplicationRepository
  - 사유: src/에 다음 클래스 없음: ['ApplicationRepository.kt']

- **GAM-110** (GAM-5): ApplicationService
  - 사유: src/에 다음 클래스 없음: ['ApplicationService.kt']

- **GAM-111** (GAM-5): ApplicationController
  - 사유: src/에 다음 클래스 없음: ['ApplicationController.kt']

- **GAM-112** (GAM-5): ApplicationStatus Enum
  - 사유: src/ 내 키워드 없음: ['ApplicationStatus']

- **GAM-113** (GAM-5): Progress 계산 로직
  - 사유: src/ 내 키워드 없음: ['Progress', '계산']

- **GAM-114** (GAM-5): Document.kt Entity
  - 사유: src/에 다음 클래스 없음: ['Document.kt']

- **GAM-115** (GAM-2): DocumentRepository
  - 사유: src/에 다음 클래스 없음: ['DocumentRepository.kt']

- **GAM-116** (GAM-5): DocumentService
  - 사유: src/에 다음 클래스 없음: ['DocumentService.kt']

- **GAM-118** (GAM-5): DocumentController
  - 사유: src/에 다음 클래스 없음: ['DocumentController.kt']

- **GAM-121** (GAM-5): DashboardController.kt
  - 사유: src/에 다음 클래스 없음: ['DashboardController.kt']

- **GAM-122** (GAM-5): DashboardService
  - 사유: src/에 다음 클래스 없음: ['DashboardService.kt']

- **GAM-123** (GAM-5): 집계 쿼리 작성
  - 사유: src/ 내 키워드 없음: ['집계', '쿼리', 'dashboard']

- **GAM-124** (GAM-5): DashboardResponse DTO
  - 사유: src/에 DTO 없음: ['DashboardResponse']

- **GAM-128** (GAM-5): 테스트 실행 및 결과 분석
  - 사유: 검증 매핑 없음

- **GAM-130** (GAM-3): RateLimitingFilter 구현
  - 사유: src/ 내 키워드 없음: ['RateLimitingFilter', 'RateLimit']

- **GAM-131** (GAM-6): InputValidator 구현
  - 사유: src/ 내 키워드 없음: ['InputValidator', 'Validator']

- **GAM-135** (GAM-6): Prometheus 메트릭 추가
  - 사유: src/ 내 키워드 없음: ['Prometheus', 'prometheus', 'micrometer']

- **GAM-137** (GAM-6): 커스텀 메트릭 추가
  - 사유: src/ 내 키워드 없음: ['메트릭', 'metric', 'custom']

- **GAM-139** (GAM-6): 04_FRONTEND_COOPERATION.md 업데이트
  - 사유: src/ 내 키워드 없음: ['04_FRONTEND_COOPERATION']

- **GAM-141** (GAM-6): 트러블슈팅 가이드 작성
  - 사유: src/ 내 키워드 없음: ['트러블슈팅', 'troubleshoot']

## 구현됨 Task (참고)

- **GAM-27**: 단일 모듈 구조 확인 (ga-api-platform 루트) — 파일 발견: settings.gradle.kts
- **GAM-28**: MockMatchingController.kt 생성 — src/ 클래스 발견: MockMatchingController.kt
- **GAM-29**: MatchingResponse DTO 정의 — src/ 클래스 발견: MatchingResponse.kt
- **GAM-30**: SchoolResponse DTO 정의 — src/ 클래스 발견: SchoolResponse.kt
- **GAM-34**: docs/api/ 엔드포인트별 Markdown (matching, programs, schools) — 파일 발견: docs/api/matching.md
- **GAM-35**: docs/FRONTEND_HANDOFF.md 작성 (링크 공유 방식, Base URL, 일정) — 파일 발견: docs/FRONTEND_HANDOFF.md
- **GAM-36**: docs/04_FRONTEND_COOPERATION.md 참조 또는 통합 — 파일 발견: docs/04_FRONTEND_COOPERATION.md
- **GAM-37**: README "프론트엔드 협업"에 FRONTEND_HANDOFF.md 링크 추가 — 파일 발견: README.md
- **GAM-38**: build.gradle.kts에 JPA, PostgreSQL, Flyway 의존성 추가 — 파일 발견: build.gradle.kts
- **GAM-39**: application.yml 프로파일 및 Flyway 기본 설정 — 파일 발견: application.yml
- **GAM-40**: application-lightsail.yml, application-local.yml 작성 — 파일 발견: application-lightsail.yml
- **GAM-52**: UserRepository, AcademicProfileRepository, UserPreferenceRepository 인터페이스 생성 — src/ 클래스 발견: UserRepository.kt
- **GAM-53**: UserProfileService 구현 — src/ 클래스 발견: UserProfileService.kt
- **GAM-54**: UserProfileController 구현 — src/ 클래스 발견: UserProfileController.kt
- **GAM-72**: 학업 적합도 계산 로직 — src/ 내 키워드 발견: ['ScoringService', '학업', '적합도']
- **GAM-73**: 영어 적합도 계산 로직 — src/ 내 키워드 발견: ['영어', '적합도', 'ScoringService']
- **GAM-74**: 예산 적합도 계산 로직 — src/ 내 키워드 발견: ['예산', '적합도']
- **GAM-75**: 지역 선호 계산 로직 — src/ 내 키워드 발견: ['지역', '선호']
- **GAM-76**: 기간 적합도 계산 로직 — src/ 내 키워드 발견: ['기간', '적합도']
- **GAM-77**: 진로 연계성 계산 로직 — src/ 내 키워드 발견: ['진로', '연계']
- **GAM-81**: 경로 최적화 시나리오 구현 — src/ 내 키워드 발견: ['경로', '최적화']
- **GAM-83**: 추천 유형 분류 로직 — src/ 내 키워드 발견: ['추천', '유형', '분류']
- **GAM-84**: 최종 점수 계산 및 검증 — src/ 내 키워드 발견: ['최종', '점수', '검증']
- **GAM-89**: seed_schools.sql 작성 (20개 학교) — src/ 내 키워드 발견: ['seed_schools', 'seed']
- **GAM-90**: seed_programs.sql 작성 (40개 프로그램) — src/ 내 키워드 발견: ['seed_programs', 'seed']
- **GAM-96**: 전체 매칭 파이프라인 구현 — src/ 내 키워드 발견: ['매칭', '파이프라인', 'MatchingService']
- **GAM-97**: 결과 저장 로직 — src/ 내 키워드 발견: ['결과', '저장', 'MatchingResult']
- **GAM-98**: 결과 조회 로직 — src/ 내 키워드 발견: ['결과', '조회']
- **GAM-102**: 점수 기여도 분석 로직 — src/ 내 키워드 발견: ['점수', '기여도']
- **GAM-104**: 설명 문구 생성 로직 — src/ 내 키워드 발견: ['설명', '문구', '생성']
- **GAM-117**: FileStorageService (S3 or Local) — src/ 내 키워드 발견: ['FileStorageService', 'S3', 'Local']
- **GAM-119**: 파일 검증 로직 — src/ 내 키워드 발견: ['파일', '검증', 'validate']
- **GAM-120**: 파일 삭제 로직 — src/ 내 키워드 발견: ['파일', '삭제', 'delete']
- **GAM-125**: E2E 테스트 작성 — src/ 내 키워드 발견: ['E2E', 'integration', 'test']
- **GAM-126**: 성능 테스트 스크립트 — src/ 내 키워드 발견: ['성능', '테스트', 'performance']
- **GAM-127**: 테스트 데이터 준비 — src/ 내 키워드 발견: ['테스트', '데이터']
- **GAM-129**: JwtAuthenticationFilter 적용 — src/ 클래스 발견: JwtAuthenticationFilter.kt
- **GAM-132**: CORS 설정 — src/ 내 키워드 발견: ['CORS', 'WebConfig', 'cors']
- **GAM-133**: 보안 체크리스트 검증 — src/ 내 키워드 발견: ['보안', '체크리스트']
- **GAM-134**: Actuator 설정 — src/ 내 키워드 발견: ['Actuator', 'actuator']
- **GAM-136**: 로그 포맷 설정 (JSON) — src/ 내 키워드 발견: ['log', 'json', 'logging']
- **GAM-138**: README.md 작성 (단일 모듈 구조, Docker 배포, .env 관리 등) — 파일 발견: README.md
- **GAM-140**: API 사용 예시 추가 — src/ 내 키워드 발견: ['API', '사용', '예시']

---

## JIRA 상태 수정 방안

미구현 Task **42개**를 "해야 할 일"로 되돌리려면:

```bash
python3 .github/scripts/jira-revert-code-incomplete-to-todo.py
```

### 검증 재실행

```bash
python3 .github/scripts/jira-verify-code-strict.py
```