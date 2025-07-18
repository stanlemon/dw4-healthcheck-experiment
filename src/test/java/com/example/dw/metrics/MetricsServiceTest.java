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
    public void setUp() throws Exception {
        // Get the singleton instance
        metricsService = MetricsService.getInstance();

        // Reset the error timestamps and counter using reflection
        resetErrorTimestamps();
        resetTotalErrorCount();
    }

    private void resetErrorTimestamps() throws Exception {
        Field errorTimestampsField = MetricsService.class.getDeclaredField("errorTimestamps");
        errorTimestampsField.setAccessible(true);
        Queue<Instant> errorTimestamps = (Queue<Instant>) errorTimestampsField.get(metricsService);
        errorTimestamps.clear();
    }

    private void resetTotalErrorCount() throws Exception {
        Field totalErrorCountField = MetricsService.class.getDeclaredField("totalErrorCount");
        totalErrorCountField.setAccessible(true);
        AtomicLong totalErrorCount = (AtomicLong) totalErrorCountField.get(metricsService);
        totalErrorCount.set(0);
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
    public void testGetErrorCountLastMinute() throws Exception {
        // Initial state
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);

        // Add some timestamps directly to the queue
        Field errorTimestampsField = MetricsService.class.getDeclaredField("errorTimestamps");
        errorTimestampsField.setAccessible(true);
        Queue<Instant> errorTimestamps = (Queue<Instant>) errorTimestampsField.get(metricsService);

        // Add 3 recent errors (within last minute)
        Instant now = Instant.now();
        errorTimestamps.add(now.minus(10, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(30, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(50, ChronoUnit.SECONDS));

        // Add 2 old errors (more than a minute ago)
        errorTimestamps.add(now.minus(61, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(120, ChronoUnit.SECONDS));

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
