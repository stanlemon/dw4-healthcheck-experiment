package com.stanlemon.healthy.dw5app.tasks;

import com.stanlemon.healthy.metrics.MetricsService;
import io.dropwizard.servlets.tasks.Task;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

/**
 * Admin task that resets metrics state. Intended for operational use and integration tests that
 * need a clean slate without restarting the application.
 */
public class ClearMetricsTask extends Task {

  private final MetricsService metricsService;

  public ClearMetricsTask(MetricsService metricsService) {
    super("clear-metrics");
    this.metricsService = metricsService;
  }

  @Override
  public void execute(Map<String, List<String>> parameters, PrintWriter output) {
    metricsService.clearMetrics();
    output.println("metrics cleared");
  }
}
