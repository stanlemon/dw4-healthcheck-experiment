package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HealthEvaluator")
class HealthEvaluatorTest {

  private DefaultMetricsService metricsService;
  private HealthEvaluator evaluator;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    evaluator = new HealthEvaluator(metricsService);
  }

  private void record(int failed, int succeeded, long latencyMs) {
    for (int i = 0; i < failed; i++) {
      metricsService.recordRequest(latencyMs, true);
    }
    for (int i = 0; i < succeeded; i++) {
      metricsService.recordRequest(latencyMs, false);
    }
  }

  @Test
  void evaluate_WhenNoTraffic_ShouldReportHealthyWithThresholds() {
    assertThat(evaluator.evaluate())
        .isEqualTo(
            new HealthResponse(
                "healthy",
                "OK - 0 errors in last minute (threshold: 100), "
                    + "0.0ms average latency in last 60 seconds (threshold: 100ms)",
                0,
                0.0,
                false,
                false));
  }

  @Test
  void evaluate_WhenOnlyErrorThresholdBreached_ShouldReportTooManyErrors() {
    record(15, 5, 50);

    assertThat(evaluator.evaluate())
        .isEqualTo(
            new HealthResponse(
                "unhealthy",
                "Too many errors: 15 errors in last minute (threshold: 100)",
                15,
                50.0,
                true,
                false));
  }

  @Test
  void evaluate_WhenOnlyLatencyThresholdBreached_ShouldReportHighLatency() {
    record(0, 10, 200);

    assertThat(evaluator.evaluate())
        .isEqualTo(
            new HealthResponse(
                "unhealthy",
                "High latency: 200.0ms average latency in last 60 seconds (threshold: 100ms)",
                0,
                200.0,
                false,
                true));
  }

  @Test
  void evaluate_WhenBothThresholdsBreached_ShouldReportCritical() {
    record(15, 5, 200);

    assertThat(evaluator.evaluate())
        .isEqualTo(
            new HealthResponse(
                "unhealthy",
                "Critical: Both error and latency thresholds breached - "
                    + "15 errors in last minute (threshold: 100), "
                    + "200.0ms average latency in last 60 seconds (threshold: 100ms)",
                15,
                200.0,
                true,
                true));
  }
}
