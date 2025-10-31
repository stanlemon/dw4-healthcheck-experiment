package com.stanlemon.healthy.spring3app.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stanlemon.healthy.spring3app.exceptions.SomethingWentWrongException;
import lombok.Data;
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

  /** Error message wrapper for JSON responses. */
  @Data
  public static class ErrorMessage {
    @JsonProperty private String message;

    @JsonProperty private int code;

    public ErrorMessage(String message, int code) {
      this.message = message;
      this.code = code;
    }
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
    // In Spring, we'll use ResponseStatusException instead of WebApplicationException
    // but first we'll build the response object with our custom error message
    ErrorMessage errorMessage =
        new ErrorMessage("Web application exception with code " + code, code);

    // Now throw an exception with the specified status code
    // This will be caught by the global exception handler
    throw new org.springframework.web.server.ResponseStatusException(
        org.springframework.http.HttpStatus.valueOf(code), errorMessage.getMessage());
  }
}
