package com.stanlemon.healthy.spring3app.filters;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

  @Mock private MetricsService metricsService;

  @Mock private HttpServletRequest request;

  @Mock private HttpServletResponse response;

  @Mock private FilterChain filterChain;

  private LatencyTrackingFilter filter;

  @BeforeEach
  void setUp() {
    filter = new LatencyTrackingFilter(metricsService);
  }

  @Test
  @DisplayName("Should record request latency for successful requests")
  void doFilterInternal_WhenSuccessful_ShouldRecordLatency() throws ServletException, IOException {
    // Setup request
    when(request.getRequestURI()).thenReturn("/test");

    // Call the method under test
    filter.doFilterInternal(request, response, filterChain);

    // Verify that filter chain was called
    verify(filterChain, times(1)).doFilter(request, response);

    // Verify that metrics were recorded
    verify(metricsService, times(1)).recordRequestLatency(anyLong());
  }

  @Test
  @DisplayName("Should record request latency even when filter chain throws exception")
  void doFilterInternal_WhenFilterChainThrowsException_ShouldStillRecordLatency()
      throws ServletException, IOException {
    // Setup request and filter chain to throw exception
    when(request.getRequestURI()).thenReturn("/test");
    doThrow(new ServletException("Test exception")).when(filterChain).doFilter(request, response);

    try {
      // Call the method under test (expecting exception)
      filter.doFilterInternal(request, response, filterChain);
    } catch (ServletException e) {
      // Exception is expected, continue with test
    }

    // Verify that metrics were still recorded despite the exception
    verify(metricsService, times(1)).recordRequestLatency(anyLong());
  }
}
