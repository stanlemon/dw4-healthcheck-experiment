package com.stanlemon.healthy.spring3app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.stanlemon.healthy.metrics.HealthResponse;
import com.stanlemon.healthy.metrics.MetricsResponse;
import com.stanlemon.healthy.metrics.MetricsService;
import java.time.Duration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Resource Integration Tests")
class ResourceIntegrationTest {

  @LocalServerPort private int port;

  @Autowired private TestRestTemplate restTemplate;

  @Autowired private MetricsService metricsService;

  private String baseUrl;

  @BeforeAll
  void setUp() {
    baseUrl = "http://localhost:" + port;

    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(500))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              ResponseEntity<String> response =
                  restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);
              assertThat(response.getStatusCode().value()).isEqualTo(200);
            });
  }

  @Test
  @Timeout(30)
  void metricsEndpoint_WhenCalledWithCleanMetrics_ShouldReturnHealthyState() {
    // Reset state so prior tests in the shared Spring context don't leak errors/latency in.
    metricsService.clearMetrics();

    ResponseEntity<MetricsResponse> response =
        restTemplate.getForEntity(baseUrl + "/metrics", MetricsResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    MetricsResponse metrics = response.getBody();
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
      ResponseEntity<String> response =
          restTemplate.getForEntity(baseUrl + "/slow/1", String.class);
      assertThat(response.getStatusCode().value()).isEqualTo(200);
    }

    await()
        .atMost(Duration.ofSeconds(10))
        .pollInterval(Duration.ofMillis(200))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              ResponseEntity<MetricsResponse> response =
                  restTemplate.getForEntity(baseUrl + "/metrics", MetricsResponse.class);
              assertThat(response.getStatusCode().value()).isEqualTo(200);
              assertThat(response.getBody()).isNotNull();
              MetricsResponse metrics = response.getBody();
              assertThat(metrics.getAvgLatencyLast60Seconds()).isGreaterThan(0.0);
              assertThat(metrics.getAvgLatencyLast60Seconds()).isLessThan(1000.0);
              assertThat(metrics.isLatencyThresholdBreached()).isFalse();
            });
  }

  @Test
  @Timeout(30)
  void actuatorHealthEndpoint_WhenCalled_ShouldReturnOkStatus() {
    ResponseEntity<String> response =
        restTemplate.getForEntity(baseUrl + "/actuator/health", String.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
  }

  @Test
  @Timeout(30)
  void applicationWiring_AllResourcesShouldBeRegistered() {
    assertThat(
            restTemplate.getForEntity(baseUrl + "/metrics", String.class).getStatusCode().value())
        .isEqualTo(200);
    assertThat(
            restTemplate
                .getForEntity(baseUrl + "/health/ready", String.class)
                .getStatusCode()
                .value())
        .isEqualTo(200);
    assertThat(
            restTemplate
                .getForEntity(baseUrl + "/health/live", String.class)
                .getStatusCode()
                .value())
        .isEqualTo(200);
    assertThat(restTemplate.getForEntity(baseUrl + "/slow/1", String.class).getStatusCode().value())
        .isEqualTo(200);
    assertThat(
            restTemplate
                .getForEntity(baseUrl + "/hangar/planes", String.class)
                .getStatusCode()
                .value())
        .isEqualTo(200);
    // /test-errors/trigger returns 500 intentionally — a 500 proves the resource is registered.
    assertThat(
            restTemplate
                .getForEntity(baseUrl + "/test-errors/trigger", String.class)
                .getStatusCode()
                .value())
        .isEqualTo(500);
  }

  @Test
  @Timeout(30)
  void readinessEndpoint_WhenCalled_ShouldReturnHealthResponse() {
    ResponseEntity<HealthResponse> response =
        restTemplate.getForEntity(baseUrl + "/health/ready", HealthResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    HealthResponse health = response.getBody();
    assertThat(health.getStatus()).isEqualTo("healthy");
    assertThat(health.isHealthy()).isTrue();
    assertThat(health.getMessage()).contains("OK");
    assertThat(health.getErrorsLastMinute()).isGreaterThanOrEqualTo(0);
    assertThat(health.isErrorThresholdBreached()).isFalse();
    assertThat(health.isLatencyThresholdBreached()).isFalse();
  }

  @Test
  @Timeout(30)
  void livenessEndpoint_WhenCalled_ShouldReturnAliveResponse() {
    ResponseEntity<com.stanlemon.healthy.metrics.LivenessResponse> response =
        restTemplate.getForEntity(
            baseUrl + "/health/live", com.stanlemon.healthy.metrics.LivenessResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();

    com.stanlemon.healthy.metrics.LivenessResponse liveness = response.getBody();
    assertThat(liveness.getStatus()).isEqualTo("alive");
    assertThat(liveness.isAlive()).isTrue();
  }
}
