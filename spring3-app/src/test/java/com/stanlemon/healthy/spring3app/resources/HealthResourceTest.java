package com.stanlemon.healthy.spring3app.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Health Resource Tests")
class HealthResourceTest {

  private DefaultMetricsService metricsService;
  private HealthResource resource;
  private long errorThreshold;
  private double latencyThreshold;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    resource = new HealthResource(new HealthEvaluator(metricsService));
    errorThreshold = metricsService.getDefaultErrorThreshold();
    latencyThreshold = metricsService.getDefaultLatencyThresholdMs();
  }

  @Test
  @DisplayName("Should return healthy response when no thresholds breached")
  void getHealth_WhenHealthy_ShouldReturnHealthyResponse() {
    HealthResponse response = resource.getHealth();

    assertThat(response.getStatus()).isEqualTo("healthy");
    assertThat(response.isHealthy()).isTrue();
    assertThat(response.getMessage()).startsWith("OK");
    assertThat(response.getErrorsLastMinute()).isZero();
    assertThat(response.isErrorThresholdBreached()).isFalse();
    assertThat(response.isLatencyThresholdBreached()).isFalse();
  }

  @Test
  @DisplayName("Should return unhealthy response when error threshold breached")
  void getHealth_WhenErrorThresholdBreached_ShouldReturnUnhealthy() {
    int errorsToGenerate = (int) (errorThreshold * 1.5);
    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 0.5));
    }

    HealthResponse response = resource.getHealth();

    assertThat(response.getStatus()).isEqualTo("unhealthy");
    assertThat(response.isHealthy()).isFalse();
    assertThat(response.getMessage()).contains("Too many errors");
    assertThat(response.isErrorThresholdBreached()).isTrue();
    assertThat(response.isLatencyThresholdBreached()).isFalse();
  }

  @Test
  @DisplayName("Should return unhealthy response when latency threshold breached")
  void getHealth_WhenLatencyThresholdBreached_ShouldReturnUnhealthy() {
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
    }

    HealthResponse response = resource.getHealth();

    assertThat(response.getStatus()).isEqualTo("unhealthy");
    assertThat(response.isHealthy()).isFalse();
    assertThat(response.getMessage()).contains("High latency");
    assertThat(response.isErrorThresholdBreached()).isFalse();
    assertThat(response.isLatencyThresholdBreached()).isTrue();
  }

  @Test
  @DisplayName("Should return critical when both thresholds breached")
  void getHealth_WhenBothBreached_ShouldReturnCritical() {
    int errorsToGenerate = (int) (errorThreshold * 1.5);
    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
    }

    HealthResponse response = resource.getHealth();

    assertThat(response.getStatus()).isEqualTo("unhealthy");
    assertThat(response.isHealthy()).isFalse();
    assertThat(response.getMessage()).contains("Critical");
    assertThat(response.isErrorThresholdBreached()).isTrue();
    assertThat(response.isLatencyThresholdBreached()).isTrue();
  }
}
