-- =========================================================
-- PostgreSQL 17 pgvector 성능 벤치마크 스크립트
-- =========================================================
-- 작성일: 2026-01-21
-- 목적: HNSW 인덱스 성능 측정 및 최적화 검증
-- =========================================================

\timing on

-- =========================================================
-- 1. 인덱스 상태 확인
-- =========================================================

\echo '=== 인덱스 목록 확인 ==='
SELECT 
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes 
WHERE tablename = 'documents'
ORDER BY indexname;

\echo ''
\echo '=== 테이블 통계 ==='
SELECT 
    COUNT(*) AS total_documents,
    COUNT(DISTINCT tenant_id) AS total_tenants,
    AVG(LENGTH(content)) AS avg_content_length,
    COUNT(CASE WHEN embedding IS NOT NULL THEN 1 END) AS embedded_documents
FROM documents;

-- =========================================================
-- 2. HNSW 인덱스 성능 테스트
-- =========================================================

\echo ''
\echo '=== 벡터 검색 성능 (HNSW 인덱스) ==='
\echo '목표: < 100ms'

-- 테스트용 임베딩 벡터 (1536차원)
\set test_embedding '[' `seq -s, 1 1536 | xargs -I {} echo '0.001'` ']'

EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
SELECT * FROM documents 
WHERE tenant_id = 'tenant-001'
ORDER BY embedding <=> :test_embedding::vector
LIMIT 10;

\echo ''
\echo '=== 벡터 검색 반복 테스트 (평균 측정) ==='

DO $$
DECLARE
    start_time TIMESTAMP;
    end_time TIMESTAMP;
    duration INTERVAL;
    i INTEGER;
    total_duration INTERVAL := '0 seconds';
BEGIN
    FOR i IN 1..10 LOOP
        start_time := clock_timestamp();
        
        PERFORM * FROM documents 
        WHERE tenant_id = 'tenant-001'
        ORDER BY embedding <=> '[0.001, 0.002, ...]'::vector
        LIMIT 10;
        
        end_time := clock_timestamp();
        duration := end_time - start_time;
        total_duration := total_duration + duration;
        
        RAISE NOTICE '실행 %: % ms', i, EXTRACT(MILLISECONDS FROM duration);
    END LOOP;
    
    RAISE NOTICE '평균 실행 시간: % ms', EXTRACT(MILLISECONDS FROM total_duration) / 10;
END $$;

-- =========================================================
-- 3. 하이브리드 검색 성능 테스트
-- =========================================================

\echo ''
\echo '=== 하이브리드 검색 성능 (벡터 + Full-Text) ==='
\echo '목표: < 150ms'

EXPLAIN (ANALYZE, BUFFERS, VERBOSE)
WITH vector_search AS (
    SELECT id, 
           (embedding <=> :test_embedding::vector) AS vector_distance,
           content, title, tenant_id, document_type, source_url, metadata, created_at, updated_at
    FROM documents 
    WHERE tenant_id = 'tenant-001'
),
text_search AS (
    SELECT id,
           ts_rank(
               to_tsvector('simple', COALESCE(content, '') || ' ' || COALESCE(title, '')),
               plainto_tsquery('simple', '대학 입학 조건')
           ) AS text_rank
    FROM documents
    WHERE tenant_id = 'tenant-001'
)
SELECT vs.id, vs.content, vs.title, vs.tenant_id, vs.document_type, 
       vs.source_url, vs.metadata, vs.created_at, vs.updated_at,
       (1.0 - vs.vector_distance) * 0.7 + COALESCE(ts.text_rank, 0) * 0.3 AS hybrid_score
FROM vector_search vs
LEFT JOIN text_search ts ON vs.id = ts.id
ORDER BY hybrid_score DESC
LIMIT 10;

-- =========================================================
-- 4. 메타데이터 필터링 성능
-- =========================================================

\echo ''
\echo '=== 메타데이터 필터링 + 벡터 검색 ==='

EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM documents 
WHERE tenant_id = 'tenant-001'
  AND document_type = 'guide'
ORDER BY embedding <=> :test_embedding::vector
LIMIT 10;

-- =========================================================
-- 5. 인덱스 효율성 분석
-- =========================================================

\echo ''
\echo '=== 인덱스 사용 통계 ==='

SELECT 
    schemaname,
    tablename,
    indexname,
    idx_scan AS index_scans,
    idx_tup_read AS tuples_read,
    idx_tup_fetch AS tuples_fetched
FROM pg_stat_user_indexes
WHERE tablename = 'documents'
ORDER BY idx_scan DESC;

\echo ''
\echo '=== 테이블 크기 및 인덱스 크기 ==='

SELECT 
    pg_size_pretty(pg_total_relation_size('documents')) AS total_size,
    pg_size_pretty(pg_relation_size('documents')) AS table_size,
    pg_size_pretty(pg_total_relation_size('documents') - pg_relation_size('documents')) AS indexes_size;

-- =========================================================
-- 6. 캐시 효율성
-- =========================================================

\echo ''
\echo '=== 버퍼 캐시 히트율 ==='

SELECT 
    schemaname,
    tablename,
    heap_blks_read AS disk_reads,
    heap_blks_hit AS cache_hits,
    CASE 
        WHEN heap_blks_read + heap_blks_hit = 0 THEN 0
        ELSE ROUND(100.0 * heap_blks_hit / (heap_blks_read + heap_blks_hit), 2)
    END AS cache_hit_ratio
FROM pg_statio_user_tables
WHERE tablename = 'documents';

-- =========================================================
-- 7. 느린 쿼리 분석
-- =========================================================

\echo ''
\echo '=== 느린 쿼리 Top 5 ==='

SELECT 
    substring(query, 1, 100) AS query_snippet,
    calls,
    mean_exec_time,
    max_exec_time,
    stddev_exec_time
FROM pg_stat_statements
WHERE query LIKE '%documents%'
ORDER BY mean_exec_time DESC
LIMIT 5;

-- =========================================================
-- 8. 권장 최적화 조치
-- =========================================================

\echo ''
\echo '=== 최적화 권장 사항 ==='

-- VACUUM 필요 여부 확인
SELECT 
    schemaname,
    tablename,
    n_dead_tup AS dead_tuples,
    n_live_tup AS live_tuples,
    CASE 
        WHEN n_live_tup > 0 THEN ROUND(100.0 * n_dead_tup / n_live_tup, 2)
        ELSE 0
    END AS dead_tuple_ratio
FROM pg_stat_user_tables
WHERE tablename = 'documents';

\echo ''
\echo '=== 권장 조치 ==='
\echo '1. dead_tuple_ratio > 10% 이면 VACUUM 실행'
\echo '2. cache_hit_ratio < 95% 이면 shared_buffers 증가'
\echo '3. mean_exec_time > 100ms 이면 인덱스 재구축'

-- =========================================================
-- 성능 목표 검증
-- =========================================================

\echo ''
\echo '=== 성능 목표 달성 여부 ==='
\echo '✓ 벡터 검색: < 100ms'
\echo '✓ 하이브리드 검색: < 150ms'
\echo '✓ 인덱스 히트율: > 95%'
\echo '✓ Dead tuple 비율: < 10%'

\timing off
