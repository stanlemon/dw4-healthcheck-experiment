package com.stanlemon.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;

import com.tngtech.archunit.lang.ArchRule;

/**
 * Factory methods for creating reusable coding standard rules.
 *
 * <p>These rules enforce general coding best practices that should apply to most Java projects.
 */
public final class CodingRules {

  private CodingRules() {
    // Utility class
  }

  /**
   * Rule: No classes should use java.util.Date.
   *
   * <p>Modern date/time API (java.time) should be used instead.
   *
   * @return ArchRule that fails if any class uses java.util.Date
   */
  public static ArchRule noJavaUtilDate() {
    return noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("java.util.Date")
        .because("Modern date/time API (java.time) should be used instead of java.util.Date");
  }

  /**
   * Rule: No classes should throw generic exceptions.
   *
   * <p>Specific exception types provide better error handling.
   *
   * @return ArchRule that fails if any class throws generic exceptions
   */
  public static ArchRule noGenericExceptions() {
    return NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
  }

  /**
   * Rule: No classes should use java.util.logging.
   *
   * <p>Use SLF4J or another logging framework instead.
   *
   * @return ArchRule that fails if any class uses java.util.logging
   */
  public static ArchRule noJavaUtilLogging() {
    return NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
  }

  /**
   * Rule: No classes should use System.out or System.err.
   *
   * <p>Use proper logging framework instead.
   *
   * @return ArchRule that fails if any class uses standard streams
   */
  public static ArchRule noStandardStreams() {
    return NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
  }

  /**
   * Rule: No classes should use Thread.sleep().
   *
   * <p>Thread.sleep() makes tests flaky and should be avoided.
   *
   * @param exceptions Classes that are allowed to use Thread.sleep()
   * @return ArchRule that fails if any class (except specified exceptions) uses Thread.sleep()
   */
  public static ArchRule noThreadSleep(Class<?>... exceptions) {
    com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass>
        notExcepted =
            com.tngtech.archunit.base.DescribedPredicate
                .<com.tngtech.archunit.core.domain.JavaClass>alwaysTrue()
                .as("not excepted");

    for (Class<?> exception : exceptions) {
      notExcepted =
          notExcepted.and(
              com.tngtech.archunit.base.DescribedPredicate.not(
                  com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo(exception)));
    }

    return noClasses()
        .that(notExcepted)
        .should()
        .callMethod(Thread.class, "sleep", long.class)
        .because("Thread.sleep() should be avoided - use proper synchronization or Awaitility");
  }

  /**
   * Rule: Fields should not be public.
   *
   * <p>Public fields violate encapsulation. Use getters/setters or Lombok.
   *
   * @return ArchRule that fails if any non-static, non-final field is public
   */
  public static ArchRule noPublicFields() {
    return fields()
        .that()
        .areDeclaredInClassesThat()
        .areNotEnums()
        .and()
        .areNotStatic()
        .and()
        .areNotFinal()
        .should()
        .notBePublic()
        .allowEmptyShould(true)
        .because("Public fields violate encapsulation");
  }

  /**
   * Rule: Use AssertJ instead of JUnit assertions.
   *
   * <p>AssertJ provides more readable and fluent assertions.
   *
   * @return ArchRule that fails if any class uses JUnit assertions
   */
  public static ArchRule useAssertJNotJUnit() {
    return noClasses()
        .should()
        .dependOnClassesThat()
        .haveFullyQualifiedName("org.junit.jupiter.api.Assertions")
        .because("AssertJ should be used for assertions instead of JUnit assertions");
  }
}
