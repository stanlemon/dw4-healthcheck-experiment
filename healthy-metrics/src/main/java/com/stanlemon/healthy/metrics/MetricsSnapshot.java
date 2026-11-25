package com.stanlemon.healthy.metrics;

import lombok.Value;

/**
 * Point-in-time view of the sliding-window metrics. Every field is read at the same moment, so
 * values that get compared or reported together (for example errors against requests, or a breach
 * flag against the number it was computed from) always agree with each other.
 */
@Value
public class MetricsSnapshot {
  /** Server errors recorded in the last 60 seconds. */
  long errorsLastMinute;

  /** Server errors recorded since the service started. */
  long totalErrors;

  /** Requests recorded in the last 60 seconds, including the ones that failed. */
  long requestsLast60Seconds;

  /** Average request latency over the last 60 seconds, or 0 if there were no requests. */
  double averageLatencyLast60Seconds;

  /** Whether the error threshold is breached. */
  boolean errorThresholdBreached;

  /** Whether the latency threshold is breached. */
  boolean latencyThresholdBreached;

  /**
   * Share of requests (0.5 means 50%) that may fail before the window counts as unhealthy. It
   * depends on traffic, so it is reported with the numbers it was applied to.
   */
  double errorRateThreshold;

  /** Average latency, in milliseconds, above which the window counts as unhealthy. */
  double latencyThresholdMs;
}
