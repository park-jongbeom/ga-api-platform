#!/bin/bash
# JIRA 백로그 문서 파싱 및 자동 이슈 생성 스크립트 (Bash 래퍼)

set -e

# 스크립트 디렉토리 찾기
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

cd "$PROJECT_ROOT"

# Python 스크립트 경로
PYTHON_SCRIPT="$SCRIPT_DIR/jira-backlog-importer.py"

# Python 설치 확인
if ! command -v python3 &> /dev/null; then
    echo "오류: python3가 설치되어 있지 않습니다."
    exit 1
fi

# 필요한 Python 패키지 확인
if ! python3 -c "import requests" 2>/dev/null; then
    echo "오류: requests 패키지가 설치되어 있지 않습니다."
    echo "설치 방법: pip3 install requests"
    exit 1
fi

# 환경 변수 확인
if [ -z "$JIRA_URL" ] || [ -z "$JIRA_EMAIL" ] || [ -z "$JIRA_API_TOKEN" ]; then
    echo "오류: JIRA 환경 변수가 설정되지 않았습니다."
    echo ""
    echo "사용법:"
    echo "  export JIRA_URL=https://your-domain.atlassian.net"
    echo "  export JIRA_EMAIL=your-email@example.com"
    echo "  export JIRA_API_TOKEN=YOUR_API_TOKEN"
    echo "  ./jira-backlog-importer.sh"
    echo ""
    echo "또는 명령줄 인자로 제공:"
    echo "  ./jira-backlog-importer.sh --jira-url URL --jira-email EMAIL --jira-api-token TOKEN"
    exit 1
fi

# Python 스크립트 실행
python3 "$PYTHON_SCRIPT" \
    --jira-url "$JIRA_URL" \
    --jira-email "$JIRA_EMAIL" \
    --jira-api-token "$JIRA_API_TOKEN" \
    --project-key "${JIRA_PROJECT_KEY:-QK54R}" \
    --backlog-file "${JIRA_BACKLOG_FILE:-docs/jira/JIRA_BACKLOG.md}" \
    "$@"
