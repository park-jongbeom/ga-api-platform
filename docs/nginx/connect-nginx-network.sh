#!/usr/bin/env bash
# ga-nginx를 ga-api-platform_ga-network에 연결하여 ga-matching-api:8080 이름 해석 가능하게 함.
# 서버에서 실행: bash connect-nginx-network.sh (또는 chmod +x 후 ./connect-nginx-network.sh)

set -e

NETWORK="ga-api-platform_ga-network"
CONTAINER="ga-nginx"

echo "1. ga-nginx 상태 확인..."
STATUS=$(docker inspect -f '{{.State.Status}}' "$CONTAINER" 2>/dev/null || echo "none")
if [ "$STATUS" = "none" ]; then
  echo "컨테이너 $CONTAINER 가 없습니다. docker-compose up -d 로 먼저 기동하세요."
  exit 1
fi
if [ "$STATUS" != "running" ]; then
  echo "컨테이너 $CONTAINER 가 실행 중이 아닙니다 (상태: $STATUS). 로그 확인: docker logs $CONTAINER"
  exit 1
fi

echo "2. 네트워크 $NETWORK 에 연결..."
if docker network connect "$NETWORK" "$CONTAINER" 2>/dev/null; then
  echo "연결 완료."
else
  if docker network inspect "$NETWORK" 2>/dev/null | grep -q "$CONTAINER"; then
    echo "이미 $NETWORK 에 연결되어 있습니다."
  else
    echo "연결 실패. 수동 실행: docker network connect $NETWORK $CONTAINER"
    exit 1
  fi
fi

echo "3. 확인..."
docker network inspect "$NETWORK" --format '{{range .Containers}}{{.Name}} {{end}}'
echo ""
echo "https://go-almond.ddnsfree.com/swagger-ui.html 접속 테스트하세요."
