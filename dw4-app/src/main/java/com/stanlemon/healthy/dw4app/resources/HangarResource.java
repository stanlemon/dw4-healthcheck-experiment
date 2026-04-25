package com.stanlemon.healthy.dw4app.resources;

import com.stanlemon.healthy.hangar.AerodynamicsPredictor;
import com.stanlemon.healthy.hangar.HangarService;
import com.stanlemon.healthy.hangar.PaperPlane;
import com.stanlemon.healthy.hangar.PaperPlaneRequest;
import com.stanlemon.healthy.hangar.PaperPlaneResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/** REST resource exposing the paper airplane hangar: stow, lookup, and list. */
@Path("/hangar/planes")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class HangarResource {

  private final HangarService hangarService;
  private final AerodynamicsPredictor predictor;

  public HangarResource(HangarService hangarService, AerodynamicsPredictor predictor) {
    this.hangarService = hangarService;
    this.predictor = predictor;
  }

  @POST
  public Response stow(@Valid PaperPlaneRequest request) {
    PaperPlane plane = hangarService.stow(request);
    PaperPlaneResponse body = new PaperPlaneResponse(plane, predictor.predictDistance(plane));
    return Response.created(
            UriBuilder.fromResource(HangarResource.class).path(plane.getId()).build())
        .entity(body)
        .build();
  }

  @GET
  @Path("/{id}")
  public PaperPlaneResponse getById(
      @PathParam("id") @Size(max = 64) @Pattern(regexp = "[A-Za-z0-9-]+") String id) {
    PaperPlane plane =
        hangarService
            .find(id)
            .orElseThrow(
                () -> {
                  log.info("Plane not found - id: {}", id);
                  return new NotFoundException(
                      "Plane not found",
                      Response.status(Response.Status.NOT_FOUND)
                          .type(MediaType.APPLICATION_JSON_TYPE)
                          .entity(Map.of("code", 404, "message", "Plane not found"))
                          .build());
                });
    return new PaperPlaneResponse(plane, predictor.predictDistance(plane));
  }

  @GET
  public List<PaperPlaneResponse> list() {
    return hangarService.listAll().stream()
        .map(plane -> new PaperPlaneResponse(plane, predictor.predictDistance(plane)))
        .toList();
  }
}
