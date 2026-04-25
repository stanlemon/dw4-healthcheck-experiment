package com.stanlemon.healthy.dw4app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Test Errors Resource Tests")
class TestErrorsResourceTest {

  private final TestErrorsResource resource = new TestErrorsResource();

  @Test
  @DisplayName(
      "triggerError always throws SomethingWentWrongException with deliberate error message")
  void triggerError_WhenCalled_ShouldThrowSomethingWentWrongException() {
    assertThatThrownBy(() -> resource.triggerError())
        .isInstanceOf(SomethingWentWrongException.class)
        .hasMessageContaining("deliberate error");
  }

  @Test
  void runtimeException_WhenCalled_ShouldThrowSomethingWentWrongExceptionWithProvidedMessage() {
    String errorMessage = "Test error message";
    assertThatThrownBy(() -> resource.testRuntimeException(errorMessage))
        .isInstanceOf(SomethingWentWrongException.class)
        .hasMessageContaining(errorMessage);
  }

  @ParameterizedTest
  @ValueSource(strings = {"error1", "test message with spaces", "special@#$chars", ""})
  void runtimeException_WhenCalledWithVariousMessages_ShouldPreserveMessageContent(String message) {
    assertThatThrownBy(() -> resource.testRuntimeException(message))
        .isInstanceOf(SomethingWentWrongException.class)
        .hasMessage(message);
  }

  @Test
  void webAppException_WhenCalled_ShouldThrowWebApplicationExceptionWithProvidedCode() {
    int errorCode = 400;
    assertThatThrownBy(() -> resource.testWebAppException(errorCode))
        .isInstanceOf(WebApplicationException.class)
        .hasMessageContaining("Web application exception with code " + errorCode);
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 404, 500, 502, 503})
  void webAppException_WhenCalledWithVariousStatusCodes_ShouldUseCorrectStatusCode(int code) {
    assertThatThrownBy(() -> resource.testWebAppException(code))
        .isInstanceOf(WebApplicationException.class)
        .hasMessageContaining("Web application exception with code " + code)
        .satisfies(
            exception -> {
              WebApplicationException webAppEx = (WebApplicationException) exception;
              assertThat(webAppEx.getResponse().getStatus()).isEqualTo(code);
            });
  }

  @Test
  void webAppException_WhenCalledWithSpecificCode_ShouldSetCorrectResponseStatus() {
    int testCode = 404;

    assertThatThrownBy(() -> resource.testWebAppException(testCode))
        .isInstanceOf(WebApplicationException.class)
        .satisfies(
            exception -> {
              WebApplicationException webAppEx = (WebApplicationException) exception;
              assertThat(webAppEx.getResponse().getStatus()).isEqualTo(testCode);
              assertThat(webAppEx.getResponse().getStatusInfo())
                  .isEqualTo(Response.Status.fromStatusCode(testCode));
            });
  }
}
