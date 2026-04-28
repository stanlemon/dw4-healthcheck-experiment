package com.stanlemon.healthy.spring4app.resources;

import com.stanlemon.healthy.metrics.MetricsResponse;
import com.stanlemon.healthy.metrics.MetricsService;
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
    long errorsLastMinute = metricsService.getErrorCountLastMinute();
    long totalErrors = metricsService.getTotalErrorCount();
    double avgLatencyLast60Seconds = metricsService.getAverageLatencyLast60Seconds();
    boolean errorThresholdBreached = metricsService.isErrorThresholdBreached();
    boolean latencyThresholdBreached = metricsService.isLatencyThresholdBreached();

    return new MetricsResponse(
        errorsLastMinute,
        totalErrors,
        avgLatencyLast60Seconds,
        errorThresholdBreached,
        latencyThresholdBreached);
  }
}
