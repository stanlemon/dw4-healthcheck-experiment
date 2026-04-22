package com.stanlemon.healthy.spring3app.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
  @DisplayName("Should return 200 with healthy response when no thresholds breached")
  void getHealth_WhenHealthy_ShouldReturn200WithHealthyResponse() {
    ResponseEntity<HealthResponse> response = resource.getHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    HealthResponse health = response.getBody();
    assertThat(health.getStatus()).isEqualTo("healthy");
    assertThat(health.isHealthy()).isTrue();
    assertThat(health.getMessage()).startsWith("OK");
    assertThat(health.getErrorsLastMinute()).isZero();
    assertThat(health.isErrorThresholdBreached()).isFalse();
    assertThat(health.isLatencyThresholdBreached()).isFalse();
  }

  @Test
  @DisplayName("Should return 503 with unhealthy response when error threshold breached")
  void getHealth_WhenErrorThresholdBreached_ShouldReturn503WithUnhealthy() {
    int errorsToGenerate = (int) (errorThreshold * 1.5);
    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 0.5));
    }

    ResponseEntity<HealthResponse> response = resource.getHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();

    HealthResponse health = response.getBody();
    assertThat(health.getStatus()).isEqualTo("unhealthy");
    assertThat(health.isHealthy()).isFalse();
    assertThat(health.getMessage()).contains("Too many errors");
    assertThat(health.isErrorThresholdBreached()).isTrue();
    assertThat(health.isLatencyThresholdBreached()).isFalse();
  }

  @Test
  @DisplayName("Should return 503 with unhealthy response when latency threshold breached")
  void getHealth_WhenLatencyThresholdBreached_ShouldReturn503WithUnhealthy() {
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
    }

    ResponseEntity<HealthResponse> response = resource.getHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();

    HealthResponse health = response.getBody();
    assertThat(health.getStatus()).isEqualTo("unhealthy");
    assertThat(health.isHealthy()).isFalse();
    assertThat(health.getMessage()).contains("High latency");
    assertThat(health.isErrorThresholdBreached()).isFalse();
    assertThat(health.isLatencyThresholdBreached()).isTrue();
  }

  @Test
  @DisplayName("Should return 503 with critical response when both thresholds breached")
  void getHealth_WhenBothBreached_ShouldReturn503WithCritical() {
    int errorsToGenerate = (int) (errorThreshold * 1.5);
    for (int i = 0; i < errorsToGenerate; i++) {
      metricsService.recordServerError();
    }
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
    }

    ResponseEntity<HealthResponse> response = resource.getHealth();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();

    HealthResponse health = response.getBody();
    assertThat(health.getStatus()).isEqualTo("unhealthy");
    assertThat(health.isHealthy()).isFalse();
    assertThat(health.getMessage()).contains("Critical");
    assertThat(health.isErrorThresholdBreached()).isTrue();
    assertThat(health.isLatencyThresholdBreached()).isTrue();
  }
}
