FROM eclipse-temurin:21.0.10_7-jre-alpine@sha256:ad0cdd9782db550ca7dde6939a16fd850d04e683d37d3cff79d84a5848ba6a5a

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --chown=app:app target/dw5-app-1.0-SNAPSHOT.jar app.jar
COPY --chown=app:app config.yml config.yml

USER app

EXPOSE 8097 8098

ENTRYPOINT ["java", "-jar", "app.jar", "server", "config.yml"]
