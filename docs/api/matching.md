# 매칭 API (Matching)

## 매칭 실행

사용자 ID를 입력받아 Mock 매칭 결과를 반환합니다. 로컬/Mock 단계에서는 인증 생략 가능.

### 요청

- **Method**: `POST`
- **Path**: `/api/v1/matching/run`
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

```bash
curl -X POST http://localhost:8080/api/v1/matching/run \
  -H "Content-Type: application/json" \
  -d '{"user_id":"550e8400-e29b-41d4-a716-446655440000"}'
```

### JavaScript Fetch

```js
const res = await fetch('http://localhost:8080/api/v1/matching/run', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
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
