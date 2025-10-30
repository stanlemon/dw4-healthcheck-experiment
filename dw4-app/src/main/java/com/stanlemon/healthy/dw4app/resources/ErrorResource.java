package com.stanlemon.healthy.dw4app.resources;

import com.stanlemon.healthy.dw4app.exceptions.SomethingWentWrongException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for testing error handling and metrics tracking by deliberately throwing
 * exceptions.
 */
@Path("/error")
@Produces(MediaType.APPLICATION_JSON)
public class ErrorResource {

  /**
   * Deliberately triggers a server error to test exception handling and error metrics.
   *
   * @return never returns normally, always throws an exception
   * @throws SomethingWentWrongException always thrown to simulate an error condition
   */
  @GET
  public Response triggerError() {
    // We don't need to manually record the error anymore
    // as the GlobalExceptionMapper will handle that

    // Deliberately throw a runtime exception that will be caught by our mapper
    throw new SomethingWentWrongException(
        "This is a deliberate error that will be caught by our global handler");
  }
}
