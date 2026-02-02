-- V2: 매칭 관련 테이블 (GAM-21) + user 프로필 확장 (GAM-22)
-- GAM-13 V1에서 users, academic_profiles, user_preferences 등 이미 생성됨

-- user_preferences에 MBTI, tags, bio 컬럼 추가 (GAM-22 프로필 기본 정보)
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS mbti VARCHAR(20);
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS tags TEXT;
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS bio TEXT;

-- academic_profiles에 영어 점수 컬럼 추가 (GAM-22 학력 정보)
ALTER TABLE academic_profiles ADD COLUMN IF NOT EXISTS school_location VARCHAR(255);
ALTER TABLE academic_profiles ADD COLUMN IF NOT EXISTS english_test_type VARCHAR(50);
ALTER TABLE academic_profiles ADD COLUMN IF NOT EXISTS english_score INTEGER;

-- user_preferences에 예산, 희망 전공 등 추가 (GAM-22 유학 목표)
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS target_program VARCHAR(255);
ALTER TABLE user_preferences ADD COLUMN IF NOT EXISTS budget_usd INTEGER;

-- ---------------------------------------------------------------------------
-- schools (학교 마스터)
-- ---------------------------------------------------------------------------
CREATE TABLE schools (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    state VARCHAR(100),
    city VARCHAR(100),
    tuition INTEGER,
    living_cost INTEGER,
    ranking INTEGER,
    description TEXT,
    acceptance_rate INTEGER,
    transfer_rate INTEGER,
    graduation_rate INTEGER,
    website VARCHAR(500),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_schools_type ON schools(type);
CREATE INDEX idx_schools_state ON schools(state);
CREATE INDEX idx_schools_created_at ON schools(created_at);

-- ---------------------------------------------------------------------------
-- programs (프로그램 마스터)
-- ---------------------------------------------------------------------------
CREATE TABLE programs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID NOT NULL REFERENCES schools(id),
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    degree VARCHAR(50),
    duration VARCHAR(100),
    tuition INTEGER,
    opt_available BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_programs_school_id ON programs(school_id);
CREATE INDEX idx_programs_type ON programs(type);
CREATE INDEX idx_programs_created_at ON programs(created_at);

-- ---------------------------------------------------------------------------
-- matching_results (매칭 결과)
-- ---------------------------------------------------------------------------
CREATE TABLE matching_results (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    matching_id VARCHAR(255) NOT NULL,
    total_matches INTEGER NOT NULL,
    execution_time_ms INTEGER,
    result_json JSONB,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_matching_results_user_id ON matching_results(user_id);
CREATE INDEX idx_matching_results_created_at ON matching_results(created_at);

-- ---------------------------------------------------------------------------
-- applications (지원 현황)
-- ---------------------------------------------------------------------------
CREATE TABLE applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    school_id UUID NOT NULL REFERENCES schools(id),
    program_id UUID NOT NULL REFERENCES programs(id),
    status VARCHAR(50) NOT NULL DEFAULT '준비중',
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_applications_user_id ON applications(user_id);
CREATE INDEX idx_applications_school_id ON applications(school_id);
CREATE INDEX idx_applications_program_id ON applications(program_id);
CREATE INDEX idx_applications_status ON applications(status);
CREATE INDEX idx_applications_created_at ON applications(created_at);

-- ---------------------------------------------------------------------------
-- bookmarks (보관한 학교 - GAM-55)
-- ---------------------------------------------------------------------------
CREATE TABLE bookmarks (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id),
    school_id UUID NOT NULL REFERENCES schools(id),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(user_id, school_id)
);
CREATE INDEX idx_bookmarks_user_id ON bookmarks(user_id);
CREATE INDEX idx_bookmarks_school_id ON bookmarks(school_id);
