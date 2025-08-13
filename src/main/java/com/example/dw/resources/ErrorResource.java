package com.example.dw.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/error")
@Produces(MediaType.APPLICATION_JSON)
public class ErrorResource {

  @GET
  public Response triggerError() {
    // We don't need to manually record the error anymore
    // as the GlobalExceptionMapper will handle that

    // Deliberately throw a runtime exception that will be caught by our mapper
    throw new RuntimeException(
        "This is a deliberate error that will be caught by our global handler");
  }
}
