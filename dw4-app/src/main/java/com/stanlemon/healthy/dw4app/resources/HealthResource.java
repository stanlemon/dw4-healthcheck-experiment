package com.stanlemon.healthy.dw4app.resources;

import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** REST endpoint returning framework-neutral health status identical across all app modules. */
@Path("/health")
@Produces(MediaType.APPLICATION_JSON)
public class HealthResource {

  private final HealthEvaluator evaluator;

  public HealthResource(HealthEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @GET
  public HealthResponse getHealth() {
    return evaluator.evaluate();
  }
}
