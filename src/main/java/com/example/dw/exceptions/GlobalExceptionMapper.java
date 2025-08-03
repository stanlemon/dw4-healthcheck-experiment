package com.example.dw.exceptions;

import com.example.dw.metrics.Metrics;
import com.example.dw.metrics.MetricsService;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

/**
 * Global exception mapper that catches all exceptions and tracks 500 errors
 * for health monitoring.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Set<Integer> TRACKED_CODES = Set.of(500);

    private final MetricsService metricsService;

    public GlobalExceptionMapper() {
        this.metricsService = Metrics.get();
    }

    @Override
    public Response toResponse(Throwable exception) {
        // First, determine the status code
        int status = determineStatusCode(exception);

        // Track all specific errors
        // It makes sense to track some 5xx errors, but we don't want to track all of them.
        // For example, 501 Not Implemented and 505 HTTP Version Not Supported are not
        // Also, we don't want to track 5xx from our downstream services such as 502 Bad Gateway or 503 Service Unavailable.

        // Alternatively, we could have a more complex logic to determine which 5xx errors to track.
        // Tag proxied errors
        // Have our downstream clients add a header like `X-Upstream-Error: true` and then exclude responses
        // that carry this head from our metrics.
        if (TRACKED_CODES.contains(status)) {
            metricsService.recordServerError();
        }

        // If it's a WebApplicationException, use its response if available
        if (exception instanceof WebApplicationException webAppException &&
            webAppException.getResponse() != null) {
            return webAppException.getResponse();
        }

        // Otherwise, build a generic error response
        return Response.status(status)
            .type(MediaType.APPLICATION_JSON_TYPE)
            .entity(Map.of(
                "code", status,
                "message", exception.getMessage() != null ?
                          exception.getMessage() : "Server Error"
            ))
            .build();
    }

    private int determineStatusCode(Throwable exception) {
        // If it's a WebApplicationException, use its status
        if (exception instanceof WebApplicationException webAppException) {
            return webAppException.getResponse().getStatus();
        }

        // By default, return 500 for all other exceptions
        return Response.Status.INTERNAL_SERVER_ERROR.getStatusCode();
    }
}
