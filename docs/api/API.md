# Go Almond Matching API

AI 매칭 Mock API 문서. Base URL: `http://localhost:8080` (또는 배포 환경의 API 주소).

## 공통 사항

- **Content-Type**: `application/json`
- **응답 형식**: 모든 성공 응답은 `ApiResponse<T>` 래퍼를 사용합니다.

### ApiResponse<T>

| 필드 | 타입 | 설명 |
|------|------|------|
| `success` | boolean | 요청 성공 여부 |
| `data` | T \| null | 응답 데이터 (에러 시 null) |
| `code` | string \| null | 에러 코드 (실패 시) |
| `message` | string \| null | 에러 메시지 (실패 시) |
| `timestamp` | string (ISO 8601) \| null | 타임스탬프 |

### 에러 응답 예시

```json
{
  "success": false,
  "code": "INVALID_PROGRAM_TYPE",
  "message": "허용되지 않은 프로그램 유형입니다.",
  "timestamp": "2024-01-01T00:00:00Z"
}
```

### 공통 타입 (TypeScript)

Cursor 등에서 참조용으로 사용할 수 있는 타입 예시입니다.

```ts
interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  code?: string | null;
  message?: string | null;
  timestamp?: string | null; // ISO 8601
}

// 매칭 결과 항목 (matching/run, matching/result 의 data.results[] 요소)
interface MatchingResult {
  rank: number;
  school: { id: string; name: string; type: string; state: string; city: string; tuition: number; image_url: string };
  program: { id: string; name: string; degree: string; duration: string; opt_available: boolean };
  total_score: number;
  score_breakdown: { academic: number; english: number; budget: number; location: number; duration: number; career: number };
  recommendation_type: string;
  explanation: string;
  pros: string[];
  cons: string[];
}
```

(JSON 응답은 snake_case이므로 위 필드명은 API 응답 키와 동일하게 사용하세요.)
샘플 응답 JSON은 `templates/sample-matching-response-normal.json`, `templates/sample-matching-response-fallback.json`을 참고하세요.

## API 목록

| API | 메서드 | 경로 | 설명 |
|-----|--------|------|------|
| [회원가입](auth.md#회원가입) | POST | `/api/v1/auth/signup` | 이메일/비밀번호 회원가입 (local 프로파일) |
| [로그인](auth.md#로그인) | POST | `/api/v1/auth/login` | 로그인 및 JWT 발급 (local 프로파일) |
| [프로필 저장](user-profile.md#프로필-기본-정보-저장수정) | PUT | `/api/v1/user/profile` | MBTI, 태그, 자기소개 (JWT 필요) |
| [학력 정보](user-profile.md#학력-정보-입력) | POST | `/api/v1/user/education` | 학교, GPA, 영어 점수 (JWT 필요) |
| [유학 목표](user-profile.md#유학-목표-설정) | POST | `/api/v1/user/preference` | 목표 프로그램, 예산 (JWT 필요) |
| [매칭 실행](matching.md#매칭-실행) | POST | `/api/v1/matching/run` | 사용자 ID로 Mock 매칭 실행 |
| [최신 매칭 결과 조회](matching.md#최신-매칭-결과-조회) | GET | `/api/v1/matching/result` | 가장 최근 매칭 결과 조회 |
| [프로그램 목록 조회](programs.md#프로그램-목록-조회) | GET | `/api/v1/programs` | 타입/지역별 프로그램 목록 |
| [학교 상세 조회](schools.md#학교-상세-조회) | GET | `/api/v1/schools/{schoolId}` | 학교 ID로 상세 정보 조회 |
