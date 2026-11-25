package com.stanlemon.healthy.spring4app.filters;

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
 * Filter that records every finished request in the {@link MetricsService}: its latency, and
 * whether it ended in a 5xx response. This is the only place metrics are recorded, so a request and
 * its error always land together.
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
    // Stays true if the chain throws, so an exception that escapes every handler still counts.
    boolean threw = true;
    try {
      filterChain.doFilter(request, response);
      threw = false;
    } finally {
      final long latencyMs = System.currentTimeMillis() - startTime;
      // Skip health-probe and actuator traffic. Kubernetes polls /health/* every few seconds
      // with tiny response bodies; counting them in the 60-second average drags it toward zero
      // and masks real latency regressions.
      if (!isHealthProbe(request.getRequestURI())) {
        metricsService.recordRequest(latencyMs, threw || response.getStatus() >= 500);
      }

      // Request paths are untrusted, so omit them to keep each debug event on one line.
      log.debug("Request took {}ms to process", latencyMs);
    }
  }

  private static boolean isHealthProbe(String uri) {
    return uri != null
        && (uri.startsWith("/health/") || uri.equals("/health") || uri.startsWith("/actuator/"));
  }
}
