package com.example.dw.resources;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/hello")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {

    @GET
    public HelloResponse sayHello() {
        return new HelloResponse("Hello, World!");
    }

    public static class HelloResponse {
        private String message;

        public HelloResponse() {
            // Jackson deserialization
        }

        public HelloResponse(String message) {
            this.message = message;
        }

        @JsonProperty
        public String getMessage() {
            return message;
        }
    }
}
