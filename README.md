# Go Almond Matching API

AI 매칭 API - 단일 모듈 Spring Boot 프로젝트. Mock API(default), Auth·User Profile API(local/lightsail 프로파일, DB 연동) 제공.

## 기술 스택

- **Java 21 (LTS)**
- **Kotlin**
- **Spring Boot 3.4+**
- **Gradle Kotlin DSL**
- **Docker & Docker Compose**

## 프로젝트 구조

**단일 모듈 프로젝트** - 모든 코드가 루트 `ga-api-platform` 디렉토리에 있습니다.

```
ga-api-platform/
├── src/
│   └── main/
│       ├── kotlin/com/goalmond/api/
│       │   ├── ApiApplication.kt          # 메인 애플리케이션 진입점
│       │   ├── config/
│       │   │   └── WebConfig.kt          # CORS 설정
│       │   ├── controller/
│       │   │   └── MockMatchingController.kt  # Mock API 컨트롤러
│       │   └── domain/dto/
│       │       ├── ApiResponse.kt         # 공통 API 응답 래퍼
│       │       ├── ErrorResponse.kt       # 에러 응답 DTO
│       │       ├── MatchingResponse.kt    # 매칭 결과 응답 DTO
│       │       ├── ProgramResponse.kt     # 프로그램 응답 DTO
│       │       └── SchoolResponse.kt      # 학교 응답 DTO
│       └── resources/
│           └── application.yml             # 애플리케이션 설정
├── build.gradle.kts                       # Gradle 빌드 설정
├── settings.gradle.kts                    # Gradle 프로젝트 설정
├── Dockerfile                             # Docker 이미지 빌드 설정
├── docker-compose.yml                     # Docker Compose 설정
├── .github/workflows/
│   └── deploy.yml                         # CI/CD 파이프라인
└── README.md
```

## 주요 기능

### Mock API 엔드포인트

1. **POST /api/v1/matching/run** - 매칭 실행
   - Request: `{ "user_id": "uuid" }`
   - Response: 매칭 점수 및 Top 5 학교 반환
   - 3가지 시나리오 Mock 데이터 제공 (안정권/도전권/전략)

2. **GET /api/v1/matching/result** - 최신 매칭 결과 조회
   - Response: 저장된 최신 매칭 결과 반환

3. **GET /api/v1/programs?type={type}** - 프로그램 목록 조회
   - type: `university`, `community_college`, `vocational`
   - Response: 프로그램 목록 (10개 샘플)

4. **GET /api/v1/schools/{schoolId}** - 학교 상세 조회
   - Response: 학교 상세 정보

### Auth & User Profile API (local / lightsail 프로파일)

DB가 연결된 환경(local, lightsail)에서 사용 가능합니다. Docker 배포 시 `SPRING_PROFILES_ACTIVE=lightsail`로 DB 연동됩니다.

- **POST /api/v1/auth/signup** - 회원가입
- **POST /api/v1/auth/login** - 로그인 (JWT 발급)
- **GET /api/v1/user/profile** - 프로필·학력·유학목표 통합 조회
- **PUT /api/v1/user/profile** - 프로필 기본 정보 저장
- **POST /api/v1/user/education** - 학력 정보 입력
- **POST /api/v1/user/preference** - 유학 목표 설정

상세 스펙: [docs/api/auth.md](docs/api/auth.md), [docs/api/user-profile.md](docs/api/user-profile.md)

## 로컬 개발

로컬에서 빌드·테스트·API 확인까지 한 번에 하려면 **[로컬 테스트 가이드](docs/LOCAL_TEST_GUIDE.md)**를 참고하세요.

### 사전 요구사항

- JDK 21 이상
- Gradle 8.5 이상 (또는 Gradle Wrapper 사용)

### 실행 방법

1. **프로젝트 빌드**:
```bash
./gradlew build
```

2. **애플리케이션 실행**:
   - **Mock API만 (default)**: DB 없이 실행
   ```bash
   ./gradlew bootRun
   ```
   - **Auth·User Profile 포함 (DB 연동)**: PostgreSQL 접속 정보가 있는 경우
   ```bash
   ./gradlew bootRun --args='--spring.profiles.active=local'
   ```
   - 로컬 DB 설정은 [application-local.yml](src/main/resources/application-local.yml) 또는 서버 `.env` 기준으로 적용됩니다.

3. **애플리케이션 접근**:
   - API: http://localhost:8080

4. **API 문서**: [docs/api/API.md](docs/api/API.md)가 메인 문서이며, [matching](docs/api/matching.md), [programs](docs/api/programs.md), [schools](docs/api/schools.md), [auth](docs/api/auth.md), [user-profile](docs/api/user-profile.md)에 상세가 있습니다.

## Docker를 통한 실행

### Docker 이미지 빌드

```bash
docker build -t ga-matching-api:latest .
```

### Docker Compose로 실행

**환경 정보 단일 관리**: 런타임 환경(프로파일, DB 연결 등)은 **`.env` 한 파일**에서만 관리합니다. 저장소의 `env.example`을 복사해 `.env`를 만든 뒤 값을 채우고, 같은 디렉터리에서 실행하세요.

```bash
cp env.example .env
# .env 에 DB_HOST, DB_PASSWORD 등 실제 값 입력 후
docker-compose up -d
```

### 컨테이너 로그 확인

```bash
docker-compose logs -f ga-matching-api
```

### 컨테이너 중지

```bash
docker-compose down
```

## 배포

### CI/CD 자동 배포

GitHub Actions를 통해 자동 빌드 및 배포가 설정되어 있습니다.

1. `main` 브랜치에 푸시하면 자동으로 빌드 및 Docker Hub에 푸시됩니다.
2. 배포는 서버로 자동 진행됩니다.

### GitHub Secrets 설정

**Repository Settings > Secrets and variables > Actions**에서 다음을 추가:

1. **Docker Hub 관련:**
   - `DOCKER_USERNAME`: Docker Hub 사용자명
   - `DOCKER_PASSWORD`: Docker Hub 액세스 토큰

2. **서버 접속 관련:**
   - `SERVER_HOST`: 배포 서버 호스트 주소
   - `SERVER_USER`: SSH 사용자명
   - `SERVER_SSH_KEY`: SSH 개인 키

### 수동 배포

서버에서 직접 Docker Compose를 실행하는 경우, **해당 디렉터리에 `.env`가 있어야** 합니다. 없으면 `env.example`을 복사해 `.env`를 만들고 값을 채운 뒤 실행하세요.

```bash
cd /home/$USER/ga-api-platform
docker-compose pull
docker-compose up -d
```

### 배포 전 확인

- **서버에 `.env` 존재**: `env.example`을 복사해 `DB_HOST`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` 등 실제 값 입력. (Docker 배포 시 `SPRING_PROFILES_ACTIVE=lightsail`는 compose에서 고정.)
- **DB 준비**: Lightsail PostgreSQL(또는 사용 DB) 기동 후 Flyway 마이그레이션이 적용될 수 있는 상태인지 확인.
- 자세한 절차: [docs/DEPLOYMENT_VERIFICATION.md](docs/DEPLOYMENT_VERIFICATION.md)

### 배포 확인

배포 후 서버·외부에서 어떻게 확인하는지는 **[배포 후 확인](docs/DEPLOYMENT_VERIFICATION.md)**을 참고하세요. 문제 발생 시 [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)를 참고하세요.

```bash
# 컨테이너 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f ga-matching-api

# 헬스 체크 (서버에서)
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/programs?type=community_college
```

## API 사용 예시

### 매칭 실행

```bash
curl -X POST http://localhost:8080/api/v1/matching/run \
  -H "Content-Type: application/json" \
  -d '{"user_id": "550e8400-e29b-41d4-a716-446655440000"}'
```

### 매칭 결과 조회

```bash
curl http://localhost:8080/api/v1/matching/result
```

### 프로그램 목록 조회

```bash
curl "http://localhost:8080/api/v1/programs?type=community_college&page=1&size=10"
```

### 학교 상세 조회

```bash
curl http://localhost:8080/api/v1/schools/school-001
```

### Auth·User Profile (local/lightsail 프로파일, JWT 필요)

```bash
# 로그인 후 토큰 발급
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"test1234Z"}' | jq -r '.data.token')

# 프로필 조회
curl -s http://localhost:8080/api/v1/user/profile -H "Authorization: Bearer $TOKEN"

# 프로필 저장
curl -X PUT http://localhost:8080/api/v1/user/profile \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"mbti":"INTJ","tags":"체계적","bio":"안녕하세요"}'

# 학력 정보 저장
curl -X POST http://localhost:8080/api/v1/user/education \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"schoolName":"테스트고","degree":"고등학교","gpa":3.5}'

# 유학 목표 저장
curl -X POST http://localhost:8080/api/v1/user/preference \
  -H "Content-Type: application/json" -H "Authorization: Bearer $TOKEN" \
  -d '{"targetProgram":"community_college","targetMajor":"CS","budgetUsd":50000}'
```

테스트 계정: `test@example.com` / `test1234Z` (배포 환경 시드 시)

## 프론트엔드 협업

프론트엔드 개발 시작 시 **[docs/FRONTEND_HANDOFF.md](docs/FRONTEND_HANDOFF.md)** 에서 사용 가능 API, 전달 링크 사용 방법, 이후 일정을 확인하세요.

프론트엔드는 API 프로젝트 데이터 없이 **전달받은 링크만** 사용합니다. API 문서는 링크로 전달되며, 전달받은 API 문서 링크와 Base URL만 사용해 연동하세요.

자세한 협업 정책은 `docs/04_FRONTEND_COOPERATION.md`를 참고하세요.

## 참고사항

- **default 프로파일**: Mock API만 제공. 인증 없이 호출 가능.
- **local / lightsail 프로파일**: DB 연동 시 Auth API, User Profile API 사용 가능. JWT 인증 필요.
- **배포**: Docker·Compose·`.env`만 준비하면 배포 가능. 프로덕션 보안·모니터링·문서화(Week 6)는 별도 진행 예정.
- **문서**: [docs/FRONTEND_HANDOFF.md](docs/FRONTEND_HANDOFF.md), [docs/DEPLOYMENT_VERIFICATION.md](docs/DEPLOYMENT_VERIFICATION.md), [docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)

## 라이선스

Copyright (c) 2024 Go Almond
