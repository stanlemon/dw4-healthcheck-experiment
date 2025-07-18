package com.example.dw;

import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import com.example.dw.health.ApplicationHealthCheck;
import com.example.dw.resources.HelloWorldResource;
import com.example.dw.resources.ErrorResource;
import com.example.dw.resources.MetricsResource;
import com.example.dw.resources.TestErrorsResource;
import com.example.dw.exceptions.GlobalExceptionMapper;

public class DwApplication extends Application<DwConfiguration> {

    public static void main(String[] args) throws Exception {
        new DwApplication().run(args);
    }

    @Override
    public String getName() {
        return "dw-application";
    }

    @Override
    public void initialize(Bootstrap<DwConfiguration> bootstrap) {
        // Nothing to initialize
    }

    @Override
    public void run(DwConfiguration configuration, Environment environment) {
        // Register resources
        final HelloWorldResource helloWorldResource = new HelloWorldResource();
        environment.jersey().register(helloWorldResource);

        final ErrorResource errorResource = new ErrorResource();
        environment.jersey().register(errorResource);

        final MetricsResource metricsResource = new MetricsResource();
        environment.jersey().register(metricsResource);

        final TestErrorsResource testErrorsResource = new TestErrorsResource();
        environment.jersey().register(testErrorsResource);

        // Register exception mapper for global error handling
        environment.jersey().register(new GlobalExceptionMapper());

        // Register health checks
        environment.healthChecks().register("application", new ApplicationHealthCheck());
    }
}
