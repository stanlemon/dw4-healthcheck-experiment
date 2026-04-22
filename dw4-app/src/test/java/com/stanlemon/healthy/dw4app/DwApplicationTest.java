package com.stanlemon.healthy.dw4app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Dw Application Tests")
class DwApplicationTest {

  @Test
  @DisplayName("Application should have correct name and be instantiable")
  void application_ShouldBeInstantiableWithCorrectName() {
    DwApplication app = new DwApplication();

    assertThat(app.getName()).isEqualTo("dw-application");
    assertThat(app).isInstanceOf(io.dropwizard.core.Application.class);
  }
}
