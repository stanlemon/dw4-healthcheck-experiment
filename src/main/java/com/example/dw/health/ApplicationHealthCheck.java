package com.example.dw.health;

import com.codahale.metrics.health.HealthCheck;
import com.example.dw.metrics.MetricsService;

public class ApplicationHealthCheck extends HealthCheck {
  private final MetricsService metricsService;

  public ApplicationHealthCheck() {
    this.metricsService = MetricsService.getInstance();
  }

  @Override
  protected Result check() {
    long errorCount = metricsService.getErrorCountLastMinute();
    double avgLatency = metricsService.getAverageLatencyLast60Minutes();

    boolean errorThresholdBreached = metricsService.isErrorThresholdBreached();
    boolean latencyThresholdBreached = metricsService.isLatencyThresholdBreached();

    if (errorThresholdBreached && latencyThresholdBreached) {
      return Result.unhealthy(
          "Critical: Both error and latency thresholds breached - "
              + "%d errors in last minute (threshold: %d), "
              + "%.1fms average latency in last 60 minutes (threshold: %.0fms)",
          errorCount,
          MetricsService.getDefaultErrorThreshold(),
          avgLatency,
          MetricsService.getDefaultLatencyThresholdMs());
    }

    if (errorThresholdBreached) {
      return Result.unhealthy(
          "Too many errors: %d errors in last minute (threshold: %d)",
          errorCount, MetricsService.getDefaultErrorThreshold());
    }

    if (latencyThresholdBreached) {
      return Result.unhealthy(
          "High latency: %.1fms average latency in last 60 minutes (threshold: %.0fms)",
          avgLatency, MetricsService.getDefaultLatencyThresholdMs());
    }

    return Result.healthy(
        "OK - %d errors in last minute (threshold: %d), "
            + "%.1fms average latency in last 60 minutes (threshold: %.0fms)",
        errorCount,
        MetricsService.getDefaultErrorThreshold(),
        avgLatency,
        MetricsService.getDefaultLatencyThresholdMs());
  }
}
