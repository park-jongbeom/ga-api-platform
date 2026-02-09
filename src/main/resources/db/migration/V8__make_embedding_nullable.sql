-- V8: college-crawler와 역할 분담 - 크롤러는 텍스트만 저장, 임베딩은 ga-api-platform에서 생성
-- embedding 컬럼을 nullable로 변경 (크롤러가 임베딩 없이 저장 가능)

ALTER TABLE school_documents
ALTER COLUMN embedding DROP NOT NULL;

ALTER TABLE program_documents
ALTER COLUMN embedding DROP NOT NULL;
