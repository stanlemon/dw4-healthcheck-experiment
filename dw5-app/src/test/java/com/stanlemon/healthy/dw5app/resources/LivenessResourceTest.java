package com.stanlemon.healthy.dw5app.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.LivenessEvaluator;
import com.stanlemon.healthy.metrics.LivenessResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Liveness Resource Tests")
class LivenessResourceTest {

  private DefaultMetricsService metricsService;
  private LivenessResource resource;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    resource = new LivenessResource(new LivenessEvaluator(metricsService));
  }

  @Test
  @DisplayName("Should return 200 when process is alive")
  void getLiveness_WhenAlive_ShouldReturn200() {
    for (int i = 0; i < 20; i++) {
      metricsService.recordRequestLatency(10);
    }

    Response response = resource.getLiveness();

    assertThat(response.getStatus()).isEqualTo(200);

    LivenessResponse liveness = (LivenessResponse) response.getEntity();
    assertThat(liveness.getStatus()).isEqualTo("alive");
    assertThat(liveness.isAlive()).isTrue();
  }

  @Test
  @DisplayName("Should return 200 when insufficient traffic to evaluate")
  void getLiveness_WhenInsufficientTraffic_ShouldReturn200() {
    Response response = resource.getLiveness();

    assertThat(response.getStatus()).isEqualTo(200);

    LivenessResponse liveness = (LivenessResponse) response.getEntity();
    assertThat(liveness.getStatus()).isEqualTo("alive");
    assertThat(liveness.isAlive()).isTrue();
  }

  @Test
  @DisplayName("Should return 503 when all requests fail")
  void getLiveness_WhenAllRequestsFail_ShouldReturn503() {
    for (int i = 0; i < 15; i++) {
      metricsService.recordServerError();
      metricsService.recordRequestLatency(100);
    }

    Response response = resource.getLiveness();

    assertThat(response.getStatus()).isEqualTo(503);

    LivenessResponse liveness = (LivenessResponse) response.getEntity();
    assertThat(liveness.getStatus()).isEqualTo("dead");
    assertThat(liveness.isAlive()).isFalse();
    assertThat(liveness.getMessage()).contains("All requests failing");
  }
}
