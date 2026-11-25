package com.stanlemon.healthy.spring4app.resources;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Metric math is covered by DefaultMetricsServiceTest; this only checks the endpoint wiring. */
@DisplayName("MetricsResource")
class MetricsResourceTest {

  @Test
  @DisplayName("Should report current snapshot when traffic recorded")
  void getMetrics_WhenTrafficRecorded_ShouldReportCurrentSnapshot() {
    DefaultMetricsService metricsService = new DefaultMetricsService();
    metricsService.recordRequest(300, true);
    for (int i = 0; i < 4; i++) {
      metricsService.recordRequest(50, false);
    }

    MetricsResponse response = new MetricsResource(metricsService).getMetrics();

    assertThat(response).isEqualTo(new MetricsResponse(1, 1, 100.0, false, false));
  }
}
