# Lightsail PostgreSQL 연동 완료 보고서

**작업 일시**: 2026-01-20  
**대상 환경**: AWS Lightsail PostgreSQL 17

---

## ✅ 완료된 작업

### 1. 검증 스크립트 생성
- ✅ `scripts/verify-lightsail-db.sql` - 데이터베이스 환경 10가지 항목 확인
- ✅ `scripts/install-pgvector.sql` - pgvector 확장 설치 자동화
- ✅ `scripts/create-ai-schema.sql` - ai_consultant 스키마 생성 (선택사항)
- ✅ `scripts/setup-lightsail.ps1` - PowerShell 환경변수 설정 자동화

### 2. 마이그레이션 스크립트 다양화
- ✅ `V1__create_ai_tables.sql` - 기본 (public 스키마 + pgvector 설치)
- ✅ `V1__create_ai_tables_with_schema.sql` - ai_consultant 스키마 전용
- ✅ `V1__create_ai_tables_no_extension.sql.disabled` - pgvector 이미 설치된 경우

### 3. 환경 설정 파일
- ✅ `application-lightsail.yml` - Lightsail 최적화 설정
  - HikariCP 연결 풀 조정 (최대 5개)
  - Flyway 스키마 설정 지원
  - CloudWatch 모니터링 옵션
- ✅ `env.lightsail.template` - 환경변수 템플릿

### 4. 문서화
- ✅ `LIGHTSAIL_SETUP_GUIDE.md` - 상세 설정 가이드 (7단계)
- ✅ `DEPLOYMENT_CHECKLIST.md` - 단계별 체크리스트 (9단계)
- ✅ `README_LIGHTSAIL.md` - 종합 README
- ✅ `LIGHTSAIL_QUICKSTART.md` - 빠른 시작 가이드 (5단계, 30분)

---

## 📦 생성된 파일 목록

### Scripts (4개)
```
scripts/
├── verify-lightsail-db.sql       # DB 환경 검증 (10개 항목)
├── install-pgvector.sql           # pgvector 설치
├── create-ai-schema.sql           # 스키마 생성
└── setup-lightsail.ps1            # 환경변수 설정 (PowerShell)
```

### Migration Files (3개)
```
src/main/resources/db/migration/
├── V1__create_ai_tables.sql                    # 기본
├── V1__create_ai_tables_with_schema.sql        # 스키마 전용
└── V1__create_ai_tables_no_extension.sql.disabled  # pgvector 제외
```

### Configuration (2개)
```
src/main/resources/
├── application-lightsail.yml      # Lightsail 최적화 설정
└── (기존) application.yml          # 기본 설정
```

### Documentation (6개)
```
ga-ai-consultant-service/
├── env.lightsail.template                 # 환경변수 템플릿
├── LIGHTSAIL_SETUP_GUIDE.md               # 상세 가이드 (7단계)
├── DEPLOYMENT_CHECKLIST.md                # 체크리스트 (9단계)
├── README_LIGHTSAIL.md                    # 종합 README
├── LIGHTSAIL_QUICKSTART.md                # 빠른 시작 (5단계)
└── LIGHTSAIL_INTEGRATION_SUMMARY.md       # 이 파일
```

---

## 🎯 지원하는 배포 시나리오

### 시나리오 1: public 스키마 사용 (권장)
**특징**: 가장 간단, 빠른 구축  
**사용 파일**: 
- `V1__create_ai_tables.sql`
- `application-lightsail.yml` (기본 설정)

**단계**:
1. 환경변수 설정
2. Flyway 마이그레이션
3. 애플리케이션 실행

---

### 시나리오 2: ai_consultant 스키마 격리
**특징**: 데이터 격리, 네임스페이스 명확화  
**사용 파일**:
- `scripts/create-ai-schema.sql` (먼저 실행)
- `V1__create_ai_tables_with_schema.sql`
- `application-lightsail.yml` (FLYWAY_SCHEMA=ai_consultant)

**단계**:
1. 스키마 생성
2. 마이그레이션 파일 교체
3. 환경변수 설정 (FLYWAY_SCHEMA 추가)
4. Flyway 마이그레이션
5. 애플리케이션 실행

---

### 시나리오 3: pgvector 이미 설치됨
**특징**: DBA가 pgvector를 미리 설치한 경우  
**사용 파일**:
- `V1__create_ai_tables_no_extension.sql.disabled` → `.sql`로 변경
- `application-lightsail.yml`

**단계**:
1. 마이그레이션 파일 교체 (CREATE EXTENSION 제외)
2. 환경변수 설정
3. Flyway 마이그레이션
4. 애플리케이션 실행

---

## 🔧 Lightsail 환경 최적화

### 연결 풀 설정
```yaml
hikari:
  maximum-pool-size: 5  # Lightsail 플랜에 맞춤
  minimum-idle: 2
```

### SSL 연결 (권장)
```bash
DATABASE_URL=jdbc:postgresql://your-endpoint:5432/db?ssl=true&sslmode=require
```

### Flyway 설정
```yaml
flyway:
  baseline-on-migrate: true  # 기존 DB에 적용 시 유용
  schemas: ${FLYWAY_SCHEMA:public}
  default-schema: ${FLYWAY_SCHEMA:public}
```

---

## 📊 검증 체크리스트

### 배포 전 확인
- [ ] Lightsail PostgreSQL 17 연결 성공
- [ ] pgvector 확장 설치/확인 완료
- [ ] 보안 그룹 5432 포트 허용
- [ ] 환경변수 11개 설정 완료
- [ ] Flyway 마이그레이션 테스트 성공

### 배포 후 확인
- [ ] 테이블 3개 생성 (conversations, messages, documents)
- [ ] pgvector 타입 정상 (vector(1536))
- [ ] HNSW 인덱스 생성 (idx_documents_embedding)
- [ ] Health Check API 200 OK
- [ ] Swagger UI 접속 가능

---

## 🚀 빠른 시작 (요약)

```bash
# 1. 환경변수 설정
cp env.lightsail.template .env.lightsail
notepad .env.lightsail
.\scripts\setup-lightsail.ps1

# 2. DB 환경 확인
psql -h your-endpoint -U your_username -d your_database -f scripts\verify-lightsail-db.sql

# 3. pgvector 설치 (필요시)
psql -h your-endpoint -U your_username -d your_database -f scripts\install-pgvector.sql

# 4. 마이그레이션
cd ..
.\gradlew.bat :ga-ai-consultant-service:flywayMigrate

# 5. 실행
.\gradlew.bat :ga-ai-consultant-service:bootRun --args='--spring.profiles.active=lightsail'

# 6. 테스트
curl http://localhost:8083/api/ai/consultant/health
```

---

## 🔍 검증 쿼리

### 테이블 확인
```sql
\dt
-- 또는
SELECT tablename FROM pg_tables WHERE schemaname = 'public';
```

### pgvector 타입 확인
```sql
\d documents
-- embedding 컬럼이 vector(1536)인지 확인
```

### 인덱스 확인
```sql
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'documents';
-- idx_documents_embedding (HNSW) 확인
```

### 활성 연결 확인
```sql
SELECT datname, usename, application_name, client_addr, state
FROM pg_stat_activity
WHERE datname = 'your_database_name';
```

---

## ⚠️ 주의사항

### 보안
- JWT_SECRET은 256비트 이상 강력한 키 사용
- 프로덕션 환경에서는 모든 시크릿을 AWS Secrets Manager 사용 권장
- SSL 연결 활성화 필수

### 성능
- Lightsail 플랜에 맞게 연결 풀 크기 조정
- HNSW 인덱스는 벡터 검색 성능 최적화 (m=16, ef_construction=64)
- Query Timeout 30초 설정으로 긴 쿼리 방지

### 백업
- Lightsail 자동 백업 활성화
- Flyway 마이그레이션 전 스냅샷 생성 권장
- 주기적인 백업 테스트

### 모니터링
- Lightsail 콘솔에서 CPU, 메모리, 연결 수 모니터링
- CloudWatch 연동 고려 (CLOUDWATCH_ENABLED=true)
- 로그 파일 정기 확인 (logs/ai-consultant-service.log)

---

## 📚 참고 문서 순서

1. **처음 시작**: [LIGHTSAIL_QUICKSTART.md](LIGHTSAIL_QUICKSTART.md) (5단계, 30분)
2. **상세 가이드**: [LIGHTSAIL_SETUP_GUIDE.md](LIGHTSAIL_SETUP_GUIDE.md) (7단계, 전체)
3. **체크리스트**: [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md) (9단계, 단계별)
4. **종합 README**: [README_LIGHTSAIL.md](README_LIGHTSAIL.md) (전체 정보)
5. **보안 검증**: [SECURITY_VERIFICATION_REPORT.md](SECURITY_VERIFICATION_REPORT.md) (15개 항목)

---

## 🎉 결론

Lightsail PostgreSQL 17 환경에서 AI 상담 모듈을 배포하기 위한 모든 준비가 완료되었습니다.

### 주요 성과
- ✅ 3가지 배포 시나리오 지원
- ✅ 완전 자동화된 검증 및 설치 스크립트
- ✅ Lightsail 최적화 설정
- ✅ 단계별 체크리스트 및 가이드

### 다음 단계
1. 환경변수 설정 (env.lightsail.template → .env.lightsail)
2. 검증 스크립트 실행 (verify-lightsail-db.sql)
3. Flyway 마이그레이션
4. 애플리케이션 테스트
5. 프로덕션 배포

### 예상 소요 시간
- **빠른 시작**: 30분
- **상세 설정**: 1시간
- **전체 검증**: 1.5시간

---

**작성자**: AI Assistant  
**마지막 업데이트**: 2026-01-20  
**버전**: 1.0.0  
**상태**: ✅ 준비 완료
