package com.stanlemon.healthy.hangar;

/** External prediction service that estimates how far a paper plane will fly. */
public interface AerodynamicsPredictor {

  /** Predict flight distance for the given plane. Must be deterministic for a given input. */
  FlightPrediction predictDistance(PaperPlane plane);
}
