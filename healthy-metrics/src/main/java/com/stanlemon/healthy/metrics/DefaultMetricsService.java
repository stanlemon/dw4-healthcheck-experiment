package com.stanlemon.healthy.metrics;

import java.time.Clock;
import java.util.Arrays;

/**
 * Default {@link MetricsService}. Keeps a 60-second sliding window as a ring of one-second buckets:
 * each bucket holds the request count, error count, and total latency for one second.
 *
 * <p>Every public method is {@code synchronized} and reads the clock inside the lock. That matters:
 * a reader that read the clock first and then waited for the lock could come back with a timestamp
 * older than the last write, and the window logic would treat that as the clock moving backwards
 * and wipe every bucket. Holding the lock across both steps keeps timestamps in order. Because all
 * state is guarded by the one lock, plain {@code long} fields are enough.
 */
public class DefaultMetricsService implements MetricsService {

  private static final int WINDOW_SECONDS = 60;

  /** Low-traffic error threshold: more errors than this in the window is unhealthy. */
  private static final long ERROR_THRESHOLD = 100;

  /** Average latency, in milliseconds, above which the window is unhealthy. */
  private static final double LATENCY_THRESHOLD_MS = 100.0;

  /** Fewer requests than this and the latency average is too noisy to judge. */
  private static final long MINIMUM_LATENCY_SAMPLE_SIZE = 5;

  /** Fewer requests than this and the error count is too noisy to judge. */
  private static final long MINIMUM_ERROR_SAMPLE_SIZE = 10;

  /**
   * Once the window holds at least this many requests, the error check switches from an absolute
   * count to a percentage rate — below this traffic level a handful of errors doesn't mean much.
   */
  private static final long HIGH_TRAFFIC_THRESHOLD = 100;

  /** Error rate (10%) used once traffic crosses {@link #HIGH_TRAFFIC_THRESHOLD}. */
  private static final double HIGH_TRAFFIC_ERROR_RATE = 0.10;

  private final Clock clock;

  // All fields below are guarded by this instance's monitor.
  private final long[] requestBuckets = new long[WINDOW_SECONDS];
  private final long[] errorBuckets = new long[WINDOW_SECONDS];
  private final long[] latencyTotalBuckets = new long[WINDOW_SECONDS];

  /** The epoch second the window was last moved to; -1 before the first call. */
  private long windowEndSecond = -1;

  private long totalErrors;

  /** Creates a service that reads the system UTC clock. */
  public DefaultMetricsService() {
    this(Clock.systemUTC());
  }

  /**
   * Creates a service that reads the given clock, so tests can control time.
   *
   * @param clock the clock to use for time measurements
   */
  public DefaultMetricsService(Clock clock) {
    this.clock = clock;
  }

  @Override
  public synchronized void recordRequest(long latencyMs, boolean serverError) {
    int bucket = advanceWindow();
    requestBuckets[bucket]++;
    latencyTotalBuckets[bucket] += Math.max(latencyMs, 0);
    if (serverError) {
      errorBuckets[bucket]++;
      totalErrors++;
    }
  }

  @Override
  public synchronized MetricsSnapshot snapshot() {
    advanceWindow();
    long requests = sum(requestBuckets);
    long errors = sum(errorBuckets);
    double averageLatency = requests == 0 ? 0.0 : (double) sum(latencyTotalBuckets) / requests;

    return new MetricsSnapshot(
        errors,
        totalErrors,
        requests,
        averageLatency,
        isErrorThresholdBreached(errors, requests),
        requests >= MINIMUM_LATENCY_SAMPLE_SIZE && averageLatency > LATENCY_THRESHOLD_MS,
        ERROR_THRESHOLD,
        LATENCY_THRESHOLD_MS);
  }

  private static boolean isErrorThresholdBreached(long errors, long requests) {
    if (requests < MINIMUM_ERROR_SAMPLE_SIZE) {
      return false;
    }
    if (requests >= HIGH_TRAFFIC_THRESHOLD) {
      return (double) errors / requests > HIGH_TRAFFIC_ERROR_RATE;
    }
    // Moderate traffic: breach when more than half the requests failed (capped by the threshold).
    return errors > Math.min(ERROR_THRESHOLD, requests / 2);
  }

  /**
   * Moves the window to the current second, zeroing every bucket for the seconds that passed since
   * the last call. Must be called while holding this instance's monitor.
   *
   * @return the bucket index for the current second
   */
  private int advanceWindow() {
    long now = clock.instant().getEpochSecond();

    // Clear everything if the clock went backwards (NTP step-back, VM resume) or the whole window
    // went stale. The initial -1 lands in one of these cases, or in the loop below for a clock
    // near the epoch; either way the buckets are already zero.
    if (now < windowEndSecond || now - windowEndSecond >= WINDOW_SECONDS) {
      Arrays.fill(requestBuckets, 0);
      Arrays.fill(errorBuckets, 0);
      Arrays.fill(latencyTotalBuckets, 0);
    } else {
      for (long second = windowEndSecond + 1; second <= now; second++) {
        int bucket = bucketFor(second);
        requestBuckets[bucket] = 0;
        errorBuckets[bucket] = 0;
        latencyTotalBuckets[bucket] = 0;
      }
    }

    windowEndSecond = now;
    return bucketFor(now);
  }

  private static int bucketFor(long epochSecond) {
    return (int) (epochSecond % WINDOW_SECONDS);
  }

  private static long sum(long[] buckets) {
    long total = 0;
    for (long value : buckets) {
      total += value;
    }
    return total;
  }
}
