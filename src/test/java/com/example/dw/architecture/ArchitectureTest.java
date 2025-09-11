package com.example.dw.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.ws.rs.Path;
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
            .importPackages("com.example.dw");
  }

  @Test
  @DisplayName("Resources should only depend on service interfaces")
  void resourcesShouldOnlyDependOnServiceInterfaces() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..resources..")
            .and()
            .areNotAssignableTo(com.example.dw.DwApplication.class)
            .should()
            .dependOnClassesThat()
            .haveNameMatching(".*Default.*")
            .because(
                "Resources should only depend on service interfaces, not concrete implementations");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Resources should not access implementation packages directly")
  void resourcesShouldNotAccessImplementationPackagesDirectly() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..resources..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..impl..", "..internal..", "..private..")
            .because("Resources should not access internal implementation packages");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Implementation details should be hidden")
  void implementationDetailsShouldBeHidden() {
    ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..resources..")
            .and()
            .areNotAssignableTo(com.example.dw.DwApplication.class)
            .should()
            .dependOnClassesThat()
            .haveNameMatching(".*Default.*")
            .because("Resources should not depend on implementation details");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Exception mappers should reside in exceptions package")
  void exceptionMappersShouldResideInExceptionsPackage() {
    ArchRule rule =
        classes()
            .that()
            .haveNameMatching(".*ExceptionMapper")
            .should()
            .resideInAPackage("..exceptions..")
            .because("Exception mappers should be organized in the exceptions package");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Naming conventions should be followed")
  void namingConventionsShouldBeFollowed() {
    ArchRule resourceRule =
        classes()
            .that()
            .resideInAPackage("..resources..")
            .and()
            .areNotMemberClasses()
            .should()
            .haveSimpleNameEndingWith("Resource")
            .because("Resource classes should end with 'Resource'");

    ArchRule serviceRule =
        classes()
            .that()
            .resideInAPackage("..metrics..")
            .and()
            .areNotInterfaces()
            .should()
            .haveSimpleNameEndingWith("Service")
            .because("Service implementation classes should end with 'Service'");

    resourceRule.check(importedClasses);
    serviceRule.check(importedClasses);
  }

  @Test
  @DisplayName("Resources should be REST controllers")
  void resourcesShouldBeRestControllers() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..resources..")
            .and()
            .areNotMemberClasses()
            .should()
            .beAnnotatedWith(Path.class)
            .because("Resource classes should be REST controllers with Path annotation");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No circular dependencies between packages")
  void noCircularDependenciesBetweenPackages() {
    ArchRule rule =
        slices()
            .matching("com.example.dw.(*)..")
            .should()
            .beFreeOfCycles()
            .because(
                "Circular dependencies between packages make the code harder to maintain and test");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Proper layer dependency enforcement")
  void properLayerDependencyEnforcement() {
    ArchRule resourcesToServicesRule =
        noClasses()
            .that()
            .resideInAPackage("..resources..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..filters..", "..health..")
            .because(
                "Resources should not depend on filters or health checks - dependencies should flow downward");

    ArchRule servicesToResourcesRule =
        noClasses()
            .that()
            .resideInAPackage("..metrics..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..resources..")
            .because(
                "Services should not depend on resource layer - dependencies should flow upward");

    ArchRule filtersToResourcesRule =
        noClasses()
            .that()
            .resideInAPackage("..filters..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("..resources..")
            .because("Filters should not depend on resources - filters are infrastructure layer");

    resourcesToServicesRule.check(importedClasses);
    servicesToResourcesRule.check(importedClasses);
    filtersToResourcesRule.check(importedClasses);
  }

  @Test
  @DisplayName("Service interface pattern enforcement")
  void serviceInterfacePatternEnforcement() {
    ArchRule rule =
        classes()
            .that()
            .resideInAPackage("..metrics..")
            .and()
            .areInterfaces()
            .should()
            .haveSimpleNameEndingWith("Service")
            .because("Service interfaces should follow naming convention ending with 'Service'");

    rule.check(importedClasses);
  }
}
