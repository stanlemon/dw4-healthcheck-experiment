package com.example.dw.metrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Default implementation of MetricsService to track application metrics, including error counts and
 * request latency in sliding windows. Uses a high-performance circular buffer approach with
 * configurable buckets for error tracking (one per second) and latency tracking (one per second).
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
  private volatile long lastBucketTime = -1;
  // Total count of errors (for logging/metrics purposes)
  private final AtomicLong totalErrorCount = new AtomicLong(0);

  // Latency tracking with 60-second sliding window
  // Array to track total latency per second bucket
  private final AtomicLong[] latencyTotalBuckets;
  // Array to track count of requests per second bucket
  private final AtomicLong[] latencyCountBuckets;
  // Track the last minute bucket we wrote to for latency
  private volatile long lastLatencyBucketTime = -1;
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

  /** Record a new 500 error */
  @Override
  public void recordServerError() {
    long nowSeconds = Instant.now().getEpochSecond();
    int bucketIndex = (int) (nowSeconds % errorBucketCount);

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

  /** Clear buckets that are older than 60 seconds */
  private void clearOldBuckets(long currentSeconds) {
    // If this is the first write or we've moved significantly forward in time
    if (lastBucketTime == -1 || currentSeconds - lastBucketTime >= errorBucketCount) {
      // Clear all buckets if we've jumped forward more than our window
      for (AtomicLong bucket : errorBuckets) {
        bucket.set(0);
      }
    } else {
      // Clear only the buckets that are now stale
      for (long time = lastBucketTime + 1; time <= currentSeconds; time++) {
        if (currentSeconds - time < errorBucketCount) { // Only clear if within our window
          int bucketIndex = (int) (time % errorBucketCount);
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
   * Check if the current error count in the last minute exceeds the specified threshold Uses
   * intelligent thresholding based on traffic volume: - Low traffic: Uses absolute error count
   * threshold - High traffic: Uses error rate percentage
   *
   * @param threshold the threshold for error count
   * @return true if the error count exceeds the threshold
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
   * Record request latency in milliseconds
   *
   * @param latencyMs the latency in milliseconds
   */
  @Override
  public void recordRequestLatency(long latencyMs) {
    long nowSeconds = Instant.now().getEpochSecond(); // Use seconds instead of minutes
    int bucketIndex = (int) (nowSeconds % latencyBucketCount);

    // Clear old latency buckets if we've moved to a new time period
    clearOldLatencyBuckets(nowSeconds);

    // Add to the current bucket
    latencyTotalBuckets[bucketIndex].addAndGet(latencyMs);
    latencyCountBuckets[bucketIndex].incrementAndGet();

    // Track total request count for traffic volume calculations
    totalRequestCount.incrementAndGet();

    lastLatencyBucketTime = nowSeconds;
  }

  /**
   * Get the average request latency over the last 60 seconds
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
   * Check if the current average latency exceeds the specified threshold Only applies the threshold
   * if there's sufficient traffic volume
   *
   * @param thresholdMs the threshold in milliseconds
   * @return true if the average latency exceeds the threshold AND there's sufficient traffic
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

  /** Clear latency buckets that are older than 60 seconds */
  private void clearOldLatencyBuckets(long currentSeconds) {
    // If this is the first write or we've moved significantly forward in time
    if (lastLatencyBucketTime == -1
        || currentSeconds - lastLatencyBucketTime >= latencyBucketCount) {
      // Clear all buckets if we've jumped forward more than our window
      for (int i = 0; i < latencyBucketCount; i++) {
        latencyTotalBuckets[i].set(0);
        latencyCountBuckets[i].set(0);
      }
    } else {
      // Clear only the buckets that are now stale
      for (long time = lastLatencyBucketTime + 1; time <= currentSeconds; time++) {
        if (currentSeconds - time < latencyBucketCount) { // Only clear if within our window
          int bucketIndex = (int) (time % latencyBucketCount);
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
    lastBucketTime = -1;

    // Clear latency buckets as well
    for (int i = 0; i < latencyBucketCount; i++) {
      latencyTotalBuckets[i].set(0);
      latencyCountBuckets[i].set(0);
    }
    lastLatencyBucketTime = -1;

    // Clear total request count
    totalRequestCount.set(0);
  }

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
