package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Error Resource Tests")
class ErrorResourceTest {

  private final ErrorResource resource = new ErrorResource();

  @Test
  @DisplayName("Should throw RuntimeException with expected message when triggerError is called")
  void triggerError_WhenCalled_ShouldThrowRuntimeExceptionWithExpectedMessage() {
    // Test that calling triggerError() throws a RuntimeException with the expected message
    assertThatThrownBy(resource::triggerError)
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining(
            "This is a deliberate error that will be caught by our global handler");
  }
}
