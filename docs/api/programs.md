# 프로그램 API (Programs)

## 프로그램 목록 조회

프로그램 목록을 타입/지역 조건으로 조회합니다.

### 요청

- **Method**: `GET`
- **Path**: `/api/v1/programs`

#### Query Parameters

| 이름 | 타입 | 필수 | 기본값 | 설명 |
|------|------|------|--------|------|
| type | string | O | - | 프로그램 유형: `university`, `community_college`, `vocational` |
| state | string | X | - | 주 코드 (예: CA) |
| page | number | X | 1 | 페이지 번호 |
| size | number | X | 10 | 페이지 크기 |

### 성공 응답 (200)

```json
{
  "success": true,
  "data": {
    "total": 45,
    "page": 1,
    "size": 10,
    "programs": [
      {
        "id": "program-1-1",
        "school_id": "school-001",
        "school_name": "CA Community College #1",
        "program_name": "Computer Science AA",
        "type": "community_college",
        "degree": "AA",
        "duration": "2 years",
        "tuition": 16500,
        "state": "CA",
        "city": "Irvine",
        "opt_available": true,
        "transfer_rate": 61,
        "career_path": "Software Developer, Web Developer"
      }
    ]
  }
}
```

### 에러 응답 (400)

| code | 설명 |
|------|------|
| INVALID_PROGRAM_TYPE | 허용되지 않은 프로그램 유형입니다. |

```json
{
  "success": false,
  "code": "INVALID_PROGRAM_TYPE",
  "message": "허용되지 않은 프로그램 유형입니다.",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### cURL

```bash
curl -s "http://localhost:8080/api/v1/programs?type=community_college&state=CA&page=1&size=10"
```

### JavaScript Fetch

```js
const params = new URLSearchParams({
  type: 'community_college',
  state: 'CA',
  page: '1',
  size: '10',
});
const res = await fetch(`http://localhost:8080/api/v1/programs?${params}`);
const json = await res.json();
```
