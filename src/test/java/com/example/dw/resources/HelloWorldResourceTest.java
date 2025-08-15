package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Hello World Resource Tests")
class HelloWorldResourceTest {

  private final HelloWorldResource resource = new HelloWorldResource();

  @Test
  @DisplayName("Should return 'Hello, World!' message when sayHello is called")
  void sayHello_WhenCalled_ShouldReturnHelloWorldMessage() {
    HelloWorldResource.HelloResponse response = resource.sayHello();

    // Verify the response contains the expected message
    assertThat(response).isNotNull();
    assertThat(response.getMessage()).isEqualTo("Hello, World!");
  }
}
