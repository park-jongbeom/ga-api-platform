# 계획 문서 인덱스

이 프로젝트의 모든 계획 문서는 **통합 관리**됩니다.

## 통합 문서 위치

**경로**: `/media/ubuntu/data120g/ai-consulting-plans/`

모든 AI 유학 상담 고도화 관련 계획, 벤치마크, 구현 가이드가 이 위치에서 중앙 집중 관리됩니다.

## 주요 문서 바로가기

### 마스터 계획
- [전체 고도화 계획](../../ai-consulting-plans/00_MASTER_PLAN/ai_유학_상담_고도화_마스터플랜.md)
- [AI Enhancement Plan 2026](../../ai-consulting-plans/00_MASTER_PLAN/AI_ENHANCEMENT_PLAN_2026.md)

### GraphRAG 구축
- [Step 0: 오픈소스 벤치마킹](../../ai-consulting-plans/01_GRAPHRAG/step0_오픈소스_벤치마킹.md)
- [Phase 1-3 상세 계획](../../ai-consulting-plans/01_GRAPHRAG/)

### 현재 RAG 시스템
- [RAG 아키텍처](../../ai-consulting-plans/02_RAG_CURRENT/RAG_ARCHITECTURE.md)
- [데이터 검증 계획](../../ai-consulting-plans/02_RAG_CURRENT/RAG_DATA_VERIFICATION_PLAN.md)
- [매칭 시스템 개선](../../ai-consulting-plans/02_RAG_CURRENT/AI_MATCHING_IMPROVEMENT_GUIDE.md)

### 구현 가이드
- [크롤러 고도화](../../ai-consulting-plans/03_IMPLEMENTATION/)
- [백엔드 GraphRAG 통합](../../ai-consulting-plans/03_IMPLEMENTATION/)
- [TDD 전략](../../ai-consulting-plans/03_IMPLEMENTATION/)

## 이 프로젝트 관련 문서

### 현재 위치 (ga-api-platform/docs/)
- `DATABASE_SCHEMA.md` - PostgreSQL 스키마 (schools, programs, knowledge_triples)
- `LOCAL_TESTING.md` - 로컬 테스트 가이드
- `AI_MATCHING_IMPROVEMENT_GUIDE.md` - 통합 폴더에 복사됨

### 백엔드 구현
- `src/main/kotlin/com/goalmond/api/service/matching/`
  - `MatchingEngineService.kt` - 매칭 엔진
  - `VectorSearchService.kt` - 벡터 검색
  - `GraphSearchService.kt` - 그래프 탐색 (구축 예정)

## 문서 업데이트 방법

1. **계획 문서**: `/media/ubuntu/data120g/ai-consulting-plans/`에서 직접 수정
2. **구현 문서**: 각 프로젝트 `docs/`에서 수정
3. **버전 관리**: Git으로 변경 이력 추적

---

**최종 업데이트**: 2026-02-12  
**다음 단계**: GraphSearchService 구현 (Recursive CTE 기반 경로 탐색)
