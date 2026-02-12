-- V11: GraphRAG Knowledge Graph 테이블 생성
-- 목적:
-- - entities 테이블: 모든 Entity(School, Program, Company, Job, Skill, Location) 통합 관리
-- - knowledge_triples 테이블: 지식 그래프 Triple(Head-Relation-Tail) 관계 저장
-- - 기존 schools, programs 테이블과 외래키 연결
--
-- 참고:
-- - PostgreSQL 기반 Lightweight GraphRAG (Neo4j 불필요)
-- - Recursive CTE 기반 경로 탐색을 위한 인덱스 최적화
-- - Phase 1 MVP: 10개 학교, 약 500개 Triples 목표

-- 1. entities 테이블 생성
CREATE TABLE entities (
    uuid UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Entity 기본 정보
    entity_type VARCHAR(50) NOT NULL,
    entity_name VARCHAR(255) NOT NULL,
    
    -- 정규화 및 동의어 관리
    aliases JSONB DEFAULT '[]'::jsonb,
    canonical_name VARCHAR(255),
    
    -- 메타데이터
    metadata JSONB DEFAULT '{}'::jsonb,
    description TEXT,
    
    -- 외래키 연결 (기존 테이블과의 매핑)
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    program_id UUID REFERENCES programs(id) ON DELETE CASCADE,
    
    -- 품질 관리
    confidence_score DECIMAL(5, 2) DEFAULT 1.0,
    source_urls TEXT[],
    
    -- 감사 정보
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    
    -- 제약조건
    CONSTRAINT unique_entity UNIQUE (entity_type, canonical_name),
    CONSTRAINT valid_entity_type CHECK (entity_type IN ('School', 'Program', 'Company', 'Job', 'Skill', 'Location')),
    CONSTRAINT valid_reference CHECK (
        (entity_type = 'School' AND school_id IS NOT NULL) OR
        (entity_type = 'Program' AND program_id IS NOT NULL) OR
        (entity_type IN ('Company', 'Job', 'Skill', 'Location'))
    )
);

-- entities 테이블 인덱스
CREATE INDEX idx_entities_type_name ON entities(entity_type, canonical_name);
CREATE INDEX idx_entities_school_id ON entities(school_id) WHERE school_id IS NOT NULL;
CREATE INDEX idx_entities_program_id ON entities(program_id) WHERE program_id IS NOT NULL;
CREATE INDEX idx_entities_aliases ON entities USING GIN(aliases);
CREATE INDEX idx_entities_created_at ON entities(created_at DESC);

-- 2. knowledge_triples 테이블 생성
CREATE TABLE knowledge_triples (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    
    -- Triple 구조 (Head-Relation-Tail)
    head_entity_uuid UUID NOT NULL REFERENCES entities(uuid) ON DELETE CASCADE,
    head_entity_type VARCHAR(50) NOT NULL,
    head_entity_name VARCHAR(255) NOT NULL,
    
    relation_type VARCHAR(50) NOT NULL,
    
    tail_entity_uuid UUID NOT NULL REFERENCES entities(uuid) ON DELETE CASCADE,
    tail_entity_type VARCHAR(50) NOT NULL,
    tail_entity_name VARCHAR(255) NOT NULL,
    
    -- 관계 메타데이터
    weight DECIMAL(5, 2) DEFAULT 1.0,
    confidence_score DECIMAL(5, 2) DEFAULT 1.0,
    
    -- 관계별 추가 속성 (JSONB로 유연하게 확장)
    properties JSONB DEFAULT '{}'::jsonb,
    
    -- 출처 및 검증
    source_url TEXT,
    source_type VARCHAR(50),
    extraction_method VARCHAR(50),
    
    -- 감사 정보
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    created_by UUID REFERENCES users(id),
    verified_by UUID REFERENCES users(id),
    verified_at TIMESTAMPTZ,
    
    -- 제약조건
    CONSTRAINT valid_relation_type CHECK (
        relation_type IN ('LOCATED_IN', 'OFFERS', 'DEVELOPS', 'LEADS_TO', 
                         'HIRES_FROM', 'REQUIRES', 'PARTNERS_WITH')
    ),
    CONSTRAINT valid_confidence CHECK (confidence_score >= 0.0 AND confidence_score <= 1.0),
    CONSTRAINT valid_weight CHECK (weight >= 0.0 AND weight <= 10.0),
    CONSTRAINT no_self_relation CHECK (head_entity_uuid != tail_entity_uuid)
);

-- knowledge_triples 테이블 인덱스 (Recursive CTE 성능 최적화)
CREATE INDEX idx_triples_head_relation ON knowledge_triples(head_entity_uuid, relation_type);
CREATE INDEX idx_triples_tail_relation ON knowledge_triples(tail_entity_uuid, relation_type);
CREATE INDEX idx_triples_relation_type ON knowledge_triples(relation_type);
CREATE INDEX idx_triples_head_tail ON knowledge_triples(head_entity_uuid, tail_entity_uuid);
CREATE INDEX idx_triples_confidence ON knowledge_triples(confidence_score) WHERE confidence_score >= 0.8;
CREATE INDEX idx_triples_weight ON knowledge_triples(weight DESC);
CREATE INDEX idx_triples_created_at ON knowledge_triples(created_at DESC);
CREATE INDEX idx_triples_head_relation_tail ON knowledge_triples(head_entity_uuid, relation_type, tail_entity_uuid);

-- 3. 기존 테이블과의 통합 (선택사항)
ALTER TABLE schools ADD COLUMN IF NOT EXISTS entity_uuid UUID REFERENCES entities(uuid);
CREATE INDEX IF NOT EXISTS idx_schools_entity_uuid ON schools(entity_uuid) WHERE entity_uuid IS NOT NULL;

ALTER TABLE programs ADD COLUMN IF NOT EXISTS entity_uuid UUID REFERENCES entities(uuid);
CREATE INDEX IF NOT EXISTS idx_programs_entity_uuid ON programs(entity_uuid) WHERE entity_uuid IS NOT NULL;
