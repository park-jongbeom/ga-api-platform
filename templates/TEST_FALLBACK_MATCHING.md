# Fallback 매칭 테스트 목표 및 검증 가이드

> **업데이트 (2026-02-04)**: Fallback 메시지 및 기본 템플릿 개선.
> - **2가지 Fallback 시나리오**: 벡터 검색 0개 + Hard Filter 0개
> - **사용자 친화 메시지**: 두 시나리오 모두 `data.message` = `"맞춤형 추천을 제공합니다."` 로 통일
> - **필터링 통계**: filterSummary 객체 (Hard Filter 0개 시나리오에만)
> - Gemini API 실패/파싱 실패 시에도 기본 추천 반환 (빈 결과 없음)
> - 프롬프트 엔지니어링: 예산·지역·전공 조건 강조, 6대 지표, 추천 유형, 확장 필드
> - **기본 추천 템플릿 확장**: CA·NY·TX·University 등 13개, 사용자 예산·지역·프로그램 유형 기반 필터링 후 5개 반환

## 1. 목적

다음 두 가지 시나리오에서 **항상 유효한 매칭 결과를 반환**하는지 검증한다:
1. **시나리오 1**: DB에 학교/임베딩 데이터가 없는 경우 (벡터 검색 0개)
2. **시나리오 2**: 벡터 검색은 성공했으나 Hard Filter에서 모두 필터링된 경우

## 2. 전제 조건

### 시나리오 1 (벡터 검색 0개)
- `school_embeddings` 테이블이 비어있거나 벡터 검색 결과가 0건이어야 한다.
- 사용자 프로필(`academic_profiles`)과 선호도(`user_preferences`)는 입력되어 있어야 한다.
- Gemini API 키가 설정되어 있어야 한다 (선택적 - 없어도 기본 추천 반환).
- `FallbackMatchingService`가 정상적으로 주입되어 있어야 한다.

### 시나리오 2 (Hard Filter 0개) **NEW**
- `school_embeddings` 테이블에 데이터가 있어야 한다 (벡터 검색 성공을 위해).
- 사용자 프로필과 선호도는 입력되어 있어야 한다.
- **낮은 예산** (예: $5,000) 또는 **영어 점수 0점** 등으로 모든 학교가 Hard Filter에서 필터링되도록 설정.
- Gemini API 키가 설정되어 있어야 한다 (선택적 - 없어도 기본 추천 반환).

## 3. 테스트 목표

1. 후보군 0건 시 Fallback 분기 진입 여부 확인
2. **Gemini API 성공 시**: AI 생성 추천 결과 1~5건 반환
3. **Gemini API 실패 시**: 기본 추천 템플릿 5건 반환 (빈 결과 없음)
4. 응답 `data.message`에 `"맞춤형 추천을 제공합니다."` 포함 여부 확인
5. `school.id`가 `fallback-*` 패턴으로 반환되는지 확인
6. 확장 필드 포함 여부: `global_ranking`, `ranking_field`, `average_salary`, `feature_badges`
7. `explanation`, `pros`, `cons`에 AI 생성 또는 기본 텍스트 포함 확인

## 4. 성공 기준

### 공통 성공 기준
- HTTP 200 응답
- `success: true`
- **`data.total_matches`가 1 이상** (최대 5) - 항상 결과 존재 보장
- `data.results` 배열 길이가 `data.total_matches`와 일치
- `data.results[].school.id`가 `fallback-*` 패턴
- `data.results[].recommendation_type`이 `safe`, `challenge`, `strategy` 중 하나
- `data.results[].pros` 최소 3개, `data.results[].cons` 최소 1개
- `data.results[].explanation`이 비어있지 않음

### 시나리오 1 (벡터 검색 0개) 추가 기준
- `data.message`가 "맞춤형 추천을 제공합니다."를 포함
- `data.filterSummary`가 `null` (벡터 검색 단계에서 종료)

### 시나리오 2 (Hard Filter 0개) 추가 기준 **NEW**
- `data.message`가 "맞춤형 추천을 제공합니다."를 포함
- **`data.filterSummary`가 존재**하고 다음 필드 포함:
  - `totalCandidates > 0`: 필터링된 총 후보 수
  - `filteredByBudget >= 0`: 예산 초과로 필터링된 수
  - `filteredByEnglish >= 0`: 영어 점수 미달로 필터링된 수
  - `minimumTuitionFound != null`: 후보 중 최저 학비

## 5. 실패 시나리오 (개선 후)

> **주의**: 개선된 로직에서는 아래 시나리오에서도 **빈 결과가 반환되지 않습니다**.

| 시나리오 | 이전 동작 | 개선 후 동작 |
|----------|----------|-------------|
| Gemini API 호출 실패 | `results: []` | 기본 추천 5개 반환 |
| JSON 파싱 실패 | `results: []` | 기본 추천 5개 반환 |
| 빈 응답 | `results: []` | 기본 추천 5개 반환 |
| 부분 파싱 성공 | 파싱된 결과만 | 파싱된 결과만 (1개 이상) |

## 6. 테스트 절차

### 시나리오 1 (벡터 검색 0개) - DB에 임베딩 데이터 없을 때

```bash
# 전제: school_embeddings 테이블이 비어있는 환경

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

### 시나리오 2 (Hard Filter 0개) - 조건에 맞는 학교 없을 때 **NEW**

```bash
# 전제: school_embeddings 테이블에 데이터가 있는 환경

# 1) 로그인 (JWT 획득)
BASE_URL=http://localhost:8080
RESP=$(curl -s -X POST "$BASE_URL/api/v1/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test1234Z"}')
TOKEN=$(echo "$RESP" | jq -r '.data.token')
USER_ID=$(echo "$RESP" | jq -r '.data.user.id')

# 2) 학력 정보 입력 (영어 점수 0으로 설정)
curl -s -X POST "$BASE_URL/api/v1/user/education" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"school_name":"강경중앙고등학교","degree":"고등학교","gpa":3.0,"gpa_scale":4.0,"english_test_type":"IELTS","english_score":0}'

# 3) 선호도 입력 (매우 낮은 예산으로 설정)
curl -s -X POST "$BASE_URL/api/v1/user/preference" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"target_major":"기계 관련","target_program":"community_college","target_location":"California","budget_usd":5000,"career_goal":"기계 관련","preferred_track":"2+2"}'

# 4) 매칭 실행 (Hard Filter 0 → Fallback 기대)
curl -s -X POST "$BASE_URL/api/v1/matching/run" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{\"user_id\":\"$USER_ID\"}" | jq
```

### 예상 응답 차이점

**시나리오 1**:
```json
{
  "data": {
    "message": "맞춤형 추천을 제공합니다.",
    "filterSummary": null
  }
}
```

**시나리오 2**:
```json
{
  "data": {
    "message": "맞춤형 추천을 제공합니다.",
    "filterSummary": {
      "totalCandidates": 29,
      "filteredByBudget": 29,
      "filteredByEnglish": 0,
      "filteredByVisa": 0,
      "minimumTuitionFound": 28000
    }
  }
}
```

## 7. 예상 응답 체크리스트

### 필수 검증 항목
- [ ] `success: true`
- [ ] `data.message`가 "맞춤형 추천을 제공합니다." 포함
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

Gemini API가 실패하면 **사용자 예산·지역·프로그램 유형**에 맞게 필터링된 5개가 반환됩니다. 템플릿 풀에는 아래와 같은 지역·유형별 학교가 포함됩니다:

| 지역/유형 | 학교 예시 | 유형 |
|----------|----------|------|
| CA (Community College) | Santa Monica College, De Anza College, Diablo Valley College, Orange Coast College, Foothill College | community_college |
| NY (Community College) | Borough of Manhattan CC, LaGuardia CC, Kingsborough CC | community_college |
| TX (Community College) | Austin CC, Houston CC, Dallas College | community_college |
| CA (University) | San Jose State University, CSU Long Beach | university |

- 예산: 학비가 예산의 70% 이하인 학교만 후보
- 프로그램 유형: `target_program`(community_college/university 등)과 일치하는 것만
- 지역: `target_location`(California, New York, Texas 등) 일치 시 우선 정렬 후 상위 5개

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
