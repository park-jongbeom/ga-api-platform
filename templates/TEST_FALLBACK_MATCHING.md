# Fallback 매칭 테스트 목표 및 검증 가이드

## 1. 목적

DB에 학교/임베딩 데이터가 없는 환경에서도 AI API(Gemini)만으로 매칭 결과를 생성할 수 있는지 검증한다.

## 2. 전제 조건

- `school_embeddings` 테이블이 비어있거나 벡터 검색 결과가 0건이어야 한다.
- 사용자 프로필(`academic_profiles`)과 선호도(`user_preferences`)는 입력되어 있어야 한다.
- Gemini API 키가 설정되어 있어야 한다.
- `FallbackMatchingService`가 정상적으로 주입되어 있어야 한다.

## 3. 테스트 목표

1. 후보군 0건 시 Fallback 분기 진입 여부 확인
2. Gemini API 호출 성공 시 추천 결과 1~5건 생성 확인
3. 응답 `data.message`에 Fallback 안내 문구 포함 여부 확인
4. `school.id`가 `fallback-*` 패턴으로 반환되는지 확인
5. `score_breakdown`의 기본값(각 75점)이 적용되는지 확인
6. `explanation`, `pros`, `cons`에 AI 생성 텍스트가 포함되는지 확인

## 4. 성공 기준

- HTTP 200 응답
- `success: true`
- `data.message`가 아래 문구와 일치
  - `DB에 데이터가 없어 API 정보만으로 생성한 추천입니다. 실제 DB 데이터와 무관할 수 있습니다.`
- `data.total_matches`가 1 이상 (최대 5)
- `data.results` 배열 길이가 `data.total_matches`와 일치
- `data.results[].school.id`가 `fallback-*` 패턴
- `data.results[].score_breakdown`이 기본값(academic/english/budget/location/duration/career = 75)

## 5. 실패 시나리오

- Gemini API 호출 실패 → `data.results` 빈 배열, `data.total_matches: 0`
- 응답 파싱 실패 → `data.results` 빈 배열

## 6. 테스트 절차 (예시)

```bash
# 1) 로그인 (JWT 획득)
BASE_URL=http://localhost:8080
RESP=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test1234Z"}')
TOKEN=$(echo "$RESP" | jq -r '.data.token')
USER_ID=$(echo "$RESP" | jq -r '.data.user.id')

# 2) 학력 정보 입력
curl -s -X POST "$BASE_URL/api/v1/user/education" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"school_name":"테스트고등학교","degree":"고등학교","gpa":3.5,"gpa_scale":4.0,"english_test_type":"TOEFL","english_score":90}'

# 3) 선호도 입력
curl -s -X POST "$BASE_URL/api/v1/user/preference" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"target_major":"Computer Science","target_program":"CC","target_location":"California","budget_usd":30000,"career_goal":"Software Engineer","preferred_track":"TRANSFER"}'

# 4) 매칭 실행 (Fallback 기대)
curl -s -X POST "$BASE_URL/api/v1/matching/run" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"user_id\":\"$USER_ID\"}" | jq
```

## 7. 예상 응답 체크리스트

- `data.message`가 Fallback 안내 문구인지
- `data.results[0].school.id`가 `fallback-1`인지
- `data.results[0].score_breakdown.academic`이 75인지
- `data.results[0].explanation`에 한 줄 설명이 존재하는지
- `data.results[0].pros`/`cons` 배열에 값이 존재하는지
