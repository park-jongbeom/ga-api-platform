# 프론트엔드 개발 시작 안내 (Mock API 기준)

프론트엔드에서는 **API 프로젝트(ga-api-platform) 레포를 clone 하지 않습니다.** 백엔드에서 **전달받은 링크만** 사용해 개발합니다.

## 전달받는 링크

| 구분 | 설명 | 예시 |
|------|------|------|
| **API Base URL** | 배포된 API 서버 주소. 모든 API 요청의 기준 URL. | `https://go-almond.ddnsfree.com` |
| **API 문서 링크** | 스펙 확인용. 요청/응답 형식, TypeScript 타입 예시 등. **이 문서와 같은 저장소의 `docs/api/` 폴더**에 있음. | GitHub `docs/api` 페이지 링크 (예: `.../ga-api-platform/blob/main/docs/api/matching.md`) |

전달받은 **문서 링크**로 스펙을 확인한 뒤, **Base URL**로 API를 호출하면 됩니다. 상세 스펙·요청/응답 예시 파일은 **이 문서(FRONTEND_HANDOFF.md)와 같은 위치의 `docs/api/` 폴더** 안에 있습니다.

---

## 지금 사용 가능한 API

아래 API의 상세 스펙·요청/응답 예시는 **같은 저장소의 `docs/api/`** 안 문서를 링크했습니다.

### Mock API (Week 1, default 프로파일)

| 메서드 | 경로 | 설명 | 상세 문서 |
|--------|------|------|-----------|
| POST | `/api/v1/matching/run` | 매칭 실행 (body: `{"user_id":"..."}`) | [matching.md](api/matching.md) |
| GET | `/api/v1/matching/result` | 최신 매칭 결과 조회 | [matching.md](api/matching.md) |
| GET | `/api/v1/programs?type=...` | 프로그램 목록 (type: university, community_college, vocational) | [programs.md](api/programs.md) |
| GET | `/api/v1/schools/{schoolId}` | 학교 상세 조회 | [schools.md](api/schools.md) |

### Auth & User Profile (Week 2, local/lightsail 프로파일)

**참고**: Auth API·User Profile API는 DB가 연결된 환경(local/lightsail 프로파일)에서만 사용 가능합니다. 배포 시 해당 프로파일이 적용되어 있으면 사용할 수 있습니다.

| 메서드 | 경로 | 설명 | 상세 문서 |
|--------|------|------|-----------|
| POST | `/api/v1/auth/signup` | 회원가입 | [auth.md](api/auth.md) |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) | [auth.md](api/auth.md) |
| PUT | `/api/v1/user/profile` | 프로필 기본 정보 (MBTI, 태그, 자기소개) | [user-profile.md](api/user-profile.md) |
| POST | `/api/v1/user/education` | 학력 정보 입력 | [user-profile.md](api/user-profile.md) |
| POST | `/api/v1/user/preference` | 유학 목표 설정 | [user-profile.md](api/user-profile.md) |

User Profile API는 **JWT 인증이 필요**합니다. 로그인 후 받은 토큰을 `Authorization: Bearer <token>` 헤더에 포함하여 호출하세요.

### 테스트 계정 (개발/연동용)

Auth·User Profile API 연동 시 바로 로그인해서 토큰을 받을 수 있도록 테스트 계정을 제공합니다. **배포 환경(local/lightsail)에서 시드되어 있습니다.**

| 항목 | 값 |
|------|-----|
| **이메일** | `test@example.com` |
| **비밀번호** | `test1234` |
| **이름** | 테스트 사용자 |

- 로그인: `POST /api/v1/auth/login` body `{"email":"test@example.com","password":"test1234"}`
- 받은 토큰으로 `PUT /api/v1/user/profile`, `POST /api/v1/user/education`, `POST /api/v1/user/preference` 등 호출 시 사용하세요.

---

## 예정 API (Week 4~5)

| 메서드 | 경로 | 설명 | 예정 시점 |
|--------|------|------|----------|
| POST | `/api/v1/bookmarks` | 보관하기 | Week 5 |
| DELETE | `/api/v1/bookmarks/{id}` | 보관 해제 | Week 5 |
| (매칭 API 실제 연동) | `/api/v1/matching/...` | 매칭 실행·결과 (실제 DB) | Week 4 |

---

## Base URL

실제 사용하는 주소는 **전달받은 배포 URL**을 사용합니다.

- **배포**: 전달받은 API Base URL (예: `https://go-almond.ddnsfree.com`)

---

## API 호출 확인 방법

배포된 API 주소(전달받은 Base URL)로 브라우저 또는 curl로 호출해 확인할 수 있습니다.

```bash
curl "https://go-almond.ddnsfree.com/api/v1/programs?type=community_college"
```

(실제 주소는 전달받은 Base URL로 치환하세요.)

---

## 작업 일정 (다음 단계)

| 주차 | API | 내용 |
|------|-----|------|
| **Week 2** (완료) | Auth API | POST /api/v1/auth/signup, POST /api/v1/auth/login → 회원가입/로그인 연동 |
| **Week 2** (완료) | User Profile API | PUT /api/v1/user/profile, POST /api/v1/user/education, POST /api/v1/user/preference → 프로필 입력 화면 연동 |
| **Week 4** | 매칭 API | POST /api/v1/matching/run, GET /api/v1/matching/result → 매칭 결과 UI 연동 |
| **Week 5** | Application API | POST /api/v1/applications, GET /api/v1/applications, PATCH /api/v1/applications/{id}/status |
| **Week 5** | Bookmark API | POST /api/v1/bookmarks, DELETE /api/v1/bookmarks/{id} → 보관하기 기능 연동 |
| **Week 5** | Dashboard API | GET /api/v1/dashboard |
| **Week 5** | Document API | POST /api/v1/documents/upload, GET /api/v1/documents |

상세 일정은 전달받은 백로그/일정 링크를 참고하세요.

---

## 공통 규칙

- **응답 래퍼**: `success`, `data`, `code`, `message`, `timestamp`
- **에러 시**: `success: false`, `code`, `message` 포함
- 자세한 형식은 **같은 저장소의 `docs/api/`** 안 API 문서를 참고하세요. 예: [matching.md](api/matching.md), [programs.md](api/programs.md), [schools.md](api/schools.md), [auth.md](api/auth.md), [user-profile.md](api/user-profile.md), [README.md](api/README.md).

---

## 문의/이슈

API 명세 변경·추가 API 요청은 이슈 또는 팀 채널로 요청해 주세요.
