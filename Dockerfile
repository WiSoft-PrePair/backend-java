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
FROM eclipse-temurin:21-jre

WORKDIR /app

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 포트 문서화
EXPOSE 7300

# 실행
ENTRYPOINT ["java", "-jar", "app.jar"]
