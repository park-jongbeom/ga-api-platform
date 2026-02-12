-- V12: GraphRAG 체크 제약조건 수정
-- 문제 1: valid_entity_type - 'School' (Title Case) vs Kotlin Enum 저장값 'SCHOOL' (UPPERCASE) 불일치
-- 문제 2: valid_reference - SCHOOL/PROGRAM 타입에 school_id/program_id NOT NULL 강제 → Phase 1 MVP에서 FK 없는 엔티티 생성 차단

-- 1. entities 테이블 제약조건 수정
ALTER TABLE entities
    DROP CONSTRAINT IF EXISTS valid_entity_type,
    DROP CONSTRAINT IF EXISTS valid_reference;

ALTER TABLE entities
    ADD CONSTRAINT valid_entity_type CHECK (
        entity_type IN ('SCHOOL', 'PROGRAM', 'COMPANY', 'JOB', 'SKILL', 'LOCATION')
    );
