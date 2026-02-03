# Auth API

회원가입 및 로그인 API. **local** 또는 **lightsail** 프로파일에서만 사용 가능합니다.

## 회원가입

**POST** `/api/v1/auth/signup`

### Request

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| email | string | O | 이메일 (유효한 형식) |
| password | string | O | 비밀번호 (최소 8자) |

### Response (200)

```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "user": {
      "id": "uuid",
      "email": "user@example.com",
      "fullName": "user"
    }
  }
}
```

### 에러 (400)

```json
{
  "success": false,
  "data": null,
  "code": "EMAIL_ALREADY_EXISTS",
  "message": "이미 등록된 이메일입니다",
  "timestamp": "2026-02-02T00:00:00Z"
}
```

---

## 로그인

**POST** `/api/v1/auth/login`

### Request

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

### Response (200)

회원가입과 동일한 구조.

### 에러 (401)

```json
{
  "success": false,
  "data": null,
  "code": "INVALID_CREDENTIALS",
  "message": "이메일 또는 비밀번호가 올바르지 않습니다",
  "timestamp": "2026-02-02T00:00:00Z"
}
```

---

## 인증 필요 API 호출

`/api/v1/user/*` 등 인증이 필요한 API는 `Authorization` 헤더에 JWT를 포함합니다:

```
Authorization: Bearer <token>
```

---

## cURL 예시

```bash
# 회원가입
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test1234Z"}'

# 로그인
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test1234Z"}'

# 인증 필요 API (token을 위 응답에서 복사)
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <token>" \
  -d '{"mbti":"INTJ","tags":["체계적"],"bio":"안녕하세요"}'
```
