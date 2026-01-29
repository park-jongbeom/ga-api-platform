# 프론트엔드 개발 시작 안내 (Mock API 기준)

이 레포(ga-api-platform)는 API 스펙·Mock API·작업 일정을 담고 있습니다. **GitHub 레포 링크**를 공유받았다면, Cursor에서 이 레포를 참조하며 프론트 작업을 진행하면 됩니다.

---

## Cursor에서 이 레포 참조하기

이 레포(ga-api-platform)를 Cursor에서 열어 두고, **프론트 프로젝트**에서 작업할 때:

1. Cursor 채팅/에이전트 입력창에서 **@** 를 누른 뒤, 이 레포의 **`docs/api`** 폴더 또는 **`docs/FRONTEND_HANDOFF.md`** 파일을 선택합니다.
2. 예: *"매칭 결과 화면 만들어줘. @docs/api 참고해서 API 호출해."*
3. 그러면 Cursor가 docs/api의 Markdown(요청/응답 예시, TypeScript 타입 등)을 참고해 코드를 제안합니다.

**참조하려면**: 레포를 clone 해서 로컬에 두거나, Cursor 워크스페이스에 이 레포 폴더를 추가해 두면 @로 참조할 수 있습니다.

---

## 지금 사용 가능한 API (Week 1 완료)

| 메서드 | 경로 | 설명 |
|--------|------|------|
| POST | `/api/v1/matching/run` | 매칭 실행 (body: `{"user_id":"..."}`) |
| GET | `/api/v1/matching/result` | 최신 매칭 결과 조회 |
| GET | `/api/v1/programs?type=...` | 프로그램 목록 (type: university, community_college, vocational) |
| GET | `/api/v1/schools/{schoolId}` | 학교 상세 조회 |

상세 스펙·요청/응답 예시·TypeScript 타입 예시는 **docs/api/** Markdown에 있습니다. Cursor에서 `@docs/api` 로 참조하면 됩니다.

---

## Base URL

- **로컬**: `http://localhost:8080`
- **배포**: 실제 배포 주소 (예: https://go-almond.ddnsfree.com)

---

## 로컬에서 API 확인하는 방법

백엔드 실행 후 curl/브라우저로 확인하려면 [로컬 테스트 가이드](LOCAL_TEST_GUIDE.md)를 참고하세요.

---

## 작업 일정 (다음 단계)

Mock 제공 후 **바로 다음** 작업은 아래와 같습니다.

| 주차 | 내용 |
|------|------|
| **Week 2** | User Profile API (실제 구현) — PUT /api/v1/user/profile, POST /api/v1/user/education, POST /api/v1/user/preference → 프로필 입력 화면 연동 |
| **Week 4** | 매칭 API 실제 전환, 학교/프로그램 실제 데이터 |
| **Week 5** | Application API, Document API, Dashboard API |

상세 일정은 [JIRA_BACKLOG.md](JIRA_BACKLOG.md)를 참고하세요.

---

## 공통 규칙

- **응답 래퍼**: `success`, `data`, `code`, `message`, `timestamp`
- **에러 시**: `success: false`, `code`, `message` 포함
- 자세한 형식은 [docs/api/API.md](api/API.md)를 참고하세요.

---

## 문의/이슈

API 명세 변경·추가 API 요청은 이슈 또는 팀 채널로 요청해 주세요.
