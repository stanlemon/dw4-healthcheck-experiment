package com.stanlemon.healthy.spring4app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.server.ResponseStatusException;

@DisplayName("Global Exception Handler Tests")
class GlobalExceptionHandlerTest {

  private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

  @Test
  @DisplayName("Should preserve 4xx status from Spring framework exceptions")
  void handleException_WhenSpringFramework4xx_ShouldPreserveStatus() throws Exception {
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("DELETE");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.METHOD_NOT_ALLOWED);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(405);
  }

  @Test
  @DisplayName("Should hide the reason for a 5xx ResponseStatusException")
  void handleResponseStatusException_When5xx_ShouldHideReason() {
    ResponseStatusException exception =
        new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Service down");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleResponseStatusException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(503);
    assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
  }

  @Test
  @DisplayName("Should echo the reason for a 4xx ResponseStatusException")
  void handleResponseStatusException_When4xx_ShouldEchoReason() {
    ResponseStatusException exception =
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Not found");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleResponseStatusException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getCode()).isEqualTo(404);
    assertThat(response.getBody().getMessage()).isEqualTo("Not found");
  }

  @Test
  @DisplayName("Should fall back to 'Not found' when ResponseStatusException reason is null")
  void handleResponseStatusException_WhenReasonNull_ShouldUseFallbackMessage() {
    ResponseStatusException exception = new ResponseStatusException(HttpStatus.NOT_FOUND);

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleResponseStatusException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Not found");
  }

  @Test
  @DisplayName("Should handle generic exceptions as 500 errors")
  void handleException_WhenGenericException_ShouldReturn500() {
    Exception exception = new SomethingWentWrongException("Test exception");

    ResponseEntity<GlobalExceptionHandler.ErrorResponse> response =
        exceptionHandler.handleException(exception);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getMessage()).isEqualTo("Internal server error");
    assertThat(response.getBody().getCode()).isEqualTo(500);
  }
}
