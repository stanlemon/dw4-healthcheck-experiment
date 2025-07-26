package com.example.dw.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
    public void testGetErrorCountLastMinute() {
        // Initial state
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);

        // Record some errors
        metricsService.recordServerError();
        metricsService.recordServerError();
        metricsService.recordServerError();

        // Verify all errors are counted
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(3);
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(3);

        // Call getErrorCountLastMinute multiple times to verify consistency
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(3);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(3);

        // Total count should remain unchanged after multiple reads
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(3);
    }

    @Test
    public void testClearMetrics() {
        // Record some errors
        metricsService.recordServerError();
        metricsService.recordServerError();

        // Verify errors are recorded
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(2);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(2);

        // Clear metrics
        metricsService.clearMetrics();

        // Verify all counts are reset
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(0);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);
    }

    @Test
    public void testMultipleCallsConsistency() {
        // Test that multiple rapid calls to getErrorCountLastMinute are consistent
        metricsService.recordServerError();

        // Multiple calls should return the same result
        long count1 = metricsService.getErrorCountLastMinute();
        long count2 = metricsService.getErrorCountLastMinute();
        long count3 = metricsService.getErrorCountLastMinute();

        assertThat(count1).isEqualTo(1);
        assertThat(count2).isEqualTo(1);
        assertThat(count3).isEqualTo(1);

        // Total count should be unchanged
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(1);
    }
}
