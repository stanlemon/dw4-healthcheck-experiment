package com.stanlemon.healthy.spring3app.resources;

import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test resource that introduces artificial delays to test latency tracking and monitoring.
 *
 * <p>This resource allows clients to request a specific delay in milliseconds to simulate slow
 * responses. It's useful for testing latency monitoring systems and alerting thresholds.
 */
@RestController
@RequestMapping("/slow")
@Slf4j
public class SlowResource {

  /** Response class for slow endpoint results. */
  @Value
  public static class SlowResponse {
    String message;
    long delayMs;
    long actualMs;
  }

  /**
   * Default slow endpoint that sleeps for 1 second.
   *
   * @return response with delay information
   */
  @GetMapping
  public ResponseEntity<SlowResponse> defaultSlow() {
    return slowRequest(1000);
  }

  /**
   * Parameterized slow endpoint that sleeps for the specified number of milliseconds.
   *
   * @param delayMs the number of milliseconds to sleep
   * @return response with delay information
   */
  @GetMapping("/{delayMs}")
  public ResponseEntity<SlowResponse> slowWithDelay(@PathVariable("delayMs") long delayMs) {
    // Limit delay to prevent abuse (max 10 seconds)
    if (delayMs > 10000) {
      return ResponseEntity.badRequest()
          .body(new SlowResponse("Delay too long. Maximum allowed is 10000ms", delayMs, 0));
    }

    if (delayMs < 0) {
      return ResponseEntity.badRequest()
          .body(new SlowResponse("Delay cannot be negative", delayMs, 0));
    }

    return slowRequest(delayMs);
  }

  /**
   * Helper method to perform the actual slow request.
   *
   * @param delayMs the number of milliseconds to sleep
   * @return response with delay information
   */
  private ResponseEntity<SlowResponse> slowRequest(long delayMs) {
    log.info("Delaying response for {} milliseconds", delayMs);
    long startTime = System.currentTimeMillis();

    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt(); // Restore interrupt status
      return ResponseEntity.internalServerError()
          .body(
              new SlowResponse(
                  "Request was interrupted", delayMs, System.currentTimeMillis() - startTime));
    }

    long actualMs = System.currentTimeMillis() - startTime;

    String message =
        String.format(
            "Slow request completed. Requested %dms delay, actual %dms", delayMs, actualMs);

    return ResponseEntity.ok(new SlowResponse(message, delayMs, actualMs));
  }
}
