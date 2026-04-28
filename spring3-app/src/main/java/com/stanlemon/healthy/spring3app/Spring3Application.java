package com.stanlemon.healthy.spring3app;

import com.stanlemon.healthy.hangar.AerodynamicsPredictor;
import com.stanlemon.healthy.hangar.DefaultAerodynamicsPredictor;
import com.stanlemon.healthy.hangar.DefaultHangarService;
import com.stanlemon.healthy.hangar.HangarService;
import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.LivenessEvaluator;
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

  // Shared-domain services (healthy-metrics, healthy-hangar) are framework-agnostic by design,
  // so they are wired here with explicit @Bean factories rather than @Component scanning —
  // adding Spring annotations to those modules would leak Spring into code that also runs under
  // Dropwizard.
  @Bean
  public MetricsService metricsService() {
    return new DefaultMetricsService();
  }

  @Bean
  public HealthEvaluator healthEvaluator(MetricsService metricsService) {
    return new HealthEvaluator(metricsService);
  }

  @Bean
  public LivenessEvaluator livenessEvaluator(MetricsService metricsService) {
    return new LivenessEvaluator(metricsService);
  }

  @Bean
  public AerodynamicsPredictor aerodynamicsPredictor() {
    return new DefaultAerodynamicsPredictor();
  }

  @Bean
  public HangarService hangarService() {
    return new DefaultHangarService();
  }
}
