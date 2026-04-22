package com.stanlemon.healthy.spring3app.resources;

import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Readiness endpoint — reports whether this instance can handle traffic. */
@RestController
@RequestMapping("/health/ready")
public class ReadinessResource {

  private final HealthEvaluator evaluator;

  public ReadinessResource(HealthEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @GetMapping
  public ResponseEntity<HealthResponse> getReadiness() {
    HealthResponse health = evaluator.evaluate();
    HttpStatus status = health.isHealthy() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return new ResponseEntity<>(health, status);
  }
}
