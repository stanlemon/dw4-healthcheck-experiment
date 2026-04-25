package com.stanlemon.healthy.hangar;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Value;

/** Validated request to stow a paper plane in the hangar. */
@Value
public class PaperPlaneRequest {

  @NotBlank
  @Size(max = 40)
  @Pattern(regexp = "[A-Za-z0-9 _-]+")
  String name;

  @DecimalMin("5")
  @DecimalMax("100")
  double wingspanCm;

  @Min(50)
  @Max(300)
  int paperGsm;

  @NotNull NoseStyle noseStyle;

  @JsonCreator
  public PaperPlaneRequest(
      @JsonProperty("name") String name,
      @JsonProperty("wingspanCm") double wingspanCm,
      @JsonProperty("paperGsm") int paperGsm,
      @JsonProperty("noseStyle") NoseStyle noseStyle) {
    this.name = name;
    this.wingspanCm = wingspanCm;
    this.paperGsm = paperGsm;
    this.noseStyle = noseStyle;
  }
}
