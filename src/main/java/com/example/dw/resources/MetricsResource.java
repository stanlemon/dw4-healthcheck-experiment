package com.example.dw.resources;

import com.example.dw.metrics.MetricsService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.annotation.JsonProperty;

@Path("/metrics")
@Produces(MediaType.APPLICATION_JSON)
public class MetricsResource {

    private final MetricsService metricsService = MetricsService.getInstance();

    @GET
    public MetricsResponse getMetrics() {
        long errorsLastMinute = metricsService.getErrorCountLastMinute();
        long totalErrors = metricsService.getTotalErrorCount();

        return new MetricsResponse(errorsLastMinute, totalErrors);
    }

    public record MetricsResponse(long errorsLastMinute, long totalErrors) {

        @Override
        @JsonProperty
            public long errorsLastMinute() {
                return errorsLastMinute;
            }

            @Override
            @JsonProperty
            public long totalErrors() {
                return totalErrors;
            }

            @JsonProperty
            public boolean isHealthy() {
                return errorsLastMinute <= 100;
            }
        }
}
