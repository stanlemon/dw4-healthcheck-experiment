package com.stanlemon.healthy.hangar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

/** Deterministic flight prediction produced by an AerodynamicsPredictor. */
@Value
public class FlightPrediction {

  @JsonProperty("predictedMeters")
  double predictedMeters;

  @JsonProperty("confidence")
  double confidence;

  @JsonProperty("verdict")
  Verdict verdict;

  @JsonCreator
  public FlightPrediction(
      @JsonProperty("predictedMeters") double predictedMeters,
      @JsonProperty("confidence") double confidence,
      @JsonProperty("verdict") Verdict verdict) {
    this.predictedMeters = predictedMeters;
    this.confidence = confidence;
    this.verdict = verdict;
  }
}
