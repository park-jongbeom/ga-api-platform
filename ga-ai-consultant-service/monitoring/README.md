# AI 상담 서비스 모니터링 가이드

## 개요

Prometheus, Grafana, Alertmanager를 사용한 포괄적인 모니터링 솔루션입니다.

**주요 기능**:
- 실시간 성능 메트릭 수집
- AI 상담 파이프라인 각 단계별 측정
- 보안 이벤트 모니터링
- 자동 알림 (Slack, 이메일)

---

## 빠른 시작

### 1. 로컬 환경 실행

```bash
# 모니터링 스택 시작
cd monitoring
docker-compose up -d

# 상태 확인
docker-compose ps

# 로그 확인
docker-compose logs -f
```

**접속 주소**:
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000 (admin/admin)
- Alertmanager: http://localhost:9093

### 2. 애플리케이션 설정

`application.yml`에 Actuator 활성화:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, metrics, info
  metrics:
    export:
      prometheus:
        enabled: true
    tags:
      application: ga-ai-consultant-service
      environment: ${ENVIRONMENT:local}
```

### 3. 대시보드 임포트

1. Grafana 접속 (http://localhost:3000)
2. 좌측 메뉴 → Dashboards → Import
3. `grafana-dashboard.json` 업로드
4. Prometheus 데이터소스 선택

---

## 메트릭 설명

### 애플리케이션 메트릭

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `consultant_chat_requests_total` | Counter | AI 상담 요청 총 건수 |
| `consultant_chat_processing_seconds` | Histogram | 전체 처리 시간 |
| `consultant_chat_errors_total` | Counter | 오류 발생 건수 |
| `rag_search_duration_seconds` | Timer | RAG 검색 실행 시간 |
| `rag_search_documents_found` | Gauge | 검색된 문서 수 |
| `masking_duration_seconds` | Timer | 마스킹 처리 시간 |
| `prompt_template_creation_seconds` | Timer | 프롬프트 생성 시간 |
| `prompt_injection_blocked_total` | Counter | 프롬프트 인젝션 차단 건수 |

### JVM 메트릭

| 메트릭 | 설명 |
|--------|------|
| `jvm_memory_used_bytes` | 힙 메모리 사용량 |
| `jvm_gc_pause_seconds` | GC 시간 |
| `jvm_threads_live` | 활성 스레드 수 |
| `process_cpu_usage` | CPU 사용률 |

### 데이터베이스 메트릭

| 메트릭 | 설명 |
|--------|------|
| `hikaricp_connections_active` | 활성 DB 연결 수 |
| `hikaricp_connections_idle` | 유휴 DB 연결 수 |
| `spring_data_repository_invocations_seconds` | Repository 메서드 실행 시간 |

---

## 알림 설정

### Slack 웹훅 설정

1. Slack Workspace 설정 → Incoming Webhooks
2. 웹훅 URL 생성
3. 환경변수 설정:

```bash
export SLACK_WEBHOOK_URL="https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
```

4. Alertmanager 재시작:

```bash
docker-compose restart alertmanager
```

### 이메일 알림 설정

```bash
# SMTP 자격증명 설정
export SMTP_USERNAME="your-email@gmail.com"
export SMTP_PASSWORD="your-app-password"

# Alertmanager 재시작
docker-compose restart alertmanager
```

### 알림 테스트

```bash
# 테스트 알림 발송
curl -X POST http://localhost:9093/api/v1/alerts -d '[{
  "labels": {
    "alertname": "TestAlert",
    "severity": "warning"
  },
  "annotations": {
    "summary": "테스트 알림입니다"
  }
}]'
```

---

## 대시보드 활용

### 주요 패널

**1. AI 상담 요청 처리량**
- 초당 요청 수 추이
- 피크 타임 식별

**2. 응답 시간 분포**
- p50, p95, p99 percentile
- 목표: p95 < 5초

**3. 오류율 모니터링**
- 전체 오류율 추이
- 알림 임계값: 5%

**4. RAG 검색 성능**
- 검색 시간 분포
- 검색된 문서 수 평균

**5. 보안 이벤트**
- 프롬프트 인젝션 차단 건수
- XSS, SQL Injection 차단

**6. 리소스 사용률**
- JVM 힙 메모리
- CPU 사용률
- DB 연결 풀

---

## 성능 목표 및 SLA

### 응답 시간 SLA

| Percentile | 목표 | 알림 임계값 |
|-----------|------|-----------|
| p50 | < 1.5초 | 3초 |
| p95 | < 3초 | 5초 |
| p99 | < 5초 | 10초 |

### 가용성 SLA

| 지표 | 목표 |
|------|------|
| Uptime | 99.9% |
| 오류율 | < 1% |
| Rate Limit 도달율 | < 5% |

### 검색 품질 SLA

| 지표 | 목표 |
|------|------|
| 평균 검색 문서 수 | 3-5개 |
| 검색 성공률 | > 98% |
| 검색 지연시간 | < 150ms |

---

## 문제 해결

### Prometheus가 메트릭을 수집하지 못함

```bash
# 1. Actuator 엔드포인트 확인
curl http://localhost:8080/actuator/prometheus

# 2. Prometheus 타겟 상태 확인
# http://localhost:9090/targets

# 3. 방화벽 규칙 확인
```

### Grafana 대시보드가 비어 있음

```bash
# 1. Prometheus 데이터소스 연결 확인
# Grafana → Configuration → Data Sources

# 2. 쿼리 테스트
# Explore → Prometheus 선택 → 메트릭 검색

# 3. 시간 범위 확인 (우측 상단)
```

### 알림이 전송되지 않음

```bash
# 1. Alertmanager 로그 확인
docker-compose logs alertmanager

# 2. Webhook URL 테스트
curl -X POST $SLACK_WEBHOOK_URL -d '{"text": "test"}'

# 3. Alert 규칙 검증
curl http://localhost:9090/api/v1/rules
```

---

## 모니터링 스택 유지보수

### 데이터 보존 정책

```bash
# Prometheus 데이터 보존 기간 (기본: 15일)
# prometheus.yml에서 설정
--storage.tsdb.retention.time=30d
```

### 백업

```bash
# Grafana 대시보드 백업
docker exec grafana grafana-cli admin export > backup.json

# Prometheus 데이터 백업
docker run --rm -v prometheus-data:/data \
  -v $(pwd):/backup \
  alpine tar czf /backup/prometheus-backup.tar.gz /data
```

### 업그레이드

```bash
# 이미지 업데이트
docker-compose pull

# 재시작
docker-compose down
docker-compose up -d
```

---

## 프로덕션 배포

### AWS Lightsail 환경

```bash
# 1. Lightsail 인스턴스에 Docker 설치
ssh lightsail
sudo yum install docker
sudo systemctl start docker

# 2. 모니터링 스택 배포
scp -r monitoring/ lightsail:/opt/monitoring/
ssh lightsail "cd /opt/monitoring && docker-compose up -d"

# 3. 방화벽 규칙 추가
# Lightsail 콘솔에서 포트 9090, 3000, 9093 열기
```

### 클라우드 메트릭 통합

```yaml
# CloudWatch 통합 (선택사항)
management:
  metrics:
    export:
      cloudwatch:
        enabled: true
        namespace: GoalAlmond/AI-Consultant
        batchSize: 20
```

---

## 모니터링 체크리스트

### 일일 확인
- [ ] 대시보드 주요 지표 확인
- [ ] 활성 알림 검토
- [ ] 오류 로그 분석

### 주간 확인
- [ ] 성능 추세 분석
- [ ] 용량 계획 검토
- [ ] Alert 규칙 튜닝

### 월간 확인
- [ ] SLA 달성률 리포트
- [ ] 비용 최적화 검토
- [ ] 모니터링 스택 업그레이드

---

## 참고 자료

- Prometheus 공식 문서: https://prometheus.io/docs/
- Grafana 공식 문서: https://grafana.com/docs/
- Spring Boot Actuator: https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html
- Micrometer Metrics: https://micrometer.io/docs/
