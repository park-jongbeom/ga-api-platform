# API 문서 (Markdown)

API 문서는 **Markdown으로만** 제공됩니다. Swagger UI나 OpenAPI JSON은 사용하지 않습니다.

## 프론트엔드에서 사용하기

이 문서는 **백엔드에서 링크로 전달**하는 API 스펙입니다. 프론트엔드는 API 프로젝트(ga-api-platform)를 보유하지 않을 수 있으므로, **전달받은 문서 링크**(예: GitHub 이 페이지 링크)로 스펙을 확인하세요.

API 프로젝트를 함께 사용하는 경우에는 [API.md](API.md) 등 로컬 경로로 직접 참조할 수 있습니다.

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
