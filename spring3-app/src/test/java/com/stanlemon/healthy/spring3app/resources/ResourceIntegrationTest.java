package com.stanlemon.healthy.spring3app.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.stanlemon.healthy.metrics.HealthResponse;
import com.stanlemon.healthy.metrics.MetricsResponse;
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
  void metricsEndpoint_WhenCalled_ShouldReturnHealthyStructure() {
    ResponseEntity<MetricsResponse> response =
        restTemplate.getForEntity(baseUrl + "/metrics", MetricsResponse.class);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isNotNull();
    MetricsResponse metrics = response.getBody();
    assertThat(metrics.getTotalErrors()).isGreaterThanOrEqualTo(0);
    assertThat(metrics.getErrorsLastMinute()).isGreaterThanOrEqualTo(0);
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
