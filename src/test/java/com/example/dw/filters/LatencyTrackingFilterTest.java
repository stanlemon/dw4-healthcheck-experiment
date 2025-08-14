package com.example.dw.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
public class LatencyTrackingFilterTest {

  @Mock private ContainerRequestContext mockRequestContext;

  @Mock private ContainerResponseContext mockResponseContext;

  private MetricsService metricsService;
  private LatencyTrackingFilter filter;

  @BeforeEach
  void setUp() {
    metricsService = MetricsService.getInstance();
    metricsService.clearMetrics(); // Start with clean state
    filter = new LatencyTrackingFilter();
  }

  @Test
  void testRequestFilter() throws IOException {
    // Execute
    filter.filter(mockRequestContext);

    // Verify that start time is set (we can't check exact value due to timing)
    verify(mockRequestContext).setProperty(eq("requestStartTime"), any(Long.class));
  }

  @Test
  void testResponseFilterWithValidStartTime() throws IOException {
    // Setup - simulate a start time from 100ms ago
    long startTime = System.currentTimeMillis() - 100;
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn(startTime);

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

    // Execute
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was recorded and is reasonable (should be around 100ms)
    double avgLatency = metricsService.getAverageLatencyLast60Minutes();
    assertThat(avgLatency).isGreaterThanOrEqualTo(90.0).isLessThanOrEqualTo(1000.0);
  }

  @Test
  void testResponseFilterWithNullStartTime() throws IOException {
    // Setup - no start time property
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn(null);

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

    // Execute
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was NOT recorded (should still be 0)
    assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);
  }

  @Test
  void testResponseFilterWithInvalidStartTimeType() throws IOException {
    // Setup - start time property is not a Long
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn("not a long");

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

    // Execute
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was NOT recorded (should still be 0)
    assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);
  }

  @Test
  void testResponseFilterWithZeroLatency() throws IOException {
    // Setup - start time is current time (should result in very small latency)
    long startTime = System.currentTimeMillis();
    when(mockRequestContext.getProperty("requestStartTime")).thenReturn(startTime);

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

    // Execute
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was recorded (should be close to 0 but not negative)
    double avgLatency = metricsService.getAverageLatencyLast60Minutes();
    assertThat(avgLatency).isGreaterThanOrEqualTo(0.0).isLessThanOrEqualTo(50.0);
  }

  @Test
  void testCompleteRequestResponseCycle() throws IOException {
    // Step 1: Request filter
    filter.filter(mockRequestContext);

    // Verify start time was set
    verify(mockRequestContext).setProperty(eq("requestStartTime"), any(Long.class));

    // Step 2: Simulate some processing time
    try {
      Thread.sleep(10); // Sleep for 10ms
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Step 3: Setup for response filter - capture the start time that was set
    when(mockRequestContext.getProperty("requestStartTime"))
        .thenAnswer(
            invocation -> {
              // Return a time from ~10ms ago
              return System.currentTimeMillis() - 10;
            });

    // Verify initial state - no latency recorded
    assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

    // Step 4: Response filter
    filter.filter(mockRequestContext, mockResponseContext);

    // Verify latency was recorded with a reasonable value
    double avgLatency = metricsService.getAverageLatencyLast60Minutes();
    assertThat(avgLatency).isGreaterThanOrEqualTo(5.0).isLessThanOrEqualTo(100.0);
  }

  @Test
  void testMultipleRequests() throws IOException {
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
    double avgLatency = metricsService.getAverageLatencyLast60Minutes();
    assertThat(avgLatency).isGreaterThanOrEqualTo(150.0).isLessThanOrEqualTo(350.0);
  }
}
