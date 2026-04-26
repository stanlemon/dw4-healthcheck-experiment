package com.stanlemon.healthy.dw5app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.stanlemon.healthy.hangar.DefaultAerodynamicsPredictor;
import com.stanlemon.healthy.hangar.DefaultHangarService;
import com.stanlemon.healthy.hangar.HangarService;
import com.stanlemon.healthy.hangar.NoseStyle;
import com.stanlemon.healthy.hangar.PaperPlane;
import com.stanlemon.healthy.hangar.PaperPlaneRequest;
import com.stanlemon.healthy.hangar.PaperPlaneResponse;
import com.stanlemon.healthy.hangar.Verdict;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HangarResource (Dropwizard)")
class HangarResourceTest {

  private HangarService hangarService;
  private final DefaultAerodynamicsPredictor aerodynamicsPredictor =
      new DefaultAerodynamicsPredictor();
  private HangarResource resource;

  @BeforeEach
  void setUp() {
    hangarService = new DefaultHangarService();
    resource = new HangarResource(hangarService, aerodynamicsPredictor);
  }

  @Test
  @DisplayName("stow returns 201 with Location header and prediction payload")
  void stow_Returns201WithLocationAndPrediction() {
    PaperPlaneRequest request = new PaperPlaneRequest("Phoenix", 22.0, 80, NoseStyle.POINTED);

    Response response = resource.stow(request);

    assertThat(response.getStatus()).isEqualTo(201);

    PaperPlaneResponse body = (PaperPlaneResponse) response.getEntity();
    assertThat(body.getPlane().getName()).isEqualTo("Phoenix");
    assertThat(body.getPlane().getId()).isNotBlank();
    assertThat(response.getLocation().getPath())
        .isEqualTo("/hangar/planes/" + body.getPlane().getId());
    assertThat(body.getPrediction().getPredictedMeters()).isCloseTo(8.8, within(1e-9));
    assertThat(body.getPrediction().getVerdict()).isEqualTo(Verdict.SKY_CHAMPION);
  }

  @Test
  @DisplayName("getById returns the stowed plane with the same prediction")
  void getById_ReturnsStowedPlaneWithPrediction() {
    PaperPlane stowed =
        hangarService.stow(new PaperPlaneRequest("Kestrel", 30.0, 100, NoseStyle.FOLDED));

    PaperPlaneResponse body = resource.getById(stowed.getId());

    assertThat(body.getPlane()).isEqualTo(stowed);
    assertThat(body.getPrediction().getPredictedMeters()).isCloseTo(8.2, within(1e-9));
    assertThat(body.getPrediction().getVerdict()).isEqualTo(Verdict.SKY_CHAMPION);
  }

  @Test
  @DisplayName("getById throws NotFoundException with no user-supplied id in message")
  void getById_ThrowsNotFoundWithoutEchoingId() {
    assertThatThrownBy(() -> resource.getById("does-not-exist"))
        .isInstanceOf(NotFoundException.class)
        .extracting(Throwable::getMessage)
        .asString()
        .doesNotContain("does-not-exist");
  }

  @Test
  @DisplayName("list returns all stowed planes, most recent first, with predictions")
  void list_ReturnsAllPlanesOrderedRecentFirst() {
    hangarService.stow(new PaperPlaneRequest("first", 22.0, 80, NoseStyle.POINTED));
    hangarService.stow(new PaperPlaneRequest("second", 22.0, 80, NoseStyle.BLUNT));

    List<PaperPlaneResponse> all = resource.list();

    assertThat(all).hasSize(2);
    assertThat(all.get(0).getPlane().getName()).isEqualTo("second");
    assertThat(all.get(1).getPlane().getName()).isEqualTo("first");
    assertThat(all.get(0).getPrediction().getPredictedMeters())
        .as("BLUNT nose prediction")
        .isCloseTo(7.3, within(1e-9));
    assertThat(all.get(1).getPrediction().getPredictedMeters())
        .as("POINTED nose prediction")
        .isCloseTo(8.8, within(1e-9));
  }
}
