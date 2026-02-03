-- V4: RAG 기능을 위한 pgvector extension 및 임베딩 테이블 생성
-- GAM-3: Rule-based AI 매칭 알고리즘 구현

-- 1. pgvector extension 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- 2. School 임베딩 저장 테이블
-- Gemini text-embedding-004 모델의 출력 차원: 768
CREATE TABLE school_embeddings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    embedding_text TEXT NOT NULL,  -- 임베딩에 사용된 원본 텍스트 (디버깅/재생성용)
    embedding vector(768) NOT NULL,  -- 768차원 임베딩 벡터
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(school_id)
);

-- 3. 코사인 유사도 검색을 위한 IVFFlat 인덱스
-- lists = 100: 100개의 클러스터로 분할 (일반적으로 행 수의 제곱근)
-- vector_cosine_ops: 코사인 거리 연산자
CREATE INDEX school_embeddings_embedding_idx 
ON school_embeddings 
USING ivfflat (embedding vector_cosine_ops) 
WITH (lists = 100);

-- 4. 인덱스 설명
COMMENT ON INDEX school_embeddings_embedding_idx IS 'IVFFlat index for cosine similarity search on school embeddings';

-- 5. 테이블 설명
COMMENT ON TABLE school_embeddings IS 'School 데이터의 임베딩 벡터 저장 (Gemini text-embedding-004, 768차원)';
COMMENT ON COLUMN school_embeddings.embedding_text IS '임베딩 생성에 사용된 원본 텍스트 (학교명, 설명, 위치 등)';
COMMENT ON COLUMN school_embeddings.embedding IS 'Gemini API로 생성된 768차원 임베딩 벡터';
