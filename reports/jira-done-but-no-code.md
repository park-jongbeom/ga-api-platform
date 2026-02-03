# JIRA 완료 처리 vs 코드 검증 보고서

JIRA에서 **완료**로 되어 있으나, 코드 기준으로 검증되지 않은 백엔드 이슈 목록입니다.

---

**완료 처리된 이슈**: 50개
**미작업 완료 의심**: 40개

## 미작업 완료 처리 목록 (코드 미검증)

| JIRA 키 | 유형 | 제목 | 백로그 키 | 사유 |
|---------|------|------|----------|------|
| GAM-1 | 에픽 | Mock API 및 API 문서(Markdown) 구축 | - | 매핑 없음 |
| GAM-2 | 에픽 | 사용자 프로필 및 유학 목표 관리 | - | 매핑 없음 |
| GAM-7 | 스토리 | GAM-11 | - | 매핑 없음 |
| GAM-8 | 스토리 | GAM-12 | - | 매핑 없음 |
| GAM-9 | 스토리 | GAM-13 | - | 매핑 없음 |
| GAM-10 | 스토리 | GAM-21 | - | 매핑 없음 |
| GAM-12 | 스토리 | GAM-23 | GAM-23 | 코드 미검증 |
| GAM-15 | 스토리 | GAM-33 | GAM-33 | 코드 미검증 |
| GAM-16 | 스토리 | GAM-41 | GAM-41 | 코드 미검증 |
| GAM-17 | 스토리 | GAM-42 | GAM-42 | 코드 미검증 |
| GAM-18 | 스토리 | GAM-43 | GAM-43 | 코드 미검증 |
| GAM-19 | 스토리 | GAM-44 | GAM-44 | 코드 미검증 |
| GAM-24 | 스토리 | GAM-61 | GAM-61 | 코드 미검증 |
| GAM-25 | 스토리 | GAM-62 | GAM-62 | 코드 미검증 |
| GAM-26 | 스토리 | GAM-63 | GAM-63 | 코드 미검증 |
| GAM-27 | 작업 | 단일 모듈 구조 확인 (ga-api-platform 루트) | GAM-11-1 | 코드 미검증 |
| GAM-34 | 작업 | docs/api/ 엔드포인트별 Markdown (matching, programs, sch | GAM-12-2 | 코드 미검증 |
| GAM-35 | 작업 | docs/FRONTEND_HANDOFF.md 작성 (링크 공유 방식, Base URL, 일 | GAM-12-3 | 코드 미검증 |
| GAM-36 | 작업 | docs/04_FRONTEND_COOPERATION.md 참조 또는 통합 | GAM-12-4 | 코드 미검증 |
| GAM-37 | 작업 | README "프론트엔드 협업"에 FRONTEND_HANDOFF.md 링크 추가 | GAM-12-5 | 코드 미검증 |
| GAM-38 | 작업 | build.gradle.kts에 JPA, PostgreSQL, Flyway 의존성 추가 | GAM-13-1 | 코드 미검증 |
| GAM-39 | 작업 | application.yml 프로파일 및 Flyway 기본 설정 | GAM-13-2 | 코드 미검증 |
| GAM-40 | 작업 | application-lightsail.yml, application-local.yml 작 | GAM-13-3 | 코드 미검증 |
| GAM-45 | 작업 | 인덱스 생성 쿼리 작성 | GAM-21-3 | 코드 미검증 |
| GAM-46 | 작업 | Flyway 설정 확인 (application.yml, 이미 GAM-13에서 설정됨) | - | 매핑 없음 |
| GAM-47 | 작업 | 마이그레이션 테스트 (로컬 DB) | GAM-21-5 | 코드 미검증 |
| GAM-48 | 작업 | ERD 다이어그램 작성 (docs/erd.png) | GAM-21-6 | 코드 미검증 |
| GAM-49 | 작업 | User Entity 생성 (GAM-13 V1 마이그레이션의 users 테이블 매핑) | - | 매핑 없음 |
| GAM-51 | 작업 | UserPreference Entity 생성 (user_preferences 테이블 매핑) | GAM-51 | 코드 미검증 |
| GAM-56 | 작업 | Validation 로직 추가 | GAM-22-8 | 코드 미검증 |
| GAM-57 | 작업 | UserProfileServiceTest.kt 작성 | GAM-23-1 | 코드 미검증 |
| GAM-58 | 작업 | UserProfileControllerTest.kt 작성 | GAM-23-2 | 코드 미검증 |
| GAM-59 | 작업 | MockK 설정 및 Mocking | GAM-23-3 | 코드 미검증 |
| GAM-60 | 작업 | 성공 시나리오 테스트 | GAM-23-4 | 코드 미검증 |
| GAM-64 | 작업 | HardFilterService.kt 생성 | GAM-31-2 | 코드 미검증 |
| GAM-65 | 작업 | 예산 초과 필터 구현 | GAM-31-3 | 코드 미검증 |
| GAM-66 | 작업 | 비자 요건 필터 구현 | GAM-31-4 | 코드 미검증 |
| GAM-67 | 작업 | 영어 점수 필터 구현 | GAM-31-5 | 코드 미검증 |
| GAM-68 | 작업 | 입학 시기 필터 구현 | GAM-31-6 | 코드 미검증 |
| GAM-69 | 작업 | 영어 점수 환산 유틸리티 | GAM-31-7 | 코드 미검증 |

---

## 조치 제안

- **에픽(GAM-1, GAM-2)**: 코드 단위 검증 불가. 하위 Story/Task 완료율로 판단하는 것이 적절합니다.
- **스토리/작업**: 코드 증거가 없이 완료 처리된 항목은 JIRA에서 **해야 할 일**으로 되돌리려면 기존 `jira-transition-issues.py` 또는 `jira-revert-*` 스크립트에 `.github/jira-done-but-no-code.json`의 `done_but_no_code` 목록을 입력으로 사용할 수 있습니다.
- **매핑 없음**: `jira-build-full-mapping.py` 재실행 후 매핑이 채워지면 코드 검증 여부가 재계산됩니다.

**기준**: `jira-verify-code-completion.py`의 Task/Story 코드 존재 여부  
**데이터**: `.github/jira-backend-issues.json`, `.github/jira-to-backlog-mapping.json`
