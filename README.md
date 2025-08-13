# Dropwizard Application

A Dropwizard application running on JDK 21 with a health check based upon customer experience metrics:

1. Hello World endpoint returning JSON
2. Error endpoint that throws a 500 error
3. Metrics endpoint showing error counts and latency statistics
4. Health check endpoint that monitors both error rates and response latency
5. Automatic latency tracking for all requests
6. Dual-threshold monitoring (errors and latency)

## Requirements

- JDK 21
- Maven

## Building the Application

```bash
mvn clean package
```

## Running the Application

### Using Maven (Recommended for Development)

```bash
mvn clean compile exec:java
```

### Using JAR (Recommended for Production)

```bash
# Build the application
mvn clean package

# Run the JAR
java -jar target/dw-test2-1.0-SNAPSHOT.jar server config.yml
```

## Endpoints

- Hello World: [http://localhost:8097/hello](http://localhost:8097/hello) - Returns a simple JSON message
- Error Test: [http://localhost:8097/error](http://localhost:8097/error) - Deliberately throws a 500 error
- Metrics: [http://localhost:8097/metrics](http://localhost:8097/metrics) - Shows comprehensive error counts, latency statistics, and threshold status
- Health Check: [http://localhost:8098/healthcheck](http://localhost:8098/healthcheck) - Returns health status based on error and latency thresholds

### Test Error Endpoints

These endpoints demonstrate how the global exception mapper catches different types of errors:

- Runtime Exception: [http://localhost:8097/test-errors/runtime/your-message](http://localhost:8097/test-errors/runtime/your-message)
- Web Application Exception: [http://localhost:8097/test-errors/web-app/500](http://localhost:8097/test-errors/web-app/500)
- Arithmetic Exception: [http://localhost:8097/test-errors/arithmetic](http://localhost:8097/test-errors/arithmetic)
- Null Pointer Exception: [http://localhost:8097/test-errors/null-pointer](http://localhost:8097/test-errors/null-pointer)

## Metrics and Monitoring

The application provides comprehensive monitoring with dual-threshold alerting:

### Error Monitoring

- Tracks 5xx errors in a 1-minute sliding window using circular buffers
- Default threshold: 100 errors per minute
- Automatic recording via global exception mapper

### Latency Monitoring

- Tracks request latency for all HTTP requests in a 60-minute sliding window
- Default threshold: 500ms average latency
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
- **Latency Threshold**: Average latency exceeding 500ms over the last 60 minutes
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
metricsService.isLatencyThresholdBreached();      // Uses default 500ms
metricsService.isLatencyThresholdBreached(300.0); // Custom threshold
```

## Global Exception Handling

The application uses a global exception mapper to catch all exceptions:

- All endpoints benefit from consistent error handling
- All 5xx errors are automatically tracked for health monitoring
- Different types of exceptions are handled appropriately
- The middleware approach ensures no errors slip through untracked

## Architecture and Implementation

### Latency Tracking Filter

The application includes a `LatencyTrackingFilter` that automatically measures request latency:

- **Implementation**: Implements both `ContainerRequestFilter` and `ContainerResponseFilter`
- **Timing**: Records start time on request, calculates duration on response
- **Coverage**: Tracks all HTTP requests automatically
- **Storage**: Uses MetricsService's 60-minute sliding window with per-minute buckets
- **Performance**: Minimal overhead using simple timestamp recording

### Metrics Service

The `MetricsService` uses high-performance circular buffer implementations:

- **Error Tracking**: 60 buckets for 1-minute sliding window (one bucket per second)
- **Latency Tracking**: 60 buckets for 60-minute sliding window (one bucket per minute)
- **Thread Safety**: Uses `AtomicLong` arrays for lock-free concurrent access
- **Memory Efficiency**: Fixed-size arrays with automatic cleanup of stale data
- **Singleton Pattern**: Single instance for application-wide metrics

### Integration Points

- **Filter Registration**: Automatically registers via `@Provider` annotation
- **Health Checks**: ApplicationHealthCheck integrates both error and latency thresholds
- **Metrics Endpoint**: Real-time reporting of all metrics and threshold status
- **Exception Mapping**: Global exception mapper records 5xx errors automatically

## Testing

The application includes comprehensive test coverage:

- **Unit Tests**: 85+ tests covering all components with extensive edge case coverage
- **Integration Tests**: End-to-end validation of latency tracking and application functionality
- **Branch Coverage**: Comprehensive testing of all code paths including bucket clearing logic
- **Thread Safety Tests**: Concurrent access validation for metrics recording
- **Health Check Testing**: All threshold scenarios and edge cases
- **Mock Removal**: Refactored tests to use real implementations for better integration testing

### Running Tests

```bash
mvn clean test
```

### Running Test Coverage

The project uses JaCoCo for test coverage analysis. To generate and view coverage reports:

```bash
# Run tests and generate coverage report
mvn clean test jacoco:report

# View the coverage report in your browser
open target/site/jacoco/index.html
```

The coverage report provides:

- **Line Coverage**: Percentage of code lines executed during tests
- **Branch Coverage**: Percentage of conditional branches tested
- **Method Coverage**: Percentage of methods invoked during tests
- **Class Coverage**: Percentage of classes that have at least one method invoked

Coverage reports are generated in HTML format and include:

- Overall project coverage summary
- Per-package coverage breakdown
- Per-class detailed coverage with highlighted source code
- Identification of untested code paths

## Code Formatting

The project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format to maintain consistent code formatting across the codebase.

### Checking Code Format

To check if all code follows the Google Java Format style:

```bash
mvn spotless:check
```

### Applying Code Format

To automatically format all Java code according to Google Java Format:

```bash
mvn spotless:apply
```

### Code Format Features

- **Google Java Format**: Uses Google's official Java formatting style
- **Import Organization**: Removes unused imports automatically
- **Whitespace Management**: Trims trailing whitespace and ensures files end with newlines
- **Consistent Style**: Enforces consistent indentation, spacing, and line breaks

The Spotless plugin is configured to:

- Format all Java files in `src/main/java` and `src/test/java`
- Use Google Java Format version 1.19.2 with GOOGLE style
- Remove unused imports automatically
- Trim trailing whitespace
- Ensure files end with a newline

**Tip**: Run `mvn spotless:apply` before committing code to ensure consistent formatting.

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
# Generate 101 errors to breach error threshold
for i in {1..101}; do curl http://localhost:8097/error; done

# Check health status
curl http://localhost:8098/healthcheck
```

### Health Check Responses

**Healthy State:**

```text
OK - 5 errors in last minute (threshold: 100), 250.0ms average latency in last 60 minutes (threshold: 500ms)
```

**Error Threshold Breached:**

```text
Too many errors: 150 errors in last minute (threshold: 100)
```

**Latency Threshold Breached:**

```text
High latency: 750.0ms average latency in last 60 minutes (threshold: 500ms)
```

**Critical State (Both Breached):**

```text
Critical: Both error and latency thresholds breached - 150 errors in last minute (threshold: 100), 750.0ms average latency in last 60 minutes (threshold: 500ms)
```
