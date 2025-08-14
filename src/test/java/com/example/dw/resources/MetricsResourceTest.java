package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import com.example.dw.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetricsResourceTest {

  private MetricsService metricsService;
  private MetricsResource resource;

  @BeforeEach
  void setUp() {
    metricsService = MetricsService.getInstance();
    metricsService.clearMetrics(); // Start with clean state
    resource = new MetricsResource();
  }

  @Test
  void testGetMetricsInitialState() {
    // Execute - with clean metrics
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify - initial state should be healthy with no errors or latency
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(0);
    assertThat(response.getTotalErrors()).isEqualTo(0);
    assertThat(response.getAvgLatencyLast60Minutes()).isEqualTo(0.0);
    assertThat(response.isErrorThresholdBreached()).isFalse();
    assertThat(response.isLatencyThresholdBreached()).isFalse();
    assertThat(response.isHealthy()).isTrue();
  }

  @Test
  void testGetMetricsWithErrors() {
    // Setup - record some errors below threshold
    for (int i = 0; i < 50; i++) {
      metricsService.recordServerError();
    }

    // Execute
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(50);
    assertThat(response.getTotalErrors()).isEqualTo(50);
    assertThat(response.getAvgLatencyLast60Minutes()).isEqualTo(0.0); // No latency recorded
    assertThat(response.isErrorThresholdBreached()).isFalse(); // 50 < 100
    assertThat(response.isLatencyThresholdBreached()).isFalse();
    assertThat(response.isHealthy()).isTrue();
  }

  @Test
  void testGetMetricsErrorThresholdBreached() {
    // Setup - record errors above threshold
    for (int i = 0; i < 120; i++) {
      metricsService.recordServerError();
    }

    // Execute
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(120);
    assertThat(response.getTotalErrors()).isEqualTo(120);
    assertThat(response.isErrorThresholdBreached()).isTrue(); // 120 > 100
    assertThat(response.isLatencyThresholdBreached()).isFalse();
    assertThat(response.isHealthy()).isFalse(); // Unhealthy due to errors
  }

  @Test
  void testGetMetricsWithLatency() {
    // Setup - record some latencies below threshold
    metricsService.recordRequestLatency(50);
    metricsService.recordRequestLatency(60);
    metricsService.recordRequestLatency(80);

    // Execute
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(0);
    assertThat(response.getTotalErrors()).isEqualTo(0);
    assertThat(response.getAvgLatencyLast60Minutes())
        .isCloseTo(63.33, withinPercentage(0.1)); // (50+60+80)/3 ≈ 63.33
    assertThat(response.isErrorThresholdBreached()).isFalse();
    assertThat(response.isLatencyThresholdBreached()).isFalse(); // 63.3 < 100
    assertThat(response.isHealthy()).isTrue();
  }

  @Test
  void testGetMetricsLatencyThresholdBreached() {
    // Setup - record high latencies above threshold
    metricsService.recordRequestLatency(150);
    metricsService.recordRequestLatency(200);

    // Execute
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(0);
    assertThat(response.getTotalErrors()).isEqualTo(0);
    assertThat(response.getAvgLatencyLast60Minutes()).isEqualTo(175.0); // (150+200)/2
    assertThat(response.isErrorThresholdBreached()).isFalse();
    assertThat(response.isLatencyThresholdBreached()).isTrue(); // 175 > 100
    assertThat(response.isHealthy()).isFalse(); // Unhealthy due to latency
  }

  @Test
  void testGetMetricsBothThresholdsBreached() {
    // Setup - record both high errors and high latency
    for (int i = 0; i < 150; i++) {
      metricsService.recordServerError();
    }
    metricsService.recordRequestLatency(150);
    metricsService.recordRequestLatency(200);
    metricsService.recordRequestLatency(250);

    // Execute
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(150);
    assertThat(response.getTotalErrors()).isEqualTo(150);
    assertThat(response.getAvgLatencyLast60Minutes())
        .isCloseTo(200.0, withinPercentage(0.1)); // (150+200+250)/3 = 200
    assertThat(response.isErrorThresholdBreached()).isTrue(); // 150 > 100
    assertThat(response.isLatencyThresholdBreached()).isTrue(); // 200 > 100
    assertThat(response.isHealthy()).isFalse(); // Unhealthy due to both
  }
}
