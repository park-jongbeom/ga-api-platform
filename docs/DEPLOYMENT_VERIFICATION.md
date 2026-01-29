# 배포 후 확인

CI/CD 또는 수동 배포가 끝난 뒤, 서버에서와 외부(본인 PC·브라우저)에서 각각 어떻게 확인할 수 있는지 정리한 가이드입니다.

---

## 0. 환경 정보 단일 관리 (.env)

**Docker 배포 시 런타임 환경 정보(프로파일, DB 연결 등)는 서버의 `.env` 한 파일에서만 관리합니다.**

- **위치**: 배포 서버의 `ga-api-platform` 디렉터리와 같은 위치에 `.env` 파일 생성
- **생성 방법**: 저장소의 `env.example`을 복사한 뒤, 값만 채워 넣기 (Git에는 `.env`가 포함되지 않음)
- **필수 변수**: `SPRING_PROFILES_ACTIVE`, DB 사용 시 `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD`
- **docker-compose**는 이 `.env`만 `env_file`로 참조하므로, 비밀/호스트 등은 다른 yml이나 설정 파일에 두지 말고 `.env`에만 둡니다.

---

(도메인은 예시로 `go-almond.ddnsfree.com`을 사용합니다. 실제 도메인이 다르면 아래 URL에서 해당 도메인으로 치환하세요.)

---

## 1. 서버에서 확인 (SSH 접속 후)

배포된 서버에 SSH로 접속한 뒤 아래 순서로 확인하세요.

### 컨테이너 상태

```bash
docker-compose ps
# 또는
docker ps
```

- **ga-matching-api**, (사용 시) **ga-nginx**가 **Up** 상태인지 확인합니다.

### API 로그

```bash
docker-compose logs -f ga-matching-api
```

- 기동 에러·예외가 없는지 확인합니다. `Ctrl+C`로 종료.

### API 직접 호출 (서버 로컬)

```bash
curl -s -o /dev/null -w "%{http_code}" "http://localhost:8080/api/v1/programs?type=community_college"
```

- **기대**: `200`이 출력됩니다.

본문 응답까지 보려면:

```bash
curl -s "http://localhost:8080/api/v1/programs?type=community_college" | head -20
```

### Healthcheck (Actuator)

ga-matching-api는 `/actuator/health`로 healthy 여부를 확인합니다.

```bash
curl -s http://localhost:8080/actuator/health
```

- **기대**: `{"status":"UP"}` 등 JSON 응답.

---

## 2. 외부에서 확인 (본인 PC·브라우저)

**전제**: 서버에서 ga-nginx가 떠 있고, 도메인(예: go-almond.ddnsfree.com)이 해당 서버를 가리킬 때.

### 브라우저

다음 URL을 브라우저 주소창에 입력합니다.

```
https://go-almond.ddnsfree.com/api/v1/programs?type=community_college
```

- **기대**: JSON 응답이 보이고, `success: true`, `data.programs` 배열이 포함됩니다.

### curl (로컬 터미널)

```bash
curl -s "https://go-almond.ddnsfree.com/api/v1/programs?type=community_college"
```

- **기대**: HTTP 200, JSON body.

### 다른 엔드포인트

- **POST** `https://go-almond.ddnsfree.com/api/v1/matching/run`  
  - Content-Type: `application/json`, body: `{"user_id":"550e8400-e29b-41d4-a716-446655440000"}`
- **GET** `https://go-almond.ddnsfree.com/api/v1/matching/result`
- **GET** `https://go-almond.ddnsfree.com/api/v1/schools/school-001`

상세 스펙은 [API 문서](api/README.md)를 참고하세요.

---

## 3. 확인 순서 요약

1. **서버**: `docker-compose ps` → 컨테이너 Up 확인
2. **서버**: `curl "http://localhost:8080/api/v1/programs?type=community_college"` → 200·JSON 확인
3. **외부**: 브라우저 또는 curl로 `https://도메인/api/v1/programs?type=community_college` 호출 → 200·JSON 확인

---

## 4. 문제 발생 시

| 상황 | 참고 문서 |
|------|------------|
| 서버에서 200이 안 나오거나 connection refused | [CONNECTION_REFUSED_FIX.md](nginx/CONNECTION_REFUSED_FIX.md) |
| 502 Bad Gateway (nginx는 동작하나 백엔드 연결 실패) | [502_TROUBLESHOOTING.md](nginx/502_TROUBLESHOOTING.md) |
| 외부에서만 실패 (서버 로컬은 성공) | nginx·네트워크·방화벽·DNS 점검. [DOCKER_NGINX_DEPLOY.md](nginx/DOCKER_NGINX_DEPLOY.md), [NETWORK_FIX.md](nginx/NETWORK_FIX.md) 참고 |

---

## 5. CI/CD 배포 직후 참고

- 워크플로는 **ga-matching-api** 이미지 pull·재기동과 함께 **docs/nginx** 설정을 서버로 복사한 뒤 **ga-nginx**를 재시작합니다. (`main` 브랜치 push 시 `src/**`, `docker-compose.yml`, `docs/nginx/**` 등 변경 시 배포가 실행됩니다.)
- **환경 정보**: CI/CD는 `.env`를 서버로 복사하지 않습니다. 서버에는 **배포 디렉터리에 `.env`가 이미 있어야** 합니다. 최초 1회 `env.example`을 복사해 `.env`를 만들고 값을 채운 뒤, 해당 위치에서 `docker-compose up -d`를 실행하세요.
- ga-nginx를 쓰는 환경이면 첫 배포 시 `docker-compose up -d`로 한 번 둘 다 기동해 두었는지 확인하세요.
- 배포 직후 healthcheck 통과까지 수십 초 걸릴 수 있으므로, 확인 전 **10~20초** 정도 대기하는 것을 권장합니다.
