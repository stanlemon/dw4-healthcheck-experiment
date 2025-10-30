package com.stanlemon.healthy.dw4app.exceptions;

/** Custom runtime exception used for testing error handling and metrics tracking. */
public class SomethingWentWrongException extends RuntimeException {

  /**
   * Creates a new exception with the specified message.
   *
   * @param message the error message
   */
  public SomethingWentWrongException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the specified message and cause.
   *
   * @param message the error message
   * @param cause the underlying cause
   */
  public SomethingWentWrongException(String message, Throwable cause) {
    super(message, cause);
  }
}
