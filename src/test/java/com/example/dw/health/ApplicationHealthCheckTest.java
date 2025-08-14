package com.example.dw.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck.Result;
import com.example.dw.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApplicationHealthCheckTest {

  private MetricsService metricsService;
  private ApplicationHealthCheck healthCheck;

  @BeforeEach
  void setUp() {
    // Get the real MetricsService and clear its state for test isolation
    metricsService = MetricsService.getInstance();
    metricsService.clearMetrics();
    healthCheck = new ApplicationHealthCheck();
  }

  @Test
  void testCheckHealthy() {
    // Setup - record errors and latency below thresholds
    for (int i = 0; i < 50; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency(300); // 300ms per request = 300ms average
    }

    // Execute
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
    assertThat(result.getMessage()).contains("50 errors");
    assertThat(result.getMessage()).contains("300.0ms");
  }

  @Test
  void testCheckErrorThresholdBreached() {
    // Setup - record errors above threshold (100), latency OK
    for (int i = 0; i < 150; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency(300); // 300ms per request = 300ms average
    }

    // Execute
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("Too many errors");
    assertThat(result.getMessage()).contains("150 errors");
    assertThat(result.getMessage()).contains("threshold: 100");
  }

  @Test
  void testCheckLatencyThresholdBreached() {
    // Setup - latency threshold breached (500ms+), errors OK
    for (int i = 0; i < 50; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency(700); // 700ms per request = 700ms average
    }

    // Execute
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("High latency");
    assertThat(result.getMessage()).contains("700.0ms");
    assertThat(result.getMessage()).contains("threshold: 500ms");
  }

  @Test
  void testCheckBothThresholdsBreached() {
    // Setup - both thresholds breached
    for (int i = 0; i < 150; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency(700); // 700ms per request = 700ms average
    }

    // Execute
    Result result = healthCheck.check();

    // Verify
    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage())
        .contains("Critical: Both error and latency thresholds breached");
    assertThat(result.getMessage()).contains("150 errors");
    assertThat(result.getMessage()).contains("700.0ms");
  }

  @Test
  void testCheckAtExactThresholds() {
    // Setup - at exact thresholds (should not breach)
    for (int i = 0; i < 100; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency(500); // 500ms per request = 500ms average
    }

    // Execute
    Result result = healthCheck.check();

    // Verify - should still be healthy at exactly the thresholds
    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
    assertThat(result.getMessage()).contains("100 errors");
    assertThat(result.getMessage()).contains("500.0ms");
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
