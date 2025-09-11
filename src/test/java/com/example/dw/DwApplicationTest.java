package com.example.dw;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import io.dropwizard.core.setup.Bootstrap;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DwApplicationTest {

  private DwApplication application;

  @BeforeEach
  void setUp() {
    application = new DwApplication();
  }

  @Test
  void getName_WhenCalled_ShouldReturnCorrectApplicationName() {
    // Test the getName() method
    String name = application.getName();
    assertThat(name).isEqualTo("dw-application");
  }

  @Test
  void initialize_WhenCalled_ShouldNotThrowException() {
    // Test the initialize method - should not throw any exceptions
    Bootstrap<DwConfiguration> bootstrap = new Bootstrap<>(application);
    assertThatCode(() -> application.initialize(bootstrap)).doesNotThrowAnyException();

    // Since initialize method is empty, just verify it can be called
    // In a more complex app, you'd verify bootstrap configuration here
  }

  @Test
  void main_WhenCalledWithHelpArg_ShouldNotThrowException() {
    // Test that main method doesn't crash with help argument
    // This tests the application can at least parse basic args
    suppressStdout(
        () -> {
          assertThatCode(
                  () -> {
                    DwApplication.main(new String[] {"--help"});
                  })
              .doesNotThrowAnyException();
        });
  }

  @Test
  void main_WhenCalledWithServerHelpArgs_ShouldNotThrowException() {
    // Test main with server help - should not throw exception
    suppressStdout(
        () -> {
          assertThatCode(
                  () -> {
                    DwApplication.main(new String[] {"server", "--help"});
                  })
              .doesNotThrowAnyException();
        });
  }

  @Test
  void main_WhenCalledWithCheckHelpArgs_ShouldNotThrowException() {
    // Test main with check command and help
    suppressStdout(
        () -> {
          assertThatCode(
                  () -> {
                    DwApplication.main(new String[] {"check", "--help"});
                  })
              .doesNotThrowAnyException();
        });
  }

  @Test
  void constructor_WhenCalled_ShouldCreateValidApplicationInstance() {
    // Test that we can create a new instance of the application
    DwApplication newApp = new DwApplication();
    assertThat(newApp).isNotNull();
    assertThat(newApp.getName()).isEqualTo("dw-application");
  }

  @Test
  void dwConfiguration_WhenInstantiated_ShouldCreateValidInstance() {
    // Test that we can create a configuration instance
    DwConfiguration config = new DwConfiguration();
    assertThat(config).isNotNull();
  }

  private void suppressStdout(Runnable runnable) {
    PrintStream originalOut = System.out;
    try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream nullOut = new PrintStream(outputStream)) {
      System.setOut(nullOut);
      runnable.run();
    } catch (Exception e) {
      // ByteArrayOutputStream.close() shouldn't throw, but handle just in case
      throw new StdoutSuppressionException("Failed to suppress stdout during test", e);
    } finally {
      System.setOut(originalOut);
    }
  }

  private static class StdoutSuppressionException extends RuntimeException {
    public StdoutSuppressionException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
