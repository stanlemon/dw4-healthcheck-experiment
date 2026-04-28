package com.stanlemon.healthy.hangar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import lombok.Value;

/** Immutable paper plane stored in the hangar. */
@Value
public class PaperPlane {
  @JsonProperty("id")
  String id;

  @JsonProperty("name")
  String name;

  @JsonProperty("wingspanCm")
  double wingspanCm;

  @JsonProperty("paperGsm")
  int paperGsm;

  @JsonProperty("noseStyle")
  NoseStyle noseStyle;

  // Force ISO-8601 string serialization regardless of framework Jackson defaults:
  // Dropwizard writes Instant as a numeric epoch by default, Spring writes ISO strings.
  // Pinning here keeps the PaperPlaneResponse byte-identical across both apps.
  @JsonFormat(shape = JsonFormat.Shape.STRING)
  @JsonProperty("stowedAt")
  Instant stowedAt;

  @JsonCreator
  public PaperPlane(
      @JsonProperty("id") String id,
      @JsonProperty("name") String name,
      @JsonProperty("wingspanCm") double wingspanCm,
      @JsonProperty("paperGsm") int paperGsm,
      @JsonProperty("noseStyle") NoseStyle noseStyle,
      @JsonProperty("stowedAt") Instant stowedAt) {
    this.id = id;
    this.name = name;
    this.wingspanCm = wingspanCm;
    this.paperGsm = paperGsm;
    this.noseStyle = noseStyle;
    this.stowedAt = stowedAt;
  }
}
