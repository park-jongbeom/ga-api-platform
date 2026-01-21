-- ========================================
-- AI Consultant 전용 스키마 생성 스크립트
-- ========================================
-- 옵션 A: 새 스키마를 생성하여 데이터 격리
-- 실행 방법:
-- psql -h your-lightsail-endpoint.rds.amazonaws.com -U your_master_username -d your_database_name -f create-ai-schema.sql

\echo '========================================='
\echo 'AI Consultant 스키마 생성'
\echo '========================================='

-- 스키마 생성
CREATE SCHEMA IF NOT EXISTS ai_consultant;

\echo 'ai_consultant 스키마 생성 완료'
\echo ''

-- pgvector 확장이 설치되어 있는지 확인
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_extension WHERE extname = 'vector') THEN
        RAISE NOTICE 'pgvector 확장 설치 중...';
        CREATE EXTENSION IF NOT EXISTS vector;
        RAISE NOTICE 'pgvector 확장 설치 완료';
    ELSE
        RAISE NOTICE 'pgvector 확장이 이미 설치되어 있습니다';
    END IF;
END $$;

\echo ''
\echo '========================================='
\echo '스키마 권한 설정'
\echo '========================================='

-- 현재 사용자에게 스키마 사용 권한 부여
GRANT USAGE ON SCHEMA ai_consultant TO CURRENT_USER;
GRANT CREATE ON SCHEMA ai_consultant TO CURRENT_USER;

\echo '권한 설정 완료'
\echo ''

-- 스키마 확인
\echo '========================================='
\echo '생성된 스키마 확인'
\echo '========================================='
SELECT schema_name 
FROM information_schema.schemata
WHERE schema_name = 'ai_consultant';

\echo ''
\echo '========================================='
\echo '완료!'
\echo '========================================='
\echo ''
\echo '다음 단계:'
\echo '1. application.yml에서 flyway.schemas를 "ai_consultant"로 설정'
\echo '2. Flyway 마이그레이션 실행'
\echo ''
