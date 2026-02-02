# 서비스별 API 문서 로드 오류 진단 가이드

## 현재 문제 상황

1. **Auth 서비스**: `500 Internal Server Error` - `/v3/api-docs`
2. **매칭 서비스**: 무한 로딩
3. **다른 서비스들** (User, Audit, AI Consultant): `502 Bad Gateway`

## 진단 단계

### 1단계: 서비스 상태 확인

```bash
# 모든 컨테이너 상태 확인
docker ps -a

# 실행 중인 서비스만 확인
docker ps | grep -E "ga-auth-service|ga-user-service|ga-audit-service|ga-ai-consultant-service|ga-matching-service"

# 포트 리스닝 상태 확인
netstat -tlnp | grep -E "8081|8082|8083|8084|8085"
# 또는
ss -tlnp | grep -E "8081|8082|8083|8084|8085"
```

**예상 결과:**
- 모든 서비스가 `Up` 상태여야 함
- 각 포트(8081-8085)가 리스닝 중이어야 함

### 2단계: 서비스별 직접 접근 테스트

#### Auth Service (500 에러 확인)
```bash
# Health check
curl -v http://127.0.0.1:8081/actuator/health

# API docs (500 에러 확인)
curl -v http://127.0.0.1:8081/v3/api-docs
```

#### User Service (502 에러 확인)
```bash
curl -v http://127.0.0.1:8082/actuator/health
curl -v http://127.0.0.1:8082/v3/api-docs
```

#### Audit Service (502 에러 확인)
```bash
curl -v http://127.0.0.1:8083/actuator/health
curl -v http://127.0.0.1:8083/v3/api-docs
```

#### AI Consultant Service (502 에러 확인)
```bash
curl -v http://127.0.0.1:8084/actuator/health
curl -v http://127.0.0.1:8084/v3/api-docs
```

#### Matching Service (무한 로딩 확인)
```bash
curl -v http://127.0.0.1:8085/actuator/health
curl -v --max-time 30 http://127.0.0.1:8085/v3/api-docs
```

### 3단계: 서비스 로그 확인

```bash
# Auth Service 로그 (500 에러 원인 확인)
docker logs ga-auth-service --tail 100

# User Service 로그
docker logs ga-user-service --tail 100

# Audit Service 로그
docker logs ga-audit-service --tail 100

# AI Consultant Service 로그
docker logs ga-ai-consultant-service --tail 100

# Matching Service 로그 (무한 로딩 원인 확인)
docker logs ga-matching-service --tail 100
```

**확인 사항:**
- 스택 트레이스나 예외 메시지
- 데이터베이스 연결 오류
- 의존성 서비스 연결 오류
- 메모리 부족 또는 기타 리소스 문제

### 4단계: nginx 에러 로그 확인

```bash
# 최근 에러 로그 확인
sudo tail -n 200 /var/log/nginx/error.log | grep -E "v3/api-docs|502|500"

# 실시간 모니터링
sudo tail -f /var/log/nginx/error.log
```

## 빠른 진단 스크립트 실행

로컬에서 생성한 진단 스크립트를 서버에 업로드하고 실행:

```bash
# 스크립트에 실행 권한 부여
chmod +x diagnose-services.sh

# 실행
./diagnose-services.sh
```

## 문제별 해결 방안

### 500 Internal Server Error (Auth Service)

**가능한 원인:**
1. `SwaggerRequestFilter`가 Windows 경로를 사용하여 파일 쓰기 실패
2. SpringDoc 설정 오류
3. 서비스 내부 예외

**해결 방법:**
1. 서비스 재시작:
   ```bash
   docker restart ga-auth-service
   ```
2. 로그 확인하여 구체적인 에러 메시지 확인
3. 필요시 `SwaggerRequestFilter` 제거 또는 수정 (이미 비활성화됨)

### 502 Bad Gateway (User, Audit, AI Consultant Services)

**가능한 원인:**
1. 서비스가 실행되지 않음
2. 서비스가 시작 중이지만 아직 준비되지 않음
3. 포트 충돌

**해결 방법:**
1. 서비스 상태 확인:
   ```bash
   docker ps -a | grep ga-user-service
   ```
2. 서비스 재시작:
   ```bash
   docker restart ga-user-service
   docker restart ga-audit-service
   docker restart ga-ai-consultant-service
   ```
3. 모든 서비스 재시작:
   ```bash
   docker-compose restart
   ```

### 무한 로딩 (Matching Service)

**가능한 원인:**
1. 서비스가 응답하지만 매우 느림
2. 타임아웃이 더 길어야 함
3. 서비스가 데드락 상태

**해결 방법:**
1. 서비스 로그 확인
2. 서비스 재시작
3. 필요시 nginx 타임아웃 증가 (이미 60s로 설정됨)

## 전체 서비스 재시작

모든 서비스를 한 번에 재시작하려면:

```bash
cd /home/ubuntu/ga-api-platform  # 또는 실제 프로젝트 경로
docker-compose restart
```

## 검증

서비스 재시작 후:

1. 각 서비스의 health check 확인:
   ```bash
   curl http://127.0.0.1:8081/actuator/health
   curl http://127.0.0.1:8082/actuator/health
   curl http://127.0.0.1:8083/actuator/health
   curl http://127.0.0.1:8084/actuator/health
   curl http://127.0.0.1:8085/actuator/health
   ```

2. Swagger UI 접속 테스트:
   - 브라우저에서 `https://go-almond.ddnsfree.com/swagger-ui/index.html` 접속
   - 각 서비스의 API 정의가 정상적으로 로드되는지 확인

## 추가 참고사항

- nginx 타임아웃 설정은 이미 60초로 증가되어 있음
- `SwaggerRequestFilter`는 비활성화되어 있음 (재배포 필요)
- 서비스 재시작 후에도 문제가 지속되면 로그를 자세히 확인해야 함
