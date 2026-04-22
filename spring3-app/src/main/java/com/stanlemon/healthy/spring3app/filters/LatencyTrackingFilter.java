package com.stanlemon.healthy.spring3app.filters;

import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that measures and records request latency for all HTTP requests.
 *
 * <p>This filter tracks how long each request takes to process and records the latency in the
 * MetricsService. It captures timing data for all requests that pass through the filter chain.
 */
@Component
@Slf4j
public class LatencyTrackingFilter extends OncePerRequestFilter {

  private final MetricsService metricsService;

  /**
   * Constructs a new LatencyTrackingFilter with the provided metrics service.
   *
   * @param metricsService the metrics service to record latency data
   */
  public LatencyTrackingFilter(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    final long startTime = System.currentTimeMillis();
    try {
      filterChain.doFilter(request, response);
    } finally {
      final long latencyMs = System.currentTimeMillis() - startTime;
      metricsService.recordRequestLatency(latencyMs);

      log.debug("Request to {} took {}ms to process", request.getRequestURI(), latencyMs);
    }
  }
}
