# 500 에러 진단 가이드

## 문제 상황

Swagger UI에서 `https://go-almond.ddnsfree.com:9081/v3/api-docs` 요청 시 500 에러 발생

## 확인된 사항

- SpringDoc 버전: 모든 서비스가 2.6.0으로 통일됨
- SecurityConfig: Auth Service와 AI Consultant Service에서 `/v3/api-docs/**`가 `permitAll()`에 포함됨
- nginx 설정: 정상 (9081 포트로 리스닝, 8081로 프록시)

## 진단 단계

### 1. Docker 컨테이너 로그 확인 (가장 중요)

**목적:** 백엔드 서비스의 실제 오류 메시지 확인

```bash
# Auth Service 로그 확인
docker logs ga-auth-service --tail=200

# 또는 docker-compose 사용
cd /home/ubuntu/ga-api-platform
docker-compose logs --tail=200 ga-auth-service

# 실시간 로그 확인 (요청 시)
docker logs -f ga-auth-service
```

**확인할 오류:**
- `NoSuchMethodError` 또는 `ClassNotFoundException` (SpringDoc 버전 문제)
- `403 Forbidden` (Security 설정 문제)
- 데이터베이스 연결 오류
- JPA 스키마 검증 오류
- 스택 트레이스 전체 내용

### 2. 백엔드 서비스 직접 접근 테스트

**목적:** nginx를 거치지 않고 백엔드가 직접 응답하는지 확인

```bash
# 로컬에서 직접 접근 (nginx 우회)
curl -v http://127.0.0.1:8081/v3/api-docs 2>&1

# 헬스 체크 확인
curl http://127.0.0.1:8081/actuator/health

# 다른 서비스들도 확인
curl http://127.0.0.1:8082/actuator/health
curl http://127.0.0.1:8083/actuator/health
curl http://127.0.0.1:8084/actuator/health
curl http://127.0.0.1:8085/actuator/health
```

**예상 결과:**
- 직접 접근에서도 500 에러 발생 → 백엔드 문제
- 직접 접근은 정상, nginx를 통해서만 500 → nginx 설정 문제

### 3. 컨테이너 상태 확인

```bash
# 모든 컨테이너 상태
docker-compose ps

# 컨테이너가 실행 중인지 확인
docker ps | grep ga-auth-service

# 컨테이너 리소스 사용량
docker stats --no-stream ga-auth-service
```

### 4. nginx 에러 로그 확인

```bash
# nginx 에러 로그 확인
sudo tail -f /var/log/nginx/error.log

# 최근 에러 로그
sudo tail -n 100 /var/log/nginx/error.log | grep -i "9081\|8081"
```

### 5. nginx 액세스 로그 확인

```bash
# 9081 포트로 들어오는 요청 확인
sudo tail -f /var/log/nginx/access.log | grep "9081"

# 최근 요청 확인
sudo tail -n 50 /var/log/nginx/access.log | grep "9081"
```

## 예상되는 문제 및 해결책

### 문제 1: SpringDoc 초기화 실패

**증상:** 로그에 `NoSuchMethodError`, `ClassNotFoundException`, 또는 SpringDoc 관련 오류

**해결:**
- 이미 SpringDoc 버전이 2.6.0으로 통일되어 있음
- 컨테이너를 재빌드해야 할 수 있음:
  ```bash
  docker-compose down
  docker-compose pull
  docker-compose up -d
  ```

### 문제 2: Security 설정 문제

**증상:** 로그에 403 Forbidden 또는 Security 관련 오류

**현재 상태:**
- Auth Service: `/v3/api-docs/**`가 `permitAll()`에 포함됨
- AI Consultant Service: `/v3/api-docs/**`가 `permitAll()`에 포함됨
- User Service, Audit Service, Matching Service: Security 설정이 없거나 다른 방식

**해결:**
- User Service, Audit Service, Matching Service에 Security가 있다면 확인 필요
- Security가 없다면 문제가 아님

### 문제 3: 데이터베이스 연결 오류

**증상:** 로그에 데이터베이스 연결 실패 메시지

**확인:**
```bash
# 데이터베이스 컨테이너 확인
docker ps | grep postgres

# 데이터베이스 연결 테스트
docker exec -it ga-auth-service sh
# 컨테이너 내부에서
curl http://127.0.0.1:8081/actuator/health
```

### 문제 4: 애플리케이션 시작 실패

**증상:** 컨테이너는 실행 중이지만 애플리케이션이 완전히 시작되지 않음

**확인:**
```bash
# 컨테이너 로그에서 시작 메시지 확인
docker logs ga-auth-service | grep -i "started\|error\|exception"

# 애플리케이션이 완전히 시작되었는지 확인
docker logs ga-auth-service | tail -50
```

### 문제 5: 환경변수 문제

**증상:** 로그에 환경변수 관련 오류

**확인:**
```bash
# 환경변수 확인
docker exec ga-auth-service env | grep -E "DB_|REDIS_|JWT_"

# docker-compose.yml의 환경변수 확인
cat docker-compose.yml | grep -A 20 "ga-auth-service"
```

## 빠른 진단 스크립트

```bash
#!/bin/bash
# quick-diagnosis.sh

echo "=== Docker 컨테이너 상태 ==="
docker-compose ps

echo ""
echo "=== Auth Service 로그 (최근 50줄) ==="
docker logs ga-auth-service --tail=50

echo ""
echo "=== 직접 접근 테스트 ==="
echo "Health check:"
curl -s http://127.0.0.1:8081/actuator/health | head -5

echo ""
echo "API docs (직접 접근):"
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" http://127.0.0.1:8081/v3/api-docs

echo ""
echo "=== nginx를 통한 접근 테스트 ==="
curl -s -o /dev/null -w "HTTP Status: %{http_code}\n" https://go-almond.ddnsfree.com:9081/v3/api-docs
```

## 다음 단계

1. **Docker 로그 확인** - 가장 중요! 실제 오류 메시지를 찾아야 함
2. 로그에서 발견한 오류에 따라 적절한 수정 적용
3. 수정 후 컨테이너 재시작
4. 다시 테스트

## 로그에서 찾아야 할 키워드

- `Exception`
- `Error`
- `NoSuchMethodError`
- `ClassNotFoundException`
- `403`
- `500`
- `SpringDoc`
- `Security`
- `Database`
- `Connection`

로그를 확인한 후, 발견한 오류 메시지를 공유해주시면 정확한 해결책을 제시할 수 있습니다.
