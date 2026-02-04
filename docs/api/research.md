# 조사 API (Research)

범용 정보 조사 에이전트 API. 매칭 결과 보완용 심층 정보 제공.

## 개요

- **Base path**: `/api/v1/research`
- **인증**: 프로파일별 상이 (default: 불필요, local/lightsail: JWT 권장)
- **응답 형식**: [ApiResponse](API.md) (`success`, `data`, `code`, `message`, `timestamp`)

## 전체 조사 실행

4단계 10개 프롬프트를 모두 실행하여 상세 보고서 생성.

- **Method**: `POST`
- **Path**: `/api/v1/research/full`
- **Content-Type**: `application/json`

### 요청 Body

```json
{
  "category": "vocational",
  "field": "기계",
  "state": "California",
  "cities": "Los Angeles, San Francisco"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| category | string | O | 조사 카테고리 (예: `vocational`) |
| field | string | X | 분야 (예: 기계, IT, 간호). 기본값: "일반" |
| state | string | X | 주 (예: California). 기본값: "California" |
| cities | string | X | 도시 목록 (쉼표 구분). 기본값: "Los Angeles, San Francisco, San Diego" |
| schoolId | string | X | 매칭 결과에서 선택한 학교 ID (향후 활용) |
| promptIds | string[] | X | 특정 프롬프트만 실행 (향후 활용) |
| stage | string | X | 특정 단계만 실행 (향후 활용) |

### 성공 응답 (200)

```json
{
  "success": true,
  "data": {
    "title": "vocational 유학 정보 조사: 기계",
    "category": "vocational",
    "targetField": "기계",
    "targetState": "California",
    "createdAt": "2026-02-04T10:00:00Z",
    "results": [
      {
        "promptId": "P1_COLLEGE_TYPES",
        "stageName": "기본 구조 파악",
        "question": "미국의 Community College(CC)와...",
        "answer": "Community College는 2년제이며...",
        "confidence": 0.85,
        "timestamp": "2026-02-04T10:00:05Z"
      }
    ],
    "summary": "기계 분야 vocational 조사 완료. 총 10개 항목 중 10개 성공...",
    "recommendations": [
      "STEM OPT 가능 프로그램 우선 고려",
      "취업 지원 서비스가 강한 학교 선택",
      "CPT 활용하여 재학 중 경력 쌓기",
      "H-1B 또는 EB-3 경로 미리 계획"
    ]
  }
}
```

- **results**: 단계별 프롬프트 실행 결과 (P1 ~ P10)
- **summary**: 전체 조사 요약
- **recommendations**: 유학생 대상 추천 사항

## 특정 단계 조사

- **Method**: `POST`
- **Path**: `/api/v1/research/stage/{stage}`
- **Path 변수**: `stage` — `STAGE_1_OVERVIEW`, `STAGE_2_SELECTION`, `STAGE_3_FINANCIALS`, `STAGE_4_ROADMAP`

### 요청 Body

동일하게 `ResearchRequest` (category, field, state, cities 등).

### 응답

`data`에 해당 단계의 `ResearchResult[]` 반환.

## 단일 프롬프트 실행

- **Method**: `POST`
- **Path**: `/api/v1/research/prompt/{promptId}`

### 요청 Body (변수 맵)

```json
{
  "field": "IT",
  "state": "California",
  "cities": "LA, SF"
}
```

### 응답

`data`에 단일 `ResearchResult` 반환.

### Vocational 프롬프트 ID 목록

| ID | 단계 | 설명 |
|----|------|------|
| P1_COLLEGE_TYPES | 1 | CC vs Trade School 비교 |
| P2_TOP_PROGRAMS | 1 | 인기 프로그램 TOP 5 |
| P3_SCHOOL_LIST | 2 | 지역별 추천 학교 |
| P4_STEM_PROGRAMS | 2 | STEM OPT 프로그램 |
| P5_CAREER_SERVICES | 2 | 취업 지원 우수 학교 |
| P6_SCHOLARSHIPS | 3 | 장학금 정보 |
| P7_CPT_WORK | 3 | CPT 근무 가이드 |
| P8_H1B_PATH | 3 | H-1B 전환 경로 |
| P9_GREEN_CARD | 4 | EB-3 영주권 가이드 |
| P10_CITY_ANALYSIS | 4 | 도시별 생활 비교 |

## 프롬프트 목록 조회

- **Method**: `GET`
- **Path**: `/api/v1/research/prompts`
- **Query**: `category` (선택) — 지정 시 해당 카테고리만 반환

### 응답

`data`에 `PromptTemplate[]` (id, category, stage, description 등). `template` 본문은 보안상 생략 가능.

## cURL 예시

```bash
# 전체 조사
curl -X POST http://localhost:8080/api/v1/research/full \
  -H "Content-Type: application/json" \
  -d '{"category":"vocational","field":"기계","state":"California"}'

# 장학금 정보만 조회
curl -X POST http://localhost:8080/api/v1/research/prompt/P6_SCHOLARSHIPS \
  -H "Content-Type: application/json" \
  -d '{"state":"California"}'

# STAGE 2 (학교 선정)만 실행
curl -X POST http://localhost:8080/api/v1/research/stage/STAGE_2_SELECTION \
  -H "Content-Type: application/json" \
  -d '{"category":"vocational","field":"IT","state":"California"}'

# 프롬프트 목록
curl "http://localhost:8080/api/v1/research/prompts?category=vocational"
```

## 성능 및 비용

| 항목 | 예상 |
|------|------|
| 단일 프롬프트 | ~1–2초, ~$0.01 |
| 단계별 조사 (2–3개) | ~5초, ~$0.03 |
| 전체 조사 (10개) | ~15초, ~$0.10 |
| Rate limit | 프롬프트 간 1초 대기 |

## 사용 시나리오

1. **매칭 후 학교 상세 조사**: 사용자가 매칭 결과에서 학교를 선택한 뒤, `category`(학교 타입), `field`(전공), `state`로 `/full` 호출.
2. **특정 정보만 조회**: 장학금(P6), CPT(P7) 등 단일 프롬프트만 `/prompt/{id}`로 호출.
