package com.stanlemon.healthy.spring4app.resources;

import com.stanlemon.healthy.metrics.MetricsResponse;
import com.stanlemon.healthy.metrics.MetricsService;
import com.stanlemon.healthy.metrics.MetricsSnapshot;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for retrieving application metrics including error counts, latency measurements,
 * and health status indicators.
 */
@RestController
@RequestMapping("/metrics")
public class MetricsResource {

  private final MetricsService metricsService;

  /**
   * Constructs a new MetricsResource with the provided metrics service.
   *
   * @param metricsService the metrics service to use
   */
  public MetricsResource(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /**
   * Retrieves current application metrics including error counts, latency data, and threshold
   * breach status.
   *
   * @return metrics response containing all current metric values
   */
  @GetMapping
  public MetricsResponse getMetrics() {
    // One snapshot, so every field in the response describes the same moment.
    MetricsSnapshot snapshot = metricsService.snapshot();

    return new MetricsResponse(
        snapshot.getErrorsLastMinute(),
        snapshot.getTotalErrors(),
        snapshot.getAverageLatencyLast60Seconds(),
        snapshot.isErrorThresholdBreached(),
        snapshot.isLatencyThresholdBreached());
  }
}
