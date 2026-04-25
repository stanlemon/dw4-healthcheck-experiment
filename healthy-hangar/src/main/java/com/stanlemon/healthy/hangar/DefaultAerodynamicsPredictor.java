package com.stanlemon.healthy.hangar;

/**
 * Deterministic stand-in for a real aerodynamics service. Output is a pure function of the plane,
 * so synthetic probes can assert the exact predicted distance rather than just a 200 response.
 */
public class DefaultAerodynamicsPredictor implements AerodynamicsPredictor {

  private static final double BASELINE_PAPER_GSM = 150.0;
  private static final double WINGSPAN_LIFT_COEFFICIENT = 0.15;
  private static final double PAPER_WEIGHT_COEFFICIENT = 0.05;
  private static final double CONFIDENCE_BASE = 0.6;
  private static final double CONFIDENCE_STEP = 0.1;
  private static final double VERDICT_HANGAR_QUEEN_CEILING = 3.0;
  private static final double VERDICT_RESPECTABLE_CEILING = 8.0;
  private static final double VERDICT_SKY_CHAMPION_CEILING = 15.0;

  // Toy model: predicted meters = lift from wingspan + weight penalty + nose bonus.
  // Wider wings and lighter paper fly farther; a pointed nose adds the most bonus.
  // Confidence (0–1 scale) rises with nose bonus. Both values are deterministic
  // so tests can pin exact expected outputs.
  @Override
  public FlightPrediction predictDistance(PaperPlane plane) {
    double noseBonus = noseBonus(plane.getNoseStyle());

    double meters =
        plane.getWingspanCm() * WINGSPAN_LIFT_COEFFICIENT
            + (BASELINE_PAPER_GSM - plane.getPaperGsm()) * PAPER_WEIGHT_COEFFICIENT
            + noseBonus;

    double confidence = CONFIDENCE_BASE + noseBonus * CONFIDENCE_STEP;

    return new FlightPrediction(meters, confidence, verdictFor(meters));
  }

  private static double noseBonus(NoseStyle style) {
    return switch (style) {
      case POINTED -> 2.0;
      case FOLDED -> 1.2;
      case BLUNT -> 0.5;
    };
  }

  private static Verdict verdictFor(double meters) {
    if (meters < VERDICT_HANGAR_QUEEN_CEILING) {
      return Verdict.HANGAR_QUEEN;
    }
    if (meters < VERDICT_RESPECTABLE_CEILING) {
      return Verdict.RESPECTABLE;
    }
    if (meters < VERDICT_SKY_CHAMPION_CEILING) {
      return Verdict.SKY_CHAMPION;
    }
    return Verdict.SUSPICIOUSLY_OPTIMISTIC;
  }
}
