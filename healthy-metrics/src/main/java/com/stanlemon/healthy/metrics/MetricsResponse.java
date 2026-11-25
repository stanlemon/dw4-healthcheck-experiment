package com.stanlemon.healthy.metrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/** Response object containing current application metrics and health status. */
@Value
public class MetricsResponse {
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

  /**
   * Builds the response from one snapshot, so every field describes the same moment.
   *
   * @param snapshot the metrics to report
   * @return the response body for the metrics endpoint
   */
  public static MetricsResponse from(MetricsSnapshot snapshot) {
    return new MetricsResponse(
        snapshot.getErrorsLastMinute(),
        snapshot.getTotalErrors(),
        snapshot.getAverageLatencyLast60Seconds(),
        snapshot.isErrorThresholdBreached(),
        snapshot.isLatencyThresholdBreached());
  }

  @JsonProperty
  public boolean isHealthy() {
    return !errorThresholdBreached && !latencyThresholdBreached;
  }
}
