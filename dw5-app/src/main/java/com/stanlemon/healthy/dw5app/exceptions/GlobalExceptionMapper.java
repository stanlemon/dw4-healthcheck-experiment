package com.stanlemon.healthy.dw5app.exceptions;

import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Global exception mapper that catches all exceptions and tracks 500 errors for health monitoring.
 */
@Provider
@Slf4j
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
    int status =
        (exception instanceof WebApplicationException webAppException
                && webAppException.getResponse() != null)
            ? webAppException.getResponse().getStatus()
            : Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();

    String message;
    if (status >= 500 && status < 600) {
      log.error("Unhandled exception", exception);
      metricsService.recordServerError();
      // Do not echo raw exception messages on 5xx — they may leak SQL, file paths, or hostnames.
      // The detail is in the log; the client only needs to know that the server failed.
      message = "Internal server error";
    } else {
      // 4xx messages come from WebApplicationException("Plane not found") or similar — explicit,
      // programmer-controlled, safe to return.
      message = exception.getMessage() != null ? exception.getMessage() : "Bad request";
    }

    return Response.status(status)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(Map.of("code", status, "message", message))
        .build();
  }
}
