# Multi-App Metrics Project

A multi-module project demonstrating real-time metrics collection and health monitoring in both Dropwizard 5 and Spring Boot 4 applications. The same framework-agnostic metrics library drives health checks in both frameworks.

The goal is to prove that core metrics and health logic can live in a shared library with zero framework dependencies, while two different web frameworks consume it and produce identical API behavior. This makes framework choice a deployment decision rather than an architectural one.

## How It Works

Every HTTP request flows through a latency-tracking filter that records its duration. Every unhandled exception flows through a global exception handler that records 5xx errors. Both feed into a shared `MetricsService` that maintains sliding-window counters (one bucket per second, 60-second window). The health check reads from that same service and compares error rates and average latency against configurable thresholds.

```
Request ──> LatencyTrackingFilter ──> Resource ──> Response
                │                         │
                │ records latency         │ on exception
                ▼                         ▼
          MetricsService  <────── GlobalExceptionMapper
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

- **healthy-metrics** has zero framework dependencies (only Jackson annotations). Both apps consume it through the `MetricsService` interface.
- **healthy-hangar** is a second framework-agnostic shared-domain module (paper-airplane storage + aerodynamic-prediction). It exists to demonstrate that the framework-neutral pattern generalizes beyond observability: any business domain can live in a shared library that both apps wire with their own DI glue.
- **architecture-rules** provides reusable ArchUnit rules (no `java.util.Date`, no generic exceptions, no `System.out`, etc.) applied to all modules.

## Requirements

- JDK 21
- Maven 3.6.3+

## Quick Start

```bash
# Build everything
mvn clean install -DskipTests -Dspotbugs.skip=true

# Run the Dropwizard app (port 8097, admin on 8098)
mvn exec:java -pl dw5-app

# Run the Spring Boot app (port 8080)
mvn spring-boot:run -pl spring4-app
```

To run both apps simultaneously, override the Spring Boot port:

```bash
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8081" -pl spring4-app
```

## Endpoints

Both applications expose equivalent functionality. The table shows the default ports; adjust if you override them.

| Endpoint | Dropwizard (8097) | Spring Boot (8080) |
|----------|-------------------|--------------------|
| Metrics | `/metrics` | `/metrics` |
| Readiness | `/health/ready` | `/health/ready` |
| Liveness | `/health/live` | `/health/live` |
| Health (native) | `:8098/healthcheck` | `/actuator/health` |
| Trigger error | `/test-errors/trigger` | `/test-errors/trigger` |
| Runtime error | `/test-errors/runtime/{msg}` | `/test-errors/runtime/{msg}` |
| Status error | `/test-errors/web-app/{code}` | `/test-errors/web-app/{code}` |
| Slow request | `/slow` or `/slow/{ms}` | `/slow` or `/slow/{ms}` |
| Stow plane | `POST /hangar/planes` | `POST /hangar/planes` |
| Get plane | `GET /hangar/planes/{id}` | `GET /hangar/planes/{id}` |
| List planes | `GET /hangar/planes` | `GET /hangar/planes` |

> **Note:** The `/test-errors` and `/slow` endpoints exist to exercise the health check. They would not be included in a production application. The `/hangar/planes` endpoints are a demo of the shared-domain pattern (see Project Structure).

**Error bodies.** Both apps emit the same shape for error responses — `{"code": <int>, "message": <string>}` — with identical HTTP status codes. Bean Validation failures return `400 Bad Request` in both runtimes; Dropwizard's default `422` is overridden by `ConstraintViolationExceptionMapper` to match Spring's behavior.

**Jackson 2 vs 3.** Dropwizard 5 serializes with Jackson 2 (`com.fasterxml.jackson`); Spring Boot 4 has migrated to Jackson 3 (`tools.jackson`). The shared DTOs don't have to pick a side: Jackson 3's databind jar still depends on `jackson-annotations` 2.x, so `@JsonProperty`, `@JsonCreator`, and `@JsonFormat` are honored by both runtimes. A full groupId move to `tools.jackson` is blocked until Dropwizard ships a Jackson-3 release.

## Metrics and Health Monitoring

### What Gets Tracked

- **Errors**: 5xx responses, counted in a 60-second sliding window via the global exception handler
- **Latency**: Every request's duration in milliseconds, averaged over a 60-second sliding window via the latency filter

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

The health check evaluates two thresholds against the metrics data:

**Error threshold** uses adaptive logic based on traffic volume:

| Traffic level | Condition | Breach rule |
|---------------|-----------|-------------|
| Very low | < 10 total requests in last 60s | Never breaches (insufficient data) |
| Moderate | 10-99 total requests | Breaches if errors > min(100, requests/2) |
| High | >= 100 total requests | Breaches if error rate > 10% |

**Latency threshold**: Average latency > 100ms, but only evaluated when there are at least 5 requests in the window. With fewer than 5 requests, the threshold is not applied regardless of latency values.

**Health states:**

| State | Condition |
|-------|-----------|
| Healthy | Both thresholds within limits |
| Error threshold breached | Too many errors relative to traffic |
| Latency threshold breached | Average latency exceeds 100ms with sufficient traffic |
| Critical | Both thresholds exceeded |

#### Readiness: `GET /health/ready`

Reports whether this instance can handle traffic. Checks error rates and latency thresholds. Returns 200 when healthy, 503 when degraded. Maps to a Kubernetes readiness probe.

```json
{
  "status": "healthy",
  "message": "OK - 5 errors in last minute (threshold: 100), 45.5ms average latency in last 60 seconds (threshold: 100ms)",
  "errorsLastMinute": 5,
  "avgLatencyLast60Seconds": 45.5,
  "errorThresholdBreached": false,
  "latencyThresholdBreached": false,
  "healthy": true
}
```

#### Liveness: `GET /health/live`

Reports whether the process is fundamentally functional. Only fails when 100% of requests are erroring with sufficient traffic (10+ requests). Returns 200 when alive, 503 when dead. Maps to a Kubernetes liveness probe.

```json
{
  "status": "alive",
  "message": "OK - 0 errors out of 20 requests in last minute",
  "errorsLastMinute": 0,
  "totalRequestsLastMinute": 20,
  "alive": true
}
```

Both apps also retain their native health endpoints for framework-specific tooling:
- **Dropwizard**: `GET :8098/healthcheck` (admin port, Dropwizard format)
- **Spring Boot**: `GET /actuator/health` (Actuator format with `UP`/`DOWN` status)

## Example Usage

The examples below use the Dropwizard port (8097). For Spring Boot, replace `8097` with `8080`.

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

> **Note:** The helper scripts target the Dropwizard ports (8097/8098) by default.

## Development

### Testing

```bash
mvn test                                          # All modules
mvn test -pl dw5-app                              # Single module
mvn test -Dtest=MetricsServiceTest -pl healthy-metrics  # Single class
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

- **Spotless**: Google Java Format 1.28.0
- **SpotBugs + FindSecBugs**: Static analysis and security scanning
- **JaCoCo**: 90% minimum line / 80% minimum branch coverage
- **ArchUnit**: Architecture rules enforced via shared `architecture-rules` module
- **Maven Enforcer**: Blocks known-vulnerable dependency versions (Log4j, Jackson, Commons Collections)

## Architecture Notes

### Why `DwConfiguration` Is Empty

Dropwizard requires a typed `Configuration` subclass even when there's no custom configuration. All thresholds are built into `DefaultMetricsService` defaults. In a production app you'd wire the thresholds through this configuration class.

### Thread Safety

`DefaultMetricsService` is designed for concurrent access. Methods that modify buckets are `synchronized`; counters use `AtomicLong`. The `Clock` parameter (defaulting to `Clock.systemUTC()`) enables deterministic testing of time-dependent sliding window behavior.

### Sliding Window Implementation

Error counts and latency measurements use circular buffers with 60 buckets (one per second). As time advances, stale buckets are lazily cleared on the next read or write operation. This gives O(1) amortized writes and O(n) reads where n=60.
