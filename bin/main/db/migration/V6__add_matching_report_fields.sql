-- V6: 매칭 리포트 화면용 School 확장 필드 (backend-data-requirements.md)
-- schools 테이블에 글로벌 랭킹, 초봉, 동문 네트워크, 특징 배지 컬럼 추가

ALTER TABLE schools ADD COLUMN IF NOT EXISTS global_ranking VARCHAR(50);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS ranking_field VARCHAR(255);
ALTER TABLE schools ADD COLUMN IF NOT EXISTS average_salary INTEGER;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS alumni_network_count INTEGER;
ALTER TABLE schools ADD COLUMN IF NOT EXISTS feature_badges TEXT;

CREATE INDEX IF NOT EXISTS idx_schools_global_ranking ON schools(global_ranking);
CREATE INDEX IF NOT EXISTS idx_schools_average_salary ON schools(average_salary);
