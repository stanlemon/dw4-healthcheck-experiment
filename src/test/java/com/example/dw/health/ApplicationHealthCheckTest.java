package com.example.dw.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.codahale.metrics.health.HealthCheck.Result;
import com.example.dw.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ApplicationHealthCheckTest {

    @Mock
    private MetricsService mockMetricsService;

    private ApplicationHealthCheck healthCheck;

    @BeforeEach
    public void setUp() {
        // Use a try-with-resources with MockedStatic if you want to mock the static method directly
        try (MockedStatic<MetricsService> mockedStatic = mockStatic(MetricsService.class)) {
            mockedStatic.when(MetricsService::getInstance).thenReturn(mockMetricsService);
            healthCheck = new ApplicationHealthCheck();
        }
    }

    @Test
    public void testCheckHealthy() {
        // Setup - below threshold
        when(mockMetricsService.getErrorCountLastMinute()).thenReturn(50L);

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
        when(mockMetricsService.getErrorCountLastMinute()).thenReturn(100L);

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
        when(mockMetricsService.getErrorCountLastMinute()).thenReturn(101L);

        // Execute
        Result result = healthCheck.check();

        // Verify
        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getMessage()).contains("Too many errors");
        assertThat(result.getMessage()).contains("101 errors");
    }
}
