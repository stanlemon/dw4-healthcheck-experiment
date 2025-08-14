package com.example.dw.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck.Result;
import com.example.dw.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationHealthCheckTest {

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
  void testCheckHealthy() {
    // Setup - record errors and latency below thresholds
    long errorsToGenerate = errorThreshold / 2; // Half the error threshold
    double latencyToUse = latencyThreshold * 0.8; // 80% of latency threshold

    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) latencyToUse);
    }

    // Execute
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
    assertThat(result.getMessage()).contains(errorsToGenerate + " errors");
    assertThat(result.getMessage()).contains(latencyToUse + "ms");
  }

  @Test
  void testCheckErrorThresholdBreached() {
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
  void testCheckLatencyThresholdBreached() {
    // Setup - latency threshold breached, errors OK
    int errorsToGenerate = (int) (errorThreshold / 2); // 50% of threshold
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
    assertThat(result.getMessage()).contains("High latency");
    assertThat(result.getMessage()).contains(latencyToUse + ".0ms");
    assertThat(result.getMessage()).contains("threshold: " + (long) latencyThreshold + "ms");
  }

  @Test
  void testCheckBothThresholdsBreached() {
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
  void testCheckAtExactThresholds() {
    // Setup - at exact thresholds (should not breach)
    int errorsToGenerate = (int) errorThreshold; // Exactly at threshold
    long latencyToUse = (long) latencyThreshold; // Exactly at threshold

    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency(latencyToUse); // Exactly at threshold
    }

    // Execute
    Result result = healthCheck.check();

    // Verify - should still be healthy at exactly the thresholds
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
    assertThat(result.getMessage()).contains(errorsToGenerate + " errors");
    assertThat(result.getMessage()).contains(latencyToUse + ".0ms");
  }

  @Test
  void testCheckWithZeroValues() {
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
