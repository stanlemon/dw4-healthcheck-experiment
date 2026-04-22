package com.stanlemon.healthy.spring3app.resources;

import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST endpoint returning framework-neutral health status identical across all app modules. */
@RestController
@RequestMapping("/health")
public class HealthResource {

  private final HealthEvaluator evaluator;

  public HealthResource(HealthEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @GetMapping
  public HealthResponse getHealth() {
    return evaluator.evaluate();
  }
}
