-- V7: college-crawler와 동기화 - schools 테이블 유학생 관련 컬럼 추가
-- college-crawler 20260209_0947_add_school_international_columns 마이그레이션과 동일

-- 유학생 담당자 정보
ALTER TABLE schools ADD COLUMN IF NOT EXISTS international_email VARCHAR(255);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS international_phone VARCHAR(50);

-- 취업률
ALTER TABLE schools ADD COLUMN IF NOT EXISTS employment_rate NUMERIC(5, 2);

-- JSONB 컬럼들
ALTER TABLE schools ADD COLUMN IF NOT EXISTS facilities JSONB;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS staff_info JSONB;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS esl_program JSONB;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS international_support JSONB;

-- 인덱스
CREATE INDEX IF NOT EXISTS idx_schools_international_email
ON schools(international_email);
