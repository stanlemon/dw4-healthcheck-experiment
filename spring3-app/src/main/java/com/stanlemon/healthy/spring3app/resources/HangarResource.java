package com.stanlemon.healthy.spring3app.resources;

import com.stanlemon.healthy.hangar.AerodynamicsPredictor;
import com.stanlemon.healthy.hangar.HangarService;
import com.stanlemon.healthy.hangar.PaperPlane;
import com.stanlemon.healthy.hangar.PaperPlaneRequest;
import com.stanlemon.healthy.hangar.PaperPlaneResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** REST controller exposing the paper airplane hangar: stow, lookup, and list. */
@RestController
@RequestMapping(value = "/hangar/planes", produces = MediaType.APPLICATION_JSON_VALUE)
@Validated
@Slf4j
public class HangarResource {

  private final HangarService hangarService;
  private final AerodynamicsPredictor predictor;

  public HangarResource(HangarService hangarService, AerodynamicsPredictor predictor) {
    this.hangarService = hangarService;
    this.predictor = predictor;
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PaperPlaneResponse> stow(@Valid @RequestBody PaperPlaneRequest request) {
    PaperPlane plane = hangarService.stow(request);
    PaperPlaneResponse body = new PaperPlaneResponse(plane, predictor.predictDistance(plane));
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(plane.getId())
            .toUri();
    return ResponseEntity.created(location).body(body);
  }

  @GetMapping("/{id}")
  public PaperPlaneResponse getById(
      @PathVariable("id") @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9-]+") String id) {
    PaperPlane plane =
        hangarService
            .find(id)
            .orElseThrow(
                () -> {
                  log.info("Plane not found - id: {}", id);
                  return new ResponseStatusException(HttpStatus.NOT_FOUND);
                });
    return new PaperPlaneResponse(plane, predictor.predictDistance(plane));
  }

  @GetMapping
  public List<PaperPlaneResponse> list() {
    return hangarService.listAll().stream()
        .map(plane -> new PaperPlaneResponse(plane, predictor.predictDistance(plane)))
        .toList();
  }
}
