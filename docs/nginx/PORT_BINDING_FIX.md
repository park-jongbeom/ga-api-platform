# nginx 포트 바인딩 오류 해결 가이드

## 문제 상황

nginx 에러 로그에서 다음 오류 발생:
```
bind() to 0.0.0.0:8081 failed (98: Address already in use)
bind() to 0.0.0.0:8082 failed (98: Address already in use)
...
```

브라우저에서 `ERR_CONNECTION_REFUSED` 오류 발생.

## 원인

8081-8085 포트가 이미 다른 프로세스에 의해 사용 중이거나, nginx가 제대로 시작되지 않았습니다.

## 해결 방법

### 1. 포트 사용 중인 프로세스 확인

```bash
# 8081-8085 포트를 사용하는 프로세스 확인
sudo lsof -i :8081
sudo lsof -i :8082
sudo lsof -i :8083
sudo lsof -i :8084
sudo lsof -i :8085

# 또는 netstat 사용
sudo netstat -tlnp | grep -E ":(8081|8082|8083|8084|8085)"
```

### 2. nginx 프로세스 확인 및 종료

```bash
# nginx 프로세스 확인
ps aux | grep nginx

# nginx 마스터 프로세스 확인
sudo systemctl status nginx

# nginx 완전 종료
sudo systemctl stop nginx

# 모든 nginx 프로세스 강제 종료 (필요시)
sudo pkill -9 nginx

# nginx 프로세스가 모두 종료되었는지 확인
ps aux | grep nginx
```

### 3. 포트를 사용하는 다른 프로세스 종료 (필요시)

만약 다른 프로세스가 포트를 사용하고 있다면:

```bash
# 프로세스 ID 확인 후 종료
sudo kill -9 <PID>

# 또는 포트를 사용하는 모든 프로세스 종료
sudo fuser -k 8081/tcp
sudo fuser -k 8082/tcp
sudo fuser -k 8083/tcp
sudo fuser -k 8084/tcp
sudo fuser -k 8085/tcp
```

**주의:** Spring Boot 서비스들이 8081-8085 포트를 사용하고 있을 수 있습니다. 이 경우 서비스는 계속 실행해야 하므로, nginx만 재시작하면 됩니다.

### 4. nginx 설정 검증

```bash
# 설정 파일 문법 검증
sudo nginx -t
```

**예상 결과:**
```
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### 5. nginx 재시작

```bash
# nginx 시작
sudo systemctl start nginx

# 또는 재시작
sudo systemctl restart nginx

# 상태 확인
sudo systemctl status nginx
```

### 6. 포트 리스닝 확인

```bash
# nginx가 포트를 리스닝하는지 확인
sudo netstat -tlnp | grep nginx
sudo ss -tlnp | grep nginx

# 또는
sudo lsof -i -P -n | grep nginx
```

**예상 결과:**
```
tcp  0  0  0.0.0.0:8081  LISTEN  <nginx-pid>/nginx
tcp  0  0  0.0.0.0:8082  LISTEN  <nginx-pid>/nginx
...
```

### 7. nginx 에러 로그 확인

```bash
# 실시간 에러 로그 확인
sudo tail -f /var/log/nginx/error.log
```

## 문제가 계속되는 경우

### 경우 1: Spring Boot 서비스가 포트를 사용 중

Spring Boot 서비스들이 8081-8085 포트를 직접 사용하고 있을 수 있습니다. 이 경우:

1. **옵션 A: Spring Boot 서비스 포트 변경**
   - 각 서비스의 `application.yml`에서 포트를 다른 값으로 변경 (예: 18081, 18082 등)
   - nginx는 8081-8085를 리스닝하고, 프록시는 변경된 포트로 전달

2. **옵션 B: nginx가 다른 포트 사용**
   - nginx 설정에서 8081-8085 대신 다른 포트 사용 (예: 9081-9085)
   - Swagger UI URLs도 새로운 포트로 변경

### 경우 2: 이전 nginx 프로세스가 남아있음

```bash
# 모든 nginx 프로세스 강제 종료
sudo pkill -9 nginx

# 잠시 대기
sleep 2

# nginx 재시작
sudo systemctl start nginx
```

### 경우 3: nginx 설정 파일 문제

```bash
# 설정 파일 문법 오류 확인
sudo nginx -t

# 설정 파일 내용 확인
sudo cat /etc/nginx/sites-available/go-almond | grep -A 5 "listen 808"
```

## 빠른 해결 스크립트

```bash
#!/bin/bash
# nginx 포트 바인딩 문제 해결

echo "1. nginx 중지..."
sudo systemctl stop nginx

echo "2. 모든 nginx 프로세스 종료..."
sudo pkill -9 nginx

echo "3. 포트 사용 확인..."
sudo netstat -tlnp | grep -E ":(8081|8082|8083|8084|8085)"

echo "4. nginx 설정 검증..."
sudo nginx -t

echo "5. nginx 시작..."
sudo systemctl start nginx

echo "6. nginx 상태 확인..."
sudo systemctl status nginx

echo "7. 포트 리스닝 확인..."
sudo netstat -tlnp | grep nginx
```

## 확인 체크리스트

- [ ] nginx가 실행 중인지 확인: `sudo systemctl status nginx`
- [ ] 포트를 사용하는 프로세스 확인: `sudo lsof -i :8081`
- [ ] nginx 설정 검증: `sudo nginx -t`
- [ ] nginx 재시작: `sudo systemctl restart nginx`
- [ ] 포트 리스닝 확인: `sudo netstat -tlnp | grep nginx`
- [ ] 브라우저에서 다시 테스트

## 예상 결과

문제 해결 후:
- ✅ nginx가 8081-8085 포트를 정상적으로 리스닝
- ✅ `sudo systemctl status nginx`에서 "active (running)" 상태
- ✅ 브라우저에서 `ERR_CONNECTION_REFUSED` 오류가 사라짐
- ✅ Swagger UI에서 API 문서가 정상적으로 로드됨
