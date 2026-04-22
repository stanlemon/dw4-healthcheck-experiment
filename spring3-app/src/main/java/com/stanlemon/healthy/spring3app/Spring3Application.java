package com.stanlemon.healthy.spring3app;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.MetricsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Main Spring Boot application class that provides REST endpoints for metrics tracking, error
 * simulation, and latency testing.
 */
@SpringBootApplication
public class Spring3Application {

  public static void main(String[] args) {
    SpringApplication.run(Spring3Application.class, args);
  }

  @Bean
  public MetricsService metricsService() {
    return new DefaultMetricsService();
  }

  @Bean
  public HealthEvaluator healthEvaluator(MetricsService metricsService) {
    return new HealthEvaluator(metricsService);
  }

  @Bean
  public com.stanlemon.healthy.metrics.LivenessEvaluator livenessEvaluator(
      MetricsService metricsService) {
    return new com.stanlemon.healthy.metrics.LivenessEvaluator(metricsService);
  }
}
