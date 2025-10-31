package com.stanlemon.healthy.spring3app;

import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main Spring Boot application class that provides REST endpoints for metrics tracking, error
 * simulation, and latency testing.
 *
 * <p>This class follows Spring Boot best practices by: 1. Using @SpringBootApplication which
 * combines @Configuration, @EnableAutoConfiguration, and @ComponentScan 2. Explicitly scanning
 * component packages to find beans 3. Providing bean definitions for application dependencies
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.stanlemon.healthy.spring3app", "com.stanlemon.healthy.metrics"})
public class Spring3Application {

  /**
   * Application entry point.
   *
   * @param args command line arguments
   */
  public static void main(String[] args) {
    SpringApplication.run(Spring3Application.class, args);
  }

  /**
   * Creates and registers the MetricsService as a singleton bean.
   *
   * @return a thread-safe MetricsService implementation
   */
  @Bean
  public MetricsService metricsService() {
    return new DefaultMetricsService();
  }
}
