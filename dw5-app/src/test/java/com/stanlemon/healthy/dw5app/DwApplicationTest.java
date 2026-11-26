package com.stanlemon.healthy.dw5app;

import static org.assertj.core.api.Assertions.assertThat;

import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.core.Response;
import java.net.URISyntaxException;
import java.nio.file.Path;
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

  private static final DropwizardAppExtension<DwConfiguration> APP =
      new DropwizardAppExtension<>(
          DwApplication.class,
          shippedConfigPath(),
          ConfigOverride.config("server.applicationConnectors[0].port", "0"),
          ConfigOverride.config("server.adminConnectors[0].port", "0"));

  /**
   * Finds dw5-app/config.yml from where the test classes were compiled
   * (dw5-app/target/test-classes), so the test does not depend on the working directory. An IDE can
   * run it from the repo root.
   */
  private static String shippedConfigPath() {
    try {
      Path testClasses =
          Path.of(
              DwApplicationTest.class.getProtectionDomain().getCodeSource().getLocation().toURI());
      return testClasses.resolve("../../config.yml").normalize().toString();
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Cannot find the dw5-app module directory", e);
    }
  }

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
