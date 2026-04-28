package com.stanlemon.healthy.dw5app.health;

import com.codahale.metrics.health.HealthCheck;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;

/** Health check that delegates to HealthEvaluator for threshold evaluation. */
public class ApplicationHealthCheck extends HealthCheck {
  private final HealthEvaluator evaluator;

  public ApplicationHealthCheck(HealthEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @Override
  protected Result check() {
    HealthResponse health = evaluator.evaluate();

    if (health.isHealthy()) {
      return Result.healthy(health.getMessage());
    }
    return Result.unhealthy(health.getMessage());
  }
}
