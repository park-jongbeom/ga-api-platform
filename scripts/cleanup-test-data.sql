-- 테스트로 들어간 데이터 삭제
-- 대상: MatchingEngineServiceTest ([TEST_MATCH] ...), VectorSearchServiceTest ([TEST_VECTOR] ...), HardFilterServiceTest (Test School for Filter) 등

BEGIN;

-- 1. school_embeddings 먼저 삭제 (외래 키)
DELETE FROM school_embeddings
WHERE school_id IN (
    SELECT id FROM schools
    WHERE name LIKE '%Test%'
       OR name LIKE '[TEST_%'
);

-- 2. programs 삭제
DELETE FROM programs
WHERE school_id IN (
    SELECT id FROM schools
    WHERE name LIKE '%Test%'
       OR name LIKE '[TEST_%'
);

-- 3. schools 삭제
DELETE FROM schools
WHERE name LIKE '%Test%'
   OR name LIKE '[TEST_%';

COMMIT;

-- 확인 (실행 후 예상: 0)
-- SELECT COUNT(*) FROM schools WHERE name LIKE '%Test%' OR name LIKE '[TEST_%';
