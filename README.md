# Go Almond (GA) API Platform

Modern MSA & Reactive-Alternative 플랫폼

## 기술 스택

- **Java 21 (LTS)**
- **Kotlin**
- **Spring Boot 3.4+**
- **Gradle Kotlin DSL**
- **Spring Data JPA**
- **gRPC**
- **PostgreSQL**
- **Redis**
- **Docker & Docker Compose**

## 프로젝트 구조

```
ga-api-platform/
├── ga-common/              # 공통 모듈
│   ├── entity/            # BaseEntity, AuditEntity
│   ├── dto/               # 공통 응답 DTO
│   └── exception/         # 예외 처리
├── ga-grpc-interface/     # gRPC 인터페이스
│   └── proto/            # .proto 파일
├── ga-auth-service/       # 인증/인가 서비스 (Port 8081)
├── ga-user-service/       # 사용자 프로필 서비스 (Port 8082, gRPC 9090)
└── ga-audit-service/      # 감사 로그 서비스 (Port 8083, gRPC 9091)
```

## 주요 기능

### 1. 가상 스레드 (Virtual Threads)
모든 모듈의 `application.yml`에 가상 스레드가 활성화되어 있어 높은 처리량을 제공합니다.

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

### 2. UUID v4 기본키
모든 Entity는 `BaseEntity`를 상속받아 UUID v4를 Primary Key로 사용합니다.

### 3. gRPC 통신
서비스 간 통신은 gRPC를 사용하여 초저지연 통신을 제공합니다.

### 4. Spring Data JPA
복잡한 정규화 테이블 간의 관계를 안정적으로 관리합니다.

## 초기 설정

### Gradle Wrapper 생성

프로젝트를 처음 클론한 경우, Gradle Wrapper를 생성해야 합니다:

```bash
gradle wrapper --gradle-version 8.5
```

또는 이미 설치된 Gradle이 있다면:

```bash
./gradlew wrapper
```

## 빌드 및 실행

### 로컬 개발

1. PostgreSQL과 Redis를 실행합니다:
```bash
docker-compose up -d postgres redis
```

2. 애플리케이션을 빌드합니다:
```bash
./gradlew clean build
```

3. 각 서비스를 실행합니다:
```bash
./gradlew :ga-auth-service:bootRun
./gradlew :ga-user-service:bootRun
./gradlew :ga-audit-service:bootRun
```

### Docker Compose로 전체 실행

```bash
docker-compose up -d
```

## 환경 변수

`.env` 파일을 생성하여 다음 변수를 설정하세요:

```env
DB_USERNAME=postgres
DB_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379
JWT_SECRET=your-secret-key-change-in-production-min-256-bits
```

## API 엔드포인트

- **Auth Service**: http://localhost:8081
- **User Service**: http://localhost:8082
- **Audit Service**: http://localhost:8083

## gRPC 포트

- **User Service**: 9090
- **Audit Service**: 9091

## CI/CD

GitHub Actions를 통해 자동 빌드 및 배포가 설정되어 있습니다.

1. `main` 브랜치에 푸시하면 자동으로 빌드 및 Docker Hub에 푸시됩니다.
2. 배포는 AWS Lightsail 서버로 자동 진행됩니다.

필요한 GitHub Secrets:
- `DOCKER_HUB_USERNAME`
- `DOCKER_HUB_TOKEN`
- `AWS_LIGHTSAIL_HOST`
- `AWS_LIGHTSAIL_USERNAME`
- `AWS_LIGHTSAIL_SSH_KEY`

## 라이선스

Copyright (c) 2024 Go Almond
