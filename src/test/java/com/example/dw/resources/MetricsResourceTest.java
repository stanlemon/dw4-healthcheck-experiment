package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.example.dw.metrics.MetricsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class MetricsResourceTest {

    @Mock
    private MetricsService mockMetricsService;

    private MetricsResource resource;

    @BeforeEach
    public void setUp() {
        // Use a try-with-resources with MockedStatic if you want to mock the static method directly
        try (MockedStatic<MetricsService> mockedStatic = mockStatic(MetricsService.class)) {
            mockedStatic.when(MetricsService::getInstance).thenReturn(mockMetricsService);
            resource = new MetricsResource();
        }
    }

    @Test
    public void testGetMetricsHealthy() {
        // Setup
        when(mockMetricsService.getErrorCountLastMinute()).thenReturn(10L);
        when(mockMetricsService.getTotalErrorCount()).thenReturn(50L);

        // Execute
        MetricsResource.MetricsResponse response = resource.getMetrics();

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.errorsLastMinute()).isEqualTo(10);
        assertThat(response.totalErrors()).isEqualTo(50);
        assertThat(response.isHealthy()).isTrue();
    }

    @Test
    public void testGetMetricsUnhealthy() {
        // Setup
        when(mockMetricsService.getErrorCountLastMinute()).thenReturn(101L);
        when(mockMetricsService.getTotalErrorCount()).thenReturn(150L);

        // Execute
        MetricsResource.MetricsResponse response = resource.getMetrics();

        // Verify
        assertThat(response).isNotNull();
        assertThat(response.errorsLastMinute()).isEqualTo(101);
        assertThat(response.totalErrors()).isEqualTo(150);
        assertThat(response.isHealthy()).isFalse();
    }
}
