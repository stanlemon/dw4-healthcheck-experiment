package com.stanlemon.healthy.spring4app.exceptions;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.validation.ConstraintViolationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
    // HttpStatusCode accepts arbitrary integers; HttpStatus.valueOf throws for non-enum codes.
    HttpStatusCode status = HttpStatusCode.valueOf(exception.getStatusCode().value());

    String message;
    if (status.is5xxServerError()) {
      log.error("Status exception", exception);
      metricsService.recordServerError();
      message = "Internal server error";
    } else {
      // 4xx: the reason string is programmer-controlled (e.g. "Plane not found"), safe to echo.
      message = exception.getReason() != null ? exception.getReason() : "Not found";
    }

    return new ResponseEntity<>(new ErrorResponse(status.value(), message), status);
  }

  /**
   * Handles Bean Validation failures from @Validated method parameters (e.g. path variables).
   * Spring does not map ConstraintViolationException to an HTTP status automatically, so without
   * this handler it would fall through to a 500.
   *
   * @param exception the ConstraintViolationException to handle
   * @return 400 Bad Request with error details
   */
  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ErrorResponse> handleConstraintViolation(
      ConstraintViolationException exception) {
    // Bean Validation messages can include field names, parameter paths, and occasionally the
    // user-supplied value, so return a stable generic message to clients. Detail is in the log.
    log.debug("Constraint violation", exception);
    return new ResponseEntity<>(
        new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed"),
        HttpStatus.BAD_REQUEST);
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
    HttpStatusCode status =
        (exception instanceof org.springframework.web.ErrorResponse springError)
            ? HttpStatusCode.valueOf(springError.getStatusCode().value())
            : HttpStatus.INTERNAL_SERVER_ERROR;

    String message;
    if (status.is5xxServerError()) {
      log.error("Unhandled exception", exception);
      metricsService.recordServerError();
      // Do not echo raw exception messages on 5xx — they may leak SQL, file paths, or hostnames.
      // The detail is in the log; the client only needs to know that the server failed.
      message = "Internal server error";
    } else {
      // 4xx messages from Spring framework exceptions describe the request problem, safe to echo.
      message = exception.getMessage() != null ? exception.getMessage() : "Bad request";
    }

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
