package com.example.dw.metrics;

/**
 * Service interface to track application metrics, including error counts and request latency in
 * sliding windows. Implementations should provide high-performance collection and retrieval of
 * metrics data.
 *
 * <p>Implementations of this interface must be thread-safe, as metrics collection and retrieval
 * will occur from multiple concurrent threads. All methods should support concurrent invocation
 * without external synchronization.
 */
public interface MetricsService {

  /**
   * Record a new server error (typically 500 status code). This method must be thread-safe and
   * support concurrent invocation.
   */
  void recordServerError();

  /**
   * Get the count of errors within the last minute.
   *
   * @return count of errors in the last minute
   */
  long getErrorCountLastMinute();

  /**
   * Get the total count of errors recorded since the service started or was last cleared.
   *
   * @return total error count
   */
  long getTotalErrorCount();

  /**
   * Check if the current error count in the last minute exceeds the specified threshold. Uses
   * intelligent thresholding based on traffic volume: - Low traffic: Uses absolute error count
   * threshold - High traffic: Uses error rate percentage
   *
   * @param threshold the threshold for error count
   * @return true if the error count exceeds the threshold
   */
  boolean isErrorThresholdBreached(long threshold);

  /**
   * Check if the current error count in the last minute exceeds the default threshold.
   *
   * @return true if the error count exceeds the default threshold
   */
  boolean isErrorThresholdBreached();

  /**
   * Record request latency in milliseconds. This method must be thread-safe and support concurrent
   * invocation.
   *
   * @param latencyMs the latency in milliseconds
   */
  void recordRequestLatency(long latencyMs);

  /**
   * Get the average request latency over the last 60 seconds.
   *
   * @return average latency in milliseconds, or 0 if no requests recorded
   */
  double getAverageLatencyLast60Seconds();

  /**
   * Get the total number of requests in the last 60 seconds.
   *
   * @return total request count
   */
  long getTotalRequestCountLast60Seconds();

  /**
   * Check if the current average latency exceeds the specified threshold. Only applies the
   * threshold if there's sufficient traffic volume.
   *
   * @param thresholdMs the threshold in milliseconds
   * @return true if the average latency exceeds the threshold AND there's sufficient traffic
   */
  boolean isLatencyThresholdBreached(double thresholdMs);

  /**
   * Check if the current average latency exceeds the default threshold. Only applies the threshold
   * if there's sufficient traffic volume.
   *
   * @return true if the average latency exceeds the default threshold AND there's sufficient
   *     traffic
   */
  boolean isLatencyThresholdBreached();

  /**
   * Get the default error threshold.
   *
   * @return the default error threshold
   */
  long getDefaultErrorThreshold();

  /**
   * Get the default latency threshold in milliseconds.
   *
   * @return the default latency threshold in milliseconds
   */
  double getDefaultLatencyThresholdMs();

  /** Clear all metrics (useful for testing). */
  void clearMetrics();
}
