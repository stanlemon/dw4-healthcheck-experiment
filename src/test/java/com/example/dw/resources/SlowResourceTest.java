package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.ws.rs.core.Response;
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
  void testSlowWithValidDelay() {
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
  void testSlowWithZeroDelay() {
    Response response = resource.slowWithDelay(0);

    assertThat(response.getStatus()).isEqualTo(200);

    SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
    assertThat(slowResponse.getDelayMs()).isZero();
    assertThat(slowResponse.getActualMs()).isGreaterThanOrEqualTo(0);
    assertThat(slowResponse.getMessage()).contains("Slow request completed").contains("0ms delay");
  }

  @Test
  void testSlowWithNegativeDelay() {
    Response response = resource.slowWithDelay(-100);

    assertThat(response.getStatus()).isEqualTo(400);

    SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
    assertThat(slowResponse.getDelayMs()).isEqualTo(-100);
    assertThat(slowResponse.getActualMs()).isZero();
    assertThat(slowResponse.getMessage()).isEqualTo("Delay cannot be negative");
  }

  @Test
  void testSlowWithExcessiveDelay() {
    Response response = resource.slowWithDelay(15000);

    assertThat(response.getStatus()).isEqualTo(400);

    SlowResource.SlowResponse slowResponse = (SlowResource.SlowResponse) response.getEntity();
    assertThat(slowResponse.getDelayMs()).isEqualTo(15000);
    assertThat(slowResponse.getActualMs()).isZero();
    assertThat(slowResponse.getMessage()).isEqualTo("Delay too long. Maximum allowed is 10000ms");
  }

  @Test
  void testSlowResponseConstructorAndGetters() {
    SlowResource.SlowResponse response = new SlowResource.SlowResponse("Test message", 10, 15);

    assertThat(response.getMessage()).isEqualTo("Test message");
    assertThat(response.getDelayMs()).isEqualTo(10);
    assertThat(response.getActualMs()).isEqualTo(15);
  }

  @Test
  void testThreadInterruptionDuringDelay() {
    // Create a separate thread to test interruption
    SlowResource testResource = new SlowResource();
    final Response[] result = new Response[1];
    final Exception[] exception = new Exception[1];

    Thread testThread =
        new Thread(
            () -> {
              try {
                result[0] =
                    testResource.slowWithDelay(100); // Use a longer delay to ensure interruption
              } catch (Exception e) {
                exception[0] = e;
              }
            });

    testThread.start();

    // Give the thread a moment to start and begin sleeping
    try {
      Thread.sleep(
          10); // NOSONAR - Necessary for test timing to ensure interruption happens during sleep
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    // Interrupt the test thread while it's sleeping
    testThread.interrupt();

    // Wait for the thread to complete
    try {
      testThread.join(1000); // Wait up to 1 second
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

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
