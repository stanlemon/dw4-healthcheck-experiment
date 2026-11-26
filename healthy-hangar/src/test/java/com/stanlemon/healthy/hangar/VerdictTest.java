package com.stanlemon.healthy.hangar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Verdict")
class VerdictTest {

  @ParameterizedTest
  @EnumSource(Verdict.class)
  @DisplayName("fromWireValue round-trips every constant")
  void fromWireValue_RoundTripsEveryConstant(Verdict verdict) {
    assertThat(Verdict.fromWireValue(verdict.getWireValue())).isEqualTo(verdict);
  }

  @ParameterizedTest(name = "[{0}]")
  @NullSource
  @ValueSource(strings = {"made up", "HANGAR QUEEN"})
  @DisplayName("fromWireValue rejects null, unknown, and uppercase values without echoing input")
  void fromWireValue_WhenInvalid_ShouldThrowWithFixedMessage(String input) {
    // An exact message also proves the input is not echoed back.
    assertThatThrownBy(() -> Verdict.fromWireValue(input))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Invalid verdict");
  }
}
