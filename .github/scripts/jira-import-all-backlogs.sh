#!/bin/bash
# 백엔드 및 프론트엔드 백로그 모두 JIRA에 등록하는 스크립트

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

PYTHON_SCRIPT="$SCRIPT_DIR/jira-backlog-importer.py"

# 환경 변수 확인
if [ -z "$JIRA_URL" ] || [ -z "$JIRA_EMAIL" ] || [ -z "$JIRA_API_TOKEN" ]; then
    echo "오류: JIRA 환경 변수가 설정되지 않았습니다."
    echo ""
    echo "사용법:"
    echo "  export JIRA_URL=https://your-domain.atlassian.net"
    echo "  export JIRA_EMAIL=your-email@example.com"
    echo "  export JIRA_API_TOKEN=YOUR_API_TOKEN"
    echo "  ./jira-import-all-backlogs.sh"
    exit 1
fi

# 프론트엔드 담당자 Account ID (홍지운)
FRONTEND_ASSIGNEE_ACCOUNT_ID="557058:e1565656-70eb-4dcb-ac30-a2880e81a8db"

echo "============================================================"
echo "JIRA 백로그 문서 일괄 등록"
echo "============================================================"
echo "프로젝트 키: GAM"
echo "백엔드 담당자: $JIRA_EMAIL"
echo "프론트엔드 담당자: 홍지운"
echo ""

# 1. 백엔드 백로그 등록
echo "============================================================"
echo "1. 백엔드 백로그 등록 시작"
echo "============================================================"
python3 "$PYTHON_SCRIPT" \
    --jira-url "$JIRA_URL" \
    --jira-email "$JIRA_EMAIL" \
    --jira-api-token "$JIRA_API_TOKEN" \
    --project-key GAM \
    --backlog-file docs/jira/JIRA_BACKLOG.md \
    --backend-assignee-email "$JIRA_EMAIL" \
    --frontend-assignee-account-id "$FRONTEND_ASSIGNEE_ACCOUNT_ID"

echo ""
echo "============================================================"
echo "2. 프론트엔드 백로그 등록 시작"
echo "============================================================"
python3 "$PYTHON_SCRIPT" \
    --jira-url "$JIRA_URL" \
    --jira-email "$JIRA_EMAIL" \
    --jira-api-token "$JIRA_API_TOKEN" \
    --project-key GAM \
    --backlog-file docs/jira/FRONT_JIRA_BACKLOG.md \
    --backend-assignee-email "$JIRA_EMAIL" \
    --frontend-assignee-account-id "$FRONTEND_ASSIGNEE_ACCOUNT_ID"

echo ""
echo "============================================================"
echo "완료!"
echo "============================================================"
