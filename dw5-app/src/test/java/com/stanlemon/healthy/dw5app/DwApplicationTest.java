package com.stanlemon.healthy.dw5app;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Starts the app with the real {@code config.yml} that {@code mvn exec:java} and the fat JAR use.
 * The other tests use {@code test-config.yml}, which turns off the request log, so they cannot see
 * a startup failure in the logging setup (for example a Logback version that Dropwizard's request
 * log does not support). Only the ports are overridden, so the test does not need 8097 and 8098.
 */
@ExtendWith(DropwizardExtensionsSupport.class)
@DisplayName("DwApplication")
class DwApplicationTest {

  // Surefire runs with the module directory as the working directory, so this is
  // dw5-app/config.yml.
  private static final DropwizardAppExtension<DwConfiguration> APP =
      new DropwizardAppExtension<>(
          DwApplication.class,
          "config.yml",
          ConfigOverride.config("server.applicationConnectors[0].port", "0"),
          ConfigOverride.config("server.adminConnectors[0].port", "0"));

  @Test
  @DisplayName("Should start and serve requests with the shipped config.yml")
  void run_WhenGivenShippedConfig_ShouldServeRequests() {
    try (Response response =
        APP.client()
            .target(String.format("http://localhost:%d/health/live", APP.getLocalPort()))
            .request()
            .get()) {
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }
}
