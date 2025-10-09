# Use Eclipse Temurin JRE 21 as base image
FROM eclipse-temurin:21.0.8_9-jre-alpine@sha256:990397e0495ac088ab6ee3d949a2e97b715a134d8b96c561c5d130b3786a489d

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/dw-test2-1.0-SNAPSHOT.jar app.jar
COPY config.yml config.yml

# Expose ports (application and admin)
EXPOSE 8097 8098

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8098/healthcheck || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar", "server", "config.yml"]
