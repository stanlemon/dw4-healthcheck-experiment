package com.stanlemon.architecture;

import static com.stanlemon.architecture.ArchitectureRules.noCircularDependencies;
import static com.stanlemon.architecture.ArchitectureRules.serviceInterfacesEndWithService;
import static com.stanlemon.architecture.ArchitectureRules.servicesEndWithService;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Architecture Rules")
class ArchitectureTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setup() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(location -> !location.contains("/test-classes/"))
            .importPackages("com.stanlemon.healthy.metrics");
  }

  @Test
  @DisplayName("Should have no circular dependencies")
  void shouldHaveNoCircularDependencies() {
    ArchRule rule =
        noCircularDependencies("com.stanlemon.healthy.metrics.(*)..").allowEmptyShould(true);
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Service implementations should end with 'Service'")
  void serviceImplementationsShouldEndWithService() {
    ArchRule rule = servicesEndWithService("..metrics..");
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Service interfaces should end with 'Service'")
  void serviceInterfacesShouldEndWithService() {
    ArchRule rule = serviceInterfacesEndWithService("..metrics..");
    rule.check(importedClasses);
  }
}
