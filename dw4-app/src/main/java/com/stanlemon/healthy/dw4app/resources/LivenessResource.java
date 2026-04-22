package com.stanlemon.healthy.dw4app.resources;

import com.stanlemon.healthy.metrics.LivenessEvaluator;
import com.stanlemon.healthy.metrics.LivenessResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Liveness endpoint — reports whether this process is fundamentally functional. */
@Path("/health/live")
@Produces(MediaType.APPLICATION_JSON)
public class LivenessResource {

  private final LivenessEvaluator evaluator;

  public LivenessResource(LivenessEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @GET
  public Response getLiveness() {
    LivenessResponse liveness = evaluator.evaluate();
    int statusCode = liveness.isAlive() ? 200 : 503;
    return Response.status(statusCode).entity(liveness).build();
  }
}
