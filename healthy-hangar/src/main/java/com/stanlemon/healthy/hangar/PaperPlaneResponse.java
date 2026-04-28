package com.stanlemon.healthy.hangar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/** Response combining a stowed paper plane with its flight prediction. */
@Value
public class PaperPlaneResponse {

  @JsonProperty("plane")
  PaperPlane plane;

  @JsonProperty("prediction")
  FlightPrediction prediction;

  @JsonCreator
  public PaperPlaneResponse(
      @JsonProperty("plane") PaperPlane plane,
      @JsonProperty("prediction") FlightPrediction prediction) {
    this.plane = plane;
    this.prediction = prediction;
  }
}
