# 당신이 해야 할 단계

JIRA·일정 설정이 끝난 뒤, 아래 순서대로 진행하면 됩니다.

---

## 1. GitHub Secrets 설정 (한 번만)

CI에서 JIRA에 접근하려면 저장소에 시크릿을 넣어야 합니다.

- **위치**: GitHub 저장소 → **Settings** → **Secrets and variables** → **Actions**
- **추가할 시크릿**:
  - `JIRA_URL`: `https://qk54r71z.atlassian.net`
  - `JIRA_EMAIL`: `qk54r71z@gmail.com`
  - `JIRA_API_TOKEN`: (JIRA에서 발급한 API 토큰)

이렇게 해 두면 **main 브랜치에 push할 때** 커밋 메시지에 있는 `GAM-xxx` 이슈가 JIRA에서 **완료**로 자동 전환됩니다 ([jira-auto-update.yml](../../.github/workflows/jira-auto-update.yml)).

---

## 2. 일상 개발 시 할 일 (백엔드·프론트 공통)

보고서는 **JIRA만 참조**합니다. 백엔드·프론트 모두 같은 GAM 프로젝트에 이슈가 있으므로, **JIRA 상태만 최신**이면 한 보고서에 둘 다 반영됩니다.

- **당신(백엔드)**: 커밋 메시지에 해당 이슈 키(`GAM-xxx`)를 포함한 뒤 **main에 push**하면, [jira-auto-update.yml](../../.github/workflows/jira-auto-update.yml)이 해당 이슈를 JIRA에서 **완료**로 전환합니다.
- **프론트 개발자**: (1) 같은 저장소에 커밋 메시지에 `GAM-xxx`(프론트 이슈 키) 포함 후 push → 동일하게 JIRA 완료 전환, 또는 (2) JIRA에서 직접 이슈 상태를 **진행 중/완료**로 변경.

예: `feat: [GAM-11] Mock API 구현`, `fix: GAM-145 버그 수정` (프론트 이슈 키도 동일 형식)

**main에 push**할 때마다 [jira-report.yml](../../.github/workflows/jira-report.yml)도 실행되어, **JIRA 기준 진행 보고서**가 `docs/jira/reports/report-YYYY-MM-DD.md`로 생성·커밋되고, 같은 제목의 GitHub Issue가 있으면 본문만 갱신됩니다.

### Push 후 보고서가 자동 생성됐는지 확인하는 방법

1. **Actions 탭**: GitHub 저장소 → **Actions** → **JIRA Progress Report** 워크플로우가 **push 이후 실행**되었는지 확인합니다. (트리거: `push` to `main` 또는 `workflow_dispatch` / 매주 월요일)
2. **최신 보고서 한곳**: `docs/jira/reports/report-latest.md` 또는 Issues에서 제목 **"프로젝트 진행 상황 보고서 (최신)"**(라벨 `latest`)인 이슈를 열면 항상 최신 스냅샷을 볼 수 있습니다.
3. **날짜별 보고서**: `docs/jira/reports/report-YYYY-MM-DD.md` 파일이 새로 생겼거나, 오늘 날짜 보고서가 최신 내용으로 커밋되었는지 확인합니다.
4. **GitHub Issue(날짜별)**: **Issues** 탭에서 제목이 "프로젝트 진행 상황 보고서 - YYYY-MM-DD"인 이슈가 생성되었거나, 같은 제목의 기존 이슈 본문이 갱신되었는지 확인합니다.

---

## 3. (선택) JIRA 날짜를 다시 넣고 싶을 때

- **이미 duedate가 있는 이슈는 덮어쓰지 않으려면**:
  ```bash
  python3 .github/scripts/jira-set-dates.py --skip-if-set
  ```
- **전부 다시 계산해서 넣으려면**:
  ```bash
  python3 .github/scripts/jira-set-dates.py
  ```
  (동일한 값이면 스킵하는 로직이 있어서, 중복으로 "추가"되지는 않습니다.)

로컬에서 실행할 때는 `JIRA_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN` 환경 변수를 설정한 뒤 실행하면 됩니다.

---

## 4. (선택) 진행 보고서

- **최신 보고서 한곳에서 보기**: 항상 최신 스냅샷을 보려면 [docs/jira/reports/report-latest.md](reports/report-latest.md)를 열거나, GitHub Issues에서 제목 **"프로젝트 진행 상황 보고서 (최신)"**(라벨 `report`, `latest`)인 이슈를 엽니다. push·수동·스케줄 실행 시마다 이 파일과 이슈 본문이 갱신됩니다. 자세한 내용은 [SCHEDULE_MANAGEMENT.md - 최종 보고서 한곳에서 보기](SCHEDULE_MANAGEMENT.md#최종-보고서-한곳에서-보기)를 참고하세요.
- **수동 실행**: Actions → **JIRA Progress Report** → **Run workflow**
  - JIRA 이슈를 조회해 `docs/jira/reports/report-YYYY-MM-DD.md`와 `report-latest.md`를 만들고, **정규 이슈만** 포함(`--canonical-only` 적용됨).
- **자동 실행**: main push 시 및 매주 **월요일 09:00 UTC**에 같은 워크플로가 실행됩니다.
- **보고서가 생성되지 않을 때**: [SCHEDULE_MANAGEMENT - 보고서가 생성되지 않을 때](SCHEDULE_MANAGEMENT.md#보고서가-생성되지-않을-때)에서 Secrets·Actions 로그 확인 방법을 참고하세요.

---

## 5. (권장) JIRA API 토큰 보안

채팅 등으로 API 토큰을 공유한 적이 있다면 보안을 위해:

- [Atlassian 계정 → 보안 → API 토큰](https://id.atlassian.com/manage-profile/security/api-tokens)에서 **기존 토큰 삭제** 후 **새로 발급**하고,
- 새 토큰을 **GitHub Secrets**의 `JIRA_API_TOKEN`에만 넣어 사용하는 것을 권장합니다.

---

## 요약 체크리스트

| 순서 | 할 일 | 필수 여부 |
|------|--------|-----------|
| 1 | GitHub Actions에 `JIRA_URL`, `JIRA_EMAIL`, `JIRA_API_TOKEN` 시크릿 등록 | 필수 (push 시 JIRA 자동 반영을 위해) |
| 2 | 작업 완료 시 커밋 메시지에 `GAM-xxx` 포함 후 main에 push | 일상 작업 |
| 3 | (선택) JIRA API 토큰 재발급 후 시크릿 갱신 | 권장 |
| 4 | (선택) 진행 보고서가 필요할 때 JIRA Progress Report 워크플로 수동 실행 | 선택 |

자세한 사용법은 [SCHEDULE_MANAGEMENT.md](SCHEDULE_MANAGEMENT.md)에 정리되어 있습니다.
