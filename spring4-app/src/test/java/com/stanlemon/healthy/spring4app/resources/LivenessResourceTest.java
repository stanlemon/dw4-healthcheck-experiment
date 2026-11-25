package com.stanlemon.healthy.spring4app.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.LivenessEvaluator;
import com.stanlemon.healthy.metrics.LivenessResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

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
  @DisplayName("Should return 200 with liveness body when alive")
  void getLiveness_WhenAlive_ShouldReturn200WithLivenessBody() {
    ResponseEntity<LivenessResponse> response = resource.getLiveness();

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody().getStatus()).isEqualTo("alive");
  }

  @Test
  @DisplayName("Should return 503 with liveness body when every request fails")
  void getLiveness_WhenEveryRequestFails_ShouldReturn503WithLivenessBody() {
    for (int i = 0; i < 10; i++) {
      metricsService.recordRequest(100, true);
    }

    ResponseEntity<LivenessResponse> response = resource.getLiveness();

    assertThat(response.getStatusCode().value()).isEqualTo(503);
    assertThat(response.getBody().getStatus()).isEqualTo("dead");
  }
}
