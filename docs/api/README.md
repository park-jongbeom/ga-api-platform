# API 문서 (Markdown)

API 문서는 **Markdown으로만** 제공됩니다. Swagger UI나 OpenAPI JSON은 사용하지 않습니다.

## Cursor에서 참조하기

프론트엔드 개발 시 Cursor에서 다음처럼 참조하면 됩니다.

- **폴더 참조**: `@docs/api` — 채팅/에이전트에서 API 문서 컨텍스트로 사용
- **메인 문서**: [API.md](API.md) — 공통 형식, API 목록, TypeScript 타입 예시

## 문서 구성

| 파일 | 설명 |
|------|------|
| [API.md](API.md) | 메인 문서 — 공통 응답 형식, 에러 예시, API 목록, 공통 TypeScript 타입 |
| [matching.md](matching.md) | 매칭 실행, 최신 매칭 결과 조회 |
| [programs.md](programs.md) | 프로그램 목록 조회 |
| [schools.md](schools.md) | 학교 상세 조회 |

각 상세 문서에는 요청/응답 예시, TypeScript 인터페이스, cURL/JavaScript Fetch 예시가 포함되어 있습니다.

## Base URL

- 로컬: `http://localhost:8080`
- 배포 환경: 배포된 API 서버 주소 (예: `https://go-almond.ddnsfree.com`)
