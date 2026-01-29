# Connection refused 해결 (ga-matching-api)

`docker-compose restart ga-matching-api` 직후 `wget` 테스트에서 Connection refused가 나오는 경우의 진단 및 해결 방법입니다.

---

## 1단계: ga-matching-api 상태 및 로그 확인

**서버에서 실행:**

```bash
# 1) 컨테이너 상태 확인
docker ps -a --filter name=ga-matching-api

# 2) 최근 로그 확인 (기동 에러 여부)
docker logs --tail 50 ga-matching-api

# 3) healthcheck 상태 확인
docker inspect ga-matching-api --format '{{.State.Health.Status}}'
```

- **1)** 에서 Status가 **Restarting** 또는 **Exited** 이면 → 애플리케이션 기동 실패. 2단계로 이동.
- **2)** 에서 에러 메시지(예: `APPLICATION FAILED TO START`, `Port 8080 already in use`, DB 연결 실패 등)가 보이면 → 해당 원인 수정.
- **3)** 에서 **unhealthy** 이면 → healthcheck 실패. 3단계로 이동.

---

## 2단계: Spring Boot 기동 시간 대기 후 재시도

**1단계**에서 컨테이너가 **Up**이고 로그에 에러가 없으면, Spring Boot가 아직 기동 중일 수 있습니다.

```bash
# 30~60초 대기 후 재시도
sleep 30
docker exec ga-nginx wget -qO- --timeout=10 "http://ga-matching-api:8080/api/v1/programs?type=community_college" | head -3

# 또는 직접 포트 확인
docker exec ga-matching-api wget -qO- --timeout=5 "http://localhost:8080/api/v1/programs?type=community_college" | head -3
```

위가 성공하면 브라우저에서 API 접속.

---

## 3단계: healthcheck/actuator 설정 확인

**1단계 3)** 에서 **unhealthy** 이거나, healthcheck가 계속 실패하는 경우:

현재 `docker-compose.yml`의 healthcheck는 `/actuator/health`를 사용합니다. `application.yml`에 `show-actuator: false`가 있지만 이는 springdoc 설정이고, Spring Boot Actuator 자체는 별도 설정이 필요할 수 있습니다.

**확인:**

```bash
# actuator health 엔드포인트 직접 테스트
docker exec ga-matching-api wget -qO- http://localhost:8080/actuator/health 2>&1
```

- **404 Not Found** 이면 → actuator가 비활성화됨. healthcheck를 다른 엔드포인트로 변경하거나 actuator 활성화 필요.
- **200 OK** 이면 → healthcheck 명령어 문제일 수 있음 (wget 경로 등).

---

## 요약

| 상황 | 할 일 |
|------|--------|
| 컨테이너 Up, 로그에 에러 없음 | 30~60초 대기 후 `docker exec ga-nginx wget ...` 재시도 |
| 컨테이너 Exited/Restarting, 로그에 에러 | `docker logs ga-matching-api` 에러 메시지 확인 후 원인 수정 |
| healthcheck unhealthy, `/actuator/health` 404 | healthcheck를 `/api/v1/programs?type=community_college` 등 API 엔드포인트로 변경하거나 actuator 활성화 |

**1단계 2)** 의 `docker logs` 출력(특히 마지막 20~30줄)을 확인하면, 구체적인 에러 원인을 더 정확히 파악할 수 있습니다.
