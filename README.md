# Multi-App Metrics Project

A multi-module project demonstrating real-time metrics collection and health monitoring in both Dropwizard 4 and Spring Boot 3 applications.

## Project Structure

This is a multi-module Maven project:
- **architecture-rules**: Shared ArchUnit rules for enforcing architectural constraints and coding standards
- **healthy-metrics**: Reusable metrics collection library used by both applications
- **dw4-app**: Dropwizard 4.x application implementation
- **spring3-app**: Spring Boot 3.x application implementation

## Features

- **Framework Agnostic Metrics**: Same metrics library works in both Dropwizard and Spring Boot
- **Real-time Metrics**: Tracks error rates and response latency across all endpoints
- **Dual-Threshold Health Monitoring**: Health checks based on both error counts and average latency
- **Automatic Instrumentation**: Latency tracking via filter middleware, error tracking via global exception handling
- **REST API**: Consistent endpoints across both implementations
- **Comprehensive Testing**: High coverage and architecture enforcement

## Requirements

- JDK 21
- Maven 3.6.3+

## Quick Start

### Building the Project

```bash
mvn clean install -DskipTests -Dspotbugs.skip=true
```

### Running the Dropwizard Application

```bash
# Using Maven exec plugin
mvn exec:java -pl dw4-app

# Or using the JAR directly
java -jar dw4-app/target/dw4-app-1.0-SNAPSHOT.jar server dw4-app/config.yml
```

The Dropwizard application will start on:
- **Application Port**: [http://localhost:8097](http://localhost:8097)
- **Admin Port**: [http://localhost:8098](http://localhost:8098)

### Running the Spring Boot Application

```bash
# Using Spring Boot Maven plugin
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081" -pl spring3-app

# Or using the JAR directly
java -jar spring3-app/target/spring3-app-1.0-SNAPSHOT.jar --server.port=8081
```

The Spring Boot application will start on:
- **Application Port**: [http://localhost:8080](http://localhost:8080)
- **Actuator Endpoints**: [http://localhost:8080/actuator](http://localhost:8080/actuator)

## Development Commands

### Testing

```bash
# Run tests for all modules
mvn test

# Run tests for a specific module
mvn test -pl spring3-app
mvn test -pl dw4-app

# Run a specific test class
mvn test -Dtest=MetricsResourceTest -pl spring3-app

# Run architecture tests only
mvn test -Dtest="*Architecture*" -pl spring3-app
mvn test -Dtest="com.stanlemon.architecture.*" -pl dw4-app

# Run tests with coverage reports
mvn clean test jacoco:report
```

### Code Quality

```bash
# Format all code using Spotless
mvn spotless:apply

# Check formatting without modifying files
mvn spotless:check

# Run Spotless on a specific module
mvn spotless:apply -pl spring3-app

# Run SpotBugs to identify potential bugs
mvn clean compile spotbugs:check

# Run SpotBugs on a specific module
mvn clean compile spotbugs:check -pl spring3-app

# Run all quality checks
./scripts/run-quality-checks.sh

# Skip SonarQube analysis
./scripts/run-quality-checks.sh --skip-sonar

# Auto-fix formatting issues
./scripts/run-quality-checks.sh --fix-formatting
```

## Endpoints

Both applications provide equivalent functionality with similar endpoints:

### Dropwizard Endpoints (Port 8097)

- Hello World: [http://localhost:8097/hello](http://localhost:8097/hello) - Returns a simple JSON message
- Error Test: [http://localhost:8097/error-trigger](http://localhost:8097/error-trigger) - Deliberately throws a 500 error
- Metrics: [http://localhost:8097/metrics](http://localhost:8097/metrics) - Shows error counts, latency statistics, and threshold status
- Health Check: [http://localhost:8098/healthcheck](http://localhost:8098/healthcheck) - Returns health status based on error and latency thresholds
- Test Errors:
  - [http://localhost:8097/test-errors/runtime/random-message](http://localhost:8097/test-errors/runtime/random-message) - Test a run time error with a random message
  - [http://localhost:8097/test-errors/web-app/1234](http://localhost:8097/test-errors/web-app/429) - Test a web application error with a random code
- Slow Requests: [http://localhost:8097/slow](http://localhost:8097/slow) - Introduces artificial delays to test latency thresholds

### Spring Boot Endpoints (Port 8081)

- Hello World: [http://localhost:8080/hello](http://localhost:8080/hello) - Returns a simple JSON message
- Error Test: [http://localhost:8080/error-trigger](http://localhost:8080/error-trigger) - Deliberately throws a 500 error
- Metrics: [http://localhost:8080/metrics](http://localhost:8080/metrics) - Shows error counts, latency statistics, and threshold status
- Health Check: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health) - Returns health status based on error and latency thresholds
- Test Errors:
  - [http://localhost:8080/test-errors/runtime/message](http://localhost:8080/test-errors/runtime/message) - Test a runtime exception with a custom message
  - [http://localhost:8080/test-errors/web-app/404](http://localhost:8080/test-errors/web-app/404) - Test a web application exception with a specific status code
- Slow Requests:
  - [http://localhost:8080/slow](http://localhost:8080/slow) - GET endpoint with default 1-second delay
  - [http://localhost:8080/slow/500](http://localhost:8080/slow/500) - GET endpoint with custom delay in milliseconds

## Metrics and Monitoring

Both applications use the same metrics library with these features:

### Error Monitoring

- Tracks 5xx errors in a 1-minute sliding window using circular buffers
- Default threshold: 100 errors per minute
- Automatic recording via global exception handling

### Latency Monitoring

- Tracks request latency for all HTTP requests in a 60-second sliding window
- Default threshold: 100ms average latency
- Automatic recording via request/response filter middleware

### Metrics Endpoint Response

The `/metrics` endpoint returns the same data format in both applications:

```json
{
  "errorsLastMinute": 5,
  "totalErrors": 25,
  "avgLatencyLast60Seconds": 245.5,
  "errorThresholdBreached": false,
  "latencyThresholdBreached": false,
  "healthy": true
}
```

### Health Check

The health check monitors both error rates and response latency:

- **Error Threshold**: More than 100 errors in the last minute
- **Latency Threshold**: Average latency exceeding 100ms over the last 60 seconds
- **Health Status**:
  - `Healthy`: Both thresholds within limits
  - `Unhealthy`: One or both thresholds exceeded

## Architecture

The project demonstrates the same metrics collection with different frameworks:

- **Shared Metrics Library**: Framework-agnostic library used by both applications
- **Error Tracking**: Global exception handling captures all errors
- **Latency Tracking**: Filters measure all request response times
- **Metrics Storage**: Circular buffer implementation with sliding time windows
- **Health Monitoring**: Configurable thresholds for both errors and latency

For detailed architecture and implementation patterns, see [CLAUDE.md](CLAUDE.md).

## Testing

The project has comprehensive tests covering:

- Unit tests for all components
- Integration tests for end-to-end validation
- Architecture tests enforcing design rules
- Thread safety and concurrent access validation

## Code Quality & Security

The project enforces quality and security through:

- **Spotless**: Google Java Format for consistent code style
- **SpotBugs with FindSecBugs**: Static analysis and security scanning
- **JaCoCo**: 70% minimum test coverage threshold
- **ArchUnit**: Architecture rules enforcement

For detailed code quality standards and development workflow, see [CLAUDE.md](CLAUDE.md).