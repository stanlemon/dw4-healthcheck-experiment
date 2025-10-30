# ArchUnit Integration Guide

This guide explains the ArchUnit architecture tests implemented in this project for enforcing coding standards and architectural constraints.

## What is ArchUnit?

ArchUnit is a Java library for testing architecture and coding rules. It allows you to express architectural assertions as readable and testable rules directly in your test code.

## Current ArchUnit Tests

This project has six comprehensive ArchUnit test classes:

### CodingRulesTest
Enforces general coding standards and best practices:

- **No java.util.Date usage** - Enforces use of modern `java.time` API
- **No generic exceptions** - Prevents throwing `Exception`, `RuntimeException`, etc.
- **No java.util.logging** - Enforces use of SLF4J/Logback logging
- **No System.out/System.err** - Prevents console output in production code
- **No Thread.sleep()** - Prevents flaky tests (except in SlowResource for demonstration)
- **No public fields** - Enforces encapsulation (excludes enums and constants)
- **No JUnit assertions** - Enforces use of AssertJ for better readability

### ArchitectureTest
Enforces architectural patterns and package structure:

- **Resource dependency isolation** - Resources should only depend on service interfaces, not implementations
- **Implementation hiding** - Resources cannot depend on classes with "Default" in the name
- **Package organization** - Exception mappers must reside in the exceptions package
- **Naming conventions** - Resources end with "Resource", services end with "Service"
- **REST controller patterns** - Resources must be annotated with `@Path`
- **Circular dependency prevention** - No circular dependencies between packages
- **Layer dependency enforcement** - Proper dependency flow between architectural layers
- **Service interface patterns** - Service interfaces follow naming conventions

### SecurityRulesTest
Enforces security best practices:

- **No hardcoded secrets** - Prevents hardcoded passwords, API keys, tokens, or secrets
- **SQL injection prevention** - Prevents dangerous string concatenation with SQL keywords
- **Safe reflection usage** - Prevents unsafe reflection methods like `setAccessible`
- **Database credential externalization** - Ensures database credentials are not hardcoded

### DropwizardRulesTest
Enforces Dropwizard-specific patterns:

- **HealthCheck extension** - Health checks must extend Dropwizard's HealthCheck
- **Provider annotation** - Exception mappers must be annotated with `@Provider`
- **HTTP method annotations** - Resources must use proper JAX-RS annotations
- **Configuration inheritance** - Configuration classes must extend Dropwizard's Configuration
- **Application structure** - Application classes must extend Dropwizard's Application
- **Managed lifecycle** - Managed objects must implement Managed interface

### LombokRulesTest
Enforces proper Lombok usage patterns:

- **Data class annotations** - Prevents manual getter/setter implementations when Lombok is available
- **Builder pattern** - Enforces use of `@Builder` for builder patterns
- **No manual boilerplate** - Prevents manual `equals`, `hashCode`, `toString` when using Lombok
- **Immutability patterns** - Enforces `@Value` for immutable classes
- **Response DTO immutability** - Response DTOs should use `@Value` for immutability

### TestQualityRulesTest
Enforces test quality and organization:

- **Test naming conventions** - `methodName_WhenCondition_ShouldExpectedResult` pattern
- **DisplayName annotations** - All test classes and methods must have descriptive names
- **Nested organization** - Complex test classes should use `@Nested` for organization
- **Mock usage limits** - Prevents over-mocking (max 3 mocks per test class)
- **Test package organization** - Integration tests in separate packages
- **Proper test setup** - Test classes should use `@BeforeEach` or `@BeforeAll`

### PerformanceRulesTest
Enforces performance best practices:

- **No reflection in hot paths** - Prevents reflection in performance-critical code
- **Concurrent collections** - Enforces thread-safe collections in multi-threaded code
- **Atomic operations** - Thread-safe counters should use `AtomicLong`
- **String concatenation** - Prevents inefficient string operations
- **Collection interfaces** - Use specific collection interfaces over general ones
- **Primitive types** - Avoid autoboxing in performance-critical code
- **Efficient data structures** - Use appropriate concurrent data structures

## Running ArchUnit Tests

### Running All Tests
```bash
# Run all tests including ArchUnit tests
mvn test
```

### Running Only Architecture Tests
```bash
# Run just the architecture tests
mvn test -Dtest=*Architecture*

# Or run specific test classes
mvn test -Dtest=CodingRulesTest
mvn test -Dtest=ArchitectureTest
```

### Convenience Script
```bash
# Use the provided script to run architecture tests specifically
./run-archunit-tests.sh
```

## Test Implementation Details

Both test classes follow a common pattern:

1. **Setup Phase**: Import all classes from `com.stanlemon.healthy.dw4app` package using `ClassFileImporter`
2. **Rule Definition**: Define architectural rules using ArchUnit's fluent API
3. **Rule Execution**: Apply rules to imported classes with descriptive failure messages

Example rule structure:
```java
@Test
@DisplayName("Descriptive test name")
void testMethodName() {
    ArchRule rule = noClasses()
        .that().meetSomeCondition()
        .should().notDoSomething()
        .because("Clear explanation of why this rule exists");
    
    rule.check(importedClasses);
}
```

## Key Architectural Constraints

### Dependency Management
- Resources only depend on service interfaces, not concrete implementations
- No circular dependencies between packages
- Implementation details are hidden from higher layers

### Naming Standards
- Resource classes: `*Resource`
- Service implementations: `*Service`
- Exception mappers: `*ExceptionMapper` (in exceptions package)

### Coding Standards
- Modern Java APIs (java.time instead of java.util.Date)
- Proper logging (SLF4J instead of System.out or java.util.logging)
- Strong typing (specific exceptions instead of generic ones)
- AssertJ assertions instead of JUnit assertions

## Adding New Rules

To add custom architecture tests:

1. Add new test methods to existing classes or create new test classes in `src/test/java/com/example/dw/architecture/`
2. Use ArchUnit's fluent API to define rules
3. Include clear `@DisplayName` annotations and descriptive `because()` clauses
4. Follow the established pattern of importing classes in `@BeforeAll` setup

Example new rule:
```java
@Test
@DisplayName("Services should not use static methods")
void servicesShouldNotUseStaticMethods() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..metrics..")
        .should().haveMethodsThat().areStatic()
        .because("Services should use dependency injection instead of static methods");
    
    rule.check(importedClasses);
}
```

## Integration with CI/CD

These architecture tests run automatically as part of the test suite, ensuring architectural constraints are enforced consistently across all builds and deployments.