FROM eclipse-temurin:25.0.5_8-jre-alpine@sha256:e1898fd6a11fc965a994d8ace186fd3585aeb636cf8f05f47a723fcfee0b0918

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --chown=app:app target/dw5-app-1.0-SNAPSHOT.jar app.jar
COPY --chown=app:app config.yml config.yml

USER app

EXPOSE 8097 8098

ENTRYPOINT ["java", "-jar", "app.jar", "server", "config.yml"]
