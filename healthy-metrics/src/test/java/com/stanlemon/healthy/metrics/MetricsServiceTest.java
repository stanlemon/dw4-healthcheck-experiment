package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Metrics Service Tests")
class MetricsServiceTest {

  private DefaultMetricsService metricsService;
  private long errorThreshold;
  private double latencyThreshold;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    errorThreshold = metricsService.getDefaultErrorThreshold();
    latencyThreshold = metricsService.getDefaultLatencyThresholdMs();
  }

  @Nested
  @DisplayName("Error recording")
  class ErrorRecording {

    @Test
    void recordServerError_WhenCalled_ShouldIncrementErrorCounts() {
      assertThat(metricsService.getTotalErrorCount()).isZero();
      assertThat(metricsService.getErrorCountLastMinute()).isZero();

      metricsService.recordServerError();

      assertThat(metricsService.getTotalErrorCount()).isEqualTo(1);
      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);

      metricsService.recordServerError();

      assertThat(metricsService.getTotalErrorCount()).isEqualTo(2);
      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(2);
    }

    @Test
    void getErrorCountLastMinute_WhenErrorsRecorded_ShouldReturnCorrectCount() {
      assertThat(metricsService.getErrorCountLastMinute()).isZero();

      metricsService.recordServerError();
      metricsService.recordServerError();
      metricsService.recordServerError();

      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(3);
      assertThat(metricsService.getTotalErrorCount()).isEqualTo(3);

      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(3);
      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(3);
      assertThat(metricsService.getTotalErrorCount()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("Latency recording")
  class LatencyRecording {

    @Test
    void recordRequestLatency_WhenCalled_ShouldUpdateAverageLatency() {
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

      metricsService.recordRequestLatency(100);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(100.0);

      metricsService.recordRequestLatency(200);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(150.0);
    }

    @Test
    void
        getAverageLatencyLast60Seconds_WhenMultipleLatenciesRecorded_ShouldCalculateCorrectAverage() {
      metricsService.recordRequestLatency(50);
      metricsService.recordRequestLatency(100);
      metricsService.recordRequestLatency(150);
      metricsService.recordRequestLatency(200);

      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(125.0);
    }

    @Test
    void recordRequestLatency_WhenZeroLatency_ShouldCalculateCorrectAverage() {
      metricsService.recordRequestLatency(0);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

      metricsService.recordRequestLatency(100);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(50.0);
    }

    @Test
    void recordRequestLatency_WhenNegativeLatency_ShouldClampToZero() {
      metricsService.recordRequestLatency(-50);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

      metricsService.recordRequestLatency(100);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(50.0);
    }
  }

  @Nested
  @DisplayName("Latency threshold evaluation")
  class LatencyThreshold {

    @Test
    void isLatencyThresholdBreached_WhenAverageAboveThreshold_ShouldReturnTrue() {
      long latency1 = (long) latencyThreshold;
      long latency2 = (long) (latencyThreshold * 2.0);

      metricsService.recordRequestLatency(latency1);
      metricsService.recordRequestLatency(latency2);
      metricsService.recordRequestLatency(latency1);
      metricsService.recordRequestLatency(latency2);
      metricsService.recordRequestLatency(latency1);

      double expectedAvg = (3 * latency1 + 2 * latency2) / 5.0;

      assertThat(metricsService.isLatencyThresholdBreached(latencyThreshold)).isTrue();
      assertThat(metricsService.isLatencyThresholdBreached(expectedAvg)).isFalse();
      assertThat(metricsService.isLatencyThresholdBreached(latencyThreshold * 2.0)).isFalse();
    }

    @Test
    void isLatencyThresholdBreached_WhenNoDataRecorded_ShouldReturnFalse() {
      assertThat(metricsService.isLatencyThresholdBreached(1.0)).isFalse();
      assertThat(metricsService.isLatencyThresholdBreached(100.0)).isFalse();
    }

    @Test
    void isLatencyThresholdBreached_WhenInsufficientSamples_ShouldReturnFalse() {
      metricsService.recordRequestLatency(10000);
      metricsService.recordRequestLatency(10000);
      metricsService.recordRequestLatency(10000);
      metricsService.recordRequestLatency(10000);

      assertThat(metricsService.isLatencyThresholdBreached(100.0)).isFalse();
      assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
    }

    @Test
    void isLatencyThresholdBreached_WhenAverageBelowDefaultThreshold_ShouldReturnFalse() {
      long latencyBelow = (long) (latencyThreshold * 0.8);
      long latencyBelow2 = (long) (latencyThreshold * 0.6);

      metricsService.recordRequestLatency(latencyBelow);
      metricsService.recordRequestLatency(latencyBelow2);
      metricsService.recordRequestLatency(latencyBelow);
      metricsService.recordRequestLatency(latencyBelow2);
      metricsService.recordRequestLatency(latencyBelow);

      double expectedAvg = (3 * latencyBelow + 2 * latencyBelow2) / 5.0;
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(expectedAvg);
      assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
    }

    @Test
    void isLatencyThresholdBreached_WhenAverageAboveDefaultThreshold_ShouldReturnTrue() {
      long latencyAbove = (long) (latencyThreshold * 1.5);
      long latencyAbove2 = (long) (latencyThreshold * 2.0);

      metricsService.recordRequestLatency(latencyAbove);
      metricsService.recordRequestLatency(latencyAbove2);
      metricsService.recordRequestLatency(latencyAbove);
      metricsService.recordRequestLatency(latencyAbove2);
      metricsService.recordRequestLatency(latencyAbove);

      double expectedAvg = (3 * latencyAbove + 2 * latencyAbove2) / 5.0;
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(expectedAvg);
      assertThat(metricsService.isLatencyThresholdBreached()).isTrue();
    }

    @Test
    void isLatencyThresholdBreached_WhenAverageExactlyAtThreshold_ShouldReturnFalse() {
      long exactLatency = (long) latencyThreshold;

      metricsService.recordRequestLatency(exactLatency);
      metricsService.recordRequestLatency(exactLatency);
      metricsService.recordRequestLatency(exactLatency);
      metricsService.recordRequestLatency(exactLatency);
      metricsService.recordRequestLatency(exactLatency);

      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(latencyThreshold);
      assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
    }

    @Test
    void isLatencyThresholdBreached_WhenMetricsCleared_ShouldReturnFalse() {
      metricsService.recordRequestLatency(600);
      metricsService.recordRequestLatency(700);
      metricsService.recordRequestLatency(600);
      metricsService.recordRequestLatency(700);
      metricsService.recordRequestLatency(700);

      assertThat(metricsService.isLatencyThresholdBreached()).isTrue();

      metricsService.clearMetrics();

      assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("Error threshold evaluation")
  class ErrorThreshold {

    private void assertErrorThreshold(int errors, int requests, boolean expectedBreach) {
      for (int i = 0; i < errors; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < requests; i++) {
        metricsService.recordRequestLatency(100);
      }
      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(errors);
      assertThat(metricsService.isErrorThresholdBreached()).isEqualTo(expectedBreach);
    }

    @Test
    void isErrorThresholdBreached_WhenModerateTrafficHighErrorRate_ShouldReturnTrue() {
      assertErrorThreshold(11, 12, true);
    }

    @Test
    void isErrorThresholdBreached_WhenInsufficientSamples_ShouldReturnFalse() {
      assertErrorThreshold(8, 0, false);
    }

    @Test
    void isErrorThresholdBreached_WhenHighTrafficAndHighErrorRate_ShouldReturnTrue() {
      assertErrorThreshold(15, 100, true);
    }

    @Test
    void isErrorThresholdBreached_WhenHighTrafficAndLowErrorRate_ShouldReturnFalse() {
      assertErrorThreshold(5, 100, false);
    }

    @Test
    void isErrorThresholdBreached_WhenCustomThresholdSpecified_ShouldRespectThresholdValue() {
      for (int i = 0; i < 6; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 14; i++) {
        metricsService.recordRequestLatency(100);
      }

      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(6);
      assertThat(metricsService.isErrorThresholdBreached(10)).isFalse();
      assertThat(metricsService.isErrorThresholdBreached(4)).isTrue();
    }

    @Test
    void isErrorThresholdBreached_WhenMetricsCleared_ShouldReturnFalse() {
      for (int i = 0; i < 15; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 100; i++) {
        metricsService.recordRequestLatency(100);
      }

      assertThat(metricsService.isErrorThresholdBreached()).isTrue();

      metricsService.clearMetrics();

      assertThat(metricsService.isErrorThresholdBreached()).isFalse();
      assertThat(metricsService.getErrorCountLastMinute()).isZero();
    }
  }

  @Nested
  @DisplayName("Clear metrics")
  class ClearMetrics {

    @Test
    void clearMetrics_WhenCalled_ShouldResetAllCounts() {
      metricsService.recordServerError();
      metricsService.recordServerError();
      metricsService.recordRequestLatency(100);
      metricsService.recordRequestLatency(200);

      assertThat(metricsService.getTotalErrorCount()).isEqualTo(2);
      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(2);
      assertThat(metricsService.getTotalRequestCountLast60Seconds()).isEqualTo(2);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(150.0);

      metricsService.clearMetrics();

      assertThat(metricsService.getTotalErrorCount()).isZero();
      assertThat(metricsService.getErrorCountLastMinute()).isZero();
      assertThat(metricsService.getTotalRequestCountLast60Seconds()).isZero();
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
    }
  }

  @Nested
  @DisplayName("Combined error and latency metrics")
  class CombinedMetrics {

    @Test
    void recordMetrics_WhenBothErrorAndLatency_ShouldTrackIndependently() {
      metricsService.recordServerError();
      metricsService.recordServerError();
      metricsService.recordRequestLatency(100);
      metricsService.recordRequestLatency(300);

      assertThat(metricsService.getTotalErrorCount()).isEqualTo(2);
      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(2);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(200.0);

      metricsService.clearMetrics();

      assertThat(metricsService.getTotalErrorCount()).isZero();
      assertThat(metricsService.getErrorCountLastMinute()).isZero();
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
    }

    @Test
    void isThresholdBreached_WhenBothErrorAndLatencyMetrics_ShouldEvaluateIndependently() {
      for (int i = 0; i < 15; i++) {
        metricsService.recordServerError();
      }
      metricsService.recordRequestLatency(150);
      metricsService.recordRequestLatency(180);
      metricsService.recordRequestLatency(160);
      metricsService.recordRequestLatency(170);
      metricsService.recordRequestLatency(140);
      for (int i = 0; i < 95; i++) {
        metricsService.recordRequestLatency(50);
      }

      assertThat(metricsService.isErrorThresholdBreached()).isTrue();
      assertThat(metricsService.isLatencyThresholdBreached()).isFalse();

      metricsService.clearMetrics();

      assertThat(metricsService.isErrorThresholdBreached()).isFalse();
      assertThat(metricsService.isLatencyThresholdBreached()).isFalse();
    }
  }

  @Nested
  @DisplayName("Instance isolation")
  class InstanceIsolation {

    @Test
    void defaultMetricsService_WhenInstantiatedSeparately_ShouldMaintainIsolatedState() {
      DefaultMetricsService instance1 = new DefaultMetricsService();
      DefaultMetricsService instance2 = new DefaultMetricsService();

      assertThat(instance1).isNotSameAs(instance2);

      instance1.recordServerError();
      assertThat(instance1.getErrorCountLastMinute()).isEqualTo(1);
      assertThat(instance2.getErrorCountLastMinute()).isZero();

      instance2.recordRequestLatency(100);
      assertThat(instance1.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
      assertThat(instance2.getAverageLatencyLast60Seconds()).isEqualTo(100.0);
    }
  }

  @Nested
  @DisplayName("Thread safety")
  class ThreadSafety {

    @Test
    void recordServerError_WhenCalledConcurrently_ShouldMaintainThreadSafety()
        throws InterruptedException {
      final int numThreads = 5;
      final int errorsPerThread = 10;
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch endLatch = new CountDownLatch(numThreads);
      Thread[] threads = new Thread[numThreads];

      for (int i = 0; i < numThreads; i++) {
        threads[i] =
            new Thread(
                () -> {
                  try {
                    startLatch.await();
                    for (int j = 0; j < errorsPerThread; j++) {
                      metricsService.recordServerError();
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  } finally {
                    endLatch.countDown();
                  }
                });
        threads[i].start();
      }

      startLatch.countDown();
      assertThat(endLatch.await(10, TimeUnit.SECONDS)).isTrue();

      assertThat(metricsService.getTotalErrorCount()).isEqualTo(numThreads * errorsPerThread);
      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(numThreads * errorsPerThread);
    }

    @Test
    void recordRequestLatency_WhenCalledConcurrently_ShouldMaintainThreadSafety()
        throws InterruptedException {
      final int numThreads = 3;
      final int latenciesPerThread = 5;
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch endLatch = new CountDownLatch(numThreads);
      Thread[] threads = new Thread[numThreads];

      for (int i = 0; i < numThreads; i++) {
        final int threadId = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    startLatch.await();
                    for (int j = 0; j < latenciesPerThread; j++) {
                      metricsService.recordRequestLatency(100 + (threadId * 50) + (j * 10));
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  } finally {
                    endLatch.countDown();
                  }
                });
        threads[i].start();
      }

      startLatch.countDown();
      assertThat(endLatch.await(10, TimeUnit.SECONDS)).isTrue();

      // Thread 0: 100,110,120,130,140; Thread 1: 150,160,170,180,190; Thread 2: 200,210,220,230,240
      // Sum = 2550, count = 15, average = 170.0
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(170.0);
    }

    @Test
    void metrics_WhenUnderHighConcurrentLoad_ShouldMaintainConsistency()
        throws InterruptedException {
      final int threadCount = 100;
      final int operationsPerThread = 1000;
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch endLatch = new CountDownLatch(threadCount);
      final AtomicInteger errorCounter = new AtomicInteger(0);
      final AtomicInteger latencyCounter = new AtomicInteger(0);

      Thread[] threads = new Thread[threadCount];
      for (int i = 0; i < threadCount; i++) {
        final int threadId = i;
        threads[i] =
            new Thread(
                () -> {
                  try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                      if (j % 10 == 0) {
                        metricsService.recordServerError();
                        errorCounter.incrementAndGet();
                      } else {
                        long latencyMs = 50 + ((threadId * 5 + j) % 100);
                        metricsService.recordRequestLatency(latencyMs);
                        latencyCounter.incrementAndGet();
                      }
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  } finally {
                    endLatch.countDown();
                  }
                });
        threads[i].start();
      }

      startLatch.countDown();

      boolean allThreadsCompleted = endLatch.await(30, TimeUnit.SECONDS);
      assertThat(allThreadsCompleted).as("All threads completed within timeout").isTrue();

      int expectedErrors = errorCounter.get();
      int expectedLatencies = latencyCounter.get();

      assertThat(metricsService.getTotalErrorCount()).isEqualTo(expectedErrors);
      assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(expectedErrors);
      assertThat(metricsService.getTotalRequestCountLast60Seconds()).isEqualTo(expectedLatencies);

      double avgLatency = metricsService.getAverageLatencyLast60Seconds();
      assertThat(avgLatency).isBetween(50.0, 150.0);
    }
  }

  @Nested
  @DisplayName("Sliding window bucket management")
  class SlidingWindowBuckets {

    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private Instant currentTime = BASE_TIME;

    private final Clock testClock =
        new Clock() {
          @Override
          public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(java.time.ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            return currentTime;
          }
        };

    private DefaultMetricsService svc;

    @BeforeEach
    void setUpClock() {
      currentTime = BASE_TIME;
      svc = new DefaultMetricsService(testClock);
    }

    private void advanceBy(Duration duration) {
      currentTime = currentTime.plus(duration);
    }

    @Test
    void errors_WhenWindowExpires_ShouldNotCountOldErrors() {
      svc.recordServerError();
      assertThat(svc.getErrorCountLastMinute()).isEqualTo(1);
      assertThat(svc.getTotalErrorCount()).isEqualTo(1);

      advanceBy(Duration.ofSeconds(65));

      assertThat(svc.getErrorCountLastMinute()).isZero();
      assertThat(svc.getTotalErrorCount()).isEqualTo(1);

      svc.recordServerError();
      assertThat(svc.getErrorCountLastMinute()).isEqualTo(1);
      assertThat(svc.getTotalErrorCount()).isEqualTo(2);
    }

    @Test
    void latency_WhenWindowExpires_ShouldNotCountOldLatency() {
      svc.recordRequestLatency(100);
      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(100.0);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isEqualTo(1);

      advanceBy(Duration.ofSeconds(65));

      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isZero();

      svc.recordRequestLatency(200);
      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(200.0);
    }

    @Test
    void errors_WhenReadAfterWindowExpires_ShouldClearStaleOnRead() {
      svc.recordServerError();
      assertThat(svc.getErrorCountLastMinute()).isEqualTo(1);

      advanceBy(Duration.ofSeconds(65));

      assertThat(svc.getErrorCountLastMinute()).isZero();
    }

    @Test
    void latency_WhenReadAfterWindowExpires_ShouldClearStaleOnRead() {
      svc.recordRequestLatency(100);
      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(100.0);

      advanceBy(Duration.ofSeconds(65));

      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isZero();
    }

    @Test
    void errors_WhenWithinWindow_ShouldStillCount() {
      svc.recordServerError();
      advanceBy(Duration.ofSeconds(30));
      svc.recordServerError();

      assertThat(svc.getErrorCountLastMinute()).isEqualTo(2);
      assertThat(svc.getTotalErrorCount()).isEqualTo(2);
    }

    @Test
    void latency_WhenWithinWindow_ShouldStillCount() {
      svc.recordRequestLatency(100);
      advanceBy(Duration.ofSeconds(30));
      svc.recordRequestLatency(200);

      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(150.0);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isEqualTo(2);
    }

    @Test
    void errors_WhenPartialWindowExpires_ShouldOnlyCountRecent() {
      svc.recordServerError();
      advanceBy(Duration.ofSeconds(50));
      svc.recordServerError();
      advanceBy(Duration.ofSeconds(15));

      // First error was 65 seconds ago — outside 60s window. Second was 15 seconds ago.
      assertThat(svc.getErrorCountLastMinute()).isEqualTo(1);
      assertThat(svc.getTotalErrorCount()).isEqualTo(2);
    }

    @Test
    void latency_WhenPartialWindowExpires_ShouldOnlyCountRecent() {
      svc.recordRequestLatency(100);
      advanceBy(Duration.ofSeconds(50));
      svc.recordRequestLatency(200);
      advanceBy(Duration.ofSeconds(15));

      // First latency was 65 seconds ago — outside window. Only second remains.
      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(200.0);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    void recordRequestLatency_WhenExtremeValues_ShouldHandleCorrectly() {
      metricsService.recordRequestLatency(0);
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(0.0);

      metricsService.recordRequestLatency(Integer.MAX_VALUE);
      double expectedAverage = (0.0 + Integer.MAX_VALUE) / 2.0;
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(expectedAverage);

      // Negative values are clamped to 0
      metricsService.recordRequestLatency(Integer.MIN_VALUE);
      expectedAverage = (0.0 + Integer.MAX_VALUE + 0.0) / 3.0;
      assertThat(metricsService.getAverageLatencyLast60Seconds()).isEqualTo(expectedAverage);
    }
  }
}
