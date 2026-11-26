package com.stanlemon.healthy.metrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("DefaultMetricsService")
class DefaultMetricsServiceTest {

  /** A clock the test moves by hand. Reads from any thread see the latest value. */
  private static final class ManualClock extends Clock {
    private volatile Instant now = Instant.parse("2026-01-01T00:00:00Z");

    void advance(Duration duration) {
      now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      return this;
    }

    @Override
    public Instant instant() {
      return now;
    }
  }

  private ManualClock clock;
  private DefaultMetricsService service;

  @BeforeEach
  void setUp() {
    clock = new ManualClock();
    service = new DefaultMetricsService(clock);
  }

  private void record(int count, long latencyMs, boolean serverError) {
    for (int i = 0; i < count; i++) {
      service.recordRequest(latencyMs, serverError);
    }
  }

  @Nested
  @DisplayName("Recording")
  class Recording {

    @Test
    @DisplayName("Should report zeros and default thresholds when no traffic")
    void snapshot_WhenNoTraffic_ShouldReportZerosAndDefaultThresholds() {
      assertThat(service.snapshot())
          .isEqualTo(new MetricsSnapshot(0, 0, 0, 0.0, false, false, 0.5, 100.0));
    }

    @Test
    @DisplayName("Should count request without error when successful")
    void recordRequest_WhenSuccessful_ShouldCountRequestWithoutError() {
      service.recordRequest(40, false);
      service.recordRequest(60, false);

      MetricsSnapshot snapshot = service.snapshot();
      assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(2);
      assertThat(snapshot.getAverageLatencyLast60Seconds()).isEqualTo(50.0);
      assertThat(snapshot.getErrorsLastMinute()).isZero();
      assertThat(snapshot.getTotalErrors()).isZero();
    }

    @Test
    @DisplayName("Should count both error and request when server error")
    void recordRequest_WhenServerError_ShouldCountBothErrorAndRequest() {
      service.recordRequest(10, true);

      MetricsSnapshot snapshot = service.snapshot();
      assertThat(snapshot.getErrorsLastMinute()).isEqualTo(1);
      assertThat(snapshot.getTotalErrors()).isEqualTo(1);
      assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should clamp to zero when negative latency")
    void recordRequest_WhenNegativeLatency_ShouldClampToZero() {
      service.recordRequest(-50, false);
      service.recordRequest(100, false);

      assertThat(service.snapshot().getAverageLatencyLast60Seconds()).isEqualTo(50.0);
    }
  }

  @Nested
  @DisplayName("Thresholds")
  class Thresholds {

    @ParameterizedTest(name = "{0} errors out of {1} requests -> breached {2}")
    @CsvSource({
      // Fewer than 10 requests is too little traffic to judge.
      "9, 9, false",
      // 10-99 requests: breach above a 50% error rate.
      "10, 20, false",
      "11, 20, true",
      // 100+ requests: breach above a 10% error rate.
      "10, 100, false",
      "11, 100, true"
    })
    @DisplayName("Should breach the error threshold above the rate for the traffic level")
    void errorThreshold_WhenGivenTraffic_ShouldBreachAboveRate(
        int errors, int requests, boolean expected) {
      for (int i = 0; i < requests; i++) {
        service.recordRequest(1, i < errors);
      }

      assertThat(service.snapshot().isErrorThresholdBreached()).isEqualTo(expected);
    }

    @ParameterizedTest(name = "{0} requests at {1}ms -> breached {2}")
    @CsvSource({
      // Fewer than 5 requests is too little traffic to judge.
      "4, 500, false",
      // Breach only above 100ms, not at it.
      "5, 100, false",
      "5, 101, true"
    })
    @DisplayName("Should breach the latency threshold only above 100ms with enough requests")
    void latencyThreshold_WhenGivenTraffic_ShouldBreachAboveThreshold(
        int requests, long latencyMs, boolean expected) {
      record(requests, latencyMs, false);

      assertThat(service.snapshot().isLatencyThresholdBreached()).isEqualTo(expected);
    }
  }

  @Nested
  @DisplayName("Sliding window")
  class SlidingWindow {

    @Test
    @DisplayName("Should still count requests when fifty nine seconds pass")
    void snapshot_WhenFiftyNineSecondsPass_ShouldStillCountRequests() {
      service.recordRequest(200, true);
      clock.advance(Duration.ofSeconds(59));

      MetricsSnapshot snapshot = service.snapshot();
      assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(1);
      assertThat(snapshot.getErrorsLastMinute()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should drop old requests but keep total errors when sixty seconds pass")
    void snapshot_WhenSixtySecondsPass_ShouldDropOldRequestsButKeepTotalErrors() {
      service.recordRequest(200, true);
      clock.advance(Duration.ofSeconds(60));

      assertThat(service.snapshot())
          .isEqualTo(new MetricsSnapshot(0, 1, 0, 0.0, false, false, 0.5, 100.0));
    }

    @Test
    @DisplayName("Should count only recent requests when part of window expires")
    void snapshot_WhenPartOfWindowExpires_ShouldCountOnlyRecentRequests() {
      service.recordRequest(300, true);
      clock.advance(Duration.ofSeconds(30));
      service.recordRequest(100, false);
      clock.advance(Duration.ofSeconds(30));

      MetricsSnapshot snapshot = service.snapshot();
      assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(1);
      assertThat(snapshot.getErrorsLastMinute()).isZero();
      assertThat(snapshot.getAverageLatencyLast60Seconds()).isEqualTo(100.0);
    }

    @Test
    @DisplayName("Should start from empty window when window went stale before write")
    void recordRequest_WhenWindowWentStaleBeforeWrite_ShouldStartFromEmptyWindow() {
      service.recordRequest(300, true);
      clock.advance(Duration.ofMinutes(5));
      service.recordRequest(100, false);

      MetricsSnapshot snapshot = service.snapshot();
      assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(1);
      assertThat(snapshot.getErrorsLastMinute()).isZero();
    }

    @Test
    @DisplayName("Should keep window when clock steps back less than window")
    void recordRequest_WhenClockStepsBackLessThanWindow_ShouldKeepWindow() {
      service.recordRequest(300, true);
      clock.advance(Duration.ofSeconds(-1));
      service.recordRequest(100, false);

      MetricsSnapshot snapshot = service.snapshot();
      assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(2);
      assertThat(snapshot.getErrorsLastMinute()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should expire from newest second when clock steps back after small step")
    void snapshot_WhenClockStepsBackAfterSmallStep_ShouldExpireFromNewestSecond() {
      service.recordRequest(300, true);
      clock.advance(Duration.ofSeconds(-30));
      assertThat(service.snapshot().getRequestsLast60Seconds()).isEqualTo(1);

      // The window still ends at the newest second seen, so the request expires 60s after it.
      clock.advance(Duration.ofSeconds(89));
      assertThat(service.snapshot().getRequestsLast60Seconds()).isEqualTo(1);

      clock.advance(Duration.ofSeconds(1));
      assertThat(service.snapshot().getRequestsLast60Seconds()).isZero();
    }

    @Test
    @DisplayName("Should reset window when clock jumps back a whole window")
    void snapshot_WhenClockJumpsBackAWholeWindow_ShouldResetWindow() {
      service.recordRequest(300, true);
      clock.advance(Duration.ofSeconds(-60));

      assertThat(service.snapshot().getRequestsLast60Seconds()).isZero();
    }

    @Test
    @DisplayName("Should still record when clock is before epoch")
    void recordRequest_WhenClockIsBeforeEpoch_ShouldStillRecord() {
      // -125 % 60 is -5, which is not a valid bucket index.
      DefaultMetricsService beforeEpoch =
          new DefaultMetricsService(Clock.fixed(Instant.ofEpochSecond(-125), ZoneOffset.UTC));

      beforeEpoch.recordRequest(10, true);

      assertThat(beforeEpoch.snapshot().getRequestsLast60Seconds()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("Concurrency")
  class Concurrency {

    @Test
    @DisplayName("Should count every request when called from many threads")
    void recordRequest_WhenCalledFromManyThreads_ShouldCountEveryRequest()
        throws InterruptedException {
      int threads = 20;
      int requestsPerThread = 500;
      CountDownLatch start = new CountDownLatch(1);
      CountDownLatch done = new CountDownLatch(threads);

      for (int t = 0; t < threads; t++) {
        new Thread(
                () -> {
                  try {
                    start.await();
                    for (int i = 0; i < requestsPerThread; i++) {
                      // Every tenth request fails; all take 10ms.
                      service.recordRequest(10, i % 10 == 0);
                    }
                  } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                  } finally {
                    done.countDown();
                  }
                })
            .start();
      }
      start.countDown();
      assertThat(done.await(10, TimeUnit.SECONDS)).isTrue();

      MetricsSnapshot snapshot = service.snapshot();
      assertThat(snapshot.getRequestsLast60Seconds()).isEqualTo(threads * requestsPerThread);
      assertThat(snapshot.getErrorsLastMinute()).isEqualTo(threads * requestsPerThread / 10);
      assertThat(snapshot.getAverageLatencyLast60Seconds()).isEqualTo(10.0);
    }
  }

  /**
   * Regression test for a reader that read the clock before taking the lock. Such a reader could
   * come back with a timestamp older than a write that landed in the next second while it waited,
   * and the window would treat that as the clock moving backwards and wipe every bucket.
   *
   * <p>The clock forces that interleaving with no sleeps. On the reader's clock call it starts a
   * writer one second later, then waits until that writer either finished (the race happened) or is
   * blocked on the service's lock (the reader holds the lock, so it cannot). Only then does it
   * return the reader's older timestamp.
   */
  @Nested
  @DisplayName("Read racing a write across a second boundary")
  class ReadWriteRace {

    private final AtomicLong seconds = new AtomicLong(1_000);
    private volatile Thread racingReader;
    private volatile Thread writer;
    private DefaultMetricsService racing;

    private final Clock racingClock =
        new Clock() {
          @Override
          public ZoneId getZone() {
            return ZoneOffset.UTC;
          }

          @Override
          public Clock withZone(ZoneId zone) {
            return this;
          }

          @Override
          public Instant instant() {
            if (Thread.currentThread() != racingReader) {
              return Instant.ofEpochSecond(seconds.get());
            }
            racingReader = null;
            long readerSecond = seconds.getAndIncrement();
            writer = new Thread(() -> racing.recordRequest(5_000, true));
            writer.start();
            await()
                .atMost(Duration.ofSeconds(5))
                .until(() ->
                    writer.getState() == Thread.State.BLOCKED
                        || writer.getState() == Thread.State.TERMINATED);
            return Instant.ofEpochSecond(readerSecond);
          }
        };

    @Test
    @DisplayName("Should keep window when write lands in next second mid read")
    void snapshot_WhenWriteLandsInNextSecondMidRead_ShouldKeepWindow() throws InterruptedException {
      racing = new DefaultMetricsService(racingClock);
      for (int i = 0; i < 50; i++) {
        racing.recordRequest(500, true);
      }
      racingReader = Thread.currentThread();

      MetricsSnapshot seenByReader = racing.snapshot();
      writer.join(TimeUnit.SECONDS.toMillis(5));
      assertThat(writer.isAlive()).as("racing writer finished").isFalse();

      // Every field is from before the racing write, none from after it.
      assertThat(seenByReader)
          .isEqualTo(new MetricsSnapshot(50, 50, 50, 500.0, true, true, 0.5, 100.0));
      assertThat(racing.snapshot().getRequestsLast60Seconds()).isEqualTo(51);
    }
  }
}
