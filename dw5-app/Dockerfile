FROM eclipse-temurin:25.0.3_9-jre-alpine@sha256:c707c0d18cb9e8556380719f80d96a7529d0746fbb42143893949b98ed2f8943

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --chown=app:app target/dw5-app-1.0-SNAPSHOT.jar app.jar
COPY --chown=app:app config.yml config.yml

USER app

EXPOSE 8097 8098

ENTRYPOINT ["java", "-jar", "app.jar", "server", "config.yml"]
