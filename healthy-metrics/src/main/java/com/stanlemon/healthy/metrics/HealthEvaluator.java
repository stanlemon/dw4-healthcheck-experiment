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
    long errorCount = metricsService.getErrorCountLastMinute();
    double avgLatency = metricsService.getAverageLatencyLast60Seconds();
    boolean errorBreached = metricsService.isErrorThresholdBreached();
    boolean latencyBreached = metricsService.isLatencyThresholdBreached();
    long errorThreshold = metricsService.getDefaultErrorThreshold();
    double latencyThreshold = metricsService.getDefaultLatencyThresholdMs();

    if (errorBreached && latencyBreached) {
      return new HealthResponse(
          "unhealthy",
          String.format(
              "Critical: Both error and latency thresholds breached - "
                  + "%d errors in last minute (threshold: %d), "
                  + "%.1fms average latency in last 60 seconds (threshold: %.0fms)",
              errorCount, errorThreshold, avgLatency, latencyThreshold),
          errorCount,
          avgLatency,
          true,
          true);
    }

    if (errorBreached) {
      return new HealthResponse(
          "unhealthy",
          String.format(
              "Too many errors: %d errors in last minute (threshold: %d)",
              errorCount, errorThreshold),
          errorCount,
          avgLatency,
          true,
          false);
    }

    if (latencyBreached) {
      return new HealthResponse(
          "unhealthy",
          String.format(
              "High latency: %.1fms average latency in last 60 seconds (threshold: %.0fms)",
              avgLatency, latencyThreshold),
          errorCount,
          avgLatency,
          false,
          true);
    }

    return new HealthResponse(
        "healthy",
        String.format(
            "OK - %d errors in last minute (threshold: %d), "
                + "%.1fms average latency in last 60 seconds (threshold: %.0fms)",
            errorCount, errorThreshold, avgLatency, latencyThreshold),
        errorCount,
        avgLatency,
        false,
        false);
  }
}
