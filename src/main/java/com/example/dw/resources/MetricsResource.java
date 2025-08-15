package com.example.dw.resources;

import com.example.dw.metrics.MetricsService;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.Value;

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

  @GET
  public MetricsResponse getMetrics() {
    long errorsLastMinute = metricsService.getErrorCountLastMinute();
    long totalErrors = metricsService.getTotalErrorCount();
    double avgLatencyLast60Minutes = metricsService.getAverageLatencyLast60Seconds();
    boolean errorThresholdBreached = metricsService.isErrorThresholdBreached();
    boolean latencyThresholdBreached = metricsService.isLatencyThresholdBreached();

    return new MetricsResponse(
        errorsLastMinute,
        totalErrors,
        avgLatencyLast60Minutes,
        errorThresholdBreached,
        latencyThresholdBreached);
  }

  @Value
  public static class MetricsResponse {
    @JsonProperty("errorsLastMinute")
    long errorsLastMinute;

    @JsonProperty("totalErrors")
    long totalErrors;

    @JsonProperty("avgLatencyLast60Minutes")
    double avgLatencyLast60Minutes;

    @JsonProperty("errorThresholdBreached")
    boolean errorThresholdBreached;

    @JsonProperty("latencyThresholdBreached")
    boolean latencyThresholdBreached;

    @JsonCreator
    public MetricsResponse(
        @JsonProperty("errorsLastMinute") long errorsLastMinute,
        @JsonProperty("totalErrors") long totalErrors,
        @JsonProperty("avgLatencyLast60Minutes") double avgLatencyLast60Minutes,
        @JsonProperty("errorThresholdBreached") boolean errorThresholdBreached,
        @JsonProperty("latencyThresholdBreached") boolean latencyThresholdBreached) {
      this.errorsLastMinute = errorsLastMinute;
      this.totalErrors = totalErrors;
      this.avgLatencyLast60Minutes = avgLatencyLast60Minutes;
      this.errorThresholdBreached = errorThresholdBreached;
      this.latencyThresholdBreached = latencyThresholdBreached;
    }

    @JsonProperty
    public boolean isHealthy() {
      return !errorThresholdBreached && !latencyThresholdBreached;
    }
  }
}
