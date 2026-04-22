package com.stanlemon.healthy.spring3app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

    @Test
    @DisplayName("Should return 400 error when delay is negative")
    void slowWithDelay_WhenNegativeDelay_ShouldReturn400Error() {
      ResponseEntity<SlowResource.SlowResponse> response = resource.slowWithDelay(-100);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getMessage()).isEqualTo("Delay cannot be negative");
    }

    @Test
    @DisplayName("Should return 400 error when delay is excessive")
    void slowWithDelay_WhenExcessiveDelay_ShouldReturn400Error() {
      ResponseEntity<SlowResource.SlowResponse> response = resource.slowWithDelay(15000);

      assertThat(response.getStatusCode().value()).isEqualTo(400);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().getMessage())
          .isEqualTo("Delay too long. Maximum allowed is 10000ms");
    }
  }

  @Nested
  @DisplayName("Response object tests")
  class ResponseTests {

    @Test
    @DisplayName("Should set fields correctly when SlowResponse is constructed")
    void slowResponse_WhenConstructed_ShouldSetFieldsCorrectly() {
      SlowResource.SlowResponse response = new SlowResource.SlowResponse("Test message", 10, 15);

      assertThat(response.getMessage()).isEqualTo("Test message");
      assertThat(response.getDelayMs()).isEqualTo(10);
      assertThat(response.getActualMs()).isEqualTo(15);
    }
  }

  @Nested
  @DisplayName("Thread interruption tests")
  class ThreadInterruptionTests {

    @Test
    @DisplayName("Should handle interruption gracefully when thread is interrupted")
    void slowWithDelay_WhenThreadInterrupted_ShouldHandleGracefully() throws InterruptedException {
      SlowResource testResource = new SlowResource();
      AtomicReference<ResponseEntity<SlowResource.SlowResponse>> result = new AtomicReference<>();

      CountDownLatch threadStarted = new CountDownLatch(1);
      CountDownLatch threadCompleted = new CountDownLatch(1);

      Thread testThread =
          new Thread(
              () -> {
                try {
                  threadStarted.countDown();
                  result.set(testResource.slowWithDelay(100));
                } finally {
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

      assertThat(result.get()).isNotNull();
      assertThat(result.get().getStatusCode().value()).isEqualTo(500);
      assertThat(result.get().getBody()).isNotNull();
      assertThat(result.get().getBody().getMessage()).isEqualTo("Request was interrupted");
      assertThat(result.get().getBody().getActualMs()).isLessThan(100);
      assertThat(testThread.isInterrupted()).isTrue();
    }
  }
}
