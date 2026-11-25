package com.stanlemon.healthy.dw5app.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.codahale.metrics.health.HealthCheck.Result;
import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Threshold rules are covered by HealthEvaluatorTest; this only checks the Dropwizard mapping. */
@DisplayName("ApplicationHealthCheck")
class ApplicationHealthCheckTest {

  private DefaultMetricsService metricsService;
  private ApplicationHealthCheck healthCheck;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    healthCheck = new ApplicationHealthCheck(new HealthEvaluator(metricsService));
  }

  @Test
  @DisplayName("Should report healthy when no traffic")
  void check_WhenNoTraffic_ShouldReportHealthy() {
    Result result = healthCheck.check();

    assertThat(result.isHealthy()).isTrue();
    assertThat(result.getMessage()).startsWith("OK - 0 errors");
  }

  @Test
  @DisplayName("Should report unhealthy with message when latency threshold breached")
  void check_WhenLatencyThresholdBreached_ShouldReportUnhealthyWithMessage() {
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequest(200, false);
    }

    Result result = healthCheck.check();

    assertThat(result.isHealthy()).isFalse();
    assertThat(result.getMessage()).startsWith("High latency: 200.0ms");
  }
}
