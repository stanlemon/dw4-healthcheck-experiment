package com.stanlemon.healthy.dw5app.exceptions;

import io.dropwizard.jersey.validation.JerseyViolationException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Maps Bean Validation failures to HTTP 400. Handles {@link JerseyViolationException} (the
 * Dropwizard-specific subtype) so this mapper wins the JAX-RS most-specific-type selection over
 * Dropwizard's built-in {@code JerseyViolationExceptionMapper} (which returns 422). This aligns
 * Dropwizard and Spring on a single validation-error status code.
 */
@Provider
public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<JerseyViolationException> {

  @Override
  public Response toResponse(JerseyViolationException exception) {
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(Map.of("code", 400, "message", summarize(exception)))
        .build();
  }

  private static String summarize(ConstraintViolationException exception) {
    return exception.getConstraintViolations().stream()
        .map(ConstraintViolationExceptionMapper::format)
        .collect(Collectors.joining("; "));
  }

  private static String format(ConstraintViolation<?> violation) {
    return violation.getPropertyPath() + " " + violation.getMessage();
  }
}
