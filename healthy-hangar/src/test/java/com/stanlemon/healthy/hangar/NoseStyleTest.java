package com.stanlemon.healthy.hangar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("NoseStyle")
class NoseStyleTest {

  @ParameterizedTest
  @EnumSource(NoseStyle.class)
  @DisplayName("fromWireValue round-trips every constant")
  void fromWireValue_RoundTripsEveryConstant(NoseStyle style) {
    assertThat(NoseStyle.fromWireValue(style.getWireValue())).isEqualTo(style);
  }

  @Test
  @DisplayName("wire values are lowercase strings matching the enum name")
  void getWireValue_ReturnsLowercaseName() {
    assertThat(NoseStyle.POINTED.getWireValue()).isEqualTo("pointed");
    assertThat(NoseStyle.BLUNT.getWireValue()).isEqualTo("blunt");
    assertThat(NoseStyle.FOLDED.getWireValue()).isEqualTo("folded");
  }

  @ParameterizedTest(name = "[{0}]")
  @NullSource
  @ValueSource(strings = {"CRUMPLED", "POINTED"})
  @DisplayName("fromWireValue rejects null, unknown, and uppercase values without echoing input")
  void fromWireValue_WhenInvalid_ShouldThrowWithFixedMessage(String input) {
    // An exact message also proves the input is not echoed back.
    assertThatThrownBy(() -> NoseStyle.fromWireValue(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid nose style");
  }
}
