package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LivenessEvaluator")
class LivenessEvaluatorTest {

  private DefaultMetricsService metricsService;
  private LivenessEvaluator evaluator;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    evaluator = new LivenessEvaluator(metricsService);
  }

  private void record(int failed, int succeeded) {
    for (int i = 0; i < failed; i++) {
      metricsService.recordRequest(100, true);
    }
    for (int i = 0; i < succeeded; i++) {
      metricsService.recordRequest(100, false);
    }
  }

  @Test
  @DisplayName("Should report alive for insufficient traffic when nine failing requests")
  void evaluate_WhenNineFailingRequests_ShouldReportAliveForInsufficientTraffic() {
    record(9, 0);

    assertThat(evaluator.evaluate())
        .isEqualTo(
            new LivenessResponse(
                "alive", "OK - insufficient traffic to evaluate (9 requests, minimum 10)", 9, 9));
  }

  @Test
  @DisplayName("Should report dead when all ten requests fail")
  void evaluate_WhenAllTenRequestsFail_ShouldReportDead() {
    record(10, 0);

    assertThat(evaluator.evaluate())
        .isEqualTo(
            new LivenessResponse(
                "dead",
                "All requests failing: 10 errors out of 10 requests in last minute",
                10,
                10));
  }

  @Test
  @DisplayName("Should report alive when one request succeeds")
  void evaluate_WhenOneRequestSucceeds_ShouldReportAlive() {
    record(14, 1);

    assertThat(evaluator.evaluate())
        .isEqualTo(
            new LivenessResponse(
                "alive", "OK - 14 errors out of 15 requests in last minute", 14, 15));
  }
}
