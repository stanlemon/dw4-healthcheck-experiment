FROM eclipse-temurin:25.0.5_8-jre-alpine@sha256:56f273059de6b13a6b49fe0ded691ed2d0399bbee76156a8f2d385f7524a3d43

RUN addgroup -S app && adduser -S app -G app

WORKDIR /app

COPY --chown=app:app target/spring4-app-1.0-SNAPSHOT.jar app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
