package com.stanlemon.healthy.dw4app.resources;

import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** REST endpoint returning framework-neutral health status identical across all app modules. */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

  private final HealthEvaluator evaluator;

  public HealthResource(HealthEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @GET
  public Response getHealth() {
    HealthResponse health = evaluator.evaluate();
    int statusCode = health.isHealthy() ? 200 : 503;
    return Response.status(statusCode).entity(health).build();
  }
}
