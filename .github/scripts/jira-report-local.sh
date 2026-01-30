#!/bin/bash
# push 전 로컬에서 JIRA 진행 보고서 생성 후 스테이징(및 선택적 커밋)
# 사용: export JIRA_URL=... JIRA_EMAIL=... JIRA_API_TOKEN=... 후 ./.github/scripts/jira-report-local.sh [--commit]

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

# JIRA 환경: 없으면 docs/jira/jira.env 참조 (docs는 push 제외)
if [ -f "docs/jira/jira.env" ]; then
  set -a
  # shellcheck source=/dev/null
  . "docs/jira/jira.env"
  set +a
fi

COMMIT=false
for arg in "$@"; do
  if [ "$arg" = "--commit" ]; then
    COMMIT=true
    break
  fi
done

# Python 설치 확인
if ! command -v python3 &> /dev/null; then
  echo "오류: python3가 설치되어 있지 않습니다." >&2
  exit 1
fi

# requests 패키지 확인
if ! python3 -c "import requests" 2>/dev/null; then
  echo "오류: requests 패키지가 설치되어 있지 않습니다." >&2
  echo "Ubuntu: sudo apt update && sudo apt install -y python3-requests" >&2
  echo "또는 가상환경: python3 -m venv .venv && ./.venv/bin/pip install requests" >&2
  exit 1
fi

# JIRA 환경 변수 확인 (환경 변수 또는 docs/jira/jira.env)
if [ -z "$JIRA_URL" ] || [ -z "$JIRA_EMAIL" ] || [ -z "$JIRA_API_TOKEN" ]; then
  echo "오류: JIRA 환경 변수가 설정되지 않았습니다." >&2
  echo "" >&2
  echo "다음 중 하나를 사용하세요:" >&2
  echo "  1) env.jira.example 을 복사해 docs/jira/jira.env 에 값을 채운 뒤 실행" >&2
  echo "  2) export JIRA_URL=... JIRA_EMAIL=... JIRA_API_TOKEN=... 후 실행" >&2
  echo "" >&2
  echo "  ./.github/scripts/jira-report-local.sh [--commit]" >&2
  exit 1
fi

REPORT_DATE=$(date +%Y-%m-%d)
# 보고서 경로는 .github/jira-config.json 과 동일 규칙 (로컬/커밋/CI 공통)
REPORTS_DIR=$(python3 -c "import json; c=json.load(open('.github/jira-config.json')); print(c.get('reportsDir','reports'))" 2>/dev/null || echo "reports")
REPORT_FILE="${REPORTS_DIR}/report-${REPORT_DATE}.md"
LATEST_FILE="${REPORTS_DIR}/report-latest.md"

mkdir -p "$REPORTS_DIR"

# 보고서 옵션은 .github/jira-config.json 에서 로드 (로컬/커밋/CI 공통)
python3 .github/scripts/jira-generate-report.py \
  --canonical-only \
  --output "$REPORT_FILE" \
  --date "$REPORT_DATE"

cp "$REPORT_FILE" "$LATEST_FILE"
echo "보고서 생성: $REPORT_FILE, $LATEST_FILE" >&2

git add "$REPORT_FILE" "$LATEST_FILE"

if [ "$COMMIT" = true ]; then
  git commit -m "docs: JIRA 진행 보고서 ${REPORT_DATE}"
  echo ""
  echo "커밋 완료. 다음 명령으로 push 하세요: git push"
else
  echo ""
  echo "다음 명령으로 커밋 후 push 하세요:"
  echo "  git commit -m \"docs: JIRA 진행 보고서 ${REPORT_DATE}\""
  echo "  git push"
fi
