package com.example.dw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import io.dropwizard.core.setup.Bootstrap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DwApplicationTest {

    private DwApplication application;

    @BeforeEach
    public void setUp() {
        application = new DwApplication();
    }

    @Test
    public void testApplicationName() {
        // Test the getName() method
        String name = application.getName();
        assertThat(name).isEqualTo("dw-application");
    }

    @Test
    public void testInitialize() {
        // Test the initialize method - should not throw any exceptions
        Bootstrap<DwConfiguration> bootstrap = new Bootstrap<>(application);
        assertDoesNotThrow(() -> application.initialize(bootstrap));

        // Since initialize method is empty, just verify it can be called
        // In a more complex app, you'd verify bootstrap configuration here
    }

    @Test
    public void testMainMethod() {
        // Test that main method doesn't crash with help argument
        // This tests the application can at least parse basic args
        assertDoesNotThrow(() -> {
            DwApplication.main(new String[]{"--help"});
        });
    }

    @Test
    public void testMainMethodWithServerHelp() {
        // Test main with server help - should not throw exception
        assertDoesNotThrow(() -> {
            DwApplication.main(new String[]{"server", "--help"});
        });
    }

    @Test
    public void testMainMethodWithCheckCommand() {
        // Test main with check command and help
        assertDoesNotThrow(() -> {
            DwApplication.main(new String[]{"check", "--help"});
        });
    }

    @Test
    public void testApplicationInstantiation() {
        // Test that we can create a new instance of the application
        DwApplication newApp = new DwApplication();
        assertThat(newApp).isNotNull();
        assertThat(newApp.getName()).isEqualTo("dw-application");
    }

    @Test
    public void testConfigurationInstantiation() {
        // Test that we can create a configuration instance
        DwConfiguration config = new DwConfiguration();
        assertThat(config).isNotNull();
    }
}
