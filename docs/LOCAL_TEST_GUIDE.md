# 로컬 테스트 가이드

로컬에서 빌드, 자동 테스트, 애플리케이션 실행, API 수동 테스트까지 한 번에 따라 할 수 있는 가이드입니다.

---

## 사전 요구사항

- **JDK 21** 이상
- **Gradle** (또는 프로젝트 루트의 Gradle Wrapper 사용)
  - Windows PowerShell 사용 시: `.\gradlew.bat` 사용
  - macOS/Linux 또는 Git Bash: `./gradlew` 사용

---

## 1단계: 빌드 및 자동 테스트

프로젝트 루트에서 실행하세요.

```bash
./gradlew build
```

- 빌드와 함께 **단위/통합 테스트**가 실행됩니다.
- 테스트만 실행하려면:

```bash
./gradlew test
```

(현재 프로젝트에는 테스트 코드가 없을 수 있습니다. 추후 테스트를 추가하면 동일한 명령으로 실행됩니다.)

---

## 2단계: 애플리케이션 실행

```bash
./gradlew bootRun
```

- 기동이 끝나면 API Base URL: **http://localhost:8080**
- 로그에 `Started ApiApplicationKt` 가 보이면 준비된 상태입니다.

---

## 3단계: API 수동 테스트

아래 명령으로 각 엔드포인트를 확인할 수 있습니다. 상세 스펙은 [API 문서](api/README.md)를 참고하세요.

### POST /api/v1/matching/run (매칭 실행)

**기대**: HTTP 200, `success: true`, `data`에 매칭 결과 포함.

```bash
curl -X POST http://localhost:8080/api/v1/matching/run \
  -H "Content-Type: application/json" \
  -d '{"user_id": "550e8400-e29b-41d4-a716-446655440000"}'
```

**Windows PowerShell** (curl 없을 때):

```powershell
Invoke-WebRequest -Uri "http://localhost:8080/api/v1/matching/run" -Method POST -ContentType "application/json" -Body '{"user_id": "550e8400-e29b-41d4-a716-446655440000"}'
```

### GET /api/v1/matching/result (최신 매칭 결과)

**기대**: 매칭을 한 번이라도 실행했다면 HTTP 200, `success: true`, `data`에 결과. 한 번도 실행하지 않았다면 HTTP 404.

```bash
curl -s http://localhost:8080/api/v1/matching/result
```

### GET /api/v1/programs (프로그램 목록)

**기대**: HTTP 200, `success: true`, `data.programs` 배열 존재.

```bash
curl -s "http://localhost:8080/api/v1/programs?type=community_college&page=1&size=10"
```

### GET /api/v1/schools/{schoolId} (학교 상세)

**기대**: HTTP 200, `success: true`, `data`에 학교 상세 정보.

```bash
curl -s http://localhost:8080/api/v1/schools/school-001
```

### Windows에서 curl이 없는 경우

- **PowerShell**: 위의 `Invoke-WebRequest` 예시처럼 `-Uri`, `-Method`, `-ContentType`, `-Body` 사용.
- **Git Bash** 또는 **WSL**을 설치하면 `curl` 명령을 그대로 사용할 수 있습니다.

---

## Docker로 실행 후 테스트 (선택)

Docker로 API를 띄운 뒤 같은 방식으로 테스트할 수 있습니다.

```bash
docker-compose up -d
```

- 기동 후 `http://localhost:8080` (또는 docker-compose에서 노출한 포트)로 위와 동일하게 curl/Invoke-WebRequest로 요청하면 됩니다.
- 자세한 Docker 사용법은 [README.md](../README.md)의 "Docker를 통한 실행" 섹션을 참고하세요.

---

## 문제 해결

| 상황 | 조치 |
|------|------|
| **포트 8080 사용 중** | `src/main/resources/application.yml`에서 `server.port`를 다른 값(예: 8081)으로 변경하거나, 8080을 사용 중인 프로세스를 종료하세요. |
| **빌드 실패** | JDK 21이 맞는지 `java -version`으로 확인. 그 다음 `./gradlew clean build`로 다시 시도. |
| **bootRun 후 연결 거부** | 로그에 에러가 없는지 확인하고, 기동이 완료될 때까지 10~20초 정도 기다린 뒤 다시 요청하세요. |

---

이 가이드를 따라하면 로컬에서 빌드·테스트·API 확인까지 한 번에 진행할 수 있습니다.
