package com.stanlemon.healthy.spring3app;

import static org.assertj.core.api.Assertions.assertThat;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.MetricsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@DisplayName("Spring 3 Application Tests")
@SpringBootTest
class Spring3ApplicationTest {

  @Autowired private MetricsService metricsService;

  @Autowired private HealthEvaluator healthEvaluator;

  @Test
  @DisplayName("Should wire MetricsService bean as DefaultMetricsService")
  void metricsService_ShouldBeWiredAsDefaultImplementation() {
    assertThat(metricsService).isNotNull().isInstanceOf(DefaultMetricsService.class);
  }

  @Test
  @DisplayName("Should wire HealthEvaluator bean")
  void healthEvaluator_ShouldBeWired() {
    assertThat(healthEvaluator).isNotNull();
  }
}
