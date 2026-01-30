# JIRA 진행 보고서 폴더 (reports)

이 폴더는 **docs 하위가 아니라 프로젝트 루트**에 있으며, JIRA Progress Report 워크플로·로컬 스크립트가 생성·갱신하는 보고서만 둡니다. **docs 전체는 push 제외**되고, **예외는 이 report 폴더(reports/)만** push 됩니다.

---

## 1. 파일 명명 규칙

| 파일/패턴 | 설명 | 생성 시점 |
|-----------|------|-----------|
| **report-latest.md** | 항상 **최신** 스냅샷. 한곳에서 최종 보고서를 볼 때 사용. | 워크플로·로컬 스크립트 실행 시마다 당일 보고서 내용으로 덮어씀. |
| **report-YYYY-MM-DD.md** | **push/날짜별** 보고서. 해당 일자 JIRA 상태 스냅샷. | main push, 수동 실행, 매주 월요일 09:00 UTC 스케줄 또는 로컬 스크립트 시 해당 날짜로 생성·갱신. |

- 같은 날 여러 번 실행되면 `report-YYYY-MM-DD.md`는 **같은 파일을 덮어쓰고**, `report-latest.md`도 그 내용으로 갱신됩니다.
- GitHub Issues: 제목 **"프로젝트 진행 상황 보고서 (최신)"**(라벨 `report`, `latest`)와 **"프로젝트 진행 상황 보고서 - YYYY-MM-DD"**(라벨 `report`, `status-update`)가 위 파일과 동일 내용으로 생성·업데이트됩니다.

---

## 2. 생성 주체 및 push

- **CI**: `.github/workflows/jira-report.yml` (JIRA Progress Report)가 main push·수동 실행·스케줄 시 **reports/** 에 생성·커밋·push 합니다.
- **로컬**: Actions에서 보고서가 생성되지 않을 때는 **push 전에** `.github/scripts/jira-report-local.sh` 를 실행해 **reports/** 에 생성·스테이징(및 선택적 `--commit`)한 뒤, 커밋·push 하면 됩니다.
- **docs/** 전체는 .gitignore로 push 제외됩니다. **예외는 이 reports/ 폴더만** (docs 하위 아님).

---

## 3. 보고서가 생성되지 않을 때

- **Repository Secrets**: `JIRA_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN` 설정 여부 확인.
- **Actions 로그**: Actions → JIRA Progress Report → **Generate JIRA report** 스텝 stderr 확인.
- 로컬 실행 시: `pip3 install requests`, 환경 변수 설정 후 `./.github/scripts/jira-report-local.sh` 실행.

---

## 4. 요약

| 항목 | 규칙 |
|------|------|
| 최신 보고서 | `reports/report-latest.md` 또는 GitHub 이슈 "프로젝트 진행 상황 보고서 (최신)" |
| push/날짜별 | `reports/report-YYYY-MM-DD.md` 또는 이슈 "프로젝트 진행 상황 보고서 - YYYY-MM-DD" |
| 로컬 조건부 | commit 후 `jira-report-if-needed.py` 실행 시, 일정 관련·미완료인 경우에만 생성. |
| push 제외 | **docs/** 전체 제외. **예외**: **reports/** (루트, docs 하위 아님) 만 push. |
