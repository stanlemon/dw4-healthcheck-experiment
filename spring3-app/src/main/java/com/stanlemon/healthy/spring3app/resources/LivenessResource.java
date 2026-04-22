package com.stanlemon.healthy.spring3app.resources;

import com.stanlemon.healthy.metrics.LivenessEvaluator;
import com.stanlemon.healthy.metrics.LivenessResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Liveness endpoint — reports whether this process is fundamentally functional. */
@RestController
@RequestMapping("/health/live")
public class LivenessResource {

  private final LivenessEvaluator evaluator;

  public LivenessResource(LivenessEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @GetMapping
  public ResponseEntity<LivenessResponse> getLiveness() {
    LivenessResponse liveness = evaluator.evaluate();
    HttpStatus status = liveness.isAlive() ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;
    return new ResponseEntity<>(liveness, status);
  }
}
