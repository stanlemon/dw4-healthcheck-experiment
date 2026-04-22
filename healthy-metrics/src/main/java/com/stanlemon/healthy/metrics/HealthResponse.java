package com.stanlemon.healthy.metrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/** Framework-neutral health check response with metrics details. */
@Value
public class HealthResponse {
  @JsonProperty("status")
  String status;

  @JsonProperty("message")
  String message;

  @JsonProperty("errorsLastMinute")
  long errorsLastMinute;

  @JsonProperty("avgLatencyLast60Seconds")
  double avgLatencyLast60Seconds;

  @JsonProperty("errorThresholdBreached")
  boolean errorThresholdBreached;

  @JsonProperty("latencyThresholdBreached")
  boolean latencyThresholdBreached;

  @JsonCreator
  public HealthResponse(
      @JsonProperty("status") String status,
      @JsonProperty("message") String message,
      @JsonProperty("errorsLastMinute") long errorsLastMinute,
      @JsonProperty("avgLatencyLast60Seconds") double avgLatencyLast60Seconds,
      @JsonProperty("errorThresholdBreached") boolean errorThresholdBreached,
      @JsonProperty("latencyThresholdBreached") boolean latencyThresholdBreached) {
    this.status = status;
    this.message = message;
    this.errorsLastMinute = errorsLastMinute;
    this.avgLatencyLast60Seconds = avgLatencyLast60Seconds;
    this.errorThresholdBreached = errorThresholdBreached;
    this.latencyThresholdBreached = latencyThresholdBreached;
  }

  @JsonProperty
  public boolean isHealthy() {
    return !errorThresholdBreached && !latencyThresholdBreached;
  }
}
