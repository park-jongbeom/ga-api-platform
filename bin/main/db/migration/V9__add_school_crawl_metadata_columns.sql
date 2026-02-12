-- V9: schools 테이블에 크롤링 메타데이터(최신 상태) 컬럼 추가
-- 하이브리드 설계:
-- - schools: 최신 크롤링 상태/시각(모니터 통계 및 "최근 업데이트" 기준)
-- - audit_logs: 상세 히스토리(append-only)

ALTER TABLE schools
    ADD COLUMN IF NOT EXISTS last_crawled_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS last_crawl_status VARCHAR(20),
    ADD COLUMN IF NOT EXISTS last_crawl_message TEXT,
    ADD COLUMN IF NOT EXISTS last_crawl_data_updated_at TIMESTAMPTZ;

CREATE INDEX IF NOT EXISTS idx_schools_last_crawled_at ON schools(last_crawled_at);
CREATE INDEX IF NOT EXISTS idx_schools_last_crawl_status ON schools(last_crawl_status);

