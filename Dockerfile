# Use Eclipse Temurin JRE 21 as base image
FROM eclipse-temurin:25-jre-alpine@sha256:bf9c91071c4f90afebb31d735f111735975d6fe2b668a82339f8204202203621

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
