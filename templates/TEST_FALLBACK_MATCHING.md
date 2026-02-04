# Fallback 매칭 테스트 목표 및 검증 가이드

> **업데이트 (2026-02-04)**: GAM-3 Phase 9에서 Fallback 로직이 대폭 개선되었습니다.
> - Gemini API 실패/파싱 실패 시에도 기본 추천 반환 (빈 결과 없음)
> - 프롬프트 엔지니어링 강화 (6대 지표, 추천 유형, 확장 필드)
> - 실제 캘리포니아 커뮤니티 칼리지 기반 기본 추천 템플릿

## 1. 목적

DB에 학교/임베딩 데이터가 없는 환경에서도 **항상 유효한 매칭 결과를 반환**하는지 검증한다.

## 2. 전제 조건

- `school_embeddings` 테이블이 비어있거나 벡터 검색 결과가 0건이어야 한다.
- 사용자 프로필(`academic_profiles`)과 선호도(`user_preferences`)는 입력되어 있어야 한다.
- Gemini API 키가 설정되어 있어야 한다 (선택적 - 없어도 기본 추천 반환).
- `FallbackMatchingService`가 정상적으로 주입되어 있어야 한다.

## 3. 테스트 목표

1. 후보군 0건 시 Fallback 분기 진입 여부 확인
2. **Gemini API 성공 시**: AI 생성 추천 결과 1~5건 반환
3. **Gemini API 실패 시**: 기본 추천 템플릿 5건 반환 (빈 결과 없음)
4. 응답 `data.message`에 Fallback 안내 문구 포함 여부 확인
5. `school.id`가 `fallback-*` 패턴으로 반환되는지 확인
6. 확장 필드 포함 여부: `global_ranking`, `ranking_field`, `average_salary`, `feature_badges`
7. `explanation`, `pros`, `cons`에 AI 생성 또는 기본 텍스트 포함 확인

## 4. 성공 기준

- HTTP 200 응답
- `success: true`
- `data.message`가 아래 문구와 일치
  - `DB에 데이터가 없어 API 정보만으로 생성한 추천입니다. 실제 DB 데이터와 무관할 수 있습니다.`
- **`data.total_matches`가 1 이상** (최대 5) - 항상 결과 존재 보장
- `data.results` 배열 길이가 `data.total_matches`와 일치
- `data.results[].school.id`가 `fallback-*` 패턴
- `data.results[].recommendation_type`이 `safe`, `challenge`, `strategy` 중 하나
- `data.results[].pros` 최소 3개, `data.results[].cons` 최소 1개
- `data.results[].explanation`이 비어있지 않음

## 5. 실패 시나리오 (개선 후)

> **주의**: 개선된 로직에서는 아래 시나리오에서도 **빈 결과가 반환되지 않습니다**.

| 시나리오 | 이전 동작 | 개선 후 동작 |
|----------|----------|-------------|
| Gemini API 호출 실패 | `results: []` | 기본 추천 5개 반환 |
| JSON 파싱 실패 | `results: []` | 기본 추천 5개 반환 |
| 빈 응답 | `results: []` | 기본 추천 5개 반환 |
| 부분 파싱 성공 | 파싱된 결과만 | 파싱된 결과만 (1개 이상) |

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
  -d '{"target_major":"Computer Science","target_program":"community_college","target_location":"California","budget_usd":30000,"career_goal":"Software Engineer","preferred_track":"편입"}'

# 4) 매칭 실행 (Fallback 기대)
curl -s -X POST "$BASE_URL/api/v1/matching/run" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"user_id\":\"$USER_ID\"}" | jq
```

## 7. 예상 응답 체크리스트

### 필수 검증 항목
- [ ] `success: true`
- [ ] `data.message`가 Fallback 안내 문구 포함
- [ ] `data.total_matches >= 1` (항상 1개 이상)
- [ ] `data.results[0].school.id`가 `fallback-1` 패턴
- [ ] `data.results[0].recommendation_type`이 `safe`/`challenge`/`strategy` 중 하나
- [ ] `data.results[0].explanation`이 비어있지 않음
- [ ] `data.results[0].pros` 배열에 최소 3개 값 존재
- [ ] `data.results[0].cons` 배열에 최소 1개 값 존재

### 선택 검증 항목 (AI 생성 시)
- [ ] `data.results[0].school.global_ranking` 값 존재 (null 허용)
- [ ] `data.results[0].school.feature_badges` 배열 존재
- [ ] `data.results[0].score_breakdown` 객체 내 6개 지표 존재

## 8. 기본 추천 템플릿 (Gemini API 실패 시)

Gemini API가 실패하면 아래 학교들이 기본 추천으로 반환됩니다:

| 순위 | 학교명 | 유형 | 추천 유형 |
|------|--------|------|----------|
| 1 | Santa Monica College | community_college | safe |
| 2 | De Anza College | community_college | challenge |
| 3 | Diablo Valley College | community_college | safe |
| 4 | Orange Coast College | community_college | strategy |
| 5 | Foothill College | community_college | strategy |

## 9. 프롬프트 엔지니어링 개선 사항

개선된 프롬프트는 다음 정보를 명시적으로 요청합니다:

### 매칭 기준 (6대 지표)
1. 학업 적합도 (20%): GPA와 학교 입학 요건 비교
2. 영어 적합도 (15%): 영어 점수와 학교 요건 비교
3. 예산 적합도 (15%): 학비+생활비가 예산 내인지
4. 지역 선호 (10%): 희망 지역과의 일치도
5. 기간 적합도 (10%): 프로그램 기간과 목표 일치도
6. 진로 연계성 (30%): 커리어 목표와 프로그램 연관성

### 추천 유형 정의
- `safe`: 합격 가능성 높음 (예산 적합, 입학 요건 충족)
- `challenge`: 도전적 선택 (명문대, 경쟁률 높음)
- `strategy`: 전략적 선택 (편입 경로, 비용 효율)

### 확장 필드 (매칭 리포트용)
- `global_ranking`: 글로벌 랭킹 (예: "#4")
- `ranking_field`: 랭킹 기준 전공
- `average_salary`: 평균 초봉 (USD)
- `alumni_network_count`: 동문 네트워크 규모
- `feature_badges`: 특징 배지 배열

## 10. 관련 파일

- **서비스 로직**: `src/main/kotlin/com/goalmond/api/service/matching/FallbackMatchingService.kt`
- **테스트 코드**: `src/test/kotlin/com/goalmond/api/service/matching/FallbackMatchingServiceTest.kt`
- **통합 테스트**: `src/test/kotlin/com/goalmond/api/service/matching/MatchingEngineServiceTest.kt`
