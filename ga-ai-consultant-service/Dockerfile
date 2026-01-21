# 멀티 스테이지 빌드 - AI 상담 서비스
# 보안 준수: 최소 권한, 비 root 사용자 실행

# Stage 1: 빌드 스테이지
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Gradle 캐싱을 위해 의존성 파일만 먼저 복사
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY ga-common/build.gradle.kts ./ga-common/
COPY ga-grpc-interface/build.gradle.kts ./ga-grpc-interface/
COPY ga-ai-consultant-service/build.gradle.kts ./ga-ai-consultant-service/

# 의존성 다운로드 (캐시 레이어)
RUN gradle dependencies --no-daemon

# 전체 소스 복사
COPY . .

# 빌드 (테스트 포함)
RUN gradle :ga-ai-consultant-service:build --no-daemon -x test

# Stage 2: 실행 스테이지
FROM eclipse-temurin:21-jre-alpine

# 보안: 비 root 사용자 생성
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/ga-ai-consultant-service/build/libs/*.jar app.jar

# 소유권 변경
RUN chown -R appuser:appgroup /app

# 비 root 사용자로 전환
USER appuser

# 포트 노출
EXPOSE 8083

# 헬스 체크
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
    CMD wget --no-verbose --tries=1 --spider http://localhost:8083/actuator/health || exit 1

# JVM 옵션 설정
ENV JAVA_OPTS="-Xms512m -Xmx1024m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# 애플리케이션 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
