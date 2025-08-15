package com.example.dw.health;

import com.codahale.metrics.health.HealthCheck;
import com.example.dw.metrics.MetricsService;

public class ApplicationHealthCheck extends HealthCheck {
  private final MetricsService metricsService;

  /**
   * Constructs a new ApplicationHealthCheck with the provided metrics service.
   *
   * @param metricsService the metrics service to use for health checks
   */
  public ApplicationHealthCheck(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @Override
  protected Result check() {
    long errorCount = metricsService.getErrorCountLastMinute();
    double avgLatency = metricsService.getAverageLatencyLast60Seconds();

    boolean errorThresholdBreached = metricsService.isErrorThresholdBreached();
    boolean latencyThresholdBreached = metricsService.isLatencyThresholdBreached();

    if (errorThresholdBreached && latencyThresholdBreached) {
      return Result.unhealthy(
          "Critical: Both error and latency thresholds breached - "
              + "%d errors in last minute (threshold: %d), "
              + "%.1fms average latency in last 60 seconds (threshold: %.0fms)",
          errorCount,
          metricsService.getDefaultErrorThreshold(),
          avgLatency,
          metricsService.getDefaultLatencyThresholdMs());
    }

    if (errorThresholdBreached) {
      return Result.unhealthy(
          "Too many errors: %d errors in last minute (threshold: %d)",
          errorCount, metricsService.getDefaultErrorThreshold());
    }

    if (latencyThresholdBreached) {
      return Result.unhealthy(
          "High latency: %.1fms average latency in last 60 seconds (threshold: %.0fms)",
          avgLatency, metricsService.getDefaultLatencyThresholdMs());
    }

    return Result.healthy(
        "OK - %d errors in last minute (threshold: %d), "
            + "%.1fms average latency in last 60 seconds (threshold: %.0fms)",
        errorCount,
        metricsService.getDefaultErrorThreshold(),
        avgLatency,
        metricsService.getDefaultLatencyThresholdMs());
  }
}
