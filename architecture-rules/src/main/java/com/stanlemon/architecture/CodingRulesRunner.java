package com.stanlemon.architecture;

import static com.stanlemon.architecture.CodingRules.noGenericExceptions;
import static com.stanlemon.architecture.CodingRules.noJavaUtilDate;
import static com.stanlemon.architecture.CodingRules.noJavaUtilLogging;
import static com.stanlemon.architecture.CodingRules.noPublicFields;
import static com.stanlemon.architecture.CodingRules.noStandardStreams;
import static com.stanlemon.architecture.CodingRules.noThreadSleep;
import static com.stanlemon.architecture.CodingRules.useAssertJNotJUnit;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Test suite runner for executing all standard coding rules against a module.
 *
 * <p>This eliminates test duplication across modules by providing a single method that runs all
 * common coding standard checks. Each module can call {@link #checkAllCodingStandards(JavaClasses,
 * Class[])} with its imported classes.
 */
public final class CodingRulesRunner {

  private CodingRulesRunner() {
    // Utility class
  }

  /**
   * Runs all standard coding rules against the provided classes.
   *
   * @param classes The JavaClasses to check (typically from ClassFileImporter)
   * @param threadSleepExceptions Classes allowed to use Thread.sleep() (e.g., demo resources)
   */
  public static void checkAllCodingStandards(
      JavaClasses classes, Class<?>... threadSleepExceptions) {
    checkNoJavaUtilDate(classes);
    checkNoGenericExceptions(classes);
    checkNoJavaUtilLogging(classes);
    checkNoStandardStreams(classes);
    checkNoThreadSleep(classes, threadSleepExceptions);
    checkNoPublicFields(classes);
    checkUseAssertJNotJUnit(classes);
  }

  /**
   * Checks that no classes use java.util.Date.
   *
   * @param classes The JavaClasses to check
   */
  public static void checkNoJavaUtilDate(JavaClasses classes) {
    ArchRule rule = noJavaUtilDate();
    rule.check(classes);
  }

  /**
   * Checks that no classes throw generic exceptions.
   *
   * @param classes The JavaClasses to check
   */
  public static void checkNoGenericExceptions(JavaClasses classes) {
    ArchRule rule = noGenericExceptions();
    rule.check(classes);
  }

  /**
   * Checks that no classes use java.util.logging.
   *
   * @param classes The JavaClasses to check
   */
  public static void checkNoJavaUtilLogging(JavaClasses classes) {
    ArchRule rule = noJavaUtilLogging();
    rule.check(classes);
  }

  /**
   * Checks that no classes use System.out or System.err.
   *
   * @param classes The JavaClasses to check
   */
  public static void checkNoStandardStreams(JavaClasses classes) {
    ArchRule rule = noStandardStreams();
    rule.check(classes);
  }

  /**
   * Checks that no classes use Thread.sleep().
   *
   * @param classes The JavaClasses to check
   * @param exceptions Classes allowed to use Thread.sleep()
   */
  public static void checkNoThreadSleep(JavaClasses classes, Class<?>... exceptions) {
    ArchRule rule = noThreadSleep(exceptions);
    rule.check(classes);
  }

  /**
   * Checks that no classes have public fields.
   *
   * @param classes The JavaClasses to check
   */
  public static void checkNoPublicFields(JavaClasses classes) {
    ArchRule rule = noPublicFields();
    rule.check(classes);
  }

  /**
   * Checks that no classes use JUnit assertions instead of AssertJ.
   *
   * @param classes The JavaClasses to check
   */
  public static void checkUseAssertJNotJUnit(JavaClasses classes) {
    ArchRule rule = useAssertJNotJUnit();
    rule.check(classes);
  }
}
