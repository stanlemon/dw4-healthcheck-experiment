package com.example.dw;

import com.example.dw.exceptions.GlobalExceptionMapper;
import com.example.dw.filters.LatencyTrackingFilter;
import com.example.dw.health.ApplicationHealthCheck;
import com.example.dw.metrics.DefaultMetricsService;
import com.example.dw.metrics.MetricsService;
import com.example.dw.resources.ErrorResource;
import com.example.dw.resources.HelloWorldResource;
import com.example.dw.resources.MetricsResource;
import com.example.dw.resources.SlowResource;
import com.example.dw.resources.TestErrorsResource;
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
