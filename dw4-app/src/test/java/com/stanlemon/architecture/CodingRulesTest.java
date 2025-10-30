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
            .importPackages("com.stanlemon.healthy.dw4app");
  }

  @Test
  @DisplayName("Should follow all coding standards")
  void shouldFollowAllCodingStandards() {
    // Run all standard coding rules with SlowResource exception for Thread.sleep()
    CodingRulesRunner.checkAllCodingStandards(
        importedClasses, com.stanlemon.healthy.dw4app.resources.SlowResource.class);
  }
}
