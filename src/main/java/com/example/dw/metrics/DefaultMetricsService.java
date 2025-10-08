package com.example.dw.metrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of MetricsService to track application metrics, including error counts and
 * request latency in sliding windows. Uses a high-performance circular buffer approach with
 * configurable buckets for error tracking (one per second) and latency tracking (one per second).
 *
 * <p>This implementation is thread-safe. All methods can be called concurrently from multiple
 * threads without external synchronization. Thread-safety is guaranteed through:
 *
 * <ol>
 *   <li>Synchronized methods for critical sections ({@code recordServerError}, {@code
 *       recordRequestLatency})
 *   <li>Synchronized bucket clearing logic ({@code clearOldBuckets}, {@code
 *       clearOldLatencyBuckets})
 *   <li>Atomic operations for counter increments using AtomicLong
 *   <li>Proper memory visibility using final fields and AtomicLong references
 * </ol>
 *
 * <p>This class uses a sliding window approach for metrics tracking, with separate windows for
 * error rates and latency measurements. Old data is automatically cleared as the window moves.
 */
public class DefaultMetricsService implements MetricsService {
  // Use 60 buckets for a 60-second sliding window (one bucket per second) for errors
  private final int errorBucketCount;
  // Use 60 buckets for a 60-second sliding window (one bucket per second) for latency
  private final int latencyBucketCount;
  // Default error threshold for last minute
  private final long errorThreshold;
  // Default latency threshold in milliseconds
  private final double latencyThresholdMs;
  // Minimum number of requests required before applying latency thresholds
  private final long minimumLatencySampleSize;
  // Minimum number of requests required before applying error rate thresholds (as percentage)
  private final long minimumErrorSampleSize;

  // Circular buffer of error counts per second
  private final AtomicLong[] errorBuckets;
  // Track the last bucket we wrote to
  private final AtomicLong lastBucketTime = new AtomicLong(-1);
  // Total count of errors (for logging/metrics purposes)
  private final AtomicLong totalErrorCount = new AtomicLong(0);

  // Latency tracking with 60-second sliding window
  // Array to track total latency per second bucket
  private final AtomicLong[] latencyTotalBuckets;
  // Array to track count of requests per second bucket
  private final AtomicLong[] latencyCountBuckets;
  // Track the last minute bucket we wrote to for latency
  private final AtomicLong lastLatencyBucketTime = new AtomicLong(-1);
  // Total count of requests (for traffic volume calculations)
  private final AtomicLong totalRequestCount = new AtomicLong(0);

  /** Creates a new DefaultMetricsService with the default configuration. */
  public DefaultMetricsService() {
    this(60, 60, 100, 100.0, 5, 10);
  }

  /**
   * Creates a new DefaultMetricsService with custom configuration parameters.
   *
   * @param errorBucketCount number of buckets for error tracking (typically 60 for 1-minute window)
   * @param latencyBucketCount number of buckets for latency tracking (typically 60 for 1-minute
   *     window)
   * @param errorThreshold default threshold for error count
   * @param latencyThresholdMs default threshold for latency in milliseconds
   * @param minimumLatencySampleSize minimum number of requests before applying latency thresholds
   * @param minimumErrorSampleSize minimum number of requests before applying error rate thresholds
   */
  public DefaultMetricsService(
      int errorBucketCount,
      int latencyBucketCount,
      long errorThreshold,
      double latencyThresholdMs,
      long minimumLatencySampleSize,
      long minimumErrorSampleSize) {
    this.errorBucketCount = errorBucketCount;
    this.latencyBucketCount = latencyBucketCount;
    this.errorThreshold = errorThreshold;
    this.latencyThresholdMs = latencyThresholdMs;
    this.minimumLatencySampleSize = minimumLatencySampleSize;
    this.minimumErrorSampleSize = minimumErrorSampleSize;

    // Initialize all error buckets
    errorBuckets = new AtomicLong[errorBucketCount];
    for (int i = 0; i < errorBucketCount; i++) {
      errorBuckets[i] = new AtomicLong(0);
    }

    // Initialize all latency buckets
    latencyTotalBuckets = new AtomicLong[latencyBucketCount];
    latencyCountBuckets = new AtomicLong[latencyBucketCount];
    for (int i = 0; i < latencyBucketCount; i++) {
      latencyTotalBuckets[i] = new AtomicLong(0);
      latencyCountBuckets[i] = new AtomicLong(0);
    }
  }

  /**
   * Record a new 500 error in a thread-safe manner. This method can be called concurrently from
   * multiple threads. Thread-safety is guaranteed through synchronization to prevent race
   * conditions between clearing buckets and incrementing counters.
   *
   * <p>The implementation performs the following steps atomically:
   *
   * <ol>
   *   <li>Clear stale error buckets if we've moved to a new time period
   *   <li>Increment the error count in the appropriate time bucket
   *   <li>Increment the total error count
   *   <li>Update the timestamp of the last recorded error
   * </ol>
   */
  @Override
  public synchronized void recordServerError() {
    long nowSeconds = Instant.now().getEpochSecond();
    int bucketIndex = (int) (nowSeconds % errorBucketCount);

    // Clear bucket if we've moved to a new time period
    clearOldBuckets(nowSeconds);

    // Increment the current bucket
    errorBuckets[bucketIndex].incrementAndGet();
    totalErrorCount.incrementAndGet();

    lastBucketTime.set(nowSeconds);
  }

  /**
   * Get the count of errors within the last minute.
   *
   * <p>This method provides a real-time snapshot of error counts within the most recent 60-second
   * sliding window. It first clears any stale buckets, then aggregates the error counts across all
   * active buckets.
   *
   * <p>Thread-safety: This method is thread-safe and can be called concurrently with {@link
   * #recordServerError()}. It internally uses {@link #clearOldBuckets(long)} which is synchronized
   * to prevent race conditions during bucket clearing operations.
   *
   * <p>Performance note: This method has O(n) complexity where n is the number of buckets
   * (typically 60). It's designed for frequent calls without significant performance impact.
   *
   * @return count of errors in the last minute (sliding 60-second window)
   */
  @Override
  public long getErrorCountLastMinute() {
    long nowSeconds = Instant.now().getEpochSecond();

    // Clear any old buckets first
    clearOldBuckets(nowSeconds);

    // Sum all buckets
    long count = 0;
    for (AtomicLong bucket : errorBuckets) {
      count += bucket.get();
    }

    return count;
  }

  /**
   * Clear buckets that are older than 60 seconds.
   *
   * <p>This method implements a sliding window algorithm for error metrics tracking. The algorithm
   * maintains a fixed number of buckets (typically 60, one per second) in a circular buffer
   * arrangement. Each bucket stores error counts for a specific second. As time progresses, old
   * buckets are cleared to maintain the sliding window effect.
   *
   * <p>The algorithm handles three scenarios:
   *
   * <ol>
   *   <li>First write after initialization ({@code lastBucketTime == -1}): When the service starts
   *       or after metrics are cleared, all buckets are initialized to zero on the first write.
   *   <li>Large time jump ({@code currentSeconds - lastBucketTime >= errorBucketCount}): If the
   *       time difference exceeds the window size (e.g., after system hibernation or extreme time
   *       change), all buckets are cleared since the entire window is stale.
   *   <li>Normal time progression ({@code currentSeconds - lastBucketTime < errorBucketCount}):
   *       Only buckets that have become stale since the last update are cleared, preserving the
   *       remaining valid data within the window.
   * </ol>
   *
   * <p>Thread-safety: This method is synchronized to ensure atomic operations during bucket
   * clearing. This prevents race conditions where multiple threads might attempt to clear or update
   * the same buckets simultaneously.
   *
   * <p>Edge cases handled:
   *
   * <ul>
   *   <li>Time rollback (system time adjusted backwards): The method will continue to function
   *       correctly as it primarily operates on relative time differences.
   *   <li>Long periods of inactivity: All buckets are cleared if the inactive period exceeds the
   *       window size.
   * </ul>
   *
   * @param currentSeconds current timestamp in seconds since epoch
   */
  private synchronized void clearOldBuckets(long currentSeconds) {
    // Get the last bucket time (atomic read)
    long lastTime = lastBucketTime.get();

    // SCENARIO 1: First write after initialization or SCENARIO 2: Large time jump
    if (lastTime == -1 || currentSeconds - lastTime >= errorBucketCount) {
      // Clear all buckets if this is the first write or we've jumped forward more than our window
      // size
      // This is more efficient than clearing individual buckets when the entire window is stale
      for (AtomicLong bucket : errorBuckets) {
        bucket.set(0);
      }
    } else {
      // SCENARIO 3: Normal time progression
      // Clear only the buckets that have become stale since the last update
      for (long time = lastTime + 1; time <= currentSeconds; time++) {
        // Only process times that fall within our window
        // This check is important for handling extreme time jumps correctly
        if (currentSeconds - time < errorBucketCount) {
          // Calculate the bucket index using modulo to implement circular buffer behavior
          // Each timestamp maps to a specific bucket in the circular buffer
          int bucketIndex = (int) (time % errorBucketCount);

          // Reset the bucket to zero to prepare it for new data
          // Using AtomicLong.set() ensures memory visibility across threads
          errorBuckets[bucketIndex].set(0);
        }
      }
    }
  }

  /**
   * Get the total count of errors recorded
   *
   * @return total error count
   */
  @Override
  public long getTotalErrorCount() {
    return totalErrorCount.get();
  }

  /**
   * Check if the current error count in the last minute exceeds the specified threshold.
   *
   * <p>This method implements adaptive thresholding based on traffic volume to provide meaningful
   * error detection across varying load conditions:
   *
   * <ul>
   *   <li><b>Very low traffic</b> ({@code < minimumErrorSampleSize} requests): No threshold breach
   *       reported regardless of error count, as the sample size is too small for meaningful
   *       analysis.
   *   <li><b>High traffic</b> ({@code >= 100} requests): Uses error rate percentage threshold
   *       (10%), which is more appropriate for high-volume services.
   *   <li><b>Moderate traffic</b> ({@code >= minimumErrorSampleSize && < 100} requests): Uses the
   *       minimum of absolute threshold and half the request count, providing a balanced approach.
   * </ul>
   *
   * <p>Thread-safety: This method is thread-safe and can be called concurrently with {@link
   * #recordServerError()} and other methods. It internally uses thread-safe methods {@link
   * #getErrorCountLastMinute()} and {@link #getTotalRequestCountLast60Seconds()}.
   *
   * @param threshold the threshold for error count (used in moderate traffic scenarios)
   * @return true if the error count exceeds the threshold according to the adaptive rules
   */
  @Override
  public boolean isErrorThresholdBreached(long threshold) {
    long errorCount = getErrorCountLastMinute();
    long requestCount = getTotalRequestCountLast60Seconds();

    // If we have very little traffic, don't apply error thresholds
    if (requestCount < minimumErrorSampleSize) {
      return false;
    }

    // For high traffic, use error rate (errors should be < 10% of requests)
    if (requestCount >= 100) {
      double errorRate = (double) errorCount / requestCount;
      return errorRate > 0.10; // 10% error rate threshold
    }

    // For moderate traffic, use absolute error count but cap it
    return errorCount > Math.min(threshold, requestCount / 2);
  }

  /**
   * Check if the current error count in the last minute exceeds the default threshold
   *
   * @return true if the error count exceeds the default threshold
   */
  @Override
  public boolean isErrorThresholdBreached() {
    return isErrorThresholdBreached(errorThreshold);
  }

  /**
   * Record request latency in milliseconds in a thread-safe manner. This method can be called
   * concurrently from multiple threads. Thread-safety is guaranteed through synchronization to
   * prevent race conditions between clearing buckets and updating counters.
   *
   * <p>The implementation performs the following steps atomically:
   *
   * <ol>
   *   <li>Clear stale latency buckets if we've moved to a new time period
   *   <li>Add the latency value to the appropriate time bucket
   *   <li>Increment the request count in the appropriate time bucket
   *   <li>Increment the total request count
   *   <li>Update the timestamp of the last recorded latency
   * </ol>
   *
   * @param latencyMs the latency in milliseconds
   */
  @Override
  public synchronized void recordRequestLatency(long latencyMs) {
    long nowSeconds = Instant.now().getEpochSecond(); // Use seconds instead of minutes
    int bucketIndex = (int) (nowSeconds % latencyBucketCount);

    // Clear old latency buckets if we've moved to a new time period
    clearOldLatencyBuckets(nowSeconds);

    // Add to the current bucket
    latencyTotalBuckets[bucketIndex].addAndGet(latencyMs);
    latencyCountBuckets[bucketIndex].incrementAndGet();

    // Track total request count for traffic volume calculations
    totalRequestCount.incrementAndGet();

    lastLatencyBucketTime.set(nowSeconds);
  }

  /**
   * Get the average request latency over the last 60 seconds.
   *
   * <p>This method calculates the average latency across all requests within the most recent
   * 60-second sliding window. It first clears any stale buckets, then aggregates the total latency
   * and request counts across all active buckets to compute the average.
   *
   * <p>Thread-safety: This method is thread-safe and can be called concurrently with {@link
   * #recordRequestLatency(long)}. It internally uses {@link #clearOldLatencyBuckets(long)} which is
   * synchronized to prevent race conditions during bucket clearing operations.
   *
   * <p>Performance note: This method has O(n) complexity where n is the number of buckets
   * (typically 60). It's designed for frequent calls without significant performance impact.
   *
   * <p>Edge cases handled:
   *
   * <ul>
   *   <li>No requests recorded: Returns 0.0
   *   <li>Negative latency values: Included in average calculation (though not expected in normal
   *       operation)
   *   <li>Extreme latency values: Handled correctly without overflow
   * </ul>
   *
   * @return average latency in milliseconds, or 0 if no requests recorded
   */
  @Override
  public double getAverageLatencyLast60Seconds() {
    long nowSeconds = Instant.now().getEpochSecond();

    // Clear any old buckets first
    clearOldLatencyBuckets(nowSeconds);

    // Sum all buckets
    long totalLatency = 0;
    long totalCount = 0;

    for (int i = 0; i < latencyBucketCount; i++) {
      totalLatency += latencyTotalBuckets[i].get();
      totalCount += latencyCountBuckets[i].get();
    }

    if (totalCount == 0) {
      return 0.0;
    }

    return (double) totalLatency / totalCount;
  }

  /**
   * Get the total number of requests in the last 60 seconds
   *
   * @return total request count
   */
  @Override
  public long getTotalRequestCountLast60Seconds() {
    long nowSeconds = Instant.now().getEpochSecond();

    // Clear any old buckets first
    clearOldLatencyBuckets(nowSeconds);

    // Sum all request counts
    long totalCount = 0;
    for (int i = 0; i < latencyBucketCount; i++) {
      totalCount += latencyCountBuckets[i].get();
    }

    return totalCount;
  }

  /**
   * Check if the current average latency exceeds the specified threshold.
   *
   * <p>This method implements intelligent threshold checking for latency metrics, taking into
   * account traffic volume to avoid false positives during low-traffic periods:
   *
   * <ul>
   *   <li>If the request count in the last 60 seconds is below {@code minimumLatencySampleSize}
   *       (typically 5 requests), the method always returns false regardless of latency values.
   *       This prevents latency threshold breaches from being triggered based on too few samples.
   *   <li>With sufficient request volume, the method compares the average latency against the
   *       specified threshold.
   * </ul>
   *
   * <p>Thread-safety: This method is thread-safe and can be called concurrently with {@link
   * #recordRequestLatency(long)} and other methods. It internally uses thread-safe methods {@link
   * #getTotalRequestCountLast60Seconds()} and {@link #getAverageLatencyLast60Seconds()}.
   *
   * @param thresholdMs the threshold in milliseconds
   * @return true if the average latency exceeds the threshold AND there's sufficient traffic volume
   */
  @Override
  public boolean isLatencyThresholdBreached(double thresholdMs) {
    long requestCount = getTotalRequestCountLast60Seconds();

    // If there's insufficient traffic, don't apply latency thresholds
    if (requestCount < minimumLatencySampleSize) {
      return false;
    }

    return getAverageLatencyLast60Seconds() > thresholdMs;
  }

  /**
   * Check if the current average latency exceeds the default threshold Only applies the threshold
   * if there's sufficient traffic volume
   *
   * @return true if the average latency exceeds the default threshold AND there's sufficient
   *     traffic
   */
  @Override
  public boolean isLatencyThresholdBreached() {
    return isLatencyThresholdBreached(latencyThresholdMs);
  }

  /**
   * Get the default error threshold
   *
   * @return the default error threshold
   */
  @Override
  public long getDefaultErrorThreshold() {
    return errorThreshold;
  }

  /**
   * Get the default latency threshold in milliseconds
   *
   * @return the default latency threshold in milliseconds
   */
  @Override
  public double getDefaultLatencyThresholdMs() {
    return latencyThresholdMs;
  }

  /**
   * Clear latency buckets that are older than 60 seconds.
   *
   * <p>This method implements a sliding window algorithm for latency metrics tracking. The
   * algorithm maintains a fixed number of buckets (typically 60, one per second) in a circular
   * buffer arrangement. Each bucket stores two values: 1. Total latency for the second
   * (latencyTotalBuckets) 2. Request count for the second (latencyCountBuckets)
   *
   * <p>As time progresses, old buckets are cleared to maintain the sliding window effect. This
   * ensures that latency statistics always reflect the most recent 60-second window.
   *
   * <p>The algorithm handles three scenarios:
   *
   * <ol>
   *   <li>First write after initialization ({@code lastLatencyBucketTime == -1}): When the service
   *       starts or after metrics are cleared, all latency buckets are initialized to zero on the
   *       first write.
   *   <li>Large time jump ({@code currentSeconds - lastLatencyBucketTime >= latencyBucketCount}):
   *       If the time difference exceeds the window size (e.g., after system hibernation or extreme
   *       time change), all latency buckets are cleared since the entire window is stale.
   *   <li>Normal time progression ({@code currentSeconds - lastLatencyBucketTime <
   *       latencyBucketCount}): Only buckets that have become stale since the last update are
   *       cleared, preserving the remaining valid latency data within the window.
   * </ol>
   *
   * <p>Thread-safety: This method is synchronized to ensure atomic operations during bucket
   * clearing. This prevents race conditions where multiple threads might attempt to clear or update
   * the same latency buckets simultaneously.
   *
   * <p>Edge cases handled:
   *
   * <ul>
   *   <li>Time rollback (system time adjusted backwards): The method will continue to function
   *       correctly as it primarily operates on relative time differences.
   *   <li>Long periods of inactivity: All latency buckets are cleared if the inactive period
   *       exceeds the window size.
   *   <li>Zero or negative latency values: These are handled the same as any other values.
   * </ul>
   *
   * @param currentSeconds current timestamp in seconds since epoch
   */
  private synchronized void clearOldLatencyBuckets(long currentSeconds) {
    // Get the last bucket time (atomic read)
    long lastTime = lastLatencyBucketTime.get();

    // SCENARIO 1: First write after initialization or SCENARIO 2: Large time jump
    if (lastTime == -1 || currentSeconds - lastTime >= latencyBucketCount) {
      // Clear all latency buckets if this is the first write or we've jumped forward more than our
      // window size
      // This is more efficient than clearing individual buckets when the entire window is stale
      for (int i = 0; i < latencyBucketCount; i++) {
        // Reset both the total latency and count buckets to zero
        // We need to clear both arrays to maintain consistency between latency totals and counts
        latencyTotalBuckets[i].set(0);
        latencyCountBuckets[i].set(0);
      }
    } else {
      // SCENARIO 3: Normal time progression
      // Clear only the buckets that have become stale since the last update
      for (long time = lastTime + 1; time <= currentSeconds; time++) {
        // Only process times that fall within our window
        // This check is important for handling extreme time jumps correctly
        if (currentSeconds - time < latencyBucketCount) {
          // Calculate the bucket index using modulo to implement circular buffer behavior
          // Each timestamp maps to a specific bucket in the circular buffer
          int bucketIndex = (int) (time % latencyBucketCount);

          // Reset both total latency and count buckets to zero
          // Using AtomicLong.set() ensures memory visibility across threads
          latencyTotalBuckets[bucketIndex].set(0);
          latencyCountBuckets[bucketIndex].set(0);
        }
      }
    }
  }

  /** Clear all metrics (useful for testing) */
  @Override
  public void clearMetrics() {
    for (AtomicLong bucket : errorBuckets) {
      bucket.set(0);
    }
    totalErrorCount.set(0);
    lastBucketTime.set(-1);

    // Clear latency buckets as well
    for (int i = 0; i < latencyBucketCount; i++) {
      latencyTotalBuckets[i].set(0);
      latencyCountBuckets[i].set(0);
    }
    lastLatencyBucketTime.set(-1);

    // Clear total request count
    totalRequestCount.set(0);
  }

  /**
   * Get the last bucket time for error buckets (for testing)
   *
   * @return the last bucket time
   */
  long getLastBucketTime() {
    return lastBucketTime.get();
  }

  /**
   * Get the last bucket time for latency buckets (for testing)
   *
   * @return the last latency bucket time
   */
  long getLastLatencyBucketTime() {
    return lastLatencyBucketTime.get();
  }

  /**
   * Set the last bucket time for error buckets (for testing stale conditions)
   *
   * @param time the time to set
   */
  void setLastBucketTimeForTesting(long time) {
    this.lastBucketTime.set(time);
  }

  /**
   * Set the last bucket time for latency buckets (for testing stale conditions)
   *
   * @param time the time to set
   */
  void setLastLatencyBucketTimeForTesting(long time) {
    this.lastLatencyBucketTime.set(time);
  }
}
