# Lightsail PostgreSQL 연동 가이드

AWS Lightsail PostgreSQL 17 환경에서 AI 상담 모듈을 배포하기 위한 단계별 가이드입니다.

## 목차

1. [사전 준비](#사전-준비)
2. [환경 확인](#환경-확인)
3. [설정 선택](#설정-선택)
4. [마이그레이션 실행](#마이그레이션-실행)
5. [애플리케이션 배포](#애플리케이션-배포)
6. [문제 해결](#문제-해결)

---

## 사전 준비

### 필요한 도구

1. **PostgreSQL 클라이언트 (psql)**
   - Windows: https://www.postgresql.org/download/windows/
   - 또는 pgAdmin 사용

2. **Gradle** (이미 프로젝트에 포함)

3. **Lightsail 접속 정보**
   - 엔드포인트
   - 포트 (기본: 5432)
   - 데이터베이스 이름
   - 마스터 사용자 이름 및 비밀번호

---

## 환경 확인

### 1단계: Lightsail PostgreSQL 연결 테스트

```bash
# psql로 연결
psql -h your-lightsail-endpoint.rds.amazonaws.com \
     -U your_master_username \
     -d your_database_name \
     -p 5432
```

**Windows (PowerShell):**
```powershell
psql -h your-lightsail-endpoint.rds.amazonaws.com -U your_master_username -d your_database_name -p 5432
```

### 2단계: 환경 검증 스크립트 실행

```bash
# 데이터베이스 환경 확인
psql -h your-endpoint -U your_username -d your_database -f scripts/verify-lightsail-db.sql
```

**확인 항목:**
- ✅ PostgreSQL 17 버전 확인
- ✅ pgvector 확장 지원 여부
- ✅ CREATE EXTENSION 권한 확인
- ✅ rds_superuser 역할 확인

---

## 설정 선택

### 옵션 1: public 스키마 사용 (권장 - 단순)

**장점:**
- 설정 변경 최소화
- 빠른 구축

**설정:**
1. 현재 마이그레이션 파일 사용 (`V1__create_ai_tables.sql`)
2. 환경변수 설정

**환경변수:**
```bash
# .env.lightsail 파일 생성
cp .env.lightsail.example .env.lightsail

# 값 수정 후 로드 (PowerShell)
.\scripts\setup-lightsail.ps1
```

### 옵션 2: ai_consultant 스키마 생성 (격리)

**장점:**
- 데이터 격리
- 명확한 네임스페이스

**설정:**
1. 스키마 생성:
   ```sql
   psql -h your-endpoint -U your_username -d your_database -f scripts/create-ai-schema.sql
   ```

2. 마이그레이션 파일 교체:
   ```bash
   # 기존 파일 백업
   mv src/main/resources/db/migration/V1__create_ai_tables.sql src/main/resources/db/migration/V1__create_ai_tables.sql.backup
   
   # 스키마 버전 사용
   cp src/main/resources/db/migration/V1__create_ai_tables_with_schema.sql src/main/resources/db/migration/V1__create_ai_tables.sql
   ```

3. 환경변수에 추가:
   ```bash
   export FLYWAY_SCHEMA=ai_consultant
   ```

### 옵션 3: pgvector가 이미 설치된 경우

**설정:**
1. 마이그레이션 파일 교체:
   ```bash
   mv src/main/resources/db/migration/V1__create_ai_tables.sql src/main/resources/db/migration/V1__create_ai_tables.sql.backup
   
   mv src/main/resources/db/migration/V1__create_ai_tables_no_extension.sql.disabled src/main/resources/db/migration/V1__create_ai_tables.sql
   ```

---

## 마이그레이션 실행

### 1단계: 환경변수 설정 확인

**.env.lightsail 예시:**
```bash
DATABASE_URL=jdbc:postgresql://ls-xxx.xxx.us-east-1.rds.amazonaws.com:5432/postgres
DATABASE_USERNAME=dbmasteruser
DATABASE_PASSWORD=your_password
OPENAI_API_KEY=sk-proj-xxx
JWT_SECRET=your-256-bit-secret-key
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
ALLOWED_ORIGINS=https://app.goalmond.com
```

### 2단계: PowerShell에서 환경변수 로드

```powershell
# setup-lightsail.ps1 실행
cd ga-ai-consultant-service
.\scripts\setup-lightsail.ps1

# 또는 수동 로드
Get-Content .env.lightsail | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
    }
}
```

### 3단계: Flyway 정보 확인

```bash
cd ..  # ga-api-platform 디렉토리로
.\gradlew.bat :ga-ai-consultant-service:flywayInfo
```

**예상 출력:**
```
+----------------+---------+----------------------+------+---------------------+---------+
| Category       | Version | Description          | Type | Installed On        | State   |
+----------------+---------+----------------------+------+---------------------+---------+
|                | 1       | create ai tables     | SQL  |                     | Pending |
+----------------+---------+----------------------+------+---------------------+---------+
```

### 4단계: Flyway 마이그레이션 실행

```bash
.\gradlew.bat :ga-ai-consultant-service:flywayMigrate
```

**성공 시:**
```
Successfully applied 1 migration to schema "public"
```

### 5단계: 테이블 생성 확인

```sql
-- psql에서 실행
\dt

-- 또는
SELECT tablename FROM pg_tables WHERE schemaname = 'public';
```

**예상 결과:**
- conversations
- messages
- documents

### 6단계: pgvector 타입 확인

```sql
\d documents
```

**확인 사항:**
- `embedding` 컬럼이 `vector(1536)` 타입인지 확인

---

## 애플리케이션 배포

### 로컬 테스트

```bash
# Lightsail 프로파일로 실행
.\gradlew.bat :ga-ai-consultant-service:bootRun --args='--spring.profiles.active=lightsail'
```

**확인 사항:**
1. Flyway 마이그레이션 자동 실행 로그
2. Hibernate 검증 성공
3. 서버 시작 완료 (포트 8083)

### Health Check 테스트

```bash
curl http://localhost:8083/api/ai/consultant/health
```

**예상 응답:**
```json
{
  "success": true,
  "data": {
    "status": "UP",
    "service": "AI Consultant Service"
  }
}
```

### 프로덕션 배포

1. **JAR 빌드:**
   ```bash
   .\gradlew.bat :ga-ai-consultant-service:build
   ```

2. **환경변수 설정 (프로덕션 서버):**
   ```bash
   # Linux/Mac: .bashrc 또는 .profile에 추가
   export DATABASE_URL=jdbc:postgresql://...
   export DATABASE_USERNAME=...
   # ... 기타 환경변수
   ```

3. **애플리케이션 실행:**
   ```bash
   java -jar ga-ai-consultant-service/build/libs/ga-ai-consultant-service-1.0.0-SNAPSHOT.jar \
     --spring.profiles.active=lightsail
   ```

4. **Docker 배포 (선택사항):**
   ```bash
   # Docker 이미지 빌드
   docker build -t ai-consultant:latest -f ga-ai-consultant-service/Dockerfile .
   
   # 실행
   docker run -p 8083:8083 --env-file .env.lightsail ai-consultant:latest
   ```

---

## 문제 해결

### 문제 1: pgvector 확장이 없음

**증상:**
```
ERROR: type "vector" does not exist
```

**해결:**
```sql
-- 1. pgvector 사용 가능 여부 확인
SELECT * FROM pg_available_extensions WHERE name = 'vector';

-- 2. 사용 가능하면 설치
psql -h your-endpoint -U your_username -d your_database -f scripts/install-pgvector.sql

-- 3. 사용 불가능하면 AWS 지원팀 문의 또는 RDS로 마이그레이션
```

### 문제 2: 연결 실패

**증상:**
```
Connection refused
```

**해결:**
1. Lightsail 보안 그룹 확인
   - 5432 포트가 열려 있는지 확인
   - 소스 IP 확인

2. 연결 문자열 확인
   ```bash
   echo $DATABASE_URL
   ```

3. psql로 직접 테스트
   ```bash
   psql -h your-endpoint -U your_username -d your_database
   ```

### 문제 3: Flyway 체크섬 불일치

**증상:**
```
Migration checksum mismatch
```

**해결:**
```sql
-- Flyway 히스토리 확인
SELECT * FROM flyway_schema_history;

-- 필요 시 재설정
-- (주의: 프로덕션에서는 조심스럽게!)
DELETE FROM flyway_schema_history WHERE version = '1';

-- 다시 마이그레이션
.\gradlew.bat :ga-ai-consultant-service:flywayMigrate
```

### 문제 4: 권한 부족

**증상:**
```
ERROR: permission denied
```

**해결:**
```sql
-- 1. 현재 역할 확인
SELECT rolname FROM pg_roles WHERE pg_has_role(current_user, oid, 'member');

-- 2. rds_superuser 역할이 없으면 Lightsail 콘솔에서 마스터 사용자 사용

-- 3. 필요 시 권한 부여 (DBA가 실행)
GRANT ALL PRIVILEGES ON SCHEMA public TO your_app_user;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO your_app_user;
```

---

## 체크리스트

### 배포 전

- [ ] Lightsail PostgreSQL 엔드포인트 확인
- [ ] psql로 연결 테스트 성공
- [ ] pgvector 확장 설치 완료
- [ ] 환경변수 설정 완료
- [ ] Flyway 마이그레이션 테스트 성공
- [ ] 로컬에서 애플리케이션 실행 성공

### 배포 후

- [ ] 테이블 3개 생성 확인
- [ ] pgvector 타입 정상 작동
- [ ] HNSW 인덱스 생성 확인
- [ ] Health Check API 정상 응답
- [ ] 로그에 에러 없음
- [ ] Redis 연결 정상

---

## 추가 리소스

- **검증 스크립트**: `scripts/verify-lightsail-db.sql`
- **설치 스크립트**: `scripts/install-pgvector.sql`
- **스키마 생성**: `scripts/create-ai-schema.sql`
- **환경변수 예시**: `.env.lightsail.example`
- **설정 파일**: `src/main/resources/application-lightsail.yml`

---

## 지원

문제가 지속되면 다음을 확인하세요:

1. **로그 파일**: `logs/ai-consultant-service.log`
2. **Lightsail 콘솔**: 데이터베이스 메트릭 및 로그
3. **AWS 지원**: pgvector 지원 여부 문의

---

**마지막 업데이트**: 2026-01-20
