package com.stanlemon.healthy.hangar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/** Shape of a paper plane's nose, which affects predicted flight distance. */
public enum NoseStyle {
  POINTED("pointed"),
  BLUNT("blunt"),
  FOLDED("folded");

  private final String wireValue;

  NoseStyle(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String getWireValue() {
    return wireValue;
  }

  @JsonCreator
  public static NoseStyle fromWireValue(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Invalid nose style");
    }
    return Arrays.stream(values())
        .filter(v -> v.wireValue.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid nose style"));
  }
}
