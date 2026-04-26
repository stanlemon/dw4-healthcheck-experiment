package com.stanlemon.healthy.dw5app.functional;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.awaitility.Awaitility.await;

import com.stanlemon.healthy.dw5app.DwApplication;
import com.stanlemon.healthy.dw5app.DwConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("HangarResource Functional Tests")
class HangarResourceFunctionalTest {

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
            () ->
                given()
                    .when()
                    .get("http://localhost:" + APP.getAdminPort() + "/healthcheck")
                    .then()
                    .statusCode(200));
  }

  @Test
  @DisplayName("POST /hangar/planes stows a plane and returns 201 with prediction")
  void post_StowsPlaneReturns201WithPrediction() {
    Map<String, Object> body =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "name", "Phoenix", "wingspanCm", 22.0, "paperGsm", 80, "noseStyle", "pointed"))
            .when()
            .post(baseUrl + "/hangar/planes")
            .then()
            .statusCode(201)
            .contentType(ContentType.JSON)
            .extract()
            .as(Map.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> plane = (Map<String, Object>) body.get("plane");
    @SuppressWarnings("unchecked")
    Map<String, Object> prediction = (Map<String, Object>) body.get("prediction");

    assertThat(plane.get("name")).isEqualTo("Phoenix");
    assertThat(plane.get("id")).isNotNull();
    assertThat(plane.get("stowedAt"))
        .as("stowedAt must be an ISO-8601 string for cross-framework parity")
        .isInstanceOf(String.class)
        .asString()
        .matches("\\d{4}-\\d{2}-\\d{2}T.*Z");
    assertThat(((Number) prediction.get("predictedMeters")).doubleValue())
        .isCloseTo(8.8, within(1e-9));
    assertThat(prediction.get("verdict")).isEqualTo("sky champion");
  }

  @Test
  @DisplayName("POST Location header points to the created resource")
  void post_LocationHeaderPointsToCreatedResource() {
    Response postResponse =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "name", "Sparrow", "wingspanCm", 20.0, "paperGsm", 80, "noseStyle", "folded"))
            .when()
            .post(baseUrl + "/hangar/planes");

    assertThat(postResponse.statusCode()).isEqualTo(201);
    String location = postResponse.header("Location");
    assertThat(location).isNotNull().startsWith("http").contains("/hangar/planes/");

    Map<String, Object> readBody =
        given().when().get(location).then().statusCode(200).extract().as(Map.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> readPlane = (Map<String, Object>) readBody.get("plane");
    assertThat(readPlane.get("name")).isEqualTo("Sparrow");
  }

  @Test
  @DisplayName("Write-then-read journey returns identical prediction for the stowed plane")
  void journey_WriteThenReadReturnsIdenticalPrediction() {
    Map<String, Object> writeBody =
        given()
            .contentType(ContentType.JSON)
            .body(
                Map.of(
                    "name", "Kestrel", "wingspanCm", 30.0, "paperGsm", 100, "noseStyle", "folded"))
            .when()
            .post(baseUrl + "/hangar/planes")
            .then()
            .statusCode(201)
            .extract()
            .as(Map.class);

    @SuppressWarnings("unchecked")
    Map<String, Object> writePlane = (Map<String, Object>) writeBody.get("plane");
    String id = (String) writePlane.get("id");

    Map<String, Object> readBody =
        given()
            .when()
            .get(baseUrl + "/hangar/planes/" + id)
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .as(Map.class);

    assertThat(readBody.get("plane")).isEqualTo(writePlane);
    assertThat(readBody.get("prediction")).isEqualTo(writeBody.get("prediction"));
  }

  @Test
  @DisplayName("GET /hangar/planes returns stowed planes in the list")
  @SuppressWarnings("unchecked")
  void list_ReturnsStowedPlanes() {
    String uniqueName = "ListTest-" + System.nanoTime();
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", uniqueName, "wingspanCm", 22.0, "paperGsm", 80, "noseStyle", "blunt"))
        .when()
        .post(baseUrl + "/hangar/planes")
        .then()
        .statusCode(201);

    List<Map<String, Object>> planes =
        given()
            .when()
            .get(baseUrl + "/hangar/planes")
            .then()
            .statusCode(200)
            .extract()
            .as(List.class);

    assertThat(planes).isNotEmpty();
    assertThat(planes)
        .anySatisfy(
            entry -> {
              Map<String, Object> plane = (Map<String, Object>) entry.get("plane");
              assertThat(plane.get("name")).isEqualTo(uniqueName);
            });
  }

  @Test
  @DisplayName("GET /hangar/planes/{unknown} returns 404 with code and message in body")
  void getById_UnknownIdReturns404WithErrorBody() {
    Map<String, Object> body =
        given()
            .when()
            .get(baseUrl + "/hangar/planes/does-not-exist")
            .then()
            .statusCode(404)
            .contentType(ContentType.JSON)
            .extract()
            .as(Map.class);

    assertThat(body).containsKey("code").containsKey("message");
    assertThat(((Number) body.get("code")).intValue()).isEqualTo(404);
    assertThat(body.get("message")).isEqualTo("Plane not found");
  }

  @Test
  @DisplayName("DELETE /hangar/planes/{id} returns 405 Method Not Allowed")
  void delete_ReturnsMethodNotAllowed() {
    given().when().delete(baseUrl + "/hangar/planes/any-id").then().statusCode(405);
  }

  @Test
  @DisplayName("POST with invalid payload returns 400")
  void post_InvalidPayloadReturns400() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("name", "", "wingspanCm", 1.0, "paperGsm", 10, "noseStyle", "pointed"))
        .when()
        .post(baseUrl + "/hangar/planes")
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("POST with only wingspanCm out of range returns 400")
  void post_WingspanOnlyViolationReturns400() {
    given()
        .contentType(ContentType.JSON)
        .body(
            Map.of("name", "ValidName", "wingspanCm", 2.0, "paperGsm", 80, "noseStyle", "pointed"))
        .when()
        .post(baseUrl + "/hangar/planes")
        .then()
        .statusCode(400);
  }

  @Test
  @DisplayName("GET /hangar/planes/{id} with oversized id returns 400")
  void get_WhenIdExceedsMaxLength_ShouldReject() {
    String oversizedId = "a".repeat(65);

    int status =
        given().when().get(baseUrl + "/hangar/planes/" + oversizedId).then().extract().statusCode();

    assertThat(status).isEqualTo(400);
  }

  @Test
  @DisplayName("GET /hangar/planes/{id} with path traversal characters is routed away")
  void get_WhenIdContainsPathTraversal_ShouldReject() {
    // The HTTP server normalizes `../secret` before dispatch, so the request never reaches the
    // handler as an id. A 404 from the router is the correct rejection; nothing dangerous makes
    // it past routing into our resource.
    int status =
        given().when().get(baseUrl + "/hangar/planes/" + "../secret").then().extract().statusCode();

    assertThat(status).isEqualTo(404);
  }

  @Test
  @DisplayName("GET /hangar/planes/{id} with special characters returns 400")
  void get_WhenIdContainsSpecialCharacters_ShouldReject() {
    int status =
        given()
            .when()
            .get(baseUrl + "/hangar/planes/" + "id@with!chars")
            .then()
            .extract()
            .statusCode();

    assertThat(status).isEqualTo(400);
  }
}
