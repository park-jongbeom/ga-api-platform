# Docker 포트 충돌 해결 방법

## 문제 상황

Docker 컨테이너가 호스트의 8081-8085 포트를 사용 중이어서 nginx가 같은 포트를 리스닝할 수 없습니다.

**docker-compose.yml 확인:**
- `ga-auth-service`: `"8081:8081"`
- `ga-user-service`: `"8082:8082"`
- `ga-audit-service`: `"8083:8083"`
- `ga-ai-consultant-service`: `"8084:8084"`
- `ga-matching-service`: `"8085:8085"`

## 해결 방법: nginx를 다른 포트로 리스닝

nginx는 9081-9085 포트를 리스닝하고, Docker 컨테이너는 8081-8085를 계속 사용합니다.

### 변경 사항

1. **nginx 설정**: `listen 8081` → `listen 9081` (모든 포트에 적용)
2. **Swagger UI URLs**: `:8081` → `:9081` (모든 포트에 적용)
3. **프록시 설정**: `proxy_pass http://127.0.0.1:8081` 유지 (Docker 컨테이너로 프록시)

### 아키텍처

```
외부 요청 → nginx (9081-9085, HTTPS) → Docker 컨테이너 (8081-8085, HTTP)
```

## 적용 방법

### 1. nginx 설정 파일 수정

`docs/nginx/go-almond.swagger.conf` 파일에서:
- 모든 `listen 8081` → `listen 9081`
- 모든 `listen 8082` → `listen 9082`
- 모든 `listen 8083` → `listen 9083`
- 모든 `listen 8084` → `listen 9084`
- 모든 `listen 8085` → `listen 9085`

**프록시 설정은 변경하지 않음:**
- `proxy_pass http://127.0.0.1:8081` 유지 (Docker 컨테이너로 프록시)

### 2. Swagger UI URLs 수정

`ga-auth-service/src/main/resources/application.yml` 파일에서:
- `:8081` → `:9081`
- `:8082` → `:9082`
- `:8083` → `:9083`
- `:8084` → `:9084`
- `:8085` → `:9085`

### 3. 방화벽 설정

서버 방화벽에서 9081-9085 포트를 열어야 합니다:

```bash
# AWS Security Group 또는 서버 방화벽 설정
# 포트 9081-9085, 프로토콜 TCP, 소스 0.0.0.0/0 (또는 특정 IP)
```

## 최종 구조

### 포트 매핑

| 서비스 | Docker 컨테이너 포트 | nginx 리스닝 포트 | 외부 접근 |
|--------|---------------------|-----------------|----------|
| Auth Service | 8081 | 9081 | `https://go-almond.ddnsfree.com:9081` |
| User Service | 8082 | 9082 | `https://go-almond.ddnsfree.com:9082` |
| Audit Service | 8083 | 9083 | `https://go-almond.ddnsfree.com:9083` |
| AI Consultant | 8084 | 9084 | `https://go-almond.ddnsfree.com:9084` |
| Matching Service | 8085 | 9085 | `https://go-almond.ddnsfree.com:9085` |

### 요청 흐름

1. 브라우저: `https://go-almond.ddnsfree.com:9081/v3/api-docs`
2. nginx (9081 포트, HTTPS): 요청 수신
3. nginx: `http://127.0.0.1:8081/v3/api-docs`로 프록시
4. Docker 컨테이너 (8081 포트): 응답 반환
5. nginx: CORS 헤더 추가 후 클라이언트에 응답

## 대안: Docker 포트 매핑 변경

만약 nginx를 8081-8085에서 리스닝하고 싶다면, Docker 포트 매핑을 변경할 수 있습니다:

**docker-compose.yml 수정:**
```yaml
ga-auth-service:
  ports:
    - "18081:8081"  # 호스트 18081 → 컨테이너 8081
```

그러면 nginx는 `proxy_pass http://127.0.0.1:18081`로 변경해야 합니다.

하지만 이 방법은 기존 설정을 많이 변경해야 하므로, nginx 포트를 변경하는 것이 더 간단합니다.
