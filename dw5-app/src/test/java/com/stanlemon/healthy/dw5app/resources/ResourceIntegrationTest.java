package com.stanlemon.healthy.dw5app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.stanlemon.healthy.dw5app.DwApplication;
import com.stanlemon.healthy.dw5app.DwConfiguration;
import com.stanlemon.healthy.metrics.HealthResponse;
import com.stanlemon.healthy.metrics.LivenessResponse;
import com.stanlemon.healthy.metrics.MetricsResponse;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("Resource Integration Tests")
class ResourceIntegrationTest {

  private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-config.yml");

  // Instance (non-static) extension → Dropwizard starts a fresh app per test method, giving each
  // test clean metrics state without needing an admin-side reset task.
  final DropwizardAppExtension<DwConfiguration> app =
      new DropwizardAppExtension<>(DwApplication.class, CONFIG_PATH);

  @BeforeEach
  void waitForAppToStart() {
    // Wait for the application to fully start and be ready to accept requests. Dropwizard 5
    // tightens the default client connection pool, so each Response must be closed to avoid
    // exhausting the pool across the suite — use try-with-resources throughout.
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(500))
        .ignoreExceptions()
        .untilAsserted(() -> {
          try (Response response =
              app.client()
                  .target(String.format("http://localhost:%d/healthcheck", app.getAdminPort()))
                  .request()
                  .get()) {
            assertThat(response.getStatus()).isEqualTo(200);
          }
        });
  }

  @Test
  @DisplayName("Should return healthy state when called with clean metrics")
  @Timeout(30)
  void metricsEndpoint_WhenCalledWithCleanMetrics_ShouldReturnHealthyState() {
    Client client = app.client();

    try (Response response =
        client
            .target(String.format("http://localhost:%d/metrics", app.getLocalPort()))
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
  @DisplayName("Should return ok status when called")
  @Timeout(30)
  void healthcheckEndpoint_WhenCalled_ShouldReturnOkStatus() {
    try (Response response =
        app.client()
            .target(String.format("http://localhost:%d/healthcheck", app.getAdminPort()))
            .request()
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  @DisplayName("Should return health response when called")
  @Timeout(30)
  void readinessEndpoint_WhenCalled_ShouldReturnHealthResponse() {
    try (Response response =
        app.client()
            .target(String.format("http://localhost:%d/health/ready", app.getLocalPort()))
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
  @DisplayName("Should return alive response when called")
  @Timeout(30)
  void livenessEndpoint_WhenCalled_ShouldReturnAliveResponse() {
    try (Response response =
        app.client()
            .target(String.format("http://localhost:%d/health/live", app.getLocalPort()))
            .request()
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);

      LivenessResponse entity = response.readEntity(LivenessResponse.class);
      assertThat(entity.getStatus()).isEqualTo("alive");
      assertThat(entity.isAlive()).isTrue();
    }
  }
}
