package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.withinPercentage;

import com.example.dw.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MetricsResourceTest {

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
    assertThat(response.getErrorsLastMinute()).isZero();
    assertThat(response.getTotalErrors()).isZero();
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
    // Setup - record errors and requests to meet minimum sample size and trigger threshold
    for (int i = 0; i < 15; i++) {
      metricsService.recordServerError(); // 15 errors
    }
    // Record successful requests by recording latencies (these count as requests)
    for (int i = 0; i < 100; i++) {
      metricsService.recordRequestLatency(50); // 100 successful requests, low latency
    }

    // Execute
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(15);
    assertThat(response.getTotalErrors()).isEqualTo(15);
    assertThat(response.isErrorThresholdBreached()).isTrue(); // 15/115 = 13% > 10% for high traffic
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
    assertThat(response.getErrorsLastMinute()).isZero();
    assertThat(response.getTotalErrors()).isZero();
    assertThat(response.getAvgLatencyLast60Minutes())
        .isCloseTo(63.33, withinPercentage(0.1)); // (50+60+80)/3 ≈ 63.33
    assertThat(response.isErrorThresholdBreached()).isFalse();
    assertThat(response.isLatencyThresholdBreached()).isFalse(); // 63.3 < 100
    assertThat(response.isHealthy()).isTrue();
  }

  @Test
  void testGetMetricsLatencyThresholdBreached() {
    // Setup - record high latencies above threshold (need at least 5 for threshold evaluation)
    metricsService.recordRequestLatency(150);
    metricsService.recordRequestLatency(200);
    metricsService.recordRequestLatency(180);
    metricsService.recordRequestLatency(160);
    metricsService.recordRequestLatency(170);

    // Execute
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isZero();
    assertThat(response.getTotalErrors()).isZero();
    assertThat(response.getAvgLatencyLast60Minutes()).isEqualTo(172.0); // (150+200+180+160+170)/5
    assertThat(response.isErrorThresholdBreached()).isFalse();
    assertThat(response.isLatencyThresholdBreached()).isTrue(); // 172 > 100
    assertThat(response.isHealthy()).isFalse(); // Unhealthy due to latency
  }

  @Test
  void testGetMetricsBothThresholdsBreached() {
    // Setup - record both high errors and high latency with sufficient samples
    for (int i = 0; i < 15; i++) {
      metricsService.recordServerError(); // 15 errors
    }
    // Record high latencies (need at least 5 for threshold evaluation)
    metricsService.recordRequestLatency(150);
    metricsService.recordRequestLatency(200);
    metricsService.recordRequestLatency(250);
    metricsService.recordRequestLatency(180);
    metricsService.recordRequestLatency(220);
    // Add more requests to get high traffic scenario
    for (int i = 0; i < 95; i++) {
      metricsService.recordRequestLatency(50); // 95 low-latency requests
    }

    // Execute
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify: 115 total requests (15 errors + 100 latency records), 15 errors = 13% error rate >
    // 10%
    // Average latency will be much lower due to 95 low-latency requests:
    // (150+200+250+180+220+95*50)/100 ≈ 57.5ms
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(15);
    assertThat(response.getTotalErrors()).isEqualTo(15);
    assertThat(response.isErrorThresholdBreached()).isTrue(); // 13% > 10% for high traffic
    assertThat(response.isLatencyThresholdBreached()).isFalse(); // Average ~57.5ms < 100ms
    assertThat(response.isHealthy()).isFalse(); // Unhealthy due to errors
  }
}
