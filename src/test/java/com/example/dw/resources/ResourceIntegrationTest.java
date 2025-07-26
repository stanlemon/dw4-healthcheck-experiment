package com.example.dw.resources;

import com.example.dw.DwConfiguration;
import com.example.dw.metrics.MetricsService;
import io.dropwizard.testing.ResourceHelpers;
import io.dropwizard.testing.junit5.DropwizardAppExtension;
import io.dropwizard.testing.junit5.DropwizardExtensionsSupport;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.example.dw.DwApplication;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(DropwizardExtensionsSupport.class)
public class ResourceIntegrationTest {

    private static final String CONFIG_PATH = ResourceHelpers.resourceFilePath("test-config.yml");

    public static final DropwizardAppExtension<DwConfiguration> APP = new DropwizardAppExtension<>(
            DwApplication.class, CONFIG_PATH);

    @BeforeEach
    public void setUp() {
        // Clear metrics before each test since MetricsService is a singleton
        MetricsService.getInstance().clearMetrics();
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
        assertThat(entity.getTotalErrors()).isEqualTo(0);
        assertThat(entity.getErrorsLastMinute()).isEqualTo(0);
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
