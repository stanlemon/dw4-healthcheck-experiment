package com.stanlemon.healthy.dw4app.resources;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.Value;

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
    long errorsLastMinute = metricsService.getErrorCountLastMinute();
    long totalErrors = metricsService.getTotalErrorCount();
    double avgLatencyLast60Seconds = metricsService.getAverageLatencyLast60Seconds();
    boolean errorThresholdBreached = metricsService.isErrorThresholdBreached();
    boolean latencyThresholdBreached = metricsService.isLatencyThresholdBreached();

    return new MetricsResponse(
        errorsLastMinute,
        totalErrors,
        avgLatencyLast60Seconds,
        errorThresholdBreached,
        latencyThresholdBreached);
  }

  /** Response object containing current application metrics and health status. */
  @Value
  public static class MetricsResponse {
    @JsonProperty("errorsLastMinute")
    long errorsLastMinute;

    @JsonProperty("totalErrors")
    long totalErrors;

    @JsonProperty("avgLatencyLast60Seconds")
    double avgLatencyLast60Seconds;

    @JsonProperty("errorThresholdBreached")
    boolean errorThresholdBreached;

    @JsonProperty("latencyThresholdBreached")
    boolean latencyThresholdBreached;

    @JsonCreator
    public MetricsResponse(
        @JsonProperty("errorsLastMinute") long errorsLastMinute,
        @JsonProperty("totalErrors") long totalErrors,
        @JsonProperty("avgLatencyLast60Seconds") double avgLatencyLast60Seconds,
        @JsonProperty("errorThresholdBreached") boolean errorThresholdBreached,
        @JsonProperty("latencyThresholdBreached") boolean latencyThresholdBreached) {
      this.errorsLastMinute = errorsLastMinute;
      this.totalErrors = totalErrors;
      this.avgLatencyLast60Seconds = avgLatencyLast60Seconds;
      this.errorThresholdBreached = errorThresholdBreached;
      this.latencyThresholdBreached = latencyThresholdBreached;
    }

    @JsonProperty
    public boolean isHealthy() {
      return !errorThresholdBreached && !latencyThresholdBreached;
    }
  }
}
