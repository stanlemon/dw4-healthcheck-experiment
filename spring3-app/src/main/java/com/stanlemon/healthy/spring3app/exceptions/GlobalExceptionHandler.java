package com.stanlemon.healthy.spring3app.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stanlemon.healthy.metrics.MetricsService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler that provides consistent error responses and tracks error metrics.
 *
 * <p>This class intercepts exceptions thrown by controllers and maps them to appropriate HTTP
 * responses with consistent error message formatting. It also records server errors in the metrics
 * service for monitoring.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

  private final MetricsService metricsService;

  /**
   * Constructs a new GlobalExceptionHandler with the provided metrics service.
   *
   * @param metricsService the metrics service to record error events
   */
  public GlobalExceptionHandler(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /**
   * Handles ResponseStatusException to preserve status codes from web exceptions.
   *
   * @param exception the ResponseStatusException to handle
   * @return response entity with error details and the original status
   */
  @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
  public ResponseEntity<ErrorResponse> handleResponseStatusException(
      org.springframework.web.server.ResponseStatusException exception) {
    log.error("Status exception", exception);

    int statusCode = exception.getStatusCode().value();
    HttpStatus status = HttpStatus.valueOf(statusCode);

    if (status.is5xxServerError()) {
      metricsService.recordServerError();
    }

    ErrorResponse errorResponse =
        new ErrorResponse(
            statusCode, exception.getReason() != null ? exception.getReason() : "Server Error");

    return new ResponseEntity<>(errorResponse, status);
  }

  /**
   * Handles all exceptions that don't have more specific handlers. Preserves the status code from
   * Spring framework exceptions (e.g. validation errors, method not allowed) and only records
   * server error metrics for 5xx responses.
   *
   * @param exception the exception to handle
   * @return response entity with error details and appropriate status
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    HttpStatus status;

    if (exception instanceof org.springframework.web.ErrorResponse springError) {
      status = HttpStatus.valueOf(springError.getStatusCode().value());
    } else {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    if (status.is5xxServerError()) {
      log.error("Unhandled exception", exception);
      metricsService.recordServerError();
    } else {
      log.warn("Client error - status: {}", status.value(), exception);
    }

    String message = exception.getMessage() != null ? exception.getMessage() : "Server Error";

    return new ResponseEntity<>(new ErrorResponse(status.value(), message), status);
  }

  /** Structured error response providing consistent error information. */
  @Data
  @AllArgsConstructor
  public static class ErrorResponse {
    @JsonProperty private int code;
    @JsonProperty private String message;
  }
}
