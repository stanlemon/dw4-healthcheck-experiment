package com.stanlemon.healthy.dw4app.resources;

import com.stanlemon.healthy.dw4app.exceptions.SomethingWentWrongException;
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
    throw new WebApplicationException(
        "Web application exception with code " + code, Response.Status.fromStatusCode(code));
  }
}
