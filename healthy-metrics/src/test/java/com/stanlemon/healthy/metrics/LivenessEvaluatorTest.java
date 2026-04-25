package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Liveness Evaluator Tests")
class LivenessEvaluatorTest {

  private DefaultMetricsService metricsService;
  private LivenessEvaluator evaluator;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    evaluator = new LivenessEvaluator(metricsService);
  }

  @Nested
  @DisplayName("Alive state")
  class AliveState {

    @Test
    @DisplayName("Should return alive when no traffic")
    void evaluate_WhenNoTraffic_ShouldReturnAlive() {
      LivenessResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("alive");
      assertThat(response.isAlive()).isTrue();
      assertThat(response.getMessage()).contains("insufficient traffic");
      assertThat(response.getTotalRequestsLastMinute()).isZero();
    }

    @Test
    @DisplayName("Should return alive when traffic is below minimum sample size")
    void evaluate_WhenBelowMinimumSample_ShouldReturnAlive() {
      for (int i = 0; i < 5; i++) {
        metricsService.recordServerError();
        metricsService.recordRequestLatency(100);
      }

      LivenessResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("alive");
      assertThat(response.isAlive()).isTrue();
      assertThat(response.getMessage()).contains("insufficient traffic");
    }

    @Test
    @DisplayName("evaluate should return alive with insufficient traffic at exactly 9 requests")
    void evaluate_WhenExactly9Requests_ShouldReturnAliveInsufficientTraffic() {
      for (int i = 0; i < 9; i++) {
        metricsService.recordServerError();
        metricsService.recordRequestLatency(100);
      }

      LivenessResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("alive");
      assertThat(response.getMessage()).contains("insufficient traffic");
    }

    @Test
    @DisplayName("Should return alive when some requests succeed")
    void evaluate_WhenSomeRequestsSucceed_ShouldReturnAlive() {
      for (int i = 0; i < 8; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 15; i++) {
        metricsService.recordRequestLatency(50);
      }

      LivenessResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("alive");
      assertThat(response.isAlive()).isTrue();
      assertThat(response.getErrorsLastMinute()).isEqualTo(8);
      assertThat(response.getTotalRequestsLastMinute()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should return alive when all requests succeed")
    void evaluate_WhenAllRequestsSucceed_ShouldReturnAlive() {
      for (int i = 0; i < 20; i++) {
        metricsService.recordRequestLatency(10);
      }

      LivenessResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("alive");
      assertThat(response.isAlive()).isTrue();
      assertThat(response.getErrorsLastMinute()).isZero();
      assertThat(response.getTotalRequestsLastMinute()).isEqualTo(20);
    }
  }

  @Nested
  @DisplayName("Dead state")
  class DeadState {

    @Test
    @DisplayName("Should return dead when all requests fail with sufficient sample")
    void evaluate_WhenAllRequestsFail_ShouldReturnDead() {
      for (int i = 0; i < 15; i++) {
        metricsService.recordServerError();
        metricsService.recordRequestLatency(100);
      }

      LivenessResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("dead");
      assertThat(response.isAlive()).isFalse();
      assertThat(response.getMessage()).contains("All requests failing");
      assertThat(response.getErrorsLastMinute()).isEqualTo(15);
      assertThat(response.getTotalRequestsLastMinute()).isEqualTo(15);
    }

    @Test
    @DisplayName("Should return dead when all requests fail at exact minimum sample")
    void evaluate_WhenAllFailAtExactMinimum_ShouldReturnDead() {
      for (int i = 0; i < 10; i++) {
        metricsService.recordServerError();
        metricsService.recordRequestLatency(100);
      }

      LivenessResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("dead");
      assertThat(response.isAlive()).isFalse();
      assertThat(response.getErrorsLastMinute()).isEqualTo(10);
      assertThat(response.getTotalRequestsLastMinute()).isEqualTo(10);
    }

    @Test
    @DisplayName("evaluate should report dead when errors exceed total requests")
    void evaluate_WhenErrorsExceedTotalRequests_ShouldReturnDead() {
      // Record 20 errors and only 15 latency records (totalRequests=15)
      for (int i = 0; i < 20; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 15; i++) {
        metricsService.recordRequestLatency(100);
      }

      LivenessResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("dead");
      assertThat(response.isAlive()).isFalse();
    }
  }

  @Nested
  @DisplayName("LivenessResponse contract")
  class ResponseContract {

    @Test
    @DisplayName("isAlive should derive from status field")
    void livenessResponse_ShouldDeriveAliveFromStatus() {
      assertThat(new LivenessResponse("alive", "ok", 0, 10).isAlive()).isTrue();
      assertThat(new LivenessResponse("dead", "failing", 10, 10).isAlive()).isFalse();
    }
  }
}
