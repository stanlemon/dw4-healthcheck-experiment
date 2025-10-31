package com.stanlemon.healthy.spring3app;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Spring 3 Application Tests")
class Spring3ApplicationTest {

  private final Spring3Application application = new Spring3Application();

  @Test
  @DisplayName("Should provide MetricsService bean")
  void metricsService_ShouldReturnDefaultImplementation() {
    // Call the method under test
    MetricsService metricsService = application.metricsService();

    // Verify correct implementation
    assertThat(metricsService).isNotNull();
    assertThat(metricsService).isInstanceOf(DefaultMetricsService.class);
  }
}
