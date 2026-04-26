package com.stanlemon.healthy.dw4app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import jakarta.ws.rs.core.Response;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for SlowResource endpoints.
 *
 * <p>NOTE: We intentionally do NOT test the default slow endpoint (defaultSlow()) because it has a
 * 1-second delay which would significantly slow down test execution. The default endpoint is
 * validated through manual testing and the parameterized endpoint tests cover all the core
 * functionality.
 */
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
      Response response = resource.slowWithDelay(5);
      long elapsedTime = System.currentTimeMillis() - startTime;

      assertThat(response.getStatus()).isEqualTo(200);
      assertThat(elapsedTime).isGreaterThanOrEqualTo(5); // Should take at least 5ms

      SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
      assertThat(slowResponse.getDelayMs()).isEqualTo(5);
      assertThat(slowResponse.getActualMs()).isGreaterThanOrEqualTo(5);
      assertThat(slowResponse.getMessage())
          .contains("Slow request completed")
          .contains("5ms delay");
    }

    @Test
    @DisplayName("Should return success immediately when delay is zero")
    void slowWithDelay_WhenZeroDelay_ShouldReturnSuccessImmediately() {
      Response response = resource.slowWithDelay(0);

      assertThat(response.getStatus()).isEqualTo(200);

      SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
      assertThat(slowResponse.getDelayMs()).isZero();
      assertThat(slowResponse.getActualMs()).isGreaterThanOrEqualTo(0);
      assertThat(slowResponse.getMessage())
          .contains("Slow request completed")
          .contains("0ms delay");
    }

    @Test
    @DisplayName("Should return 400 error when delay is negative")
    void slowWithDelay_WhenNegativeDelay_ShouldReturn400Error() {
      Response response = resource.slowWithDelay(-100);

      assertThat(response.getStatus()).isEqualTo(400);

      SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
      assertThat(slowResponse.getDelayMs()).isEqualTo(-100);
      assertThat(slowResponse.getActualMs()).isZero();
      assertThat(slowResponse.getMessage()).isEqualTo("Delay cannot be negative");
    }

    @Test
    @DisplayName("Should return 400 error when delay is excessive")
    void slowWithDelay_WhenExcessiveDelay_ShouldReturn400Error() {
      Response response = resource.slowWithDelay(15000);

      assertThat(response.getStatus()).isEqualTo(400);

      SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
      assertThat(slowResponse.getDelayMs()).isEqualTo(15000);
      assertThat(slowResponse.getActualMs()).isZero();
      assertThat(slowResponse.getMessage()).isEqualTo("Delay too long. Maximum allowed is 10000ms");
    }
  }

  @Nested
  @DisplayName("Boundary tests")
  class BoundaryTests {

    @Test
    @DisplayName("Should return 400 when delay is exactly 10001ms (just over limit)")
    void slowWithDelay_WhenExactlyOverLimit_ShouldReturn400() {
      Response response = resource.slowWithDelay(10001);

      assertThat(response.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("Should reject delay one above the 10000ms limit")
    void slowWithDelay_WhenOneAboveLimit_ShouldReturn400() {
      // Verify the 10000ms cap is enforced from the other side — 10001 is rejected with a 400.
      // We don't assert on the happy-path boundary (slowWithDelay(10000)) because it would
      // actually Thread.sleep for 10 seconds in the test suite.
      Response response = resource.slowWithDelay(10001);
      assertThat(response.getStatus()).isEqualTo(400);
    }
  }

  @Nested
  @DisplayName("Thread interruption tests")
  class ThreadInterruptionTests {

    @Test
    @DisplayName("Should handle interruption gracefully when thread is interrupted")
    void slowWithDelay_WhenThreadInterrupted_ShouldHandleInterruptionGracefully()
        throws InterruptedException {
      // Create a separate thread to test interruption
      SlowResource testResource = new SlowResource();
      final Response[] result = new Response[1];
      final Exception[] exception = new Exception[1];

      // Use CountDownLatch to ensure proper synchronization
      CountDownLatch threadStarted = new CountDownLatch(1);
      CountDownLatch threadCompleted = new CountDownLatch(1);

      Thread testThread =
          new Thread(
              () -> {
                try {
                  threadStarted.countDown(); // Signal that thread has started
                  result[0] =
                      testResource.slowWithDelay(100); // Use a longer delay to ensure interruption
                } catch (Exception e) {
                  exception[0] = e;
                } finally {
                  threadCompleted.countDown(); // Signal that thread has completed
                }
              });

      testThread.start();

      assertThat(threadStarted.await(1, TimeUnit.SECONDS)).isTrue();

      // Wait for the thread to actually enter Thread.sleep (TIMED_WAITING state)
      await()
          .atMost(Duration.ofSeconds(2))
          .pollInterval(Duration.ofMillis(5))
          .until(() -> testThread.getState() == Thread.State.TIMED_WAITING);

      testThread.interrupt();

      // Wait for the thread to complete (more reliable than thread.join with timeout)
      assertThat(threadCompleted.await(2, TimeUnit.SECONDS)).isTrue();

      // Verify that the interruption was handled correctly
      assertThat(result[0]).isNotNull();
      assertThat(result[0].getStatus()).isEqualTo(500);

      SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) result[0].getEntity();
      assertThat(slowResponse.getMessage()).isEqualTo("Request was interrupted");
      assertThat(slowResponse.getDelayMs()).isEqualTo(100);
      assertThat(slowResponse.getActualMs())
          .isLessThan(100); // Should be interrupted before full delay

      // Verify that no exception was thrown (it was handled internally)
      assertThat(exception[0]).isNull();

      // Verify that the thread's interrupt status was properly restored
      assertThat(testThread.isInterrupted()).isTrue();
    }
  }
}
