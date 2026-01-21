#!/bin/bash

# =========================================================
# 모니터링 스택 시작 스크립트
# =========================================================
# 사용법: ./start_monitoring.sh [start|stop|restart|status]
# =========================================================

set -e

COMMAND=${1:-start}

case ${COMMAND} in
    start)
        echo "🚀 모니터링 스택 시작..."
        
        # 환경변수 확인
        if [ -z "$SLACK_WEBHOOK_URL" ]; then
            echo "⚠️  SLACK_WEBHOOK_URL이 설정되지 않았습니다."
            echo "   Slack 알림이 비활성화됩니다."
        fi
        
        # Docker Compose 시작
        docker-compose up -d
        
        echo ""
        echo "✅ 모니터링 스택 시작 완료!"
        echo ""
        echo "📊 접속 주소:"
        echo "   - Prometheus: http://localhost:9090"
        echo "   - Grafana:    http://localhost:3000 (admin/admin)"
        echo "   - Alertmanager: http://localhost:9093"
        echo ""
        echo "🔍 상태 확인:"
        echo "   docker-compose ps"
        echo ""
        echo "📜 로그 확인:"
        echo "   docker-compose logs -f [service-name]"
        ;;
    
    stop)
        echo "🛑 모니터링 스택 중지..."
        docker-compose down
        echo "✅ 중지 완료"
        ;;
    
    restart)
        echo "🔄 모니터링 스택 재시작..."
        docker-compose restart
        echo "✅ 재시작 완료"
        ;;
    
    status)
        echo "📊 모니터링 스택 상태:"
        docker-compose ps
        echo ""
        echo "🔍 컨테이너 리소스:"
        docker stats --no-stream
        ;;
    
    logs)
        SERVICE=${2:-}
        if [ -z "$SERVICE" ]; then
            docker-compose logs --tail=50 -f
        else
            docker-compose logs --tail=50 -f $SERVICE
        fi
        ;;
    
    health)
        echo "🏥 헬스 체크..."
        
        # Prometheus
        echo -n "Prometheus: "
        if curl -s http://localhost:9090/-/healthy > /dev/null; then
            echo "✅ 정상"
        else
            echo "❌ 비정상"
        fi
        
        # Grafana
        echo -n "Grafana: "
        if curl -s http://localhost:3000/api/health | grep -q "ok"; then
            echo "✅ 정상"
        else
            echo "❌ 비정상"
        fi
        
        # Alertmanager
        echo -n "Alertmanager: "
        if curl -s http://localhost:9093/-/healthy > /dev/null; then
            echo "✅ 정상"
        else
            echo "❌ 비정상"
        fi
        
        # Application
        echo -n "AI Consultant Service: "
        if curl -s http://localhost:8080/actuator/health | grep -q "UP"; then
            echo "✅ 정상"
        else
            echo "❌ 비정상"
        fi
        ;;
    
    backup)
        BACKUP_DIR="backups/$(date +%Y%m%d_%H%M%S)"
        mkdir -p $BACKUP_DIR
        
        echo "💾 모니터링 데이터 백업..."
        
        # Grafana 대시보드 백업
        docker exec grafana grafana-cli admin export > ${BACKUP_DIR}/grafana-dashboards.json
        
        # Prometheus 데이터 백업
        docker run --rm \
            -v monitoring_prometheus-data:/data \
            -v $(pwd)/${BACKUP_DIR}:/backup \
            alpine tar czf /backup/prometheus-data.tar.gz /data
        
        echo "✅ 백업 완료: ${BACKUP_DIR}"
        ;;
    
    *)
        echo "사용법: $0 {start|stop|restart|status|logs|health|backup}"
        exit 1
        ;;
esac
