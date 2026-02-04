# 프론트엔드 개발 시작 안내 (Mock API 기준)

프론트엔드에서는 **API 프로젝트(ga-api-platform) 레포를 clone 하지 않습니다.** 백엔드에서 **전달받은 링크만** 사용해 개발합니다.

## 최근 업데이트 (2026-02-04)

- **Fallback 매칭 개선** (GAM-3 Phase 9): DB 데이터 없어도 항상 추천 결과 반환 보장 ✅
  - 프롬프트 엔지니어링 강화: 6대 매칭 지표, 추천 유형(safe/challenge/strategy), 확장 필드 명시
  - 에러 처리 강화: Gemini API 실패/파싱 실패 시에도 기본 추천 반환
  - 기본 추천 템플릿: 실제 캘리포니아 커뮤니티 칼리지 5개 기반
  - 확장 필드 지원: `global_ranking`, `average_salary`, `feature_badges` 등
  - 테스트 코드 추가: `FallbackMatchingServiceTest` (17개 테스트 케이스)

## 이전 업데이트 (2026-02-03)

- **RAG 기반 AI 매칭 엔진**: GAM-3 완료 - pgvector, Gemini AI, Rule-based 하이브리드 매칭 시스템 구현 ✅
  - Vector DB (pgvector) + 벡터 검색 (Top 20)
  - Hard Filter (4가지), Base Score (6대 지표), Path Optimization, Risk Penalty
  - Explainable AI (설명 생성)
  - `POST /api/v1/matching/run` (실제 매칭, local/lightsail 프로파일)
- **Fallback 매칭**: DB에 학교/임베딩 데이터가 없을 때 AI 추천으로 결과 생성 ✅
- **CORS**: 로컬 개발용 Origin 추가 (`http://localhost:5173`, `http://127.0.0.1:5173`, `http://localhost:3000`, `http://127.0.0.1:3000`)
- **엔티티**: School, Program 엔티티 추가 (V3 마이그레이션 schools/programs 테이블 매핑). AcademicProfile에 major, gpaScale, graduationDate, institution 반영
- **설정**: WeightConfig (매칭 가중치) 빈 등록
- **User Profile API**: `GET /api/v1/user/profile` 프로필·학력·유학목표 통합 조회 API 추가
- **Response DTO**: ProfileResponse, EducationResponse, PreferenceResponse, CompleteUserProfileResponse 추가
- **Validation**: EducationRequest/PreferenceRequest 검증 강화 (GPA·예산 범위 등)
- **Repository**: SchoolRepository, ProgramRepository 추가
- **테스트**: 성공/실패 시나리오 및 JaCoCo 커버리지 측정. 전체 28개 테스트 통과
- **문서**: API 명세는 아래 링크된 문서만 프론트 공유 대상입니다. (내부 문서는 공유 제외)
- **작업 현황**: 전체 115개 작업 중 **64개 완료**. 상세 일정은 전달받은 백로그/일정 링크를 참고하세요.

---

## 전달받는 링크

| 구분 | 설명 | 예시 |
|------|------|------|
| **API Base URL** | 배포된 API 서버 주소. 모든 API 요청의 기준 URL. | `https://go-almond.ddnsfree.com` |
| **API 문서 링크** | 스펙 확인용. 요청/응답 형식 등. 아래 표의 상세 문서는 **raw 링크**로 열람 가능. | 예: `https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/user-profile.md` |

전달받은 **문서 링크**로 스펙을 확인한 뒤, **Base URL**로 API를 호출하면 됩니다. 아래 표의 상세 문서는 Cursor/IDE에서 정확히 위치를 열기 위해 **raw GitHub URL**로 안내합니다.

---

## 지금 사용 가능한 API

아래 API의 상세 스펙·요청/응답 예시는 **raw GitHub URL**로 링크했습니다. (Cursor 등에서 정확한 문서 위치 확인용)

### Mock API (Week 1, default 프로파일) ✅ 구현 완료

| 메서드 | 경로 | 설명 | 상세 문서 | 상태 |
|--------|------|------|-----------|------|
| POST | `/api/v1/matching/run` | 매칭 실행 (Mock, 인증 불필요) | [matching.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/matching.md) | ✅ |
| GET | `/api/v1/matching/result` | 최신 매칭 결과 조회 (Mock) | [matching.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/matching.md) | ✅ |
| GET | `/api/v1/programs?type=...` | 프로그램 목록 (type: university, community_college, vocational) | [programs.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/programs.md) | ✅ |
| GET | `/api/v1/schools/{schoolId}` | 학교 상세 조회 | [schools.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/schools.md) | ✅ |

### Auth & User Profile (Week 2, local/lightsail 프로파일) ✅ 구현 완료

**참고**: Auth API·User Profile API는 DB가 연결된 환경(local/lightsail 프로파일)에서만 사용 가능합니다. 배포 시 해당 프로파일이 적용되어 있으면 사용할 수 있습니다.

| 메서드 | 경로 | 설명 | 상세 문서 | 상태 |
|--------|------|------|-----------|------|
| POST | `/api/v1/auth/signup` | 회원가입 | [auth.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/auth.md) | ✅ |
| POST | `/api/v1/auth/login` | 로그인 (JWT 발급) | [auth.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/auth.md) | ✅ |
| GET | `/api/v1/user/profile` | 프로필·학력·유학목표 통합 조회 | [user-profile.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/user-profile.md) | ✅ |
| PUT | `/api/v1/user/profile` | 프로필 기본 정보 (MBTI, 태그, 자기소개) | [user-profile.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/user-profile.md) | ✅ |
| POST | `/api/v1/user/education` | 학력 정보 입력 | [user-profile.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/user-profile.md) | ✅ |
| POST | `/api/v1/user/preference` | 유학 목표 설정 | [user-profile.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/user-profile.md) | ✅ |

### RAG 기반 매칭 API (Week 3, local/lightsail 프로파일) ✅ 구현 완료

**참고**: RAG 기반 실제 매칭 API는 DB가 연결된 환경(local/lightsail 프로파일)에서만 사용 가능합니다. Mock API(`default` 프로파일)와 동일한 엔드포인트를 사용하지만, 실제 벡터 검색 + Rule-based 알고리즘으로 매칭을 수행합니다.
**Fallback**: DB에 학교/임베딩 데이터(`school_embeddings`)가 없으면 프로필·선호도만으로 Gemini가 생성한 추천을 동일한 형식으로 반환합니다. `data.message`에 안내 문구가 포함되며, 결과의 `school.id`/`program.id`는 `fallback-1`, `fallback-2` 등의 플레이스홀더입니다.

| 메서드 | 경로 | 설명 | 상세 문서 | 상태 |
|--------|------|------|-----------|------|
| POST | `/api/v1/matching/run` | 매칭 실행 (RAG 기반, JWT 필요) | [matching.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/matching.md) | ✅ |
| GET | `/api/v1/matching/result` | 최신 매칭 결과 조회 (local/lightsail에서는 **미구현**, Week 4 예정. 현재는 NOT_IMPLEMENTED 반환) | [matching.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/matching.md) | ⏳ Week 4 |

User Profile API는 **JWT 인증이 필요**합니다. 로그인 후 받은 토큰을 `Authorization: Bearer <token>` 헤더에 포함하여 호출하세요.

**성능**:
- 벡터 검색 (Top 20): ~500ms
- Rule-based 계산: ~1초
- 전체 매칭 시간: ~2초

**매칭 알고리즘**:
1. 사용자 프로필 기반 벡터 검색 (pgvector, Gemini embedding)
2. Hard Filter (예산, 비자, 영어 점수, 입학 시기)
3. Base Score (6대 지표: 학업 20%, 영어 15%, 예산 15%, 지역 10%, 기간 10%, 진로 30%)
4. Path Optimization (GPA 낮음+예산 제한→CC, 영어 없음→Vocational 등)
5. Risk Penalty (경쟁률, 임계 점수, 의사 불명확 등)
6. Explainable AI (Gemini 기반 설명 생성)

매칭 알고리즘 상세는 백엔드 내부 문서로, 프론트 공유 문서에는 포함되지 않습니다.

### 테스트 계정 (개발/연동용)

Auth·User Profile API 연동 시 바로 로그인해서 토큰을 받을 수 있도록 테스트 계정을 제공합니다. **배포 환경(local/lightsail)에서 시드되어 있습니다.**

| 항목 | 값 |
|------|-----|
| **이메일** | `test@example.com` |
| **비밀번호** | `test1234Z` |
| **이름** | 테스트 사용자 |

- 로그인: `POST /api/v1/auth/login` body `{"email":"test@example.com","password":"test1234Z"}`
- 받은 토큰으로 `GET /api/v1/user/profile`, `PUT /api/v1/user/profile`, `POST /api/v1/user/education`, `POST /api/v1/user/preference` 등 호출 시 사용하세요.

---

## 예정 API (Week 4~5)

| 메서드 | 경로 | 설명 | 예정 시점 |
|--------|------|------|----------|
| POST | `/api/v1/bookmarks` | 보관하기 | Week 5 |
| DELETE | `/api/v1/bookmarks/{id}` | 보관 해제 | Week 5 |
| (추가 예정) | `/api/v1/applications/...` | 지원 현황 관리 | Week 5 |

---

## Base URL

| 환경 | URL | 설명 |
|------|-----|------|
| **로컬 개발** | `http://localhost:8080` | 백엔드를 로컬에서 실행 (`./gradlew bootRun`). React/Vite 로컬(5173)에서 호출 가능 (CORS 설정됨) |
| **배포** | 전달받은 URL (예: `https://go-almond.ddnsfree.com`) | 실제 배포된 API 서버 |

### 로컬 개발 시 프론트 환경 변수

`.env` 또는 `.env.local`:

```bash
VITE_API_URL=http://localhost:8080
```

API 호출 예:

```typescript
const API_URL = import.meta.env.VITE_API_URL;
const res = await fetch(`${API_URL}/api/v1/programs?type=university`);
```

로컬 연동 방법은 전달받은 가이드 또는 팀 채널을 참고하세요.

---

## API 호출 확인 방법

배포된 API 주소(전달받은 Base URL)로 브라우저 또는 curl로 호출해 확인할 수 있습니다.

```bash
curl "https://go-almond.ddnsfree.com/api/v1/programs?type=community_college"
```

(실제 주소는 전달받은 Base URL로 치환하세요.)

---

## 작업 진행 현황

| 주차 | Epic | 완료/전체 | 사용 가능 API |
|------|------|-----------|---------------|
| Week 1 | GAM-1 | 23/23 | Mock API 4개 ✅ |
| Week 2 | GAM-2 | 18/21 | Auth 2개 ✅, User Profile 4개 ✅ |
| Week 3 | GAM-3 | 21/22 | RAG 매칭 API 1개 ✅ |
| Week 4 | GAM-4 | 0/21 | - |
| Week 5 | GAM-5 | 2/19 | - |
| Week 6 | GAM-6 | 0/9 | - |

- 전체: **64/115** (백로그 기준)
- 상세 일정·작업 목록은 전달받은 백로그/일정 링크를 참고하세요.

---

## 백엔드 데이터 구조 (참고)

프론트에서 API 응답 구조를 이해하는 데 도움이 되도록, 백엔드 주요 엔티티와 DB 테이블 관계를 간략히 정리합니다.

| 테이블 | 엔티티 | 설명 | 마이그레이션 |
|--------|--------|------|-------------|
| `users` | User.kt | 사용자 계정 | V1 |
| `academic_profiles` | AcademicProfile.kt | 학력 프로필 | V1 |
| `user_preferences` | UserPreference.kt | 유학 선호도 | V1 |
| `schools` | School.kt | 학교 마스터 | V3 |
| `programs` | Program.kt | 프로그램 마스터 | V3 |
| `matching_results` | (DTO) | 매칭 결과 | V3 |
| `applications` | (미구현) | 지원 현황 | V3 |

상세 DB 스키마는 백엔드 내부 문서로, 프론트 공유 문서에는 포함되지 않습니다.

---

## 공통 규칙

- **응답 래퍼**: `success`, `data`, `code`, `message`, `timestamp`
- **에러 시**: `success: false`, `code`, `message` 포함
- 자세한 형식은 아래 raw 링크의 API 문서를 참고하세요.  
  [matching.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/matching.md) · [programs.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/programs.md) · [schools.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/schools.md) · [auth.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/auth.md) · [user-profile.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/user-profile.md) · [README.md](https://raw.githubusercontent.com/park-jongbeom/ga-api-platform/refs/heads/main/docs/api/README.md)

---

## 문의/이슈

API 명세 변경·추가 API 요청은 이슈 또는 팀 채널로 요청해 주세요.
