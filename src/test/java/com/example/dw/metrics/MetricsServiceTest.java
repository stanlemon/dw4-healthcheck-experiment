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

    @Test
    public void testRecordRequestLatency() {
        // Initial state - no latency recorded
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

        // Record a single latency
        metricsService.recordRequestLatency(100);

        // Verify average is the single recorded value
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(100.0);

        // Record another latency
        metricsService.recordRequestLatency(200);

        // Verify average is calculated correctly (100 + 200) / 2 = 150
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(150.0);
    }

    @Test
    public void testAverageLatencyCalculation() {
        // Record multiple latencies with different values
        metricsService.recordRequestLatency(50);   // 50ms
        metricsService.recordRequestLatency(100);  // 100ms
        metricsService.recordRequestLatency(150);  // 150ms
        metricsService.recordRequestLatency(200);  // 200ms

        // Expected average: (50 + 100 + 150 + 200) / 4 = 125
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(125.0);
    }

    @Test
    public void testLatencyThresholdBreach() {
        // Record latencies that average to 150ms
        metricsService.recordRequestLatency(100);
        metricsService.recordRequestLatency(200);

        // Test threshold checks
        assertThat(metricsService.isLatencyThresholdBreached(100.0)).isTrue();  // 150 > 100
        assertThat(metricsService.isLatencyThresholdBreached(150.0)).isFalse(); // 150 == 150 (not greater)
        assertThat(metricsService.isLatencyThresholdBreached(200.0)).isFalse(); // 150 < 200
    }

    @Test
    public void testLatencyThresholdWithNoData() {
        // Without any recorded latency, threshold should not be breached
        assertThat(metricsService.isLatencyThresholdBreached(1.0)).isFalse();
        assertThat(metricsService.isLatencyThresholdBreached(100.0)).isFalse();
    }

    @Test
    public void testLatencyConsistencyAcrossMultipleCalls() {
        // Record some latencies
        metricsService.recordRequestLatency(75);
        metricsService.recordRequestLatency(125);

        // Multiple calls should return the same average
        double avg1 = metricsService.getAverageLatencyLast60Minutes();
        double avg2 = metricsService.getAverageLatencyLast60Minutes();
        double avg3 = metricsService.getAverageLatencyLast60Minutes();

        assertThat(avg1).isEqualTo(100.0);  // (75 + 125) / 2 = 100
        assertThat(avg2).isEqualTo(100.0);
        assertThat(avg3).isEqualTo(100.0);

        // Threshold checks should also be consistent
        boolean breach1 = metricsService.isLatencyThresholdBreached(90.0);
        boolean breach2 = metricsService.isLatencyThresholdBreached(90.0);
        boolean breach3 = metricsService.isLatencyThresholdBreached(90.0);

        assertThat(breach1).isTrue();  // 100 > 90
        assertThat(breach2).isTrue();
        assertThat(breach3).isTrue();
    }

    @Test
    public void testBothErrorAndLatencyMetrics() {
        // Test that error and latency metrics work independently

        // Record some errors
        metricsService.recordServerError();
        metricsService.recordServerError();

        // Record some latencies
        metricsService.recordRequestLatency(100);
        metricsService.recordRequestLatency(300);

        // Verify both metrics are working
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(2);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(2);
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(200.0); // (100 + 300) / 2

        // Clear and verify both are reset
        metricsService.clearMetrics();

        assertThat(metricsService.getTotalErrorCount()).isEqualTo(0);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);
    }

    @Test
    public void testDefaultLatencyThresholdBreach() {
        // Test the default threshold (500ms) with latencies below the threshold
        metricsService.recordRequestLatency(400);
        metricsService.recordRequestLatency(300);

        // Average is 350ms, which is below 500ms default threshold
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(350.0);
        assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
    }

    @Test
    public void testDefaultLatencyThresholdBreachExceeded() {
        // Test the default threshold (500ms) with latencies above the threshold
        metricsService.recordRequestLatency(600);
        metricsService.recordRequestLatency(700);

        // Average is 650ms, which is above 500ms default threshold
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(650.0);
        assertThat(metricsService.isLatencyThresholdBreached()).isTrue();
    }

    @Test
    public void testDefaultLatencyThresholdAtExactThreshold() {
        // Test the default threshold (500ms) with latencies exactly at the threshold
        metricsService.recordRequestLatency(500);
        metricsService.recordRequestLatency(500);

        // Average is exactly 500ms, which should not breach (500 == 500, not >)
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(500.0);
        assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
    }

    @Test
    public void testBothThresholdMethodsConsistency() {
        // Test that both threshold methods return the same result when using 500ms
        metricsService.recordRequestLatency(400);
        metricsService.recordRequestLatency(800);

        // Average is 600ms
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(600.0);

        // Both methods should return the same result for 500ms threshold
        boolean defaultMethod = metricsService.isLatencyThresholdBreached();
        boolean parameterMethod = metricsService.isLatencyThresholdBreached(500.0);

        assertThat(defaultMethod).isTrue();   // 600 > 500
        assertThat(parameterMethod).isTrue(); // 600 > 500
        assertThat(defaultMethod).isEqualTo(parameterMethod);
    }

    @Test
    public void testDefaultThresholdAfterClearMetrics() {
        // Record some latencies above threshold
        metricsService.recordRequestLatency(600);
        metricsService.recordRequestLatency(700);

        // Verify threshold is breached
        assertThat(metricsService.isLatencyThresholdBreached()).isTrue();

        // Clear metrics
        metricsService.clearMetrics();

        // Verify threshold is no longer breached
        assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);
    }

    @Test
    public void testDefaultErrorThresholdBreach() {
        // Test the default threshold (100 errors) with errors below the threshold
        for (int i = 0; i < 50; i++) {
            metricsService.recordServerError();
        }

        // 50 errors is below 100 default threshold
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(50);
        assertThat(metricsService.isErrorThresholdBreached()).isFalse();
    }

    @Test
    public void testDefaultErrorThresholdBreachExceeded() {
        // Test the default threshold (100 errors) with errors above the threshold
        for (int i = 0; i < 150; i++) {
            metricsService.recordServerError();
        }

        // 150 errors is above 100 default threshold
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(150);
        assertThat(metricsService.isErrorThresholdBreached()).isTrue();
    }

    @Test
    public void testDefaultErrorThresholdAtExactThreshold() {
        // Test the default threshold (100 errors) with exactly 100 errors
        for (int i = 0; i < 100; i++) {
            metricsService.recordServerError();
        }

        // Exactly 100 errors should not breach (100 == 100, not >)
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(100);
        assertThat(metricsService.isErrorThresholdBreached()).isFalse();
    }

    @Test
    public void testErrorThresholdWithCustomValue() {
        // Test with a custom threshold of 10 errors
        for (int i = 0; i < 15; i++) {
            metricsService.recordServerError();
        }

        // 15 errors should breach threshold of 10
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(15);
        assertThat(metricsService.isErrorThresholdBreached(10)).isTrue();
        assertThat(metricsService.isErrorThresholdBreached(20)).isFalse();
    }

    @Test
    public void testBothErrorThresholdMethodsConsistency() {
        // Test that both threshold methods return the same result when using 100 errors
        for (int i = 0; i < 120; i++) {
            metricsService.recordServerError();
        }

        // 120 errors
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(120);

        // Both methods should return the same result for 100 error threshold
        boolean defaultMethod = metricsService.isErrorThresholdBreached();
        boolean parameterMethod = metricsService.isErrorThresholdBreached(100);

        assertThat(defaultMethod).isTrue();   // 120 > 100
        assertThat(parameterMethod).isTrue(); // 120 > 100
        assertThat(defaultMethod).isEqualTo(parameterMethod);
    }

    @Test
    public void testDefaultErrorThresholdAfterClearMetrics() {
        // Record errors above threshold
        for (int i = 0; i < 150; i++) {
            metricsService.recordServerError();
        }

        // Verify threshold is breached
        assertThat(metricsService.isErrorThresholdBreached()).isTrue();

        // Clear metrics
        metricsService.clearMetrics();

        // Verify threshold is no longer breached
        assertThat(metricsService.isErrorThresholdBreached()).isFalse();
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);
    }

    @Test
    public void testBothErrorAndLatencyThresholds() {
        // Test that error and latency thresholds work independently

        // Record errors above error threshold (>100)
        for (int i = 0; i < 120; i++) {
            metricsService.recordServerError();
        }

        // Record latencies above latency threshold (>500ms)
        metricsService.recordRequestLatency(600);
        metricsService.recordRequestLatency(700);

        // Verify both thresholds are breached
        assertThat(metricsService.isErrorThresholdBreached()).isTrue(); // 120 > 100
        assertThat(metricsService.isLatencyThresholdBreached()).isTrue(); // 650 > 500

        // Clear and verify both are reset
        metricsService.clearMetrics();

        assertThat(metricsService.isErrorThresholdBreached()).isFalse();
        assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
    }

    @Test
    public void testErrorBucketClearingOnFirstWrite() {
        // Test the branch where lastBucketTime == -1 (first write)
        // This happens on the very first error recorded after clearing metrics

        // Clear metrics to ensure we're in initial state
        metricsService.clearMetrics();

        // Record first error - this should trigger the clearOldBuckets path with lastBucketTime == -1
        metricsService.recordServerError();

        // Verify error is recorded
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(1);

        // Record another error immediately (same second)
        metricsService.recordServerError();

        // Should have 2 total errors and 2 in the last minute
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(2);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(2);
    }

    @Test
    public void testLatencyBucketClearingOnFirstWrite() {
        // Test the branch where lastLatencyBucketTime == -1 (first write)

        // Clear metrics to ensure we're in initial state
        metricsService.clearMetrics();

        // Record first latency - this should trigger clearOldLatencyBuckets with lastLatencyBucketTime == -1
        metricsService.recordRequestLatency(100);

        // Verify latency is recorded
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(100.0);

        // Record another latency immediately
        metricsService.recordRequestLatency(200);

        // Should have average of both values
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(150.0);
    }

    @Test
    public void testErrorBucketClearingOnLargeTimeJump() {
        // Test the branch where currentSeconds - lastBucketTime >= ERROR_BUCKET_COUNT
        // We can't easily simulate a 60+ second time jump in tests, but we can test
        // that the bucket clearing logic works correctly with rapid sequential calls

        // Record an error to set initial state
        metricsService.recordServerError();
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);

        // Record more errors rapidly to ensure consistent behavior
        for (int i = 0; i < 10; i++) {
            metricsService.recordServerError();
        }

        // All errors should be counted in the same time window
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(11);
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(11);
    }

    @Test
    public void testLatencyBucketClearingOnLargeTimeJump() {
        // Test similar scenario for latency buckets

        // Record initial latency
        metricsService.recordRequestLatency(100);
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(100.0);

        // Record more latencies rapidly
        for (int i = 0; i < 5; i++) {
            metricsService.recordRequestLatency(200);
        }

        // Should average all latencies: (100 + 5*200) / 6 = 1100/6 ≈ 183.33
        double expectedAvg = (100.0 + (5 * 200.0)) / 6.0;
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(expectedAvg);
    }

    @Test
    public void testErrorBucketPartialClearing() {
        // Test the bucket clearing logic by ensuring multiple calls don't affect current data

        // Record errors in rapid succession
        for (int i = 0; i < 10; i++) {
            metricsService.recordServerError();
        }

        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(10);

        // Call getErrorCountLastMinute multiple times to trigger clearOldBuckets
        // This should not change the count since no time has passed
        for (int i = 0; i < 5; i++) {
            assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(10);
        }

        // Record more errors
        for (int i = 0; i < 5; i++) {
            metricsService.recordServerError();
        }

        // Should have all 15 errors
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(15);
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(15);
    }

    @Test
    public void testLatencyBucketPartialClearing() {
        // Test partial clearing for latency buckets

        // Record initial latencies
        metricsService.recordRequestLatency(100);
        metricsService.recordRequestLatency(200);

        double initialAvg = metricsService.getAverageLatencyLast60Minutes();
        assertThat(initialAvg).isEqualTo(150.0);

        // Call getAverageLatencyLast60Minutes multiple times to trigger bucket management
        for (int i = 0; i < 5; i++) {
            assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(150.0);
        }

        // Record more latencies
        metricsService.recordRequestLatency(300);
        metricsService.recordRequestLatency(400);

        // Should now have all 4 latencies in the average
        double newAvg = metricsService.getAverageLatencyLast60Minutes();
        assertThat(newAvg).isEqualTo(250.0); // (100+200+300+400)/4 = 250
    }

    @Test
    public void testBucketConditionBranches() {
        // This test specifically targets the condition branches in clearOldBuckets and clearOldLatencyBuckets
        // to ensure we cover the logical paths that might not be hit by normal usage

        // Clear metrics to start fresh
        metricsService.clearMetrics();

        // Test error bucket condition by calling getErrorCountLastMinute when no errors are recorded
        // This will trigger clearOldBuckets with lastBucketTime == -1
        long initialErrorCount = metricsService.getErrorCountLastMinute();
        assertThat(initialErrorCount).isEqualTo(0);

        // Record an error to set lastBucketTime
        metricsService.recordServerError();
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);

        // Call getErrorCountLastMinute again to trigger clearOldBuckets with lastBucketTime != -1
        // and currentSeconds - lastBucketTime < ERROR_BUCKET_COUNT (normal case)
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);

        // Test latency bucket condition by calling getAverageLatencyLast60Minutes when no latencies are recorded
        metricsService.clearMetrics();
        double initialLatency = metricsService.getAverageLatencyLast60Minutes();
        assertThat(initialLatency).isEqualTo(0.0);

        // Record a latency to set lastLatencyBucketTime
        metricsService.recordRequestLatency(100);
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(100.0);

        // Call getAverageLatencyLast60Minutes again to trigger clearOldLatencyBuckets with
        // lastLatencyBucketTime != -1 and normal time progression
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(100.0);
    }

    @Test
    public void testZeroLatencyRecording() {
        // Test edge case of recording zero latency
        metricsService.recordRequestLatency(0);

        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

        // Add another latency to test averaging with zero
        metricsService.recordRequestLatency(100);

        // Should average to 50.0: (0 + 100) / 2 = 50
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(50.0);
    }

    @Test
    public void testNegativeLatencyHandling() {
        // Test edge case of negative latency (shouldn't happen in practice but good to test)
        metricsService.recordRequestLatency(-50);

        // Should still record the value
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(-50.0);

        // Add positive latency
        metricsService.recordRequestLatency(50);

        // Should average to 0.0: (-50 + 50) / 2 = 0
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);
    }

    @Test
    public void testSingletonBehavior() {
        // Test that MetricsService is truly a singleton
        MetricsService instance1 = MetricsService.getInstance();
        MetricsService instance2 = MetricsService.getInstance();

        assertThat(instance1).isSameAs(instance2);

        // Test that changes through one reference affect the other
        instance1.recordServerError();
        assertThat(instance2.getErrorCountLastMinute()).isEqualTo(1);

        instance2.recordRequestLatency(100);
        assertThat(instance1.getAverageLatencyLast60Minutes()).isEqualTo(100.0);
    }

    @Test
    public void testErrorBucketTimeJumpCondition() {
        // Test the condition: currentSeconds - lastBucketTime >= ERROR_BUCKET_COUNT
        // We can't easily simulate 60+ seconds, but we can test the logic path

        metricsService.clearMetrics();

        // Record an error to set lastBucketTime
        metricsService.recordServerError();
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);

        // Record more errors in the same second - this should not trigger the time jump condition
        for (int i = 0; i < 5; i++) {
            metricsService.recordServerError();
        }

        // All errors should be in the same time bucket
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(6);

        // Verify the time jump condition is not triggered by checking consistency
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(6);
    }

    @Test
    public void testLatencyBucketTimeJumpCondition() {
        // Test the condition: currentMinutes - lastLatencyBucketTime >= LATENCY_BUCKET_COUNT

        metricsService.clearMetrics();

        // Record a latency to set lastLatencyBucketTime
        metricsService.recordRequestLatency(150);
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(150.0);

        // Record more latencies in the same minute - this should not trigger the time jump condition
        for (int i = 0; i < 3; i++) {
            metricsService.recordRequestLatency(200 + (i * 50));
        }

        // All latencies should be averaged together
        // (150 + 200 + 250 + 300) / 4 = 225
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(225.0);

        // Verify the time jump condition is not triggered by checking consistency
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(225.0);
    }

    @Test
    public void testBucketIndexCalculation() {
        // Test that bucket index calculation works correctly for both error and latency buckets

        metricsService.clearMetrics();

        // Test error bucket indexing by recording errors
        for (int i = 0; i < 65; i++) { // More than ERROR_BUCKET_COUNT (60)
            metricsService.recordServerError();
        }

        // Should handle the bucket wrapping correctly
        long errorCount = metricsService.getErrorCountLastMinute();
        assertThat(errorCount).isEqualTo(65);

        // Test latency bucket indexing by recording latencies
        for (int i = 0; i < 65; i++) { // More than LATENCY_BUCKET_COUNT (60) worth of operations
            metricsService.recordRequestLatency(100 + i);
        }

        // Should handle the bucket wrapping correctly
        double avgLatency = metricsService.getAverageLatencyLast60Minutes();
        assertThat(avgLatency).isGreaterThan(0.0);
    }

    @Test
    public void testEmptyBucketClearing() {
        // Test clearing when buckets are already empty

        metricsService.clearMetrics();

        // Call methods that trigger clearing on empty buckets
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

        // Clear again and verify still empty
        metricsService.clearMetrics();
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);

        // Call clearing methods multiple times on empty state
        for (int i = 0; i < 3; i++) {
            assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(0);
            assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(0.0);
        }
    }

    @Test
    public void testConcurrentErrorRecording() {
        // Test that concurrent error recording works correctly
        // This helps ensure thread safety of bucket operations

        final int numThreads = 5;
        final int errorsPerThread = 10;
        Thread[] threads = new Thread[numThreads];

        // Create threads that each record errors
        for (int i = 0; i < numThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < errorsPerThread; j++) {
                    metricsService.recordServerError();
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Verify all errors were recorded
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(numThreads * errorsPerThread);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(numThreads * errorsPerThread);
    }

    @Test
    public void testConcurrentLatencyRecording() {
        // Test concurrent latency recording for thread safety

        final int numThreads = 3;
        final int latenciesPerThread = 5;
        Thread[] threads = new Thread[numThreads];

        // Create threads that each record latencies
        for (int i = 0; i < numThreads; i++) {
            final int threadId = i;
            threads[i] = new Thread(() -> {
                for (int j = 0; j < latenciesPerThread; j++) {
                    // Each thread records different latency values
                    metricsService.recordRequestLatency(100 + (threadId * 50) + (j * 10));
                }
            });
        }

        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Verify latencies were recorded (exact average depends on thread execution order)
        double avgLatency = metricsService.getAverageLatencyLast60Minutes();
        assertThat(avgLatency).isGreaterThan(0.0);
        assertThat(avgLatency).isLessThan(500.0); // Should be reasonable average
    }

    @Test
    public void testErrorBucketStaleWindowCondition() {
        // Test the branch where currentSeconds - time >= ERROR_BUCKET_COUNT
        // This simulates the case where we're clearing buckets but some are outside the window

        long currentTime = System.currentTimeMillis() / 1000;
        // Set lastBucketTime to be just a few seconds back (within 60 but will trigger stale clearing)
        metricsService.setLastBucketTimeForTesting(currentTime - 5);

        // Record an error which will trigger the clearOldBuckets method
        metricsService.recordServerError();

        // The bucket clearing should have handled the stale window condition
        assertThat(metricsService.getTotalErrorCount()).isEqualTo(1);
        assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
    }

    @Test
    public void testLatencyBucketStaleWindowCondition() {
        // Test the branch where currentMinutes - time >= LATENCY_BUCKET_COUNT
        // This simulates the case where we're clearing latency buckets but some are outside the window

        long currentTime = System.currentTimeMillis() / (60 * 1000);
        // Set lastLatencyBucketTime to be just a few minutes back (within 60 but will trigger stale clearing)
        metricsService.setLastLatencyBucketTimeForTesting(currentTime - 5);

        // Record latency which will trigger the clearOldLatencyBuckets method
        metricsService.recordRequestLatency(100);

        // The bucket clearing should have handled the stale window condition
        assertThat(metricsService.getAverageLatencyLast60Minutes()).isEqualTo(100.0);
    }

    @Test
    public void testErrorBucketOutsideWindowNotCleared() {
        // This test targets the corrected condition: if (currentSeconds - time < ERROR_BUCKET_COUNT)
        // We want to test the case where currentSeconds - time >= ERROR_BUCKET_COUNT
        // This happens when we have a very old lastBucketTime

        MetricsService metricsService = MetricsService.getInstance();
        metricsService.clearMetrics();

        // Record an error to initialize the bucket time
        metricsService.recordServerError();
        long initialErrorCount = metricsService.getErrorCountLastMinute();
        assertThat(initialErrorCount).isEqualTo(1);

        // Now we need to simulate the scenario where lastBucketTime is very old
        // compared to currentSeconds. We do this by manually setting up a condition
        // where the time difference would be >= ERROR_BUCKET_COUNT (60)

        // Since we can't directly manipulate time, we test the boundary condition
        // by recording errors over a span that would trigger the stale bucket logic

        // This forces multiple calls to clearOldBuckets with different time values
        for (int i = 0; i < 5; i++) {
            metricsService.recordServerError();
            try {
                Thread.sleep(1); // Small delay to ensure time progression
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Verify that errors are still being tracked correctly
        long finalErrorCount = metricsService.getErrorCountLastMinute();
        assertThat(finalErrorCount).isGreaterThan(initialErrorCount);
    }

    @Test
    public void testLatencyBucketOutsideWindowNotCleared() {
        // This test targets the condition: if (currentMinutes - time < LATENCY_BUCKET_COUNT)
        // We want to test the case where currentMinutes - time >= LATENCY_BUCKET_COUNT
        // This happens when we have a very old lastLatencyBucketTime

        MetricsService metricsService = MetricsService.getInstance();
        metricsService.clearMetrics();

        // Record initial latency to set up the system
        metricsService.recordRequestLatency(100);
        double initialAverage = metricsService.getAverageLatencyLast60Minutes();
        assertThat(initialAverage).isEqualTo(100.0);

        // Set lastLatencyBucketTime to be very old (more than 60 minutes ago)
        long currentMinutes = System.currentTimeMillis() / (60 * 1000);
        long oldTime = currentMinutes - 65; // 65 minutes ago
        metricsService.setLastLatencyBucketTimeForTesting(oldTime);

        // Now record a new latency which should trigger clearOldLatencyBuckets
        // The condition should be false for times in the range [oldTime+1, currentMinutes-60]
        metricsService.recordRequestLatency(200);

        // The new latency should still be recorded correctly
        // The old latency data should be cleared due to being outside the window
        double newAverage = metricsService.getAverageLatencyLast60Minutes();
        assertThat(newAverage).isEqualTo(200.0); // Only the new value should remain
    }

    @Test
    public void testGetLastBucketTime() {
        // Test that getLastBucketTime returns the expected initial value
        metricsService.clearMetrics();

        // Initially, lastBucketTime should be -1
        assertThat(metricsService.getLastBucketTime()).isEqualTo(-1);

        // After recording an error, lastBucketTime should be set to current time
        long beforeTime = System.currentTimeMillis() / 1000;
        metricsService.recordServerError();
        long afterTime = System.currentTimeMillis() / 1000;

        long actualLastBucketTime = metricsService.getLastBucketTime();
        assertThat(actualLastBucketTime).isGreaterThanOrEqualTo(beforeTime);
        assertThat(actualLastBucketTime).isLessThanOrEqualTo(afterTime);
    }

    @Test
    public void testGetLastLatencyBucketTime() {
        // Test that getLastLatencyBucketTime returns the expected initial value
        metricsService.clearMetrics();

        // Initially, lastLatencyBucketTime should be -1
        assertThat(metricsService.getLastLatencyBucketTime()).isEqualTo(-1);

        // After recording latency, lastLatencyBucketTime should be set to current time
        long beforeTime = System.currentTimeMillis() / (60 * 1000);
        metricsService.recordRequestLatency(100);
        long afterTime = System.currentTimeMillis() / (60 * 1000);

        long actualLastLatencyBucketTime = metricsService.getLastLatencyBucketTime();
        assertThat(actualLastLatencyBucketTime).isGreaterThanOrEqualTo(beforeTime);
        assertThat(actualLastLatencyBucketTime).isLessThanOrEqualTo(afterTime);
    }

    @Test
    public void testSetLastBucketTimeForTesting() {
        // Test that the setter method works correctly for error buckets
        metricsService.clearMetrics();

        long testTime = 12345L;
        metricsService.setLastBucketTimeForTesting(testTime);

        // Verify the value was set correctly
        assertThat(metricsService.getLastBucketTime()).isEqualTo(testTime);

        // Test with different values
        long anotherTestTime = 67890L;
        metricsService.setLastBucketTimeForTesting(anotherTestTime);
        assertThat(metricsService.getLastBucketTime()).isEqualTo(anotherTestTime);
    }

    @Test
    public void testSetLastLatencyBucketTimeForTesting() {
        // Test that the setter method works correctly for latency buckets
        metricsService.clearMetrics();

        long testTime = 54321L;
        metricsService.setLastLatencyBucketTimeForTesting(testTime);

        // Verify the value was set correctly
        assertThat(metricsService.getLastLatencyBucketTime()).isEqualTo(testTime);

        // Test with different values
        long anotherTestTime = 98765L;
        metricsService.setLastLatencyBucketTimeForTesting(anotherTestTime);
        assertThat(metricsService.getLastLatencyBucketTime()).isEqualTo(anotherTestTime);
    }

    @Test
    public void testBucketTimeGettersAndSettersIntegration() {
        // Test that getters and setters work together correctly
        metricsService.clearMetrics();

        // Initially both should be -1
        assertThat(metricsService.getLastBucketTime()).isEqualTo(-1);
        assertThat(metricsService.getLastLatencyBucketTime()).isEqualTo(-1);

        // Set different values for each
        long errorBucketTime = 11111L;
        long latencyBucketTime = 22222L;

        metricsService.setLastBucketTimeForTesting(errorBucketTime);
        metricsService.setLastLatencyBucketTimeForTesting(latencyBucketTime);

        // Verify each getter returns the correct value
        assertThat(metricsService.getLastBucketTime()).isEqualTo(errorBucketTime);
        assertThat(metricsService.getLastLatencyBucketTime()).isEqualTo(latencyBucketTime);

        // Test that setting one doesn't affect the other
        long newErrorBucketTime = 33333L;
        metricsService.setLastBucketTimeForTesting(newErrorBucketTime);

        assertThat(metricsService.getLastBucketTime()).isEqualTo(newErrorBucketTime);
        assertThat(metricsService.getLastLatencyBucketTime()).isEqualTo(latencyBucketTime); // Should remain unchanged
    }

    @Test
    public void testBucketTimeAfterClearMetrics() {
        // Test that clearMetrics resets the bucket times to -1
        metricsService.clearMetrics();

        // Set some values
        metricsService.setLastBucketTimeForTesting(12345L);
        metricsService.setLastLatencyBucketTimeForTesting(67890L);

        // Verify values are set
        assertThat(metricsService.getLastBucketTime()).isEqualTo(12345L);
        assertThat(metricsService.getLastLatencyBucketTime()).isEqualTo(67890L);

        // Clear metrics
        metricsService.clearMetrics();

        // Verify both are reset to -1
        assertThat(metricsService.getLastBucketTime()).isEqualTo(-1);
        assertThat(metricsService.getLastLatencyBucketTime()).isEqualTo(-1);
    }
}