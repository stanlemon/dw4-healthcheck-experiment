package com.stanlemon.healthy.hangar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Verdict")
class VerdictTest {

  @Test
  @DisplayName("fromWireValue round-trips every constant")
  void fromWireValue_RoundTripsEveryConstant() {
    for (Verdict v : Verdict.values()) {
      assertThat(Verdict.fromWireValue(v.getWireValue())).isEqualTo(v);
    }
  }

  @Test
  @DisplayName("fromWireValue rejects unknown strings without echoing input")
  void fromWireValue_RejectsUnknownWithoutEchoing() {
    String badInput = "made up";
    assertThatThrownBy(() -> Verdict.fromWireValue(badInput))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid verdict")
        .hasMessageNotContaining(badInput);
  }

  @Test
  @DisplayName("fromWireValue with null throws IllegalArgumentException")
  void fromWireValue_WhenNull_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(() -> Verdict.fromWireValue(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid verdict");
  }

  @Test
  @DisplayName("fromWireValue rejects uppercase variant")
  void fromWireValue_WhenUpperCase_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(() -> Verdict.fromWireValue("HANGAR QUEEN"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
