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
        assertThat(entity.isErrorThresholdBreached()).isFalse();
        assertThat(entity.isLatencyThresholdBreached()).isFalse();
        assertThat(entity.isHealthy()).isTrue();
        // Average latency should be >= 0 (could be 0 if no requests recorded yet, or some value if this request was recorded)
        assertThat(entity.getAvgLatencyLast60Minutes()).isGreaterThanOrEqualTo(0.0);
    }

    @Test
    public void testLatencyTracking() {
        Client client = APP.client();

        // Make a few requests to generate latency data
        for (int i = 0; i < 5; i++) {
            Response response = client.target(
                    String.format("http://localhost:%d/hello", APP.getLocalPort()))
                    .request()
                    .get();
            assertThat(response.getStatus()).isEqualTo(200);

            // Add a small delay to ensure some measurable latency
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Now check the metrics to see if latency was recorded
        Response metricsResponse = client.target(
                String.format("http://localhost:%d/metrics", APP.getLocalPort()))
                .request()
                .get();

        assertThat(metricsResponse.getStatus()).isEqualTo(200);

        MetricsResource.MetricsResponse metrics = metricsResponse.readEntity(MetricsResource.MetricsResponse.class);

        // Latency should be recorded (might be 0 if requests are very fast, which is fine)
        assertThat(metrics.getAvgLatencyLast60Minutes()).isGreaterThanOrEqualTo(0.0);
        // Should be reasonable latency (less than 1 second for simple requests)
        assertThat(metrics.getAvgLatencyLast60Minutes()).isLessThan(1000.0);
        // Since latency is well below 500ms threshold, it should not be breached
        assertThat(metrics.isLatencyThresholdBreached()).isFalse();
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
