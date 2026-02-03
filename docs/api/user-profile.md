# User Profile API

사용자 프로필, 학력 정보, 유학 목표를 저장하는 API. **local** 또는 **lightsail** 프로파일에서만 사용 가능하며, **JWT 인증이 필요**합니다.

로그인 후 받은 토큰을 `Authorization: Bearer <token>` 헤더에 포함하여 호출하세요.

---

## 프로필·학력·유학목표 통합 조회

**GET** `/api/v1/user/profile`

프로필 기본 정보, 학력 정보, 유학 목표를 한 번에 조회합니다. 미저장 시 해당 섹션은 `null`입니다.

### Response (200)

```json
{
  "success": true,
  "data": {
    "profile": {
      "mbti": "INTJ",
      "tags": "체계적,논리적",
      "bio": "안녕하세요."
    },
    "education": {
      "schoolName": "테스트고등학교",
      "schoolLocation": "서울",
      "gpa": 3.5,
      "gpaScale": 4.0,
      "englishTestType": "TOEFL",
      "englishScore": 95,
      "degreeType": "고등학교",
      "degree": "고등학교",
      "major": "문과",
      "graduationDate": "2024-02-01",
      "institution": "테스트고"
    },
    "preference": {
      "targetProgram": "community_college",
      "targetMajor": "CS",
      "targetLocation": "CA",
      "budgetUsd": 50000,
      "careerGoal": "소프트웨어 엔지니어",
      "preferredTrack": "2+2"
    }
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| data.profile | object \| null | MBTI, tags, bio (미저장 시 null) |
| data.education | object \| null | 학력 정보 (미저장 시 null) |
| data.preference | object \| null | 유학 목표 (미저장 시 null) |

---

## 프로필 기본 정보 저장/수정

**PUT** `/api/v1/user/profile`

### Request

```json
{
  "mbti": "INTJ",
  "tags": "체계적,논리적",
  "bio": "안녕하세요. 유학을 준비 중입니다."
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| mbti | string | - | MBTI (최대 20자) |
| tags | string | - | 성향 태그 (쉼표 구분 등) |
| bio | string | - | 자기소개 (최대 500자) |

### Response (200)

```json
{
  "success": true,
  "data": null
}
```

---

## 학력 정보 입력

**POST** `/api/v1/user/education`

### Request

```json
{
  "schoolName": "테스트고등학교",
  "schoolLocation": "서울",
  "gpa": 3.5,
  "gpaScale": 4.0,
  "englishTestType": "TOEFL",
  "englishScore": 95,
  "degreeType": "고등학교",
  "degree": "고등학교",
  "major": "문과",
  "graduationDate": "2024-02-01",
  "institution": "테스트고"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| schoolName | string | O | 학교명 (1~255자) |
| schoolLocation | string | - | 학교 지역 |
| gpa | number | - | GPA (0~5, gpaScale 기준) |
| gpaScale | number | - | GPA 스케일 (1~5, 기본 4.0) |
| englishTestType | string | - | 영어 시험 유형 (TOEFL, IELTS 등) |
| englishScore | number | - | 영어 점수 (0~120) |
| degreeType | string | - | 학위 유형 |
| degree | string | - | 학위 (기본: 고등학교) |
| major | string | - | 전공 (최대 100자) |
| graduationDate | string | - | 졸업일 (YYYY-MM-DD) |
| institution | string | - | 기관명 (최대 255자) |

### Response (200)

```json
{
  "success": true,
  "data": null
}
```

---

## 유학 목표 설정

**POST** `/api/v1/user/preference`

### Request

```json
{
  "targetProgram": "community_college",
  "targetMajor": "CS",
  "targetLocation": "CA",
  "budgetUsd": 50000,
  "careerGoal": "소프트웨어 엔지니어",
  "preferredTrack": "2+2"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| targetProgram | string | - | 목표 프로그램 (community_college, university 등) |
| targetMajor | string | - | 희망 전공 |
| targetLocation | string | - | 선호 지역 |
| budgetUsd | number | - | 예산 (USD, 0~500000) |
| careerGoal | string | - | 커리어 목표 |
| preferredTrack | string | - | 선호 트랙 (2+2 등) |

### Response (200)

```json
{
  "success": true,
  "data": null
}
```

---

## cURL 예시

```bash
# 1. 로그인하여 토큰 발급
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test1234Z"}' | jq -r '.data.token')

# 2. 프로필 조회
curl -s http://localhost:8080/api/v1/user/profile \
  -H "Authorization: Bearer $TOKEN"

# 3. 프로필 저장
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"mbti":"INTJ","tags":"체계적","bio":"안녕하세요"}'

# 4. 학력 정보 저장
curl -X POST http://localhost:8080/api/v1/user/education \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"schoolName":"테스트고","schoolLocation":"서울","gpa":3.5,"degree":"고등학교"}'

# 5. 유학 목표 저장
curl -X POST http://localhost:8080/api/v1/user/preference \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"targetProgram":"community_college","targetMajor":"CS","budgetUsd":50000}'
```

---

## 에러 (401)

JWT가 없거나 유효하지 않을 때:

```json
{
  "success": false,
  "code": null,
  "message": "Unauthorized"
}
```
