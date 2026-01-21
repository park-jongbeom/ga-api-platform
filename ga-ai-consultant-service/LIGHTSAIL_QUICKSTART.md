# Lightsail PostgreSQL 빠른 시작 가이드

5단계로 AI 상담 모듈을 Lightsail PostgreSQL에 배포하세요! (약 30분 소요)

## ⚡ 빠른 시작 (5단계)

### 📌 사전 준비

필요한 것:
- ✅ Lightsail PostgreSQL 17 인스턴스
- ✅ OpenAI API 키
- ✅ Redis 인스턴스
- ✅ psql 클라이언트

---

### 1️⃣ 데이터베이스 연결 확인 (2분)

```bash
# Lightsail PostgreSQL 연결 테스트
psql -h your-lightsail-endpoint.rds.amazonaws.com \
     -U your_master_username \
     -d your_database_name

# 연결되면 바로 종료
\q
```

**✅ 성공**: psql 프롬프트가 표시됨  
**❌ 실패**: 보안 그룹에서 5432 포트 열기

---

### 2️⃣ 환경 검증 (5분)

```bash
# 검증 스크립트 실행
cd c:\Users\qk54r\ga-api-platform\ga-ai-consultant-service
psql -h your-endpoint -U your_username -d your_database -f scripts\verify-lightsail-db.sql
```

**확인 사항:**
- ✅ PostgreSQL 17 버전
- ✅ pgvector 확장 지원 여부
- ✅ CREATE EXTENSION 권한

**pgvector 없으면:**
```bash
# pgvector 설치
psql -h your-endpoint -U your_username -d your_database -f scripts\install-pgvector.sql
```

---

### 3️⃣ 환경변수 설정 (5분)

```bash
# 1. 템플릿 복사
copy env.lightsail.template .env.lightsail

# 2. 편집 (메모장으로)
notepad .env.lightsail
```

**필수 항목 입력:**
```bash
DATABASE_URL=jdbc:postgresql://ls-xxx.rds.amazonaws.com:5432/postgres
DATABASE_USERNAME=dbmasteruser
DATABASE_PASSWORD=your_password
OPENAI_API_KEY=sk-proj-xxx
JWT_SECRET=your-256-bit-secret-key
REDIS_HOST=your-redis-host
REDIS_PORT=6379
REDIS_PASSWORD=your_redis_password
ALLOWED_ORIGINS=https://app.goalmond.com
```

**환경변수 로드 (PowerShell):**
```powershell
.\scripts\setup-lightsail.ps1
```

---

### 4️⃣ 마이그레이션 실행 (5분)

```bash
# 프로젝트 루트로 이동
cd ..

# Flyway 정보 확인
.\gradlew.bat :ga-ai-consultant-service:flywayInfo

# 마이그레이션 실행
.\gradlew.bat :ga-ai-consultant-service:flywayMigrate
```

**✅ 성공 메시지:**
```
Successfully applied 1 migration to schema "public"
```

**테이블 생성 확인:**
```bash
psql -h your-endpoint -U your_username -d your_database -c "\dt"
```

**예상 결과:**
- conversations
- messages
- documents

---

### 5️⃣ 애플리케이션 실행 (10분)

```bash
# 애플리케이션 빌드
.\gradlew.bat :ga-ai-consultant-service:build -x test

# Lightsail 프로파일로 실행
.\gradlew.bat :ga-ai-consultant-service:bootRun --args='--spring.profiles.active=lightsail'
```

**✅ 성공 로그:**
```
Started AiConsultantServiceApplication in X.XXX seconds
```

**테스트:**
```bash
# Health Check
curl http://localhost:8083/api/ai/consultant/health

# 브라우저에서 Swagger UI
start http://localhost:8083/swagger-ui.html
```

---

## 🎉 완료!

### 다음 단계

1. **API 테스트**
   - Swagger UI에서 각 엔드포인트 테스트
   - JWT 토큰 필요 (ga-auth-service에서 발급)

2. **모니터링 설정**
   - Lightsail 콘솔에서 메트릭 확인
   - CloudWatch 연동 고려

3. **프로덕션 배포**
   - JAR 파일 빌드: `.\gradlew.bat :ga-ai-consultant-service:bootJar`
   - Docker 이미지 빌드: `docker build -t ai-consultant:latest -f ga-ai-consultant-service/Dockerfile .`

---

## 🔧 문제 해결 (빠른 참조)

### 연결 실패
```bash
# 보안 그룹 확인
# Lightsail 콘솔 → 데이터베이스 → 네트워킹 → 보안 그룹
# 5432 포트가 내 IP에서 열려있는지 확인
```

### pgvector 없음
```bash
# 설치 스크립트 재실행
psql -h your-endpoint -U your_username -d your_database -f scripts\install-pgvector.sql
```

### Flyway 실패
```bash
# 연결 문자열 확인
echo $env:DATABASE_URL

# Flyway 캐시 클리어
.\gradlew.bat clean
.\gradlew.bat :ga-ai-consultant-service:flywayClean
.\gradlew.bat :ga-ai-consultant-service:flywayMigrate
```

### 애플리케이션 시작 실패
```bash
# 로그 확인
cat logs\ai-consultant-service.log

# 환경변수 확인
.\scripts\setup-lightsail.ps1
```

---

## 📚 추가 자료

- **상세 가이드**: [LIGHTSAIL_SETUP_GUIDE.md](LIGHTSAIL_SETUP_GUIDE.md)
- **체크리스트**: [DEPLOYMENT_CHECKLIST.md](DEPLOYMENT_CHECKLIST.md)
- **README**: [README_LIGHTSAIL.md](README_LIGHTSAIL.md)

---

## 💡 프로 팁

### 빠른 재배포
```bash
# 한 번에 실행 (환경변수 이미 설정된 경우)
.\gradlew.bat :ga-ai-consultant-service:flywayClean flywayMigrate bootRun --args='--spring.profiles.active=lightsail'
```

### 로컬 개발
```bash
# dev 프로파일 사용 (H2 메모리 DB)
.\gradlew.bat :ga-ai-consultant-service:bootRun --args='--spring.profiles.active=dev'
```

### 프로덕션 배포
```bash
# JAR 빌드 + 실행
.\gradlew.bat :ga-ai-consultant-service:bootJar
java -jar build\libs\*.jar --spring.profiles.active=lightsail
```

---

**시작 시간**: 약 30분  
**난이도**: ⭐⭐ 중급

도움이 필요하면 상세 가이드를 참조하세요! 🚀
