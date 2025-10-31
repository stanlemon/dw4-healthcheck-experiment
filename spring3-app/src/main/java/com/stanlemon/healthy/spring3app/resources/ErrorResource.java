package com.stanlemon.healthy.spring3app.resources;

import com.stanlemon.healthy.spring3app.exceptions.SomethingWentWrongException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for testing error handling and metrics tracking by deliberately throwing
 * exceptions.
 */
@RestController
@RequestMapping("/error-trigger")
public class ErrorResource {

  /**
   * Deliberately triggers a server error to test exception handling and error metrics.
   *
   * @return never returns normally, always throws an exception
   * @throws SomethingWentWrongException always thrown to simulate an error condition
   */
  @GetMapping
  public void triggerError() {
    // We don't need to manually record the error anymore
    // as the GlobalExceptionHandler will handle that

    // Deliberately throw a runtime exception that will be caught by our handler
    throw new SomethingWentWrongException(
        "This is a deliberate error that will be caught by our global handler");
  }
}
