package com.example.dw.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.*;

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
    importedClasses =
        new ClassFileImporter()
            .withImportOption(location -> !location.contains("/test-classes/"))
            .importPackages("com.example.dw");
  }

  @Test
  @DisplayName("No classes should use java.util.Date")
  void noClassesShouldUseJavaUtilDate() {
    ArchRule rule =
        noClasses()
            .that()
            .areNotAssignableTo(CodingRulesTest.class)
            .should()
            .dependOnClassesThat()
            .areAssignableTo(java.util.Date.class)
            .because("Modern date/time API (java.time) should be used instead of java.util.Date");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No classes should throw generic exceptions")
  void noClassesShouldThrowGenericExceptions() {
    ArchRule rule = NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS;
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No classes should use java.util.logging")
  void noClassesShouldUseJavaUtilLogging() {
    ArchRule rule = NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING;
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No classes should use System.out or System.err")
  void noClassesShouldUseStandardStreams() {
    ArchRule rule = NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;
    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No classes should use Thread.sleep()")
  void noClassesShouldUseThreadSleep() {
    ArchRule rule =
        noClasses()
            .that()
            .areNotAssignableTo(com.example.dw.resources.SlowResource.class)
            .should()
            .callMethod(Thread.class, "sleep", long.class)
            .because("Thread.sleep() should be avoided as mentioned in development guidelines");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("Fields should not be public")
  void fieldsShouldNotBePublic() {
    ArchRule rule =
        fields()
            .that()
            .areDeclaredInClassesThat()
            .areNotEnums()
            .and()
            .areNotStatic()
            .and()
            .areNotFinal()
            .should()
            .notBePublic()
            .because("Public fields violate encapsulation");

    rule.check(importedClasses);
  }

  @Test
  @DisplayName("No classes should directly use JUnit assertions")
  void noClassesShouldDirectlyUseJunitAssertions() {
    ArchRule rule =
        noClasses()
            .that()
            .areNotAssignableTo(CodingRulesTest.class)
            .should()
            .dependOnClassesThat()
            .areAssignableTo(org.junit.jupiter.api.Assertions.class)
            .because(
                "AssertJ should be used for assertions as mentioned in development guidelines");

    rule.check(importedClasses);
  }
}
