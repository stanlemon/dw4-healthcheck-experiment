package com.stanlemon.healthy.dw4app.filters;

import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet filter that measures and records request latency for all HTTP requests.
 *
 * <p>This filter tracks how long each request takes to process and records the latency in the
 * MetricsService. It captures timing data for all requests that pass through the filter chain,
 * including requests that result in exceptions.
 */
@Slf4j
public class LatencyTrackingFilter extends HttpFilter {

  private final MetricsService metricsService;

  /**
   * Creates a new latency tracking filter.
   *
   * @param metricsService the metrics service to use for recording latency
   */
  public LatencyTrackingFilter(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @Override
  protected void doFilter(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    final long startTime = System.currentTimeMillis();
    try {
      chain.doFilter(request, response);
    } finally {
      final long latencyMs = System.currentTimeMillis() - startTime;
      metricsService.recordRequestLatency(latencyMs);

      log.debug("Request to {} took {}ms to process", request.getRequestURI(), latencyMs);
    }
  }
}
