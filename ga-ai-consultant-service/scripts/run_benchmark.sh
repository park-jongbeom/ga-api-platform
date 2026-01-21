#!/bin/bash

# =========================================================
# PostgreSQL 성능 벤치마크 자동화 스크립트
# =========================================================
# 작성일: 2026-01-21
# 사용법: ./run_benchmark.sh [local|staging|production]
# =========================================================

set -e

ENVIRONMENT=${1:-local}
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
REPORT_DIR="benchmark_reports"
REPORT_FILE="${REPORT_DIR}/benchmark_${ENVIRONMENT}_${TIMESTAMP}.txt"

mkdir -p ${REPORT_DIR}

echo "=================================================="
echo "PostgreSQL 성능 벤치마크"
echo "환경: ${ENVIRONMENT}"
echo "시작 시간: $(date)"
echo "=================================================="

# 환경별 DB 연결 정보
case ${ENVIRONMENT} in
    local)
        DB_HOST="localhost"
        DB_PORT="5432"
        DB_NAME="goalmond"
        DB_USER="postgres"
        DB_PASSWORD="postgres"
        ;;
    staging)
        DB_HOST=${STAGING_DB_HOST}
        DB_PORT=${STAGING_DB_PORT:-5432}
        DB_NAME=${STAGING_DB_NAME}
        DB_USER=${STAGING_DB_USER}
        DB_PASSWORD=${STAGING_DB_PASSWORD}
        ;;
    production)
        echo "⚠️  프로덕션 벤치마크는 신중하게 실행하세요!"
        read -p "계속하시겠습니까? (yes/no): " confirm
        if [ "$confirm" != "yes" ]; then
            echo "취소되었습니다."
            exit 0
        fi
        DB_HOST=${PROD_DB_HOST}
        DB_PORT=${PROD_DB_PORT:-5432}
        DB_NAME=${PROD_DB_NAME}
        DB_USER=${PROD_DB_USER}
        DB_PASSWORD=${PROD_DB_PASSWORD}
        ;;
    *)
        echo "❌ 잘못된 환경: ${ENVIRONMENT}"
        echo "사용법: ./run_benchmark.sh [local|staging|production]"
        exit 1
        ;;
esac

# DB 연결 테스트
echo "📡 DB 연결 테스트..."
export PGPASSWORD=${DB_PASSWORD}
psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} -c "SELECT version();" > /dev/null

if [ $? -eq 0 ]; then
    echo "✅ DB 연결 성공"
else
    echo "❌ DB 연결 실패"
    exit 1
fi

# =========================================================
# 벤치마크 실행
# =========================================================

echo ""
echo "🚀 벤치마크 실행 중..."

psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USER} -d ${DB_NAME} \
    -f benchmark_performance.sql \
    2>&1 | tee ${REPORT_FILE}

# =========================================================
# 결과 요약
# =========================================================

echo ""
echo "=================================================="
echo "벤치마크 완료"
echo "종료 시간: $(date)"
echo "리포트: ${REPORT_FILE}"
echo "=================================================="

# 주요 지표 추출
echo ""
echo "📊 주요 지표 요약:"

# 평균 실행 시간 추출
AVG_TIME=$(grep "평균 실행 시간" ${REPORT_FILE} | grep -oP '\d+\.\d+')
if [ -n "$AVG_TIME" ]; then
    echo "  - 벡터 검색 평균: ${AVG_TIME} ms"
    
    if (( $(echo "$AVG_TIME < 100" | bc -l) )); then
        echo "    ✅ 목표 달성 (< 100ms)"
    else
        echo "    ⚠️  목표 미달 (목표: < 100ms)"
    fi
fi

# 캐시 히트율 추출
CACHE_HIT=$(grep "cache_hit_ratio" ${REPORT_FILE} | tail -1 | grep -oP '\d+\.\d+')
if [ -n "$CACHE_HIT" ]; then
    echo "  - 캐시 히트율: ${CACHE_HIT}%"
    
    if (( $(echo "$CACHE_HIT > 95" | bc -l) )); then
        echo "    ✅ 목표 달성 (> 95%)"
    else
        echo "    ⚠️  목표 미달 (목표: > 95%)"
    fi
fi

# Dead tuple 비율 추출
DEAD_RATIO=$(grep "dead_tuple_ratio" ${REPORT_FILE} | tail -1 | grep -oP '\d+\.\d+')
if [ -n "$DEAD_RATIO" ]; then
    echo "  - Dead Tuple 비율: ${DEAD_RATIO}%"
    
    if (( $(echo "$DEAD_RATIO < 10" | bc -l) )); then
        echo "    ✅ 정상 (< 10%)"
    else
        echo "    ⚠️  VACUUM 필요 (> 10%)"
        echo ""
        echo "🔧 최적화 명령어:"
        echo "   VACUUM ANALYZE documents;"
    fi
fi

echo ""
echo "📝 전체 리포트는 ${REPORT_FILE}에서 확인하세요."
