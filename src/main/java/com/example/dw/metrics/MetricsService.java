package com.example.dw.metrics;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Service to track application metrics, including error counts in a sliding window.
 */
public class MetricsService {
    private static final MetricsService INSTANCE = new MetricsService();

    // Queue to store timestamps of 500 errors
    private final Queue<Instant> errorTimestamps = new ConcurrentLinkedQueue<>();
    // Total count of errors (for logging/metrics purposes)
    private final AtomicLong totalErrorCount = new AtomicLong(0);

    private MetricsService() {
        // Private constructor for singleton
    }

    public static MetricsService getInstance() {
        return INSTANCE;
    }

    /**
     * Record a new 500 error
     */
    public void recordServerError() {
        errorTimestamps.add(Instant.now());
        totalErrorCount.incrementAndGet();
    }

    /**
     * Get the count of errors within the last minute
     * @return count of errors in the last minute
     */
    public long getErrorCountLastMinute() {
        // Get the cutoff time (1 minute ago)
        final Instant cutoff = Instant.now().minus(1, ChronoUnit.MINUTES);

        // Remove old timestamps using removeIf
        errorTimestamps.removeIf(timestamp -> timestamp.isBefore(cutoff));

        // Return the count of timestamps still in the queue (they're all within last minute)
        return errorTimestamps.size();
    }

    /**
     * Reset the metrics, clearing all error timestamps and total error count
     */
    public void resetMetrics() {
        // Clear the error timestamps and reset the total error count
        errorTimestamps.clear();
        totalErrorCount.set(0);
    }

    /**
     * Get the total count of errors recorded
     * @return total error count
     */
    public long getTotalErrorCount() {
        return totalErrorCount.get();
    }
}
