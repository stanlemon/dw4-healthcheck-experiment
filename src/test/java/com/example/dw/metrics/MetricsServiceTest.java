package com.example.dw.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;

public class MetricsServiceTest {

    private MetricsService metricsService;

    @BeforeEach
    public void setUp() {
        // Get the singleton instance and clear it
        metricsService = MetricsService.getInstance();
        metricsService.clearMetrics();
    }

    @Test
    public void testRecordServerError() {
        // Initial state
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(0);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);

        // Record an error
        metricsService.recordServerError();

        // Verify counts increased
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(1);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);

        // Record another error
        metricsService.recordServerError();

        // Verify counts increased again
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(2);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetErrorCountLastMinute() throws Exception {
        // Initial state
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);

        // Add some timestamps directly to the queue
        Field errorTimestampsField = MetricsService.class.getDeclaredField("errorTimestamps");
        errorTimestampsField.setAccessible(true);
        Queue<Instant> errorTimestamps = (Queue<Instant>) errorTimestampsField.get(metricsService);

        // Add timestamps in chronological order (oldest first, as they would naturally occur)
        Instant now = Instant.now();

        // Add 2 old errors first (more than a minute ago) - these should be cleaned up
        errorTimestamps.add(now.minus(120, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(61, ChronoUnit.SECONDS));

        // Then add 3 recent errors (within last minute) - these should remain
        errorTimestamps.add(now.minus(50, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(30, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(10, ChronoUnit.SECONDS));

        // Update total count to match
        Field totalErrorCountField = MetricsService.class.getDeclaredField("totalErrorCount");
        totalErrorCountField.setAccessible(true);
        AtomicLong totalErrorCount = (AtomicLong) totalErrorCountField.get(metricsService);
        totalErrorCount.set(5);

        // Verify only recent errors are counted, and old ones are removed
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(3);

        // Check that old timestamps were removed from the queue
        assertThat(errorTimestamps.size()).isEqualTo(3);

        // Total count should still include all errors ever recorded
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(5);
    }
}
