package com.stanlemon.healthy.spring3app.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stanlemon.healthy.metrics.MetricsService;
import java.time.Instant;
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

    // Get the status code
    int statusCode = exception.getStatusCode().value();
    HttpStatus status = HttpStatus.valueOf(statusCode);

    // Only record server errors (5xx) in metrics
    if (status.is5xxServerError()) {
      metricsService.recordServerError();
    }

    ErrorResponse errorResponse =
        new ErrorResponse(exception.getMessage(), status.value(), Instant.now().toEpochMilli());

    return new ResponseEntity<>(errorResponse, status);
  }

  /**
   * Handles all exceptions that don't have more specific handlers, treating them as server errors.
   *
   * @param exception the exception to handle
   * @return response entity with error details and 500 status
   */
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception exception) {
    log.error("Unhandled exception", exception);

    // Record server error in metrics
    metricsService.recordServerError();

    ErrorResponse errorResponse =
        new ErrorResponse(
            "Internal Server Error: " + exception.getMessage(),
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            Instant.now().toEpochMilli());

    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  /** Structured error response providing consistent error information. */
  @Data
  @AllArgsConstructor
  public static class ErrorResponse {
    @JsonProperty private String message;
    @JsonProperty private int code;
    @JsonProperty private long timestamp;
  }
}
