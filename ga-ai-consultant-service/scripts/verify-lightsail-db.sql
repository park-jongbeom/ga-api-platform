-- ========================================
-- Lightsail PostgreSQL 환경 확인 스크립트
-- ========================================
-- 실행 방법:
-- psql -h your-lightsail-endpoint.rds.amazonaws.com -U your_master_username -d your_database_name -f verify-lightsail-db.sql

\echo '========================================='
\echo '1. PostgreSQL 버전 확인'
\echo '========================================='
SELECT version();

\echo ''
\echo '========================================='
\echo '2. 현재 데이터베이스 및 사용자'
\echo '========================================='
SELECT current_database() as database, current_user as user;

\echo ''
\echo '========================================='
\echo '3. 사용자 역할 및 권한'
\echo '========================================='
SELECT rolname 
FROM pg_roles 
WHERE pg_has_role(current_user, oid, 'member')
ORDER BY rolname;

\echo ''
\echo '========================================='
\echo '4. CREATE EXTENSION 권한 확인'
\echo '========================================='
SELECT has_database_privilege(current_database(), 'CREATE') as can_create_extension;

\echo ''
\echo '========================================='
\echo '5. pgvector 확장 설치 여부 확인'
\echo '========================================='
SELECT extname, extversion, extrelocatable 
FROM pg_extension 
WHERE extname = 'vector';

\echo ''
\echo '========================================='
\echo '6. pgvector 확장 사용 가능 여부'
\echo '========================================='
SELECT name, default_version, comment 
FROM pg_available_extensions 
WHERE name = 'vector';

\echo ''
\echo '========================================='
\echo '7. 현재 데이터베이스 목록'
\echo '========================================='
SELECT datname, pg_size_pretty(pg_database_size(datname)) as size
FROM pg_database 
WHERE datistemplate = false
ORDER BY datname;

\echo ''
\echo '========================================='
\echo '8. 현재 스키마 목록'
\echo '========================================='
SELECT schema_name 
FROM information_schema.schemata
WHERE schema_name NOT IN ('pg_catalog', 'information_schema', 'pg_toast')
ORDER BY schema_name;

\echo ''
\echo '========================================='
\echo '9. 현재 스키마의 테이블 목록'
\echo '========================================='
SELECT schemaname, tablename, pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) as size
FROM pg_tables 
WHERE schemaname NOT IN ('pg_catalog', 'information_schema')
ORDER BY schemaname, tablename;

\echo ''
\echo '========================================='
\echo '10. 연결 제한 확인'
\echo '========================================='
SELECT 
    setting::int as max_connections,
    (SELECT count(*) FROM pg_stat_activity) as current_connections,
    setting::int - (SELECT count(*) FROM pg_stat_activity) as available_connections
FROM pg_settings 
WHERE name = 'max_connections';

\echo ''
\echo '========================================='
\echo '검증 완료!'
\echo '========================================='
\echo ''
\echo '다음 단계:'
\echo '1. pgvector가 설치되어 있으면: 6번 결과에 표시됨'
\echo '2. pgvector가 설치 가능하면: 5번 결과에 표시됨'
\echo '3. CREATE EXTENSION 권한이 있으면: 4번이 "t" 또는 "true"'
\echo '4. rds_superuser 역할이 있으면: 3번 결과에 표시됨'
\echo ''
