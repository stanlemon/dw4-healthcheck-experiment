# ArchUnit Integration Guide

Architecture and coding-standard rules are enforced as executable tests via [ArchUnit](https://www.archunit.org/).

> **Note on JUnit 6 compatibility:** ArchUnit 1.4.2 was compiled against JUnit Platform
> 1.14.x. This project runs JUnit Platform 6.0.3; the TestEngine API classes ArchUnit calls
> still exist in Platform 6, so the architecture tests pass today, but the combination is
> not officially supported. Upgrade when a GA ArchUnit release that targets JUnit 6 is
> available (track: <https://github.com/TNG/ArchUnit/issues>).

## Project layout

The rules themselves live in the `architecture-rules/` module:

- `ArchitectureRules.java` — factory methods returning reusable `ArchRule`s
  (e.g. `noContainerFrameworkDependencies`, `noCircularDependencies`, `servicesEndWithService`).
- `CodingRules.java` — factory methods for the coding-standard rules.
- `CodingRulesRunner.java` — a helper that applies the full coding-rules suite to a given
  package in one call.

Each module then has its own test class or two that imports those factories and applies
them to its own classes:

| Module | Architecture tests | Coding-rules tests |
| --- | --- | --- |
| `healthy-metrics` | `ArchitectureTest` | `CodingRulesTest` |
| `healthy-hangar` | `ArchitectureTest` | `CodingRulesTest` |
| `dw5-app` | `ArchitectureTest` | `CodingRulesTest` |
| `spring4-app` | `ArchitectureTest` | `CodingRulesTest` |

All test classes live under `com.stanlemon.architecture` in their module's test sources.

## Rules enforced

### Coding standards (`CodingRules`)

Applied via `CodingRulesRunner.checkAllCodingStandards(importedClasses, /* exceptions */ ...)`:

- No `java.util.Date` — use `java.time`.
- No generic exceptions — no `throw new Exception(...)` / `RuntimeException(...)`.
- No `java.util.logging` — use SLF4J.
- No `System.out` / `System.err` — use the logger.
- No `Thread.sleep(...)` in tests (`SlowResource` is the documented demo exception).
- No public fields — enforce encapsulation via accessors.
- No direct JUnit assertions (`assertEquals`, etc.) — use AssertJ.

### Architectural patterns (`ArchitectureRules`)

- **`noContainerFrameworkDependencies(basePackage)`** — the shared domain modules
  (`healthy-metrics`, `healthy-hangar`) must not depend on `org.springframework`,
  `io.dropwizard`, `jakarta.ws.rs`, or `org.glassfish`. This guards the
  framework-agnostic invariant of those modules. Note: Jackson annotations and
  `jakarta.validation` annotations are *not* banned — they are deliberately allowed so
  DTOs can remain cross-framework compatible.
- **`noCircularDependencies(packagePattern)`** — no cycles between packages.
- **`servicesEndWithService(servicePackage)`** — implementation classes that implement
  a `*Service` interface must themselves end with `Service` (so `DefaultMetricsService`
  and `DefaultHangarService` are checked, but plain value types aren't).

App-specific architecture tests also enforce:

- **`@Path` present** on every Dropwizard resource (JAX-RS), or `@RestController`
  present on every Spring controller.
- **No dependency on `Default*` implementations** from production code — callers
  use interfaces so the default can be swapped.
- **Exception mappers live in the `exceptions` package**.

## Running

```bash
# Full reactor test run (includes architecture tests)
mvn test

# Just the architecture package, one module
mvn test -pl healthy-hangar -Dtest='com.stanlemon.architecture.*'
```

## Adding a new rule

1. Add a factory method to `architecture-rules/.../ArchitectureRules.java` or
   `CodingRules.java`. Keep the rule package-agnostic so every module can reuse it.
2. Call the factory from `com.stanlemon.architecture.ArchitectureTest` in whichever
   module(s) the rule applies to. Use `rule.allowEmptyShould(true)` if the rule could
   have zero matches in some modules.
3. Run `mvn test -pl <module>` to confirm the rule fires correctly.

Example:

```java
@Test
@DisplayName("Resources should not use static methods")
void resourcesShouldNotUseStaticMethods() {
  ArchRule rule =
      noClasses()
          .that()
          .resideInAPackage("..resources..")
          .should()
          .haveMethodsThat()
          .areStatic()
          .because("Resources should use dependency injection");
  rule.check(importedClasses);
}
```
