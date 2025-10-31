package com.stanlemon.healthy.spring3app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.stanlemon.healthy.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("Global Exception Handler Tests")
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

  @Mock private MetricsService metricsService;

  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    exceptionHandler = new GlobalExceptionHandler(metricsService);
  }

  @Test
  @DisplayName("Should handle generic exceptions as 500 errors")
  void handleException_WhenGenericException_ShouldReturn500() {
    // Setup test data
    Exception exception = new RuntimeException("Test exception");

    // Call the method under test
    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleException(exception);

    // Verify the response
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).contains("Test exception");
    assertThat(response.getBody().getCode()).isEqualTo(500);
    assertThat(response.getBody().getTimestamp()).isGreaterThan(0L);

    // Verify that the exception was recorded in metrics
    verify(metricsService, times(1)).recordServerError();
  }

  @Test
  @DisplayName("ErrorResponse should contain correct information")
  void errorResponse_ShouldContainCorrectInfo() {
    // Create a test error response
    GlobalExceptionHandler.ErrorResponse response =
        new GlobalExceptionHandler.ErrorResponse("Test error", 500, 123456789L);

    // Verify the response fields
    assertThat(response.getMessage()).isEqualTo("Test error");
    assertThat(response.getCode()).isEqualTo(500);
    assertThat(response.getTimestamp()).isEqualTo(123456789L);
  }
}
