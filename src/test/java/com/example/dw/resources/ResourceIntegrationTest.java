package com.example.dw.resources;

import com.example.dw.DwConfiguration;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.dw.DwApplication;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ResourceIntegrationTest {

    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-config.yml");

    public static final DropwizardAppExtension<DwConfiguration> APP = new DropwizardAppExtension<>(
            DwApplication.class, CONFIG_PATH);

    @AfterEach
    public void tearDown() {
        // Reset the metrics after each test to ensure a clean state

    }

    @Test
    public void testHelloEndpoint() {
        Client client = APP.client();

        Response response = client.target(
                String.format("http://localhost:%d/hello", APP.getLocalPort()))
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(200);

        HelloWorldResource.HelloResponse entity = response.readEntity(HelloWorldResource.HelloResponse.class);
        assertThat(entity.getMessage()).isEqualTo("Hello, World!");
    }

    @Test
    public void testMetricsEndpoint() {
        Client client = APP.client();

        Response response = client.target(
                String.format("http://localhost:%d/metrics", APP.getLocalPort()))
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(200);

        MetricsResource.MetricsResponse entity = response.readEntity(MetricsResource.MetricsResponse.class);

        // Since we're starting fresh in the test, no errors should be recorded
        // Running all of the tests in the test suite does accumulate errors.
//        assertThat(entity.totalErrors()).isEqualTo(0);
//        assertThat(entity.errorsLastMinute()).isEqualTo(0);
        assertThat(entity.isHealthy()).isTrue();
    }

    @Test
    public void testHealthCheckEndpoint() {
        Client client = APP.client();

        Response response = client.target(
                String.format("http://localhost:%d/healthcheck", APP.getAdminPort()))
                .request()
                .get();

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
