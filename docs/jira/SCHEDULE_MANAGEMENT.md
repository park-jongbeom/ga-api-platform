# JIRA 일정 관리 시스템 사용 가이드

**지금 당장 할 일**: [당신이 해야 할 단계 (NEXT_STEPS.md)](NEXT_STEPS.md)에서 체크리스트를 확인하세요.

## 개요

이 문서는 JIRA 작업 일정 설정, GitHub push 시 자동 완료 처리, 진행 상황 보고서 생성 및 제출 방법을 설명합니다.

- **작업 일정**: 2026년 1월 27일 시작, 주말 제외 평일(월~목) 기준
- **백엔드**: 6주 (약 3월 4일 완료 목표)
- **프론트엔드**: 2.5주 (약 2월 25일 완료 목표)
- **작업물 확인**: [https://go-almond.ddnsfree.com/](https://go-almond.ddnsfree.com/)

### 백엔드·프론트 공동 작업 시: JIRA 단일 참조

보고서는 **당신만의 작업이 아니라 프론트 개발자와 함께** 진행하는 경우에도, **JIRA를 단일 기준**으로 두면 됩니다.

| 역할 | 진행 반영 방법 | 보고서 반영 |
|------|----------------|-------------|
| **당신(백엔드)** | 커밋 메시지에 `GAM-xxx`(백엔드 이슈 키) 포함 후 main에 push → JIRA 해당 이슈가 **완료**로 자동 전환 | JIRA를 조회해 생성하므로 **자동 포함** |
| **프론트 개발자** | (1) 같은 저장소에 커밋 메시지에 `GAM-xxx`(프론트 이슈 키) 포함 후 push → 동일하게 JIRA **완료** 전환, 또는 (2) JIRA에서 직접 해당 이슈 상태를 **진행 중/완료**로 변경 | (1)(2) 모두 JIRA에 반영되므로 보고서에 **자동 포함** |

**정리**: 보고서 스크립트는 **GAM 프로젝트 전체** 이슈를 JIRA API로 조회해 완료/진행 중/할 일을 나눕니다. 백엔드·프론트 이슈가 모두 같은 GAM 프로젝트에 있으므로, **JIRA만 최신 상태로 유지**하면 보고서에는 두 사람 작업이 모두 들어갑니다. 별도로 “백엔드용 / 프론트용” 보고서를 나눌 필요 없습니다.

### 중복 방지 정책

진행 중 **중복된 일정/이슈가 계속 추가되지 않도록** 다음이 적용됩니다.

| 대상 | 동작 |
|------|------|
| **날짜 설정** (`jira-set-dates.py`) | 이슈의 현재 `duedate`를 조회 후, **이미 동일한 값이면 API 호출 없이 스킵**. `--skip-if-set` 옵션으로 "이미 설정된 이슈는 모두 스킵" 가능. |
| **보고서 이슈** (`jira-report.yml`) | 동일 제목(날짜)의 **기존 open 이슈**가 있으면 **새 이슈 생성 대신 해당 이슈 본문만 업데이트**. |
| **백로그 임포트** (`jira-backlog-importer.py`) | 실행 시 **기존 `.github/jira-mapping.json` 로드**. Epic/Story/Task ID가 이미 매핑에 있으면 **생성 건너뛰고 기존 JIRA 키 사용**. |

### 이미 생긴 중복 일정 정리

**현재** 중복으로 등록된 일정이 있다면, 아래 스크립트로 **삭제/취소** 또는 **참조 제외** 처리할 수 있습니다.

| 처리 | 스크립트 | 설명 |
|------|----------|------|
| **GitHub 보고서 이슈** | `github-dedupe-report-issues.py` | 동일 제목의 open 이슈가 여러 개 있으면 **가장 최근 1개만 유지**, 나머지는 **close**. |
| **JIRA 보고서** | `jira-generate-report.py --canonical-only` | **매핑/백로그에 있는 정규 이슈만** 포함. 중복·고아 이슈는 보고서에서 **참조하지 않음**. (워크플로에서는 기본 적용) |
| **JIRA 미참조 이슈 취소** | `jira-close-unmapped-issues.py` | 매핑·백로그에 **없는** 이슈(중복/고아)를 **취소** 상태로 전환해, 일정에서 **참조하지 않도록** 처리. |

#### GitHub 중복 보고서 이슈 정리

```bash
export GITHUB_TOKEN=your_token
export GITHUB_REPOSITORY=owner/repo

# 미리보기
python3 .github/scripts/github-dedupe-report-issues.py --dry-run

# 실제 close
python3 .github/scripts/github-dedupe-report-issues.py
```

#### JIRA 매핑에 없는 이슈 취소 (중복·고아)

```bash
export JIRA_URL=... JIRA_EMAIL=... JIRA_API_TOKEN=...

# 대상만 출력 (실제 변경 없음)
python3 .github/scripts/jira-close-unmapped-issues.py --dry-run

# 취소 전환 실행 (확인 필요 시 --yes 제외 후 실행하면 안내만 출력)
python3 .github/scripts/jira-close-unmapped-issues.py --yes
```

---

## 1. JIRA 이슈에 날짜 설정

### 실행 방법

```bash
# 환경 변수 설정
export JIRA_URL=https://your-domain.atlassian.net
export JIRA_EMAIL=your-email@example.com
export JIRA_API_TOKEN=YOUR_API_TOKEN

# 미리보기 (API 호출 없음)
python3 .github/scripts/jira-set-dates.py --dry-run

# 실제 적용
python3 .github/scripts/jira-set-dates.py
```

### 옵션

| 옵션 | 기본값 | 설명 |
|------|--------|------|
| `--start-date` | 2026-01-27 | 시작일 (YYYY-MM-DD) |
| `--backend-weeks` | 6 | 백엔드 주차 수 |
| `--frontend-weeks` | 2.5 | 프론트엔드 주차 수 |
| `--backlog-backend` | docs/jira/JIRA_BACKLOG.md | 백엔드 백로그 경로 |
| `--backlog-frontend` | docs/jira/FRONT_JIRA_BACKLOG.md | 프론트엔드 백로그 경로 |
| `--dry-run` | - | 실제 API 호출 없이 미리보기만 수행 |
| `--skip-if-set` | - | 이미 duedate가 설정된 이슈는 모두 스킵 (중복 일정 방지) |

### 동작

- 백로그 문서의 Epic/Story/Task와 **Sprint**(Week N) 정보를 파싱합니다.
- 주차별 평일(월~목)에 맞춰 각 이슈의 **기한(duedate)** 을 JIRA에 설정합니다.
- 매핑은 `.github/jira-mapping.json`을 사용합니다.

---

## 2. GitHub Push 시 JIRA 연동 및 보고서 생성

### 2-1. Push 시 JIRA 자동 완료 처리

### 커밋 메시지 형식

커밋 메시지에 JIRA 이슈 키(`GAM-숫자`)를 포함하면, push 시 해당 이슈가 **완료** 상태로 자동 전환됩니다.

- `feat: [GAM-123] Mock API 구현`
- `fix: GAM-456 버그 수정`
- `docs: GAM-11, GAM-12 문서 반영`

### 동작

- **JIRA Auto Update** 워크플로우(`.github/workflows/jira-auto-update.yml`)가 `main` 브랜치 push 시 실행됩니다.
- **Deploy Matching API** 워크플로우(`.github/workflows/deploy.yml`)에서 배포 성공 후에도 동일 스크립트가 실행됩니다.
- 스크립트: `.github/scripts/jira-update-from-commit.py`

### GitHub Secrets 설정

Repository Settings → Secrets and variables → Actions에서 다음을 설정합니다.

- `JIRA_URL`: JIRA 인스턴스 URL (예: https://your-domain.atlassian.net)
- `JIRA_EMAIL`: JIRA 계정 이메일
- `JIRA_API_TOKEN`: JIRA API 토큰

---

## 3. 진행 상황 보고서 생성 및 제출

보고서는 **JIRA GAM 프로젝트 전체**를 조회해 생성하므로, 백엔드·프론트 작업이 모두 JIRA에 반영되어 있으면 한 보고서에 함께 나옵니다.

### Push 시 자동 생성

- **main 브랜치에 push**할 때마다 **JIRA Progress Report** 워크플로우가 실행됩니다.
- JIRA 이슈를 조회해 `docs/jira/reports/report-YYYY-MM-DD.md`를 만들고, 동일 날짜의 GitHub Issue가 있으면 본문만 업데이트합니다 (중복 이슈 방지).
- 보고서 커밋은 `[skip ci]`로 push되어, 그 push로 인한 재실행은 발생하지 않습니다.

### 수동 실행 (GitHub Actions)

1. GitHub 저장소 → **Actions** → **JIRA Progress Report** 워크플로우 선택
2. **Run workflow** → **Run workflow** 클릭
3. 실행 후:
   - `docs/jira/reports/report-YYYY-MM-DD.md` 파일이 생성·커밋됩니다.
   - **프로젝트 진행 상황 보고서 - YYYY-MM-DD** 제목의 GitHub Issue가 생성되거나 기존 이슈 본문이 갱신됩니다 (라벨: `report`, `status-update`).

### 스케줄 실행

- 매주 **월요일 09:00 UTC**에 동일 워크플로우가 자동 실행됩니다.

### 로컬에서 보고서만 생성

```bash
export JIRA_URL=https://your-domain.atlassian.net
export JIRA_EMAIL=your-email@example.com
export JIRA_API_TOKEN=YOUR_API_TOKEN

# stdout 출력
python3 .github/scripts/jira-generate-report.py

# 파일로 저장
python3 .github/scripts/jira-generate-report.py -o docs/jira/reports/report-$(date +%Y-%m-%d).md
```

### 보고서 내용

- 프로젝트 개요 및 보고일
- **진행도 요약**: 전체/완료/진행 중/남은 작업 건수, 진행률(%)
- **전체 일정**: Epic 타임라인
- **완료된 작업** / **진행 중인 작업** / **남은 작업** 목록
- **작업물 링크**: [https://go-almond.ddnsfree.com/](https://go-almond.ddnsfree.com/)

---

## 4. 보스에게 보고서 제출

- **GitHub 링크**: 저장소 → Issues에서 `report`, `status-update` 라벨이 붙은 이슈를 열면 최신 보고서를 볼 수 있습니다.
- **문서 링크**: `docs/jira/reports/report-YYYY-MM-DD.md` 파일 경로를 공유하면 해당 일자 보고서를 확인할 수 있습니다.
- **작업물 확인**: 보고서 본문에 포함된 [Go Almond](https://go-almond.ddnsfree.com/) 링크로 실제 서비스를 확인할 수 있습니다.

---

## 5. 설정 파일

| 파일 | 용도 |
|------|------|
| `.github/jira-config.json` | 프로젝트 키(GAM), 이슈 패턴, 워크플로 상태명, 시작일, 주차 수, 보고서 URL 등 |
| `.github/jira-mapping.json` | 백로그 ID(GAM-XX, GAMF-XX)와 JIRA 이슈 키(GAM-XXX) 매핑 |

---

## 6. 스크립트 목록

| 스크립트 | 용도 |
|----------|------|
| `.github/scripts/jira-set-dates.py` | JIRA 이슈에 기한(duedate) 설정 |
| `.github/scripts/jira-update-from-commit.py` | 커밋 메시지에서 이슈 키 추출 후 완료 상태로 전환 |
| `.github/scripts/jira-generate-report.py` | 진행 상황 보고서 마크다운 생성 |

---

## 7. 주의사항

- JIRA API rate limit을 고려해 스크립트에 요청 간 지연이 포함되어 있습니다.
- GitHub Actions에서 JIRA 연동을 쓰려면 반드시 Repository Secrets에 JIRA 인증 정보를 설정해야 합니다.
- 커밋 메시지에 이슈 키를 넣을 때는 `GAM-숫자` 형식을 사용하세요.
