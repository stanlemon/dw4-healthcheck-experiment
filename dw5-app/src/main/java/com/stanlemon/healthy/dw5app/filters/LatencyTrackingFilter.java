package com.stanlemon.healthy.dw5app.filters;

import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;

/**
 * Servlet filter that records every finished request in the {@link MetricsService}: its latency,
 * and whether it ended in a 5xx response. This is the only place metrics are recorded, so a request
 * and its error always land together.
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
    // Stays true if the chain throws, so an exception that escapes every mapper still counts.
    boolean threw = true;
    try {
      chain.doFilter(request, response);
      threw = false;
    } finally {
      final long latencyMs = System.currentTimeMillis() - startTime;
      // Skip health-probe traffic. Kubernetes polls /health/* every few seconds with tiny
      // response bodies; counting them in the 60-second average drags it toward zero and
      // masks real latency regressions.
      if (!isHealthProbe(request.getRequestURI())) {
        metricsService.recordRequest(latencyMs, isServerError(response, threw));
      }

      // Request paths are untrusted, so omit them to keep each debug event on one line.
      log.debug("Request took {}ms to process", latencyMs);
    }
  }

  /**
   * A request is a server error if the client got a 5xx status, or if the chain threw before any
   * status was sent (the container then answers 500). A throw after the response was committed
   * means the status already went out, usually because the client hung up mid-body, so the sent
   * status decides.
   */
  private static boolean isServerError(HttpServletResponse response, boolean threw) {
    if (threw && !response.isCommitted()) {
      return true;
    }
    int status = response.getStatus();
    return status >= 500 && status < 600;
  }

  private static boolean isHealthProbe(String uri) {
    return uri != null
        && (uri.startsWith("/health/") || uri.equals("/health") || uri.startsWith("/healthcheck"));
  }
}
