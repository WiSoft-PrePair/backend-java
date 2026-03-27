# ================================
# Stage 1: Build
# ================================
FROM eclipse-temurin:21 AS builder

WORKDIR /app

# Gradle 캐싱을 위해 설정 파일 먼저 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .

# 의존성 다운로드 (캐싱 레이어)
RUN ./gradlew dependencies --no-daemon

# 소스코드 복사 후 빌드
COPY src src
RUN ./gradlew build -x test --no-daemon

# ================================
# Stage 2: Runtime
# ================================
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble

WORKDIR /app

RUN apt-get update && apt-get install -y ffmpeg && rm -rf /var/lib/apt/lists/*

COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 문서화
EXPOSE 7300

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
