#!/usr/bin/env bash
#
# 매칭 실행(RAG 기반) API 검증 스크립트
# - signup(필요 시) → login → education → preference → matching/run 순서로 호출
# - HTTP 200 및 success=true, data.results 존재 여부 검증 후 exit 0(성공) / 1(실패)
#
# 사용: BASE_URL=http://localhost:8080 ./scripts/verify-matching-api.sh
#       또는 ./scripts/verify-matching-api.sh https://go-almond.ddnsfree.com
#
set -e

# BASE_URL: 환경 변수 또는 첫 번째 인자 (없으면 http://localhost:8080)
BASE_URL="${1:-${BASE_URL:-http://localhost:8080}}"
# BASE_URL 끝 슬래시 제거
BASE_URL="${BASE_URL%/}"

EMAIL="${VERIFY_EMAIL:-test@example.com}"
PASSWORD="${VERIFY_PASSWORD:-test1234Z}"

if ! command -v jq &>/dev/null; then
  echo "jq 가 필요합니다. 설치 후 다시 실행하세요." >&2
  exit 1
fi

echo "=== 매칭 실행 API 검증 (BASE_URL=$BASE_URL) ==="

# 1) 회원가입 (실패해도 계속: 이미 있으면 400 등)
echo "[1/5] 회원가입..."
SIGNUP_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/signup" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
SIGNUP_BODY=$(echo "$SIGNUP_RESP" | head -n -1)
SIGNUP_CODE=$(echo "$SIGNUP_RESP" | tail -n 1)
if [[ "$SIGNUP_CODE" != "200" && "$SIGNUP_CODE" != "201" ]]; then
  echo "  (회원가입 스킵 또는 실패: HTTP $SIGNUP_CODE, 이미 가입된 계정일 수 있음)"
else
  echo "  회원가입 OK (HTTP $SIGNUP_CODE)"
fi

# 2) 로그인
echo "[2/5] 로그인..."
LOGIN_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$EMAIL\",\"password\":\"$PASSWORD\"}")
LOGIN_BODY=$(echo "$LOGIN_RESP" | head -n -1)
LOGIN_CODE=$(echo "$LOGIN_RESP" | tail -n 1)
if [[ "$LOGIN_CODE" != "200" ]]; then
  echo "로그인 실패 (HTTP $LOGIN_CODE). 응답: $LOGIN_BODY" >&2
  exit 1
fi
TOKEN=$(echo "$LOGIN_BODY" | jq -r '.data.token')
USER_ID=$(echo "$LOGIN_BODY" | jq -r '.data.user.id')
if [[ -z "$TOKEN" || "$TOKEN" == "null" || -z "$USER_ID" || "$USER_ID" == "null" ]]; then
  echo "로그인 응답에서 token 또는 user.id 를 찾을 수 없습니다." >&2
  exit 1
fi
echo "  로그인 OK (USER_ID=$USER_ID)"

# 3) 학력 저장
echo "[3/5] 학력 정보 저장..."
EDU_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/user/education" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"school_name":"테스트고등학교","degree":"고등학교","gpa":3.5,"gpa_scale":4.0,"english_test_type":"TOEFL","english_score":90}')
if [[ "$EDU_CODE" != "200" && "$EDU_CODE" != "201" ]]; then
  echo "학력 저장 실패 (HTTP $EDU_CODE)" >&2
  exit 1
fi
echo "  학력 저장 OK"

# 4) 유학 목표 저장
echo "[4/5] 유학 목표 저장..."
PREF_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/api/v1/user/preference" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"target_major":"Computer Science","target_program":"CC","target_location":"California","budget_usd":30000,"career_goal":"Software Engineer","preferred_track":"TRANSFER"}')
if [[ "$PREF_CODE" != "200" && "$PREF_CODE" != "201" ]]; then
  echo "유학 목표 저장 실패 (HTTP $PREF_CODE)" >&2
  exit 1
fi
echo "  유학 목표 저장 OK"

# 5) 매칭 실행
echo "[5/5] 매칭 실행 (POST /api/v1/matching/run)..."
MATCH_RESP=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/api/v1/matching/run" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"user_id\":\"$USER_ID\"}")
MATCH_BODY=$(echo "$MATCH_RESP" | head -n -1)
MATCH_CODE=$(echo "$MATCH_RESP" | tail -n 1)

if [[ "$MATCH_CODE" != "200" ]]; then
  echo "매칭 실행 실패 (HTTP $MATCH_CODE)" >&2
  echo "응답 본문: $MATCH_BODY" >&2
  exit 1
fi

SUCCESS=$(echo "$MATCH_BODY" | jq -r '.success')
if [[ "$SUCCESS" != "true" ]]; then
  echo "매칭 응답 success 가 true 가 아닙니다. 응답: $MATCH_BODY" >&2
  exit 1
fi

# data.results 존재 여부 (배열이면 OK, 빈 배열 허용)
HAS_DATA=$(echo "$MATCH_BODY" | jq -r 'if .data then "ok" else "missing" end')
HAS_RESULTS=$(echo "$MATCH_BODY" | jq -r 'if .data.results != null then "ok" else "missing" end')
if [[ "$HAS_DATA" != "ok" || "$HAS_RESULTS" != "ok" ]]; then
  echo "매칭 응답에 data 또는 data.results 가 없습니다. 응답: $MATCH_BODY" >&2
  exit 1
fi

TOTAL=$(echo "$MATCH_BODY" | jq -r '.data.total_matches // 0')
echo "  매칭 실행 OK (HTTP 200, success=true, total_matches=$TOTAL)"
echo "=== 검증 완료: 매칭 실행 API 정상 동작 ==="
exit 0
