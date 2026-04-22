package com.stanlemon.healthy.dw4app.resources;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * REST resource for testing different types of exceptions and their handling by the global
 * exception mapper.
 */
@Path("/test-errors")
@Produces(MediaType.APPLICATION_JSON)
public class TestErrorsResource {

  /**
   * Deliberately triggers a server error to test exception handling and error metrics.
   *
   * @return never returns normally, always throws an exception
   * @throws SomethingWentWrongException always thrown to simulate an error condition
   */
  @GET
  @Path("/trigger")
  public String triggerError() {
    throw new SomethingWentWrongException(
        "This is a deliberate error that will be caught by our global handler");
  }

  /**
   * Tests runtime exception handling with a custom message.
   *
   * @param message the error message to include in the exception
   * @return never returns normally, always throws an exception
   * @throws SomethingWentWrongException always thrown with the provided message
   */
  @GET
  @Path("/runtime/{message}")
  public String testRuntimeException(@PathParam("message") String message) {
    throw new SomethingWentWrongException(message);
  }

  /**
   * Tests WebApplicationException handling with a specific HTTP status code.
   *
   * @param code the HTTP status code to use in the exception
   * @return never returns normally, always throws an exception
   * @throws WebApplicationException always thrown with the specified status code
   */
  @GET
  @Path("/web-app/{code}")
  public String testWebAppException(@PathParam("code") int code) {
    String errorMessage = "Web application exception with code " + code;
    throw new WebApplicationException(
        errorMessage,
        Response.status(code).entity(errorMessage).type(MediaType.TEXT_PLAIN).build());
  }
}
