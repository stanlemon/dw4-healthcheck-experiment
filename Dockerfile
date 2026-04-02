# Use Eclipse Temurin JRE 21 as base image
FROM eclipse-temurin:25.0.2_10-jre-alpine@sha256:f10d6259d0798c1e12179b6bf3b63cea0d6843f7b09c9f9c9c422c50e44379ec

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
