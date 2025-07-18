package com.example.dw.resources;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/test-errors")
@Produces(MediaType.APPLICATION_JSON)
public class TestErrorsResource {

    @GET
    @Path("/runtime/{message}")
    public String testRuntimeException(@PathParam("message") String message) {
        throw new RuntimeException(message);
    }

    @GET
    @Path("/web-app/{code}")
    public String testWebAppException(@PathParam("code") int code) {
        throw new WebApplicationException(
            "Web application exception with code " + code,
            Response.Status.fromStatusCode(code)
        );
    }

    @GET
    @Path("/arithmetic")
    public int testArithmeticException() {
        // Division by zero
        return 42 / 0;
    }

    @GET
    @Path("/null-pointer")
    public String testNullPointerException() {
        String nullString = null;
        return nullString.toLowerCase();
    }
}
