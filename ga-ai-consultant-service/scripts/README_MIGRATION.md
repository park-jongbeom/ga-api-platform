# 데이터베이스 마이그레이션 가이드

## 개요

이 디렉토리는 PostgreSQL 17 pgvector 최적화를 위한 마이그레이션 스크립트를 포함합니다.

## 마이그레이션 파일

### V2__optimize_pgvector.sql

**목적**: HNSW 인덱스 적용 및 하이브리드 검색 최적화

**주요 변경 사항**:
1. HNSW 인덱스 생성 (벡터 검색 성능 50% 향상 목표)
2. Full-Text Search GIN 인덱스 추가 (하이브리드 검색용)
3. 복합 인덱스 생성 (tenant_id + document_type)
4. 기존 IVFFlat 인덱스 제거

**예상 효과**:
- 벡터 검색 지연시간: ~200ms → ~100ms
- 하이브리드 검색 정확도: 30% 향상
- 메타데이터 필터링 속도: 2배 향상

## 실행 방법

### 1. 로컬 환경 (Docker)

```bash
# PostgreSQL 17 컨테이너 실행
docker run -d \
  --name goalmond-postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=goalmond \
  -p 5432:5432 \
  pgvector/pgvector:pg17

# pgvector 확장 활성화
docker exec -it goalmond-postgres psql -U postgres -d goalmond -c "CREATE EXTENSION IF NOT EXISTS vector;"

# 마이그레이션 실행
docker exec -i goalmond-postgres psql -U postgres -d goalmond < V2__optimize_pgvector.sql

# 결과 확인
docker exec -it goalmond-postgres psql -U postgres -d goalmond -c "\d documents"
docker exec -it goalmond-postgres psql -U postgres -d goalmond -c "\di"
```

### 2. AWS Lightsail PostgreSQL

```bash
# 환경변수 설정 (Lightsail 연결 정보)
export DB_HOST=your-instance.us-east-1.rds.amazonaws.com
export DB_NAME=goalmond
export DB_USER=admin
export DB_PASSWORD=your-password

# 마이그레이션 실행
psql -h $DB_HOST -U $DB_USER -d $DB_NAME -f V2__optimize_pgvector.sql

# 또는 pgAdmin, DBeaver 등 GUI 도구 사용
```

### 3. Flyway/Liquibase 사용 시

```bash
# Flyway 마이그레이션
./gradlew :ga-ai-consultant-service:flywayMigrate

# Liquibase 마이그레이션
./gradlew :ga-ai-consultant-service:update
```

## 성능 검증

### 1. 인덱스 생성 확인

```sql
-- 생성된 인덱스 목록
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'documents';

-- 예상 결과:
-- idx_documents_embedding_hnsw (HNSW 벡터 인덱스)
-- idx_documents_content_fts (Full-Text Search 인덱스)
-- idx_documents_tenant_type (복합 인덱스)
-- idx_documents_created_at (시간 인덱스)
```

### 2. 벡터 검색 성능 테스트

```sql
-- 실행 계획 확인 (HNSW 인덱스 사용 여부)
EXPLAIN ANALYZE
SELECT * FROM documents 
WHERE tenant_id = 'test-tenant'
ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector
LIMIT 10;

-- 기대 결과:
-- Index Scan using idx_documents_embedding_hnsw
-- Execution Time: < 100ms
```

### 3. 하이브리드 검색 성능 테스트

```sql
EXPLAIN ANALYZE
WITH vector_search AS (
    SELECT id, (embedding <=> '[0.1, 0.2, ...]'::vector) AS distance
    FROM documents 
    WHERE tenant_id = 'test-tenant'
),
text_search AS (
    SELECT id, ts_rank(to_tsvector('simple', content), plainto_tsquery('simple', 'test')) AS rank
    FROM documents
    WHERE tenant_id = 'test-tenant'
)
SELECT vs.id, (1.0 - vs.distance) * 0.7 + COALESCE(ts.rank, 0) * 0.3 AS score
FROM vector_search vs
LEFT JOIN text_search ts ON vs.id = ts.id
ORDER BY score DESC
LIMIT 10;

-- 기대 결과:
-- Bitmap Index Scan (GIN 인덱스 사용)
-- HNSW Index Scan (벡터 인덱스 사용)
-- Execution Time: < 150ms
```

## 롤백 방법

마이그레이션을 되돌려야 하는 경우:

```sql
-- HNSW 인덱스 제거
DROP INDEX IF EXISTS idx_documents_embedding_hnsw;

-- Full-Text Search 인덱스 제거
DROP INDEX IF EXISTS idx_documents_content_fts;

-- 복합 인덱스 제거
DROP INDEX IF EXISTS idx_documents_tenant_type;
DROP INDEX IF EXISTS idx_documents_created_at;

-- 기존 IVFFlat 인덱스 재생성 (선택사항)
CREATE INDEX idx_documents_embedding_ivfflat 
ON documents 
USING ivfflat (embedding vector_cosine_ops)
WITH (lists = 100);
```

## 주의사항

### 1. 인덱스 구축 시간

- 문서 10,000개 기준: 약 2-5분
- 문서 100,000개 기준: 약 20-30분
- **권장**: 트래픽이 적은 시간대에 실행

### 2. 디스크 공간

- HNSW 인덱스는 벡터 데이터의 약 1.5배 공간 필요
- 예: 10,000개 문서 (1536차원) ≈ 60MB 벡터 + 90MB 인덱스 = 150MB

### 3. 메모리 요구사항

- HNSW 인덱스는 메모리에 로드되어 사용
- 권장 메모리: 벡터 데이터 크기 × 2 이상
- AWS Lightsail: 최소 2GB RAM 권장

## 벤치마크 목표

| 항목 | 현재 (IVFFlat) | 목표 (HNSW) | 개선율 |
|------|---------------|------------|--------|
| 벡터 검색 지연 | ~200ms | <100ms | 50%+ |
| 하이브리드 검색 지연 | ~250ms | <150ms | 40%+ |
| 검색 정확도 (Recall@10) | 85% | 95%+ | 10%+ |
| 인덱스 구축 시간 | 1분 | 3분 | -200% |

## 문제 해결

### 인덱스 생성 실패

```bash
# 오류: pgvector 확장이 없음
ERROR:  type "vector" does not exist

# 해결: pgvector 확장 설치
CREATE EXTENSION vector;
```

### 메모리 부족

```bash
# 오류: out of memory
ERROR:  out of shared memory

# 해결: shared_buffers 증가 (postgresql.conf)
shared_buffers = 2GB
work_mem = 256MB
```

### 인덱스 스캔 안 됨

```sql
-- HNSW 인덱스가 사용되지 않는 경우
-- 원인: 통계 정보 부족

-- 해결: 통계 업데이트
ANALYZE documents;

-- 또는 쿼리 플래너 힌트
SET enable_seqscan = off;
```

## 참고 자료

- pgvector HNSW 문서: https://github.com/pgvector/pgvector#hnsw
- PostgreSQL 17 Release Notes: https://www.postgresql.org/docs/17/release-17.html
- Full-Text Search 가이드: https://www.postgresql.org/docs/17/textsearch.html
- 성능 튜닝 가이드: https://wiki.postgresql.org/wiki/Performance_Optimization
