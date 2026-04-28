package com.stanlemon.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Coding Rules")
class CodingRulesTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setup() {
    importedClasses =
        new ClassFileImporter()
            .withImportOption(location -> !location.contains("/test-classes/"))
            .importPackages("com.stanlemon.healthy.spring4app");
  }

  @Test
  @DisplayName("Should follow all coding standards")
  void shouldFollowAllCodingStandards() {
    CodingRulesRunner.checkAllCodingStandards(
        importedClasses, com.stanlemon.healthy.spring4app.resources.SlowResource.class);
  }
}
