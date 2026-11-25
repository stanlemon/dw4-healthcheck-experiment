package com.stanlemon.healthy.dw5app.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.LivenessEvaluator;
import com.stanlemon.healthy.metrics.LivenessResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Liveness rules are covered by LivenessEvaluatorTest; this only checks the HTTP status mapping.
 */
@DisplayName("LivenessResource")
class LivenessResourceTest {

  private DefaultMetricsService metricsService;
  private LivenessResource resource;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    resource = new LivenessResource(new LivenessEvaluator(metricsService));
  }

  @Test
  void getLiveness_WhenAlive_ShouldReturn200WithLivenessBody() {
    Response response = resource.getLiveness();

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(((LivenessResponse) response.getEntity()).getStatus()).isEqualTo("alive");
  }

  @Test
  void getLiveness_WhenEveryRequestFails_ShouldReturn503WithLivenessBody() {
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequest(100, true);
    }

    Response response = resource.getLiveness();

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(((LivenessResponse) response.getEntity()).getStatus()).isEqualTo("dead");
  }
}
