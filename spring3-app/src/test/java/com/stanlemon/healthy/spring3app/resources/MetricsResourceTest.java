package com.stanlemon.healthy.spring3app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.stanlemon.healthy.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("Metrics Resource Tests")
@ExtendWith(MockitoExtension.class)
class MetricsResourceTest {

  @Mock private MetricsService metricsService;

  private MetricsResource resource;

  @BeforeEach
  void setUp() {
    resource = new MetricsResource(metricsService);
  }

  @Test
  @DisplayName("Should return metrics from metrics service")
  void getMetrics_ShouldReturnMetricsFromService() {
    // Setup mock service
    when(metricsService.getErrorCountLastMinute()).thenReturn(5L);
    when(metricsService.getTotalErrorCount()).thenReturn(10L);
    when(metricsService.getAverageLatencyLast60Seconds()).thenReturn(150.0);
    when(metricsService.isErrorThresholdBreached()).thenReturn(false);
    when(metricsService.isLatencyThresholdBreached()).thenReturn(false);

    // Call the method under test
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify the response contains the expected values
    assertThat(response).isNotNull();
    assertThat(response.getErrorsLastMinute()).isEqualTo(5L);
    assertThat(response.getTotalErrors()).isEqualTo(10L);
    assertThat(response.getAvgLatencyLast60Seconds()).isEqualTo(150.0);
    assertThat(response.isErrorThresholdBreached()).isFalse();
    assertThat(response.isLatencyThresholdBreached()).isFalse();
    assertThat(response.isHealthy()).isTrue();

    // Verify that all methods were called on the service
    verify(metricsService, times(1)).getErrorCountLastMinute();
    verify(metricsService, times(1)).getTotalErrorCount();
    verify(metricsService, times(1)).getAverageLatencyLast60Seconds();
    verify(metricsService, times(1)).isErrorThresholdBreached();
    verify(metricsService, times(1)).isLatencyThresholdBreached();
  }

  @Test
  @DisplayName("Should report unhealthy when error threshold breached")
  void getMetrics_WhenErrorThresholdBreached_ShouldReportUnhealthy() {
    // Setup mock service with error threshold breach
    when(metricsService.isErrorThresholdBreached()).thenReturn(true);
    when(metricsService.isLatencyThresholdBreached()).thenReturn(false);

    // Call the method under test
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify health status is correctly determined
    assertThat(response.isErrorThresholdBreached()).isTrue();
    assertThat(response.isHealthy()).isFalse();
  }

  @Test
  @DisplayName("Should report unhealthy when latency threshold breached")
  void getMetrics_WhenLatencyThresholdBreached_ShouldReportUnhealthy() {
    // Setup mock service with latency threshold breach
    when(metricsService.isErrorThresholdBreached()).thenReturn(false);
    when(metricsService.isLatencyThresholdBreached()).thenReturn(true);

    // Call the method under test
    MetricsResource.MetricsResponse response = resource.getMetrics();

    // Verify health status is correctly determined
    assertThat(response.isLatencyThresholdBreached()).isTrue();
    assertThat(response.isHealthy()).isFalse();
  }

  @Test
  @DisplayName("MetricsResponse isHealthy should work correctly")
  void metricsResponse_isHealthy_ShouldWorkAsExpected() {
    // Test healthy case
    MetricsResource.MetricsResponse healthyResponse =
        new MetricsResource.MetricsResponse(0, 0, 0.0, false, false);
    assertThat(healthyResponse.isHealthy()).isTrue();

    // Test error threshold breach
    MetricsResource.MetricsResponse errorResponse =
        new MetricsResource.MetricsResponse(10, 20, 50.0, true, false);
    assertThat(errorResponse.isHealthy()).isFalse();

    // Test latency threshold breach
    MetricsResource.MetricsResponse latencyResponse =
        new MetricsResource.MetricsResponse(0, 0, 200.0, false, true);
    assertThat(latencyResponse.isHealthy()).isFalse();

    // Test both thresholds breached
    MetricsResource.MetricsResponse bothResponse =
        new MetricsResource.MetricsResponse(10, 20, 200.0, true, true);
    assertThat(bothResponse.isHealthy()).isFalse();
  }
}
