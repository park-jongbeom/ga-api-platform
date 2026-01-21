# AI 상담 모듈 - Lightsail PostgreSQL 연동

AWS Lightsail PostgreSQL 17 환경에서 AI 상담 모듈을 빠르게 배포하기 위한 가이드입니다.

## 🚀 빠른 시작 (Quick Start)

### 1. 환경변수 설정

```bash
# 1. 템플릿 파일 복사
cp env.lightsail.template .env.lightsail

# 2. 편집기로 열어서 실제 값으로 변경
notepad .env.lightsail  # Windows
# vi .env.lightsail     # Linux/Mac

# 3. 환경변수 로드 (PowerShell)
.\scripts\setup-lightsail.ps1
```

### 2. 데이터베이스 환경 확인

```bash
# Lightsail PostgreSQL 연결 테스트
psql -h your-lightsail-endpoint -U your_username -d your_database -f scripts/verify-lightsail-db.sql
```

### 3. pgvector 설치 (필요시)

```bash
# pgvector가 설치되지 않은 경우에만
psql -h your-lightsail-endpoint -U your_username -d your_database -f scripts/install-pgvector.sql
```

### 4. 마이그레이션 실행

```bash
# Flyway 마이그레이션
cd c:\Users\qk54r\ga-api-platform
.\gradlew.bat :ga-ai-consultant-service:flywayMigrate
```

### 5. 애플리케이션 실행

```bash
# Lightsail 프로파일로 실행
.\gradlew.bat :ga-ai-consultant-service:bootRun --args='--spring.profiles.active=lightsail'
```

### 6. 확인

```bash
# Health Check
curl http://localhost:8083/api/ai/consultant/health

# Swagger UI
start http://localhost:8083/swagger-ui.html
```

---

## 📁 파일 구조

```
ga-ai-consultant-service/
├── scripts/
│   ├── verify-lightsail-db.sql      # DB 환경 확인
│   ├── install-pgvector.sql          # pgvector 설치
│   ├── create-ai-schema.sql          # 스키마 생성 (옵션)
│   └── setup-lightsail.ps1           # 환경변수 설정 (PowerShell)
│
├── src/main/resources/
│   ├── application-lightsail.yml     # Lightsail 전용 설정
│   └── db/migration/
│       ├── V1__create_ai_tables.sql                    # 기본 (public 스키마 + pgvector 설치)
│       ├── V1__create_ai_tables_with_schema.sql        # ai_consultant 스키마 사용
│       └── V1__create_ai_tables_no_extension.sql.disabled  # pgvector 이미 설치된 경우
│
├── env.lightsail.template            # 환경변수 템플릿
├── LIGHTSAIL_SETUP_GUIDE.md          # 상세 설정 가이드
├── DEPLOYMENT_CHECKLIST.md           # 배포 체크리스트
└── README_LIGHTSAIL.md               # 이 파일
```

---

## 📋 필수 환경변수

| 변수 | 설명 | 예시 |
|------|------|------|
| `DATABASE_URL` | Lightsail PostgreSQL JDBC URL | `jdbc:postgresql://ls-xxx.rds.amazonaws.com:5432/postgres` |
| `DATABASE_USERNAME` | 데이터베이스 사용자 | `dbmasteruser` |
| `DATABASE_PASSWORD` | 데이터베이스 비밀번호 | `your_password` |
| `OPENAI_API_KEY` | OpenAI API 키 | `sk-proj-xxx` |
| `JWT_SECRET` | JWT 시크릿 키 (256비트) | `your-secret-key` |
| `REDIS_HOST` | Redis 호스트 | `localhost` |
| `REDIS_PORT` | Redis 포트 | `6379` |
| `REDIS_PASSWORD` | Redis 비밀번호 | `your_redis_password` |
| `ALLOWED_ORIGINS` | CORS 허용 오리진 | `https://app.goalmond.com` |

---

## 🔧 설정 옵션

### 옵션 1: public 스키마 사용 (권장)

**가장 간단한 방법**

- 현재 마이그레이션 파일 그대로 사용
- 추가 설정 불필요

### 옵션 2: ai_consultant 스키마 생성

**데이터 격리가 필요한 경우**

1. 스키마 생성:
   ```bash
   psql -h your-endpoint -U your_username -d your_database -f scripts/create-ai-schema.sql
   ```

2. 마이그레이션 파일 교체:
   ```bash
   mv src/main/resources/db/migration/V1__create_ai_tables.sql V1__create_ai_tables.sql.backup
   cp src/main/resources/db/migration/V1__create_ai_tables_with_schema.sql V1__create_ai_tables.sql
   ```

3. 환경변수 추가:
   ```bash
   FLYWAY_SCHEMA=ai_consultant
   ```

### 옵션 3: pgvector 이미 설치됨

**pgvector가 DBA에 의해 이미 설치된 경우**

마이그레이션 파일 교체:
```bash
mv src/main/resources/db/migration/V1__create_ai_tables.sql V1__create_ai_tables.sql.backup
mv src/main/resources/db/migration/V1__create_ai_tables_no_extension.sql.disabled V1__create_ai_tables.sql
```

---

## 🔍 검증 방법

### 1. 데이터베이스 테이블 확인

```sql
-- psql에서 실행
\dt

-- 예상 결과:
-- conversations
-- messages
-- documents
```

### 2. pgvector 타입 확인

```sql
\d documents

-- embedding 컬럼이 vector(1536) 타입인지 확인
```

### 3. 인덱스 확인

```sql
SELECT indexname, indexdef 
FROM pg_indexes 
WHERE tablename = 'documents';

-- idx_documents_embedding (HNSW 인덱스) 확인
```

### 4. API 테스트

```bash
# Health Check
curl http://localhost:8083/api/ai/consultant/health

# 예상 응답:
# {
#   "success": true,
#   "data": {
#     "status": "UP",
#     "service": "AI Consultant Service"
#   }
# }
```

---

## ⚠️ 문제 해결

### 문제 1: pgvector 확장 없음

**증상**: `ERROR: type "vector" does not exist`

**해결**:
```sql
-- 사용 가능 여부 확인
SELECT * FROM pg_available_extensions WHERE name = 'vector';

-- 설치
CREATE EXTENSION IF NOT EXISTS vector;
```

### 문제 2: 연결 실패

**증상**: `Connection refused`

**해결**:
1. Lightsail 보안 그룹에서 5432 포트 확인
2. 내 IP가 허용되었는지 확인
3. psql로 직접 연결 테스트

### 문제 3: Flyway 체크섬 오류

**증상**: `Migration checksum mismatch`

**해결**:
```sql
-- Flyway 히스토리 확인
SELECT * FROM flyway_schema_history;

-- 필요 시 해당 마이그레이션 삭제 후 재실행
DELETE FROM flyway_schema_history WHERE version = '1';
```

더 많은 문제 해결 방법은 `LIGHTSAIL_SETUP_GUIDE.md`를 참조하세요.

---

## 📚 관련 문서

- **상세 설정 가이드**: [LIGHTSAIL_SETUP_GUIDE.md](LIGHTSAIL_SETUP_GUIDE.md)
- **배포 체크리스트**: [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)
- **보안 검증 보고서**: [SECURITY_VERIFICATION_REPORT.md](SECURITY_VERIFICATION_REPORT.md)
- **검증 결과 요약**: [VERIFICATION_SUMMARY.md](VERIFICATION_SUMMARY.md)

---

## 🎯 배포 체크리스트

배포 전 확인:
- [ ] Lightsail PostgreSQL 연결 테스트 성공
- [ ] pgvector 확장 설치 완료
- [ ] 환경변수 설정 완료
- [ ] Flyway 마이그레이션 성공
- [ ] 로컬에서 애플리케이션 실행 성공

배포 후 확인:
- [ ] 테이블 3개 생성 확인
- [ ] Health Check API 정상 응답
- [ ] Swagger UI 접속 가능
- [ ] 로그에 에러 없음

---

## 💡 팁

### Lightsail 최적화

1. **연결 풀 크기 조정**
   ```bash
   export DB_MAX_POOL_SIZE=5  # Lightsail 플랜에 맞게
   export DB_MIN_IDLE=2
   ```

2. **SSL 연결 사용**
   ```bash
   DATABASE_URL=jdbc:postgresql://your-endpoint:5432/db?ssl=true&sslmode=require
   ```

3. **백업 활성화**
   - Lightsail 콘솔에서 자동 백업 설정
   - 마이그레이션 전 스냅샷 생성

4. **모니터링**
   - Lightsail 콘솔에서 CPU, 메모리 확인
   - CloudWatch 연동 고려

### 프로덕션 배포

```bash
# 1. JAR 빌드
.\gradlew.bat :ga-ai-consultant-service:bootJar

# 2. JAR 파일 확인
ls build\libs\*.jar

# 3. 프로덕션 서버에서 실행
java -jar ga-ai-consultant-service-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=lightsail
```

---

## 🚀 성능 권장사항

| 항목 | 권장값 | 설명 |
|------|--------|------|
| HikariCP Pool | 5-10 | Lightsail 연결 제한 고려 |
| Query Timeout | 30초 | 긴 쿼리 방지 |
| HNSW Index | m=16, ef_construction=64 | 벡터 검색 성능 |
| Redis Timeout | 3초 | Rate Limiting 응답성 |

---

## 📞 지원

문제가 지속되면:
1. 로그 파일 확인: `logs/ai-consultant-service.log`
2. Lightsail 콘솔에서 데이터베이스 메트릭 확인
3. `LIGHTSAIL_SETUP_GUIDE.md`의 문제 해결 섹션 참조

---

**마지막 업데이트**: 2026-01-20
**버전**: 1.0.0
