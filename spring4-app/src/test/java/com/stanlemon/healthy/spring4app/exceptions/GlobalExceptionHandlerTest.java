package com.stanlemon.healthy.spring4app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

  private MetricsService metricsService;
  private GlobalExceptionHandler exceptionHandler;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    exceptionHandler = new GlobalExceptionHandler(metricsService);
  }

  @Test
  @DisplayName(
      "Should preserve 4xx status from Spring framework exceptions and not record server error metric")
  void handleException_WhenSpringFramework4xx_ShouldPreserveStatusAndNotRecordMetric()
      throws Exception {
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("DELETE");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(405);
    assertThat(metricsService.getErrorCountLastMinute()).isZero();
  }

  @Test
  @DisplayName("Should record metric for 5xx ResponseStatusException")
  void handleResponseStatusException_When5xx_ShouldRecordMetric() {
    ResponseStatusException exception =
        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service down");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleResponseStatusException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(503);
    assertThat(response.getBody().getMessage()).isEqualTo("Service down");
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should not record metric for 4xx ResponseStatusException")
  void handleResponseStatusException_When4xx_ShouldNotRecordMetric() {
    ResponseStatusException exception =
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleResponseStatusException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(404);
    assertThat(metricsService.getErrorCountLastMinute()).isZero();
  }

  @Test
  @DisplayName("Should fall back to 'Server Error' when ResponseStatusException reason is null")
  void handleResponseStatusException_WhenReasonNull_ShouldUseFallbackMessage() {
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND);

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleResponseStatusException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Server Error");
  }

  @Test
  @DisplayName("Should record metric for 502 Bad Gateway ResponseStatusException")
  void handleResponseStatusException_When502_ShouldRecordMetric() {
    ResponseStatusException exception =
        new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Bad gateway");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleResponseStatusException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(502);
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should handle generic exceptions as 500 errors")
  void handleException_WhenGenericException_ShouldReturn500() {
    Exception exception = new SomethingWentWrongException("Test exception");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Test exception");
    assertThat(response.getBody().getCode()).isEqualTo(500);
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
  }

  @Test
  @DisplayName("Should use fallback message when exception message is null")
  void handleException_WhenNullMessage_ShouldUseFallback() {
    Exception exception = new SomethingWentWrongException(null);

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Server Error");
    assertThat(response.getBody().getCode()).isEqualTo(500);
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
  }

  @Test
  @DisplayName("ErrorResponse should contain correct information")
  void errorResponse_ShouldContainCorrectInfo() {
    GlobalExceptionHandler.ErrorResponse response =
        new GlobalExceptionHandler.ErrorResponse(500, "Test error");

    assertThat(response.getMessage()).isEqualTo("Test error");
    assertThat(response.getCode()).isEqualTo(500);
  }
}
