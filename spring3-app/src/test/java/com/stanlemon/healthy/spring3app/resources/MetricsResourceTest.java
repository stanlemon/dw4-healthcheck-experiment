package com.stanlemon.healthy.spring3app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsResponse;
import com.stanlemon.healthy.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Metrics Resource Tests")
class MetricsResourceTest {

  private MetricsService metricsService;
  private MetricsResource resource;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    resource = new MetricsResource(metricsService);
  }

  @Nested
  @DisplayName("Initial metrics state tests")
  class InitialStateTests {

    @Test
    @DisplayName("Should return healthy initial state when no metrics recorded")
    void getMetrics_WhenNoMetricsRecorded_ShouldReturnHealthyInitialState() {
      MetricsResponse response = resource.getMetrics();

      assertThat(response).isNotNull();
      assertThat(response.getErrorsLastMinute()).isZero();
      assertThat(response.getTotalErrors()).isZero();
      assertThat(response.getAvgLatencyLast60Seconds()).isZero();
      assertThat(response.isErrorThresholdBreached()).isFalse();
      assertThat(response.isLatencyThresholdBreached()).isFalse();
      assertThat(response.isHealthy()).isTrue();
    }
  }

  @Nested
  @DisplayName("Error threshold tests")
  class ErrorThresholdTests {

    @Test
    @DisplayName("Should return healthy state when errors below threshold")
    void getMetrics_WhenErrorsBelowThreshold_ShouldReturnHealthyState() {
      for (int i = 0; i < 50; i++) {
        metricsService.recordServerError();
      }

      MetricsResponse response = resource.getMetrics();

      assertThat(response).isNotNull();
      assertThat(response.getErrorsLastMinute()).isEqualTo(50);
      assertThat(response.getTotalErrors()).isEqualTo(50);
      assertThat(response.getAvgLatencyLast60Seconds()).isZero();
      assertThat(response.isErrorThresholdBreached()).isFalse();
      assertThat(response.isLatencyThresholdBreached()).isFalse();
      assertThat(response.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should return unhealthy state when error threshold breached")
    void getMetrics_WhenErrorThresholdBreached_ShouldReturnUnhealthyState() {
      for (int i = 0; i < 15; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 100; i++) {
        metricsService.recordRequestLatency(50);
      }

      MetricsResponse response = resource.getMetrics();

      assertThat(response).isNotNull();
      assertThat(response.getErrorsLastMinute()).isEqualTo(15);
      assertThat(response.getTotalErrors()).isEqualTo(15);
      assertThat(response.isErrorThresholdBreached()).isTrue();
      assertThat(response.isLatencyThresholdBreached()).isFalse();
      assertThat(response.isHealthy()).isFalse();
    }
  }

  @Nested
  @DisplayName("Latency threshold tests")
  class LatencyThresholdTests {

    @Test
    @DisplayName("Should return healthy state when latency below threshold")
    void getMetrics_WhenLatencyBelowThreshold_ShouldReturnHealthyState() {
      metricsService.recordRequestLatency(50);
      metricsService.recordRequestLatency(60);
      metricsService.recordRequestLatency(80);

      MetricsResponse response = resource.getMetrics();

      assertThat(response).isNotNull();
      assertThat(response.getErrorsLastMinute()).isZero();
      assertThat(response.getTotalErrors()).isZero();
      assertThat(response.getAvgLatencyLast60Seconds()).isCloseTo(63.33, withinPercentage(0.1));
      assertThat(response.isErrorThresholdBreached()).isFalse();
      assertThat(response.isLatencyThresholdBreached()).isFalse();
      assertThat(response.isHealthy()).isTrue();
    }

    @Test
    @DisplayName("Should return unhealthy state when latency threshold breached")
    void getMetrics_WhenLatencyThresholdBreached_ShouldReturnUnhealthyState() {
      metricsService.recordRequestLatency(150);
      metricsService.recordRequestLatency(200);
      metricsService.recordRequestLatency(180);
      metricsService.recordRequestLatency(160);
      metricsService.recordRequestLatency(170);

      MetricsResponse response = resource.getMetrics();

      assertThat(response).isNotNull();
      assertThat(response.getErrorsLastMinute()).isZero();
      assertThat(response.getTotalErrors()).isZero();
      assertThat(response.getAvgLatencyLast60Seconds()).isEqualTo(172.0);
      assertThat(response.isErrorThresholdBreached()).isFalse();
      assertThat(response.isLatencyThresholdBreached()).isTrue();
      assertThat(response.isHealthy()).isFalse();
    }

    @Test
    @DisplayName("Should return unhealthy state when error threshold breached with valid latency")
    void getMetrics_WhenErrorThresholdBreachedWithValidLatency_ShouldReturnUnhealthyState() {
      for (int i = 0; i < 15; i++) {
        metricsService.recordServerError();
      }
      metricsService.recordRequestLatency(150);
      metricsService.recordRequestLatency(200);
      metricsService.recordRequestLatency(250);
      metricsService.recordRequestLatency(180);
      metricsService.recordRequestLatency(220);
      for (int i = 0; i < 95; i++) {
        metricsService.recordRequestLatency(50);
      }

      MetricsResponse response = resource.getMetrics();

      assertThat(response).isNotNull();
      assertThat(response.getErrorsLastMinute()).isEqualTo(15);
      assertThat(response.getTotalErrors()).isEqualTo(15);
      assertThat(response.isErrorThresholdBreached()).isTrue();
      assertThat(response.isLatencyThresholdBreached()).isFalse();
      assertThat(response.isHealthy()).isFalse();
    }
  }

  @Nested
  @DisplayName("MetricsResponse isHealthy tests")
  class MetricsResponseHealthyTests {

    @Test
    @DisplayName("MetricsResponse isHealthy should work correctly")
    void metricsResponse_isHealthy_ShouldWorkAsExpected() {
      assertThat(new MetricsResponse(0, 0, 0.0, false, false).isHealthy()).isTrue();
      assertThat(new MetricsResponse(10, 20, 50.0, true, false).isHealthy()).isFalse();
      assertThat(new MetricsResponse(0, 0, 200.0, false, true).isHealthy()).isFalse();
      assertThat(new MetricsResponse(10, 20, 200.0, true, true).isHealthy()).isFalse();
    }
  }
}
