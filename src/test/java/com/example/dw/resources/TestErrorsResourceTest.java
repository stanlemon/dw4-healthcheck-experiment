package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

class TestErrorsResourceTest {

  private final TestErrorsResource resource = new TestErrorsResource();

  @Test
  void testRuntimeException() {
    String errorMessage = "Test error message";
    assertThatThrownBy(() -> resource.testRuntimeException(errorMessage))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining(errorMessage);
  }

  @Test
  void testRuntimeExceptionWithDifferentMessages() {
    // Test with different message values to ensure parameter handling works correctly
    String[] testMessages = {"error1", "test message with spaces", "special@#$chars", ""};

    for (String message : testMessages) {
      assertThatThrownBy(() -> resource.testRuntimeException(message))
          .isInstanceOf(RuntimeException.class)
          .hasMessage(message);
    }
  }

  @Test
  void testWebAppException() {
    int errorCode = 400;
    assertThatThrownBy(() -> resource.testWebAppException(errorCode))
        .isInstanceOf(WebApplicationException.class)
        .hasMessageContaining("Web application exception with code " + errorCode);
  }

  @Test
  void testWebAppExceptionWithDifferentStatusCodes() {
    // Test various HTTP status codes to ensure proper handling
    int[] statusCodes = {400, 401, 403, 404, 500, 502, 503};

    for (int code : statusCodes) {
      assertThatThrownBy(() -> resource.testWebAppException(code))
          .isInstanceOf(WebApplicationException.class)
          .hasMessageContaining("Web application exception with code " + code)
          .satisfies(
              exception -> {
                WebApplicationException webAppEx = (WebApplicationException) exception;
                assertThat(webAppEx.getResponse().getStatus()).isEqualTo(code);
              });
    }
  }

  @Test
  void testWebAppExceptionResponseStatus() {
    // Test that the response status is correctly set
    int testCode = 404; // Not Found - a standard status code

    try {
      resource.testWebAppException(testCode);
    } catch (WebApplicationException e) {
      Response response = e.getResponse();
      assertThat(response.getStatus()).isEqualTo(testCode);
      assertThat(response.getStatusInfo()).isEqualTo(Response.Status.fromStatusCode(testCode));
    }
  }

  @Test
  void testAllMethodsThrowExceptions() {
    // Comprehensive test to ensure all methods throw exceptions as designed
    assertThatThrownBy(() -> resource.testRuntimeException("test")).isInstanceOf(Exception.class);

    assertThatThrownBy(() -> resource.testWebAppException(500)).isInstanceOf(Exception.class);
  }
}
