package com.stanlemon.healthy.spring3app.health;

import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.HealthResponse;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/** Health indicator that delegates to HealthEvaluator for threshold evaluation. */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {

  private final HealthEvaluator evaluator;

  public ApplicationHealthIndicator(HealthEvaluator evaluator) {
    this.evaluator = evaluator;
  }

  @Override
  public Health health() {
    HealthResponse health = evaluator.evaluate();

    Health.Builder builder = health.isHealthy() ? Health.up() : Health.down();

    builder
        .withDetail("status", health.getMessage())
        .withDetail("errorsLastMinute", health.getErrorsLastMinute())
        .withDetail("avgLatencyLast60Seconds", health.getAvgLatencyLast60Seconds());

    if (!health.isHealthy()) {
      builder
          .withDetail("errorThresholdBreached", health.isErrorThresholdBreached())
          .withDetail("latencyThresholdBreached", health.isLatencyThresholdBreached());
    }

    return builder.build();
  }
}
