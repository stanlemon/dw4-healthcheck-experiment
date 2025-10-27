# Dropwizard Application

A Dropwizard application demonstrating real-time metrics collection and health monitoring based on customer experience signals.

## Features

- **Real-time Metrics**: Tracks error rates and response latency across all endpoints
- **Dual-Threshold Health Monitoring**: Health checks based on both error counts and average latency
- **Automatic Instrumentation**: Latency tracking via filter middleware, error tracking via global exception mapper
- **REST API**: Simple endpoints for testing metrics collection and health monitoring
- **Comprehensive Testing**: 85+ tests with high coverage and architecture enforcement

## Requirements

- JDK 21
- Maven 3.6.3+

## Quick Start

```bash
# Clone and build
git clone <repository-url>
cd dw-test2
mvn clean package

# Run the application
mvn exec:java

# Or use the JAR
java -jar target/dw-test2-1.0-SNAPSHOT.jar server config.yml
```

The application will start on:
- **Application Port**: [http://localhost:8097](http://localhost:8097)
- **Admin Port**: [http://localhost:8098](http://localhost:8098)

## Development

For detailed development guidelines, testing strategies, and code quality standards, see [CLAUDE.md](CLAUDE.md).

```bash
# Run tests
mvn test

# Run with coverage
mvn clean test jacoco:report
open target/site/jacoco/index.html

# Check code formatting
mvn spotless:check

# Apply code formatting
mvn spotless:apply
```

## Endpoints

- Hello World: [http://localhost:8097/hello](http://localhost:8097/hello) - Returns a simple JSON message
- Error Test: [http://localhost:8097/error](http://localhost:8097/error) - Deliberately throws a 500 error
- Metrics: [http://localhost:8097/metrics](http://localhost:8097/metrics) - Shows comprehensive error counts, latency statistics, and threshold status
- Health Check: [http://localhost:8098/healthcheck](http://localhost:8098/healthcheck) - Returns health status based on error and latency thresholds
- Slow Requests: [http://localhost:8097/slow](http://localhost:8097/slow) - Introduces artificial delays to test latency thresholds

## Utility Scripts

- **`generate_errors.sh [count] [latency_percentage]`** - Generate errors and latency for testing thresholds
- **`poll_metrics.sh`** - Continuously poll and display metrics in real-time
- **`analyze-code.sh`** - Run tests and upload analysis to SonarCloud

### Test Error Endpoints

These endpoints demonstrate how the global exception mapper catches different types of errors:

- Runtime Exception: [http://localhost:8097/test-errors/runtime/your-message](http://localhost:8097/test-errors/runtime/your-message)
- Web Application Exception: [http://localhost:8097/test-errors/web-app/500](http://localhost:8097/test-errors/web-app/500)

### Slow Request Endpoints

These endpoints introduce artificial delays to test latency monitoring:

- Default 1-second delay: [http://localhost:8097/slow](http://localhost:8097/slow)
- Custom delay (in milliseconds): [http://localhost:8097/slow/2000](http://localhost:8097/slow/2000)
- Examples for testing:
  - 600ms delay: [http://localhost:8097/slow/600](http://localhost:8097/slow/600) - Exceeds 100ms threshold
  - 2-second delay: [http://localhost:8097/slow/2000](http://localhost:8097/slow/2000) - Significantly exceeds threshold
  - 5-second delay: [http://localhost:8097/slow/5000](http://localhost:8097/slow/5000) - Maximum practical delay

**Note:** Maximum allowed delay is 10 seconds to prevent abuse.

## Metrics and Monitoring

The application provides comprehensive monitoring with dual-threshold alerting:

### Error Monitoring

- Tracks 5xx errors in a 1-minute sliding window using circular buffers
- Default threshold: 100 errors per minute
- Automatic recording via global exception mapper

### Latency Monitoring

- Tracks request latency for all HTTP requests in a 60-minute sliding window
- Default threshold: 100ms average latency
- Automatic recording via request/response filter middleware

### Metrics Endpoint Response

The `/metrics` endpoint returns comprehensive monitoring data:

```json
{
  "errorsLastMinute": 5,
  "totalErrors": 25,
  "avgLatencyLast60Minutes": 245.5,
  "errorThresholdBreached": false,
  "latencyThresholdBreached": false,
  "healthy": true
}
```

### Health Check

The health check monitors both error rates and response latency:

- **Error Threshold**: More than 100 errors in the last minute
- **Latency Threshold**: Average latency exceeding 100ms over the last 60 minutes
- **Health Status**:
  - `Healthy`: Both thresholds within limits
  - `Unhealthy`: One or both thresholds exceeded
  - `Critical`: Both thresholds exceeded simultaneously

### Threshold Methods

Both error and latency monitoring support custom and default thresholds:

```java
// Error thresholds
metricsService.isErrorThresholdBreached();        // Uses default 100
metricsService.isErrorThresholdBreached(50);      // Custom threshold

// Latency thresholds
metricsService.isLatencyThresholdBreached();      // Uses default 100ms
metricsService.isLatencyThresholdBreached(300.0); // Custom threshold
```

## Global Exception Handling

The application uses a global exception mapper to catch all exceptions:

- All endpoints benefit from consistent error handling
- All 5xx errors are automatically tracked for health monitoring
- Different types of exceptions are handled appropriately
- The middleware approach ensures no errors slip through untracked

## Architecture

The application uses a dual-threshold monitoring approach:

- **Latency Tracking**: JAX-RS filter measures all request response times
- **Error Tracking**: Global exception mapper captures all 5xx errors
- **Metrics Storage**: Circular buffer implementation with sliding time windows
- **Health Monitoring**: Configurable thresholds for both errors and latency

For detailed architecture and implementation patterns, see [CLAUDE.md](CLAUDE.md).

## Testing

The application has 85+ tests covering:

- Unit tests for all components
- Integration tests for end-to-end validation
- Functional tests for API behavior
- Architecture tests enforcing design rules
- Thread safety and concurrent access validation

```bash
# Run all tests
mvn test

# Run with coverage report
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

For testing strategies, patterns, and best practices, see [CLAUDE.md](CLAUDE.md).

## Code Quality & Security

The project enforces quality and security through:

- **Spotless**: Google Java Format for consistent code style
- **SpotBugs with FindSecBugs**: Static analysis and security scanning
- **JaCoCo**: 70% minimum test coverage threshold
- **ArchUnit**: Architecture rules enforcement
- **Maven Enforcer**: Dependency security rules
- **GitHub Actions**: Automated CI/CD with security scans

```bash
# Format code
mvn spotless:apply

# Run security analysis
mvn spotbugs:check

# Run all checks
mvn clean verify
```

For detailed code quality standards and development workflow, see [CLAUDE.md](CLAUDE.md).


## SonarCloud Integration

The project supports SonarCloud for continuous code quality analysis. SonarCloud is free for open source projects and provides detailed reports on code smells, bugs, security vulnerabilities, coverage, and complexity.

### Setup

1. Sign up at [sonarcloud.io](https://sonarcloud.io) with your GitHub account
2. Import this repository to SonarCloud
3. Generate a token from [your account security settings](https://sonarcloud.io/account/security)

### Run Analysis

```bash
# For personal accounts
export SONAR_TOKEN=your_token_here
./analyze-code.sh

# For organization accounts
export SONAR_TOKEN=your_token_here
export SONAR_ORGANIZATION=your_organization_key
./analyze-code.sh
```

The script runs tests with coverage and uploads the analysis to SonarCloud.

## Example Usage

### Monitoring Latency in Real-Time

```bash
# Make some requests
curl http://localhost:8097/hello
curl http://localhost:8097/hello
curl http://localhost:8097/hello

# Check metrics
curl http://localhost:8097/metrics
```

### Triggering Error Thresholds

```bash
# Generate errors using the provided script
./generate_errors.sh                    # Generates 15 errors with 30% latency (default)
./generate_errors.sh 25                 # Generates 25 errors with 30% latency
./generate_errors.sh 25 50              # Generates 25 errors with 50% latency
./generate_errors.sh 101 0              # Generates 101 errors with no latency (errors only)
./generate_errors.sh 50 100             # Generates 50 errors with 100% latency (all requests slow)

# Aggressive latency testing (percentages > 100%)
./generate_errors.sh 10 200             # 10 errors + 20 latency requests (2x multiplier)
./generate_errors.sh 5 500              # 5 errors + 25 latency requests (5x multiplier)
./generate_errors.sh 20 300             # 20 errors + 60 latency requests (3x multiplier)

# Alternative: Generate errors manually
for i in {1..101}; do curl http://localhost:8097/error; done

# Check health status
curl http://localhost:8098/healthcheck
```

### Triggering Latency Thresholds

```bash
# Single slow requests to test latency tracking
curl http://localhost:8097/slow/600    # 600ms delay (exceeds 100ms threshold)
curl http://localhost:8097/slow/1000   # 1 second delay
curl http://localhost:8097/slow/2000   # 2 second delay

# Multiple slow requests to breach average latency threshold
# Generate several slow requests to raise average latency above 100ms
for i in {1..10}; do curl http://localhost:8097/slow/800; done

# Check metrics and health status
curl http://localhost:8097/metrics
curl http://localhost:8098/healthcheck
```

### Combined Error and Latency Testing

The `generate_errors.sh` script now supports testing both error and latency thresholds simultaneously:

```bash
# Test scenarios for comprehensive monitoring validation

# Scenario 1: High error rate with moderate latency
./generate_errors.sh 150 25           # 150 errors, 25% slow - triggers error threshold

# Scenario 2: Moderate errors with high latency
./generate_errors.sh 50 80            # 50 errors, 80% slow - may trigger latency threshold

# Scenario 3: Critical scenario - both thresholds breached
./generate_errors.sh 120 70           # 120 errors, 70% slow - likely triggers both thresholds

# Scenario 4: Latency testing without error threshold breach
./generate_errors.sh 30 100           # 30 errors, 100% slow - focuses on latency impact

# Scenario 5: Egregious latency testing (percentages > 100%)
./generate_errors.sh 10 500           # 10 errors + 50 latency requests - guaranteed latency threshold breach
./generate_errors.sh 20 300           # 20 errors + 60 latency requests - aggressive latency testing
./generate_errors.sh 5 1000           # 5 errors + 50 latency requests - extreme latency scenario

# Monitor the results
curl http://localhost:8097/metrics     # Check current metrics
curl http://localhost:8098/healthcheck # Check health status
```

**Latency Delays Used**: The script randomly selects from delays ranging from 600ms to 10 seconds (600ms, 800ms, 1000ms, 1200ms, 1500ms, 2000ms, 3000ms, 4000ms, 5000ms, 7000ms, 10000ms) - all exceeding the default 100ms threshold, with the higher delays guaranteeing latency threshold breaches.

### Health Check Responses

**Healthy State:**

```text
OK - 5 errors in last minute (threshold: 100), 250.0ms average latency in last 60 minutes (threshold: 100ms)
```

**Error Threshold Breached:**

```text
Too many errors: 150 errors in last minute (threshold: 100)
```

**Latency Threshold Breached:**

```text
High latency: 750.0ms average latency in last 60 minutes (threshold: 100ms)
```

**Critical State (Both Breached):**

```text
Critical: Both error and latency thresholds breached - 150 errors in last minute (threshold: 100), 750.0ms average latency in last 60 minutes (threshold: 100ms)
```
