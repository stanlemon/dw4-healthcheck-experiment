package com.stanlemon.healthy.spring3app.resources;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Simple REST resource that provides a hello world endpoint for testing connectivity. */
@RestController
@RequestMapping("/hello")
public class HelloWorldResource {

  /**
   * Returns a simple greeting message.
   *
   * @return hello world response
   */
  @GetMapping
  public HelloResponse sayHello() {
    return new HelloResponse("Hello, World!");
  }

  /** Response object containing a greeting message. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class HelloResponse {
    @JsonProperty private String message;
  }
}
