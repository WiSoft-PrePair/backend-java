# ================================
# Stage 1: Build
# ================================
FROM eclipse-temurin:21 AS builder

WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew build -x test --no-daemon

# ================================
# Stage 2: Runtime
# ================================
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble

WORKDIR /app

RUN apt-get update && apt-get install -y ffmpeg curl && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 7300

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl -sf http://localhost:7300/actuator/health

ENTRYPOINT ["java", "-jar", "app.jar"]