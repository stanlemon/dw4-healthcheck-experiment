package com.stanlemon.healthy.metrics;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/**
 * Framework-neutral liveness check response indicating whether the process is fundamentally
 * functional.
 */
@Value
public class LivenessResponse {
  @JsonProperty("status")
  String status;

  @JsonProperty("message")
  String message;

  @JsonProperty("errorsLastMinute")
  long errorsLastMinute;

  @JsonProperty("totalRequestsLastMinute")
  long totalRequestsLastMinute;

  @JsonCreator
  public LivenessResponse(
      @JsonProperty("status") String status,
      @JsonProperty("message") String message,
      @JsonProperty("errorsLastMinute") long errorsLastMinute,
      @JsonProperty("totalRequestsLastMinute") long totalRequestsLastMinute) {
    this.status = status;
    this.message = message;
    this.errorsLastMinute = errorsLastMinute;
    this.totalRequestsLastMinute = totalRequestsLastMinute;
  }

  @JsonProperty
  public boolean isAlive() {
    return "alive".equals(status);
  }
}
