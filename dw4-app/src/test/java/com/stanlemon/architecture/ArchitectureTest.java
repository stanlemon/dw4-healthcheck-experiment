package com.stanlemon.architecture;

import static com.stanlemon.architecture.ArchitectureRules.classesInPackageShouldEndWith;
import static com.stanlemon.architecture.ArchitectureRules.classesWithNameShouldBeInPackage;
import static com.stanlemon.architecture.ArchitectureRules.noAccessToInternalPackages;
import static com.stanlemon.architecture.ArchitectureRules.noCircularDependencies;
import static com.stanlemon.architecture.ArchitectureRules.noDependencyOnImplementations;
import static com.stanlemon.architecture.ArchitectureRules.noLayerViolation;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

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
            .importPackages("com.stanlemon.healthy.dw4app");
  }

  @Test
  @DisplayName("Resources should only depend on service interfaces")
  void resourcesShouldOnlyDependOnServiceInterfaces() {
    ArchRule rule =
        noDependencyOnImplementations(
            ".*Resource",
            ".*Default.*",
            "Resources should only depend on service interfaces, not concrete implementations");
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Resources should not access implementation packages directly")
  void resourcesShouldNotAccessImplementationPackagesDirectly() {
    ArchRule rule = noAccessToInternalPackages("..resources..");
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Exception mappers should reside in exceptions package")
  void exceptionMappersShouldResideInExceptionsPackage() {
    ArchRule rule =
        classesWithNameShouldBeInPackage(
            ".*ExceptionMapper",
            "..exceptions..",
            "Exception mappers should be organized in the exceptions package");
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Resources should follow naming conventions")
  void resourcesShouldFollowNamingConventions() {
    ArchRule rule =
        classesInPackageShouldEndWith(
            "..resources..", "Resource", "Resource classes should end with 'Resource'");
    rule.check(importedClasses);
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
    ArchRule rule = noCircularDependencies("com.stanlemon.healthy.dw4app.(*)..");
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Proper layer dependency enforcement")
  void properLayerDependencyEnforcement() {
    ArchRule resourcesToServicesRule =
        noLayerViolation(
            "..resources..",
            new String[] {"..filters..", "..health.."},
            "Resources should not depend on filters or health checks - dependencies should flow downward");

    ArchRule filtersToResourcesRule =
        noLayerViolation(
            "..filters..",
            new String[] {"..resources.."},
            "Filters should not depend on resources - filters are infrastructure layer");

    resourcesToServicesRule.check(importedClasses);
    filtersToResourcesRule.check(importedClasses);
  }
}
