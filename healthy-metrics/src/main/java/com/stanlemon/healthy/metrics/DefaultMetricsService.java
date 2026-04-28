package com.stanlemon.healthy.metrics;

import java.time.Clock;
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
  private final int errorBucketCount;
  private final int latencyBucketCount;
  private final long errorThreshold;
  private final double latencyThresholdMs;
  private final long minimumLatencySampleSize;
  private final long minimumErrorSampleSize;
  private final Clock clock;

  private final AtomicLong[] errorBuckets;
  private final AtomicLong lastBucketTime = new AtomicLong(-1);
  private final AtomicLong totalErrorCount = new AtomicLong(0);

  private final AtomicLong[] latencyTotalBuckets;
  private final AtomicLong[] latencyCountBuckets;
  private final AtomicLong lastLatencyBucketTime = new AtomicLong(-1);
  private final AtomicLong totalRequestCount = new AtomicLong(0);

  /** Creates a new DefaultMetricsService with the default configuration. */
  public DefaultMetricsService() {
    this(60, 60, 100, 100.0, 5, 10, Clock.systemUTC());
  }

  /**
   * Creates a new DefaultMetricsService with a custom clock. Primarily useful for testing
   * time-dependent behavior without relying on real wall-clock time.
   *
   * @param clock the clock to use for time measurements
   */
  public DefaultMetricsService(Clock clock) {
    this(60, 60, 100, 100.0, 5, 10, clock);
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
   * @param clock the clock to use for time measurements
   */
  public DefaultMetricsService(
      int errorBucketCount,
      int latencyBucketCount,
      long errorThreshold,
      double latencyThresholdMs,
      long minimumLatencySampleSize,
      long minimumErrorSampleSize,
      Clock clock) {
    this.errorBucketCount = errorBucketCount;
    this.latencyBucketCount = latencyBucketCount;
    this.errorThreshold = errorThreshold;
    this.latencyThresholdMs = latencyThresholdMs;
    this.minimumLatencySampleSize = minimumLatencySampleSize;
    this.minimumErrorSampleSize = minimumErrorSampleSize;
    this.clock = clock;

    errorBuckets = new AtomicLong[errorBucketCount];
    for (int i = 0; i < errorBucketCount; i++) {
      errorBuckets[i] = new AtomicLong(0);
    }

    latencyTotalBuckets = new AtomicLong[latencyBucketCount];
    latencyCountBuckets = new AtomicLong[latencyBucketCount];
    for (int i = 0; i < latencyBucketCount; i++) {
      latencyTotalBuckets[i] = new AtomicLong(0);
      latencyCountBuckets[i] = new AtomicLong(0);
    }
  }

  @Override
  public synchronized void recordServerError() {
    long nowSeconds = clock.instant().getEpochSecond();
    int bucketIndex = (int) (nowSeconds % errorBucketCount);

    clearOldBuckets(nowSeconds);

    errorBuckets[bucketIndex].incrementAndGet();
    totalErrorCount.incrementAndGet();

    lastBucketTime.set(nowSeconds);
  }

  @Override
  public long getErrorCountLastMinute() {
    long nowSeconds = clock.instant().getEpochSecond();

    clearOldBuckets(nowSeconds);

    long count = 0;
    for (AtomicLong bucket : errorBuckets) {
      count += bucket.get();
    }

    return count;
  }

  private synchronized void clearOldBuckets(long currentSeconds) {
    long lastTime = lastBucketTime.get();

    if (lastTime == -1 || currentSeconds - lastTime >= errorBucketCount) {
      for (AtomicLong bucket : errorBuckets) {
        bucket.set(0);
      }
    } else {
      // The else branch guarantees currentSeconds - lastTime < errorBucketCount, so every
      // timestamp in this loop is within the window; no inner guard needed.
      for (long time = lastTime + 1; time <= currentSeconds; time++) {
        int bucketIndex = (int) (time % errorBucketCount);
        errorBuckets[bucketIndex].set(0);
      }
    }
  }

  @Override
  public long getTotalErrorCount() {
    return totalErrorCount.get();
  }

  @Override
  public boolean isErrorThresholdBreached(long threshold) {
    long errorCount = getErrorCountLastMinute();
    long requestCount = getTotalRequestCountLast60Seconds();

    if (requestCount < minimumErrorSampleSize) {
      return false;
    }

    if (requestCount >= 100) {
      double errorRate = (double) errorCount / requestCount;
      return errorRate > 0.10;
    }

    return errorCount > Math.min(threshold, requestCount / 2);
  }

  @Override
  public boolean isErrorThresholdBreached() {
    return isErrorThresholdBreached(errorThreshold);
  }

  @Override
  public synchronized void recordRequestLatency(long latencyMs) {
    long clamped = Math.max(latencyMs, 0);
    long nowSeconds = clock.instant().getEpochSecond();
    int bucketIndex = (int) (nowSeconds % latencyBucketCount);

    clearOldLatencyBuckets(nowSeconds);

    latencyTotalBuckets[bucketIndex].addAndGet(clamped);
    latencyCountBuckets[bucketIndex].incrementAndGet();

    totalRequestCount.incrementAndGet();

    lastLatencyBucketTime.set(nowSeconds);
  }

  @Override
  public double getAverageLatencyLast60Seconds() {
    long nowSeconds = clock.instant().getEpochSecond();

    clearOldLatencyBuckets(nowSeconds);

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

  @Override
  public long getTotalRequestCountLast60Seconds() {
    long nowSeconds = clock.instant().getEpochSecond();

    clearOldLatencyBuckets(nowSeconds);

    long totalCount = 0;
    for (int i = 0; i < latencyBucketCount; i++) {
      totalCount += latencyCountBuckets[i].get();
    }

    return totalCount;
  }

  @Override
  public boolean isLatencyThresholdBreached(double thresholdMs) {
    long requestCount = getTotalRequestCountLast60Seconds();

    if (requestCount < minimumLatencySampleSize) {
      return false;
    }

    return getAverageLatencyLast60Seconds() > thresholdMs;
  }

  @Override
  public boolean isLatencyThresholdBreached() {
    return isLatencyThresholdBreached(latencyThresholdMs);
  }

  @Override
  public long getDefaultErrorThreshold() {
    return errorThreshold;
  }

  @Override
  public double getDefaultLatencyThresholdMs() {
    return latencyThresholdMs;
  }

  private synchronized void clearOldLatencyBuckets(long currentSeconds) {
    long lastTime = lastLatencyBucketTime.get();

    if (lastTime == -1 || currentSeconds - lastTime >= latencyBucketCount) {
      for (int i = 0; i < latencyBucketCount; i++) {
        latencyTotalBuckets[i].set(0);
        latencyCountBuckets[i].set(0);
      }
    } else {
      // The else branch guarantees currentSeconds - lastTime < latencyBucketCount, so every
      // timestamp in this loop is within the window; no inner guard needed.
      for (long time = lastTime + 1; time <= currentSeconds; time++) {
        int bucketIndex = (int) (time % latencyBucketCount);
        latencyTotalBuckets[bucketIndex].set(0);
        latencyCountBuckets[bucketIndex].set(0);
      }
    }
  }

  @Override
  public synchronized void clearMetrics() {
    for (AtomicLong bucket : errorBuckets) {
      bucket.set(0);
    }
    totalErrorCount.set(0);
    lastBucketTime.set(-1);

    for (int i = 0; i < latencyBucketCount; i++) {
      latencyTotalBuckets[i].set(0);
      latencyCountBuckets[i].set(0);
    }
    lastLatencyBucketTime.set(-1);

    totalRequestCount.set(0);
  }
}
