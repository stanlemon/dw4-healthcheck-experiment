package com.stanlemon.architecture;

import static com.stanlemon.architecture.ArchitectureRules.noCircularDependencies;
import static com.stanlemon.architecture.ArchitectureRules.servicesEndWithService;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

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
            .importPackages("com.stanlemon.healthy.hangar");
  }

  @Test
  @DisplayName("Should have no circular dependencies")
  void shouldHaveNoCircularDependencies() {
    ArchRule rule =
        noCircularDependencies("com.stanlemon.healthy.hangar.(*)..").allowEmptyShould(true);
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Service implementations should end with 'Service'")
  void serviceImplementationsShouldEndWithService() {
    ArchRule rule = servicesEndWithService("..hangar..");
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Interfaces should end with 'Service' or 'Predictor'")
  void serviceInterfacesFollowNamingConvention() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..hangar..")
            .and()
            .areInterfaces()
            .should()
            .haveSimpleNameEndingWith("Service")
            .orShould()
            .haveSimpleNameEndingWith("Predictor")
            .allowEmptyShould(true)
            .because("Interfaces in the hangar domain follow Service/Predictor naming conventions");
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Shared module must not depend on any framework-specific packages")
  void sharedModuleShouldNotDependOnFrameworks() {
    ArchRule rule = ArchitectureRules.noFrameworkDependencies("com.stanlemon.healthy.hangar");
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Production code should not depend on Default* predictors directly")
  void productionCodeShouldNotDependOnDefaultPredictors() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..hangar..")
            .and()
            .haveSimpleNameNotStartingWith("Default")
            .should()
            .dependOnClassesThat()
            .haveSimpleNameStartingWith("Default")
            .andShould()
            .dependOnClassesThat()
            .haveSimpleNameEndingWith("Predictor")
            .allowEmptyShould(true)
            .because(
                "Default* predictor implementations are stand-ins for real services; production "
                    + "code should depend on the AerodynamicsPredictor interface so the default "
                    + "can be swapped out without touching callers.");
    rule.check(importedClasses);
  }
}
