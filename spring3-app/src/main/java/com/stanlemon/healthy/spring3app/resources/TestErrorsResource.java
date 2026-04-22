package com.stanlemon.healthy.spring3app.resources;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST resource for testing different types of exceptions and their handling by the global
 * exception handler.
 */
@RestController
@RequestMapping("/test-errors")
public class TestErrorsResource {

  /**
   * Deliberately triggers a server error to test exception handling and error metrics.
   *
   * @return never returns normally, always throws an exception
   * @throws SomethingWentWrongException always thrown to simulate an error condition
   */
  @GetMapping("/trigger")
  public String triggerError() {
    throw new SomethingWentWrongException(
        "This is a deliberate error that will be caught by our global handler");
  }

  /**
   * Tests runtime exception handling with a custom message.
   *
   * @param message the error message to include in the exception
   * @return never returns normally, always throws an exception
   * @throws SomethingWentWrongException always thrown with the provided message
   */
  @GetMapping("/runtime/{message}")
  public String testRuntimeException(@PathVariable("message") String message) {
    throw new SomethingWentWrongException(message);
  }

  /**
   * Tests WebApplicationException handling with a specific HTTP status code.
   *
   * @param code the HTTP status code to use in the exception
   * @return never returns normally, always throws an exception
   * @throws RuntimeException always thrown with the specified status code
   */
  @GetMapping("/web-app/{code}")
  public String testWebAppException(@PathVariable("code") int code) {
    throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.valueOf(code),
        "Web application exception with code " + code);
  }
}
