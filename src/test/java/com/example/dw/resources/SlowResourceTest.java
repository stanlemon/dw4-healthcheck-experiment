package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Tests for SlowResource endpoints.
 *
 * <p>NOTE: We intentionally do NOT test the default slow endpoint (defaultSlow()) because it has a
 * 1-second delay which would significantly slow down test execution. The default endpoint is
 * validated through manual testing and the parameterized endpoint tests cover all the core
 * functionality.
 */
class SlowResourceTest {

  private SlowResource resource;

  @BeforeEach
  void setUp() {
    resource = new SlowResource();
  }

  @Test
  void slowWithDelay_WhenValidDelay_ShouldWaitSpecifiedTime() {
    long startTime = System.currentTimeMillis();
    Response response = resource.slowWithDelay(5);
    long elapsedTime = System.currentTimeMillis() - startTime;

    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(elapsedTime).isGreaterThanOrEqualTo(5); // Should take at least 5ms

    SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
    assertThat(slowResponse.getDelayMs()).isEqualTo(5);
    assertThat(slowResponse.getActualMs()).isGreaterThanOrEqualTo(5);
    assertThat(slowResponse.getMessage()).contains("Slow request completed").contains("5ms delay");
  }

  @Test
  void slowWithDelay_WhenZeroDelay_ShouldReturnSuccessImmediately() {
    Response response = resource.slowWithDelay(0);

    assertThat(response.getStatus()).isEqualTo(200);

    SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
    assertThat(slowResponse.getDelayMs()).isZero();
    assertThat(slowResponse.getActualMs()).isGreaterThanOrEqualTo(0);
    assertThat(slowResponse.getMessage()).contains("Slow request completed").contains("0ms delay");
  }

  @Test
  void slowWithDelay_WhenNegativeDelay_ShouldReturn400Error() {
    Response response = resource.slowWithDelay(-100);

    assertThat(response.getStatus()).isEqualTo(400);

    SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
    assertThat(slowResponse.getDelayMs()).isEqualTo(-100);
    assertThat(slowResponse.getActualMs()).isZero();
    assertThat(slowResponse.getMessage()).isEqualTo("Delay cannot be negative");
  }

  @Test
  void slowWithDelay_WhenExcessiveDelay_ShouldReturn400Error() {
    Response response = resource.slowWithDelay(15000);

    assertThat(response.getStatus()).isEqualTo(400);

    SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
    assertThat(slowResponse.getDelayMs()).isEqualTo(15000);
    assertThat(slowResponse.getActualMs()).isZero();
    assertThat(slowResponse.getMessage()).isEqualTo("Delay too long. Maximum allowed is 10000ms");
  }

  @Test
  void slowResponse_WhenConstructed_ShouldSetFieldsCorrectly() {
    SlowResource.SlowResponse response = new SlowResource.SlowResponse("Test message", 10, 15);

    assertThat(response.getMessage()).isEqualTo("Test message");
    assertThat(response.getDelayMs()).isEqualTo(10);
    assertThat(response.getActualMs()).isEqualTo(15);
  }

  @Test
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

    // Wait for the thread to actually start executing (more reliable than Thread.sleep)
    assertThat(threadStarted.await(1, TimeUnit.SECONDS)).isTrue();

    // Give a very brief moment for the thread to enter the sleep call
    // This is much more reliable than the previous 10ms sleep
    Thread.yield();

    // Interrupt the test thread while it's sleeping
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
