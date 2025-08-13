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
              + "%d errors in last minute (threshold: 100), "
              + "%.1fms average latency in last 60 minutes (threshold: 500ms)",
          errorCount, avgLatency);
    }

    if (errorThresholdBreached) {
      return Result.unhealthy(
          "Too many errors: %d errors in last minute (threshold: 100)", errorCount);
    }

    if (latencyThresholdBreached) {
      return Result.unhealthy(
          "High latency: %.1fms average latency in last 60 minutes (threshold: 500ms)", avgLatency);
    }

    return Result.healthy(
        "OK - %d errors in last minute (threshold: 100), "
            + "%.1fms average latency in last 60 minutes (threshold: 500ms)",
        errorCount, avgLatency);
  }
}
