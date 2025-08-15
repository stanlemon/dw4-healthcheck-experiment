package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class HelloWorldResourceTest {

  private final HelloWorldResource resource = new HelloWorldResource();

  @Test
  void testSayHello() {
    HelloWorldResource.HelloResponse response = resource.sayHello();

    // Verify the response contains the expected message
    assertThat(response).isNotNull();
    assertThat(response.getMessage()).isEqualTo("Hello, World!");
  }
}
