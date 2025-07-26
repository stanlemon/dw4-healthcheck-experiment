package com.example.dw.metrics;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to track application metrics, including error counts in a sliding window.
 * Uses a high-performance circular buffer approach with 60 buckets (one per second).
 */
public class MetricsService {
    private static final MetricsService INSTANCE = new MetricsService();

    // Use 60 buckets for a 60-second sliding window (one bucket per second)
    private static final int BUCKET_COUNT = 60;

    // Circular buffer of error counts per second
    private final AtomicLong[] errorBuckets = new AtomicLong[BUCKET_COUNT];
    // Track the last bucket we wrote to
    private volatile long lastBucketTime = -1;
    // Total count of errors (for logging/metrics purposes)
    private final AtomicLong totalErrorCount = new AtomicLong(0);

    private MetricsService() {
        // Initialize all buckets
        for (int i = 0; i < BUCKET_COUNT; i++) {
            errorBuckets[i] = new AtomicLong(0);
        }
    }

    public static MetricsService getInstance() {
        return INSTANCE;
    }

    /**
     * Record a new 500 error
     */
    public void recordServerError() {
        long nowSeconds = Instant.now().getEpochSecond();
        int bucketIndex = (int) (nowSeconds % BUCKET_COUNT);

        // Clear bucket if we've moved to a new time period
        clearOldBuckets(nowSeconds);

        // Increment the current bucket
        errorBuckets[bucketIndex].incrementAndGet();
        totalErrorCount.incrementAndGet();

        lastBucketTime = nowSeconds;
    }

    /**
     * Get the count of errors within the last minute
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

    /**
     * Clear buckets that are older than 60 seconds
     */
    private void clearOldBuckets(long currentSeconds) {
        // If this is the first write or we've moved significantly forward in time
        if (lastBucketTime == -1 || currentSeconds - lastBucketTime >= BUCKET_COUNT) {
            // Clear all buckets if we've jumped forward more than our window
            for (AtomicLong bucket : errorBuckets) {
                bucket.set(0);
            }
        } else {
            // Clear only the buckets that are now stale
            for (long time = lastBucketTime + 1; time <= currentSeconds; time++) {
                if (time - currentSeconds < BUCKET_COUNT) { // Only clear if within our window
                    int bucketIndex = (int) (time % BUCKET_COUNT);
                    errorBuckets[bucketIndex].set(0);
                }
            }
        }
    }

    /**
     * Get the total count of errors recorded
     * @return total error count
     */
    public long getTotalErrorCount() {
        return totalErrorCount.get();
    }

    /**
     * Clear all metrics (useful for testing)
     */
    public void clearMetrics() {
        for (AtomicLong bucket : errorBuckets) {
            bucket.set(0);
        }
        totalErrorCount.set(0);
        lastBucketTime = -1;
    }
}
