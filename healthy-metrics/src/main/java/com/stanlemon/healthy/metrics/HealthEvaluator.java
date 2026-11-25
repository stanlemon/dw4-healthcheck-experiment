package com.stanlemon.healthy.metrics;

/**
 * Evaluates application health based on error rates and latency thresholds. This class contains the
 * health evaluation logic shared by both Dropwizard and Spring Boot health check implementations.
 */
public class HealthEvaluator {

  private final MetricsService metricsService;

  public HealthEvaluator(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /** Evaluates current health based on error and latency thresholds. */
  public HealthResponse evaluate() {
    // One snapshot, so the breach flags always match the numbers reported next to them.
    MetricsSnapshot snapshot = metricsService.snapshot();
    long errors = snapshot.getErrorsLastMinute();
    double latency = snapshot.getAverageLatencyLast60Seconds();
    boolean errorBreached = snapshot.isErrorThresholdBreached();
    boolean latencyBreached = snapshot.isLatencyThresholdBreached();

    String errorDetail =
        String.format(
            "%d errors in last minute (threshold: %d)", errors, snapshot.getErrorThreshold());
    String latencyDetail =
        String.format(
            "%.1fms average latency in last 60 seconds (threshold: %.0fms)",
            latency, snapshot.getLatencyThresholdMs());

    String message;
    if (errorBreached && latencyBreached) {
      message =
          "Critical: Both error and latency thresholds breached - "
              + errorDetail
              + ", "
              + latencyDetail;
    } else if (errorBreached) {
      message = "Too many errors: " + errorDetail;
    } else if (latencyBreached) {
      message = "High latency: " + latencyDetail;
    } else {
      message = "OK - " + errorDetail + ", " + latencyDetail;
    }

    String status = errorBreached || latencyBreached ? "unhealthy" : "healthy";
    return new HealthResponse(status, message, errors, latency, errorBreached, latencyBreached);
  }
}
