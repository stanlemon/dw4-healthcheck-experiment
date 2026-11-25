package com.stanlemon.healthy.dw5app.exceptions;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.exceptions.SomethingWentWrongException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Global Exception Mapper Tests")
class GlobalExceptionMapperTest {

  private final GlobalExceptionMapper exceptionMapper = new GlobalExceptionMapper();

  @SuppressWarnings("unchecked")
  private Map<String, Object> entityMap(Response response) {
    assertThat(response.getEntity()).isInstanceOf(Map.class);
    return (Map<String, Object>) response.getEntity();
  }

  @Test
  void toResponse_WhenRuntimeException_ShouldReturnGeneric500() {
    SomethingWentWrongException exception =
        new SomethingWentWrongException("Test runtime exception");

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

    assertThat(entityMap(response))
        .containsEntry("code", 500)
        .containsEntry("message", "Internal server error");
  }

  @Test
  void toResponse_When400BadRequest_ShouldEchoMessage() {
    WebApplicationException exception =
        new WebApplicationException("Bad request", Response.Status.BAD_REQUEST);

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(400);
    assertThat(entityMap(response)).containsEntry("message", "Bad request");
  }

  @Test
  void toResponse_When503ServiceUnavailable_ShouldHideMessage() {
    WebApplicationException exception =
        new WebApplicationException("Service unavailable", Response.Status.SERVICE_UNAVAILABLE);

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(503);
    assertThat(entityMap(response)).containsEntry("message", "Internal server error");
  }

  @Test
  void toResponse_WhenExceptionHasNullMessage_ShouldUseDefaultMessage() {
    NullPointerException exception = new NullPointerException();

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(500);
    assertThat(entityMap(response)).containsEntry("message", "Internal server error");
  }

  @Test
  void toResponse_WhenWebApplicationExceptionHasNullResponse_ShouldReturn500() {
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
  }

  @Test
  void toResponse_WhenNotFoundException_ShouldReturn404() {
    Response response = exceptionMapper.toResponse(new NotFoundException());

    assertThat(response.getStatus()).isEqualTo(404);
  }

  @Test
  void toResponse_WhenStatusAbove5xxRange_ShouldEchoMessage() {
    WebApplicationException exception =
        new WebApplicationException("Status 600", Response.status(600).build());

    Response response = exceptionMapper.toResponse(exception);

    assertThat(response.getStatus()).isEqualTo(600);
    assertThat(entityMap(response)).containsEntry("message", "Status 600");
  }
}
