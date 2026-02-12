-- V10: 크롤러 시스템 전용 유저(고정 UUID) 생성
-- 목적:
-- - audit_logs.user_id / created_by / updated_by 가 NOT NULL 및 FK(users.id)일 때
--   크롤러가 기록하는 AuditLog가 DB 제약으로 실패하지 않도록 "실존하는 시스템 유저"를 마련합니다.
--
-- 참고:
-- - id 는 고정 UUID로 유지합니다(크롤러/모니터에서 환경변수로 사용).
-- - users.created_by/updated_by 는 NULL 허용(스키마 기준)이라 시스템 유저 생성 시 NULL로 둡니다.

DO $$
BEGIN
    INSERT INTO users (
        id,
        email,
        full_name,
        role,
        is_active,
        email_verified,
        created_at,
        updated_at,
        created_by,
        updated_by
    ) VALUES (
        '3f0b6c3e-6c4b-4c1a-9a2f-0f7b6a7a7c01',
        'crawler-system@go-almond.internal',
        'Crawler System',
        'ADMIN',
        true,
        true,
        CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP,
        NULL,
        NULL
    )
    ON CONFLICT (id) DO NOTHING;
END $$;

