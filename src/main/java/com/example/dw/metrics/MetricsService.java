package com.example.dw.metrics;

public interface MetricsService {

    void recordServerError();

    long getErrorCountLastMinute();

    long getTotalErrorCount();

    void resetMetrics();
}
