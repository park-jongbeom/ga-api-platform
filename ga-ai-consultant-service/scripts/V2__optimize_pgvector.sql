-- =========================================================
-- PostgreSQL 17 pgvector 최적화 마이그레이션
-- =========================================================
-- 작성일: 2026-01-21
-- 목적: HNSW 인덱스 적용으로 벡터 검색 성능 향상
-- 참고: https://github.com/pgvector/pgvector#hnsw
-- =========================================================

-- HNSW 인덱스 생성 (ANN 검색 성능 대폭 향상)
-- m: 그래프 연결 수 (기본 16, 높을수록 정확도 증가, 메모리 증가)
-- ef_construction: 인덱스 구축 시 탐색 깊이 (기본 64, 높을수록 구축 시간 증가)
CREATE INDEX IF NOT EXISTS idx_documents_embedding_hnsw 
ON documents 
USING hnsw (embedding vector_cosine_ops)
WITH (m = 16, ef_construction = 64);

-- 기존 IVFFlat 인덱스 제거 (HNSW가 더 빠르고 정확함)
DROP INDEX IF EXISTS idx_documents_embedding_ivfflat;

-- Full-Text Search 인덱스 생성 (하이브리드 검색용)
-- GIN 인덱스는 텍스트 검색 성능을 크게 향상시킵니다
CREATE INDEX IF NOT EXISTS idx_documents_content_fts
ON documents
USING gin(to_tsvector('simple', COALESCE(content, '') || ' ' || COALESCE(title, '')));

-- 복합 인덱스: tenant_id + document_type (메타데이터 필터링 최적화)
CREATE INDEX IF NOT EXISTS idx_documents_tenant_type
ON documents(tenant_id, document_type)
WHERE tenant_id IS NOT NULL;

-- 생성일자 인덱스 (시간 범위 검색용)
CREATE INDEX IF NOT EXISTS idx_documents_created_at
ON documents(created_at DESC);

-- 통계 업데이트 (쿼리 플래너 최적화)
ANALYZE documents;

-- =========================================================
-- 성능 검증 쿼리 (배포 후 실행 권장)
-- =========================================================

-- 벡터 검색 성능 테스트
-- EXPLAIN ANALYZE
-- SELECT * FROM documents 
-- WHERE tenant_id = 'test-tenant'
-- ORDER BY embedding <=> '[0.1, 0.2, ...]'::vector
-- LIMIT 10;

-- 하이브리드 검색 성능 테스트
-- EXPLAIN ANALYZE
-- WITH vector_search AS (
--     SELECT id, (embedding <=> '[0.1, 0.2, ...]'::vector) AS distance
--     FROM documents 
--     WHERE tenant_id = 'test-tenant'
-- ),
-- text_search AS (
--     SELECT id, ts_rank(to_tsvector('simple', content), plainto_tsquery('simple', 'test')) AS rank
--     FROM documents
--     WHERE tenant_id = 'test-tenant'
-- )
-- SELECT vs.id, (1.0 - vs.distance) * 0.7 + COALESCE(ts.rank, 0) * 0.3 AS score
-- FROM vector_search vs
-- LEFT JOIN text_search ts ON vs.id = ts.id
-- ORDER BY score DESC
-- LIMIT 10;

-- =========================================================
-- 향후 최적화 옵션 (대규모 운영 시 고려)
-- =========================================================

-- 테넌트별 파티셔닝 (데이터가 수백만 건 이상일 때)
-- CREATE TABLE documents_partitioned (
--     LIKE documents INCLUDING ALL
-- ) PARTITION BY LIST (tenant_id);
-- 
-- CREATE TABLE documents_tenant_001 PARTITION OF documents_partitioned
-- FOR VALUES IN ('tenant-001');
-- 
-- CREATE TABLE documents_tenant_002 PARTITION OF documents_partitioned
-- FOR VALUES IN ('tenant-002');

-- 병렬 쿼리 설정 (대용량 검색 시)
-- ALTER TABLE documents SET (parallel_workers = 4);

-- 벡터 압축 (메모리 절약, 약간의 정확도 하락)
-- 현재는 1536차원 유지, 향후 A/B 테스트 후 결정
-- ALTER TABLE documents ALTER COLUMN embedding TYPE vector(512);
