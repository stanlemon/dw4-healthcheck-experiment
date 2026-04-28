package com.stanlemon.healthy.hangar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultAerodynamicsPredictor")
class DefaultAerodynamicsPredictorTest {

  private static final double EPSILON = 1e-9;

  private final DefaultAerodynamicsPredictor client = new DefaultAerodynamicsPredictor();

  @Test
  @DisplayName("POINTED nose with 22cm wingspan / 80gsm paper predicts the expected distance")
  void predictDistance_PointedWithStandardPaper() {
    PaperPlane plane = plane(22.0, 80, NoseStyle.POINTED);

    FlightPrediction prediction = client.predictDistance(plane);

    // 22 * 0.15 + (150 - 80) * 0.05 + 2.0 = 3.3 + 3.5 + 2.0 = 8.8
    assertThat(prediction.getPredictedMeters()).isCloseTo(8.8, within(EPSILON));
    assertThat(prediction.getVerdict()).isEqualTo(Verdict.SKY_CHAMPION);
    // 0.6 + 2.0 * 0.1 = 0.8
    assertThat(prediction.getConfidence()).isCloseTo(0.8, within(EPSILON));
  }

  @Test
  @DisplayName("Heavy cardstock with blunt nose lands in hangar queen territory")
  void predictDistance_HeavyBluntIsHangarQueen() {
    PaperPlane plane = plane(10.0, 300, NoseStyle.BLUNT);

    FlightPrediction prediction = client.predictDistance(plane);

    // 10 * 0.15 + (150 - 300) * 0.05 + 0.5 = 1.5 - 7.5 + 0.5 = -5.5
    assertThat(prediction.getPredictedMeters()).isCloseTo(-5.5, within(EPSILON));
    assertThat(prediction.getVerdict()).isEqualTo(Verdict.HANGAR_QUEEN);
  }

  @Test
  @DisplayName("Large pointed plane on tissue paper ends up suspiciously optimistic")
  void predictDistance_SuspiciouslyOptimistic() {
    PaperPlane plane = plane(100.0, 50, NoseStyle.POINTED);

    FlightPrediction prediction = client.predictDistance(plane);

    // 100 * 0.15 + (150 - 50) * 0.05 + 2.0 = 15 + 5 + 2 = 22
    assertThat(prediction.getPredictedMeters()).isCloseTo(22.0, within(EPSILON));
    assertThat(prediction.getVerdict()).isEqualTo(Verdict.SUSPICIOUSLY_OPTIMISTIC);
  }

  @Test
  @DisplayName(
      "Verdict transitions at each boundary: <3 hangar queen, <8 respectable, <15 sky champion, >=15 suspicious")
  void predictDistance_VerdictBoundaries() {
    // Just below 3.0 → HANGAR_QUEEN: 10 * 0.15 + (150 - 140) * 0.05 + 0.5 = 1.5 + 0.5 + 0.5 = 2.5
    assertThat(client.predictDistance(plane(10.0, 140, NoseStyle.BLUNT)).getVerdict())
        .isEqualTo(Verdict.HANGAR_QUEEN);

    // Just above 3.0 → RESPECTABLE: 18 * 0.15 + (150 - 100) * 0.05 + 1.2 = 2.7 + 2.5 + 1.2 = 6.4
    assertThat(client.predictDistance(plane(18.0, 100, NoseStyle.FOLDED)).getVerdict())
        .isEqualTo(Verdict.RESPECTABLE);

    // Just below 8.0 → still RESPECTABLE: 20 * 0.15 + (150 - 100) * 0.05 + 1.2 = 3 + 2.5 + 1.2 =
    // 6.7
    assertThat(client.predictDistance(plane(20.0, 100, NoseStyle.FOLDED)).getVerdict())
        .isEqualTo(Verdict.RESPECTABLE);

    // Exactly 8.0 → SKY_CHAMPION: 20 * 0.15 + (150 - 74) * 0.05 + 1.2 = 3 + 3.8 + 1.2 = 8.0
    PaperPlane at8m = plane(20.0, 74, NoseStyle.FOLDED);
    assertThat(client.predictDistance(at8m).getPredictedMeters()).isCloseTo(8.0, within(EPSILON));
    assertThat(client.predictDistance(at8m).getVerdict()).isEqualTo(Verdict.SKY_CHAMPION);

    // Just below 15.0 → SKY_CHAMPION: 80 * 0.15 + (150 - 150) * 0.05 + 2.0 = 12 + 0 + 2 = 14.0
    assertThat(client.predictDistance(plane(80.0, 150, NoseStyle.POINTED)).getVerdict())
        .isEqualTo(Verdict.SKY_CHAMPION);

    // Exactly 15.0 → SUSPICIOUSLY_OPTIMISTIC: 80 * 0.15 + (150 - 130) * 0.05 + 2.0 = 12 + 1 + 2 =
    // 15.0
    PaperPlane atBoundary = plane(80.0, 130, NoseStyle.POINTED);
    assertThat(client.predictDistance(atBoundary).getPredictedMeters())
        .isCloseTo(15.0, within(EPSILON));
    assertThat(client.predictDistance(atBoundary).getVerdict())
        .isEqualTo(Verdict.SUSPICIOUSLY_OPTIMISTIC);
  }

  @Test
  @DisplayName("Exactly 3.0 meters yields RESPECTABLE, not HANGAR_QUEEN")
  void predictDistance_WhenExactly3Meters_ShouldBeRespectable() {
    // FOLDED (bonus=1.2): 3.0 = w*0.15 + (150-gsm)*0.05 + 1.2
    // gsm=150: 3.0 = w*0.15 + 1.2 → w*0.15 = 1.8 → w = 12.0
    PaperPlane plane = plane(12.0, 150, NoseStyle.FOLDED);
    FlightPrediction prediction = client.predictDistance(plane);
    // 12*0.15 + (150-150)*0.05 + 1.2 = 1.8 + 0 + 1.2 = 3.0
    assertThat(prediction.getPredictedMeters()).isCloseTo(3.0, within(EPSILON));
    assertThat(prediction.getVerdict()).isEqualTo(Verdict.RESPECTABLE);
  }

  @Test
  @DisplayName("FOLDED nose has confidence 0.72")
  void predictDistance_FoldedNose_ShouldHaveExpectedConfidence() {
    PaperPlane plane = plane(20.0, 100, NoseStyle.FOLDED);
    FlightPrediction prediction = client.predictDistance(plane);
    // confidence = 0.6 + 1.2 * 0.1 = 0.72
    assertThat(prediction.getConfidence()).isCloseTo(0.72, within(EPSILON));
  }

  @Test
  @DisplayName("BLUNT nose has confidence 0.65")
  void predictDistance_BluntNose_ShouldHaveExpectedConfidence() {
    PaperPlane plane = plane(20.0, 100, NoseStyle.BLUNT);
    FlightPrediction prediction = client.predictDistance(plane);
    // confidence = 0.6 + 0.5 * 0.1 = 0.65
    assertThat(prediction.getConfidence()).isCloseTo(0.65, within(EPSILON));
  }

  private static PaperPlane plane(double wingspanCm, int paperGsm, NoseStyle style) {
    return new PaperPlane("id", "test", wingspanCm, paperGsm, style, Instant.EPOCH);
  }
}
