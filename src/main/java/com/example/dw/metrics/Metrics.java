package com.example.dw.metrics;

public final class Metrics {
    private static final MetricsService DELEGATE;

    static {
        // Example: choose via JVM system property
        String impl = System.getProperty("metrics.backend", "queue");
        DELEGATE = switch (impl) {
            case "queue" -> QueueMetricsService.INSTANCE;
            case "ringBuffer" -> RingBufferMetricsService.INSTANCE;
            default -> throw new IllegalArgumentException(
                    "Unknown metrics backend: " + impl);
        };
    }

    public static MetricsService get() {
        return DELEGATE;
    }

    private Metrics() {
    }
}