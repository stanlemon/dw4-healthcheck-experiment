# Development Guidelines

This document gives you the facts and the rules for development tasks in this repository.

## Key Information
- Language: Java 21
- Frameworks: Dropwizard 5.x and Spring Boot 4.x
- Architecture: Two runtime modules, `dw5-app` and `spring4-app`, expose the same REST API over different frameworks. Two shared domain modules, `healthy-metrics` and `healthy-hangar`, hold the business logic. The `architecture-rules` module holds the ArchUnit rules that all modules share.
- Testing framework: JUnit 6 with Mockito, and REST Assured for integration tests
- Code quality: Spotless for formatting, JaCoCo for coverage, and SpotBugs with FindSecBugs for security analysis
- Architecture testing: ArchUnit for architectural constraints
- Libraries: Lombok for less boilerplate code, AssertJ for fluent assertions, and Awaitility for async tests

For the exact dependency versions, see [pom.xml](pom.xml).

## Maven Test Execution

For all Maven test runs, you must use the `maven-test-runner` subagent. Do not run Maven commands directly. The agent analyzes the test results, reports each failure clearly, and tells you what to do next.

### When to Use the Maven Test Runner Agent
Use the agent in these cases:
- After you change code, to make sure that nothing is broken
- When you add a new feature that needs tests
- When you debug a failing build or a CI failure
- When you run a specific test suite or a single test class
- When you make and analyze code coverage reports

### Example Usage
```
"Run the test suite to make sure my changes didn't break anything"
"Execute mvn test and let me know if there are any failures"  
"Run the architecture tests to validate my refactoring"
"Check if all tests pass before I commit these changes"
```

## Development Commands

### Building and Testing
```bash
# Clean build with tests - USE MAVEN-TEST-RUNNER AGENT INSTEAD
mvn clean install

# Run tests only - USE MAVEN-TEST-RUNNER AGENT INSTEAD  
mvn test

# Run specific test class - USE MAVEN-TEST-RUNNER AGENT INSTEAD
mvn test -Dtest=SpecificTest -pl healthy-metrics

# Generate code coverage report - USE MAVEN-TEST-RUNNER AGENT INSTEAD
mvn clean test jacoco:report

# View coverage report for one module (macOS)
open healthy-metrics/target/site/jacoco/index.html

# Apply code formatting
mvn spotless:apply

# Run the Dropwizard application (run mvn clean install first)
mvn exec:java -pl dw5-app

# Run the Spring Boot application
mvn spring-boot:run -pl spring4-app

# Run architecture tests only - USE MAVEN-TEST-RUNNER AGENT INSTEAD
mvn test -Dtest="com.stanlemon.architecture.*"

# Run specific architecture test class - USE MAVEN-TEST-RUNNER AGENT INSTEAD  
mvn test -Dtest="com.stanlemon.architecture.CodingRulesTest"

# Run security analysis with SpotBugs/FindSecBugs
mvn spotbugs:check
```

The Maven commands above are for reference only. When you must run tests, ask the `maven-test-runner` agent to run them. Do not run these commands directly.

## Testing Strategy

### Test Organization
- Unit tests: In `src/test/java` of each module, in the same package structure as the main source
- Integration tests: In `src/test/java` next to the unit tests (for example, `ResourceIntegrationTest`)
- Functional tests: End-to-end tests in the `src/test/java/.../functional` package
- Architecture tests: ArchUnit tests in the `src/test/java/com/stanlemon/architecture` package of each module
- Test naming: Method names that describe the behavior, with `@DisplayName` annotations
- Test structure: Nested test classes with `@Nested` to group related tests

### Test Patterns
- Arrange-Act-Assert: Use this pattern for clarity.
- Mocking: Use Mockito only as a last resort. Do not mock more than the test needs.
- Assertions: Use AssertJ for fluent, readable assertions.
- Test coverage: Test the edge cases and the exception handling.
- Naming: Use the `methodName_WhenCondition_ShouldExpectedResult` pattern.

### Thread.sleep() Elimination
Never use `Thread.sleep()` in tests. Use correct synchronization:

```java
// Bad - Flaky and unreliable
Thread.sleep(10);

// Good - Use CountDownLatch for synchronization
CountDownLatch threadStarted = new CountDownLatch(1);
threadStarted.countDown(); // Signal
threadStarted.await(1, TimeUnit.SECONDS); // Wait

// Good - Use Awaitility for condition-based waiting
await().atMost(Duration.ofSeconds(5))
       .pollInterval(Duration.ofMillis(100))
       .untilAsserted(() -> {
           assertThat(metrics.getLatency()).isGreaterThan(0);
       });
```

### Test Assertions
Use semantic assertions. They are easier to read:

```java
// Bad
assertThat(count).isEqualTo(0);
assertThat(map.get("key")).isEqualTo(value);

// Good
assertThat(count).isZero();
assertThat(map).containsEntry("key", value);
```

### Test Coverage Requirements
- Test the edge cases: null values, empty strings, and invalid inputs.
- For adapters and converters, test the round-trip conversion.
- Test each exception scenario with specific assertions.
- For external dependencies, assert the mock interactions with the Mockito `verify` method.
- Do not use `any()` in Mockito. Use specific matchers or argument captors.

### Do not test Lombok-generated code
Do not write tests that only exercise code that Lombok synthesizes. This code includes getters, setters, `equals`,
`hashCode`, `toString`, and all-args constructors on `@Value`/`@Data`/`@Builder` classes. Lombok owns the contract for those
methods, not this project. A test of them tests the annotation
processor, not our logic. These signs show that a test does this:

- The test constructs an object and immediately asserts `getFoo().equals(foo)` on every field.
- The class under test has no hand-written method bodies, only Lombok annotations.
- The compiler, not the test, catches a rename.

Delete such tests. Write tests for the behavior of the code that Lombok does not write. Examples are validation,
derived fields, business rules, and serialization edge cases that involve custom annotations.

The root `lombok.config` enables `lombok.addLombokGeneratedAnnotation = true`. Thus Lombok
stamps `@lombok.Generated` on synthesized members. JaCoCo (≥ 0.8.6) automatically excludes
all code with this annotation, so coverage reports already ignore these methods. Do not add
tests for them again.

### Test Coverage Exclusions
Some classes have lower coverage in the per-module JaCoCo reports. JaCoCo measures coverage only in the module under test. It does not count calls from the app modules. These classes have this problem:
- Exception classes (for example, `SomethingWentWrongException`): The dw5-app and spring4-app tests exercise them.
- The Spring `main()` method (`Spring4Application.main`): The `@SpringBootTest` integration tests cover the boot wiring.

The POM does not exclude these classes with JaCoCo `<excludes>`. JaCoCo measures them, and they can show lower coverage in the module report.

### Test Performance
- Keep test delays small (≤5ms for timing tests).
- Use deterministic approaches, not real delays.
- If a real implementation is available, use it instead of a mock.

### Test Execution Best Practices
Always use the maven-test-runner agent to run tests. Do not run Maven commands directly. The agent gives you:
- An analysis of the test results
- A list of the failing tests, with guidance on what to fix
- Performance data and suggestions
- The results of quality tools that run during the test phase, such as SpotBugs
- The same test feedback format across the development workflow

## Code Quality

### Lombok Usage
This project uses Lombok in many classes to reduce boilerplate code. When you use Lombok, follow these guidelines:

```java
// For immutable value classes
@Value
public class ImmutableResponse {
    String message;
    int code;
    long timestamp;
}

// For data classes with getters, setters, equals, hashCode, and toString
@Data
public class StandardDto {
    private String name;
    private int value;
}

// For creating objects with the builder pattern
@Data
@Builder
public class ComplexObject {
    private String id;
    private String name;
    private List<String> tags;
}

// For Jackson deserialization
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse {
    private String status;
    private String message;
}
```

Use these annotations:
- `@Value`: Makes an immutable class with getters, toString, equals, and hashCode.
- `@Data`: Makes getters, setters, toString, equals, hashCode, and a required-args constructor.
- `@Builder`: Makes a builder pattern implementation for the class.
- `@NoArgsConstructor`: Makes a default constructor. Jackson deserialization needs it.
- `@AllArgsConstructor`: Makes a constructor with a parameter for each field.
- `@Slf4j`: Makes a logger field.

### Exception Handling
When you catch `InterruptedException`, always restore the interrupt status:

```java
try {
    Thread.sleep(delayMs);
} catch (InterruptedException e) {
    Thread.currentThread().interrupt(); // Always restore interrupt status
    // Handle interruption appropriately
}
```

### Import Conventions
- No star imports: Import specific classes, not wildcards.
- Simple class names: Import classes and use their simple names.
- Name collisions: If two classes have the same name and one is from generated code, do these steps:
  - Import the generated class.
  - Use the fully qualified name for the other class.
- Import order: Use the standard Java import order.

### Code Formatting

For code formatting, you must use the `maven-spotless-formatter` subagent. Do not run Maven commands directly. The agent makes sure that the code is formatted, applies the fixes, and reports style issues.

```bash
# Reference commands (use maven-spotless-formatter agent instead)
mvn spotless:check    # Check formatting without modifying files
mvn spotless:apply    # Apply formatting fixes automatically
```

This project uses Spotless with Google Java Format (version 2.0.0). Spotless applies these rules:
- Standard Google Java Style formatting
- Removal of unused imports
- Removal of trailing whitespace
- A newline at the end of each file

### Quality Tools
- SpotBugs: Finds security vulnerabilities and common bugs. Run it with the `maven-spotbugs-analyzer` agent.
- FindSecBugs: A SpotBugs plugin for security static analysis.
- Test coverage: JaCoCo requires at least 90% line coverage and 80% branch coverage in each module.
- ArchUnit: Architecture tests that enforce coding standards and design rules.
- Maven Enforcer: Enforces dependency rules and security constraints.

## ArchUnit Integration

ArchUnit tests are executable tests that enforce architectural constraints and coding standards. The shared rules are in the `architecture-rules` module. Each module runs them from its own `ArchitectureTest` and `CodingRulesTest` classes.

### Architecture Test Categories

#### CodingRulesTest
This test enforces general coding standards in every module:
- No java.util.Date usage: Use the modern `java.time` API.
- No generic exceptions: Do not throw `Exception`, `RuntimeException`, or other generic exception types.
- No java.util.logging: Use SLF4J/Logback logging.
- No System.out/System.err: Do not write console output in production code.
- No Thread.sleep(): This rule prevents flaky tests. The SlowResource demo is the only exception.
- No public fields: Keep fields encapsulated.
- No JUnit assertions: Use AssertJ, because it is easier to read.

#### ArchitectureTest
In `dw5-app` and `spring4-app`, this test enforces these architectural patterns:
- Resource dependency isolation: Resources depend only on service interfaces.
- Implementation hiding: Resources have no dependencies on "Default*" implementations.
- Package organization: Exception mappers (and the Spring exception handler) are in the exceptions package.
- Naming conventions: Resources end with "Resource", and services end with "Service".
- REST patterns: Resources must have the `@Path` annotation in `dw5-app` and the `@RestController` annotation in `spring4-app`.
- Package structure: Packages have no circular dependencies, and each layer depends only on the layers that it is allowed to use.

In `healthy-metrics` and `healthy-hangar`, this test enforces these rules:
- The module has no dependencies on container-framework packages.
- Packages have no circular dependencies.
- Service names end with "Service". In `healthy-hangar`, interface names end with "Service" or "Predictor".
- In `healthy-hangar`, production code has no dependencies on the "Default*" predictors.

### Running ArchUnit Tests

You must use the maven-test-runner agent for all ArchUnit test runs:

```
"Run all architecture tests to validate the code structure"
"Execute the CodingRulesTest to check for coding standard violations"  
"Run ArchitectureTest to verify architectural constraints"
"Check if the ArchUnit tests pass after my refactoring"
```

Reference commands (use the agent instead):
```bash
# Run all architecture tests - USE MAVEN-TEST-RUNNER AGENT INSTEAD
mvn test -Dtest="com.stanlemon.architecture.*"

# Run specific test classes - USE MAVEN-TEST-RUNNER AGENT INSTEAD
mvn test -Dtest="CodingRulesTest"
mvn test -Dtest="ArchitectureTest"
```

### Adding Custom ArchUnit Rules

If a rule applies to all modules, add it as a static method to `ArchitectureRules` or `CodingRules` in `architecture-rules/src/main/java/com/stanlemon/architecture/`. Then call it from the `ArchitectureTest` or `CodingRulesTest` class of each module. If a rule applies to one module only, add it to that module in `src/test/java/com/stanlemon/architecture/`:

```java
@Test
@DisplayName("Services should not use static methods")
void servicesShouldNotUseStaticMethods() {
    ArchRule rule = noClasses()
        .that().resideInAPackage("..metrics..")
        .should().haveMethodsThat().areStatic()
        .because("Services should use dependency injection");
    
    rule.check(importedClasses);
}
```

## Security Analysis

### FindSecBugs Integration
FindSecBugs runs inside SpotBugs and does the security static analysis.

For security analysis, you must use the `maven-spotbugs-analyzer` subagent. Do not run Maven commands directly. The agent analyzes bugs, finds security vulnerabilities, and tells you what to fix.

```bash
# Reference commands (use maven-spotbugs-analyzer agent instead)
mvn spotbugs:check              # Fail build on errors
mvn clean compile spotbugs:spotbugs   # Generate report without failing build
```

The `maven-spotbugs-analyzer` agent finds new issues in your code changes before you commit them. It gives you:

- Thread safety issues
- Security vulnerabilities (from FindSecBugs)
- Possible performance problems
- An explanation of each issue
- Suggested fixes with code examples
- A fix plan in priority order

### Maven Enforcer Security Rules
The project uses the Maven Enforcer Plugin to block vulnerable dependencies:

- Log4j security: Bans vulnerable log4j versions (< 2.17.0).
- Jackson security: Bans vulnerable Jackson versions (< 2.12.7.1).
- Commons Collections: Bans versions with security issues (< 3.2.2).
- Lombok compatibility: Requires a Lombok version that works with Java 21 (>= 1.18.20).
- Plugin version control: Requires explicit plugin versions, so that builds are reproducible.

## Architecture Patterns

### Metrics Collection
- If a value is a lone counter, use atomic operations.
- If several values must agree (like the sliding-window buckets), guard them all with one lock. Read the clock inside that lock.
- Use circular buffers for time-series data.
- Clear stale data early to prevent memory leaks.

### Error Handling
All exceptions must go through the global exception handling. In `dw5-app`, this is `GlobalExceptionMapper`, plus `ConstraintViolationExceptionMapper` for validation errors. In `spring4-app`, this is `GlobalExceptionHandler`. This design gives:
- Consistent error responses
- Correct logging

Server-error metrics come from `LatencyTrackingFilter`. The filter records the latency and the 5xx status of each request in one `recordRequest` call. Exception mappers do not touch metrics.

### Health Checks
Write health checks that do these things:
- Monitor both error rates and latency.
- Use dynamic thresholds that change with traffic.
- Give clear diagnostic messages.

### Input Validation
- Make sure that all external inputs are valid.
- Use correct limits (for example, a maximum delay of 10 seconds).
- Handle edge cases without a crash.

### Shared Domain Modules
The framework-agnostic modules (`healthy-metrics`, `healthy-hangar`) must stay free of framework-specific annotations. Do not add Spring stereotypes (`@Component`, `@Service`) or Dropwizard-managed lifecycle annotations to classes in these modules. Each app module wires the shared services with its own glue code. `Spring4Application` uses `@Bean` factories. `DwApplication` uses explicit `new ...()` + `environment.jersey().register(...)` calls. Thus the same implementation can ship in both runtimes, and neither framework's classpath gets into the shared modules.

## Application Structure

### Component Registration Pattern
`DwApplication` uses the standard Dropwizard component registration pattern:

```java
public void run(DwConfiguration configuration, Environment environment) {
    // 1. Create the shared services once, as singletons
    final MetricsService metricsService = new DefaultMetricsService();
    final HealthEvaluator healthEvaluator = new HealthEvaluator(metricsService);
    final LivenessEvaluator livenessEvaluator = new LivenessEvaluator(metricsService);
    final AerodynamicsPredictor aerodynamicsPredictor = new DefaultAerodynamicsPredictor();
    final HangarService hangarService = new DefaultHangarService();

    // 2. Register the latency filter as a servlet filter (REQUEST dispatch only)
    environment
        .servlets()
        .addFilter("latency-tracking", new LatencyTrackingFilter(metricsService))
        .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");

    // 3. Register resources with dependency injection
    environment.jersey().register(new MetricsResource(metricsService));
    environment.jersey().register(new ReadinessResource(healthEvaluator));
    environment.jersey().register(new LivenessResource(livenessEvaluator));
    environment.jersey().register(new TestErrorsResource());
    environment.jersey().register(new SlowResource());
    environment.jersey().register(new HangarResource(hangarService, aerodynamicsPredictor));

    // 4. Register exception mappers for global error handling
    environment.jersey().register(new ConstraintViolationExceptionMapper());
    environment.jersey().register(new GlobalExceptionMapper());

    // 5. Register health checks
    environment.healthChecks().register("application", new ApplicationHealthCheck(healthEvaluator));
}
```

### Package Structure
```
com.stanlemon.healthy.dw5app/          # dw5-app module
├── exceptions/          # Exception mappers
├── filters/             # Servlet filter for latency tracking
├── health/              # Dropwizard health checks
├── resources/           # JAX-RS REST endpoints
├── DwApplication.java   # Main application class
└── DwConfiguration.java # Configuration class

com.stanlemon.healthy.spring4app/      # spring4-app module
├── exceptions/          # GlobalExceptionHandler (@RestControllerAdvice)
├── filters/             # OncePerRequestFilter for latency tracking
├── health/              # Spring Boot health indicator
├── resources/           # REST controllers
└── Spring4Application.java # Main application class and @Bean factories

com.stanlemon.healthy.metrics/         # healthy-metrics module: metrics collection, health evaluation
com.stanlemon.healthy.exceptions/      # healthy-metrics module: shared custom exceptions
com.stanlemon.healthy.hangar/          # healthy-hangar module: paper plane domain services
```

### Dependency Injection Pattern
- Services: The application class creates them as singletons.
- Resources: They get their service dependencies through constructor injection.
- Shared state: Service interfaces (`MetricsService`) manage it.
- Global components: Filters get service references. Exception mappers need none.

### Service Interface Pattern
All business logic is behind interfaces:
- The `MetricsService` interface has the `DefaultMetricsService` implementation.
- Resources depend only on interfaces, not on concrete implementations.
- ArchUnit tests enforce this separation, so the coupling stays loose.

## Common Issues to Watch For

### Data Conversion
- Enum value matching: Make sure that the enum values in adapters and enums match exactly.
- Case sensitivity: Be careful with string comparisons for enum lookups.
- Round-trip conversion: When you convert data formats, always test both directions.

### Constructor Behavior
- Null handling variance: Constructor null handling is different in each class.
- Before you assume a behavior, read the existing implementations.

### External Services
- Mock setup: Make sure that the mock setup for external services is correct.
- To assert complex mock interactions, use argument captors.

### Generated Code
- Do not modify generated code directly.
- Create adapter layers between generated and hand-written code.
- Handle null values and unexpected states from generated classes.

## Development Workflow

When you change the codebase, follow this workflow:

1. Plan the changes. For multi-step tasks, track the steps in the task list of your agent tool.
2. Make the code changes. Follow the existing patterns and naming conventions.
3. Run Spotless with the maven-spotless-formatter agent, to make sure that the code is formatted correctly.
4. Run SpotBugs with the maven-spotbugs-analyzer agent, to find bugs and vulnerabilities.
   - CRITICAL: After significant code changes, always run SpotBugs.
   - SpotBugs finds possible issues before you commit them.
   - It prevents thread safety, security, and logic bugs in the codebase.
5. Run the tests with the maven-test-runner agent, to make sure that your changes work correctly.
6. Commit only after all three steps (formatting, bugs, tests) pass.

With this workflow, each change keeps the code quality standards and adds no regressions. The fast feedback loop between coding and the quality tools keeps the codebase in good condition.

## Additional Guidance

When you work on this repository, follow these rules:

1. Before you write new tests, always read the existing test files.
2. Track multi-step tasks in the task list of your agent tool.
3. For all Maven test runs, use the maven-test-runner agent. Do not run the commands directly.
4. Follow the existing code patterns.
5. Add comments for anything that is not obvious to a fourth grader.
6. Follow the existing naming conventions.
7. After each code change, run the maven-spotless-formatter agent to make sure that the formatting is correct.
8. Run the maven-spotbugs-analyzer agent regularly to find possible bugs early.

### Subagent Usage Guidelines
- maven-test-runner: Use it for all test runs and for coverage analysis.
- maven-spotbugs-analyzer: Use it for static code analysis that finds bugs and security vulnerabilities.
  - Run it after each significant code change.
  - It finds issues before you commit them.
  - It gives detailed explanations and fix recommendations.
- maven-spotless-formatter: Use it for code formatting and style enforcement.
- Task list: Use the task list of your agent tool to track progress on multi-step development tasks.
- To request an agent, describe in natural language what you want it to do.

### Creating Custom Claude Code Subagents

Subagents are a Claude Code feature. A subagent does a specialized task with a prompt for that task. These instructions apply only to Claude Code subagents. They do not work with other LLM coding tools.

To create a new Claude Code subagent, do these steps:

1. Create a Markdown file in the `.claude/agents/` directory. Use this naming convention:
   - Use the `maven-` prefix for Maven-related tools.
   - Use a descriptive name that tells what the subagent does.
   - Example: `maven-spotbugs-analyzer.md`

2. Add Claude Code-specific frontmatter at the top of the file:
   ```
   ---
   name: maven-subagent-name
   description: Use this agent when... [include usage examples]
   tools: Bash, Read, Grep, Glob  # Only the tools the subagent needs
   model: sonnet
   color: green  # Choose a color that distinguishes this subagent
   ---
   ```

3. Give the Claude Code subagent content these parts:
   - A clear title and an introduction that tells the purpose of the subagent
   - A command section that shows the exact commands to run
   - A process section that gives the steps to analyze the output
   - A report format section that tells how to present the results
   - Troubleshooting guidance for common errors

4. Put example usage in the description. When Claude decides whether to invoke the subagent, it uses these examples:
   ```
   <example>Context: User wants to... user: "..." assistant: "I'll use the maven-subagent-name..."</example>
   ```

After you create the subagent, Claude can start it in three ways:
- If a request matches the description of the subagent, Claude picks it.
- You type `@agent-maven-subagent-name` in a prompt.
- You run `claude --agent maven-subagent-name`.

A subagent does not get a `/maven-subagent-name` slash command. A subagent keeps the long Maven output in its own context and returns only a short report. For this reason, use a subagent and not a skill for these Maven tasks.

## Dependencies and Libraries

### Preferred Testing Libraries
- AssertJ: For fluent assertions (`isZero()`, `containsEntry()`)
- Awaitility: For async and timing-based tests
- JUnit 6: For modern testing features
- CountDownLatch: For thread synchronization in tests
- REST Assured: For API integration tests and HTTP endpoint tests

### Dependency Management
- Latest versions: Always use the latest stable versions.
- Current dependencies: Update the existing dependencies regularly.
- Security updates: Give security patches and vulnerability fixes first priority.
- Version consistency: Make sure that related dependencies have compatible versions.
- CVEs: Make sure that new dependencies add no known vulnerabilities.

## API Design Patterns

### Resource Design
- RESTful URLs: Use noun-based paths (`/users`, `/metrics`, not `/getUsers`).
- HTTP status codes: Use the correct codes (200, 201, 400, 404, 500).
- Path parameters: Make sure that each path parameter has a valid format and exists.
- Query parameters: Give sensible defaults. Make sure that each value is in its valid range.

### Request/Response Patterns
```java
// Good - Clear DTOs with validation
@JsonProperty("delayMs")
@Min(0) @Max(10000)
private int delayMs;

// Good - Consistent error responses
public class ErrorResponse {
    private final String message;
    private final int code;
    private final long timestamp;
}
```

### Resource Method Structure
```java
@POST
@Path("/example")
public Response createExample(@Valid ExampleRequest request) {
    // 1. Validate input
    // 2. Business logic
    // 3. Return appropriate response with status code
}
```

## Logging Standards

### Structured Logging
```java
// Good - Use structured logging with context
log.info("Processing request - endpoint: {}, method: {}, requestId: {}",
         path, method, requestId);

// Good - Include relevant metrics
log.warn("High latency detected - endpoint: {}, latency: {}ms, threshold: {}ms",
         endpoint, actualLatency, threshold);
```

### Log Levels
- ERROR: System errors, and exceptions that need attention immediately
- WARN: Possible issues, threshold breaches, and recoverable errors
- INFO: Important business events, startup and shutdown, and configuration changes
- DEBUG: Detailed flow information for troubleshooting (not in production)

### MDC (Mapped Diagnostic Context)
```java
// Add request correlation ID for tracing
MDC.put("requestId", UUID.randomUUID().toString());
try {
    // Process request
} finally {
    MDC.clear(); // Always clean up
}
```

## Security Best Practices

### Input Validation
```java
// Always validate and sanitize inputs
@Valid @NotNull ExampleRequest request

// Use appropriate limits
@Min(0) @Max(10000) int delayMs

// Validate string inputs
@Pattern(regexp = "^[a-zA-Z0-9_-]+$") String identifier
```

### Security Headers
```java
// Add security headers in responses
response.header("X-Content-Type-Options", "nosniff");
response.header("X-Frame-Options", "DENY");
response.header("X-XSS-Protection", "1; mode=block");
```

### Error Information Disclosure
```java
// Bad - Exposes internal details
throw new RuntimeException("Database connection failed: " + dbUrl);

// Good - Generic error with internal logging
log.error("Database connection failed", exception);
throw new ServiceUnavailableException("Service temporarily unavailable");
```

## Performance Guidelines

### Metrics Collection
```java
// Use atomic operations for thread-safe counters
private final AtomicLong counter = new AtomicLong(0);

// Implement efficient circular buffers for time-series data
// Clear stale data proactively to prevent memory leaks
```

### Resource Management
```java
// Always use try-with-resources for auto-closeable resources
try (Connection conn = dataSource.getConnection()) {
    // Use connection
}

// Pool expensive resources
// Monitor connection pool metrics
```

### Caching Strategies
- Cache data that the code reads often and that changes rarely.
- Use correct TTL values.
- Monitor the cache hit rates.
- For critical data, fill the cache before the first request (cache warming).

## Configuration Management

### Convention Over Configuration
- Practical defaults: Each configuration value must have a sensible default that works without changes.
- Minimal required configuration: Require explicit configuration only for critical values.
- Environment-aware defaults: If the environment needs different defaults, select them from the detected environment.

### Environment-Specific Configuration
The examples below show the pattern. This repository has no `config-dev.yml`, `config-prod.yml`, or database. `dw5-app` uses `config.yml`, and `spring4-app` uses `application.yml`.

```yaml
# config-dev.yml - Override only what differs from defaults
server:
  applicationConnectors:
    - type: http
      port: 8080  # Different from production default
logging:
  level: DEBUG    # More verbose for development

# config-prod.yml - Minimal overrides for production
server:
  applicationConnectors:
    - type: http
      port: 8097  # Production port
database:
  url: ${DATABASE_URL}  # Environment variable required in prod
  password: ${DATABASE_PASSWORD}  # Secret from environment
```

### Secret Management Through Environment Variables
```yaml
# Configuration files reference environment variables
database:
  url: ${DATABASE_URL:-jdbc:h2:mem:testdb}  # Default for dev/test
  username: ${DATABASE_USER:-sa}
  password: ${DATABASE_PASSWORD}  # Required in production, no default for security

# Application reads secrets through config, not directly
```

```java
// Good - Configuration class with defaults
public class DatabaseConfiguration {
    @JsonProperty("url")
    private String url = "jdbc:h2:mem:testdb";  // Sensible default

    @JsonProperty("username")
    private String username = "sa";  // Default for dev

    @JsonProperty("password")
    private String password;  // No default - must be provided in prod

    @JsonProperty("maxConnections")
    private int maxConnections = 10;  // Practical default
}
```

### Configuration Hierarchy
1. Application defaults: In the code. They work for development.
2. Environment configuration files: They override the defaults for each environment.
3. Environment variables: Secrets and deployment-specific values.
4. System properties: Runtime overrides (rare, for debugging).

### Critical Configuration Requirements
If a value is in one of these groups, require explicit configuration for it:
- Security sensitive (passwords, API keys, certificates)
- Environment dependent (database URLs, external service endpoints)
- Business critical (feature toggles that affect revenue or compliance)

## Documentation Standards

### JavaDoc Requirements
```java
/**
 * Records one finished request and whether it was a server error.
 * This method is thread-safe and can be called concurrently.
 *
 * @param latencyMs how long the request took, in milliseconds
 * @param serverError true if the request ended in a 5xx response
 * @since 1.0
 */
public void recordRequest(long latencyMs, boolean serverError) {
    // Implementation
}
```

### API Documentation
- Use OpenAPI/Swagger annotations for REST endpoints.
- Document all request and response schemas.
- Give example requests and responses.
- Document the error codes and their meanings.

### README Maintenance
- Keep the endpoint documentation current.
- Update the build and run instructions.
- Document the environment setup requirements.
- Give troubleshooting guides.

## Git Workflow

### Pull Request Requirements
- A descriptive title in the commit message format
- A clear description of the changes and the reason for them
- Test coverage for new features
- If the API changes, documentation updates
- Code review approval before the merge
- All CI jobs pass (tests, linting, security scans)