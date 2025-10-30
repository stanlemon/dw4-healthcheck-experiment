package com.stanlemon.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Factory methods for creating reusable architecture rules.
 *
 * <p>These rules enforce architectural patterns and package organization.
 */
public final class ArchitectureRules {

  private ArchitectureRules() {
    // Utility class
  }

  /**
   * Rule: No circular dependencies between packages.
   *
   * <p>Circular dependencies make code harder to maintain and test.
   *
   * @param packagePattern Pattern for packages to check (e.g., "com.example.(*)..")
   * @return ArchRule that fails if circular dependencies exist
   */
  public static ArchRule noCircularDependencies(String packagePattern) {
    return slices()
        .matching(packagePattern)
        .should()
        .beFreeOfCycles()
        .because(
            "Circular dependencies between packages make the code harder to maintain and test");
  }

  /**
   * Rule: Service implementation classes should end with "Service".
   *
   * @param servicePackage Package pattern where services reside (e.g., "..metrics..")
   * @return ArchRule that enforces naming convention for service implementations
   */
  public static ArchRule servicesEndWithService(String servicePackage) {
    return classes()
        .that()
        .resideInAPackage(servicePackage)
        .and()
        .areNotInterfaces()
        .should()
        .haveSimpleNameEndingWith("Service")
        .because("Service implementation classes should end with 'Service'");
  }

  /**
   * Rule: Service interfaces should end with "Service".
   *
   * @param servicePackage Package pattern where services reside (e.g., "..metrics..")
   * @return ArchRule that enforces naming convention for service interfaces
   */
  public static ArchRule serviceInterfacesEndWithService(String servicePackage) {
    return classes()
        .that()
        .resideInAPackage(servicePackage)
        .and()
        .areInterfaces()
        .should()
        .haveSimpleNameEndingWith("Service")
        .because("Service interfaces should follow naming convention ending with 'Service'");
  }

  /**
   * Rule: No access to internal/impl/private packages.
   *
   * <p>Internal implementation details should not be accessed directly.
   *
   * @param fromPackage Package that should not access internal packages (e.g., "..api..")
   * @return ArchRule that prevents access to internal packages
   */
  public static ArchRule noAccessToInternalPackages(String fromPackage) {
    return noClasses()
        .that()
        .resideInAPackage(fromPackage)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..impl..", "..internal..", "..private..")
        .because("Internal implementation packages should not be accessed directly");
  }

  /**
   * Rule: Classes with specific name pattern should not depend on implementations.
   *
   * @param classPattern Name pattern for classes (e.g., ".*Resource")
   * @param implementationPattern Pattern for implementation classes (e.g., ".*Default.*")
   * @param reason Reason for the rule
   * @return ArchRule that enforces interface usage over implementations
   */
  public static ArchRule noDependencyOnImplementations(
      String classPattern, String implementationPattern, String reason) {
    return noClasses()
        .that()
        .haveNameMatching(classPattern)
        .should()
        .dependOnClassesThat()
        .haveNameMatching(implementationPattern)
        .because(reason);
  }

  /**
   * Rule: Classes should not depend on classes in specific packages.
   *
   * @param fromPackage Source package pattern (e.g., "..services..")
   * @param toPackages Target packages that should not be accessed (e.g., "..resources..",
   *     "..controllers..")
   * @param reason Reason for the rule
   * @return ArchRule that enforces layer isolation
   */
  public static ArchRule noLayerViolation(String fromPackage, String[] toPackages, String reason) {
    return noClasses()
        .that()
        .resideInAPackage(fromPackage)
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(toPackages)
        .because(reason);
  }

  /**
   * Rule: Classes in a package should have specific name ending.
   *
   * @param packagePattern Package pattern (e.g., "..resources..")
   * @param nameSuffix Required name suffix (e.g., "Resource")
   * @param reason Reason for the rule
   * @return ArchRule that enforces naming conventions
   */
  public static ArchRule classesInPackageShouldEndWith(
      String packagePattern, String nameSuffix, String reason) {
    return classes()
        .that()
        .resideInAPackage(packagePattern)
        .and()
        .areNotMemberClasses()
        .should()
        .haveSimpleNameEndingWith(nameSuffix)
        .because(reason);
  }

  /**
   * Rule: Classes with specific name should be in specific package.
   *
   * @param namePattern Name pattern (e.g., ".*ExceptionMapper")
   * @param packagePattern Package pattern (e.g., "..exceptions..")
   * @param reason Reason for the rule
   * @return ArchRule that enforces package organization
   */
  public static ArchRule classesWithNameShouldBeInPackage(
      String namePattern, String packagePattern, String reason) {
    return classes()
        .that()
        .haveNameMatching(namePattern)
        .should()
        .resideInAPackage(packagePattern)
        .because(reason);
  }
}
