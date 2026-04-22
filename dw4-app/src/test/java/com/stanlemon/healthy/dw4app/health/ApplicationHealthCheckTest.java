package com.stanlemon.healthy.dw4app.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck.Result;
import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Application Health Check Tests")
class ApplicationHealthCheckTest {

  private DefaultMetricsService metricsService;
  private ApplicationHealthCheck healthCheck;
  private long errorThreshold;
  private double latencyThreshold;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    healthCheck = new ApplicationHealthCheck(new HealthEvaluator(metricsService));
    errorThreshold = metricsService.getDefaultErrorThreshold();
    latencyThreshold = metricsService.getDefaultLatencyThresholdMs();
  }

  @Test
  void check_WhenMetricsBelowThresholds_ShouldReportHealthy() {
    for (int i = 0; i < 5; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 100; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 0.8));
    }

    Result result = healthCheck.check();

    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
    assertThat(result.getMessage()).contains("5 errors");
  }

  @Test
  void check_WhenErrorThresholdBreached_ShouldReportUnhealthy() {
    int errorsToGenerate = (int) (errorThreshold * 1.5);
    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 0.5));
    }

    Result result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("Too many errors");
    assertThat(result.getMessage()).contains("threshold: " + errorThreshold);
  }

  @Test
  void check_WhenLatencyThresholdBreached_ShouldReportUnhealthy() {
    for (int i = 0; i < 2; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
    }

    Result result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).contains("High latency");
  }

  @Test
  void check_WhenBothThresholdsBreached_ShouldReportCritical() {
    int errorsToGenerate = (int) (errorThreshold * 1.5);
    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
    }

    Result result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage())
        .contains("Critical: Both error and latency thresholds breached");
  }

  @Test
  void check_WhenNoMetricsRecorded_ShouldReportHealthy() {
    Result result = healthCheck.check();

    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).contains("OK");
  }
}
