package com.stanlemon.healthy.metrics;

/**
 * Evaluates whether the application process is fundamentally functional. Unlike {@link
 * HealthEvaluator} which detects degradation, this evaluator only reports failure when every
 * request is failing — indicating the process itself is broken and should be replaced.
 */
public class LivenessEvaluator {

  private static final long MINIMUM_SAMPLE_SIZE = 10;

  private final MetricsService metricsService;

  public LivenessEvaluator(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /** Evaluates whether the process is alive based on total error rate. */
  public LivenessResponse evaluate() {
    long errorCount = metricsService.getErrorCountLastMinute();
    long totalRequests = metricsService.getTotalRequestCountLast60Seconds();

    if (totalRequests < MINIMUM_SAMPLE_SIZE) {
      return new LivenessResponse(
          "alive",
          String.format(
              "OK - insufficient traffic to evaluate (%d requests, minimum %d)",
              totalRequests, MINIMUM_SAMPLE_SIZE),
          errorCount,
          totalRequests);
    }

    if (errorCount >= totalRequests) {
      return new LivenessResponse(
          "dead",
          String.format(
              "All requests failing: %d errors out of %d requests in last minute",
              errorCount, totalRequests),
          errorCount,
          totalRequests);
    }

    return new LivenessResponse(
        "alive",
        String.format(
            "OK - %d errors out of %d requests in last minute", errorCount, totalRequests),
        errorCount,
        totalRequests);
  }
}
