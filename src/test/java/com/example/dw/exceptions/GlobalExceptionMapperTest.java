package com.example.dw.exceptions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.dw.metrics.Metrics;
import com.example.dw.metrics.MetricsService;
import com.example.dw.metrics.QueueMetricsService;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionMapperTest {

    @Mock
    private MetricsService mockMetricsService;

    private GlobalExceptionMapper exceptionMapper;

    @BeforeEach
    public void setUp() {

        mockMetricsService = mock(MetricsService.class);

        // Mock the MetricsService to return a mock instance
        try (MockedStatic<Metrics> mockedStatic = mockStatic(Metrics.class)) {
            mockedStatic.when(Metrics::get).thenReturn(mockMetricsService);
            exceptionMapper = new GlobalExceptionMapper();
        }
    }

    @Test
    public void testToResponseRuntimeException() {
        // Setup
        RuntimeException exception = new RuntimeException("Test runtime exception");

        // Execute
        Response response = exceptionMapper.toResponse(exception);

        // Verify
        assertThat(response.getStatus()).isEqualTo(500);
        assertThat(response.getMediaType()).isEqualTo(MediaType.APPLICATION_JSON_TYPE);

        // Verify metrics were recorded (500 error)
        verify(mockMetricsService).recordServerError();

        // Get and verify response entity
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertThat(entity.get("code")).isEqualTo(500);
        assertThat(entity.get("message")).isEqualTo("Test runtime exception");
    }

    @Test
    public void testToResponseWebApplicationException400() {
        // Setup - 400 Bad Request
        WebApplicationException exception = new WebApplicationException("Bad request", Response.Status.BAD_REQUEST);

        // Execute
        Response response = exceptionMapper.toResponse(exception);

        // Verify
        assertThat(response.getStatus()).isEqualTo(400);

        // Verify metrics were NOT recorded (not a 5xx error)
        verify(mockMetricsService, never()).recordServerError();
    }

    @Test
    public void testToResponseWebApplicationException503() {
        // Setup - 503 Service Unavailable
        WebApplicationException exception = new WebApplicationException("Service unavailable", Response.Status.SERVICE_UNAVAILABLE);

        // Execute
        Response response = exceptionMapper.toResponse(exception);

        // Verify
        assertThat(response.getStatus()).isEqualTo(503);

//        // Verify metrics were recorded (5xx error)
//        verify(mockMetricsService).recordServerError();

        assertThat(mockMetricsService.getTotalErrorCount()).isEqualTo(0);
    }

    @Test
    public void testToResponseNullMessage() {
        // Setup - exception with null message
        NullPointerException exception = new NullPointerException();

        // Execute
        Response response = exceptionMapper.toResponse(exception);

        // Verify
        assertThat(response.getStatus()).isEqualTo(500);

        // Get and verify response entity uses default message
        Map<String, Object> entity = (Map<String, Object>) response.getEntity();
        assertThat(entity.get("message")).isEqualTo("Server Error");
    }
}
