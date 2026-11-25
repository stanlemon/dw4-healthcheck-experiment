package com.stanlemon.healthy.dw5app.resources;

import com.stanlemon.healthy.metrics.MetricsResponse;
import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * REST resource for retrieving application metrics including error counts, latency measurements,
 * and health status indicators.
 */
@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {

  private final MetricsService metricsService;

  /**
   * Constructs a new MetricsResource with the provided metrics service.
   *
   * @param metricsService the metrics service to use
   */
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /**
   * Retrieves current application metrics including error counts, latency data, and threshold
   * breach status.
   *
   * @return metrics response containing all current metric values
   */
  @GET
  public MetricsResponse getMetrics() {
    return MetricsResponse.from(metricsService.snapshot());
  }
}
