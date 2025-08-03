package com.example.dw.health;

import com.codahale.metrics.health.HealthCheck;
import com.example.dw.metrics.Metrics;
import com.example.dw.metrics.MetricsService;

public class ApplicationHealthCheck extends HealthCheck {
    private static final int ERROR_THRESHOLD = 100;

    private final MetricsService metricsService;

    public ApplicationHealthCheck(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    public ApplicationHealthCheck() {
        this.metricsService = Metrics.get();
    }

    @Override
    protected Result check() {
        long errorCount = metricsService.getErrorCountLastMinute();

        if (errorCount > ERROR_THRESHOLD) {
            return Result.unhealthy("Too many errors: %d errors in the last minute (threshold: %d)",
                    errorCount, ERROR_THRESHOLD);
        }

        return Result.healthy("ok - %d errors in last minute (threshold: %d)",
                errorCount, ERROR_THRESHOLD);
    }
}
