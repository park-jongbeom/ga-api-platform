# CORS 헤더 수정 적용 가이드

## 수정 내용

nginx 설정 파일에 `proxy_hide_header` 지시어를 추가하여 백엔드 서버에서 보낸 기존 CORS 헤더를 숨기고, nginx에서 새로 추가하도록 수정했습니다.

### 변경 사항

각 포트별 server 블록(8081-8085)의 `location /` 블록에 다음을 추가:

```nginx
# 백엔드의 기존 CORS 헤더 숨기기 (nginx에서 새로 추가하기 위해)
proxy_hide_header 'Access-Control-Allow-Origin';
proxy_hide_header 'Access-Control-Allow-Methods';
proxy_hide_header 'Access-Control-Allow-Headers';
proxy_hide_header 'Access-Control-Expose-Headers';
proxy_hide_header 'Access-Control-Allow-Credentials';
```

## 서버에 적용하는 방법

### 1. 로컬 파일을 서버로 업로드

```bash
# 로컬에서 실행
scp docs/nginx/go-almond.swagger.conf ubuntu@your-server:/tmp/go-almond.swagger.conf
```

### 2. 서버에서 설정 적용

```bash
# SSH로 서버 접속 후

# 백업 생성
sudo cp /etc/nginx/sites-available/go-almond /etc/nginx/sites-available/go-almond.backup.$(date +%Y%m%d_%H%M%S)

# 새 설정 파일 복사
sudo cp /tmp/go-almond.swagger.conf /etc/nginx/sites-available/go-almond

# 설정 파일 검증
sudo nginx -t
```

**예상 결과:**
```
nginx: the configuration file /etc/nginx/nginx.conf syntax is ok
nginx: configuration file /etc/nginx/nginx.conf test is successful
```

### 3. nginx 재시작

```bash
# nginx 재시작
sudo systemctl reload nginx

# 또는 완전 재시작 (필요시)
sudo systemctl restart nginx

# 상태 확인
sudo systemctl status nginx
```

## 테스트 방법

### 1. curl로 전체 응답 확인 (grep 없이)

```bash
# GET 요청 - 전체 응답 확인
curl -v -H "Origin: https://go-almond.ddnsfree.com" \
  https://go-almond.ddnsfree.com:8081/v3/api-docs 2>&1

# OPTIONS 요청 - 전체 응답 확인
curl -X OPTIONS -v \
  -H "Origin: https://go-almond.ddnsfree.com" \
  -H "Access-Control-Request-Method: GET" \
  https://go-almond.ddnsfree.com:8081/v3/api-docs 2>&1
```

**확인 사항:**
- `< Access-Control-Allow-Origin: https://go-almond.ddnsfree.com` 헤더가 포함되어야 함
- `< Access-Control-Allow-Methods: GET, POST, PUT, DELETE, OPTIONS` 헤더가 포함되어야 함
- OPTIONS 요청 시 `204 No Content` 응답

### 2. 브라우저에서 확인

1. 브라우저에서 `https://go-almond.ddnsfree.com/swagger-ui/index.html` 접속
2. 개발자 도구(F12) → Network 탭 열기
3. 페이지 새로고침
4. `/v3/api-docs` 요청 클릭
5. **Headers** 탭에서 **Response Headers** 확인:
   - `Access-Control-Allow-Origin: https://go-almond.ddnsfree.com` 확인
   - CORS 오류가 사라지고 API 문서가 정상적으로 로드되는지 확인

### 3. nginx 설정 확인

```bash
# 8081 포트 설정 확인
sudo cat /etc/nginx/sites-available/go-almond | grep -A 50 "listen 8081" | head -60

# proxy_hide_header가 포함되어 있는지 확인
sudo cat /etc/nginx/sites-available/go-almond | grep "proxy_hide_header"
```

## 문제 해결

### 문제 1: nginx 설정 검증 실패

**증상:** `sudo nginx -t` 실행 시 오류 발생

**해결:**
- 설정 파일 문법 오류 확인
- `if` 블록이 `location /` 내부에 있는지 확인
- 중괄호가 올바르게 닫혀있는지 확인

### 문제 2: 여전히 CORS 헤더가 없음

**증상:** 설정 적용 후에도 curl에서 헤더가 보이지 않음

**확인 사항:**
1. nginx가 재시작되었는지 확인: `sudo systemctl status nginx`
2. 전체 curl 응답 확인 (grep 없이)
3. nginx 에러 로그 확인: `sudo tail -f /var/log/nginx/error.log`
4. 백엔드 서비스가 실행 중인지 확인: `curl http://127.0.0.1:8081/v3/api-docs`

### 문제 3: 브라우저 캐시

**증상:** 설정은 올바르지만 브라우저에서 여전히 오류 발생

**해결:**
- 브라우저 캐시 지우기 (Ctrl+Shift+Delete)
- 하드 새로고침 (Ctrl+F5)
- 시크릿 모드에서 테스트

## 추가 확인 명령어

```bash
# nginx 버전 확인
nginx -v

# nginx 프로세스 확인
ps aux | grep nginx

# nginx 에러 로그 실시간 확인
sudo tail -f /var/log/nginx/error.log

# nginx 액세스 로그 확인
sudo tail -f /var/log/nginx/access.log | grep "8081"

# 백엔드 서비스 직접 테스트 (nginx 우회)
curl -v http://127.0.0.1:8081/v3/api-docs 2>&1 | head -50
```

## 예상 결과

수정 적용 후:

1. ✅ curl 테스트에서 CORS 헤더가 응답에 포함됨
2. ✅ 브라우저 개발자 도구에서 Response Headers에 CORS 헤더 확인
3. ✅ Swagger UI에서 API 문서가 정상적으로 로드됨
4. ✅ CORS 오류가 사라짐
