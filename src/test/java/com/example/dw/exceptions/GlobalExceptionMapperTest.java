package com.example.dw.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.dw.metrics.DefaultMetricsService;
import com.example.dw.metrics.MetricsService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GlobalExceptionMapperTest {

  private MetricsService metricsService;
  private GlobalExceptionMapper exceptionMapper;

  @BeforeEach
  void setUp() {
    // Create a new MetricsService instance for test isolation
    metricsService = new DefaultMetricsService();
    metricsService.clearMetrics();
    exceptionMapper = new GlobalExceptionMapper(metricsService);
  }

  @Test
  void toResponse_WhenRuntimeException_ShouldReturn500AndRecordMetric() {
    // Setup
    RuntimeException exception = new RuntimeException("Test runtime exception");
    long initialErrorCount = metricsService.getErrorCountLastMinute();

    // Execute
    Response response = exceptionMapper.toResponse(exception);

    // Verify
    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

    // Verify metrics were recorded (500 error)
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(initialErrorCount + 1);

    // Get and verify response entity
    @SuppressWarnings("unchecked")
    Map<String, Object> entity = (Map<String, Object>) response.getEntity();
    assertThat(entity)
        .containsEntry("code", 500)
        .containsEntry("message", "Test runtime exception");
  }

  @Test
  void toResponse_When400BadRequest_ShouldNotRecordMetric() {
    // Setup - 400 Bad Request
    WebApplicationException exception =
        new WebApplicationException("Bad request", Response.Status.BAD_REQUEST);
    long initialErrorCount = metricsService.getErrorCountLastMinute();

    // Execute
    Response response = exceptionMapper.toResponse(exception);

    // Verify
    assertThat(response.getStatus()).isEqualTo(400);

    // Verify metrics were NOT recorded (not a 5xx error)
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(initialErrorCount);
  }

  @Test
  void toResponse_When503ServiceUnavailable_ShouldRecordMetric() {
    // Setup - 503 Service Unavailable
    WebApplicationException exception =
        new WebApplicationException("Service unavailable", Response.Status.SERVICE_UNAVAILABLE);
    long initialErrorCount = metricsService.getErrorCountLastMinute();

    // Execute
    Response response = exceptionMapper.toResponse(exception);

    // Verify
    assertThat(response.getStatus()).isEqualTo(503);

    // Verify metrics were recorded (5xx error)
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(initialErrorCount + 1);
  }

  @Test
  void toResponse_WhenExceptionHasNullMessage_ShouldUseDefaultMessage() {
    // Setup - exception with null message
    NullPointerException exception = new NullPointerException();
    long initialErrorCount = metricsService.getErrorCountLastMinute();

    // Execute
    Response response = exceptionMapper.toResponse(exception);

    // Verify
    assertThat(response.getStatus()).isEqualTo(500);

    // Verify metrics were recorded (5xx error)
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(initialErrorCount + 1);

    // Get and verify response entity uses default message
    @SuppressWarnings("unchecked")
    Map<String, Object> entity = (Map<String, Object>) response.getEntity();
    assertThat(entity).containsEntry("message", "Server Error");
  }

  @Test
  void toResponse_WhenWebApplicationExceptionHasNullResponse_ShouldReturn500AndRecordMetric() {
    // Setup - WebApplicationException with null response (covers missing branch)
    WebApplicationException exception =
        new WebApplicationException("Test exception") {
          @Override
          public Response getResponse() {
            return null; // This triggers the missing branch
          }
        };
    long initialErrorCount = metricsService.getErrorCountLastMinute();

    // Execute
    Response response = exceptionMapper.toResponse(exception);

    // Verify - should return a generic response since original response is null
    assertThat(response.getStatus()).isEqualTo(500);
    @SuppressWarnings("unchecked")
    Map<String, Object> entity = (Map<String, Object>) response.getEntity();
    assertThat(entity).containsEntry("code", 500).containsEntry("message", "Test exception");

    // Verify metrics were recorded (5xx error)
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(initialErrorCount + 1);
  }

  @Test
  void toResponse_WhenStatus599_ShouldRecordMetric() {
    // Setup - Test the upper bound of 5xx range (covers status < 600 branch)
    WebApplicationException exception =
        new WebApplicationException("Status 599", Response.status(599).build());
    long initialErrorCount = metricsService.getErrorCountLastMinute();

    // Execute
    Response response = exceptionMapper.toResponse(exception);

    // Verify
    assertThat(response.getStatus()).isEqualTo(599);

    // Verify metrics were recorded (5xx error)
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(initialErrorCount + 1);
  }

  @Test
  void toResponse_WhenStatus600_ShouldNotRecordMetric() {
    // Setup - Test outside 5xx range (status >= 600, should NOT record error)
    WebApplicationException exception =
        new WebApplicationException("Status 600", Response.status(600).build());
    long initialErrorCount = metricsService.getErrorCountLastMinute();

    // Execute
    Response response = exceptionMapper.toResponse(exception);

    // Verify
    assertThat(response.getStatus()).isEqualTo(600);

    // Verify metrics were NOT recorded (not in 5xx range)
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(initialErrorCount);
  }
}
