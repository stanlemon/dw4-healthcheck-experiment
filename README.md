# Multi-App Metrics Project

This project is a Maven build with many modules. One REST API runs on Dropwizard 5 and on Spring Boot 4. Both applications collect metrics in real time and use them for health checks. One shared metrics library, with no framework code, drives the health checks in both frameworks.

The project shows that the core metrics and health logic can live in a shared library with zero framework dependencies. Two different web frameworks use this library and give identical API behavior. Thus, you choose the framework at deploy time, not at design time.

## How It Works

Every HTTP request goes through a latency-tracking filter. Latency is the time that a request takes. When the request finishes, the filter makes one call to a shared `MetricsService`. That call records the duration of the request and whether the response had a 5xx (server error) status.

The service keeps its counters in a sliding window. A sliding window holds only the most recent 60 seconds. The window has one bucket per second, and each bucket holds the counts for its second. The health check reads one consistent snapshot of the window. A snapshot is a copy of all counts at one instant. The health check compares the error rate and the average latency with fixed thresholds, which are the limits for a healthy service.

```
Request ──> LatencyTrackingFilter ──> Resource ──> Response
                │                         │
                │ records latency         │ on exception
                │ and 5xx status          ▼
                ▼                  GlobalExceptionMapper (sets the 5xx status)
          MetricsService
                │
                │ polled by
                ▼
          Health Check ──> healthy / unhealthy
```

## Project Structure

```
healthy-parent/
├── architecture-rules/   # Shared ArchUnit rules for coding standards
├── healthy-metrics/      # Framework-agnostic metrics library
├── healthy-hangar/       # Framework-agnostic paper-airplane domain module
├── dw5-app/              # Dropwizard 5.x application
└── spring4-app/          # Spring Boot 4.x application
```

- `healthy-metrics` has zero framework dependencies. Its only dependency is the Jackson annotations. Both apps use it through the `MetricsService` interface.
- `healthy-hangar` is a second shared domain module with no framework code. It stores paper airplanes and predicts how well they fly. It shows that the framework-neutral pattern works for more than metrics and health data. Any business domain can live in a shared library. Each app connects the library with its own dependency injection (DI) code. DI is code that gives each object the services it needs.
- `architecture-rules` gives reusable ArchUnit rules that apply to all modules. Examples are no `java.util.Date`, no generic exceptions, and no `System.out`.

## Requirements

You need JDK 21 and Maven 3.6.3 or later.

## Quick Start

```bash
# Build everything
mvn clean install -DskipTests -Dspotbugs.skip=true

# Run the Dropwizard app (port 8097, admin on 8098)
mvn exec:java -pl dw5-app

# Run the Spring Boot app (port 8080)
mvn spring-boot:run -pl spring4-app
```

The default ports of the two apps do not overlap, so both apps can run at the same time. To run the Spring Boot app on a different port, for example 8081, use this command:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081" -pl spring4-app
```

## Endpoints

Both applications give the same functions. The table shows the default ports. If you change a port, use your port in its place.

| Endpoint | Dropwizard (8097) | Spring Boot (8080) |
|----------|-------------------|--------------------|
| Metrics | `/metrics` | `/metrics` |
| Readiness | `/health/ready` | `/health/ready` |
| Liveness | `/health/live` | `/health/live` |
| Health (native) | `:8098/healthcheck` | `/actuator/health` |
| Trigger error | `/test-errors/trigger` | `/test-errors/trigger` |
| Runtime error | `/test-errors/runtime/{message}` | `/test-errors/runtime/{message}` |
| Status error | `/test-errors/web-app/{code}` | `/test-errors/web-app/{code}` |
| Slow request | `/slow` or `/slow/{delayMs}` | `/slow` or `/slow/{delayMs}` |
| Stow plane | `POST /hangar/planes` | `POST /hangar/planes` |
| Get plane | `GET /hangar/planes/{id}` | `GET /hangar/planes/{id}` |
| List planes | `GET /hangar/planes` | `GET /hangar/planes` |

> The `/test-errors` and `/slow` endpoints exist only to exercise the health check. A production application does not include them. The `/hangar/planes` endpoints are a demo of the shared domain pattern (see Project Structure).

Both apps send error responses in the same shape, `{"code": <int>, "message": <string>}`, with identical HTTP status codes. Bean Validation is the standard Java check of input against annotations. A Bean Validation failure returns `400 Bad Request` in both runtimes. By default, Dropwizard returns `422` for these failures. `ConstraintViolationExceptionMapper` changes this status to `400` to match the behavior of Spring.

Dropwizard 5 serializes JSON with Jackson 2 (`com.fasterxml.jackson`). Spring Boot 4 uses Jackson 3 (`tools.jackson`). The shared DTOs do not have to pick one version. A DTO (data transfer object) is a class that the API sends as JSON. The databind jar of Jackson 3 still depends on `jackson-annotations` 2.x. Thus, both runtimes honor `@JsonProperty`, `@JsonCreator`, and `@JsonFormat`. A full move of the groupId to `tools.jackson` must wait until Dropwizard ships a release for Jackson 3.

## Metrics and Health Monitoring

### What Gets Tracked

The `LatencyTrackingFilter` counts errors, which are 5xx responses, in a 60-second sliding window. The same filter records the duration of every request in milliseconds. The service gives the average of these durations over the same 60-second window.

### Metrics Endpoint

`GET /metrics` returns the same JSON format from both applications:

```json
{
  "errorsLastMinute": 5,
  "totalErrors": 25,
  "avgLatencyLast60Seconds": 45.5,
  "errorThresholdBreached": false,
  "latencyThresholdBreached": false,
  "healthy": true
}
```

### Health Check

The health check compares the metrics data with two thresholds. A breach means that a value went past its threshold.

The error threshold changes with the traffic level. The traffic level is the number of requests in the last 60 seconds.

| Traffic level | Condition | Breach rule |
|---------------|-----------|-------------|
| Very low | < 10 total requests in last 60s | Never breaches (not enough data) |
| Moderate | 10-99 total requests | An error rate > 50% breaches |
| High | >= 100 total requests | An error rate > 10% breaches |

The latency threshold is an average latency above 100ms. The health check needs at least 5 requests in the window to apply it. With fewer than 5 requests, the health check does not apply the latency threshold, whatever the latency values are.

The health check reports one of these states:

| State | Condition |
|-------|-----------|
| Healthy | Both values are within their thresholds |
| Error threshold breached | Too many errors for the traffic level |
| Latency threshold breached | Average latency is above 100ms, with at least 5 requests |
| Critical | Both thresholds are breached |

#### Readiness: `GET /health/ready`

This endpoint reports whether this instance can accept traffic. It compares the error rate and the latency with their thresholds. If the status is healthy, it returns 200. If the status is unhealthy, it returns 503. It maps to a Kubernetes readiness probe, which is a check that decides which pods get traffic.

```json
{
  "status": "healthy",
  "message": "OK - 5 errors out of 120 requests in last minute (threshold: 10% error rate), 45.5ms average latency in last 60 seconds (threshold: 100ms)",
  "errorsLastMinute": 5,
  "avgLatencyLast60Seconds": 45.5,
  "errorThresholdBreached": false,
  "latencyThresholdBreached": false,
  "healthy": true
}
```

#### Liveness: `GET /health/live`

This endpoint reports whether the process works at the most basic level. If 100% of requests fail and the window holds at least 10 requests, the check fails. In all other cases, the check passes. If the process is alive, it returns 200. If the process is dead, it returns 503. It maps to a Kubernetes liveness probe, which is a check that restarts a broken container.

```json
{
  "status": "alive",
  "message": "OK - 0 errors out of 20 requests in last minute",
  "errorsLastMinute": 0,
  "totalRequestsLastMinute": 20,
  "alive": true
}
```

Both apps also keep their native health endpoints for tools that are specific to each framework. Dropwizard serves `GET :8098/healthcheck` on its admin port in the Dropwizard format. The admin port listens only on `127.0.0.1`. Spring Boot serves `GET /actuator/health` in the Actuator format, with an `UP` or `DOWN` status.

## Example Usage

The examples below use the Dropwizard port (8097). For Spring Boot, use `8080` in place of `8097`.

### Monitoring Latency

```bash
# Generate some traffic
for i in {1..10}; do curl -s http://localhost:8097/metrics > /dev/null; done

# Check metrics
curl http://localhost:8097/metrics
```

### Triggering Error Thresholds

```bash
# Generate errors (need enough requests for adaptive thresholding to apply)
for i in {1..20}; do curl -s http://localhost:8097/test-errors/trigger > /dev/null; done

# Also generate some successful requests so total traffic >= 10
for i in {1..5}; do curl -s http://localhost:8097/metrics > /dev/null; done

# Check readiness — should show errors breached
curl http://localhost:8097/health/ready
```

### Triggering Latency Thresholds

```bash
# Generate slow requests (need at least 5 for threshold evaluation)
for i in {1..6}; do curl -s http://localhost:8097/slow/600 > /dev/null; done

# Check readiness — average latency should exceed 100ms threshold
curl http://localhost:8097/health/ready
```

### Using the Helper Scripts

```bash
# Generate errors and slow requests together
# Usage: ./generate_errors.sh [error_count] [latency_percentage]
./generate_errors.sh 25 50      # 25 errors + 50% as slow requests

# Poll metrics continuously
./poll_metrics.sh
```

> The helper scripts send requests to the Dropwizard port (8097). Start the Dropwizard app before you run them. If `generate_errors.sh` cannot connect to port 8097, it stops.

## Development

### Testing

```bash
mvn test                                          # All modules
mvn test -pl dw5-app                              # Single module
mvn test -Dtest=DefaultMetricsServiceTest -pl healthy-metrics  # Single class
mvn clean test jacoco:report                      # With coverage
```

### Code Quality

```bash
mvn spotless:apply         # Format code (Google Java Format)
mvn spotless:check         # Check formatting
mvn spotbugs:check         # Static analysis + FindSecBugs
mvn clean verify           # All checks
```

### Quality Enforcement

The build uses these tools to enforce quality:

- Spotless: Google Java Format 2.0.0
- SpotBugs and FindSecBugs: static analysis and security scans
- JaCoCo: a minimum of 90% line coverage and 80% branch coverage
- ArchUnit: architecture rules from the shared `architecture-rules` module
- Maven Enforcer: blocks dependency versions with known vulnerabilities (Log4j, Jackson, Commons Collections)

## Continuous integration

Pull requests and Renovate branches run two required jobs: `test` and `security`.
The test job runs the tests of every module and enforces the JaCoCo coverage minimums.
The security job runs SpotBugs, the credential scanner, dependency analysis, and the dependency review for pull requests.
Scheduled full scans and manual full scans also run CodeQL.
The reports and failure artifacts include the `target` directory of every module.
The security job replaced an earlier standalone SpotBugs job.
The two required jobs also replaced a duplicate Renovate validation job.

## Architecture Notes

### Why `DwConfiguration` Is Empty

Dropwizard requires a typed `Configuration` subclass, even for an app with no custom configuration. The defaults in `DefaultMetricsService` hold all the thresholds. A production app can read the thresholds from this configuration class.

### Thread Safety

`DefaultMetricsService` is safe for concurrent access. Every public method is `synchronized` and reads the clock inside the lock. One lock guards all state, so the counters are plain `long` fields. A constructor takes a `Clock` parameter, and the default constructor uses `Clock.systemUTC()`. With this parameter, tests control time and get the same result from the sliding window on every run.

### Sliding Window Implementation

The request counts, error counts, and latency totals use circular buffers with 60 buckets, one per second. A circular buffer is a fixed array that reuses its oldest slots. When time moves forward, the service does not clear old buckets immediately. It clears them on the next read or write operation. Thus, writes are O(1) amortized and reads are O(n), where n=60.
