package com.stanlemon.healthy.dw5app.exceptions;

import io.dropwizard.jersey.validation.JerseyViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

/**
 * Maps Bean Validation failures to HTTP 400. Handles {@link JerseyViolationException} (the
 * Dropwizard-specific subtype) so this mapper wins the JAX-RS most-specific-type selection over
 * Dropwizard's built-in {@code JerseyViolationExceptionMapper} (which returns 422). This aligns
 * Dropwizard and Spring on a single validation-error status code.
 *
 * <p>Returns a stable generic "Validation failed" message to the client. Bean Validation messages
 * embed property paths (method/parameter names) and sometimes the violating value — all of which
 * are internal API details. Detail stays in the debug log.
 */
@Provider
@Slf4j
public class ConstraintViolationExceptionMapper
    implements ExceptionMapper<JerseyViolationException> {

  @Override
  public Response toResponse(JerseyViolationException exception) {
    log.debug("Constraint violation", exception);
    return Response.status(Response.Status.BAD_REQUEST)
        .type(MediaType.APPLICATION_JSON_TYPE)
        .entity(Map.of("code", 400, "message", "Validation failed"))
        .build();
  }
}
