package com.stanlemon.healthy.spring4app.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

@DisplayName("Latency Tracking Filter Tests")
@ExtendWith(MockitoExtension.class)
class LatencyTrackingFilterTest {

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private MetricsService metricsService;
  private LatencyTrackingFilter filter;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    filter = new LatencyTrackingFilter(metricsService);
    when(request.getRequestURI()).thenReturn("/test");
  }

  @Test
  @DisplayName("Should record request latency for successful requests")
  void doFilterInternal_WhenSuccessful_ShouldRecordLatency() throws ServletException, IOException {
    assertThat(metricsService.getTotalRequestCountLast60Seconds()).isZero();

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    assertThat(metricsService.getTotalRequestCountLast60Seconds()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should record request latency even when filter chain throws exception")
  void doFilterInternal_WhenFilterChainThrowsException_ShouldStillRecordLatency()
      throws ServletException, IOException {
    doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

    assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
        .isInstanceOf(ServletException.class)
        .hasMessage("Test exception");

    assertThat(metricsService.getTotalRequestCountLast60Seconds()).isEqualTo(1);
  }
}
