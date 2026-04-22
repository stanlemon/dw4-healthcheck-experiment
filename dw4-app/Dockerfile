FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY target/dw4-app-1.0-SNAPSHOT.jar app.jar
COPY config.yml config.yml

EXPOSE 8097 8098

ENTRYPOINT ["java", "-jar", "app.jar", "server", "config.yml"]
