package com.example.dw.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.dw.metrics.DefaultMetricsService;
import com.example.dw.metrics.MetricsService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LatencyTrackingFilterTest {

  @Mock private ContainerRequestContext mockRequestContext;

  @Mock private ContainerResponseContext mockResponseContext;

  private MetricsService metricsService;
  private LatencyTrackingFilter filter;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    metricsService.clearMetrics(); // Start with clean state
    filter = new LatencyTrackingFilter(metricsService);
  }

  @Test
  void filter_WhenRequestReceived_ShouldSetStartTime() throws IOException {
    // Execute
    filter.filter(mockRequestContext);

    // Verify that start time is set (we can't check exact value due to timing)
    verify(mockRequestContext).setProperty(eq("requestStartTime"), any(Long.class));
  }

  @Test
  void filter_WhenValidStartTime_ShouldRecordLatency() throws IOException {
    // Setup - simulate a start time from 100ms ago
    long startTime = System.currentTimeMillis() - 100;
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn(startTime);

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

    // Execute
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was recorded and is reasonable (should be around 100ms)
    double avgLatency = metricsService.getAverageLatencyLast60Seconds();
    assertThat(avgLatency).isGreaterThanOrEqualTo(90.0).isLessThanOrEqualTo(1000.0);
  }

  @Test
  void filter_WhenNullStartTime_ShouldNotRecordLatency() throws IOException {
    // Setup - no start time property
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn(null);

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

    // Execute
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was NOT recorded (should still be 0)
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
  }

  @Test
  void filter_WhenInvalidTimeType_ShouldNotRecordLatency() throws IOException {
    // Setup - start time property is not a Long
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn("not a long");

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

    // Execute
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was NOT recorded (should still be 0)
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
  }

  @Test
  void filter_WhenZeroLatency_ShouldRecordSmallPositiveLatency() throws IOException {
    // Setup - start time is current time (should result in very small latency)
    long startTime = System.currentTimeMillis();
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn(startTime);

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

    // Execute
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was recorded (should be close to 0 but not negative)
    double avgLatency = metricsService.getAverageLatencyLast60Seconds();
    assertThat(avgLatency).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(50.0);
  }

  @Test
  void filter_WhenCompleteCycle_ShouldRecordAccurateLatency() throws IOException {
    // Step 1: Request filter - this should set the start time
    filter.filter(mockRequestContext);

    // Verify start time was set
    verify(mockRequestContext).setProperty(eq("requestStartTime"), any(Long.class));

    // Step 2: Setup for response filter - simulate a realistic request processing time
    // Instead of using a sleep, we'll mock the start time to be from a specific time ago
    long simulatedStartTime = System.currentTimeMillis() - 50; // Simulate 50ms processing time
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn(simulatedStartTime);

    // Verify initial state - no latency recorded yet
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

    // Step 3: Response filter - this should calculate and record the latency
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was recorded with a reasonable value (should be around 50ms)
    double avgLatency = metricsService.getAverageLatencyLast60Seconds();
    assertThat(avgLatency).isGreaterThanOrEqualTo(40.0).isLessThanOrEqualTo(100.0);
  }

  @Test
  void filter_WhenMultipleRequests_ShouldCalculateCorrectAverage() throws IOException {
    // Record multiple latencies to test averaging
    when(mockRequestContext.getProperty("requestStartTime"))
        .thenReturn(System.currentTimeMillis() - 100) // First: 100ms
        .thenReturn(System.currentTimeMillis() - 200) // Second: 200ms
        .thenReturn(System.currentTimeMillis() - 300); // Third: 300ms

    // Execute multiple response filters
    filter.filter(mockRequestContext, mockResponseContext);
    filter.filter(mockRequestContext, mockResponseContext);
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify average latency is reasonable (should be around 200ms average)
    double avgLatency = metricsService.getAverageLatencyLast60Seconds();
    assertThat(avgLatency).isGreaterThanOrEqualTo(150.0).isLessThanOrEqualTo(350.0);
  }
}
