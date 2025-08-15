package com.example.dw.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Path("/hello")
@Produces(MediaType.APPLICATION_JSON)
public class HelloWorldResource {

  @GET
  public HelloResponse sayHello() {
    return new HelloResponse("Hello, World!");
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HelloResponse {
    @JsonProperty private String message;
  }
}
