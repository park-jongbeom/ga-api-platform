# Go Almond AI 매칭 MVP - 프론트엔드 협업 제안서

**작성일**: 2026-01-26  
**업데이트**: 실제 작업 시간 반영  
**버전**: 1.1  
**대상**: 프론트엔드 개발자  
**백엔드 담당**: Go Almond Backend Team

---

## 📋 목차

1. [프로젝트 개요](#프로젝트-개요)
2. [협업 전략 및 일정](#협업-전략-및-일정)
3. [API 명세 상세](#api-명세-상세)
4. [데이터 구조 및 응답 형식](#데이터-구조-및-응답-형식)
5. [Mock API 제공 계획](#mock-api-제공-계획)
6. [Cursor 기반 협업 방법](#cursor-기반-협업-방법)
7. [개발 환경 및 도구](#개발-환경-및-도구)
8. [마일스톤 및 통합 계획](#마일스톤-및-통합-계획)
9. [Q&A 및 커뮤니케이션](#qa-및-커뮤니케이션)

---

## 프로젝트 개요

### 프로젝트 목표
**Go Almond AI 매칭 시스템**은 유학을 준비하는 학생들에게 AI 기반 학교 추천을 제공하는 서비스입니다.

### 핵심 기능
- ✅ **AI 매칭**: 사용자 프로필 기반 학교 추천 (Top 5)
- ✅ **6대 지표 시각화**: 학업/영어/예산/지역/기간/진로 적합도
- ✅ **User Profile 관리**: 학력 정보, 유학 목표 입력
- ✅ **Application 관리**: 지원 현황 추적
- ✅ **Document 관리**: 서류 업로드

### MVP 범위
- Rule-based 매칭 알고리즘 (딥러닝은 Phase 2)
- Mock 학교 데이터 20개
- 실시간 크롤링 제외 (수동 데이터 관리)

---

## 협업 전략 및 일정

### 백엔드 개발 일정 (6주)

| 주차 | 백엔드 작업 내용 | 프론트엔드 작업 가능 범위 | 통합 가능 시점 |
|-----|----------------|---------------------|-------------|
| **Week 1** | Mock API + Swagger 문서 | ✅ **전체 UI 개발 시작** | **즉시 가능** |
| **Week 2** | User Profile API + DB | ✅ 프로필 입력 화면 연동 | Week 2 종료 |
| **Week 3** | AI 매칭 엔진 (내부 로직) | Mock 데이터로 계속 개발 | - |
| **Week 4** | 매칭 API + 학교 데이터 | ✅ **실제 매칭 결과 연동** | Week 4 종료 |
| **Week 5** | Application + Document API | ✅ 지원 관리 화면 연동 | Week 5 종료 |
| **Week 6** | Lightsail 배포 + 보안 | ✅ 통합 테스트 | Week 6 종료 |

### 🎯 핵심 협업 전략

#### 1단계: Mock API로 병렬 개발 (Week 1~3)
```
프론트: Mock API로 전체 UI 개발 (3주)
백엔드: 실제 로직 구현 (3주)
→ 서로 기다리지 않고 독립적으로 작업
```

#### 2단계: 실제 API 통합 (Week 4~5)
```
프론트: Mock → 실제 API 엔드포인트 변경 (설정 파일만 수정)
백엔드: 실제 데이터 제공
→ 빠른 통합 가능
```

#### 3단계: 최종 테스트 및 배포 (Week 6)
```
프론트 + 백엔드: 통합 테스트
→ 프로덕션 배포
```

### 백엔드 작업 환경
- **가용 시간**: 월~목, 19:00~21:30 (일 2.5시간)
- **주당 총 10시간**
- **실제 실적**: 초기 31시간에 AI 컨설턴트 서비스 (RAG, 보안, 배포 포함) 완성 ✅
- **생산성**: Cursor AI 활용으로 기존 IDE 대비 2-3배 향상
- **배포**: Push 시 GitHub Actions가 자동으로 Docker 빌드 및 Lightsail 배포

→ **Mock API 최우선 제공** + **CI/CD 자동화**로 일정 안정성 확보

---

## API 명세 상세

### Base URL
```
개발 환경: http://localhost:8084/api/v1
프로덕션: https://api.goalmond.com/api/v1
```

### 인증 방식
```http
Authorization: Bearer {JWT_TOKEN}
```

모든 API는 JWT 인증 필요 (인증 API 제외)

---

### 1. 인증 API (Auth Service - Port 8081)

#### 1.1 회원가입
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123!",
  "name": "홍길동",
  "birth_date": "2000-01-01"
}
```

**Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "name": "홍길동",
    "email": "user@example.com"
  }
}
```

#### 1.2 로그인
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword123!"
}
```

**Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com"
  }
}
```

---

### 2. User Profile API (Matching Service - Port 8084)

#### 2.1 기본 정보 저장/수정
```http
PUT /api/v1/user/profile
Authorization: Bearer {token}
Content-Type: application/json

{
  "mbti": "INTJ",
  "personality_tags": ["planner", "goal-oriented", "independent"],
  "bio": "I want to study Computer Science in the US"
}
```

**Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "mbti": "INTJ",
    "personality_tags": ["planner", "goal-oriented", "independent"],
    "bio": "I want to study Computer Science in the US",
    "updated_at": "2026-01-26T10:30:00Z"
  }
}
```

#### 2.2 학력 정보 입력
```http
POST /api/v1/user/education
Authorization: Bearer {token}
Content-Type: application/json

{
  "education_level": "highschool",
  "school_name": "서울고등학교",
  "school_region": "Seoul, Korea",
  "gpa": 3.8,
  "grading_system": "4.0",
  "english_test_type": "TOEFL",
  "english_score": 95,
  "transcript_summary": "수학, 물리 우수"
}
```

**Response 201 Created**:
```json
{
  "success": true,
  "data": {
    "id": "660e8400-e29b-41d4-a716-446655440001",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "education_level": "highschool",
    "school_name": "서울고등학교",
    "gpa": 3.8,
    "english_test_type": "TOEFL",
    "english_score": 95,
    "created_at": "2026-01-26T10:35:00Z"
  }
}
```

#### 2.3 유학 목표 설정
```http
POST /api/v1/user/preference
Authorization: Bearer {token}
Content-Type: application/json

{
  "target_program": "community_college",
  "desired_major": "Computer Science",
  "desired_career": "Software Engineer",
  "budget_min": 15000,
  "budget_max": 25000,
  "preferred_state": "CA",
  "preferred_city": "Irvine",
  "study_period": "2 years",
  "post_graduation_plan": "Transfer to 4-year university"
}
```

**Response 201 Created**:
```json
{
  "success": true,
  "data": {
    "id": "770e8400-e29b-41d4-a716-446655440002",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "target_program": "community_college",
    "desired_major": "Computer Science",
    "budget_min": 15000,
    "budget_max": 25000,
    "preferred_state": "CA",
    "created_at": "2026-01-26T10:40:00Z"
  }
}
```

---

### 3. AI 매칭 API

#### 3.1 매칭 실행
```http
POST /api/v1/matching/run
Authorization: Bearer {token}
Content-Type: application/json

{
  "user_id": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "matching_id": "880e8400-e29b-41d4-a716-446655440003",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "total_matches": 5,
    "execution_time_ms": 2340,
    "results": [
      {
        "rank": 1,
        "school": {
          "id": "school-001",
          "name": "Irvine Valley College",
          "type": "community_college",
          "state": "CA",
          "city": "Irvine",
          "tuition": 18000,
          "image_url": "https://cdn.goalmond.com/schools/ivc.jpg"
        },
        "program": {
          "id": "program-001",
          "name": "Computer Science AA",
          "degree": "AA",
          "duration": "2 years",
          "opt_available": true
        },
        "total_score": 87.5,
        "score_breakdown": {
          "academic": 18,
          "english": 14,
          "budget": 15,
          "location": 10,
          "duration": 9,
          "career": 28
        },
        "recommendation_type": "safe",
        "explanation": "이 학교는 예산 대비 학비가 안정적이며, 귀하의 영어 점수로 바로 입학이 가능하고, 졸업 후 OPT 연계 확률이 높아 추천되었습니다.",
        "pros": [
          "예산 여유 충분 ($7,000)",
          "영어 점수 입학 기준 초과 (TOEFL 95 vs 70)",
          "OPT 가능",
          "높은 편입 성공률 (75%)"
        ],
        "cons": [
          "경쟁률 다소 높음 (45%)"
        ]
      },
      {
        "rank": 2,
        "school": {
          "id": "school-002",
          "name": "Santa Monica College",
          "type": "community_college",
          "state": "CA",
          "city": "Santa Monica",
          "tuition": 20000,
          "image_url": "https://cdn.goalmond.com/schools/smc.jpg"
        },
        "program": {
          "id": "program-002",
          "name": "Computer Science AS",
          "degree": "AS",
          "duration": "2 years",
          "opt_available": true
        },
        "total_score": 84.2,
        "score_breakdown": {
          "academic": 17,
          "english": 13,
          "budget": 13,
          "location": 10,
          "duration": 9,
          "career": 27
        },
        "recommendation_type": "safe",
        "explanation": "명문 편입 학교로 UCLA 편입률이 높으며, 귀하의 학업 성적으로 충분히 입학 가능합니다.",
        "pros": [
          "UCLA 편입률 1위",
          "캠퍼스 위치 우수",
          "OPT 가능"
        ],
        "cons": [
          "예산 임계 ($5,000 여유)",
          "경쟁률 높음 (35%)"
        ]
      },
      {
        "rank": 3,
        "school": {
          "id": "school-003",
          "name": "De Anza College",
          "type": "community_college",
          "state": "CA",
          "city": "Cupertino",
          "tuition": 17000,
          "image_url": "https://cdn.goalmond.com/schools/deanza.jpg"
        },
        "program": {
          "id": "program-003",
          "name": "Computer Science AA",
          "degree": "AA",
          "duration": "2 years",
          "opt_available": true
        },
        "total_score": 82.8,
        "score_breakdown": {
          "academic": 17,
          "english": 14,
          "budget": 15,
          "location": 8,
          "duration": 9,
          "career": 26
        },
        "recommendation_type": "challenge",
        "explanation": "실리콘밸리 중심에 위치하여 IT 기업 인턴십 기회가 많습니다.",
        "pros": [
          "실리콘밸리 위치",
          "IT 기업 네트워킹 우수",
          "예산 적합"
        ],
        "cons": [
          "선호 도시와 거리 있음"
        ]
      }
      // ... 총 5개 결과
    ],
    "created_at": "2026-01-26T11:00:00Z"
  }
}
```

#### 3.2 매칭 결과 조회
```http
GET /api/v1/matching/result
Authorization: Bearer {token}
```

**Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "matching_id": "880e8400-e29b-41d4-a716-446655440003",
    "results": [ /* 위와 동일한 결과 배열 */ ],
    "created_at": "2026-01-26T11:00:00Z"
  }
}
```

---

### 4. 프로그램 & 학교 API

#### 4.1 프로그램 리스트
```http
GET /api/v1/programs?type=community_college&state=CA&page=1&size=10
Authorization: Bearer {token}
```

**Query Parameters**:
- `type`: `university` | `community_college` | `vocational` | `elementary`
- `state`: 주 코드 (예: `CA`, `NY`, `TX`)
- `page`: 페이지 번호 (기본값: 1)
- `size`: 페이지 크기 (기본값: 10, 최대: 50)

**Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "total": 45,
    "page": 1,
    "size": 10,
    "programs": [
      {
        "id": "program-001",
        "school_id": "school-001",
        "school_name": "Irvine Valley College",
        "program_name": "Computer Science AA",
        "type": "community_college",
        "degree": "AA",
        "duration": "2 years",
        "tuition": 18000,
        "state": "CA",
        "city": "Irvine",
        "opt_available": true,
        "transfer_rate": 75,
        "career_path": "Software Developer, Web Developer"
      }
      // ... 10개 항목
    ]
  }
}
```

#### 4.2 학교 상세
```http
GET /api/v1/schools/{schoolId}
Authorization: Bearer {token}
```

**Response 200 OK**:
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
    }
  }
}
```

---

### 5. Application 관리 API

#### 5.1 지원 생성
```http
POST /api/v1/applications
Authorization: Bearer {token}
Content-Type: application/json

{
  "school_id": "school-001",
  "program_id": "program-001"
}
```

**Response 201 Created**:
```json
{
  "success": true,
  "data": {
    "id": "app-001",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "school_id": "school-001",
    "program_id": "program-001",
    "status": "draft",
    "progress": 0,
    "created_at": "2026-01-26T11:30:00Z"
  }
}
```

#### 5.2 지원 현황 조회
```http
GET /api/v1/applications
Authorization: Bearer {token}
```

**Response 200 OK**:
```json
{
  "success": true,
  "data": [
    {
      "id": "app-001",
      "school": {
        "id": "school-001",
        "name": "Irvine Valley College",
        "image_url": "https://cdn.goalmond.com/schools/ivc.jpg"
      },
      "program": {
        "id": "program-001",
        "name": "Computer Science AA"
      },
      "status": "in_progress",
      "progress": 45,
      "submitted_at": null,
      "created_at": "2026-01-26T11:30:00Z",
      "updated_at": "2026-01-27T10:00:00Z"
    }
  ]
}
```

**Application Status Enum**:
- `draft`: 준비 중
- `in_progress`: 작성 중
- `submitted`: 제출 완료
- `under_review`: 심사 중
- `accepted`: 합격
- `rejected`: 불합격

---

### 6. Document 관리 API

#### 6.1 문서 업로드
```http
POST /api/v1/documents/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data

{
  "file": [binary],
  "document_type": "transcript",
  "application_id": "app-001"
}
```

**Response 201 Created**:
```json
{
  "success": true,
  "data": {
    "id": "doc-001",
    "user_id": "550e8400-e29b-41d4-a716-446655440000",
    "document_type": "transcript",
    "file_name": "transcript.pdf",
    "file_size": 2457600,
    "file_url": "https://storage.goalmond.com/documents/doc-001/transcript.pdf",
    "created_at": "2026-01-26T12:00:00Z"
  }
}
```

**Document Type Enum**:
- `transcript`: 성적증명서
- `recommendation`: 추천서
- `essay`: 에세이
- `passport`: 여권
- `financial`: 재정증명서
- `other`: 기타

#### 6.2 문서 리스트
```http
GET /api/v1/documents
Authorization: Bearer {token}
```

**Response 200 OK**:
```json
{
  "success": true,
  "data": [
    {
      "id": "doc-001",
      "document_type": "transcript",
      "file_name": "transcript.pdf",
      "file_size": 2457600,
      "file_url": "https://storage.goalmond.com/documents/doc-001/transcript.pdf",
      "created_at": "2026-01-26T12:00:00Z"
    }
  ]
}
```

---

### 7. Dashboard API

#### 7.1 메인 대시보드
```http
GET /api/v1/dashboard
Authorization: Bearer {token}
```

**Response 200 OK**:
```json
{
  "success": true,
  "data": {
    "user": {
      "name": "홍길동",
      "profile_completion": 85
    },
    "matching": {
      "total_matches": 5,
      "last_matched_at": "2026-01-26T11:00:00Z",
      "top_recommendation": {
        "school_name": "Irvine Valley College",
        "score": 87.5
      }
    },
    "applications": {
      "total": 3,
      "in_progress": 1,
      "submitted": 1,
      "accepted": 0,
      "rejected": 1
    },
    "documents": {
      "total": 5,
      "pending": 2
    },
    "recent_activities": [
      {
        "type": "matching",
        "message": "AI 매칭 완료: 5개 학교 추천",
        "timestamp": "2026-01-26T11:00:00Z"
      },
      {
        "type": "application",
        "message": "Irvine Valley College 지원 시작",
        "timestamp": "2026-01-26T11:30:00Z"
      }
    ]
  }
}
```

---

## 데이터 구조 및 응답 형식

### 공통 응답 구조

#### 성공 응답
```json
{
  "success": true,
  "data": { /* 실제 데이터 */ }
}
```

#### 에러 응답
```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "입력 데이터가 유효하지 않습니다.",
    "details": [
      {
        "field": "email",
        "message": "이메일 형식이 올바르지 않습니다."
      }
    ]
  }
}
```

### HTTP 상태 코드

| 코드 | 의미 | 사용 사례 |
|-----|------|---------|
| 200 | OK | 성공적인 GET, PUT |
| 201 | Created | 성공적인 POST (리소스 생성) |
| 204 | No Content | 성공적인 DELETE |
| 400 | Bad Request | 잘못된 요청 데이터 |
| 401 | Unauthorized | 인증 실패 (토큰 없음/만료) |
| 403 | Forbidden | 권한 없음 |
| 404 | Not Found | 리소스 없음 |
| 409 | Conflict | 리소스 충돌 (중복) |
| 429 | Too Many Requests | Rate Limit 초과 |
| 500 | Internal Server Error | 서버 에러 |

### 에러 코드 목록

```typescript
enum ErrorCode {
  // 인증 관련
  UNAUTHORIZED = "UNAUTHORIZED",
  TOKEN_EXPIRED = "TOKEN_EXPIRED",
  INVALID_CREDENTIALS = "INVALID_CREDENTIALS",
  
  // 검증 관련
  VALIDATION_ERROR = "VALIDATION_ERROR",
  INVALID_REQUEST = "INVALID_REQUEST",
  
  // 리소스 관련
  NOT_FOUND = "NOT_FOUND",
  ALREADY_EXISTS = "ALREADY_EXISTS",
  
  // 비즈니스 로직
  MATCHING_FAILED = "MATCHING_FAILED",
  INSUFFICIENT_DATA = "INSUFFICIENT_DATA",
  
  // 시스템
  INTERNAL_ERROR = "INTERNAL_ERROR",
  SERVICE_UNAVAILABLE = "SERVICE_UNAVAILABLE"
}
```

---

## Mock API 제공 계획

### Week 1 제공 사항

#### 1. Mock API 엔드포인트
```
✅ POST /api/v1/matching/run
✅ GET /api/v1/matching/result
✅ GET /api/v1/programs?type={type}
✅ GET /api/v1/schools/{schoolId}
```

#### 2. Mock 데이터 시나리오

**시나리오 A: 안정권 추천 (고GPA + 충분한 예산)**
- User Profile: GPA 3.8, TOEFL 95, Budget $15k-$25k
- 결과: 5개 학교, 평균 점수 85+

**시나리오 B: 도전권 추천 (중간GPA + 제한된 예산)**
- User Profile: GPA 3.2, TOEFL 80, Budget $10k-$18k
- 결과: 5개 학교, 평균 점수 70-84

**시나리오 C: 전략 경로 (저GPA + 취업 목표)**
- User Profile: GPA 2.8, IELTS 6.0, Budget $12k-$20k
- 결과: Vocational School 중심 추천, 점수 60-75

#### 3. Swagger UI
```
http://localhost:8084/swagger-ui.html
```
- 모든 API "Try it out" 가능
- 요청/응답 예시 포함
- 인증 토큰 설정 가능

#### 4. Postman Collection
```json
// GoAlmond_API.postman_collection.json
{
  "info": {
    "name": "Go Almond API",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Auth",
      "item": [
        {
          "name": "Register",
          "request": { /* ... */ }
        },
        {
          "name": "Login",
          "request": { /* ... */ }
        }
      ]
    },
    {
      "name": "Matching",
      "item": [
        {
          "name": "Run Matching",
          "request": { /* ... */ }
        }
      ]
    }
  ]
}
```

---

## Cursor 기반 협업 방법

### Cursor AI 활용 가이드

#### 1. API 통합 시 프롬프트 예시

```
나는 Go Almond 프론트엔드 개발자야.
백엔드 API 문서를 참고해서 매칭 결과 페이지를 만들고 싶어.

@FRONTEND_COOPERATION_PROPOSAL.md 문서를 참고해서:
1. AI 매칭 API (POST /api/v1/matching/run)를 호출하는 React Hook을 만들어줘
2. 6대 지표를 Radar Chart로 시각화하는 컴포넌트를 만들어줘
3. Top 5 추천 학교를 카드 형식으로 보여주는 UI를 만들어줘

기술 스택:
- React 18
- TypeScript
- TanStack Query (React Query)
- Recharts (차트 라이브러리)
- Tailwind CSS
```

#### 2. TypeScript 타입 정의

백엔드 응답 구조를 TypeScript 타입으로 정의:

```typescript
// types/matching.ts
export interface MatchingResult {
  matching_id: string;
  user_id: string;
  total_matches: number;
  execution_time_ms: number;
  results: SchoolRecommendation[];
  created_at: string;
}

export interface SchoolRecommendation {
  rank: number;
  school: School;
  program: Program;
  total_score: number;
  score_breakdown: ScoreBreakdown;
  recommendation_type: 'safe' | 'challenge' | 'strategic';
  explanation: string;
  pros: string[];
  cons: string[];
}

export interface School {
  id: string;
  name: string;
  type: 'university' | 'community_college' | 'vocational' | 'elementary';
  state: string;
  city: string;
  tuition: number;
  image_url: string;
}

export interface Program {
  id: string;
  name: string;
  degree: string;
  duration: string;
  opt_available: boolean;
}

export interface ScoreBreakdown {
  academic: number;
  english: number;
  budget: number;
  location: number;
  duration: number;
  career: number;
}
```

#### 3. API 클라이언트 예시 (React Query)

```typescript
// hooks/useMatching.ts
import { useMutation, useQuery } from '@tanstack/react-query';
import { MatchingResult } from '@/types/matching';

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8084/api/v1';

export const useRunMatching = () => {
  return useMutation({
    mutationFn: async (userId: string) => {
      const response = await fetch(`${API_BASE_URL}/matching/run`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        },
        body: JSON.stringify({ user_id: userId })
      });
      
      if (!response.ok) {
        throw new Error('Matching failed');
      }
      
      const result = await response.json();
      return result.data as MatchingResult;
    }
  });
};

export const useMatchingResult = () => {
  return useQuery({
    queryKey: ['matching-result'],
    queryFn: async () => {
      const response = await fetch(`${API_BASE_URL}/matching/result`, {
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`
        }
      });
      
      if (!response.ok) {
        throw new Error('Failed to fetch result');
      }
      
      const result = await response.json();
      return result.data as MatchingResult;
    }
  });
};
```

#### 4. 환경 변수 설정

```bash
# .env.development (Mock API 사용)
NEXT_PUBLIC_API_URL=http://localhost:8084/api/v1
NEXT_PUBLIC_USE_MOCK=true

# .env.production (실제 API 사용)
NEXT_PUBLIC_API_URL=https://api.goalmond.com/api/v1
NEXT_PUBLIC_USE_MOCK=false
```

Mock에서 실제 API로 전환:
```typescript
const API_BASE_URL = process.env.NEXT_PUBLIC_USE_MOCK 
  ? 'http://localhost:8084/api/v1'  // Mock API
  : 'https://api.goalmond.com/api/v1';  // Real API
```

---

## 개발 환경 및 도구

### 백엔드 기술 스택
- **언어**: Kotlin
- **프레임워크**: Spring Boot 3.4+
- **데이터베이스**: PostgreSQL 17
- **인증**: JWT
- **문서화**: Swagger (SpringDoc OpenAPI 3.0)
- **배포**: AWS Lightsail

### 권장 프론트엔드 기술 스택
```json
{
  "framework": "React 18 / Next.js 14",
  "language": "TypeScript",
  "state": "TanStack Query (React Query)",
  "styling": "Tailwind CSS",
  "charts": "Recharts / Chart.js",
  "forms": "React Hook Form + Zod",
  "http": "Fetch API / Axios"
}
```

### CORS 설정
백엔드에서 허용할 프론트엔드 도메인:
```
개발: http://localhost:3000
스테이징: https://dev.goalmond.com
프로덕션: https://app.goalmond.com
```

### API 테스트 도구
1. **Swagger UI**: `http://localhost:8084/swagger-ui.html`
2. **Postman**: Collection 제공 예정
3. **cURL**: 문서 내 예시 포함

---

## 마일스톤 및 통합 계획

### Milestone 1: Mock API 제공 (Week 1 종료)
**날짜**: 2026-02-02 (예상)

**백엔드 제공**:
- ✅ Mock API 4개 엔드포인트
- ✅ Swagger UI
- ✅ 3가지 시나리오 Mock 데이터
- ✅ 협업 문서 (이 문서)

**프론트엔드 시작 가능**:
- 전체 UI 개발
- Mock 데이터로 테스트
- 상태 관리 구조 확립

---

### Milestone 2: User Profile API 통합 (Week 2 종료)
**날짜**: 2026-02-09 (예상)

**백엔드 제공**:
- ✅ User Profile API 3개
- ✅ DB 연동 완료
- ✅ 실제 데이터 저장/조회

**프론트엔드 통합**:
- 프로필 입력 화면 실제 API 연동
- 데이터 검증 및 에러 처리
- 회원가입 플로우 완성

**통합 테스트 항목**:
- [ ] 회원가입 → 로그인 → 프로필 입력
- [ ] 학력 정보 저장 확인
- [ ] 유학 목표 설정 확인

---

### Milestone 3: AI 매칭 API 통합 (Week 4 종료)
**날짜**: 2026-02-23 (예상)

**백엔드 제공**:
- ✅ AI 매칭 엔진 완성
- ✅ 실제 학교 데이터 20개
- ✅ 매칭 결과 API

**프론트엔드 통합**:
- Mock → 실제 API 전환 (환경변수만 변경)
- 실제 매칭 결과 표시
- 6대 지표 시각화

**통합 테스트 항목**:
- [ ] 프로필 완성 → 매칭 실행
- [ ] Top 5 결과 표시
- [ ] Radar Chart 정상 렌더링
- [ ] 설명 문구 표시

---

### Milestone 4: Application API 통합 (Week 5 종료)
**날짜**: 2026-03-02 (예상)

**백엔드 제공**:
- ✅ Application 관리 API
- ✅ Document 업로드 API
- ✅ Dashboard API

**프론트엔드 통합**:
- 지원하기 버튼 연동
- 지원 현황 페이지
- 문서 업로드 기능
- 대시보드 통합

**통합 테스트 항목**:
- [ ] 학교 선택 → 지원 생성
- [ ] 지원 상태 변경
- [ ] 파일 업로드 (10MB 제한)
- [ ] Dashboard 데이터 표시

---

### Milestone 5: 최종 배포 (Week 6 종료)
**날짜**: 2026-03-09 (예상)

**백엔드 제공**:
- ✅ Lightsail 배포 완료
- ✅ HTTPS 적용
- ✅ 프로덕션 환경 설정

**프론트엔드 배포**:
- 프로덕션 빌드
- 환경변수 전환
- CORS 최종 확인

**최종 테스트 항목**:
- [ ] E2E 테스트 (회원가입 → 매칭 → 지원)
- [ ] 성능 테스트 (Lighthouse)
- [ ] 모바일 반응형 확인
- [ ] 브라우저 호환성 테스트

---

## Q&A 및 커뮤니케이션

### 질문 채널

#### 1. 긴급 이슈 (24시간 내 응답)
- **Slack**: #goalmond-dev 채널
- **이메일**: backend@goalmond.com
- **이슈**: API 장애, 배포 문제

#### 2. 일반 질문 (48시간 내 응답)
- **GitHub Issues**: API 명세 변경 요청
- **Notion**: 협업 문서 질문
- **이메일**: 일반 문의

#### 3. 정기 미팅
- **주간 싱크**: 매주 월요일 오전 10시 (30분)
- **통합 테스트**: Milestone 달성 시점

### 자주 묻는 질문 (FAQ)

#### Q1. Mock API에서 실제 API로 전환할 때 코드 수정이 많이 필요한가요?
**A**: 아니요. 환경변수(`NEXT_PUBLIC_API_URL`)만 변경하면 됩니다. API 엔드포인트와 응답 구조가 동일하도록 설계되어 있습니다.

#### Q2. API 응답이 느리면 어떻게 하나요?
**A**: 
1. Loading State 표시 (Skeleton UI 권장)
2. Timeout 설정 (5초)
3. 에러 발생 시 재시도 로직

#### Q3. 인증 토큰이 만료되면?
**A**: 
- 401 에러 발생 시 자동으로 로그아웃 처리
- Refresh Token은 Phase 2에서 구현 예정
- 현재는 Access Token만 사용 (유효기간 24시간)

#### Q4. 파일 업로드 크기 제한은?
**A**: 
- 최대 10MB
- 허용 형식: PDF, JPG, PNG
- 초과 시 413 에러 반환

#### Q5. API 명세가 변경되면?
**A**: 
1. GitHub Issue로 변경 사항 공지
2. Swagger UI 자동 업데이트
3. 이 문서 업데이트
4. Breaking Change는 버전 업데이트 (v2)

#### Q6. 매칭 결과가 없으면?
**A**: 
```json
{
  "success": true,
  "data": {
    "matching_id": "...",
    "total_matches": 0,
    "results": [],
    "message": "조건에 맞는 학교가 없습니다. 프로필을 조정해보세요."
  }
}
```

#### Q7. Rate Limit 정책은?
**A**: 
- 기본: 분당 10회
- 매칭 API: 분당 5회
- 초과 시 429 에러
- 프론트에서 Debounce 권장

---

## 부록

### A. Radar Chart 데이터 변환 예시

```typescript
// 6대 지표를 Recharts Radar Chart 형식으로 변환
const convertToRadarData = (scoreBreakdown: ScoreBreakdown) => {
  return [
    { subject: '학업', score: scoreBreakdown.academic, fullMark: 20 },
    { subject: '영어', score: scoreBreakdown.english, fullMark: 15 },
    { subject: '예산', score: scoreBreakdown.budget, fullMark: 15 },
    { subject: '지역', score: scoreBreakdown.location, fullMark: 10 },
    { subject: '기간', score: scoreBreakdown.duration, fullMark: 10 },
    { subject: '진로', score: scoreBreakdown.career, fullMark: 30 }
  ];
};
```

### B. 추천 타입별 배지 색상

```typescript
const recommendationBadge = {
  safe: {
    label: '안정권',
    color: 'bg-green-100 text-green-800',
    icon: '🎯'
  },
  challenge: {
    label: '도전권',
    color: 'bg-yellow-100 text-yellow-800',
    icon: '⚖️'
  },
  strategic: {
    label: '전략 경로',
    color: 'bg-blue-100 text-blue-800',
    icon: '🔄'
  }
};
```

### C. 에러 처리 예시

```typescript
const handleApiError = (error: any) => {
  if (error.response?.status === 401) {
    // 인증 만료
    localStorage.removeItem('token');
    router.push('/login');
  } else if (error.response?.status === 429) {
    // Rate Limit
    toast.error('요청이 너무 많습니다. 잠시 후 다시 시도해주세요.');
  } else {
    // 일반 에러
    toast.error(error.response?.data?.error?.message || '오류가 발생했습니다.');
  }
};
```

---

## 문서 변경 이력

| 버전 | 날짜 | 변경 내용 | 작성자 |
|-----|------|---------|-------|
| 1.0 | 2026-01-26 | 초안 작성 | Backend Team |

---

## 연락처

**백엔드 팀**:
- Email: backend@goalmond.com
- Slack: @backend-team
- GitHub: @goalmond/backend

**프로젝트 관리**:
- JIRA: https://goalmond.atlassian.net
- Notion: https://notion.so/goalmond

---

**이 문서를 검토하신 후, 다음 사항을 확인해주세요**:

- [ ] API 명세가 프론트엔드 요구사항을 충족하는가?
- [ ] Mock API 제공 시점(Week 1)이 적절한가?
- [ ] 데이터 구조가 프론트엔드에서 사용하기 편한가?
- [ ] 추가로 필요한 API가 있는가?
- [ ] 협업 방식(Cursor, GitHub)에 동의하는가?

**피드백 제공 방법**:
1. 이 문서에 직접 코멘트
2. GitHub Issue 생성
3. Slack DM

우리는 효율적인 협업을 위해 최선을 다하겠습니다! 🚀
