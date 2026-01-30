# 프론트엔드 개발 시작 안내 (Mock API 기준)

프론트엔드에서는 **API 프로젝트(ga-api-platform) 레포를 clone 하지 않습니다.** 백엔드에서 **전달받은 링크만** 사용해 개발합니다.

## 전달받는 링크

| 구분 | 설명 | 예시 |
|------|------|------|
| **API Base URL** | 배포된 API 서버 주소. 모든 API 요청의 기준 URL. | `https://go-almond.ddnsfree.com` |
| **API 문서 링크** | 스펙 확인용. 요청/응답 형식, TypeScript 타입 예시 등. **이 문서와 같은 저장소의 `docs/api/` 폴더**에 있음. | GitHub `docs/api` 페이지 링크 (예: `.../ga-api-platform/blob/main/docs/api/matching.md`) |

전달받은 **문서 링크**로 스펙을 확인한 뒤, **Base URL**로 API를 호출하면 됩니다. 상세 스펙·요청/응답 예시 파일은 **이 문서(FRONTEND_HANDOFF.md)와 같은 위치의 `docs/api/` 폴더** 안에 있습니다.

---

## 지금 사용 가능한 API (Week 1 완료)

아래 API의 상세 스펙·요청/응답 예시는 **같은 저장소의 `docs/api/`** 안 문서를 링크했습니다.

| 메서드 | 경로 | 설명 | 상세 문서 |
|--------|------|------|-----------|
| POST | `/api/v1/matching/run` | 매칭 실행 (body: `{"user_id":"..."}`) | [matching.md](api/matching.md) |
| GET | `/api/v1/matching/result` | 최신 매칭 결과 조회 | [matching.md](api/matching.md) |
| GET | `/api/v1/programs?type=...` | 프로그램 목록 (type: university, community_college, vocational) | [programs.md](api/programs.md) |
| GET | `/api/v1/schools/{schoolId}` | 학교 상세 조회 | [schools.md](api/schools.md) |

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

Mock 제공 후 **바로 다음** 작업은 아래와 같습니다.

| 주차 | 내용 |
|------|------|
| **Week 2** | User Profile API (실제 구현) — PUT /api/v1/user/profile, POST /api/v1/user/education, POST /api/v1/user/preference → 프로필 입력 화면 연동 |
| **Week 4** | 매칭 API 실제 전환, 학교/프로그램 실제 데이터 |
| **Week 5** | Application API, Document API, Dashboard API |

상세 일정은 전달받은 백로그/일정 링크를 참고하세요.

---

## 공통 규칙

- **응답 래퍼**: `success`, `data`, `code`, `message`, `timestamp`
- **에러 시**: `success: false`, `code`, `message` 포함
- 자세한 형식은 **같은 저장소의 `docs/api/`** 안 API 문서를 참고하세요. 예: [matching.md](api/matching.md), [programs.md](api/programs.md), [schools.md](api/schools.md), [README.md](api/README.md).

---

## 문의/이슈

API 명세 변경·추가 API 요청은 이슈 또는 팀 채널로 요청해 주세요.
