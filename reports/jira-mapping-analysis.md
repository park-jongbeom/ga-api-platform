# JIRA-백로그 매핑 분석 보고서

총 JIRA 이슈: 71개

---

## 1. Summary 수정 필요 (16개)

JIRA summary가 백로그 키(GAM-XX)로 되어 있는 경우:

| JIRA 키 | 현재 Summary | 올바른 Summary |
|---------|--------------|-----------------|
| GAM-11 | GAM-22 | User Profile API 구현 |
| GAM-12 | GAM-23 | 단위 테스트 & 통합 테스트 |
| GAM-13 | GAM-31 | 매칭 엔진 Hard Filter 구현 |
| GAM-14 | GAM-32 | Base Score 계산 (6대 지표) |
| GAM-15 | GAM-33 | 경로 최적화 & 리스크 패널티 |
| GAM-16 | GAM-41 | School & Program Master Data |
| GAM-17 | GAM-42 | 매칭 실행 API 구현 |
| GAM-18 | GAM-43 | Explainable AI 설명 생성 |
| GAM-19 | GAM-44 | E2E 통합 시나리오 검증 |
| GAM-20 | GAM-51 | UserPreference Entity 생성 (user_preferences 테이블 매핑) |
| GAM-21 | GAM-52 | Document 업로드 |
| GAM-22 | GAM-53 | Dashboard API |
| GAM-23 | GAM-54 | 통합 테스트 |
| GAM-24 | GAM-61 | 보안 강화 |
| GAM-25 | GAM-62 | 모니터링 & 로깅 |
| GAM-26 | GAM-63 | 최종 문서화 |

**총 16개**

## 2. Epic 매핑 수정 필요 (38개)

| JIRA 키 | 백로그 키 | 현재 Epic | 올바른 Epic | 작업 |
|---------|----------|-----------|-------------|------|
| GAM-6 | GAM-61 | - | GAM-6 | 보안 강화 |
| GAM-11 | GAM-22 | GAM-1 | GAM-2 | User Profile API 구현 |
| GAM-12 | GAM-23 | GAM-1 | GAM-2 | 단위 테스트 & 통합 테스트 |
| GAM-13 | GAM-31 | GAM-1 | GAM-3 | 매칭 엔진 Hard Filter 구현 |
| GAM-14 | GAM-32 | - | GAM-3 | Base Score 계산 (6대 지표) |
| GAM-15 | GAM-33 | - | GAM-3 | 경로 최적화 & 리스크 패널티 |
| GAM-16 | GAM-41 | - | GAM-4 | School & Program Master Data |
| GAM-17 | GAM-42 | - | GAM-4 | 매칭 실행 API 구현 |
| GAM-18 | GAM-43 | - | GAM-4 | Explainable AI 설명 생성 |
| GAM-19 | GAM-44 | - | GAM-4 | E2E 통합 시나리오 검증 |
| GAM-20 | GAM-51 | GAM-2 | GAM-5 | UserPreference Entity 생성 (user_preferenc |
| GAM-21 | GAM-52 | GAM-2 | GAM-5 | Document 업로드 |
| GAM-22 | GAM-53 | GAM-2 | GAM-5 | Dashboard API |
| GAM-23 | GAM-54 | GAM-2 | GAM-5 | 통합 테스트 |
| GAM-24 | GAM-61 | - | GAM-6 | 보안 강화 |
| GAM-25 | GAM-62 | - | GAM-6 | 모니터링 & 로깅 |
| GAM-26 | GAM-63 | - | GAM-6 | 최종 문서화 |
| GAM-50 | GAM-22-2 | - | GAM-2 | AcademicProfile Entity 생성 (academic_prof |
| GAM-52 | GAM-22-4 | GAM-5 | GAM-2 | UserRepository, AcademicProfileRepositor |
| GAM-53 | GAM-22-5 | GAM-5 | GAM-2 | UserProfileService 구현 |
| GAM-54 | GAM-22-6 | GAM-5 | GAM-2 | UserProfileController 구현 |
| GAM-55 | GAM-22-7 | GAM-5 | GAM-2 | Request/Response DTO 생성 |
| GAM-56 | GAM-22-8 | - | GAM-2 | Validation 로직 추가 |
| GAM-57 | GAM-23-1 | - | GAM-2 | UserProfileServiceTest.kt 작성 |
| GAM-58 | GAM-23-2 | - | GAM-2 | UserProfileControllerTest.kt 작성 |
| GAM-59 | GAM-23-3 | - | GAM-2 | MockK 설정 및 Mocking |
| GAM-60 | GAM-23-4 | - | GAM-2 | 성공 시나리오 테스트 |
| GAM-61 | GAM-23-5 | GAM-6 | GAM-2 | 실패 시나리오 테스트 |
| GAM-62 | GAM-23-6 | GAM-6 | GAM-2 | 테스트 커버리지 측정 |
| GAM-63 | GAM-31-1 | GAM-6 | GAM-3 | MatchingEngineService.kt 생성 |

**총 38개**

## 3. 완료 처리 필요 (5개)

코드 완료인데 JIRA 상태가 '해야 할 일':

| JIRA 키 | 백로그 키 | 작업 |
|---------|-----------|---------|
| GAM-31 | GAM-11-5 | ProgramResponse DTO 정의 |
| GAM-52 | GAM-22-4 | UserRepository, AcademicProfileRepository, UserPre |
| GAM-53 | GAM-22-5 | UserProfileService 구현 |
| GAM-54 | GAM-22-6 | UserProfileController 구현 |
| GAM-55 | GAM-22-7 | Request/Response DTO 생성 |

**총 5개**

