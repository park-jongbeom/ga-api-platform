# 매칭 API (Matching)

## 매칭 실행

사용자 ID를 입력받아 매칭 결과를 반환합니다.

**프로파일별 동작**:
- **`default` 프로파일** (Mock): Mock 데이터 반환, 인증 불필요
- **`local`/`lightsail` 프로파일** (실제): RAG 기반 AI 매칭 수행, **JWT 인증 필요**

### RAG 기반 실제 매칭 (`local`/`lightsail`)

**알고리즘 단계**:
1. 벡터 검색 (pgvector + Gemini embedding, Top 20 후보)
2. Hard Filter (예산, 비자, 영어 점수, 입학 시기)
3. Base Score (6대 지표: 학업 20%, 영어 15%, 예산 15%, 지역 10%, 기간 10%, 진로 30%)
4. Path Optimization (최적 경로 가점)
5. Risk Penalty (리스크 패널티)
6. Explainable AI (설명 생성)

**성능**: 전체 매칭 시간 ~2초

상세: [docs/RAG_ARCHITECTURE.md](../RAG_ARCHITECTURE.md)

### 요청

- **Method**: `POST`
- **Path**: `/api/v1/matching/run`
- **Headers**: 
  - `Content-Type: application/json`
  - `Authorization: Bearer <token>` (local/lightsail 프로파일만, default는 불필요)
- **Body**: `application/json`

```json
{
  "user_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

#### TypeScript

```ts
interface MatchingRunRequest {
  user_id: string;
}
```

### 성공 응답 (200)

```json
{
  "success": true,
  "data": {
    "matching_id": "880e8400-e29b-41d4-a716-446655440003",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "total_matches": 5,
    "execution_time_ms": 2340,
    "results": [],
    "created_at": "2024-01-01T00:00:00Z"
  }
}
```

(실제 `results` 배열에는 [공통 타입 (TypeScript)](API.md#공통-타입-typescript)의 `MatchingResult` 항목이 포함됩니다.)

**MatchingResult 확장 필드 (매칭 리포트용)**  
- `estimated_roi`: number — 연간 예상 ROI (%).

**results[].school (SchoolSummary) 확장 필드**  
- `global_ranking`: string | null — 글로벌 랭킹 표시 (예: "#4").  
- `ranking_field`: string | null — 랭킹 기준 전공 (예: "Computer Science").  
- `average_salary`: number | null — 평균 초봉 USD.  
- `alumni_network_count`: number | null — 동문 네트워크 규모.  
- `feature_badges`: string[] — 특징 배지 (예: ["OPT STEM ELIGIBLE", "ON-CAMPUS HOUSING"]).

샘플 응답 예시:
- 정상 응답: `templates/sample-matching-response-normal.json`
- Fallback 응답: `templates/sample-matching-response-fallback.json`

**Fallback (DB 데이터 없을 때 또는 조건에 맞는 학교 없을 때)**: 두 가지 시나리오에서 **프로필·선호도만으로 Gemini가 생성한 추천**을 동일한 `MatchingResponse` 형식으로 반환합니다.

**Fallback 시나리오**:
1. **벡터 검색 0개**: DB에 `school_embeddings` 데이터가 없는 경우
2. **Hard Filter 0개** (NEW): 벡터 검색은 성공했으나 모든 학교가 Hard Filter(예산, 영어 점수 등)에서 필터링된 경우

**Fallback 개선 (2026-02-04)**:
- **하이브리드 방식**: 필터링 이유를 설명하면서 AI 추천도 함께 제공
- **항상 결과 반환 보장**: Gemini API 실패/파싱 실패 시에도 기본 추천 템플릿(캘리포니아 CC 5개) 반환
- **상세 메시지**: 필터링 통계(예산 초과 수, 최저 학비 등)를 포함한 개선 방안 안내
- **필터링 통계**: `data.filterSummary` 객체로 상세 정보 제공 (Hard Filter 0개 시나리오에만)
- **프롬프트 엔지니어링**: 6대 매칭 지표, 추천 유형(safe/challenge/strategy), 확장 필드 명시
- **확장 필드 지원**: `global_ranking`, `average_salary`, `feature_badges` 등 매칭 리포트 필드 포함

**메시지 예시**:
- 시나리오 1 (벡터 검색 0개): `"DB에 데이터가 없어 API 정보만으로 생성한 추천입니다. 실제 DB 데이터와 무관할 수 있습니다."`
- 시나리오 2 (Hard Filter 0개): `"⚠️ 입력하신 조건으로는 적합한 학교가 없어 AI 추천을 제공합니다.\n\n필터링 이유:\n• 29개 학교 예산 초과 (최저 학비: $28,000)\n\n아래는 조건을 완화한 추천입니다."`

`data.message`가 설정되어 있으면 Fallback 모드이므로, 클라이언트에서 이 문구를 사용자에게 노출하여 안내할 수 있습니다. `results` 내 항목의 `school.id` / `program.id`는 `fallback-1`, `fallback-2` 등 플레이스홀더입니다.

Fallback 테스트 절차는 `templates/TEST_FALLBACK_MATCHING.md`를 참고하세요.

### 에러 응답

| 상태 | code | 설명 |
|------|------|------|
| 400 | INVALID_REQUEST | 요청 형식 오류 |
| 500 | INTERNAL_SERVER_ERROR | 서버 오류 |

### cURL

**Mock API (`default` 프로파일)**:
```bash
curl -X POST http://localhost:8080/api/v1/matching/run \
  -H "Content-Type: application/json" \
  -d '{"user_id":"550e8400-e29b-41d4-a716-446655440000"}'
```

**실제 매칭 (`local`/`lightsail` 프로파일, JWT 필요)**:
```bash
# 1. 로그인하여 토큰 받기
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test1234Z"}' | jq -r '.data.token')

# 2. 토큰으로 매칭 실행
curl -X POST http://localhost:8080/api/v1/matching/run \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"user_id":"550e8400-e29b-41d4-a716-446655440000"}'
```

### JavaScript Fetch

**Mock API (`default` 프로파일)**:
```js
const res = await fetch('http://localhost:8080/api/v1/matching/run', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ user_id: '550e8400-e29b-41d4-a716-446655440000' }),
});
const json = await res.json();
```

**실제 매칭 (`local`/`lightsail` 프로파일, JWT 필요)**:
```js
// 1. 로그인하여 토큰 받기
const loginRes = await fetch('http://localhost:8080/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'test@example.com', password: 'test1234Z' }),
});
const { data: { token } } = await loginRes.json();

// 2. 토큰으로 매칭 실행
const res = await fetch('http://localhost:8080/api/v1/matching/run', {
  method: 'POST',
  headers: { 
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`,
  },
  body: JSON.stringify({ user_id: '550e8400-e29b-41d4-a716-446655440000' }),
});
const json = await res.json();
```

---

## 최신 매칭 결과 조회

가장 최근에 실행된 매칭 결과를 반환합니다.

**프로파일별 동작**:
- **`default` 프로파일 (Mock)**: 정상 응답을 반환합니다.
- **`local`/`lightsail` 프로파일**: 현재 **미구현**입니다. 이 엔드포인트를 호출하면 `success: false`, `code: "NOT_IMPLEMENTED"`, `message: "매칭 이력 조회는 Week 4에 구현됩니다. /api/v1/matching/run을 사용하세요."` 를 반환합니다. 실제 매칭 결과는 `POST /api/v1/matching/run` 응답으로 받으세요.

### 요청

- **Method**: `GET`
- **Path**: `/api/v1/matching/result`

### 성공 응답 (200, default 프로파일만)

동일한 `ApiResponse<MatchingResponse>` 형식. `data`에 매칭 결과 객체가 포함됩니다.

### 에러 응답 (404)

| code | 설명 |
|------|------|
| MATCHING_RESULT_NOT_FOUND | 매칭 결과가 없습니다. (아직 한 번도 매칭 실행하지 않은 경우) |

```json
{
  "success": false,
  "code": "MATCHING_RESULT_NOT_FOUND",
  "message": "매칭 결과가 없습니다.",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### cURL

```bash
curl -s http://localhost:8080/api/v1/matching/result
```

### JavaScript Fetch

```js
const res = await fetch('http://localhost:8080/api/v1/matching/result');
const json = await res.json();
```

---

## 검증

매칭 실행 API가 정상 동작하는지(HTTP 200, `success=true`, `data.results` 존재) 한 번에 검증하려면 **검증 스크립트**를 사용합니다.

- **스크립트**: 프로젝트 루트 `scripts/verify-matching-api.sh` (필요: `curl`, `jq`)
- **사용 예**: `BASE_URL=http://localhost:8080 ./scripts/verify-matching-api.sh` (로컬), `BASE_URL=https://도메인 ./scripts/verify-matching-api.sh` (배포)
- **성공**: exit 0. **실패**: exit 1.

검증 목표·성공 기준·실패 시 확인 순서: [docs/DEPLOY_AND_API_TEST_GUIDE.md](../DEPLOY_AND_API_TEST_GUIDE.md) 상단 「매칭 실행 API 검증 목표」 참고.
