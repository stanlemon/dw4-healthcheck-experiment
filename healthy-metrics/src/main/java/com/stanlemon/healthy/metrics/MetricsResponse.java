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

  @JsonProperty
  public boolean isHealthy() {
    return !errorThresholdBreached && !latencyThresholdBreached;
  }
}
