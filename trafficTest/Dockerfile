# 1단계: 이미지 빌드 시 jar도 함께 빌드한다.
#        로컬에서 bootJar를 다시 돌리지 않아 예전 jar가 실행되는 일을 막기 위해서다.
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /src
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN chmod +x gradlew && ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon -q bootJar

# 2단계: 실행 이미지. jcmd로 적용된 JVM 옵션을 확인할 수 있도록 JDK 이미지를 쓴다.
FROM eclipse-temurin:21-jdk-jammy
COPY --from=build /src/build/libs/*.jar /app.jar

EXPOSE 8080

# JVM 옵션은 java 실행기가 직접 읽는 JAVA_TOOL_OPTIONS 환경 변수로 넘긴다 (docker-compose.yml 참고).
# JAVA_OPTS는 java가 읽지 않으므로 이 ENTRYPOINT에서는 아무 효과가 없다.
ENTRYPOINT ["java", "-jar", "/app.jar"]
