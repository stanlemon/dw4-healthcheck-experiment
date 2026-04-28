package com.stanlemon.healthy.dw5app.functional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.stanlemon.healthy.dw5app.DwApplication;
import com.stanlemon.healthy.dw5app.DwConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("MetricsResource Functional Tests")
class MetricsResourceFunctionalTest {

  private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-config.yml");
  private static String baseUrl;

  public static final DropwizardAppExtension<DwConfiguration> APP =
      new DropwizardAppExtension<>(DwApplication.class, CONFIG_PATH);

  @BeforeAll
  static void setupClass() {
    baseUrl = "http://localhost:" + APP.getLocalPort();
    RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

    await()
        .atMost(Duration.ofSeconds(30))
        .pollInterval(Duration.ofMillis(500))
        .ignoreExceptions()
        .untilAsserted(
            () -> {
              given()
                  .when()
                  .get("http://localhost:" + APP.getAdminPort() + "/healthcheck")
                  .then()
                  .statusCode(200);
            });
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class MetricsResponseDTO {
    @JsonProperty("errorsLastMinute")
    private long errorsLastMinute;

    @JsonProperty("totalErrors")
    private long totalErrors;

    @JsonProperty("avgLatencyLast60Seconds")
    private double avgLatencyLast60Seconds;

    @JsonProperty("errorThresholdBreached")
    private boolean errorThresholdBreached;

    @JsonProperty("latencyThresholdBreached")
    private boolean latencyThresholdBreached;

    @JsonProperty("healthy")
    private boolean healthy;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SlowResponseDTO {
    @JsonProperty("message")
    private String message;

    @JsonProperty("delayMs")
    private long delayMs;

    @JsonProperty("actualMs")
    private long actualMs;
  }

  private record MetricsSnapshot(
      long totalErrors,
      long errorsLastMinute,
      double avgLatencyLast60Seconds,
      boolean errorThresholdBreached,
      boolean latencyThresholdBreached,
      boolean healthy) {

    static MetricsSnapshot fromMetricsEndpoint(String baseUrl) {
      MetricsResponseDTO metrics =
          given()
              .when()
              .get(baseUrl + "/metrics")
              .then()
              .statusCode(200)
              .contentType(ContentType.JSON)
              .extract()
              .as(MetricsResponseDTO.class);

      return new MetricsSnapshot(
          metrics.getTotalErrors(),
          metrics.getErrorsLastMinute(),
          metrics.getAvgLatencyLast60Seconds(),
          metrics.isErrorThresholdBreached(),
          metrics.isLatencyThresholdBreached(),
          metrics.isHealthy());
    }
  }

  private void waitForMetricsToUpdate(Supplier<Boolean> condition) {
    await()
        .atMost(Duration.ofSeconds(5))
        .pollInterval(Duration.ofMillis(100))
        .untilAsserted(() -> assertThat(condition.get()).isTrue());
  }

  @Nested
  @DisplayName("Error Metrics Tests")
  class ErrorMetricsTests {

    @Test
    @DisplayName("Error endpoint should return 500 status and be recorded in metrics")
    void errorEndpoint_WhenCalled_ShouldReturn500AndIncrementErrorMetrics() {
      MetricsSnapshot initialMetrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);

      given().when().get(baseUrl + "/test-errors/trigger").then().statusCode(500);

      waitForMetricsToUpdate(
          () -> {
            MetricsSnapshot updatedMetrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
            return updatedMetrics.totalErrors() > initialMetrics.totalErrors();
          });

      MetricsSnapshot finalMetrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
      assertThat(finalMetrics.totalErrors()).isGreaterThan(initialMetrics.totalErrors());
      assertThat(finalMetrics.errorsLastMinute()).isGreaterThanOrEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 10, 20})
    @DisplayName("Multiple errors should be accurately counted in metrics")
    void multipleErrors_WhenTriggered_ShouldBeAccuratelyCountedInMetrics(int errorCount) {
      MetricsSnapshot initialMetrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);

      IntStream.range(0, errorCount)
          .forEach(
              i -> {
                given().when().get(baseUrl + "/test-errors/trigger").then().statusCode(500);
              });

      waitForMetricsToUpdate(
          () -> {
            MetricsSnapshot updatedMetrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
            return updatedMetrics.totalErrors() >= initialMetrics.totalErrors() + errorCount;
          });

      MetricsSnapshot finalMetrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
      assertThat(finalMetrics.totalErrors() - initialMetrics.totalErrors()).isEqualTo(errorCount);
      assertThat(finalMetrics.errorsLastMinute()).isGreaterThanOrEqualTo(errorCount);
    }
  }

  @Nested
  @DisplayName("Latency Metrics Tests")
  class LatencyMetricsTests {

    @Test
    @DisplayName("Default slow endpoint should add expected latency to metrics")
    void defaultSlowEndpoint_WhenCalled_ShouldAddExpectedLatencyToMetrics() {
      SlowResponseDTO response =
          given()
              .when()
              .get(baseUrl + "/slow")
              .then()
              .statusCode(200)
              .contentType(ContentType.JSON)
              .extract()
              .as(SlowResponseDTO.class);

      assertThat(response.getDelayMs()).isEqualTo(1000);
      assertThat(response.getActualMs()).isBetween(900L, 1500L);

      waitForMetricsToUpdate(
          () -> {
            MetricsSnapshot metrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
            return metrics.avgLatencyLast60Seconds() > 0;
          });

      MetricsSnapshot metrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
      assertThat(metrics.avgLatencyLast60Seconds()).isBetween(1.0, 2000.0);
    }

    @ParameterizedTest
    @ValueSource(ints = {100, 200, 300})
    @DisplayName("Parameterized slow endpoint should add measurable latency to metrics")
    void parameterizedSlowEndpoint_WhenCalled_ShouldAddMeasurableLatencyToMetrics(int delayMs) {
      SlowResponseDTO response =
          given()
              .when()
              .get(baseUrl + "/slow/" + delayMs)
              .then()
              .statusCode(200)
              .contentType(ContentType.JSON)
              .extract()
              .as(SlowResponseDTO.class);

      assertThat(response.getDelayMs()).isEqualTo(delayMs);
      long expectedMinDelay = (long) (delayMs * 0.9);
      long expectedMaxDelay = (long) (delayMs * 1.5);
      assertThat(response.getActualMs()).isBetween(expectedMinDelay, expectedMaxDelay);

      waitForMetricsToUpdate(
          () -> {
            MetricsSnapshot metrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
            return metrics.avgLatencyLast60Seconds() > 0;
          });

      MetricsSnapshot metrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
      assertThat(metrics.avgLatencyLast60Seconds()).isBetween(1.0, 2000.0);
    }

    @Test
    @DisplayName("Multiple slow requests should produce non-trivial average latency")
    void multipleSlowRequests_WhenCalled_ShouldProduceNonTrivialAverageLatency() {
      MetricsSnapshot before = MetricsSnapshot.fromMetricsEndpoint(baseUrl);

      List<Integer> delays = List.of(100, 200, 300);
      for (int delay : delays) {
        given().when().get(baseUrl + "/slow/" + delay).then().statusCode(200);
      }

      waitForMetricsToUpdate(
          () -> {
            MetricsSnapshot metrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
            return metrics.avgLatencyLast60Seconds() > before.avgLatencyLast60Seconds()
                || metrics.avgLatencyLast60Seconds() > 0;
          });

      MetricsSnapshot after = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
      assertThat(after.avgLatencyLast60Seconds()).isBetween(1.0, 2000.0);
    }

    @Test
    @DisplayName("Too large delay should be rejected with 400 Bad Request")
    void tooLargeDelay_WhenRequested_ShouldBeRejectedWith400() {
      int tooLargeDelay = 10001;

      SlowResponseDTO response =
          given()
              .when()
              .get(baseUrl + "/slow/" + tooLargeDelay)
              .then()
              .statusCode(400)
              .contentType(ContentType.JSON)
              .extract()
              .as(SlowResponseDTO.class);

      assertThat(response.getMessage()).contains("Delay too long");
      assertThat(response.getDelayMs()).isEqualTo(tooLargeDelay);
      assertThat(response.getActualMs()).isZero();
    }

    @Test
    @DisplayName("Negative delay should be rejected with 400 Bad Request")
    void negativeDelay_WhenRequested_ShouldBeRejectedWith400() {
      int negativeDelay = -100;

      SlowResponseDTO response =
          given()
              .when()
              .get(baseUrl + "/slow/" + negativeDelay)
              .then()
              .statusCode(400)
              .contentType(ContentType.JSON)
              .extract()
              .as(SlowResponseDTO.class);

      assertThat(response.getMessage()).contains("Delay cannot be negative");
      assertThat(response.getDelayMs()).isEqualTo(negativeDelay);
      assertThat(response.getActualMs()).isZero();
    }
  }

  @Nested
  @DisplayName("Combined Metrics Tests")
  class CombinedMetricsTests {

    @Test
    @DisplayName("Metrics should correctly reflect both errors and latency")
    void metrics_WhenBothErrorsAndLatencyOccur_ShouldReflectBothCorrectly() {
      MetricsSnapshot initialMetrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);

      int errorCount = 5;
      IntStream.range(0, errorCount)
          .forEach(
              i -> {
                given().when().get(baseUrl + "/test-errors/trigger").then().statusCode(500);
              });

      List<Integer> delays = List.of(150, 250, 350);
      for (int delay : delays) {
        given().when().get(baseUrl + "/slow/" + delay).then().statusCode(200);
      }

      waitForMetricsToUpdate(
          () -> {
            MetricsSnapshot metrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
            return metrics.totalErrors() >= initialMetrics.totalErrors() + errorCount
                && metrics.avgLatencyLast60Seconds() > 0;
          });

      MetricsSnapshot metrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
      assertThat(metrics.totalErrors())
          .isGreaterThanOrEqualTo(initialMetrics.totalErrors() + errorCount);
      assertThat(metrics.errorsLastMinute()).isGreaterThanOrEqualTo(errorCount);
      assertThat(metrics.avgLatencyLast60Seconds()).isBetween(1.0, 2000.0);
    }

    @Test
    @DisplayName("Health status should reflect error and latency conditions")
    void healthStatus_WhenErrorsGenerated_ShouldReflectInMetrics() {
      MetricsSnapshot initialMetrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);

      IntStream.range(0, 25)
          .forEach(
              i -> {
                given().when().get(baseUrl + "/test-errors/trigger").then().statusCode(500);
              });

      waitForMetricsToUpdate(
          () -> {
            MetricsSnapshot metrics = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
            return metrics.totalErrors() >= initialMetrics.totalErrors() + 25;
          });

      MetricsSnapshot afterErrors = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
      assertThat(afterErrors.totalErrors())
          .isGreaterThanOrEqualTo(initialMetrics.totalErrors() + 25);
      assertThat(afterErrors.errorsLastMinute()).isGreaterThanOrEqualTo(25);

      given().when().get(baseUrl + "/slow/500").then().statusCode(200);

      waitForMetricsToUpdate(
          () -> MetricsSnapshot.fromMetricsEndpoint(baseUrl).avgLatencyLast60Seconds() > 0);

      MetricsSnapshot afterLatency = MetricsSnapshot.fromMetricsEndpoint(baseUrl);
      assertThat(afterLatency.avgLatencyLast60Seconds()).isBetween(1.0, 2000.0);
    }

    @Test
    @DisplayName("Metrics endpoint should return the correct response structure")
    void metricsEndpoint_ShouldReturnCorrectResponseStructure() {
      @SuppressWarnings("unchecked")
      Map<String, Object> metricsMap =
          given()
              .when()
              .get(baseUrl + "/metrics")
              .then()
              .statusCode(200)
              .contentType(ContentType.JSON)
              .extract()
              .as(Map.class);

      assertThat(metricsMap)
          .containsKey("totalErrors")
          .containsKey("errorsLastMinute")
          .containsKey("avgLatencyLast60Seconds")
          .containsKey("errorThresholdBreached")
          .containsKey("latencyThresholdBreached")
          .containsKey("healthy");

      assertThat(metricsMap.get("totalErrors")).isInstanceOf(Number.class);
      assertThat(metricsMap.get("errorsLastMinute")).isInstanceOf(Number.class);
      assertThat(metricsMap.get("avgLatencyLast60Seconds")).isInstanceOf(Number.class);
      assertThat(metricsMap.get("errorThresholdBreached")).isInstanceOf(Boolean.class);
      assertThat(metricsMap.get("latencyThresholdBreached")).isInstanceOf(Boolean.class);
      assertThat(metricsMap.get("healthy")).isInstanceOf(Boolean.class);
    }

    @Test
    @Timeout(60)
    @DisplayName("Readiness endpoint returns 503 when error threshold is breached")
    void readinessEndpoint_WhenErrorThresholdBreached_ShouldReturn503() {
      // Shared DropwizardAppExtension means prior tests have accumulated requests.
      // Generate enough errors to guarantee >10% error rate on the high-traffic
      // path (requestCount >= 100), regardless of prior request count.
      int errorCount = 200;
      IntStream.range(0, errorCount)
          .forEach(
              i -> {
                given().when().get(baseUrl + "/test-errors/trigger").then().statusCode(500);
              });

      waitForMetricsToUpdate(
          () -> MetricsSnapshot.fromMetricsEndpoint(baseUrl).errorThresholdBreached());

      @SuppressWarnings("unchecked")
      Map<String, Object> healthBody =
          given()
              .when()
              .get(baseUrl + "/health/ready")
              .then()
              .statusCode(503)
              .contentType(ContentType.JSON)
              .extract()
              .as(Map.class);

      assertThat(healthBody.get("errorThresholdBreached")).isEqualTo(true);
    }
  }
}
