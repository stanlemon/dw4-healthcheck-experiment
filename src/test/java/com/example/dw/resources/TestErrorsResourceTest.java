package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import jakarta.ws.rs.WebApplicationException;

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
    public void testWebAppException() {
        int errorCode = 400;
        assertThatThrownBy(() -> resource.testWebAppException(errorCode))
            .isInstanceOf(WebApplicationException.class)
            .hasMessageContaining("Web application exception with code " + errorCode);
    }

    @Test
    public void testArithmeticException() {
        assertThatThrownBy(() -> resource.testArithmeticException())
            .isInstanceOf(ArithmeticException.class);
    }

    @Test
    public void testNullPointerException() {
        assertThatThrownBy(() -> resource.testNullPointerException())
            .isInstanceOf(NullPointerException.class);
    }
}
