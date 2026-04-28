package com.stanlemon.healthy.hangar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

/** Categorical assessment of a paper plane's predicted flight. */
public enum Verdict {
  HANGAR_QUEEN("hangar queen"),
  RESPECTABLE("respectable"),
  SKY_CHAMPION("sky champion"),
  SUSPICIOUSLY_OPTIMISTIC("suspiciously optimistic");

  private final String wireValue;

  Verdict(String wireValue) {
    this.wireValue = wireValue;
  }

  @JsonValue
  public String getWireValue() {
    return wireValue;
  }

  @JsonCreator
  public static Verdict fromWireValue(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Invalid verdict");
    }
    return Arrays.stream(values())
        .filter(v -> v.wireValue.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Invalid verdict"));
  }
}
