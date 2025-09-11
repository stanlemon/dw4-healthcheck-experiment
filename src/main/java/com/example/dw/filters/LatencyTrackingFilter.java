package com.example.dw.filters;

import com.example.dw.metrics.MetricsService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * JAX-RS filter that automatically tracks request latency for all HTTP requests. Records timestamps
 * on request entry and calculates elapsed time on response completion.
 */
@Provider
public class LatencyTrackingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  /** Property key for storing request start timestamp in request context. */
  private static final String REQUEST_START_TIME = "requestStartTime";

  /** Metrics service for recording latency measurements. */
  private final MetricsService metricsService;

  /**
   * Creates a new latency tracking filter.
   *
   * @param metricsService the metrics service to use for recording latency
   */
  public LatencyTrackingFilter(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /**
   * Records the request start timestamp for latency calculation.
   *
   * @param requestContext the request context
   * @throws IOException if an error occurs
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    requestContext.setProperty(REQUEST_START_TIME, System.currentTimeMillis());
  }

  /**
   * Calculates and records request latency using the stored start timestamp.
   *
   * @param requestContext the request context containing the start timestamp
   * @param responseContext the response context (unused)
   * @throws IOException if an error occurs
   */
  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    Object startTimeObj = requestContext.getProperty(REQUEST_START_TIME);

    if (startTimeObj instanceof Long startTime) {
      long endTime = System.currentTimeMillis();
      long latencyMs = endTime - startTime;
      metricsService.recordRequestLatency(latencyMs);
    }
  }
}
