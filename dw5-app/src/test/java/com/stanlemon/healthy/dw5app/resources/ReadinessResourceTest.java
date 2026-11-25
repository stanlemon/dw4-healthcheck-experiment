package com.stanlemon.healthy.dw5app.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
  @DisplayName("Should return 200 with health body when healthy")
  void getReadiness_WhenHealthy_ShouldReturn200WithHealthBody() {
    Response response = resource.getReadiness();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(((HealthResponse) response.getEntity()).getStatus()).isEqualTo("healthy");
  }

  @Test
  @DisplayName("Should return 503 with health body when unhealthy")
  void getReadiness_WhenUnhealthy_ShouldReturn503WithHealthBody() {
    for (int i = 0; i < 5; i++) {
      metricsService.recordRequest(200, false);
    }

    Response response = resource.getReadiness();

    assertThat(response.getStatus()).isEqualTo(503);
    HealthResponse health = (HealthResponse) response.getEntity();
    assertThat(health.getStatus()).isEqualTo("unhealthy");
    assertThat(health.isLatencyThresholdBreached()).isTrue();
  }
}
