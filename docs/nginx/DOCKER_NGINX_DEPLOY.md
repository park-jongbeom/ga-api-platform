# Docker Nginx 전용 배포 가이드

`go-almond.swagger.conf`는 **Docker 컨테이너(ga-nginx) 전용** 설정입니다.  
upstream 호스트명(`ga-matching-api`)은 Docker 네트워크(ga-network) 안에서만 동작하므로, **호스트 nginx의 sites-enabled에 이 설정을 두면 안 됩니다.**

## "host not found in upstream" 발생 시

호스트에서 `sudo nginx -t` 실행 시 위 에러가 나면, 호스트 nginx가 이 설정을 읽고 있는 상태입니다. 아래 순서대로 적용하세요.

## 권장 조치 (Docker nginx만 사용)

### 1. 호스트 nginx에서 go-almond 제거

```bash
# go-almond 설정을 호스트 nginx가 로드하지 않도록 심볼릭 링크 삭제
sudo rm /etc/nginx/sites-enabled/go-almond

# 80/443/8081-8085 포트를 ga-nginx에 양보하기 위해 호스트 nginx 중지
sudo systemctl stop nginx

# (선택) 다른 사이트만 있다면 검증
sudo nginx -t
```

### 2. Docker 스택 기동

```bash
cd ~/ga-api-platform   # 또는 프로젝트 실제 경로
docker-compose up -d
```

ga-nginx가 80, 443을 리스닝하고, 같은 Docker 네트워크의 `ga-matching-api:8080`으로 프록시합니다 (경량화: 단일 API).

### 3. 동작 확인

- API: `https://go-almond.ddnsfree.com/api/v1/programs?type=community_college` 또는 `https://go-almond.ddnsfree.com/api/v1/matching/result`
- 로그: `docker logs ga-nginx`

### 4. API 미동작 시 (ga-nginx가 네트워크에 없는 경우)

`docker network inspect ga-api-platform_ga-network` 에 ga-nginx가 없으면 [NETWORK_FIX.md](NETWORK_FIX.md) 참고. 서버에서 `bash docs/nginx/connect-nginx-network.sh` 실행.

## 요약

- **이 설정 파일은 Docker ga-nginx 전용**이며, 호스트의 `/etc/nginx/sites-enabled/`에 두지 마세요.
- 호스트 nginx를 쓰지 않고 Docker nginx만 사용할 때 위 단계를 따르면 "host not found in upstream"이 해결됩니다.
