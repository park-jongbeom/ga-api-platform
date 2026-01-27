FROM gradle:8.5-jdk21-jammy AS build
WORKDIR /app

COPY settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY ga-matching-service ./ga-matching-service

RUN gradle :ga-matching-service:bootJar --no-daemon

FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=build /app/ga-matching-service/build/libs/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
