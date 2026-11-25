package com.stanlemon.healthy.spring4app.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Threshold rules are covered by HealthEvaluatorTest; this only checks the HTTP status mapping. */
@DisplayName("ReadinessResource")
class ReadinessResourceTest {

  private DefaultMetricsService metricsService;
  private ReadinessResource resource;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    resource = new ReadinessResource(new HealthEvaluator(metricsService));
  }

  @Test
  void getReadiness_WhenHealthy_ShouldReturn200WithHealthBody() {
    ResponseEntity<HealthResponse> response = resource.getReadiness();

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody().getStatus()).isEqualTo("healthy");
  }

  @Test
  void getReadiness_WhenUnhealthy_ShouldReturn503WithHealthBody() {
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequest(200, false);
    }

    ResponseEntity<HealthResponse> response = resource.getReadiness();

    assertThat(response.getStatusCode().value()).isEqualTo(503);
    HealthResponse health = response.getBody();
    assertThat(health.getStatus()).isEqualTo("unhealthy");
    assertThat(health.isLatencyThresholdBreached()).isTrue();
  }
}
