# 학교 API (Schools)

## 학교 상세 조회

학교 ID를 기준으로 상세 정보를 조회합니다.

### 요청

- **Method**: `GET`
- **Path**: `/api/v1/schools/{schoolId}`

#### Path Parameters

| 이름 | 타입 | 설명 |
|------|------|------|
| schoolId | string | 학교 ID (예: school-001) |

### 성공 응답 (200)

```json
{
  "success": true,
  "data": {
    "id": "school-001",
    "name": "Irvine Valley College",
    "type": "community_college",
    "state": "CA",
    "city": "Irvine",
    "tuition": 18000,
    "living_cost": 15000,
    "ranking": 15,
    "description": "Irvine Valley College is a premier community college in Orange County...",
    "campus_info": "Modern campus with state-of-the-art facilities",
    "dormitory": false,
    "dining": true,
    "programs": [
      {
        "id": "program-001",
        "name": "Computer Science AA",
        "degree": "AA",
        "duration": "2 years"
      },
      {
        "id": "program-002",
        "name": "Business Administration AS",
        "degree": "AS",
        "duration": "2 years"
      }
    ],
    "acceptance_rate": 45,
    "transfer_rate": 75,
    "graduation_rate": 68,
    "images": [
      "https://cdn.goalmond.com/schools/ivc-campus-1.jpg",
      "https://cdn.goalmond.com/schools/ivc-campus-2.jpg"
    ],
    "website": "https://www.ivc.edu",
    "contact": {
      "email": "admissions@ivc.edu",
      "phone": "+1-949-451-5100",
      "address": "5500 Irvine Center Dr, Irvine, CA 92618"
    },
    "global_ranking": "Top 50 in California",
    "average_salary": 65000,
    "alumni_network_count": 15000,
    "feature_badges": "Top Transfer Rate, Best Value",
    "international_email": "international@ivc.edu",
    "international_phone": "+1-949-451-5200",
    "employment_rate": 82.5,
    "facilities": {
      "library": true,
      "sports_center": true,
      "computer_labs": 5
    },
    "staff_info": {
      "international_advisor": "Jane Doe",
      "office_hours": "Mon-Fri 9-5"
    },
    "esl_program": {
      "available": true,
      "levels": 6,
      "cost": 3000
    },
    "international_support": {
      "orientation": true,
      "counseling": true,
      "visa_assistance": true
    }
  }
}
```

### 에러 응답 (404)

| code | 설명 |
|------|------|
| SCHOOL_NOT_FOUND | 학교 정보를 찾을 수 없습니다. |

```json
{
  "success": false,
  "code": "SCHOOL_NOT_FOUND",
  "message": "학교 정보를 찾을 수 없습니다.",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### cURL

```bash
curl -s http://localhost:8080/api/v1/schools/school-001
```

### JavaScript Fetch

```js
const schoolId = 'school-001';
const res = await fetch(`http://localhost:8080/api/v1/schools/${schoolId}`);
const json = await res.json();
```
