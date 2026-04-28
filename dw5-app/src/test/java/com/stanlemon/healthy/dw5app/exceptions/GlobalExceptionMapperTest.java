package com.stanlemon.healthy.dw5app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsService;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Global Exception Mapper Tests")
class GlobalExceptionMapperTest {

  private MetricsService metricsService;
  private GlobalExceptionMapper exceptionMapper;

  @BeforeEach
  void setUp() {
    metricsService = new DefaultMetricsService();
    exceptionMapper = new GlobalExceptionMapper(metricsService);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> entityMap(Response response) {
    assertThat(response.getEntity()).isInstanceOf(Map.class);
    return (Map<String, Object>) response.getEntity();
  }

  @Test
  void toResponse_WhenRuntimeException_ShouldReturn500AndRecordMetric() {
    SomethingWentWrongException exception =
        new SomethingWentWrongException("Test runtime exception");

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);

    assertThat(entityMap(response))
        .containsEntry("code", 500)
        .containsEntry("message", "Internal server error");
  }

  @Test
  void toResponse_When400BadRequest_ShouldNotRecordMetric() {
    WebApplicationException exception =
        new WebApplicationException("Bad request", Response.Status.BAD_REQUEST);

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(metricsService.getErrorCountLastMinute()).isZero();
  }

  @Test
  void toResponse_When503ServiceUnavailable_ShouldRecordMetric() {
    WebApplicationException exception =
        new WebApplicationException("Service unavailable", Response.Status.SERVICE_UNAVAILABLE);

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
  }

  @Test
  void toResponse_WhenExceptionHasNullMessage_ShouldUseDefaultMessage() {
    NullPointerException exception = new NullPointerException();

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
    assertThat(entityMap(response)).containsEntry("message", "Internal server error");
  }

  @Test
  void toResponse_WhenWebApplicationExceptionHasNullResponse_ShouldReturn500AndRecordMetric() {
    WebApplicationException exception =
        new WebApplicationException("Test exception") {
          @Override
          public Response getResponse() {
            return null;
          }
        };

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(entityMap(response))
        .containsEntry("code", 500)
        .containsEntry("message", "Internal server error");
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
  }

  @Test
  void toResponse_WhenStatus599_ShouldRecordMetric() {
    WebApplicationException exception =
        new WebApplicationException("Status 599", Response.status(599).build());

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(599);
    assertThat(metricsService.getErrorCountLastMinute()).isEqualTo(1);
  }

  @Test
  void toResponse_WhenStatus600_ShouldNotRecordMetric() {
    WebApplicationException exception =
        new WebApplicationException("Status 600", Response.status(600).build());

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(600);
    assertThat(metricsService.getErrorCountLastMinute()).isZero();
  }

  @Test
  @DisplayName(
      "Jakarta NotFoundException flows through the mapper as 404 without recording a 5xx metric")
  void toResponse_WhenNotFoundException_ShouldReturn404AndNotRecordMetric() {
    Response response = exceptionMapper.toResponse(new NotFoundException());

    assertThat(response.getStatus()).isEqualTo(404);
    assertThat(metricsService.getErrorCountLastMinute()).isZero();
  }
}
