package com.example.dw.exceptions;

import com.example.dw.metrics.MetricsService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Global exception mapper that catches all exceptions and tracks 500 errors for health monitoring.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

  private final MetricsService metricsService;

  /**
   * Constructs a new GlobalExceptionMapper with the provided metrics service.
   *
   * @param metricsService the metrics service to use for error tracking
   */
  public GlobalExceptionMapper(MetricsService metricsService) {
    this.metricsService = metricsService;
  }

  /**
   * Converts any thrown exception into an appropriate HTTP response and tracks server errors.
   *
   * @param exception the exception to handle
   * @return HTTP response with appropriate status code and error message
   */
  @Override
  public Response toResponse(Throwable exception) {
    // First, determine the status code
    int status = determineStatusCode(exception);

    // Track all 5xx errors
    if (status >= 500 && status < 600) {
      metricsService.recordServerError();
    }

    // If it's a WebApplicationException, use its response if available
    if (exception instanceof WebApplicationException webAppException
        && webAppException.getResponse() != null) {
      return webAppException.getResponse();
    }

    // Otherwise, build a generic error response
    return Response.status(status)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(
            Map.of(
                "code",
                status,
                "message",
                exception.getMessage() != null ? exception.getMessage() : "Server Error"))
        .build();
  }

  /**
   * Determines the appropriate HTTP status code for the given exception.
   *
   * @param exception the exception to analyze
   * @return HTTP status code (WebApplicationException status or 500)
   */
  private int determineStatusCode(Throwable exception) {
    // If it's a WebApplicationException, use its status
    if (exception instanceof WebApplicationException webAppException
        && webAppException.getResponse() != null) {
      return webAppException.getResponse().getStatus();
    }

    // By default, return 500 for all other exceptions
    return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
  }
}
