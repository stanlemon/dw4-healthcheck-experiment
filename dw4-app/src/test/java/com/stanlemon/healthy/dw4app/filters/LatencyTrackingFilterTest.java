package com.stanlemon.healthy.dw4app.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("LatencyTrackingFilter")
class LatencyTrackingFilterTest {

  @Mock private HttpServletRequest mockRequest;
  @Mock private HttpServletResponse mockResponse;
  @Mock private FilterChain mockFilterChain;

  private MetricsService metricsService;
  private LatencyTrackingFilter filter;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    filter = new LatencyTrackingFilter(metricsService);
  }

  @Test
  @DisplayName("Should record request latency for successful requests")
  void doFilter_WhenRequestSucceeds_ShouldRecordLatency() throws ServletException, IOException {
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isZero();

    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    assertThat(metricsService.getTotalRequestCountLast60Seconds()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should record request latency even when filter chain throws exception")
  void doFilter_WhenFilterChainThrowsException_ShouldStillRecordLatency()
      throws ServletException, IOException {
    doThrow(new ServletException("Test exception"))
        .when(mockFilterChain)
        .doFilter(mockRequest, mockResponse);

    assertThatThrownBy(() -> filter.doFilter(mockRequest, mockResponse, mockFilterChain))
        .isInstanceOf(ServletException.class)
        .hasMessage("Test exception");

    assertThat(metricsService.getTotalRequestCountLast60Seconds()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should calculate correct average for multiple requests")
  void doFilter_WhenMultipleRequests_ShouldCalculateCorrectAverage()
      throws ServletException, IOException {
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);
    filter.doFilter(mockRequest, mockResponse, mockFilterChain);

    assertThat(metricsService.getTotalRequestCountLast60Seconds()).isEqualTo(3);
    assertThat(metricsService.getAverageLatencyLast60Seconds()).isNotNaN();
  }
}
