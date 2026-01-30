# JIRA 진행 보고서 폴더 (docs/jira/reports)

이 폴더에는 **JIRA Progress Report** 워크플로우가 생성·갱신하는 보고서 파일만 둡니다.

---

## 1. 파일 명명 규칙

| 파일/패턴 | 설명 | 생성 시점 |
|-----------|------|-----------|
| **report-latest.md** | 항상 **최신** 스냅샷. 한곳에서 최종 보고서를 볼 때 사용. | 워크플로 실행 시마다 당일 보고서 내용으로 덮어씀. |
| **report-YYYY-MM-DD.md** | **push/날짜별** 보고서. 해당 일자 JIRA 상태 스냅샷. | main push, 수동 실행, 매주 월요일 09:00 UTC 스케줄 시 해당 날짜로 생성·갱신. |

- 같은 날 여러 번 실행되면 `report-YYYY-MM-DD.md`는 **같은 파일을 덮어쓰고**, `report-latest.md`도 그 내용으로 갱신됩니다.
- GitHub Issues: 제목 **"프로젝트 진행 상황 보고서 (최신)"**(라벨 `report`, `latest`)와 **"프로젝트 진행 상황 보고서 - YYYY-MM-DD"**(라벨 `report`, `status-update`)가 위 파일과 동일 내용으로 생성·업데이트됩니다.

---

## 2. 생성 주체 및 수동 push

- **생성·커밋·push**: `.github/workflows/jira-report.yml` (JIRA Progress Report)만 수행합니다.
- **개발자가 이 폴더의 보고서 파일을 수동으로 push할 필요 없습니다.** 워크플로가 `[skip ci]`로 커밋·push합니다.
- **docs 또는 다른 경로만 push할 때**에도, 보고서 파일이 아직 없거나 오래됐다면 **main에 push하거나** Actions에서 **JIRA Progress Report → Run workflow**로 한 번 실행하면 생성·갱신됩니다. (보고서 워크플로는 `push` 시 경로 제한 없이 main만 대상으로 실행됩니다.)

---

## 3. .gitignore 및 push 미포함 정책

- **이 폴더와 그 안의 `report-*.md` 파일은 .gitignore에 넣지 마세요.**  
  워크플로가 생성한 파일을 커밋·push해야 저장소에 반영됩니다. .gitignore에 넣으면 CI가 push할 수 없습니다.
- **docs/jira** 아래 다른 파일(예: pdf, 일부 가이드)만 push 제외 대상이며, **reports/** 는 제외하지 않습니다.

---

## 4. 보고서가 생성되지 않을 때

- **Repository Secrets**: `JIRA_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN`이 설정되어 있는지 확인하세요.
- **Actions 로그**: Actions → JIRA Progress Report → **Generate JIRA report** 스텝에서 stderr(예: `requests` 오류, JIRA 인증 오류)를 확인하세요.
- 자세한 내용: [SCHEDULE_MANAGEMENT.md - 보고서가 생성되지 않을 때](../SCHEDULE_MANAGEMENT.md#보고서가-생성되지-않을-때)

---

## 5. 요약

| 항목 | 규칙 |
|------|------|
| 최신 보고서 | `report-latest.md` 또는 GitHub 이슈 "프로젝트 진행 상황 보고서 (최신)" |
| push/날짜별 보고서 | `report-YYYY-MM-DD.md` 또는 이슈 "프로젝트 진행 상황 보고서 - YYYY-MM-DD" |
| 생성·push | 워크플로만 수행. 개발자 수동 push 불필요. |
| .gitignore | `docs/jira/reports/` 및 `report-*.md` 미포함 유지. |
