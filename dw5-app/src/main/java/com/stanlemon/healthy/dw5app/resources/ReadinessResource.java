package com.stanlemon.healthy.dw5app.resources;

import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Readiness endpoint — reports whether this instance can handle traffic. */
@Path("/health/ready")
@Produces(MediaType.APPLICATION_JSON)
public class ReadinessResource {

  private final HealthEvaluator evaluator;

  public ReadinessResource(HealthEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @GET
  public Response getReadiness() {
    HealthResponse health = evaluator.evaluate();
    int statusCode = health.isHealthy() ? 200 : 503;
    return Response.status(statusCode).entity(health).build();
  }
}
