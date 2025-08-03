package com.example.dw.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public abstract class AbstractMetricsServiceTest {

    protected MetricsService metricsService;

    protected abstract MetricsService createMetricsService();

    @BeforeEach
    public void setUp() {
        metricsService = createMetricsService();
        metricsService.resetMetrics();
    }

    @Test
    public void testRecordServerError() {
        // Initial state
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(0);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);

        // Record a server error
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
        Field errorTimestampsField = metricsService.getClass().getDeclaredField("errorTimestamps");
        errorTimestampsField.setAccessible(true);
        Queue<Instant> errorTimestamps = (Queue<Instant>) errorTimestampsField.get(metricsService);

        // Add recent errors (within last minute)
        Instant now = Instant.now();
        errorTimestamps.add(now.minus(10, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(30, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(50, ChronoUnit.SECONDS));

        // Add older errors (more than a minute ago)
        errorTimestamps.add(now.minus(61, ChronoUnit.SECONDS));
        errorTimestamps.add(now.minus(120, ChronoUnit.SECONDS));

        // Update the total error count to match
        Field totalErrorCountField = metricsService.getClass().getDeclaredField("totalErrorCount");
        totalErrorCountField.setAccessible(true);
        AtomicLong totalErrorCount = (AtomicLong) totalErrorCountField.get(metricsService);
        totalErrorCount.set(5);

        // Verify only recent errors are counted and old ones are removed
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(3);
        assertThat(errorTimestamps.size()).isEqualTo(3);
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(5);
    }
}