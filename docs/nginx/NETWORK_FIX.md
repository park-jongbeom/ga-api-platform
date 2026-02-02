# ga-nginx 네트워크 미연결 해결 (서버 실행 가이드)

`docker network inspect ga-api-platform_ga-network` 결과 **Containers**에 `ga-matching-api`만 있고 `ga-nginx`가 없으면, nginx가 `ga-matching-api:8080` 호스트명을 해석하지 못해 Swagger/API가 동작하지 않습니다. 아래 순서대로 서버에서 실행하세요.

---

## 1단계: ga-nginx 상태 확인

```bash
docker ps -a --filter name=ga-nginx
```

- **Up** 이면: 2단계로 이동 (네트워크 수동 연결).
- **Exited** 이면: 3단계로 이동 (로그 확인 후 원인 수정).

---

## 2단계: ga-nginx가 떠 있는 경우 — 같은 네트워크에 수동 연결

**방법 A: 스크립트 실행 (권장)**

```bash
cd ~/ga-api-platform
bash docs/nginx/connect-nginx-network.sh
```

**방법 B: 수동 명령**

```bash
docker network connect ga-api-platform_ga-network ga-nginx
```

이후 확인:

```bash
docker network inspect ga-api-platform_ga-network
```

`Containers`에 `ga-matching-api`와 `ga-nginx` 둘 다 있어야 합니다.  
이후 `https://go-almond.ddnsfree.com/api/v1/programs?type=community_college` 접속 테스트.

---

## 3단계: ga-nginx가 Exited 인 경우 — 로그로 원인 확인

```bash
docker logs ga-nginx
```

흔한 원인:

| 원인 | 조치 |
|------|------|
| 설정 파일 없음 | 서버에 `~/ga-api-platform/docs/nginx/go-almond.swagger.conf` 있는지 확인. 없으면 `git pull` 또는 해당 경로에 파일 복사. |
| SSL 경로 오류 | 호스트에 `/etc/letsencrypt` 존재 여부 확인. 없으면 인증서 배치 또는 테스트용으로 SSL 없이 nginx 설정 분리. |
| nginx 설정 문법 오류 | `docker logs ga-nginx` 출력의 에러 메시지에 따라 `go-almond.swagger.conf` 수정. |

원인 수정 후:

```bash
cd ~/ga-api-platform
docker-compose up -d
```

다시 `docker network inspect ga-api-platform_ga-network` 로 `ga-nginx`가 포함되는지 확인.

---

## 4단계: (선택) compose로 같은 네트워크 유지 확인

수동으로 `docker network connect`로 해결한 경우, 이후 `docker-compose up -d`만 해도 두 컨테이너가 같은 네트워크에 올라가야 합니다. 다음으로 한 번 검증해 두면 좋습니다.

```bash
cd ~/ga-api-platform
docker-compose down
docker-compose up -d
docker network inspect ga-api-platform_ga-network
```

`Containers`에 `ga-matching-api`와 `ga-nginx` 둘 다 있으면 정상입니다.

---

## 요약

| 상황 | 할 일 |
|------|--------|
| ga-nginx **Up** | `bash docs/nginx/connect-nginx-network.sh` 또는 `docker network connect ga-api-platform_ga-network ga-nginx` 후 Swagger/API 재테스트 |
| ga-nginx **Exited** | `docker logs ga-nginx`로 원인 확인 → 설정/볼륨 수정 후 `docker-compose up -d` |
