package com.stanlemon.healthy.spring4app.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@DisplayName("Application Health Indicator Tests")
class ApplicationHealthIndicatorTest {

  private DefaultMetricsService metricsService;
  private ApplicationHealthIndicator healthIndicator;
  private long errorThreshold;
  private double latencyThreshold;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    healthIndicator = new ApplicationHealthIndicator(new HealthEvaluator(metricsService));
    errorThreshold = metricsService.getDefaultErrorThreshold();
    latencyThreshold = metricsService.getDefaultLatencyThresholdMs();
  }

  @Test
  @DisplayName("Should report UP when all metrics are healthy")
  void health_WhenAllMetricsHealthy_ShouldReturnUp() {
    for (int i = 0; i < 5; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 100; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 0.5));
    }

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("errorsLastMinute", 5L);
    assertThat(health.getDetails().get("status").toString()).startsWith("OK");
  }

  @Test
  @DisplayName("Should report DOWN with error message when error threshold breached")
  void health_WhenErrorThresholdBreached_ShouldReturnDownWithErrorMessage() {
    int errorsToGenerate = (int) (errorThreshold * 1.5);
    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 0.5));
    }

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("errorThresholdBreached", true);
    assertThat(health.getDetails()).containsEntry("latencyThresholdBreached", false);
    assertThat(health.getDetails().get("status").toString()).startsWith("Too many errors");
  }

  @Test
  @DisplayName("Should report DOWN with latency message when latency threshold breached")
  void health_WhenLatencyThresholdBreached_ShouldReturnDownWithLatencyMessage() {
    for (int i = 0; i < 2; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
    }

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("errorThresholdBreached", false);
    assertThat(health.getDetails()).containsEntry("latencyThresholdBreached", true);
    assertThat(health.getDetails().get("status").toString()).startsWith("High latency");
  }

  @Test
  @DisplayName("Should report DOWN with critical message when both thresholds breached")
  void health_WhenBothThresholdsBreached_ShouldReturnDownWithCriticalMessage() {
    int errorsToGenerate = (int) (errorThreshold * 1.5);
    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
    }

    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("errorThresholdBreached", true);
    assertThat(health.getDetails()).containsEntry("latencyThresholdBreached", true);
    assertThat(health.getDetails().get("status").toString()).startsWith("Critical");
  }

  @Test
  @DisplayName("Should report UP when no metrics recorded")
  void health_WhenNoMetricsRecorded_ShouldReturnUp() {
    Health health = healthIndicator.health();

    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("errorsLastMinute", 0L);
    assertThat(health.getDetails()).containsEntry("avgLatencyLast60Seconds", 0.0);
    assertThat(health.getDetails().get("status").toString()).startsWith("OK");
  }
}
