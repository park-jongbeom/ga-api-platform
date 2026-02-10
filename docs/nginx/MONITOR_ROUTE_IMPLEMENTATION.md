# /monitor/ 경로 운영 배포 구현 요약

## 수행한 작업

### 1. nginx 설정 백업
- 백업 파일: `/tmp/nginx_backup_20260210_192420.conf`
- 실행 중이던 ga-nginx의 `default.conf` 내용 보관

### 2. ga-nginx 재시작 및 swagger.conf 적용
- `docker compose restart ga-nginx`: 재시작만으로는 볼륨 소스가 바뀌지 않음 (기존 컨테이너는 local.conf 마운트 상태 유지)
- `docker compose up -d --force-recreate ga-nginx`: ga-nginx만 재생성 시도 시 ga-matching-api도 함께 재생성되며 8080 포트 충돌로 실패
- ga-nginx를 중지 후 제거하고 `docker compose up -d ga-nginx`로 새 컨테이너 생성 시 **볼륨이 go-almond.swagger.conf로 적용됨** (docker-compose.yml 명시대로)

### 3. 마운트 확인
- 재생성된 ga-nginx 컨테이너의 마운트 확인:
  - **Source**: `/media/ubuntu/data120g/ga-api-platform/docs/nginx/go-almond.swagger.conf`
  - **Destination**: `/etc/nginx/conf.d/default.conf`
- `go-almond.swagger.conf`에는 `/monitor/` location 블록이 포함되어 있음 (college-crawler-monitor:8080으로 프록시)

### 4. 접근 테스트
- **실제 HTTPS** (`https://go-almond.ddnsfree.com/monitor/api/health`): HTTP 200, 단 Content-Type이 `text/html`(1877 bytes)로 응답됨 → 현재 해당 도메인 트래픽이 이 서버의 ga-nginx가 아닌 **다른 nginx/프록시**(예: 호스트 nginx)에서 처리되고 있어, React 앱이 반환되는 것으로 보임
- **로컬** (`http://localhost/monitor/`): ga-nginx를 swagger.conf로 기동하려 할 때 **Let's Encrypt 파일 부재**로 nginx 기동 실패

## 환경별 정리

### 현재 머신 (개발/테스트)
- `/etc/letsencrypt/` 디렉터리는 존재하나 `options-ssl-nginx.conf`, `ssl-dhparams.pem` 및 도메인 인증서가 없음
- `go-almond.swagger.conf`는 다음을 참조:
  - `include /etc/letsencrypt/options-ssl-nginx.conf;`
  - `ssl_dhparam /etc/letsencrypt/ssl-dhparams.pem;`
  - `ssl_certificate` / `ssl_certificate_key` (live/go-almond.ddnsfree.com)
- 따라서 이 환경에서는 **swagger.conf로 nginx 기동 불가** (emerg 오류)
- 로컬용 `go-almond.local.conf`(SSL 없음, 80만 사용)에는 이미 `/monitor/` 블록이 있으므로, 이 환경에서는 local.conf로 기동 시 `/monitor/` 동작 가능

### 운영 서버 (실제 go-almond.ddnsfree.com 서빙)
- **적용 방법**: 해당 서버에서 `docker compose up -d`(또는 `docker compose up -d --force-recreate ga-nginx`)로 ga-nginx를 기동하면 **docker-compose.yml에 따라 go-almond.swagger.conf가 마운트**됨
- **필수 조건**: 운영 서버에 Let's Encrypt 발급 완료 상태로 다음이 있어야 함:
  - `/etc/letsencrypt/options-ssl-nginx.conf`
  - `/etc/letsencrypt/ssl-dhparams.pem`
  - `/etc/letsencrypt/live/go-almond.ddnsfree.com/fullchain.pem`, `privkey.pem`
- **네트워크**: ga-nginx와 college-crawler-monitor가 같은 Docker 네트워크(ga-api-platform_ga-network)에 있어야 함. college-crawler-monitor는 별도 프로젝트이므로 `docker network connect ga-api-platform_ga-network college-crawler-monitor` 로 연결 필요
- 위 조건이 갖춰진 상태에서 nginx가 swagger.conf로 기동되면, `https://go-almond.ddnsfree.com/monitor/` 는 college-crawler-monitor 대시보드로 프록시됨

## 롤백
- 백업에서 복구: `docker cp /tmp/nginx_backup_20260210_192420.conf ga-nginx:/etc/nginx/conf.d/default.conf` 후 `docker exec ga-nginx nginx -s reload`
- 또는 docker-compose에서 ga-nginx만 재시작하여 현재 compose에 정의된 볼륨 설정 유지

## 결론
- **설정 측면**: `go-almond.swagger.conf`에 `/monitor/` 분기 추가 완료, docker-compose.yml은 swagger.conf 마운트로 일치
- **운영 반영**: 실제 `https://go-almond.ddnsfree.com` 트래픽을 받는 서버에서 ga-nginx를 docker compose로 기동하고, Let's Encrypt 및 college-crawler-monitor 네트워크 연결이 되어 있으면 `/monitor/` 접근 가능
- **현재 머신**: Let's Encrypt 미설치로 swagger.conf 기동 불가; 로컬 검증은 `go-almond.local.conf` 사용 시에만 가능
