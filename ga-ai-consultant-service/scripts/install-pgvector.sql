-- ========================================
-- pgvector 확장 설치 스크립트
-- ========================================
-- 주의: 이 스크립트는 CREATE EXTENSION 권한이 있는 사용자만 실행 가능합니다.
-- 실행 방법:
-- psql -h your-lightsail-endpoint.rds.amazonaws.com -U your_master_username -d your_database_name -f install-pgvector.sql

\echo '========================================='
\echo 'pgvector 확장 설치 시작'
\echo '========================================='

-- pgvector 확장 설치
CREATE EXTENSION IF NOT EXISTS vector;

\echo ''
\echo '설치 완료! 확인 중...'
\echo ''

-- 설치 확인
SELECT 
    extname as extension_name, 
    extversion as version,
    extrelocatable as relocatable
FROM pg_extension 
WHERE extname = 'vector';

\echo ''
\echo '========================================='
\echo 'pgvector 기능 테스트'
\echo '========================================='

-- 벡터 타입 테스트
DO $$
DECLARE
    test_vector vector(3);
BEGIN
    test_vector := '[1,2,3]';
    RAISE NOTICE 'Vector type test: %', test_vector;
    RAISE NOTICE 'pgvector가 정상적으로 작동합니다!';
END $$;

\echo ''
\echo '========================================='
\echo '설치 완료!'
\echo '========================================='
