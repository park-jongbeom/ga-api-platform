-- AI 상담 서비스: public 스키마에 테이블 보장/이관
--
-- 배경:
-- - 과거 마이그레이션에서 ai_consultant 스키마(search_path)로 테이블이 생성되었거나,
-- - Flyway 히스토리는 존재하지만(public) 실제 테이블이 public에 없어
--   Hibernate ddl-auto=validate 단계에서 `missing table [conversations]`로 기동이 실패할 수 있음
--
-- 목표:
-- - public 스키마에 conversations/messages/documents 테이블이 존재하도록 보장
-- - ai_consultant 스키마에 존재하는 경우 public으로 이동

-- UUID 기본키(gen_random_uuid)용
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- RAG 임베딩(vector)용 (환경에 따라 설치가 필요할 수 있음)
CREATE EXTENSION IF NOT EXISTS vector;

-- ai_consultant 스키마에 테이블이 있으면 public으로 이관
DO $$
BEGIN
  IF EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'ai_consultant') THEN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'ai_consultant' AND table_name = 'conversations') THEN
      EXECUTE 'ALTER TABLE ai_consultant.conversations SET SCHEMA public';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'ai_consultant' AND table_name = 'messages') THEN
      EXECUTE 'ALTER TABLE ai_consultant.messages SET SCHEMA public';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'ai_consultant' AND table_name = 'documents') THEN
      EXECUTE 'ALTER TABLE ai_consultant.documents SET SCHEMA public';
    END IF;
  END IF;
END $$;

-- public 스키마에 테이블이 없으면 생성(이미 존재하면 스킵)
CREATE TABLE IF NOT EXISTS public.conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(50) NOT NULL,
    title VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conversations_user_tenant ON public.conversations(user_id, tenant_id);
CREATE INDEX IF NOT EXISTS idx_conversations_created_at ON public.conversations(created_at);
CREATE INDEX IF NOT EXISTS idx_conversations_tenant ON public.conversations(tenant_id);
CREATE INDEX IF NOT EXISTS idx_conversations_status ON public.conversations(status);

CREATE TABLE IF NOT EXISTS public.messages (
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
        REFERENCES public.conversations(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_messages_conversation ON public.messages(conversation_id);
CREATE INDEX IF NOT EXISTS idx_messages_created_at ON public.messages(created_at);
CREATE INDEX IF NOT EXISTS idx_messages_role ON public.messages(role);

CREATE TABLE IF NOT EXISTS public.documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    metadata JSONB,
    tenant_id VARCHAR(50),
    source_url VARCHAR(500),
    document_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_documents_tenant ON public.documents(tenant_id);
CREATE INDEX IF NOT EXISTS idx_documents_created_at ON public.documents(created_at);
CREATE INDEX IF NOT EXISTS idx_documents_type ON public.documents(document_type);

-- HNSW 인덱스 (이미 존재하면 스킵)
CREATE INDEX IF NOT EXISTS idx_documents_embedding ON public.documents
    USING hnsw (embedding vector_cosine_ops)
    WITH (m = 16, ef_construction = 64);

COMMENT ON TABLE public.conversations IS 'AI 상담 세션';
COMMENT ON TABLE public.messages IS '상담 메시지 (마스킹 전/후 저장)';
COMMENT ON TABLE public.documents IS 'RAG 지식 베이스 문서';

COMMENT ON COLUMN public.messages.original_content IS '마스킹 전 원본 콘텐츠 (암호화 권장)';
COMMENT ON COLUMN public.messages.masked_content IS '마스킹 후 콘텐츠 (LLM 전송용)';
COMMENT ON COLUMN public.messages.masked_tokens IS '마스킹된 토큰 매핑 정보 (JSON)';
COMMENT ON COLUMN public.documents.embedding IS 'OpenAI Embedding 벡터 (1536차원)';

