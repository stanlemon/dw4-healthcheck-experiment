package com.stanlemon.healthy.spring3app.health;

import com.stanlemon.healthy.metrics.MetricsService;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom health check indicator for application health status based on error rates and latency.
 *
 * <p>This health indicator uses the metrics service to determine application health based on
 * configured thresholds for error rates and request latency.
 */
@Component
public class ApplicationHealthIndicator implements HealthIndicator {

  private final MetricsService metricsService;

  /**
   * Constructs a new ApplicationHealthIndicator with the provided metrics service.
   *
   * @param metricsService the metrics service to use for health checks
   */
  public ApplicationHealthIndicator(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /**
   * Performs health check based on error rates and latency metrics.
   *
   * @return Health.up() if metrics are within thresholds, Health.down() otherwise
   */
  @Override
  public Health health() {
    boolean errorThresholdBreached = metricsService.isErrorThresholdBreached();
    boolean latencyThresholdBreached = metricsService.isLatencyThresholdBreached();

    long errorsLastMinute = metricsService.getErrorCountLastMinute();
    double avgLatency = metricsService.getAverageLatencyLast60Seconds();

    if (!errorThresholdBreached && !latencyThresholdBreached) {
      return Health.up()
          .withDetail("errorsLastMinute", errorsLastMinute)
          .withDetail("avgLatencyLast60Seconds", avgLatency)
          .withDetail("status", "Healthy: metrics within thresholds")
          .build();
    } else {
      return Health.down()
          .withDetail("errorsLastMinute", errorsLastMinute)
          .withDetail("avgLatencyLast60Seconds", avgLatency)
          .withDetail("errorThresholdBreached", errorThresholdBreached)
          .withDetail("latencyThresholdBreached", latencyThresholdBreached)
          .withDetail("status", "Unhealthy: metrics exceed thresholds")
          .build();
    }
  }
}
