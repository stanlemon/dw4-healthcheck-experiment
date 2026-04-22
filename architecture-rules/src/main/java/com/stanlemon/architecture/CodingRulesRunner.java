package com.stanlemon.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import java.util.List;

/**
 * Runs all standard coding rules against a module's classes.
 *
 * <p>Eliminates test duplication across modules by providing a single method that runs all common
 * coding standard checks.
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
    List<ArchRule> rules =
        List.of(
            CodingRules.noJavaUtilDate(),
            CodingRules.noGenericExceptions(),
            CodingRules.noJavaUtilLogging(),
            CodingRules.noStandardStreams(),
            CodingRules.noThreadSleep(threadSleepExceptions),
            CodingRules.noPublicFields(),
            CodingRules.useAssertJNotJUnit());

    for (ArchRule rule : rules) {
      rule.check(classes);
    }
  }
}
