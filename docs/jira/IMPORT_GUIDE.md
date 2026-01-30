# JIRA 백로그 문서 기반 자동 이슈 생성 가이드

## 개요

`docs/jira/JIRA_BACKLOG.md` 파일을 파싱하여 JIRA 프로젝트에 Epic, Story, Task를 자동으로 생성하는 스크립트입니다.

## 사전 준비

### 1. Python 환경 확인

```bash
python3 --version
# Python 3.7 이상 필요
```

### 2. 필요한 패키지 설치

```bash
pip3 install requests
```

### 3. JIRA API 토큰 생성

1. JIRA 계정 로그인
2. 계정 설정 → 보안 → API 토큰 생성
3. 생성된 토큰 복사

## 사용 방법

### 방법 1: 환경 변수 사용 (권장)

```bash
# 환경 변수 설정
export JIRA_URL=https://your-domain.atlassian.net
export JIRA_EMAIL=your-email@example.com
export JIRA_API_TOKEN=YOUR_API_TOKEN

# 스크립트 실행
./.github/scripts/jira-backlog-importer.sh
```

### 방법 2: 명령줄 인자 사용

```bash
python3 .github/scripts/jira-backlog-importer.py \
    --jira-url https://your-domain.atlassian.net \
    --jira-email your-email@example.com \
    --jira-api-token YOUR_API_TOKEN \
    --project-key QK54R \
    --backlog-file docs/jira/JIRA_BACKLOG.md
```

### 방법 3: Python 스크립트 직접 실행

```bash
cd /media/ubuntu/data120g/ga-api-platform
python3 .github/scripts/jira-backlog-importer.py \
    --jira-url https://your-domain.atlassian.net \
    --jira-email your-email@example.com \
    --jira-api-token YOUR_API_TOKEN
```

## 실행 결과

스크립트 실행 시:

1. 백로그 문서 파싱
   - Epic, Story, Task 정보 추출
   - 계층 구조 파악

2. JIRA API 호출
   - Epic 생성 (GAM-1 → QK54R-1)
   - Story 생성 및 Epic 연결 (GAM-11 → QK54R-11)
   - Task 생성 및 Story 연결 (GAM-11-1 → QK54R-11-1)

3. 매핑 테이블 생성
   - `.github/jira-mapping.json` 파일에 `GAM-XX` → `QK54R-XX` 매핑 저장

## 출력 예시

```
============================================================
JIRA 백로그 문서 파싱 및 자동 이슈 생성
============================================================
프로젝트: QK54R
백로그 파일: docs/jira/JIRA_BACKLOG.md

백로그 문서 파싱 중...
파싱 완료: Epic 6개, Story 20개, Task 80개

Epic 생성 중...
✓ Epic 생성 성공: GAM-1 -> QK54R-1 (Mock API 및 API 문서(Markdown) 구축)
✓ Epic 생성 성공: GAM-2 -> QK54R-2 (사용자 프로필 및 유학 목표 관리)
...

Story 생성 중...
  ✓ Story 생성 성공: GAM-11 -> QK54R-11 (Mock API 명세 구현)
  ✓ Story 생성 성공: GAM-12 -> QK54R-12 (API 문서(Markdown) & 프론트엔드 협업 가이드)
...

Task 생성 중...
    ✓ Task 생성 성공: GAM-11-1 -> QK54R-11-1 (단일 모듈 구조 확인...)
    ✓ Task 생성 성공: GAM-11-2 -> QK54R-11-2 (MockMatchingController.kt 생성)
...

매핑 테이블 저장 완료: .github/jira-mapping.json

============================================================
완료!
생성된 이슈: 106개
============================================================
```

## 매핑 테이블

생성된 `.github/jira-mapping.json` 파일 예시:

```json
{
  "GAM-1": "QK54R-1",
  "GAM-11": "QK54R-11",
  "GAM-11-1": "QK54R-11-1",
  "GAM-11-2": "QK54R-11-2",
  ...
}
```

## 주의사항

1. **JIRA 프로젝트 키**: 기본값은 `QK54R`입니다. 다른 프로젝트 키를 사용하려면 `--project-key` 옵션 사용

2. **Epic Link 필드**: JIRA 버전에 따라 Epic Link 필드 ID가 다를 수 있습니다. 기본값은 `customfield_10011`입니다.

3. **이슈 타입**: JIRA 프로젝트에 Epic, Story, Task 타입이 있어야 합니다.

4. **중복 실행**: 같은 백로그 문서를 다시 실행하면 중복 이슈가 생성될 수 있습니다. 실행 전 확인하세요.

5. **API 권한**: JIRA API 토큰에 이슈 생성 권한이 있어야 합니다.

## 문제 해결

### Epic Link 필드 오류

JIRA 버전에 따라 Epic Link 필드 ID가 다를 수 있습니다. 스크립트의 `customfield_10011` 값을 프로젝트에 맞게 수정하세요.

### 이슈 타입 오류

JIRA 프로젝트에 Epic, Story, Task 타입이 없는 경우, 프로젝트 설정에서 활성화하거나 스크립트의 이슈 타입 이름을 수정하세요.

### 인증 오류

API 토큰이 올바른지 확인하세요. JIRA 계정 설정에서 새 토큰을 생성할 수 있습니다.
