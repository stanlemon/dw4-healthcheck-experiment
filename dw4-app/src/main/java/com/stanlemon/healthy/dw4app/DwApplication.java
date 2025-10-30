package com.stanlemon.healthy.dw4app;

import com.stanlemon.healthy.dw4app.exceptions.GlobalExceptionMapper;
import com.stanlemon.healthy.dw4app.filters.LatencyTrackingFilter;
import com.stanlemon.healthy.dw4app.health.ApplicationHealthCheck;
import com.stanlemon.healthy.dw4app.resources.ErrorResource;
import com.stanlemon.healthy.dw4app.resources.HelloWorldResource;
import com.stanlemon.healthy.dw4app.resources.MetricsResource;
import com.stanlemon.healthy.dw4app.resources.SlowResource;
import com.stanlemon.healthy.dw4app.resources.TestErrorsResource;
import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.MetricsService;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;

/**
 * Main Dropwizard application class that provides REST endpoints for metrics tracking, error
 * simulation, and latency testing.
 */
public class DwApplication extends Application<DwConfiguration> {

  /**
   * Application entry point.
   *
   * @param args command line arguments
   * @throws Exception if the application fails to start
   */
  public static void main(String[] args) throws Exception {
    new DwApplication().run(args);
  }

  @Override
  public String getName() {
    return "dw-application";
  }

  @Override
  public void initialize(Bootstrap<DwConfiguration> bootstrap) {
    // Nothing to initialize
  }

  /**
   * Configures and registers all application components including resources, filters, exception
   * mappers, and health checks.
   *
   * @param configuration the application configuration
   * @param environment the Dropwizard environment
   */
  @Override
  public void run(DwConfiguration configuration, Environment environment) {
    // Create and register MetricsService as a singleton managed component
    final MetricsService metricsService = new DefaultMetricsService();

    // Register latency tracking filter to measure all request latencies
    environment.jersey().register(new LatencyTrackingFilter(metricsService));

    // Register resources
    final HelloWorldResource helloWorldResource = new HelloWorldResource();
    environment.jersey().register(helloWorldResource);

    final ErrorResource errorResource = new ErrorResource();
    environment.jersey().register(errorResource);

    final MetricsResource metricsResource = new MetricsResource(metricsService);
    environment.jersey().register(metricsResource);

    final TestErrorsResource testErrorsResource = new TestErrorsResource();
    environment.jersey().register(testErrorsResource);

    final SlowResource slowResource = new SlowResource();
    environment.jersey().register(slowResource);

    // Register exception mapper for global error handling
    environment.jersey().register(new GlobalExceptionMapper(metricsService));

    // Register health checks
    environment.healthChecks().register("application", new ApplicationHealthCheck(metricsService));
  }
}
