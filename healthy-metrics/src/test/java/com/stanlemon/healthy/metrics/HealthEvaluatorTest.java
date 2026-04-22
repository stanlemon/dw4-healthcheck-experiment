package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Health Evaluator Tests")
class HealthEvaluatorTest {

  private DefaultMetricsService metricsService;
  private HealthEvaluator evaluator;
  private long errorThreshold;
  private double latencyThreshold;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    evaluator = new HealthEvaluator(metricsService);
    errorThreshold = metricsService.getDefaultErrorThreshold();
    latencyThreshold = metricsService.getDefaultLatencyThresholdMs();
  }

  @Nested
  @DisplayName("Healthy state")
  class HealthyState {

    @Test
    @DisplayName("Should return healthy when no metrics recorded")
    void evaluate_WhenNoMetrics_ShouldReturnHealthy() {
      HealthResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("healthy");
      assertThat(response.isHealthy()).isTrue();
      assertThat(response.getMessage()).startsWith("OK");
      assertThat(response.getErrorsLastMinute()).isZero();
      assertThat(response.getAvgLatencyLast60Seconds()).isEqualTo(0.0);
      assertThat(response.isErrorThresholdBreached()).isFalse();
      assertThat(response.isLatencyThresholdBreached()).isFalse();
    }

    @Test
    @DisplayName("Should return healthy when metrics below thresholds")
    void evaluate_WhenBelowThresholds_ShouldReturnHealthy() {
      for (int i = 0; i < 5; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 100; i++) {
        metricsService.recordRequestLatency((long) (latencyThreshold * 0.5));
      }

      HealthResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("healthy");
      assertThat(response.isHealthy()).isTrue();
      assertThat(response.getMessage()).contains("OK");
      assertThat(response.getErrorsLastMinute()).isEqualTo(5);
    }

    @Test
    @DisplayName("Should include threshold values in healthy message")
    void evaluate_WhenHealthy_ShouldIncludeThresholdValues() {
      HealthResponse response = evaluator.evaluate();

      assertThat(response.getMessage()).contains("threshold: " + errorThreshold);
      assertThat(response.getMessage()).contains("threshold: " + (long) latencyThreshold + "ms");
    }
  }

  @Nested
  @DisplayName("Error threshold breached")
  class ErrorBreached {

    @Test
    @DisplayName("Should return unhealthy when error threshold breached")
    void evaluate_WhenErrorThresholdBreached_ShouldReturnUnhealthy() {
      int errorsToGenerate = (int) (errorThreshold * 1.5);
      for (int i = 0; i < errorsToGenerate; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 10; i++) {
        metricsService.recordRequestLatency((long) (latencyThreshold * 0.5));
      }

      HealthResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("unhealthy");
      assertThat(response.isHealthy()).isFalse();
      assertThat(response.getMessage()).startsWith("Too many errors");
      assertThat(response.getMessage()).contains(errorsToGenerate + " errors");
      assertThat(response.isErrorThresholdBreached()).isTrue();
      assertThat(response.isLatencyThresholdBreached()).isFalse();
    }
  }

  @Nested
  @DisplayName("Latency threshold breached")
  class LatencyBreached {

    @Test
    @DisplayName("Should return unhealthy when latency threshold breached")
    void evaluate_WhenLatencyThresholdBreached_ShouldReturnUnhealthy() {
      for (int i = 0; i < 2; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 5; i++) {
        metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
      }

      HealthResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("unhealthy");
      assertThat(response.isHealthy()).isFalse();
      assertThat(response.getMessage()).startsWith("High latency");
      assertThat(response.isErrorThresholdBreached()).isFalse();
      assertThat(response.isLatencyThresholdBreached()).isTrue();
    }
  }

  @Nested
  @DisplayName("Both thresholds breached")
  class BothBreached {

    @Test
    @DisplayName("Should return critical when both thresholds breached")
    void evaluate_WhenBothBreached_ShouldReturnCritical() {
      int errorsToGenerate = (int) (errorThreshold * 1.5);
      for (int i = 0; i < errorsToGenerate; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 10; i++) {
        metricsService.recordRequestLatency((long) (latencyThreshold * 2.0));
      }

      HealthResponse response = evaluator.evaluate();

      assertThat(response.getStatus()).isEqualTo("unhealthy");
      assertThat(response.isHealthy()).isFalse();
      assertThat(response.getMessage()).startsWith("Critical");
      assertThat(response.getMessage()).contains("Both error and latency");
      assertThat(response.isErrorThresholdBreached()).isTrue();
      assertThat(response.isLatencyThresholdBreached()).isTrue();
    }
  }

  @Nested
  @DisplayName("HealthResponse serialization")
  class ResponseContract {

    @Test
    @DisplayName("HealthResponse isHealthy should derive from threshold flags")
    void healthResponse_ShouldDeriveHealthyFromFlags() {
      assertThat(new HealthResponse("healthy", "ok", 0, 0.0, false, false).isHealthy()).isTrue();
      assertThat(new HealthResponse("unhealthy", "err", 10, 0.0, true, false).isHealthy())
          .isFalse();
      assertThat(new HealthResponse("unhealthy", "lat", 0, 200.0, false, true).isHealthy())
          .isFalse();
      assertThat(new HealthResponse("unhealthy", "crit", 10, 200.0, true, true).isHealthy())
          .isFalse();
    }
  }
}
