package com.stanlemon.healthy.spring4app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("Test Errors Resource Tests")
class TestErrorsResourceTest {

  private final TestErrorsResource resource = new TestErrorsResource();

  @Test
  @DisplayName("Should throw SomethingWentWrongException when trigger is called")
  void triggerError_WhenCalled_ShouldThrowSomethingWentWrongException() {
    assertThatThrownBy(() -> resource.triggerError())
        .isInstanceOf(SomethingWentWrongException.class)
        .hasMessageContaining("deliberate error");
  }

  @Test
  @DisplayName("Should throw SomethingWentWrongException with provided message")
  void testRuntimeException_WhenCalled_ShouldThrowWithMessage() {
    assertThatThrownBy(() -> resource.testRuntimeException("custom error"))
        .isInstanceOf(SomethingWentWrongException.class)
        .hasMessage("custom error");
  }

  @ParameterizedTest
  @ValueSource(strings = {"error1", "test message with spaces", "special@#$chars", ""})
  @DisplayName("Should preserve message content for various inputs")
  void runtimeException_WhenCalledWithVariousMessages_ShouldPreserveMessageContent(String message) {
    assertThatThrownBy(() -> resource.testRuntimeException(message))
        .isInstanceOf(SomethingWentWrongException.class)
        .hasMessage(message);
  }

  @Test
  @DisplayName("Should throw ResponseStatusException with provided status code")
  void testWebAppException_WhenCalled_ShouldThrowWithStatusCode() {
    assertThatThrownBy(() -> resource.testWebAppException(404))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Web application exception with code 404");
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
  @DisplayName("Should use correct status code for various HTTP status codes")
  void webAppException_WhenCalledWithVariousStatusCodes_ShouldUseCorrectStatusCode(int code) {
    assertThatThrownBy(() -> resource.testWebAppException(code))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Web application exception with code " + code)
        .satisfies(
            exception -> {
              ResponseStatusException rse = (ResponseStatusException) exception;
              assertThat(rse.getStatusCode().value()).isEqualTo(code);
            });
  }
}
