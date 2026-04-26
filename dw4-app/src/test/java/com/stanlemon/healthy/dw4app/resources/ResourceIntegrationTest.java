package com.stanlemon.healthy.dw4app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.stanlemon.healthy.dw4app.DwApplication;
import com.stanlemon.healthy.dw4app.DwConfiguration;
import com.stanlemon.healthy.metrics.HealthResponse;
import com.stanlemon.healthy.metrics.LivenessResponse;
import com.stanlemon.healthy.metrics.MetricsResponse;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("Resource Integration Tests")
class ResourceIntegrationTest {

  private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-config.yml");

  static final DropwizardAppExtension<DwConfiguration> APP =
      new DropwizardAppExtension<>(DwApplication.class, CONFIG_PATH);

  @BeforeAll
  static void waitForAppToStart() {
    // Wait for the application to fully start and be ready to accept requests. Dropwizard 5
    // tightens the default client connection pool, so each Response must be closed to avoid
    // exhausting the pool across the suite — use try-with-resources throughout.
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(500))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              try (Response response =
                  APP.client()
                      .target(String.format("http://localhost:%d/healthcheck", APP.getAdminPort()))
                      .request()
                      .get()) {
                assertThat(response.getStatus()).isEqualTo(200);
              }
            });
  }

  @Test
  @Timeout(30)
  void metricsEndpoint_WhenCalledWithCleanMetrics_ShouldReturnHealthyState() {
    Client client = APP.client();

    // Reset state so prior tests in the shared APP extension don't leak errors/latency in.
    try (Response clearResponse =
        client
            .target(String.format("http://localhost:%d/tasks/clear-metrics", APP.getAdminPort()))
            .request()
            .post(Entity.text(""))) {
      assertThat(clearResponse.getStatus()).isEqualTo(200);
    }

    try (Response response =
        client
            .target(String.format("http://localhost:%d/metrics", APP.getLocalPort()))
            .request()
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);

      MetricsResponse entity = response.readEntity(MetricsResponse.class);
      assertThat(entity.getTotalErrors()).isZero();
      assertThat(entity.getErrorsLastMinute()).isZero();
      assertThat(entity.isErrorThresholdBreached()).isFalse();
      assertThat(entity.isLatencyThresholdBreached()).isFalse();
      assertThat(entity.isHealthy()).isTrue();
      assertThat(entity.getAvgLatencyLast60Seconds()).isGreaterThanOrEqualTo(0.0);
    }
  }

  @Test
  @Timeout(30)
  void latencyTracking_WhenMultipleRequests_ShouldRecordReasonableLatency() {
    Client client = APP.client();

    // Make a few requests to generate latency data
    for (int i = 0; i < 5; i++) {
      try (Response response =
          client
              .target(String.format("http://localhost:%d/slow/1", APP.getLocalPort()))
              .request()
              .get()) {
        assertThat(response.getStatus()).isEqualTo(200);
      }
    }

    // Use Awaitility to wait for latency metrics to be recorded and stabilize
    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(200))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              try (Response metricsResponse =
                  client
                      .target(String.format("http://localhost:%d/metrics", APP.getLocalPort()))
                      .request()
                      .get()) {
                assertThat(metricsResponse.getStatus()).isEqualTo(200);

                MetricsResponse metrics = metricsResponse.readEntity(MetricsResponse.class);

                // Latency should be recorded and greater than 0
                assertThat(metrics.getAvgLatencyLast60Seconds()).isGreaterThan(0.0);
                // Should be reasonable latency (less than 1 second for simple requests)
                assertThat(metrics.getAvgLatencyLast60Seconds()).isLessThan(1000.0);
                // Since latency is well below 500ms threshold, it should not be breached
                assertThat(metrics.isLatencyThresholdBreached()).isFalse();
              }
            });
  }

  @Test
  @Timeout(30)
  void healthcheckEndpoint_WhenCalled_ShouldReturnOkStatus() {
    try (Response response =
        APP.client()
            .target(String.format("http://localhost:%d/healthcheck", APP.getAdminPort()))
            .request()
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  @Timeout(30)
  void readinessEndpoint_WhenCalled_ShouldReturnHealthResponse() {
    try (Response response =
        APP.client()
            .target(String.format("http://localhost:%d/health/ready", APP.getLocalPort()))
            .request()
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);

      HealthResponse entity = response.readEntity(HealthResponse.class);
      assertThat(entity.getStatus()).isEqualTo("healthy");
      assertThat(entity.isHealthy()).isTrue();
      assertThat(entity.getMessage()).contains("OK");
      assertThat(entity.getErrorsLastMinute()).isZero();
      assertThat(entity.isErrorThresholdBreached()).isFalse();
      assertThat(entity.isLatencyThresholdBreached()).isFalse();
    }
  }

  @Test
  @Timeout(30)
  void livenessEndpoint_WhenCalled_ShouldReturnAliveResponse() {
    try (Response response =
        APP.client()
            .target(String.format("http://localhost:%d/health/live", APP.getLocalPort()))
            .request()
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);

      LivenessResponse entity = response.readEntity(LivenessResponse.class);
      assertThat(entity.getStatus()).isEqualTo("alive");
      assertThat(entity.isAlive()).isTrue();
    }
  }
}
