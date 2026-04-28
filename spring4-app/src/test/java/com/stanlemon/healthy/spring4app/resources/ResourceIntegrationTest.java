package com.stanlemon.healthy.spring4app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.stanlemon.healthy.metrics.HealthResponse;
import com.stanlemon.healthy.metrics.LivenessResponse;
import com.stanlemon.healthy.metrics.MetricsResponse;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Resource Integration Tests")
class ResourceIntegrationTest {

  @LocalServerPort private int port;

  private RestClient restClient;

  @BeforeEach
  void setUp() {
    // Rebuild per-test: @DirtiesContext on one method restarts Tomcat on a new port, so we can't
    // cache the RestClient across methods.
    restClient = RestClient.create("http://localhost:" + port);

    // Spring Boot 4 removed TestRestTemplate in favor of Spring Framework's RestClient.
    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(500))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              int status =
                  restClient
                      .get()
                      .uri("/actuator/health")
                      .retrieve()
                      .toBodilessEntity()
                      .getStatusCode()
                      .value();
              assertThat(status).isEqualTo(200);
            });
  }

  @Test
  @Timeout(30)
  @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
  void metricsEndpoint_WhenCalledWithCleanMetrics_ShouldReturnHealthyState() {
    // @DirtiesContext rebuilds the Spring context (and the MetricsService singleton) before this
    // method runs, so prior tests in the shared context can't leak errors/latency into the
    // assertions below.
    MetricsResponse metrics =
        restClient.get().uri("/metrics").retrieve().body(MetricsResponse.class);

    assertThat(metrics).isNotNull();
    assertThat(metrics.getTotalErrors()).isZero();
    assertThat(metrics.getErrorsLastMinute()).isZero();
    assertThat(metrics.isErrorThresholdBreached()).isFalse();
    assertThat(metrics.isLatencyThresholdBreached()).isFalse();
    assertThat(metrics.isHealthy()).isTrue();
    assertThat(metrics.getAvgLatencyLast60Seconds()).isGreaterThanOrEqualTo(0.0);
  }

  @Test
  @Timeout(30)
  void latencyTracking_WhenMultipleRequests_ShouldRecordReasonableLatency() {
    for (int i = 0; i < 5; i++) {
      int status =
          restClient.get().uri("/slow/1").retrieve().toBodilessEntity().getStatusCode().value();
      assertThat(status).isEqualTo(200);
    }

    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(200))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              MetricsResponse metrics =
                  restClient.get().uri("/metrics").retrieve().body(MetricsResponse.class);
              assertThat(metrics).isNotNull();
              assertThat(metrics.getAvgLatencyLast60Seconds()).isGreaterThan(0.0);
              assertThat(metrics.getAvgLatencyLast60Seconds()).isLessThan(1000.0);
              assertThat(metrics.isLatencyThresholdBreached()).isFalse();
            });
  }

  @Test
  @Timeout(30)
  void actuatorHealthEndpoint_WhenCalled_ShouldReturnOkStatus() {
    int status =
        restClient
            .get()
            .uri("/actuator/health")
            .retrieve()
            .toBodilessEntity()
            .getStatusCode()
            .value();

    assertThat(status).isEqualTo(200);
  }

  @Test
  @Timeout(30)
  void readinessEndpoint_WhenCalled_ShouldReturnHealthResponse() {
    HealthResponse health =
        restClient.get().uri("/health/ready").retrieve().body(HealthResponse.class);

    assertThat(health).isNotNull();
    assertThat(health.getStatus()).isEqualTo("healthy");
    assertThat(health.isHealthy()).isTrue();
    assertThat(health.getMessage()).contains("OK");
    assertThat(health.getErrorsLastMinute()).isZero();
    assertThat(health.isErrorThresholdBreached()).isFalse();
    assertThat(health.isLatencyThresholdBreached()).isFalse();
  }

  @Test
  @Timeout(30)
  void livenessEndpoint_WhenCalled_ShouldReturnAliveResponse() {
    LivenessResponse liveness =
        restClient.get().uri("/health/live").retrieve().body(LivenessResponse.class);

    assertThat(liveness).isNotNull();
    assertThat(liveness.getStatus()).isEqualTo("alive");
    assertThat(liveness.isAlive()).isTrue();
  }
}
