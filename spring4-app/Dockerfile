FROM eclipse-temurin:25.0.4_7-jre-alpine@sha256:3137541deb3cac6626b5d9a4a2187bc0d6a34312f858bd2c67dd01e732e6b682

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --chown=app:app target/spring4-app-1.0-SNAPSHOT.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
