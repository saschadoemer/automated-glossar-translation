# AGENTS.md

## Purpose

This file defines the operating rules for coding agents working on this Java Spring Boot project.

Agents may clean up, document, and improve the maintainability of the codebase, but must not change business logic, runtime behavior, public APIs, persistence behavior, security behavior, or integration semantics unless explicitly instructed.

The default task is safe refactoring, documentation, and observability improvement.

---

## Core Principle

Preserve behavior.

Every change must be behavior-neutral unless the user explicitly requests a functional change.

Do not make speculative improvements. Do not redesign the application. Do not introduce new patterns, dependencies, or abstractions unless they are clearly necessary and low risk.

When in doubt, choose the smallest safe change.

---

## Project Type

This is a Java Spring Boot project.

Typical components may include:

* REST controllers
* Services
* Repositories
* DTOs
* Domain models
* Configuration classes
* Scheduled jobs
* Security configuration
* External API clients
* Persistence mappings
* Tests
* OpenAPI / Swagger documentation

Treat all public APIs and integration contracts as stable unless instructed otherwise.

---

## Non-Negotiable Constraints

Agents must not:

* Change business rules.
* Change method behavior.
* Change REST paths, HTTP methods, request parameters, headers, status codes, or response structures.
* Change DTO field names or serialization behavior.
* Change database schema, queries, transaction boundaries, or repository behavior unless explicitly requested.
* Change security rules, authentication, authorization, or validation semantics.
* Change exception semantics or error response behavior.
* Remove tests.
* Add dependencies unless strictly required and approved by context.
* Perform broad rewrites.
* Rename public classes, public methods, public fields, or configuration properties unless explicitly instructed.

Agents may:

* Clean up clutter.
* Improve readability.
* Extract private helper methods.
* Add missing Javadoc to public methods.
* Add or improve OpenAPI descriptions.
* Improve safe production logging.
* Remove unused imports and dead code.
* Improve private/local naming where safe.
* Apply project formatting conventions.

---

## Clean Code Rules

Apply common clean-code practices conservatively.

### General Cleanup

* Remove unused imports.
* Remove dead code.
* Remove redundant comments.
* Remove redundant variables.
* Remove unnecessary nesting.
* Prefer guard clauses where behavior remains unchanged.
* Extract complex private logic into clearly named private methods.
* Reduce duplication when it is obviously safe.
* Replace magic values with named constants where appropriate.
* Keep methods focused and readable.
* Preserve existing package structure unless a small move is clearly justified.
* Avoid clever code.
* Prefer clarity over compactness.

### Method Cleanup

For each method:

* Preserve input/output behavior.
* Preserve side effects.
* Preserve call order where order could matter.
* Preserve validation flow.
* Preserve exception handling.
* Preserve transaction behavior.
* Preserve repository and external API interactions.
* Do not reorder statements unless the reordering is clearly behavior-neutral.
* Do not change null-handling behavior unless explicitly requested.

---

## Java Type Inference

Use Java local variable type inference where appropriate.

This means using `var` only for local variables where the inferred type is obvious from the right-hand side.

Good:

```java
var customer = customerRepository.findById(customerId);
var response = new CustomerResponse(customer.id(), customer.name());
var items = List.of("A", "B", "C");
```

Avoid:

```java
var result = service.process(input);
var data = externalClient.fetch(request);
var value = repository.findSomething();
```

Do not use `var` when it hides important domain meaning.

Never use `var` for:

* Fields
* Method parameters
* Return types
* Public API signatures

Keep public signatures explicit.

---

## Javadoc Rules

Add Javadoc for every public method where Javadoc is missing.

Javadoc must describe the method from the caller’s perspective.

Include:

* `@param` for each parameter.
* `@return` when the method returns a value.
* `@throws` only when the exception is part of the expected contract.

Do not:

* Document obvious implementation details.
* Invent behavior not visible in the code.
* Claim guarantees the code does not provide.
* Add noisy or meaningless documentation.

Example:

```java
/**
 * Retrieves the customer with the given identifier.
 *
 * @param customerId the unique identifier of the customer
 * @return the customer data for the given identifier
 * @throws CustomerNotFoundException if no customer exists for the given identifier
 */
public CustomerDto getCustomer(String customerId) {
    ...
}
```

---

## OpenAPI Documentation Rules

For all Spring REST endpoints, add or improve OpenAPI documentation.

Use annotations where available, such as:

* `@Operation`
* `@ApiResponses`
* `@ApiResponse`
* `@Parameter`
* `@RequestBody`
* `@Schema`

Each endpoint should include:

* A concise summary.
* A useful description.
* Parameter descriptions.
* Request body description where applicable.
* Success response descriptions.
* Relevant error response descriptions.
* DTO schema descriptions where missing and useful.

Do not change:

* Endpoint paths.
* HTTP methods.
* Request mappings.
* Parameter names.
* Header names.
* Request or response DTO structure.
* Status codes.
* Validation behavior.

Example:

```java
@Operation(
    summary = "Retrieve a customer",
    description = "Returns the customer data for the provided customer identifier."
)
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "Customer was found"),
    @ApiResponse(responseCode = "404", description = "Customer was not found")
})
@GetMapping("/{customerId}")
public CustomerDto getCustomer(
    @Parameter(description = "Unique identifier of the customer", required = true)
    @PathVariable String customerId
) {
    ...
}
```

---

## Production Logging Rules

Ensure that meaningful decision branches have safe, production-ready logging.

Decision branches include:

* `if` / `else`
* `switch` cases
* validation outcomes
* fallback paths
* skipped processing
* rejected requests
* retries
* external system calls
* exception paths
* security-relevant decisions
* state transitions

Logging must be useful, sufficient, and safe.

### Required Logging Style

Use SLF4J-style parameterized logging.

```java
private static final Logger log = LoggerFactory.getLogger(CustomerService.class);

log.info("Customer lookup requested: customerId={}", customerId);
log.warn("Customer not found: customerId={}", customerId);
log.error("Customer lookup failed: customerId={}", customerId, exception);
```

Do not use:

```java
System.out.println(...);
exception.printStackTrace();
log.info("Customer: " + customer);
```

### Log Levels

Use appropriate levels:

* `trace`: very detailed diagnostic flow.
* `debug`: development-level diagnostics.
* `info`: important process or business milestones.
* `warn`: unexpected but recoverable situations.
* `error`: failures requiring attention.

Avoid excessive logging in high-volume paths.

Avoid duplicate logs for the same event.

### Safe Logging

Never log sensitive data.

Do not log:

* Passwords
* Tokens
* Secrets
* API keys
* Authorization headers
* Session IDs
* Raw credentials
* Full request bodies
* Full response bodies
* Payment data
* IBANs
* Personal customer data
* Private email contents
* Stack traces at inappropriate levels

Prefer safe identifiers:

* Technical IDs
* Correlation IDs
* Request IDs
* Entity IDs
* Counts
* Status values
* Enum values
* Reason codes

Good:

```java
log.info("Payment validation failed: paymentId={}, reason={}", paymentId, reasonCode);
```

Bad:

```java
log.info("Payment validation failed for IBAN {} and customer {}", iban, customerName);
```

### Branch Logging Example

```java
if (customer.isActive()) {
    log.debug("Processing active customer: customerId={}", customer.getId());
    process(customer);
} else {
    log.info("Skipping inactive customer: customerId={}", customer.getId());
}
```

---

## Exception Handling

Do not change exception behavior unless explicitly requested.

When improving logging around exceptions:

* Preserve the original exception type.
* Preserve wrapping behavior.
* Preserve HTTP error behavior.
* Preserve rollback behavior.
* Preserve retry behavior.
* Avoid logging the same exception repeatedly at multiple layers.

Use `error` only where the failure requires operational attention.

Use `warn` for recoverable or expected exceptional states.

---

## Spring Boot Specific Rules

### Controllers

For REST controllers:

* Preserve mappings exactly.
* Add OpenAPI descriptions where missing.
* Add safe logging for important request handling decisions.
* Avoid logging full request bodies.
* Keep validation behavior unchanged.
* Do not move business logic into controllers.

### Services

For services:

* Improve method readability.
* Extract private helpers where useful.
* Add Javadoc to public methods.
* Add logging around meaningful decision branches.
* Preserve transactional boundaries.
* Preserve business flow.

### Repositories

For repositories:

* Do not change queries unless explicitly instructed.
* Do not rename query methods unless safe and necessary.
* Do not change persistence behavior.
* Avoid adding logging inside repositories unless the project already follows that pattern.

### DTOs

For DTOs:

* Do not rename fields.
* Do not change serialization annotations.
* Add `@Schema` descriptions where useful.
* Do not change validation annotations unless explicitly requested.

### Configuration

For configuration classes:

* Do not change property names.
* Do not change defaults unless explicitly requested.
* Add documentation only where helpful.
* Avoid logging secrets or resolved credentials.

---

## Testing and Verification

After changes:

* Run the existing test suite.
* Run compile/build checks.
* Run formatting or linting tools if already configured.
* Do not introduce new tools without clear need.
* If tests fail, determine whether behavior changed.
* Revert behavior-changing cleanup.
* Do not weaken or remove failing tests to make the build pass.

Preferred commands depend on the project:

```bash
./mvnw clean test
```

or:

```bash
./gradlew test
```

If available, also run:

```bash
./mvnw verify
```

or:

```bash
./gradlew check
```

---

## Change Reporting

After completing work, provide a concise summary grouped by:

* Clean code cleanup
* Javadoc additions
* OpenAPI documentation
* Logging improvements
* Files changed
* Tests/checks executed
* Risks or assumptions
* Items intentionally not changed to avoid behavior changes

The summary must explicitly mention whether business logic was preserved.

---

## Default Agent Workflow

1. Inspect the project structure.
2. Identify Spring Boot controllers, services, DTOs, repositories, and configuration classes.
3. Make small, behavior-neutral cleanup changes.
4. Add missing Javadoc for public methods.
5. Add or improve OpenAPI descriptions for REST endpoints.
6. Improve production-safe logging for meaningful decision branches.
7. Run tests and build checks.
8. Report changes and any limitations.

---

## Final Instruction

Improve the codebase without changing what the application does.

Documentation, readability, and observability may improve.

Behavior must remain stable.
