package com.stanlemon.architecture;

import static com.stanlemon.architecture.CodingRules.noGenericExceptions;
import static com.stanlemon.architecture.CodingRules.noJavaUtilDate;
import static com.stanlemon.architecture.CodingRules.noJavaUtilLogging;
import static com.stanlemon.architecture.CodingRules.noPublicFields;
import static com.stanlemon.architecture.CodingRules.noStandardStreams;
import static com.stanlemon.architecture.CodingRules.noThreadSleep;
import static com.stanlemon.architecture.CodingRules.useAssertJNotJUnit;

import com.stanlemon.healthy.spring3app.resources.SlowResource;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Coding Rules")
class CodingRulesTest {

  private static JavaClasses importedClasses;

  @BeforeAll
  static void setup() {
    importedClasses = new ClassFileImporter().importPackages("com.stanlemon.healthy.spring3app");
  }

  @Test
  @DisplayName("No java.util.Date should be used")
  void noJavaUtilDateUsage() {
    ArchRule rule = noJavaUtilDate();
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No generic exceptions should be thrown")
  void noGenericExceptionUsage() {
    ArchRule rule = noGenericExceptions();
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No java.util.logging should be used")
  void noJavaUtilLoggingUsage() {
    ArchRule rule = noJavaUtilLogging();
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No System.out or System.err should be used")
  void noStandardStreamsUsage() {
    ArchRule rule = noStandardStreams();
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No Thread.sleep should be used except in SlowResource")
  void noThreadSleepUsage() {
    // SlowResource is an intentional exception since it's designed to simulate latency
    ArchRule rule = noThreadSleep(SlowResource.class);
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No public fields should be used")
  void noPublicFieldsUsage() {
    ArchRule rule = noPublicFields();
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("AssertJ should be used instead of JUnit assertions")
  void useAssertJNotJUnitUsage() {
    ArchRule rule = useAssertJNotJUnit();
    rule.check(importedClasses);
  }
}
