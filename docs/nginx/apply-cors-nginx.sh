#!/usr/bin/env bash
# CORS localhost 허용 nginx 설정 적용 스크립트
# 서버에서 프로젝트 루트(ga-api-platform)로 이동한 뒤 실행: bash docs/nginx/apply-cors-nginx.sh
# 전제: ga-nginx 컨테이너가 실행 중이며, docs/nginx/go-almond.swagger.conf 가 볼륨 마운트됨

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
cd "$PROJECT_ROOT"

echo "=== CORS nginx 설정 적용 ==="

if ! docker ps --format '{{.Names}}' | grep -q '^ga-nginx$'; then
    echo "ga-nginx 컨테이너가 실행 중이 아닙니다. docker-compose up -d 로 기동 후 다시 실행하세요."
    exit 1
fi

# 설정 파일 문법 검증
if docker exec ga-nginx nginx -t 2>/dev/null; then
    echo "nginx 설정 문법 OK"
else
    echo "nginx 설정 문법 오류. docs/nginx/go-almond.swagger.conf 를 확인하세요."
    exit 1
fi

# 적용: nginx 재로드 (마운트된 설정 파일을 다시 읽음)
docker exec ga-nginx nginx -s reload
echo "nginx 재로드 완료."

# 검증: localhost:5173 Origin 으로 OPTIONS 요청 시 CORS 헤더 확인
echo ""
echo "=== CORS 헤더 검증 (Origin: http://localhost:5173) ==="
if curl -sS -D - -o /dev/null \
    -H "Origin: http://localhost:5173" \
    -H "Access-Control-Request-Method: POST" \
    -H "Access-Control-Request-Headers: Content-Type" \
    -X OPTIONS \
    "https://go-almond.ddnsfree.com/api/v1/auth/login" 2>/dev/null | grep -i "Access-Control-Allow-Origin"; then
    echo "CORS 헤더 확인됨. 로컬 프론트(localhost:5173)에서 API 호출 가능합니다."
else
    echo "참고: curl 검증은 서버 외부에서 실행 시 SSL/네트워크로 실패할 수 있습니다. 브라우저에서 로그인 재시도로 확인하세요."
fi

echo ""
echo "완료."
