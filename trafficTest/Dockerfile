FROM eclipse-temurin:21-jdk-jammy

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8080

# 5. 컨테이너가 시작될 때 실행할 명령어 (JVM 옵션은 여기서 비워둡니다)
#    (JAVA_OPTS는 docker-compose에서 주입할 예정입니다)
ENTRYPOINT ["java", "-jar", "/app.jar"]