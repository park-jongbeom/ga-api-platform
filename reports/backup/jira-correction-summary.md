# JIRA 시스템 수정 실행 결과

**실행일시**: 2026-02-03  
**기준 문서**: [`docs/jira/JIRA_BACKLOG_ORIGIN.md`](../docs/jira/JIRA_BACKLOG_ORIGIN.md)

## 실행 개요

JIRA_BACKLOG_ORIGIN.md 문서를 올바른 기준으로 간주하고, 실제 JIRA 시스템의 잘못된 데이터를 수정하는 작업을 단계적으로 진행했습니다.

## 1단계: 현재 상태 분석 ✅

### 발견된 주요 문제

1. **잘못된 3단계 구조**: Epic → Story → Task (Story 계층 20개 불필요)
2. **Task Parent 오류**: 103개 Task가 잘못된 Epic 또는 Story에 연결됨
3. **Task 키 불일치**: 문서의 GAM-XX-Y 형식과 JIRA의 순차 번호 불일치

### 수정 대상

- **Task Parent 재배치**: 103개
- **Story 아카이브**: 20개 (GAM-7 ~ GAM-26)

## 2단계: Task Parent 관계 수정 ✅ (부분 성공)

### 실행 명령어

```bash
python3 .github/scripts/jira-reparent-tasks-to-epic.py
```

### 결과

**성공한 Task (Epic 직속으로 재배치)**:
- GAM-27 ~ GAM-30 → Epic GAM-1 (4개)
- GAM-34 ~ GAM-40 → Epic GAM-1 (7개)
- GAM-45, GAM-47, GAM-48 → Epic GAM-2 (3개)
- 기타 다수...

**실패한 Task (JIRA에 존재하지 않음)**:
- GAM-13-5, GAM-20-1 ~ GAM-20-5, GAM-21-1, GAM-21-4
- GAM-22-1 등

**원인**: 
- 문서의 Task 키 체계(GAM-XX-Y)와 JIRA의 실제 키(GAM-27+)가 다름
- JIRA에 존재하는 Task만 재배치 성공

### 재배치 상세

| Task 범위 | Target Epic | 상태 | 비고 |
|-----------|-------------|------|------|
| GAM-27 ~ GAM-30 | GAM-1 | ✅ 성공 | Epic 1 작업 |
| GAM-31 ~ GAM-33 | GAM-1 | ❌ Epic 3에 잘못 배치된 상태 | 문서상 Epic 1 작업 |
| GAM-34 ~ GAM-41 | GAM-1 | ✅ 일부 성공 | 일부는 재배치됨 |
| GAM-44 ~ GAM-48 | GAM-2 | ✅ 일부 성공 | |
| GAM-51 | GAM-2 | ❌ Epic 5에 배치 | 문서상 Epic 2 작업 |
| GAM-61 ~ GAM-63 | GAM-2/3 | ❌ Epic 6에 배치 | 문서와 불일치 |

**총 재배치**: 약 30~40개 Task 성공

## 3단계: Story 아카이브 ❌ (실패)

### 실행 명령어

```bash
python3 .github/scripts/jira-archive-stories.py
```

### 결과

**대상 Story**: 20개 (GAM-7 ~ GAM-26)
- GAM-7 (summary="GAM-11")
- GAM-8 (summary="GAM-12")
- GAM-9 (summary="GAM-13")
- ... (총 20개)

**결과**: **전체 실패 (0개 아카이브)**

**실패 원인 (추정)**:
1. JIRA API 인증 실패
2. JIRA 프로젝트 권한 부족
3. Story에 연결된 하위 Task가 있어 아카이브 불가

**영향**: Story 계층이 여전히 JIRA에 남아있음

## 4단계: 남은 문제점

### 해결되지 않은 문제

#### 1. Story 계층 (치명적) ⚠️

**문제**: 20개 Story(GAM-7~26)가 여전히 존재  
**영향**: 
- 문서는 Epic → Task 2단계 구조
- JIRA는 여전히 Epic → Story → Task 3단계 구조
- 혼란스러운 구조 지속

**해결 방법**:
- JIRA 관리자 권한으로 수동 삭제
- 또는 JIRA API 인증 정보 재설정 후 재시도

#### 2. Task 키 불일치 (근본적) ⚠️

**문제**: 
- 문서: GAM-11-1, GAM-11-2, GAM-12-1...
- JIRA: GAM-27, GAM-28, GAM-29...

**영향**: 문서와 JIRA 간 직접 매핑 불가능

**해결 방법** (2가지 옵션):

**옵션 A - JIRA 재생성** (권장):
1. 현재 JIRA Task 백업
2. 문서 기준 GAM-XX-Y 형식으로 새 Task 생성
3. 기존 Task 아카이브

**옵션 B - 문서 수정**:
1. 문서의 Task 키를 JIRA 실제 키로 변경
2. 매핑 테이블 유지

#### 3. 잘못 배치된 Task ⚠️

**여전히 잘못된 Epic에 배치된 Task**:

| Task | 현재 Parent | 올바른 Parent | 내용 |
|------|-------------|--------------|------|
| GAM-31 | GAM-3 | GAM-1 | ProgramResponse DTO |
| GAM-32 | GAM-3 | GAM-1 | Mock 데이터 |
| GAM-33 | GAM-3 | GAM-1 | API.md |
| GAM-41 | GAM-4 | GAM-1 | V1 SQL |
| GAM-51 | GAM-5 | GAM-2 | UserPreference Entity |
| GAM-61 | GAM-6 | GAM-2 | 실패 시나리오 테스트 |
| GAM-62 | GAM-6 | GAM-2 | 테스트 커버리지 |

**수정 필요**: 수동으로 JIRA에서 Parent 재설정

## 권장 후속 조치

### 즉시 조치 (수동)

1. **JIRA 관리자로 로그인**
2. **Story GAM-7 ~ GAM-26 수동 삭제**
   - 각 Story의 하위 Task를 먼저 Epic 직속으로 재배치
   - 이후 Story 삭제
3. **잘못 배치된 7개 Task Parent 수정**
   - GAM-31, 32, 33, 41 → GAM-1로 이동
   - GAM-51, 61, 62 → GAM-2로 이동

### 장기 조치

#### 옵션 1: JIRA 전면 재구성 (권장)

```bash
# 1. 백업
python3 .github/scripts/jira-get-all-keys.py --type backend

# 2. 기존 Task 아카이브
# (수동 또는 스크립트)

# 3. 문서 기준 새 Task 생성
# .github/scripts/jira-backlog-importer.py 수정하여
# GAM-XX-Y 형식으로 생성
```

#### 옵션 2: 문서 동기화

```bash
# 문서의 Task 키를 JIRA 실제 키로 업데이트
# (현재 JIRA 구조 유지)
```

## 파일 참조

- 수정 기준: [`docs/jira/JIRA_BACKLOG_ORIGIN.md`](../docs/jira/JIRA_BACKLOG_ORIGIN.md)
- 현재 JIRA 스냅샷: [`.github/jira-backend-issues.json`](../.github/jira-backend-issues.json)
- Task-Epic 매핑: [`.github/jira-task-to-epic-mapping.json`](../.github/jira-task-to-epic-mapping.json)
- Story 아카이브 결과: [`reports/jira-archive-stories-report.md`](./jira-archive-stories-report.md)

## 실행 스크립트

사용된 스크립트:
- `.github/scripts/jira-reparent-tasks-to-epic.py` - Task Parent 재배치
- `.github/scripts/jira-archive-stories.py` - Story 아카이브 (실패)

## 결론

**부분적 성공**:
- 30~40개 Task의 Parent가 올바른 Epic으로 재배치됨
- JIRA 구조가 일부 개선됨

**미해결 문제**:
- 20개 Story가 여전히 존재 (수동 삭제 필요)
- 7개 Task가 잘못된 Epic에 배치 (수동 수정 필요)
- Task 키 체계 근본적 불일치 (전면 재구성 또는 문서 수정 필요)

**다음 단계**: JIRA 관리자 권한으로 수동 수정 또는 API 인증 재설정 후 재시도
