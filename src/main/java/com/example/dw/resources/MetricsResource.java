package com.example.dw.resources;

import com.example.dw.metrics.MetricsService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;

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

    public static class MetricsResponse {
        private final long errorsLastMinute;
        private final long totalErrors;

        @JsonCreator
        public MetricsResponse(@JsonProperty("errorsLastMinute") long errorsLastMinute,
                             @JsonProperty("totalErrors") long totalErrors) {
            this.errorsLastMinute = errorsLastMinute;
            this.totalErrors = totalErrors;
        }

        @JsonProperty
        public long getErrorsLastMinute() {
            return errorsLastMinute;
        }

        @JsonProperty
        public long getTotalErrors() {
            return totalErrors;
        }

        @JsonProperty
        public boolean isHealthy() {
            return errorsLastMinute <= 100;
        }
    }
}
