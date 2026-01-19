# Build stage
FROM gradle:8.5-jdk21-alpine AS build
WORKDIR /app

# Copy Gradle files
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy all module source code
COPY ga-common ./ga-common
COPY ga-grpc-interface ./ga-grpc-interface
COPY ga-audit-service ./ga-audit-service

# Build the application
RUN gradle :ga-audit-service:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the JAR from build stage
COPY --from=build /app/ga-audit-service/build/libs/*.jar app.jar

EXPOSE 8083 9091

ENTRYPOINT ["java", "-jar", "app.jar"]
