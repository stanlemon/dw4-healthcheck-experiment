package com.stanlemon.healthy.dw4app;

import com.stanlemon.healthy.dw4app.exceptions.ConstraintViolationExceptionMapper;
import com.stanlemon.healthy.dw4app.exceptions.GlobalExceptionMapper;
import com.stanlemon.healthy.dw4app.filters.LatencyTrackingFilter;
import com.stanlemon.healthy.dw4app.health.ApplicationHealthCheck;
import com.stanlemon.healthy.dw4app.resources.HangarResource;
import com.stanlemon.healthy.dw4app.resources.LivenessResource;
import com.stanlemon.healthy.dw4app.resources.MetricsResource;
import com.stanlemon.healthy.dw4app.resources.ReadinessResource;
import com.stanlemon.healthy.dw4app.resources.SlowResource;
import com.stanlemon.healthy.dw4app.resources.TestErrorsResource;
import com.stanlemon.healthy.dw4app.tasks.ClearMetricsTask;
import com.stanlemon.healthy.hangar.AerodynamicsPredictor;
import com.stanlemon.healthy.hangar.DefaultAerodynamicsPredictor;
import com.stanlemon.healthy.hangar.DefaultHangarService;
import com.stanlemon.healthy.hangar.HangarService;
import com.stanlemon.healthy.metrics.DefaultMetricsService;
import com.stanlemon.healthy.metrics.HealthEvaluator;
import com.stanlemon.healthy.metrics.LivenessEvaluator;
import com.stanlemon.healthy.metrics.MetricsService;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import jakarta.servlet.DispatcherType;
import java.util.EnumSet;

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
    final MetricsService metricsService = new DefaultMetricsService();
    final HealthEvaluator healthEvaluator = new HealthEvaluator(metricsService);
    final LivenessEvaluator livenessEvaluator = new LivenessEvaluator(metricsService);
    final AerodynamicsPredictor aerodynamicsPredictor = new DefaultAerodynamicsPredictor();
    final HangarService hangarService = new DefaultHangarService();

    environment
        .servlets()
        .addFilter("latency-tracking", new LatencyTrackingFilter(metricsService))
        .addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), true, "/*");

    environment.jersey().register(new MetricsResource(metricsService));
    environment.jersey().register(new ReadinessResource(healthEvaluator));
    environment.jersey().register(new LivenessResource(livenessEvaluator));
    environment.jersey().register(new TestErrorsResource());
    environment.jersey().register(new SlowResource());
    environment.jersey().register(new HangarResource(hangarService, aerodynamicsPredictor));

    environment.jersey().register(new ConstraintViolationExceptionMapper());
    environment.jersey().register(new GlobalExceptionMapper(metricsService));

    environment.admin().addTask(new ClearMetricsTask(metricsService));

    environment.healthChecks().register("application", new ApplicationHealthCheck(healthEvaluator));
  }
}
