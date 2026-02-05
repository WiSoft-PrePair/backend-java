# Stage 1: Build
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./

#의존성 다운로드 (캐싱)
RUN ./gradlew dependencies --configuration compileClasspath > /dev/null 2>&1

COPY src/ src/
RUN ./gradlew bootJar -x test

# Stage 2: Run
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 7300
ENTRYPOINT ["java", "-jar", "app.jar"]
