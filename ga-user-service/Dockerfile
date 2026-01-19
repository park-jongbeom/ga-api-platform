# Build stage
FROM gradle:8.5-jdk21-jammy AS build
WORKDIR /app

# Copy Gradle files
COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy dependency modules first (build order matters)
COPY ga-common ./ga-common
COPY ga-grpc-interface ./ga-grpc-interface

# Build dependency modules first
RUN gradle :ga-common:build :ga-grpc-interface:build --no-daemon

# Copy service module
COPY ga-user-service ./ga-user-service

# Build the application
RUN gradle :ga-user-service:bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create non-root user
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the JAR from build stage
COPY --from=build /app/ga-user-service/build/libs/*.jar app.jar

EXPOSE 8082 9090

ENTRYPOINT ["java", "-jar", "app.jar"]
