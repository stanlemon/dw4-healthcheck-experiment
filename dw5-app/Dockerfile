FROM eclipse-temurin:25.0.5.1_2-jre-alpine@sha256:69c2a26e9894016e274c880fa542956d005b3755f84db765145185e87405bd89

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --chown=app:app target/dw5-app-1.0-SNAPSHOT.jar app.jar
COPY --chown=app:app config.yml config.yml

USER app

EXPOSE 8097 8098

ENTRYPOINT ["java", "-jar", "app.jar", "server", "config.yml"]
