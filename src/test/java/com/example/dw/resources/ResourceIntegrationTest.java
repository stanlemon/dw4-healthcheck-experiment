package com.example.dw.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.example.dw.DwApplication;
import com.example.dw.DwConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
class ResourceIntegrationTest {

  private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-config.yml");

  static final DropwizardAppExtension<DwConfiguration> APP =
      new DropwizardAppExtension<>(DwApplication.class, CONFIG_PATH);

  @BeforeAll
  static void waitForAppToStart() {
    // Wait for the application to fully start and be ready to accept requests
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(500))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              Response response =
                  APP.client()
                      .target(String.format("http://localhost:%d/healthcheck", APP.getAdminPort()))
                      .request()
                      .get();
              assertThat(response.getStatus()).isEqualTo(200);
            });
  }

  @Test
  @Timeout(30)
  void helloEndpoint_WhenCalled_ShouldReturnHelloWorldMessage() {
    Client client = APP.client();

    Response response =
        client
            .target(String.format("http://localhost:%d/hello", APP.getLocalPort()))
            .request()
            .get();

    assertThat(response.getStatus()).isEqualTo(200);

    HelloWorldResource.HelloResponse entity =
        response.readEntity(HelloWorldResource.HelloResponse.class);
    assertThat(entity.getMessage()).isEqualTo("Hello, World!");
  }

  @Test
  @Timeout(30)
  void metricsEndpoint_WhenCalledWithCleanMetrics_ShouldReturnHealthyState() {
    Client client = APP.client();

    Response response =
        client
            .target(String.format("http://localhost:%d/metrics", APP.getLocalPort()))
            .request()
            .get();

    assertThat(response.getStatus()).isEqualTo(200);

    MetricsResource.MetricsResponse entity =
        response.readEntity(MetricsResource.MetricsResponse.class);
    // Since we're starting fresh in the test, no errors should be recorded
    assertThat(entity.getTotalErrors()).isZero();
    assertThat(entity.getErrorsLastMinute()).isZero();
    assertThat(entity.isErrorThresholdBreached()).isFalse();
    assertThat(entity.isLatencyThresholdBreached()).isFalse();
    assertThat(entity.isHealthy()).isTrue();
    // Average latency should be >= 0 (could be 0 if no requests recorded yet, or some value if this
    // request was recorded)
    assertThat(entity.getAvgLatencyLast60Seconds()).isGreaterThanOrEqualTo(0.0);
  }

  @Test
  @Timeout(30)
  void latencyTracking_WhenMultipleRequests_ShouldRecordReasonableLatency() {
    Client client = APP.client();

    // Make a few requests to generate latency data
    for (int i = 0; i < 5; i++) {
      Response response =
          client
              .target(String.format("http://localhost:%d/hello", APP.getLocalPort()))
              .request()
              .get();
      assertThat(response.getStatus()).isEqualTo(200);
    }

    // Use Awaitility to wait for latency metrics to be recorded and stabilize
    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(200))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              Response metricsResponse =
                  client
                      .target(String.format("http://localhost:%d/metrics", APP.getLocalPort()))
                      .request()
                      .get();

              assertThat(metricsResponse.getStatus()).isEqualTo(200);

              MetricsResource.MetricsResponse metrics =
                  metricsResponse.readEntity(MetricsResource.MetricsResponse.class);

              // Latency should be recorded and greater than 0
              assertThat(metrics.getAvgLatencyLast60Seconds()).isGreaterThan(0.0);
              // Should be reasonable latency (less than 1 second for simple requests)
              assertThat(metrics.getAvgLatencyLast60Seconds()).isLessThan(1000.0);
              // Since latency is well below 500ms threshold, it should not be breached
              assertThat(metrics.isLatencyThresholdBreached()).isFalse();
            });
  }

  @Test
  @Timeout(30)
  void healthcheckEndpoint_WhenCalled_ShouldReturnOkStatus() {
    Client client = APP.client();

    Response response =
        client
            .target(String.format("http://localhost:%d/healthcheck", APP.getAdminPort()))
            .request()
            .get();

    assertThat(response.getStatus()).isEqualTo(200);
  }
}
