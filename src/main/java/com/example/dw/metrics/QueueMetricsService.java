package com.example.dw.metrics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to track application metrics, including error counts in a sliding window.
 */
public enum QueueMetricsService implements MetricsService {
    INSTANCE;

    // Queue to store timestamps of 500 errors
    private final Queue<Instant> errorTimestamps = new ConcurrentLinkedQueue<>();
    // Total count of errors (for logging/metrics purposes)
    private final AtomicLong totalErrorCount = new AtomicLong(0);

    /**
     * Record a new 500 error
     */
    @Override
    public void recordServerError() {
        errorTimestamps.add(Instant.now());
        totalErrorCount.incrementAndGet();
    }

    /**
     * Get the count of errors within the last minute
     * @return count of errors in the last minute
     */
    @Override
    public long getErrorCountLastMinute() {
        // Get the cutoff time (1 minute ago)
        final Instant cutoff = Instant.now().minus(1, ChronoUnit.MINUTES);

        // Remove old timestamps using removeIf
        errorTimestamps.removeIf(timestamp -> timestamp.isBefore(cutoff));

        // Return the count of timestamps still in the queue (they're all within last minute)
        return errorTimestamps.size();
    }

    /**
     * Get the total count of errors recorded
     * @return total error count
     */
    @Override
    public long getTotalErrorCount() {
        return totalErrorCount.get();
    }

    /**
     * Reset all metrics (for testing purposes)
     */
    @Override
    public void resetMetrics() {
        errorTimestamps.clear();
        totalErrorCount.set(0);
    }
}
