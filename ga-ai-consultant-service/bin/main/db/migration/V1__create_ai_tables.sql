-- AI 상담 서비스 초기 스키마 생성
-- pgvector 확장 활성화 (벡터 유사도 검색)
CREATE EXTENSION IF NOT EXISTS vector;

-- 상담 세션 테이블
CREATE TABLE conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_conversations_user_tenant ON conversations(user_id, tenant_id);
CREATE INDEX idx_conversations_created_at ON conversations(created_at);
CREATE INDEX idx_conversations_tenant ON conversations(tenant_id);
CREATE INDEX idx_conversations_status ON conversations(status);

-- 메시지 테이블
CREATE TABLE messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    role VARCHAR(20) NOT NULL,
    original_content TEXT NOT NULL,
    masked_content TEXT,
    llm_response TEXT,
    masked_tokens JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) 
        REFERENCES conversations(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_messages_created_at ON messages(created_at);
CREATE INDEX idx_messages_role ON messages(role);

-- RAG 문서 테이블
CREATE TABLE documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),  -- OpenAI text-embedding-ada-002 차원
    metadata JSONB,
    tenant_id VARCHAR(50),
    source_url VARCHAR(500),
    document_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 인덱스 생성
CREATE INDEX idx_documents_tenant ON documents(tenant_id);
CREATE INDEX idx_documents_created_at ON documents(created_at);
CREATE INDEX idx_documents_type ON documents(document_type);

-- 벡터 유사도 검색을 위한 HNSW 인덱스 (성능 최적화)
CREATE INDEX idx_documents_embedding ON documents 
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

-- 테이블 코멘트
COMMENT ON TABLE conversations IS 'AI 상담 세션';
COMMENT ON TABLE messages IS '상담 메시지 (마스킹 전/후 저장)';
COMMENT ON TABLE documents IS 'RAG 지식 베이스 문서';

-- 컬럼 코멘트
COMMENT ON COLUMN messages.original_content IS '마스킹 전 원본 콘텐츠 (암호화 권장)';
COMMENT ON COLUMN messages.masked_content IS '마스킹 후 콘텐츠 (LLM 전송용)';
COMMENT ON COLUMN messages.masked_tokens IS '마스킹된 토큰 매핑 정보 (JSON)';
COMMENT ON COLUMN documents.embedding IS 'OpenAI Embedding 벡터 (1536차원)';
