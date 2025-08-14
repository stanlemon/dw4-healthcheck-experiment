package com.example.dw.metrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to track application metrics, including error counts and request latency in sliding
 * windows. Uses a high-performance circular buffer approach with 60 buckets for error tracking (one
 * per second) and 60 buckets for latency tracking (one per minute).
 */
public class MetricsService {
  private static final MetricsService INSTANCE = new MetricsService();

  // Use 60 buckets for a 60-second sliding window (one bucket per second) for errors
  private static final int ERROR_BUCKET_COUNT = 60;
  // Use 60 buckets for a 60-second sliding window (one bucket per second) for latency
  private static final int LATENCY_BUCKET_COUNT = 60;
  // Default error threshold for last minute
  private static final long DEFAULT_ERROR_THRESHOLD = 100;
  // Default latency threshold in milliseconds
  private static final double DEFAULT_LATENCY_THRESHOLD_MS = 100.0;

  // Circular buffer of error counts per second
  private final AtomicLong[] errorBuckets = new AtomicLong[ERROR_BUCKET_COUNT];
  // Track the last bucket we wrote to
  private volatile long lastBucketTime = -1;
  // Total count of errors (for logging/metrics purposes)
  private final AtomicLong totalErrorCount = new AtomicLong(0);

  // Latency tracking with 60-second sliding window
  // Array to track total latency per second bucket
  private final AtomicLong[] latencyTotalBuckets = new AtomicLong[LATENCY_BUCKET_COUNT];
  // Array to track count of requests per second bucket
  private final AtomicLong[] latencyCountBuckets = new AtomicLong[LATENCY_BUCKET_COUNT];
  // Track the last second bucket we wrote to for latency
  private volatile long lastLatencyBucketTime = -1;

  private MetricsService() {
    // Initialize all error buckets
    for (int i = 0; i < ERROR_BUCKET_COUNT; i++) {
      errorBuckets[i] = new AtomicLong(0);
    }

    // Initialize all latency buckets
    for (int i = 0; i < LATENCY_BUCKET_COUNT; i++) {
      latencyTotalBuckets[i] = new AtomicLong(0);
      latencyCountBuckets[i] = new AtomicLong(0);
    }
  }

  public static MetricsService getInstance() {
    return INSTANCE;
  }

  /** Record a new 500 error */
  public void recordServerError() {
    long nowSeconds = Instant.now().getEpochSecond();
    int bucketIndex = (int) (nowSeconds % ERROR_BUCKET_COUNT);

    // Clear bucket if we've moved to a new time period
    clearOldBuckets(nowSeconds);

    // Increment the current bucket
    errorBuckets[bucketIndex].incrementAndGet();
    totalErrorCount.incrementAndGet();

    lastBucketTime = nowSeconds;
  }

  /**
   * Get the count of errors within the last minute
   *
   * @return count of errors in the last minute
   */
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

  /** Clear buckets that are older than 60 seconds */
  private void clearOldBuckets(long currentSeconds) {
    // If this is the first write or we've moved significantly forward in time
    if (lastBucketTime == -1 || currentSeconds - lastBucketTime >= ERROR_BUCKET_COUNT) {
      // Clear all buckets if we've jumped forward more than our window
      for (AtomicLong bucket : errorBuckets) {
        bucket.set(0);
      }
    } else {
      // Clear only the buckets that are now stale
      for (long time = lastBucketTime + 1; time <= currentSeconds; time++) {
        if (currentSeconds - time < ERROR_BUCKET_COUNT) { // Only clear if within our window
          int bucketIndex = (int) (time % ERROR_BUCKET_COUNT);
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
  public long getTotalErrorCount() {
    return totalErrorCount.get();
  }

  /**
   * Check if the current error count in the last minute exceeds the specified threshold
   *
   * @param threshold the threshold for error count
   * @return true if the error count exceeds the threshold
   */
  public boolean isErrorThresholdBreached(long threshold) {
    return getErrorCountLastMinute() > threshold;
  }

  /**
   * Check if the current error count in the last minute exceeds the default threshold
   *
   * @return true if the error count exceeds the default threshold
   */
  public boolean isErrorThresholdBreached() {
    return isErrorThresholdBreached(DEFAULT_ERROR_THRESHOLD);
  }

  /**
   * Record request latency in milliseconds
   *
   * @param latencyMs the latency in milliseconds
   */
  public void recordRequestLatency(long latencyMs) {
    long nowSeconds = Instant.now().getEpochSecond(); // Use seconds instead of minutes
    int bucketIndex = (int) (nowSeconds % LATENCY_BUCKET_COUNT);

    // Clear old latency buckets if we've moved to a new time period
    clearOldLatencyBuckets(nowSeconds);

    // Add to the current bucket
    latencyTotalBuckets[bucketIndex].addAndGet(latencyMs);
    latencyCountBuckets[bucketIndex].incrementAndGet();

    lastLatencyBucketTime = nowSeconds;
  }

  /**
   * Get the average request latency over the last 60 seconds
   *
   * @return average latency in milliseconds, or 0 if no requests recorded
   */
  public double getAverageLatencyLast60Seconds() {
    long nowSeconds = Instant.now().getEpochSecond();

    // Clear any old buckets first
    clearOldLatencyBuckets(nowSeconds);

    // Sum all buckets
    long totalLatency = 0;
    long totalCount = 0;

    for (int i = 0; i < LATENCY_BUCKET_COUNT; i++) {
      totalLatency += latencyTotalBuckets[i].get();
      totalCount += latencyCountBuckets[i].get();
    }

    if (totalCount == 0) {
      return 0.0;
    }

    return (double) totalLatency / totalCount;
  }

  /**
   * Check if the current average latency exceeds the specified threshold
   *
   * @param thresholdMs the threshold in milliseconds
   * @return true if the average latency exceeds the threshold
   */
  public boolean isLatencyThresholdBreached(double thresholdMs) {
    return getAverageLatencyLast60Seconds() > thresholdMs;
  }

  /**
   * Check if the current average latency exceeds the default threshold
   *
   * @return true if the average latency exceeds the default threshold
   */
  public boolean isLatencyThresholdBreached() {
    return isLatencyThresholdBreached(DEFAULT_LATENCY_THRESHOLD_MS);
  }

  /**
   * Get the default error threshold
   *
   * @return the default error threshold
   */
  public static long getDefaultErrorThreshold() {
    return DEFAULT_ERROR_THRESHOLD;
  }

  /**
   * Get the default latency threshold in milliseconds
   *
   * @return the default latency threshold in milliseconds
   */
  public static double getDefaultLatencyThresholdMs() {
    return DEFAULT_LATENCY_THRESHOLD_MS;
  }

  /** Clear latency buckets that are older than 60 seconds */
  private void clearOldLatencyBuckets(long currentSeconds) {
    // If this is the first write or we've moved significantly forward in time
    if (lastLatencyBucketTime == -1
        || currentSeconds - lastLatencyBucketTime >= LATENCY_BUCKET_COUNT) {
      // Clear all buckets if we've jumped forward more than our window
      for (int i = 0; i < LATENCY_BUCKET_COUNT; i++) {
        latencyTotalBuckets[i].set(0);
        latencyCountBuckets[i].set(0);
      }
    } else {
      // Clear only the buckets that are now stale
      for (long time = lastLatencyBucketTime + 1; time <= currentSeconds; time++) {
        if (currentSeconds - time < LATENCY_BUCKET_COUNT) { // Only clear if within our window
          int bucketIndex = (int) (time % LATENCY_BUCKET_COUNT);
          latencyTotalBuckets[bucketIndex].set(0);
          latencyCountBuckets[bucketIndex].set(0);
        }
      }
    }
  }

  /** Clear all metrics (useful for testing) */
  public void clearMetrics() {
    for (AtomicLong bucket : errorBuckets) {
      bucket.set(0);
    }
    totalErrorCount.set(0);
    lastBucketTime = -1;

    // Clear latency buckets as well
    for (int i = 0; i < LATENCY_BUCKET_COUNT; i++) {
      latencyTotalBuckets[i].set(0);
      latencyCountBuckets[i].set(0);
    }
    lastLatencyBucketTime = -1;
  }

  // Package-private methods for testing bucket clearing edge cases
  // These should only be used by test classes in the same package

  /**
   * Get the last bucket time for error buckets (for testing)
   *
   * @return the last bucket time
   */
  long getLastBucketTime() {
    return lastBucketTime;
  }

  /**
   * Get the last bucket time for latency buckets (for testing)
   *
   * @return the last latency bucket time
   */
  long getLastLatencyBucketTime() {
    return lastLatencyBucketTime;
  }

  /**
   * Set the last bucket time for error buckets (for testing stale conditions)
   *
   * @param time the time to set
   */
  void setLastBucketTimeForTesting(long time) {
    this.lastBucketTime = time;
  }

  /**
   * Set the last bucket time for latency buckets (for testing stale conditions)
   *
   * @param time the time to set
   */
  void setLastLatencyBucketTimeForTesting(long time) {
    this.lastLatencyBucketTime = time;
  }
}
