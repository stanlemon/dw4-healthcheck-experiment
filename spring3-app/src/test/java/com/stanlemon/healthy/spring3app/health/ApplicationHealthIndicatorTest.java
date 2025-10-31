package com.stanlemon.healthy.spring3app.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.stanlemon.healthy.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

@DisplayName("Application Health Indicator Tests")
@ExtendWith(MockitoExtension.class)
class ApplicationHealthIndicatorTest {

  @Mock private MetricsService metricsService;

  private ApplicationHealthIndicator healthIndicator;

  @BeforeEach
  void setUp() {
    healthIndicator = new ApplicationHealthIndicator(metricsService);
  }

  @Test
  @DisplayName("Should report UP when all metrics are healthy")
  void health_WhenAllMetricsHealthy_ShouldReturnUp() {
    // Setup mock service
    when(metricsService.isErrorThresholdBreached()).thenReturn(false);
    when(metricsService.isLatencyThresholdBreached()).thenReturn(false);
    when(metricsService.getErrorCountLastMinute()).thenReturn(0L);
    when(metricsService.getAverageLatencyLast60Seconds()).thenReturn(50.0);

    // Call the method under test
    Health health = healthIndicator.health();

    // Verify the health status
    assertThat(health.getStatus()).isEqualTo(Status.UP);
    assertThat(health.getDetails()).containsEntry("errorsLastMinute", 0L);
    assertThat(health.getDetails()).containsEntry("avgLatencyLast60Seconds", 50.0);
    assertThat(health.getDetails()).containsKey("status");
  }

  @Test
  @DisplayName("Should report DOWN when error threshold is breached")
  void health_WhenErrorThresholdBreached_ShouldReturnDown() {
    // Setup mock service
    when(metricsService.isErrorThresholdBreached()).thenReturn(true);
    when(metricsService.isLatencyThresholdBreached()).thenReturn(false);
    when(metricsService.getErrorCountLastMinute()).thenReturn(10L);
    when(metricsService.getAverageLatencyLast60Seconds()).thenReturn(50.0);

    // Call the method under test
    Health health = healthIndicator.health();

    // Verify the health status
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("errorsLastMinute", 10L);
    assertThat(health.getDetails()).containsEntry("errorThresholdBreached", true);
    assertThat(health.getDetails()).containsEntry("latencyThresholdBreached", false);
    assertThat(health.getDetails()).containsKey("status");
  }

  @Test
  @DisplayName("Should report DOWN when latency threshold is breached")
  void health_WhenLatencyThresholdBreached_ShouldReturnDown() {
    // Setup mock service
    when(metricsService.isErrorThresholdBreached()).thenReturn(false);
    when(metricsService.isLatencyThresholdBreached()).thenReturn(true);
    when(metricsService.getErrorCountLastMinute()).thenReturn(0L);
    when(metricsService.getAverageLatencyLast60Seconds()).thenReturn(150.0);

    // Call the method under test
    Health health = healthIndicator.health();

    // Verify the health status
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("avgLatencyLast60Seconds", 150.0);
    assertThat(health.getDetails()).containsEntry("errorThresholdBreached", false);
    assertThat(health.getDetails()).containsEntry("latencyThresholdBreached", true);
    assertThat(health.getDetails()).containsKey("status");
  }

  @Test
  @DisplayName("Should report DOWN when both thresholds are breached")
  void health_WhenBothThresholdsBreached_ShouldReturnDown() {
    // Setup mock service
    when(metricsService.isErrorThresholdBreached()).thenReturn(true);
    when(metricsService.isLatencyThresholdBreached()).thenReturn(true);
    when(metricsService.getErrorCountLastMinute()).thenReturn(10L);
    when(metricsService.getAverageLatencyLast60Seconds()).thenReturn(150.0);

    // Call the method under test
    Health health = healthIndicator.health();

    // Verify the health status
    assertThat(health.getStatus()).isEqualTo(Status.DOWN);
    assertThat(health.getDetails()).containsEntry("errorThresholdBreached", true);
    assertThat(health.getDetails()).containsEntry("latencyThresholdBreached", true);
    assertThat(health.getDetails()).containsKey("status");
  }
}
