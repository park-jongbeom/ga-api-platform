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

### 요청

- **Method**: `GET`
- **Path**: `/api/v1/matching/result`

### 성공 응답 (200)

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
