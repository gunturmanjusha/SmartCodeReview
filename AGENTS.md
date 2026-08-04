# Smart Code Review Repository Standards

## Scope and toolchain

This repository contains one Order Management REST microservice under
`com.manjusha.smartcodereview`. It uses Java 23, Spring Boot 4.1, Gradle 9.6, Spring MVC,
Spring Data JPA, Bean Validation, H2, Actuator, JUnit 5, Mockito, AssertJ, and MockMvc.

Repository reviews assess the service as a production candidate, not only as a local demonstration.
Configuration keys and dependencies show design intent but do not by themselves prove that a
production dependency or security boundary is operational.

Do not add the Markdown-driven review model to the production Java runtime. Review automation
belongs in `AGENTS.md`, `.agents/skills/code-review`, and the root `code-review` launcher.

## Architecture and dependency direction

- Keep Order code under the feature root `order`, separated into `controller`, `service`,
  `repository`, `dto`, and `entity` packages. Keep cross-cutting error handling in `exception`.
- Require dependencies to flow `controller -> service -> repository -> entity`. DTOs may refer to
  public API enums, but controllers must not access repositories and entities must not depend on
  controllers, services, repositories, or DTOs.
- Accept and return DTOs at the HTTP boundary. Never expose JPA entities from controllers.
- Keep business decisions and transaction orchestration in services. Controllers translate HTTP;
  repositories provide persistence only.
- Use constructor injection. Do not add field injection or service-locator access.

## Web and validation

- Keep Order endpoints rooted at `/api/orders` and use HTTP semantics consistently: `201` plus a
  `Location` header for create, `200` for reads and updates, `204` for delete, `400` for invalid
  input, and `404` for missing orders.
- Validate every externally supplied request DTO with Bean Validation and `@Valid`. Add constraints
  that reflect persistence and domain limits; never rely only on database exceptions.
- Keep response fields intentional. Do not expose credentials, stack traces, implementation types,
  internal database details, or mutable entity state.
- Evolve API behavior compatibly or document the breaking change. Use explicit pagination before
  an unbounded collection endpoint is used with production-scale data.
- For retried or externally orchestrated writes, define idempotency behavior. Add optimistic
  locking or another explicit conflict strategy when concurrent updates can lose data.

## Persistence and transactions

- Put write operations inside service-level `@Transactional` boundaries. Use read-only
  transactions for queries. Do not open transactions in controllers.
- Keep repository interfaces free of business logic. Avoid N+1 queries and unbounded reads.
- Treat H2, `ddl-auto=create-drop`, `data.sql`, and the H2 console as local demonstration/test
  facilities. Production profiles must use persistent storage, versioned migrations, externally
  supplied secrets, and must disable the H2 console and destructive schema generation.
- Production readiness requires evidence for a durable target database: a deployable PostgreSQL
  binding or environment contract, Flyway verification against PostgreSQL rather than only H2,
  and documented ownership of backup, restore, availability, and credentials. A PostgreSQL driver
  and unresolved environment properties alone do not satisfy this requirement.
- Do not log complete request/response objects when they may contain customer or sensitive data.

## Exceptions, logging, security, and reliability

- Translate known domain and validation failures through `GlobalExceptionHandler` into the stable
  `ApiError` structure. Never return internal exception messages or stack traces for unexpected
  failures.
- Log unexpected failures once, with stack trace and safe correlation context. Avoid duplicate
  logging across controller and service layers and never use `System.out`.
- Do not add secrets, tokens, production passwords, or private customer data to source control.
- Apply authentication and authorization before exposing write endpoints outside a trusted demo
  environment. Keep operational endpoints restricted appropriately in non-local profiles.
- Production authentication requires an operational identity-provider contract covering issuer,
  audience, signing keys/JWKS, token validation, and failure behavior. A mocked `JwtDecoder` proves
  Spring rule wiring but does not prove the production authentication boundary.
- Production authorization requires an owned contract for the external claim carrying roles, the
  allowed `ORDER_READER`, `ORDER_ADMIN`, and `OPERATIONS` values, provisioning/revocation ownership,
  and end-to-end allow/deny verification with signed tokens. Route rules alone are partial evidence.
- Handle malformed input, invalid enum values, persistence conflicts, and unexpected failures with
  deterministic client-safe responses. Avoid catching exceptions only to suppress them.
- Propagate or create correlation identifiers at service boundaries. Keep health/readiness signals
  useful but free of secrets and implementation details.
- Configure timeouts and bounded retries for any future downstream client. Never retry
  non-idempotent operations blindly, and define degraded behavior or failure propagation.

## Maintainability

- Use descriptive names, small cohesive methods, immutable DTO records, and focused public APIs.
- Remove dead code and avoid speculative abstractions. Explain non-obvious business decisions,
  not syntax, in comments.
- Keep configuration environment-specific. Document API behavior, prerequisites, build commands,
  and demonstration-only limitations in `README.md`.
- Do not change application behavior merely to improve a review score. Findings must be reviewed
  and prioritized by the developer and architect.

## Tests and verification

- Add unit tests for service decisions and exceptional paths. Mock repositories only in unit tests.
- Add Spring integration tests for HTTP status, JSON contract, validation, persistence, and global
  exception translation. Tests must be isolated and deterministic.
- Every defect fix requires a regression test. New endpoints require successful, invalid-input,
  missing-resource, and relevant authorization cases.
- Enforce at least 85% line coverage and 80% branch coverage with JaCoCo. A coverage-gate failure
  fails the build. Do not exclude production classes merely to meet the threshold.
- Do not mark coverage, formatting, lint, static analysis, or dependency scanning as passing unless the repository
  contains the tool and its command was run successfully.

Run the supported verification commands from the repository root:

```bash
./gradlew clean test
./gradlew build
./gradlew jacocoTestCoverageVerification jacocoTestReport
```

JaCoCo coverage and thresholds are configured. The HTML coverage report is generated at
`build/reports/jacoco/test/html/index.html`. No formatting, lint, static-analysis, or dependency-
vulnerability plugin is currently configured; report those checks as `UNVERIFIED`.

## Review execution

Use the repository-local `code-review` skill and follow both reference files. Reviews are
read-only: do not modify Java, resources, tests, build configuration, or Git state. Every `FAIL`
or `PARTIAL` control must identify a file and line/method when one exists, a concrete failure
scenario, impact, correction, and confidence. Repository-level missing artifacts must name the
expected path instead of fabricating a line number.
