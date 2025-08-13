package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

public class TestErrorsResourceTest {

    private final TestErrorsResource resource = new TestErrorsResource();

    @Test
    public void testRuntimeException() {
        String errorMessage = "Test error message";
        assertThatThrownBy(() -> resource.testRuntimeException(errorMessage))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining(errorMessage);
    }

    @Test
    public void testRuntimeExceptionWithDifferentMessages() {
        // Test with different message values to ensure parameter handling works correctly
        String[] testMessages = {"error1", "test message with spaces", "special@#$chars", ""};

        for (String message : testMessages) {
            assertThatThrownBy(() -> resource.testRuntimeException(message))
                .isInstanceOf(RuntimeException.class)
                .hasMessage(message);
        }
    }

    @Test
    public void testWebAppException() {
        int errorCode = 400;
        assertThatThrownBy(() -> resource.testWebAppException(errorCode))
            .isInstanceOf(WebApplicationException.class)
            .hasMessageContaining("Web application exception with code " + errorCode);
    }

    @Test
    public void testWebAppExceptionWithDifferentStatusCodes() {
        // Test various HTTP status codes to ensure proper handling
        int[] statusCodes = {400, 401, 403, 404, 500, 502, 503};

        for (int code : statusCodes) {
            assertThatThrownBy(() -> resource.testWebAppException(code))
                .isInstanceOf(WebApplicationException.class)
                .hasMessageContaining("Web application exception with code " + code)
                .satisfies(exception -> {
                    WebApplicationException webAppEx = (WebApplicationException) exception;
                    assertThat(webAppEx.getResponse().getStatus()).isEqualTo(code);
                });
        }
    }

    @Test
    public void testWebAppExceptionResponseStatus() {
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
    public void testArithmeticExceptionDetails() {
        // Test that the arithmetic exception behaves as expected
        assertThatThrownBy(() -> resource.testArithmeticException())
            .isInstanceOf(ArithmeticException.class)
            .satisfies(exception -> {
                // Verify it's specifically a division by zero error
                assertThat(exception.getMessage()).contains("/ by zero");
            });
    }

    @Test
    public void testNullPointerExceptionDetails() {
        // Test that the null pointer exception occurs as expected
        assertThatThrownBy(() -> resource.testNullPointerException())
            .isInstanceOf(NullPointerException.class)
            .satisfies(exception -> {
                // Just verify it's a NullPointerException - message may vary by JVM
                assertThat(exception).isInstanceOf(NullPointerException.class);
            });
    }

    @Test
    public void testAllMethodsThrowExceptions() {
        // Comprehensive test to ensure all methods throw exceptions as designed
        assertThatThrownBy(() -> resource.testRuntimeException("test"))
            .isInstanceOf(Exception.class);

        assertThatThrownBy(() -> resource.testWebAppException(500))
            .isInstanceOf(Exception.class);

        assertThatThrownBy(() -> resource.testArithmeticException())
            .isInstanceOf(Exception.class);

        assertThatThrownBy(() -> resource.testNullPointerException())
            .isInstanceOf(Exception.class);
    }
}
