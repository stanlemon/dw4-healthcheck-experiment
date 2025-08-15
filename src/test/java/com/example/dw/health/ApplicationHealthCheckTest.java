package com.example.dw.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck.Result;
import com.example.dw.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApplicationHealthCheckTest {

  private MetricsService metricsService;
  private ApplicationHealthCheck healthCheck;
  private long errorThreshold;
  private double latencyThreshold;

  @BeforeEach
  void setUp() {
    // Get the real MetricsService and clear its state for test isolation
    metricsService = MetricsService.getInstance();
    metricsService.clearMetrics();
    healthCheck = new ApplicationHealthCheck();

    // Get the current thresholds dynamically
    errorThreshold = MetricsService.getDefaultErrorThreshold();
    latencyThreshold = MetricsService.getDefaultLatencyThresholdMs();
  }

  @Test
  void check_WhenMetricsBelowThresholds_ShouldReportHealthy() {
    // Setup - record errors and latency below thresholds with sufficient traffic
    long errorsToGenerate = 5; // Low error count
    double latencyToUse = latencyThreshold * 0.8; // 80% of latency threshold

    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    // Record sufficient requests for threshold evaluation and maintain low error rate
    for (int i = 0; i < 100; i++) {
      metricsService.recordRequestLatency((long) latencyToUse);
    }

    // Execute - Total: 105 requests, 5 errors = 4.8% error rate < 10% threshold, latency <
    // threshold
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
    assertThat(result.getMessage()).contains(errorsToGenerate + " errors");
    assertThat(result.getMessage()).contains(latencyToUse + "ms");
  }

  @Test
  void check_WhenErrorThresholdBreached_ShouldReportUnhealthy() {
    // Setup - record errors above threshold, latency OK
    int errorsToGenerate = (int) (errorThreshold * 1.5); // 150% of threshold
    long latencyToUse = (long) (latencyThreshold * 0.8); // 80% of threshold

    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency(latencyToUse); // Below latency threshold
    }

    // Execute
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("Too many errors");
    assertThat(result.getMessage()).contains(errorsToGenerate + " errors");
    assertThat(result.getMessage()).contains("threshold: " + errorThreshold);
  }

  @Test
  void check_WhenLatencyThresholdBreached_ShouldReportUnhealthy() {
    // Setup - record errors below threshold but latency above threshold
    long errorsToGenerate = 2; // Low error count
    long latencyToUse = (long) (latencyThreshold * 2.0); // 200% of threshold

    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    // Record sufficient latency samples for threshold evaluation (need at least 5)
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequestLatency(latencyToUse); // Above latency threshold
    }

    // Execute - Total: 7 requests, 2 errors, high latency but insufficient total requests for error
    // thresholds
    Result result = healthCheck.check();

    // Verify - Should be unhealthy due to latency (errors ignored due to insufficient sample size)
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("High latency");
    assertThat(result.getMessage()).contains(latencyToUse + ".0ms");
    assertThat(result.getMessage()).contains("threshold: " + (long) latencyThreshold + "ms");
  }

  @Test
  void check_WhenBothThresholdsBreached_ShouldReportCritical() {
    // Setup - both thresholds breached
    int errorsToGenerate = (int) (errorThreshold * 1.5); // 150% of threshold
    long latencyToUse = (long) (latencyThreshold * 2.0); // 200% of threshold

    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency(latencyToUse); // Above latency threshold
    }

    // Execute
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage())
        .contains("Critical: Both error and latency thresholds breached");
    assertThat(result.getMessage()).contains(errorsToGenerate + " errors");
    assertThat(result.getMessage()).contains(latencyToUse + ".0ms");
  }

  @Test
  void check_WhenMetricsExactlyAtThresholds_ShouldStillReportHealthy() {
    // Setup - at exact thresholds (should not breach)
    int errorsToGenerate = 10; // 10% error rate exactly at threshold
    long latencyToUse = (long) latencyThreshold; // Exactly at threshold

    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    // Record sufficient requests for proper threshold evaluation
    for (int i = 0; i < 100; i++) {
      metricsService.recordRequestLatency(latencyToUse); // Exactly at threshold
    }

    // Execute - Total: 110 requests, 10 errors = 9.1% error rate < 10% threshold, latency =
    // threshold
    Result result = healthCheck.check();

    // Verify - should still be healthy at exactly the thresholds (not greater than)
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
    assertThat(result.getMessage()).contains(errorsToGenerate + " errors");
    assertThat(result.getMessage()).contains(latencyToUse + ".0ms");
  }

  @Test
  void check_WhenNoErrorsOrLatency_ShouldReportHealthy() {
    // Setup - no errors or latency data (clearMetrics already called in setUp)

    // Execute
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
    assertThat(result.getMessage()).contains("0 errors");
    assertThat(result.getMessage()).contains("0.0ms");
  }
}
