package com.stanlemon.healthy.hangar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NoseStyle")
class NoseStyleTest {

  @Test
  @DisplayName("fromWireValue round-trips every constant")
  void fromWireValue_RoundTripsEveryConstant() {
    for (NoseStyle style : NoseStyle.values()) {
      assertThat(NoseStyle.fromWireValue(style.getWireValue())).isEqualTo(style);
    }
  }

  @Test
  @DisplayName("wire values are lowercase strings matching the enum name")
  void getWireValue_ReturnsLowercaseName() {
    assertThat(NoseStyle.POINTED.getWireValue()).isEqualTo("pointed");
    assertThat(NoseStyle.BLUNT.getWireValue()).isEqualTo("blunt");
    assertThat(NoseStyle.FOLDED.getWireValue()).isEqualTo("folded");
  }

  @Test
  @DisplayName("fromWireValue rejects unknown strings without echoing input")
  void fromWireValue_RejectsUnknownWithoutEchoing() {
    String badInput = "CRUMPLED";
    assertThatThrownBy(() -> NoseStyle.fromWireValue(badInput))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid nose style")
        .hasMessageNotContaining(badInput);
  }

  @Test
  @DisplayName("fromWireValue with null throws IllegalArgumentException")
  void fromWireValue_WhenNull_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(() -> NoseStyle.fromWireValue(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Invalid nose style");
  }

  @Test
  @DisplayName("fromWireValue rejects uppercase variant")
  void fromWireValue_WhenUpperCase_ShouldThrowIllegalArgumentException() {
    assertThatThrownBy(() -> NoseStyle.fromWireValue("POINTED"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
