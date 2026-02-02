# CORS 오류 진단 단계별 가이드

## 1. nginx 설정 검증 및 재시작

서버에서 다음 명령어를 실행하세요:

```bash
# 설정 파일 검증
sudo nginx -t

# nginx 재시작 (검증 통과 시)
sudo systemctl reload nginx

# 또는 완전 재시작 (필요시)
sudo systemctl restart nginx

# nginx 상태 확인
sudo systemctl status nginx
```

**예상 결과:**
- `nginx -t` 실행 시 "syntax is ok", "test is successful" 메시지 출력
- nginx가 정상적으로 재시작됨

## 2. 실제 응답 헤더 확인 (curl)

서버에서 다음 명령어로 CORS 헤더가 포함되어 있는지 확인:

```bash
# GET 요청으로 CORS 헤더 확인
curl -v -H "Origin: https://go-almond.ddnsfree.com" \
  https://go-almond.ddnsfree.com:8081/v3/api-docs 2>&1 | grep -i "access-control"

# OPTIONS preflight 요청 테스트
curl -X OPTIONS -v \
  -H "Origin: https://go-almond.ddnsfree.com" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Content-Type" \
  https://go-almond.ddnsfree.com:8081/v3/api-docs 2>&1 | grep -i "access-control"
```

**예상 결과:**
- `Access-Control-Allow-Origin: https://go-almond.ddnsfree.com` 헤더가 포함되어야 함
- `Access-Control-Allow-Methods`, `Access-Control-Allow-Headers` 헤더도 포함되어야 함
- OPTIONS 요청 시 `204 No Content` 응답

## 3. 브라우저 개발자 도구 확인

1. 브라우저에서 `https://go-almond.ddnsfree.com/swagger-ui/index.html` 접속
2. 개발자 도구(F12) 열기
3. **Network** 탭 열기
4. 페이지 새로고침
5. `/v3/api-docs` 요청 찾기
6. 요청 클릭하여 **Headers** 탭 확인:
   - **Request Headers**: `Origin: https://go-almond.ddnsfree.com` 확인
   - **Response Headers**: `Access-Control-Allow-Origin` 헤더 확인
7. OPTIONS 요청이 있는지 확인 (preflight 요청)

**예상 결과:**
- Response Headers에 `Access-Control-Allow-Origin: https://go-almond.ddnsfree.com` 포함
- CORS 오류가 사라짐
- API 문서가 정상적으로 로드됨

## 4. nginx 로그 확인

서버에서 다음 명령어로 로그 확인:

```bash
# nginx 에러 로그 확인 (실시간)
sudo tail -f /var/log/nginx/error.log

# nginx 액세스 로그 확인 (8081 포트)
sudo tail -f /var/log/nginx/access.log | grep "8081"

# 최근 에러 로그 확인
sudo tail -n 50 /var/log/nginx/error.log
```

**확인 사항:**
- CORS 관련 오류 메시지가 있는지 확인
- 8081 포트로 들어오는 요청이 정상적으로 처리되는지 확인

## 5. 문제 해결 체크리스트

만약 여전히 CORS 오류가 발생한다면:

- [ ] nginx 설정 파일이 올바르게 수정되었는지 확인
- [ ] nginx가 재시작되었는지 확인 (`sudo systemctl status nginx`)
- [ ] `if` 블록이 `location /` 내부에 있는지 확인
- [ ] curl로 실제 응답 헤더에 CORS 헤더가 포함되어 있는지 확인
- [ ] 브라우저 캐시를 지우고 다시 시도
- [ ] 다른 브라우저에서도 테스트
- [ ] nginx 에러 로그에 오류가 있는지 확인

## 6. 추가 확인 사항

### nginx 설정 파일 구조 확인

```bash
# 8081 포트 설정 확인
sudo cat /etc/nginx/sites-available/go-almond | grep -A 30 "listen 8081"

# location 블록 내부에 if 블록이 있는지 확인
sudo cat /etc/nginx/sites-available/go-almond | grep -A 20 "location / {" | head -30
```

**올바른 구조:**
```
server {
    listen 8081 ssl;
    ...
    location / {
        if ($request_method = 'OPTIONS') {
            ...
        }
        add_header 'Access-Control-Allow-Origin' ...;
        ...
    }
}
```

**잘못된 구조 (수정 필요):**
```
server {
    listen 8081 ssl;
    ...
    if ($request_method = 'OPTIONS') {  # ❌ server 레벨에 있으면 안됨
        ...
    }
    location / {
        ...
    }
}
```

## 7. 최종 확인

모든 단계를 완료한 후:

1. 브라우저에서 Swagger UI 접속
2. 각 서비스의 API 문서가 정상적으로 로드되는지 확인
3. 브라우저 콘솔에 CORS 오류가 없는지 확인
4. Network 탭에서 모든 요청이 성공(200 또는 204)하는지 확인
