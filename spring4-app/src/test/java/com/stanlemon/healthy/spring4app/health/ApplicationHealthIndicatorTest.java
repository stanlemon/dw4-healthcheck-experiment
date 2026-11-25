package com.stanlemon.healthy.spring4app.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

/** Threshold rules are covered by HealthEvaluatorTest; this only checks the Spring mapping. */
@DisplayName("ApplicationHealthIndicator")
class ApplicationHealthIndicatorTest {

  private DefaultMetricsService metricsService;
  private ApplicationHealthIndicator healthIndicator;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    healthIndicator = new ApplicationHealthIndicator(new HealthEvaluator(metricsService));
  }

  @Test
  @DisplayName("Should report up without breach details when no traffic")
  void health_WhenNoTraffic_ShouldReportUpWithoutBreachDetails() {
    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails())
        .containsEntry("errorsLastMinute", 0L)
        .containsEntry("avgLatencyLast60Seconds", 0.0)
        .doesNotContainKey("errorThresholdBreached");
    assertThat(health.getDetails().get("status").toString()).startsWith("OK");
  }

  @Test
  @DisplayName("Should report down with breach details when latency threshold breached")
  void health_WhenLatencyThresholdBreached_ShouldReportDownWithBreachDetails() {
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequest(200, false);
    }

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails())
        .containsEntry("avgLatencyLast60Seconds", 200.0)
        .containsEntry("errorThresholdBreached", false)
        .containsEntry("latencyThresholdBreached", true);
    assertThat(health.getDetails().get("status").toString()).startsWith("High latency");
  }
}
