# JIRA 진행 보고서 폴더 (reports)

이 폴더는 **docs 하위가 아니라 프로젝트 루트**에 있으며, **로컬 스크립트**만 보고서를 생성·갱신합니다. CI에서는 보고서를 생성하지 않습니다. **docs 전체는 push 제외**되고, **예외는 이 report 폴더(reports/)만** push 됩니다.

---

## 1. 파일 명명 규칙

| 파일/패턴 | 설명 | 생성 시점 |
|-----------|------|-----------|
| **report-latest.md** | 항상 **최신** 스냅샷. 한곳에서 최종 보고서를 볼 때 사용. | 로컬 스크립트 실행 시마다 당일 보고서 내용으로 덮어씀. |
| **report-YYYY-MM-DD.md** | **날짜별** 보고서. 해당 일자 JIRA 상태 스냅샷. | 로컬 스크립트 실행 시 해당 날짜로 생성·갱신. |

- 같은 날 여러 번 실행되면 `report-YYYY-MM-DD.md`는 **같은 파일을 덮어쓰고**, `report-latest.md`도 그 내용으로 갱신됩니다.

---

## 2. 공통 규칙 (로컬 / 커밋 시)

**보고서 생성 규칙은 한곳에서만 정의됩니다.** 로컬 요청 시·commit 시 조건부가 **동일한 설정**을 사용합니다. **CI에서는 보고서를 생성하지 않습니다.**

| 항목 | 정의 위치 | 설명 |
|------|------------|------|
| **설정 파일** | `.github/jira-config.json` | projectKey, reportWebUrl, mappingFile, backlogDocument, frontendBacklog, **reportsDir** |
| **출력 경로** | config의 `reportsDir` (기본 `reports`) | `{reportsDir}/report-YYYY-MM-DD.md`, `{reportsDir}/report-latest.md` |
| **생성 스크립트** | `.github/scripts/jira-generate-report.py` | `--config .github/jira-config.json` 로 위 옵션 로드, `--canonical-only` 적용 |

경로나 프로젝트 키·백로그 문서를 바꿀 때는 **`.github/jira-config.json`만 수정**하면 로컬/커밋 시 생성에 반영됩니다.

---

## 3. 생성 주체 (로컬만, CI 없음)

- **CI**: 보고서 생성 워크플로는 **제거**되어 있습니다. CI에서는 JIRA 참조·완료 처리(`jira-auto-update.yml`)만 수행합니다.
- **로컬 (요청 시 생성)**: **commit 트리거가 아닌, 내가 요청할 때** `.github/scripts/jira-report-local.sh` 를 실행합니다. **JIRA만** 참조하여 `report-latest.md`·날짜별 보고서를 생성·갱신합니다.
- **로컬 (commit 시 조건부)**: **commit 후** `.github/scripts/jira-report-if-needed.py` 를 실행하면, JIRA 일정과 연관된 커밋인 경우에만 필요 시 보고서를 생성합니다.
- **docs/** 전체는 .gitignore로 push 제외됩니다. **예외는 이 reports/ 폴더만** (docs 하위 아님).

---

## 4. Ubuntu 로컬에서 실행하는 방법

**사전 준비** (한 번만):

1. **Python3**: Ubuntu 기본 설치되어 있음. 없으면 `sudo apt update && sudo apt install -y python3`
2. **requests** (Ubuntu에서 pip가 externally-managed-environment 오류를 낼 때):
   - **방법 A (권장)**: `sudo apt update && sudo apt install -y python3-requests` — 시스템 패키지로 설치
   - **방법 B**: 가상환경 사용 — `python3 -m venv .venv`, `./.venv/bin/pip install requests`, 실행 시 `./.venv/bin/python3 .github/scripts/...` 사용
3. **JIRA 환경**: `docs/jira/jira.env` 에 `JIRA_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN` 이 있으면 **export 없이** 실행 가능. (이미 적용되어 있으면 생략)

**실행**: 프로젝트 루트(`ga-api-platform/`)에서 아래만 실행하면 됩니다.

```bash
# 프로젝트 루트로 이동
cd /path/to/ga-api-platform

# (1) 요청 시 보고서 생성 — JIRA만 참조해 report-latest 갱신 (commit 트리거 아님)
./.github/scripts/jira-report-local.sh
# 커밋까지 하려면:
./.github/scripts/jira-report-local.sh --commit

# (2) commit 후 조건부 보고서 생성 — 일정 관련·미완료일 때만 생성
python3 .github/scripts/jira-report-if-needed.py
# 커밋까지 하려면:
python3 .github/scripts/jira-report-if-needed.py --commit
```

스크립트가 `docs/jira/jira.env` 를 자동으로 읽습니다. `jira-report-local.sh` 는 실행 권한이 있어야 하며, 없으면 `chmod +x .github/scripts/jira-report-local.sh` 로 부여하세요.

---

## 5. 보고서가 생성되지 않을 때

- **로컬 실행 시**: Ubuntu에서는 `sudo apt install -y python3-requests` 로 requests 설치 (pip가 externally-managed-environment 오류를 낼 때). JIRA 환경값은 **docs/jira/jira.env** 에 두면 됩니다.

---

## 6. 보고서에 표시되는 작업 이름

보고서의 각 작업 줄(예: `- **GAM-11** [스토리] ...`)에서 **이름 부분**은 다음 순서로 결정됩니다.

1. **백로그 문서 제목**: `docs/jira/JIRA_BACKLOG.md`, `docs/jira/FRONT_JIRA_BACKLOG.md`에 있는 Epic/Story 제목(예: `### Story GAM-11: Mock API 명세 구현`)을 파싱해 **우선 사용**합니다.
2. **JIRA summary**: 백로그에 해당 키가 없거나 제목이 없으면 JIRA 이슈의 **요약(summary)** 필드를 사용합니다.

그래서 JIRA에 summary가 "GAM-22"처럼 키만 들어 있어도, 백로그에 "GAM-11: Mock API 명세 구현"이 있으면 보고서에는 **"Mock API 명세 구현"**처럼 요약된 제목으로 표시됩니다. JIRA 이슈 이름을 일일이 바꾸지 않아도 됩니다.

**백엔드/프론트엔드 구분**: 보고서는 **백엔드**·**프론트엔드**로 나누어 표시됩니다. 매핑 파일(`.github/jira-mapping.json`)에서 `GAMF-*` 키에 대응하는 JIRA 이슈(GAM-xxx)는 **프론트엔드**, 그 외는 **백엔드**로 분류됩니다. 진행도 요약·Epic 타임라인·완료/진행 중/남은 작업이 각각 백엔드·프론트엔드 섹션으로 구분됩니다.

---

## 7. 요약

| 항목 | 규칙 |
|------|------|
| 최신 보고서 | `reports/report-latest.md` (로컬에서 생성·push) |
| 날짜별 | `reports/report-YYYY-MM-DD.md` (로컬에서 생성·push) |
| CI | 보고서 생성 없음. JIRA 완료 처리만 `jira-auto-update.yml` 에서 수행. |
| 로컬 요청 시 | **요청할 때** `jira-report-local.sh` 실행 → JIRA만 참조, report-latest 대조·갱신 (commit 트리거 아님). |
| 로컬 조건부 | commit 후 `jira-report-if-needed.py` 실행 시, 일정 관련·미완료인 경우에만 생성. |
| push 제외 | **docs/** 전체 제외. **예외**: **reports/** (루트, docs 하위 아님) 만 push. |
| 표시 이름 | 백로그 문서(Epic/Story 제목) 우선, 없으면 JIRA summary. |
