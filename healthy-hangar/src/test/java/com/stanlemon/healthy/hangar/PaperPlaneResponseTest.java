package com.stanlemon.healthy.hangar;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PaperPlaneResponse")
class PaperPlaneResponseTest {

  private static final ObjectMapper MAPPER =
      new ObjectMapper().registerModule(new JavaTimeModule());

  private final PaperPlane plane =
      new PaperPlane(
          "id-1", "Phoenix", 22.0, 80, NoseStyle.POINTED, Instant.parse("2026-04-21T10:00:00Z"));
  private final FlightPrediction prediction = new FlightPrediction(8.8, 0.8, Verdict.SKY_CHAMPION);

  @Test
  @DisplayName("JSON round-trip preserves all fields including nested objects")
  void jsonRoundTrip_PreservesAllFields() throws Exception {
    PaperPlaneResponse original = new PaperPlaneResponse(plane, prediction);

    String json = MAPPER.writeValueAsString(original);

    assertThat(json).contains("\"plane\"").contains("\"prediction\"");
    assertThat(json).contains("\"name\":\"Phoenix\"");
    assertThat(json).contains("\"noseStyle\":\"pointed\"");
    assertThat(json).contains("\"verdict\":\"sky champion\"");
    assertThat(json).contains("\"predictedMeters\":8.8");

    PaperPlaneResponse restored = MAPPER.readValue(json, PaperPlaneResponse.class);

    assertThat(restored).isEqualTo(original);
    assertThat(restored.getPlane().getName()).isEqualTo("Phoenix");
    assertThat(restored.getPlane().getNoseStyle()).isEqualTo(NoseStyle.POINTED);
    assertThat(restored.getPrediction().getVerdict()).isEqualTo(Verdict.SKY_CHAMPION);
    assertThat(restored.getPrediction().getPredictedMeters()).isEqualTo(8.8);
  }

  @Test
  @DisplayName("Distinguishes responses that differ only in prediction")
  void unequalWhenPredictionDiffers() {
    PaperPlaneResponse a = new PaperPlaneResponse(plane, prediction);
    PaperPlaneResponse b =
        new PaperPlaneResponse(plane, new FlightPrediction(1.0, 0.1, Verdict.HANGAR_QUEEN));

    assertThat(a).isNotEqualTo(b);
  }
}
