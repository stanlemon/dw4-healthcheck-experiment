# ArchUnit Integration Guide

This project uses [ArchUnit](https://www.archunit.org/) to run its architecture rules and
coding-standard rules as tests. If a class breaks a rule, the test fails.

> Note on JUnit 6 compatibility: ArchUnit 1.5.1 was compiled against JUnit Platform 1.14.x.
> This project runs JUnit Platform 6.1.3. The TestEngine API classes that ArchUnit calls
> are still in Platform 6, so the architecture tests pass today. But ArchUnit does not
> officially support this combination. When a stable (GA) release of ArchUnit that targets
> JUnit 6 is available, upgrade ArchUnit. You can follow the status at
> <https://github.com/TNG/ArchUnit/issues>.

## Project layout

The rules are in the `architecture-rules/` module. A factory method is a method that
builds and returns an object. The module has three classes:

- `ArchitectureRules.java` has factory methods that return `ArchRule`s that each module
  can use again (for example `noContainerFrameworkDependencies`, `noCircularDependencies`,
  `servicesEndWithService`).
- `CodingRules.java` has the factory methods for the coding-standard rules.
- `CodingRulesRunner.java` is a helper class. One call applies all of the coding rules to
  a set of imported classes.

Each of the other modules has two test classes. Each test class imports those factory
methods and applies the rules to the classes of its module:

| Module | Architecture tests | Coding-rules tests |
| --- | --- | --- |
| `healthy-metrics` | `ArchitectureTest` | `CodingRulesTest` |
| `healthy-hangar` | `ArchitectureTest` | `CodingRulesTest` |
| `dw5-app` | `ArchitectureTest` | `CodingRulesTest` |
| `spring4-app` | `ArchitectureTest` | `CodingRulesTest` |

All of these test classes are in the `com.stanlemon.architecture` package, in the test
sources of their module. Each test class imports only the production classes of its
module. The rules do not examine test classes.

## Rules enforced

### Coding standards (`CodingRules`)

Each `CodingRulesTest` applies these rules through
`CodingRulesRunner.checkAllCodingStandards(importedClasses, /* exceptions */ ...)`:

- No `java.util.Date`. Use `java.time`.
- No generic exceptions. Do not use `throw new Exception(...)` or `RuntimeException(...)`.
- No `java.util.logging`. Use SLF4J.
- No `System.out` or `System.err`. Use the logger.
- No `Thread.sleep(...)` in production code. In `dw5-app` and `spring4-app`, the
  `SlowResource` class is the one permitted exception, because it is a demo.
- No public fields, except fields that are static or final, and fields in enums. Use
  accessor methods (methods that read or change a field).
- No direct JUnit assertions from `org.junit.jupiter.api.Assertions` (`assertEquals`, and
  the others in that class). Use AssertJ.

### Architectural patterns (`ArchitectureRules`)

A shared domain module is a module that both apps use. The shared domain modules are
`healthy-metrics` and `healthy-hangar`.

- `noContainerFrameworkDependencies(basePackage)`: the shared domain modules must not
  depend on `org.springframework`, `io.dropwizard`, `jakarta.ws.rs`, or `org.glassfish`.
  This rule keeps those modules free of framework code. Jackson annotations and
  `jakarta.validation` annotations are not banned. The rule permits them on purpose, so
  that DTOs (objects that carry data in and out of the API) can work with both
  frameworks.
- `noCircularDependencies(packagePattern)`: no cycles between packages. All four modules
  apply this rule.
- `servicesEndWithService(servicePackage)`: if a class implements a `*Service` interface,
  the name of that class must also end with `Service`. Thus the rule examines
  `DefaultMetricsService` and `DefaultHangarService`, but not plain value types. The two
  shared domain modules apply this rule.
- `serviceInterfacesEndWithService(servicePackage)`: the name of each interface in the
  package must end with `Service`. Only `healthy-metrics` applies this rule.

The app modules (`dw5-app` and `spring4-app`) also apply these rules:

- `@Path` is on every Dropwizard resource (JAX-RS) in `..resources..`, and
  `@RestController` is on every Spring resource in `..resources..`.
- `classesInPackageShouldEndWith`: the name of each class in `..resources..` ends with
  `Resource`.
- `noDependencyOnImplementations`: `*Resource` classes do not depend on `Default*`
  implementations. The callers use the interfaces, so you can replace the default
  implementation.
- `noAccessToInternalPackages`: resources do not depend on `..impl..`, `..internal..`, or
  `..private..` packages.
- `classesWithNameShouldBeInPackage`: exception mappers (`*ExceptionMapper` in `dw5-app`)
  and exception handlers (`*ExceptionHandler` in `spring4-app`) are in the `exceptions`
  package.
- `noLayerViolation`: resources do not depend on `..filters..` or `..health..`, and
  filters do not depend on `..resources..`.

The `healthy-hangar` module also has two rules of its own:

- The name of each interface ends with `Service` or `Predictor`.
- Production code does not depend directly on the `Default*` predictors. Callers use the
  `AerodynamicsPredictor` interface, so you can replace the default.

## Running

The first command runs all of the tests in all of the modules. The architecture tests are
part of this run. The second command runs only the architecture tests of one module.
Replace `healthy-hangar` with the name of the module that you want to test.

```bash
# Full reactor test run (includes architecture tests)
mvn test

# Just the architecture package, one module
mvn test -pl healthy-hangar -Dtest='com.stanlemon.architecture.*'
```

## Adding a new rule

1. Add a factory method to `architecture-rules/.../ArchitectureRules.java` or
   `CodingRules.java`. Do not put a package name in the rule, so that each module can use
   it.
2. For an `ArchitectureRules` method, call the method from
   `com.stanlemon.architecture.ArchitectureTest` in each module that the rule applies to.
   If the rule can have zero matches in a module, use `rule.allowEmptyShould(true)`.
3. For a `CodingRules` method, add the method to the list in
   `CodingRulesRunner.checkAllCodingStandards`. Then each `CodingRulesTest` applies it.
4. Run `mvn test -pl <module>`, and make sure that the rule finds the classes you expect.

Example:

```java
@Test
@DisplayName("Resources should not use static methods")
void resourcesShouldNotUseStaticMethods() {
  ArchRule rule =
      methods()
          .that()
          .areDeclaredInClassesThat()
          .resideInAPackage("..resources..")
          .should()
          .notBeStatic()
          .because("Resources should use dependency injection");
  rule.check(importedClasses);
}
```
