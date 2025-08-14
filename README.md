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

## Utility Scripts

The project includes several utility scripts for testing and development:

- **`generate_errors.sh [count]`** - Generate errors for testing thresholds (default: 15)
- **`poll_metrics.sh`** - Continuously poll and display metrics
- **`run_tests.sh`** - Run tests with coverage reporting
- **`analyze-code.sh`** - Upload code analysis to SonarCloud

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

## Code Quality & Security

The project includes comprehensive code quality and security tooling:

### Static Analysis & Quality

- **SonarCloud**: Continuous code quality analysis with coverage integration
- **Spotless**: Google Java Format enforcement for consistent code style
- **JaCoCo**: Test coverage analysis with 70% minimum threshold

### Security Scanning

- **OWASP Dependency Check**: Scans for known vulnerabilities in dependencies
- **GitHub CodeQL**: Static Application Security Testing (SAST)
- **Security Workflow**: Weekly automated security scans

### Build Standards

- **Maven Enforcer**: Ensures Java 21 and Maven 3.6.3+ requirements
- **Dependency Management**: Renovate Bot for automated dependency updates

### Running Security Checks

```bash
# Run OWASP dependency vulnerability scan
mvn org.owasp:dependency-check-maven:check

# Check code formatting
mvn spotless:check

# Apply code formatting
mvn spotless:apply

# Run all quality checks
mvn clean verify
```

### Continuous Integration

The project includes GitHub Actions workflows for:

- **Test Workflow**: Runs tests, coverage, and uploads results to Codecov
- **Security Workflow**: OWASP scans and CodeQL analysis
- **Release Workflow**: Automated releases on version tags

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

## Code Quality Analysis with SonarCloud

The project is configured to work with SonarCloud (free for open source projects) for comprehensive code quality analysis:

- **Code Smells**: Maintainability issues and anti-patterns
- **Bugs**: Potential runtime errors and logic issues
- **Security Vulnerabilities**: Security hotspots and vulnerabilities
- **Code Coverage**: Integration with JaCoCo coverage reports
- **Duplicated Code**: Detection of code duplication across the project
- **Complexity Metrics**: Cyclomatic complexity and cognitive complexity analysis

### Setup and Usage

1. **Sign up for SonarCloud**: Go to [sonarcloud.io](https://sonarcloud.io) and sign in with your GitHub account
2. **Import your project**: Add this repository to SonarCloud
3. **Generate a token**: Go to [sonarcloud.io/account/security](https://sonarcloud.io/account/security)
4. **Check if you need an organization**:
   - **Personal account**: You might not need an organization key
   - **Organization account**: Find your key at [sonarcloud.io/organizations](https://sonarcloud.io/organizations)

### Run Analysis

**For personal accounts** (try this first):

```bash
# Set only your token
export SONAR_TOKEN=your_token_here

# Run analysis
./analyze-code.sh
```

**If you need a custom project key**:

```bash
# Set token and custom project key
export SONAR_TOKEN=your_token_here
export SONAR_PROJECT_KEY=your-custom-project-key

# Run analysis
./analyze-code.sh
```

**For organization accounts**:

```bash
# Set both token and organization
export SONAR_TOKEN=your_token_here
export SONAR_ORGANIZATION=your_organization_key

# Run analysis
./analyze-code.sh
```

The script will:

- Run all tests and generate JaCoCo coverage reports
- Upload code analysis to SonarCloud
- Provide a direct link to view results

### Analysis Reports

SonarCloud provides detailed reports including:

- **Quality Gates**: Pass/fail criteria for code quality
- **Code Coverage**: Line and branch coverage metrics from JaCoCo
- **Maintainability Rating**: Technical debt ratio assessment
- **Reliability Rating**: Bug density analysis
- **Security Rating**: Security vulnerability assessment
- **Duplication**: Percentage of duplicated lines
- **Issues Breakdown**: Detailed list of all detected issues with severity levels

### Project Analysis Focus

For this Dropwizard project, SonarCloud will analyze:

- **Line Coverage**: Currently achieving high coverage through comprehensive test suite (95 tests)
- **Code Quality**: 20 Java files including metrics service, health checks, and REST endpoints
- **Security**: Input validation and exception handling patterns
- **Maintainability**: Code complexity in latency calculations and filter logic

### Quick Start Summary

**For personal accounts:**

```bash
# 1. Sign up at https://sonarcloud.io with GitHub
# 2. Import your repository
# 3. Get your token from account security
# 4. Run analysis:

export SONAR_TOKEN=your_token_here
./analyze-code.sh
```

**For organization accounts:**

```bash
# 1. Sign up at https://sonarcloud.io with GitHub
# 2. Import your repository to your organization
# 3. Get your organization key and token
# 4. Run analysis:

export SONAR_TOKEN=your_token_here
export SONAR_ORGANIZATION=your_organization_key
./analyze-code.sh
```

That's it! Your code quality analysis will be available at SonarCloud.

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
./generate_errors.sh           # Generates 15 errors (default)
./generate_errors.sh 25        # Generates 25 errors
./generate_errors.sh 101       # Generates 101 errors to breach threshold

# Alternative: Generate errors manually
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
