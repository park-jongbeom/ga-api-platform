-- V5: RAG용 문서 임베딩 테이블 추가
-- GAM-3: Rule-based AI 매칭 알고리즘 구현 (RAG 완성)

-- 1. 학교 문서 테이블 (리뷰, 입학 가이드, 통계, 장단점)
CREATE TABLE school_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,  -- review, admission_guide, statistics, pros_cons
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768) NOT NULL,  -- Gemini text-embedding-004 (768차원)
    metadata JSONB,  -- 추가 정보 (평점, 출처, 연도 등)
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 2. 학교 문서 임베딩 인덱스 (코사인 유사도)
CREATE INDEX school_documents_embedding_idx 
ON school_documents 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 3. 학교 문서 복합 인덱스 (school_id + document_type 조회용)
CREATE INDEX idx_school_documents_school_type 
ON school_documents(school_id, document_type);

-- 4. 프로그램 문서 테이블 (커리큘럼, 진로 통계, 학생 후기)
CREATE TABLE program_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    program_id UUID NOT NULL REFERENCES programs(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL,  -- curriculum, career_outcome, student_review
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(768) NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 5. 프로그램 문서 임베딩 인덱스
CREATE INDEX program_documents_embedding_idx 
ON program_documents 
USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);

-- 6. 프로그램 문서 복합 인덱스
CREATE INDEX idx_program_documents_program_type 
ON program_documents(program_id, document_type);

-- 7. 테이블 설명
COMMENT ON TABLE school_documents IS 'RAG용 학교 관련 문서 (리뷰, 입학 가이드, 통계, 장단점)';
COMMENT ON COLUMN school_documents.document_type IS '문서 유형: review(리뷰), admission_guide(입학 가이드), statistics(통계), pros_cons(장단점)';
COMMENT ON COLUMN school_documents.embedding IS 'Gemini API로 생성된 768차원 임베딩 벡터';
COMMENT ON COLUMN school_documents.metadata IS '추가 정보: 평점, 출처, 작성 연도, 작성자 등';

COMMENT ON TABLE program_documents IS 'RAG용 프로그램 관련 문서 (커리큘럼, 진로 통계, 학생 후기)';
COMMENT ON COLUMN program_documents.document_type IS '문서 유형: curriculum(커리큘럼), career_outcome(진로 통계), student_review(학생 후기)';
COMMENT ON COLUMN program_documents.embedding IS 'Gemini API로 생성된 768차원 임베딩 벡터';
