# 502 Bad Gateway (Swagger) 해결 (서버 실행 가이드)

502 = nginx는 동작하지만 **백엔드(ga-matching-api:8080)로의 요청이 실패**한 상태입니다. 아래 순서대로 서버에서 확인·조치하세요.

---

## 1단계: 상태 확인 (진단)

```bash
# 1) 두 컨테이너 모두 Up 인지
docker ps -a --format "table {{.Names}}\t{{.Status}}" | grep -E "ga-nginx|ga-matching-api"

# 2) 같은 네트워크에 있는지 (ga-api-platform_ga-network 에 둘 다 나와야 함)
docker network inspect ga-api-platform_ga-network --format '{{range .Containers}}{{.Name}} {{end}}'

# 3) ga-nginx 안에서 백엔드 접속 테스트 (선택)
docker exec ga-nginx wget -qO- --timeout=5 http://ga-matching-api:8080/swagger-ui.html | head -5
```

- **2)** 에 ga-matching-api만 있고 ga-nginx가 없으면 → 네트워크 불일치. 2단계 A 적용.
- **1)** 에 ga-matching-api가 Exited 이거나 없으면 → 백엔드 미기동. 2단계 B 적용.
- **3)** 에 connection refused / timeout 이면 → 같은 네트워크여도 백엔드가 8080에서 안 받는 상태. 2단계 B 적용.

---

## 2단계 A: ga-nginx를 ga-matching-api와 같은 네트워크에 연결

**1)** 결과에 ga-nginx가 `ga-api-platform_ga-network`에 없을 때만 실행.

```bash
docker network connect ga-api-platform_ga-network ga-nginx
```

이후 `https://go-almond.ddnsfree.com/swagger-ui.html` 다시 접속.

---

## 2단계 B: ga-matching-api 기동 확인 및 재기동

ga-matching-api가 Exited 이거나 **3)** 에서 연결 실패한 경우:

```bash
cd ~/ga-api-platform
docker-compose restart ga-matching-api
# 10~20초 대기 후 (Spring Boot 기동 시간)
docker exec ga-nginx wget -qO- --timeout=10 http://ga-matching-api:8080/swagger-ui.html | head -3
```

위가 성공하면 브라우저에서 Swagger 다시 접속.

---

## 요약

| 확인 결과 | 할 일 |
|-----------|--------|
| ga-nginx가 네트워크에 없음 | `docker network connect ga-api-platform_ga-network ga-nginx` 후 Swagger 재접속 |
| ga-matching-api Exited/미기동 | `docker-compose restart ga-matching-api` 후 10~20초 대기, 다시 접속 |
| 둘 다 같은 네트워크·Up인데 502 | `docker exec ga-nginx wget ... http://ga-matching-api:8080/swagger-ui.html` 로 백엔드 직접 확인. 실패 시 ga-matching-api 로그: `docker logs ga-matching-api` |

---

## 참고: healthcheck 적용 시

`docker-compose.yml`에 ga-matching-api healthcheck와 ga-nginx `depends_on: condition: service_healthy` 가 적용되어 있으면, nginx는 백엔드가 준비된 뒤에 기동합니다. 새로 배포한 이미지로 `docker-compose up -d` 한 경우 healthcheck 통과까지 ga-nginx 기동이 지연될 수 있습니다.
