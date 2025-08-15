package com.example.dw.filters;

import com.example.dw.metrics.MetricsService;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * A JAX-RS filter that automatically tracks request latency for all incoming HTTP requests.
 *
 * <p>This filter implements both {@link ContainerRequestFilter} and {@link ContainerResponseFilter}
 * to measure the complete request processing time from when the request enters the application
 * until the response is ready to be sent back to the client.
 *
 * <p><strong>How it works:</strong>
 *
 * <ul>
 *   <li>On request arrival: Records the current timestamp and stores it in the request context
 *   <li>On response completion: Retrieves the start timestamp, calculates the elapsed time, and
 *       records the latency metrics
 * </ul>
 *
 * <p><strong>Metrics Integration:</strong>
 *
 * <p>The measured latency is automatically recorded in the {@link MetricsService} which:
 *
 * <ul>
 *   <li>Maintains a 60-minute sliding window of latency measurements
 *   <li>Calculates average latency over the past 60 minutes
 *   <li>Provides threshold-based health monitoring (default: 500ms)
 *   <li>Integrates with the application's health check system
 * </ul>
 *
 * <p><strong>Usage:</strong>
 *
 * <p>This filter is automatically registered as a {@code @Provider} and will be applied to all
 * JAX-RS resources in the application. No additional configuration is required.
 *
 * <p><strong>Thread Safety:</strong>
 *
 * <p>This filter is thread-safe. Each request gets its own context, and the underlying {@link
 * MetricsService} uses atomic operations for thread-safe metric recording.
 *
 * @author Generated
 * @since 1.0
 * @see MetricsService#recordRequestLatency(long)
 * @see ContainerRequestFilter
 * @see ContainerResponseFilter
 */
@Provider
public class LatencyTrackingFilter implements ContainerRequestFilter, ContainerResponseFilter {

  /**
   * Property key used to store the request start timestamp in the JAX-RS request context. This
   * allows us to retrieve the timestamp in the response filter to calculate latency.
   */
  private static final String REQUEST_START_TIME = "requestStartTime";

  /**
   * The metrics service instance used to record latency measurements. Uses the singleton pattern to
   * ensure consistent metrics across the application.
   */
  private final MetricsService metricsService;

  /**
   * Constructs a new LatencyTrackingFilter.
   *
   * <p>Initializes the filter with the provided {@link MetricsService} instance to ensure all
   * latency measurements are recorded in the central metrics system.
   *
   * @param metricsService the metrics service to use
   */
  public LatencyTrackingFilter(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /**
   * Request filter method that executes before the request is processed by the resource method.
   *
   * <p>This method is called for every incoming HTTP request and is responsible for recording the
   * start timestamp that will be used later to calculate request latency.
   *
   * <p><strong>Implementation Details:</strong>
   *
   * <ul>
   *   <li>Uses {@link System#currentTimeMillis()} for timestamp accuracy
   *   <li>Stores the timestamp in the request context properties for later retrieval
   *   <li>The stored property survives throughout the request lifecycle
   * </ul>
   *
   * <p><strong>Performance Impact:</strong>
   *
   * <p>This method has minimal performance overhead as it only performs a single system call and a
   * property assignment.
   *
   * @param requestContext the JAX-RS request context containing request information and properties
   *     that persist throughout the request lifecycle
   * @throws IOException if an I/O error occurs during request processing (rarely thrown)
   * @see ContainerRequestFilter#filter(ContainerRequestContext)
   * @see #filter(ContainerRequestContext, ContainerResponseContext)
   */
  @Override
  public void filter(ContainerRequestContext requestContext) throws IOException {
    // Record the precise start time when the request enters our application
    // This timestamp will be used in the response filter to calculate total latency
    requestContext.setProperty(REQUEST_START_TIME, System.currentTimeMillis());
  }

  /**
   * Response filter method that executes after the resource method has processed the request but
   * before the response is sent to the client.
   *
   * <p>This method is responsible for calculating the request latency by comparing the current time
   * with the start timestamp recorded in the request filter.
   *
   * <p><strong>Latency Calculation:</strong>
   *
   * <ul>
   *   <li>Retrieves the start timestamp from the request context properties
   *   <li>Calculates elapsed time: {@code endTime - startTime}
   *   <li>Records the latency in milliseconds to the metrics service
   * </ul>
   *
   * <p><strong>Error Handling:</strong>
   *
   * <ul>
   *   <li>Gracefully handles missing start timestamp (no latency recorded)
   *   <li>Uses pattern matching to ensure type safety when retrieving the timestamp
   *   <li>Does not throw exceptions that could disrupt response processing
   * </ul>
   *
   * <p><strong>Metrics Integration:</strong>
   *
   * <p>The calculated latency is immediately recorded in the {@link MetricsService} which will
   * update the sliding window metrics and trigger health check evaluations if latency thresholds
   * are breached.
   *
   * @param requestContext the JAX-RS request context containing the original request information
   *     and the start timestamp property
   * @param responseContext the JAX-RS response context containing response information (not used in
   *     this implementation but required by the interface)
   * @throws IOException if an I/O error occurs during response processing (rarely thrown)
   * @see ContainerResponseFilter#filter(ContainerRequestContext, ContainerResponseContext)
   * @see MetricsService#recordRequestLatency(long)
   * @see #filter(ContainerRequestContext)
   */
  @Override
  public void filter(
      ContainerRequestContext requestContext, ContainerResponseContext responseContext)
      throws IOException {
    // Retrieve the start timestamp that was stored during request processing
    Object startTimeObj = requestContext.getProperty(REQUEST_START_TIME);

    // Use pattern matching to ensure type safety and handle potential missing timestamps
    if (startTimeObj instanceof Long startTime) {
      // Calculate the total request processing time
      long endTime = System.currentTimeMillis();
      long latencyMs = endTime - startTime;

      // Record the latency measurement in the metrics system
      // This will update the 60-minute sliding window and potentially trigger
      // health check alerts if latency thresholds are exceeded
      metricsService.recordRequestLatency(latencyMs);
    }
    // Note: If startTimeObj is null or not a Long, we silently skip recording
    // This prevents exceptions from disrupting response processing while still
    // logging the issue (if logging were implemented)
  }
}
