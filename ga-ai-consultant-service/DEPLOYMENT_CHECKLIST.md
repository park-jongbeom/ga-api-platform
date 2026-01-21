# Lightsail PostgreSQL 배포 체크리스트

이 체크리스트를 사용하여 단계별로 배포를 진행하세요.

## Phase 1: 사전 확인 (10분)

### Lightsail 접속 정보
- [ ] Lightsail PostgreSQL 엔드포인트 확인
- [ ] 데이터베이스 이름 확인
- [ ] 마스터 사용자 이름 및 비밀번호 확인
- [ ] 보안 그룹 설정 (5432 포트 허용)
- [ ] 내 IP 주소가 보안 그룹에 추가되어 있음

### 로컬 환경
- [ ] psql 클라이언트 설치 완료
- [ ] Java 21 설치 확인
- [ ] Gradle 사용 가능

---

## Phase 2: 데이터베이스 환경 확인 (15분)

### 연결 테스트
```bash
psql -h your-lightsail-endpoint -U your_username -d your_database -p 5432
```
- [ ] Lightsail PostgreSQL 연결 성공
- [ ] PostgreSQL 17 버전 확인

### 검증 스크립트 실행
```bash
psql -h your-endpoint -U your_username -d your_database -f scripts/verify-lightsail-db.sql
```

확인 항목:
- [ ] pgvector 확장 사용 가능 (pg_available_extensions에 표시)
- [ ] pgvector 확장 설치 여부 (pg_extension에 표시)
- [ ] CREATE EXTENSION 권한 확인 (true)
- [ ] rds_superuser 역할 확인
- [ ] 현재 스키마 확인 (public 또는 ai_consultant)
- [ ] 최대 연결 수 확인 (max_connections)

---

## Phase 3: pgvector 설치 (5분)

**pgvector가 설치되지 않은 경우:**

```bash
psql -h your-endpoint -U your_username -d your_database -f scripts/install-pgvector.sql
```

- [ ] pgvector 확장 설치 성공
- [ ] 벡터 타입 테스트 성공

**pgvector가 이미 설치된 경우:**

- [ ] 마이그레이션 파일 교체 (V1__create_ai_tables_no_extension.sql)

**pgvector 사용 불가능한 경우:**

- [ ] AWS 지원팀에 문의
- [ ] 또는 RDS PostgreSQL로 마이그레이션 계획

---

## Phase 4: 스키마 전략 선택 (5분)

### 옵션 1: public 스키마 사용 (권장)
- [ ] 현재 마이그레이션 파일 그대로 사용
- [ ] 환경변수: FLYWAY_SCHEMA 설정 불필요

### 옵션 2: ai_consultant 스키마 생성
```bash
psql -h your-endpoint -U your_username -d your_database -f scripts/create-ai-schema.sql
```
- [ ] ai_consultant 스키마 생성 완료
- [ ] 마이그레이션 파일 교체 (V1__create_ai_tables_with_schema.sql)
- [ ] 환경변수: FLYWAY_SCHEMA=ai_consultant 설정

---

## Phase 5: 환경변수 설정 (10분)

### 환경변수 파일 생성
```bash
cp env.lightsail.template .env.lightsail
```
- [ ] .env.lightsail 파일 생성
- [ ] 실제 값으로 수정 완료

### 필수 환경변수 설정 확인
- [ ] DATABASE_URL (Lightsail 엔드포인트)
- [ ] DATABASE_USERNAME (마스터 사용자)
- [ ] DATABASE_PASSWORD (비밀번호)
- [ ] OPENAI_API_KEY (OpenAI API 키)
- [ ] JWT_SECRET (256비트 시크릿 키)
- [ ] REDIS_HOST (Redis 호스트)
- [ ] REDIS_PORT (Redis 포트, 기본 6379)
- [ ] REDIS_PASSWORD (Redis 비밀번호)
- [ ] ALLOWED_ORIGINS (CORS 설정)

### 환경변수 로드 (PowerShell)
```powershell
.\scripts\setup-lightsail.ps1
```
- [ ] 환경변수 로드 성공
- [ ] 필수 환경변수 확인 완료

---

## Phase 6: Flyway 마이그레이션 (10분)

### Flyway 정보 확인
```bash
cd c:\Users\qk54r\ga-api-platform
.\gradlew.bat :ga-ai-consultant-service:flywayInfo
```
- [ ] Flyway 연결 성공
- [ ] Pending 상태 마이그레이션 확인

### Flyway 검증
```bash
.\gradlew.bat :ga-ai-consultant-service:flywayValidate
```
- [ ] 마이그레이션 스크립트 검증 성공

### Flyway 마이그레이션 실행
```bash
.\gradlew.bat :ga-ai-consultant-service:flywayMigrate
```
- [ ] 마이그레이션 성공 메시지 확인
- [ ] "Successfully applied 1 migration" 확인

### 데이터베이스 테이블 확인
```sql
\dt
```
- [ ] conversations 테이블 생성 확인
- [ ] messages 테이블 생성 확인
- [ ] documents 테이블 생성 확인

### pgvector 타입 확인
```sql
\d documents
```
- [ ] embedding 컬럼이 vector(1536) 타입 확인
- [ ] HNSW 인덱스 생성 확인 (idx_documents_embedding)

---

## Phase 7: 로컬 애플리케이션 테스트 (15분)

### 빌드 확인
```bash
.\gradlew.bat :ga-ai-consultant-service:build -x test
```
- [ ] 빌드 성공 (BUILD SUCCESSFUL)
- [ ] JAR 파일 생성 확인

### 애플리케이션 실행
```bash
.\gradlew.bat :ga-ai-consultant-service:bootRun --args='--spring.profiles.active=lightsail'
```

확인 사항:
- [ ] Flyway 마이그레이션 자동 실행 로그 확인
- [ ] Hibernate 검증 성공 로그
- [ ] "Started AiConsultantServiceApplication" 메시지 확인
- [ ] 포트 8083에서 리스닝 중

### Health Check API 테스트
```bash
curl http://localhost:8083/api/ai/consultant/health
```
- [ ] 200 OK 응답
- [ ] "status": "UP" 확인

### Swagger UI 접속
```
http://localhost:8083/swagger-ui.html
```
- [ ] Swagger UI 페이지 로드 성공
- [ ] 4개 API 엔드포인트 표시 확인

### 데이터베이스 연결 확인
```sql
SELECT datname, usename, application_name, client_addr, state
FROM pg_stat_activity
WHERE datname = 'your_database_name';
```
- [ ] Spring Boot 애플리케이션 연결 확인
- [ ] HikariPool 연결 확인

---

## Phase 8: 프로덕션 배포 준비 (선택사항)

### JAR 빌드
```bash
.\gradlew.bat :ga-ai-consultant-service:bootJar
```
- [ ] JAR 파일 생성 (build/libs/*.jar)

### Docker 이미지 빌드 (선택사항)
```bash
docker build -t ai-consultant:latest -f ga-ai-consultant-service/Dockerfile .
```
- [ ] Docker 이미지 빌드 성공

### 프로덕션 환경변수 설정
- [ ] 프로덕션 DATABASE_URL 설정
- [ ] 프로덕션 OPENAI_API_KEY 설정
- [ ] 강력한 JWT_SECRET 생성 및 설정
- [ ] 프로덕션 ALLOWED_ORIGINS 설정
- [ ] CloudWatch 설정 (선택사항)

---

## Phase 9: 배포 후 검증

### 애플리케이션 상태
- [ ] Health Check API 정상 응답
- [ ] Swagger UI 접속 가능
- [ ] 로그 파일에 에러 없음

### 데이터베이스
- [ ] 테이블 3개 정상 존재
- [ ] pgvector 타입 정상 작동
- [ ] 인덱스 모두 생성 완료
- [ ] 연결 풀 정상 동작

### Redis
- [ ] Redis 연결 성공
- [ ] Rate Limiting 테스트 (11회 연속 요청)

### 보안
- [ ] JWT 인증 정상 작동
- [ ] CORS 설정 적용 확인
- [ ] Rate Limiting 적용 확인

---

## 문제 해결 참고

문제 발생 시:
1. 로그 파일 확인: `logs/ai-consultant-service.log`
2. Lightsail 콘솔에서 데이터베이스 메트릭 확인
3. 보안 그룹 설정 재확인
4. 환경변수 설정 재확인
5. `LIGHTSAIL_SETUP_GUIDE.md`의 문제 해결 섹션 참조

---

## 완료!

모든 체크리스트 항목이 완료되면 AI 상담 모듈이 Lightsail PostgreSQL 환경에서 정상적으로 작동합니다.

**다음 단계:**
- 실제 데이터로 테스트
- 모니터링 설정
- 백업 정책 수립
- 성능 튜닝

---

**마지막 업데이트**: 2026-01-20
