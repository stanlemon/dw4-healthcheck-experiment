package com.example.dw.exceptions;

public class SomethingWentWrongException extends RuntimeException {

  public SomethingWentWrongException(String message) {
    super(message);
  }

  public SomethingWentWrongException(String message, Throwable cause) {
    super(message, cause);
  }
}
