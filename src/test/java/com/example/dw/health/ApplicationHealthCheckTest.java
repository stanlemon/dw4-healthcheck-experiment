package com.example.dw.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.codahale.metrics.health.HealthCheck.Result;
import com.example.dw.metrics.Metrics;
import com.example.dw.metrics.MetricsService;
import com.example.dw.metrics.QueueMetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationHealthCheckTest {

    @Mock
    private QueueMetricsService metricsService;

    @Mock
    private Metrics metrics;

    private ApplicationHealthCheck healthCheck;

    @BeforeEach
    public void setUp() {

        // Mock the MetricsService to return a mock instance
        try (MockedStatic<Metrics> mockedStatic = mockStatic(Metrics.class)) {
            mockedStatic.when(Metrics::get).thenReturn(metricsService);
            when(metricsService.getErrorCountLastMinute()).thenReturn(0L); // Default to 0 for setup
        }

        healthCheck = new ApplicationHealthCheck(metricsService);
    }

    @Test
    public void testCheckHealthy() {
        // Setup - below threshold
        when(metricsService.getErrorCountLastMinute()).thenReturn(50L);

        // Execute
        Result result = healthCheck.check();

        // Verify
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getMessage()).contains("ok");
        assertThat(result.getMessage()).contains("50 errors");
    }

    @Test
    public void testCheckExactThreshold() {
        // Setup - at threshold (100 errors)
        when(metricsService.getErrorCountLastMinute()).thenReturn(100L);

        // Execute
        Result result = healthCheck.check();

        // Verify - should still be healthy at exactly 100
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getMessage()).contains("ok");
        assertThat(result.getMessage()).contains("100 errors");
    }

    @Test
    public void testCheckUnhealthy() {
        // Setup - above threshold
        when(metricsService.getErrorCountLastMinute()).thenReturn(101L);

        // Execute
        Result result = healthCheck.check();

        // Verify
        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getMessage()).contains("Too many errors");
        assertThat(result.getMessage()).contains("101 errors");
    }
}
