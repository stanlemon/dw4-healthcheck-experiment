package com.stanlemon.healthy.spring3app.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Custom exception indicating that something unexpected went wrong.
 *
 * <p>This exception is mapped to a 500 Internal Server Error response and is used for testing and
 * demonstration purposes.
 */
@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class SomethingWentWrongException extends RuntimeException {

  /**
   * Constructs a new exception with the specified detail message.
   *
   * @param message the detail message
   */
  public SomethingWentWrongException(String message) {
    super(message);
  }

  /**
   * Constructs a new exception with the specified detail message and cause.
   *
   * @param message the detail message
   * @param cause the cause of this exception
   */
  public SomethingWentWrongException(String message, Throwable cause) {
    super(message, cause);
  }
}
