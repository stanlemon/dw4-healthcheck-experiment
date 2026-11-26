package com.stanlemon.healthy.spring4app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.ResponseEntity;

@DisplayName("Slow Resource Tests")
class SlowResourceTest {

  private SlowResource resource;

  @BeforeEach
  void setUp() {
    resource = new SlowResource();
  }

  @Nested
  @DisplayName("Delay handling tests")
  class DelayTests {

    @Test
    @DisplayName("Should wait specified time when given valid delay")
    void slowWithDelay_WhenValidDelay_ShouldWaitSpecifiedTime() {
      long startTime = System.currentTimeMillis();
      ResponseEntity<SlowResource.SlowResponse> response = resource.slowWithDelay(5);
      long elapsedTime = System.currentTimeMillis() - startTime;

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(elapsedTime).isGreaterThanOrEqualTo(5);

      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getDelayMs()).isEqualTo(5);
      assertThat(response.getBody().getActualMs()).isGreaterThanOrEqualTo(5);
      assertThat(response.getBody().getMessage()).contains("Slow request completed");
    }

    @Test
    @DisplayName("Should return success immediately when delay is zero")
    void slowWithDelay_WhenZeroDelay_ShouldReturnSuccessImmediately() {
      ResponseEntity<SlowResource.SlowResponse> response = resource.slowWithDelay(0);

      assertThat(response.getStatusCode().value()).isEqualTo(200);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getDelayMs()).isZero();
    }

    // 10000 itself is allowed, but testing it would sleep for 10 seconds, so the upper boundary is
    // checked from the rejected side only.
    @ParameterizedTest(name = "{0}ms -> 400")
    @CsvSource(
        delimiter = '|',
        value = {
          "-1     | Delay cannot be negative",
          "-100   | Delay cannot be negative",
          "10001  | Delay too long. Maximum allowed is 10000ms",
          "15000  | Delay too long. Maximum allowed is 10000ms"
        })
    @DisplayName("Should return 400 without sleeping when delay is out of range")
    void slowWithDelay_WhenDelayOutOfRange_ShouldReturn400(int delayMs, String message) {
      ResponseEntity<SlowResource.SlowResponse> response = resource.slowWithDelay(delayMs);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
      assertThat(response.getBody()).isEqualTo(new SlowResource.SlowResponse(message, delayMs, 0));
    }
  }

  @Nested
  @DisplayName("Thread interruption tests")
  class ThreadInterruptionTests {

    @Test
    @DisplayName("Should throw so the global handler records a server error when interrupted")
    void slowWithDelay_WhenThreadInterrupted_ShouldThrowAndRestoreInterruptStatus()
        throws InterruptedException {
      SlowResource testResource = new SlowResource();
      AtomicReference<ResponseEntity<SlowResource.SlowResponse>> result = new AtomicReference<>();
      AtomicReference<RuntimeException> thrown = new AtomicReference<>();
      AtomicBoolean interruptStatusAfterCall = new AtomicBoolean();

      CountDownLatch threadStarted = new CountDownLatch(1);
      CountDownLatch threadCompleted = new CountDownLatch(1);

      Thread testThread =
          new Thread(() -> {
            try {
              threadStarted.countDown();
              result.set(testResource.slowWithDelay(100));
            } catch (RuntimeException e) {
              thrown.set(e);
            } finally {
              // Read on the thread itself, before it terminates
              interruptStatusAfterCall.set(Thread.currentThread().isInterrupted());
              threadCompleted.countDown();
            }
          });

      testThread.start();
      assertThat(threadStarted.await(1, TimeUnit.SECONDS)).isTrue();

      await()
          .atMost(Duration.ofSeconds(2))
          .pollInterval(Duration.ofMillis(5))
          .until(() -> testThread.getState() == Thread.State.TIMED_WAITING);

      testThread.interrupt();
      assertThat(threadCompleted.await(2, TimeUnit.SECONDS)).isTrue();

      // No hand-built 500: the exception goes to GlobalExceptionHandler, which counts the error
      assertThat(result.get()).isNull();
      assertThat(thrown.get())
          .isInstanceOf(SomethingWentWrongException.class)
          .hasMessage("Request was interrupted")
          .hasCauseInstanceOf(InterruptedException.class);
      assertThat(interruptStatusAfterCall).isTrue();
    }
  }
}
