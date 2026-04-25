package com.stanlemon.healthy.spring3app.resources;

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
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("HangarResource (Spring)")
class HangarResourceTest {

  private HangarService hangarService;
  private final DefaultAerodynamicsPredictor aerodynamicsPredictor =
      new DefaultAerodynamicsPredictor();
  private HangarResource resource;

  @BeforeEach
  void setUp() {
    hangarService = new DefaultHangarService();
    resource = new HangarResource(hangarService, aerodynamicsPredictor);

    MockHttpServletRequest mockRequest = new MockHttpServletRequest("POST", "/hangar/planes");
    mockRequest.setServerName("localhost");
    mockRequest.setServerPort(8080);
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(mockRequest));
  }

  @AfterEach
  void tearDown() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  @DisplayName("stow returns 201 with absolute Location header and prediction payload")
  void stow_Returns201WithLocationAndPrediction() {
    PaperPlaneRequest request = new PaperPlaneRequest("Phoenix", 22.0, 80, NoseStyle.POINTED);

    ResponseEntity<PaperPlaneResponse> response = resource.stow(request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

    PaperPlaneResponse body = Objects.requireNonNull(response.getBody());
    assertThat(body.getPlane().getName()).isEqualTo("Phoenix");
    assertThat(body.getPlane().getId()).isNotBlank();
    assertThat(Objects.requireNonNull(response.getHeaders().getLocation()).toString())
        .startsWith("http://")
        .contains("/hangar/planes/" + body.getPlane().getId());
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
  @DisplayName("getById throws ResponseStatusException(404) without echoing the id")
  void getById_ThrowsResponseStatus404WithoutEchoingId() {
    assertThatThrownBy(() -> resource.getById("does-not-exist"))
        .isInstanceOf(ResponseStatusException.class)
        .satisfies(
            ex -> {
              ResponseStatusException rse = (ResponseStatusException) ex;
              assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
              assertThat(rse.getReason()).isNull();
              assertThat(rse.getMessage()).doesNotContain("does-not-exist");
            });
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
