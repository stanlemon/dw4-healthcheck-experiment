package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
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
    void isErrorThresholdBreached_WhenExactly10PercentErrorRate_ShouldNotBreach() {
      assertErrorThreshold(10, 100, false);
    }

    @Test
    void isErrorThresholdBreached_WhenCustomThresholdWithHighTraffic_ShouldUseRateNotThreshold() {
      // 5% error rate — below hardcoded 10%, so not breached regardless of low threshold
      DefaultMetricsService belowRate = new DefaultMetricsService();
      for (int i = 0; i < 5; i++) {
        belowRate.recordServerError();
      }
      for (int i = 0; i < 100; i++) {
        belowRate.recordRequestLatency(100);
      }
      assertThat(belowRate.isErrorThresholdBreached(1)).isFalse();

      // 15% error rate — above 10%, breached regardless of high threshold
      DefaultMetricsService aboveRate = new DefaultMetricsService();
      for (int i = 0; i < 15; i++) {
        aboveRate.recordServerError();
      }
      for (int i = 0; i < 100; i++) {
        aboveRate.recordRequestLatency(100);
      }
      assertThat(aboveRate.isErrorThresholdBreached(Long.MAX_VALUE)).isTrue();
    }

    @Test
    void isErrorThresholdBreached_WhenErrorCountEqualsModerateTrafficThreshold_ShouldNotBreach() {
      // requestCount=20, threshold=100 → Math.min(100, 20/2) = 10
      // errorCount == 10, uses >, so NOT breached
      for (int i = 0; i < 10; i++) {
        metricsService.recordServerError();
      }
      for (int i = 0; i < 20; i++) {
        metricsService.recordRequestLatency(100);
      }
      assertThat(metricsService.isErrorThresholdBreached()).isFalse();
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
    }
  }

  @Nested
  @DisplayName("Instance isolation")
  class InstanceIsolation {

    @Test
    void defaultMetricsService_WhenInstantiatedSeparately_ShouldMaintainIsolatedState() {
      DefaultMetricsService instance1 = new DefaultMetricsService();
      DefaultMetricsService instance2 = new DefaultMetricsService();

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
    void errors_WhenExactlyAtWindowBoundary_ShouldClearOldBuckets() {
      svc.recordServerError();
      assertThat(svc.getErrorCountLastMinute()).isEqualTo(1);

      advanceBy(Duration.ofSeconds(60));

      assertThat(svc.getErrorCountLastMinute()).isZero();
    }

    @Test
    void latency_WhenExactlyAtWindowBoundary_ShouldClearOldBuckets() {
      svc.recordRequestLatency(100);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isEqualTo(1);

      advanceBy(Duration.ofSeconds(60));

      assertThat(svc.getTotalRequestCountLast60Seconds()).isZero();
      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(0.0);
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
    void errors_WhenClockMovesBackward_ShouldResetInsteadOfLingering() {
      svc.recordServerError();
      assertThat(svc.getErrorCountLastMinute()).isEqualTo(1);

      // NTP step-back / VM resume: clock jumps into the past.
      advanceBy(Duration.ofSeconds(-5));
      assertThat(svc.getErrorCountLastMinute()).isZero();

      // New writes continue to work against the realigned timestamp.
      svc.recordServerError();
      assertThat(svc.getErrorCountLastMinute()).isEqualTo(1);
    }

    @Test
    void latency_WhenClockMovesBackward_ShouldResetInsteadOfLingering() {
      svc.recordRequestLatency(100);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isEqualTo(1);

      advanceBy(Duration.ofSeconds(-5));
      assertThat(svc.getTotalRequestCountLast60Seconds()).isZero();

      svc.recordRequestLatency(200);
      assertThat(svc.getAverageLatencyLast60Seconds()).isEqualTo(200.0);
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

  /**
   * A read takes a timestamp, then works on the buckets. If a write in the next second can slip in
   * between those two steps, the reader's timestamp ends up older than the last write and looks
   * like the clock moved backwards, which wipes the whole window. These tests force that
   * interleaving: the reader's first clock call starts a writer at T+1 and waits until that writer
   * has either finished (the race happened) or is blocked on the service's lock (the reader holds
   * the lock, so the race cannot happen). Only then does the reader get its timestamp T back.
   */
  @Nested
  @DisplayName("Reads racing with writes across a second boundary")
  class ReadWriteRace {

    private static final long START_SECONDS = 1_000;

    private final AtomicLong currentSeconds = new AtomicLong(START_SECONDS);
    private volatile Thread racingReader;
    private volatile Runnable racingWrite;
    private volatile Thread writerThread;

    private final Clock racingClock =
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
            if (Thread.currentThread() != racingReader) {
              return Instant.ofEpochSecond(currentSeconds.get());
            }
            racingReader = null;
            long staleSeconds = currentSeconds.getAndIncrement();
            Thread writer = new Thread(racingWrite);
            writerThread = writer;
            writer.start();
            await()
                .atMost(Duration.ofSeconds(5))
                .until(
                    () ->
                        writer.getState() == Thread.State.BLOCKED
                            || writer.getState() == Thread.State.TERMINATED);
            return Instant.ofEpochSecond(staleSeconds);
          }
        };

    private DefaultMetricsService svc;

    @BeforeEach
    void setUpRacingClock() {
      svc = new DefaultMetricsService(racingClock);
      for (int i = 0; i < 50; i++) {
        svc.recordServerError();
        svc.recordRequestLatency(500);
      }
    }

    private void raceNextReadAgainst(Runnable write) {
      racingWrite = write;
      racingReader = Thread.currentThread();
    }

    private void awaitWriter() throws InterruptedException {
      writerThread.join(TimeUnit.SECONDS.toMillis(5));
      assertThat(writerThread.isAlive()).as("racing writer finished").isFalse();
    }

    @Test
    void getErrorCountLastMinute_WhenWriteLandsInNextSecondMidRead_ShouldKeepWindow()
        throws InterruptedException {
      raceNextReadAgainst(svc::recordServerError);

      long countSeenByReader = svc.getErrorCountLastMinute();
      awaitWriter();

      assertThat(countSeenByReader).isEqualTo(50);
      assertThat(svc.getErrorCountLastMinute()).isEqualTo(51);
    }

    @Test
    void getTotalRequestCountLast60Seconds_WhenWriteLandsInNextSecondMidRead_ShouldKeepWindow()
        throws InterruptedException {
      raceNextReadAgainst(() -> svc.recordRequestLatency(500));

      long countSeenByReader = svc.getTotalRequestCountLast60Seconds();
      awaitWriter();

      assertThat(countSeenByReader).isEqualTo(50);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isEqualTo(51);
    }

    @Test
    void getAverageLatencyLast60Seconds_WhenWriteLandsInNextSecondMidRead_ShouldKeepWindow()
        throws InterruptedException {
      raceNextReadAgainst(() -> svc.recordRequestLatency(500));

      double averageSeenByReader = svc.getAverageLatencyLast60Seconds();
      awaitWriter();

      assertThat(averageSeenByReader).isEqualTo(500.0);
      assertThat(svc.getTotalRequestCountLast60Seconds()).isEqualTo(51);
    }

    @Test
    void isErrorThresholdBreached_WhenWriteLandsInNextSecondMidRead_ShouldStillBreach()
        throws InterruptedException {
      raceNextReadAgainst(svc::recordServerError);

      boolean breachedSeenByReader = svc.isErrorThresholdBreached();
      awaitWriter();

      assertThat(breachedSeenByReader).isTrue();
      assertThat(svc.isErrorThresholdBreached()).isTrue();
    }

    @Test
    void isLatencyThresholdBreached_WhenWriteLandsInNextSecondMidRead_ShouldStillBreach()
        throws InterruptedException {
      raceNextReadAgainst(() -> svc.recordRequestLatency(500));

      boolean breachedSeenByReader = svc.isLatencyThresholdBreached();
      awaitWriter();

      assertThat(breachedSeenByReader).isTrue();
      assertThat(svc.isLatencyThresholdBreached()).isTrue();
    }

    @Test
    void snapshot_WhenWriteLandsInNextSecondMidRead_ShouldDescribeOneMoment()
        throws InterruptedException {
      raceNextReadAgainst(
          () -> {
            svc.recordServerError();
            svc.recordRequestLatency(5_000);
          });

      MetricsSnapshot seenByReader = svc.snapshot();
      awaitWriter();

      // Every field is from before the racing write, none from after it.
      assertThat(seenByReader).isEqualTo(new MetricsSnapshot(50, 50, 50, 500.0, true, true));
      assertThat(svc.snapshot().getErrorsLastMinute()).isEqualTo(51);
      assertThat(svc.snapshot().getRequestsLast60Seconds()).isEqualTo(51);
    }
  }

  @Nested
  @DisplayName("Snapshot")
  class Snapshot {

    private static final Instant BASE_TIME = Instant.parse("2026-01-01T00:00:00Z");

    private Instant currentTime = BASE_TIME;
    private boolean jumpWindowAfterEachRead;

    // When jumpWindowAfterEachRead is on, each clock read moves time forward a full window, so a
    // second clock read during one snapshot would see every bucket as expired.
    private final Clock steppingClock =
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
            Instant now = currentTime;
            if (jumpWindowAfterEachRead) {
              currentTime = currentTime.plus(Duration.ofSeconds(60));
            }
            return now;
          }
        };

    private DefaultMetricsService svc;

    @BeforeEach
    void setUpClock() {
      currentTime = BASE_TIME;
      jumpWindowAfterEachRead = false;
      svc = new DefaultMetricsService(steppingClock);
    }

    @Test
    void snapshot_WhenNoTraffic_ShouldReportZerosAndNoBreach() {
      assertThat(svc.snapshot()).isEqualTo(new MetricsSnapshot(0, 0, 0, 0.0, false, false));
    }

    @Test
    void snapshot_WhenMetricsRecorded_ShouldMatchIndividualGetters() {
      for (int i = 0; i < 10; i++) {
        svc.recordRequestLatency(150);
      }
      for (int i = 0; i < 6; i++) {
        svc.recordServerError();
      }

      MetricsSnapshot snapshot = svc.snapshot();

      assertThat(snapshot.getErrorsLastMinute()).isEqualTo(svc.getErrorCountLastMinute());
      assertThat(snapshot.getTotalErrors()).isEqualTo(svc.getTotalErrorCount());
      assertThat(snapshot.getRequestsLast60Seconds())
          .isEqualTo(svc.getTotalRequestCountLast60Seconds());
      assertThat(snapshot.getAverageLatencyLast60Seconds())
          .isEqualTo(svc.getAverageLatencyLast60Seconds());
      assertThat(snapshot.isErrorThresholdBreached()).isEqualTo(svc.isErrorThresholdBreached());
      assertThat(snapshot.isLatencyThresholdBreached()).isEqualTo(svc.isLatencyThresholdBreached());
      assertThat(snapshot).isEqualTo(new MetricsSnapshot(6, 6, 10, 150.0, true, true));
    }

    @Test
    void snapshot_WhenClockAdvancesDuringSnapshot_ShouldUseSingleClockReading() {
      for (int i = 0; i < 10; i++) {
        svc.recordServerError();
        svc.recordRequestLatency(200);
      }
      jumpWindowAfterEachRead = true;

      MetricsSnapshot snapshot = svc.snapshot();

      assertThat(snapshot).isEqualTo(new MetricsSnapshot(10, 10, 10, 200.0, true, true));
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    void customConstructor_WhenNonDefaultBucketCounts_ShouldRespectConfiguration() {
      DefaultMetricsService custom =
          new DefaultMetricsService(10, 10, 50, 200.0, 3, 5, Clock.systemUTC());
      assertThat(custom.getDefaultErrorThreshold()).isEqualTo(50);
      assertThat(custom.getDefaultLatencyThresholdMs()).isEqualTo(200.0);
      // minimumLatencySampleSize=3: should evaluate after 3 requests
      custom.recordRequestLatency(500);
      custom.recordRequestLatency(500);
      custom.recordRequestLatency(500);
      assertThat(custom.isLatencyThresholdBreached()).isTrue();
      // minimumErrorSampleSize=5: 4 requests not enough to evaluate
      for (int i = 0; i < 4; i++) {
        custom.recordServerError();
        custom.recordRequestLatency(100);
      }
      // Still only 4+3=7 latency records but errors need 5 error samples with
      // requestCount >= minimumErrorSampleSize
      // Let's test a fresh one cleanly
      DefaultMetricsService custom2 =
          new DefaultMetricsService(10, 10, 50, 200.0, 3, 5, Clock.systemUTC());
      for (int i = 0; i < 4; i++) {
        custom2.recordServerError();
        custom2.recordRequestLatency(100);
      }
      assertThat(custom2.isErrorThresholdBreached()).isFalse();
      custom2.recordServerError();
      custom2.recordRequestLatency(100);
      assertThat(custom2.isErrorThresholdBreached()).isTrue();
    }

    @Test
    void isErrorThresholdBreached_WhenErrorsRecordedWithoutLatency_ShouldNotBreach() {
      for (int i = 0; i < 100; i++) {
        metricsService.recordServerError();
      }
      // No latency records → requestCount is 0 → below minimumErrorSampleSize
      assertThat(metricsService.isErrorThresholdBreached()).isFalse();
    }

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
