# Smart Code Review

Smart Code Review combines a small Order Management microservice with a repository-local,
Markdown-driven enterprise review model. The service is the demonstration codebase; `AGENTS.md`,
`.agents/skills/code-review`, and `./code-review` contain the review standards and automation.

## Technology

- Java 23
- Spring Boot 4.1
- Gradle 9.6 with the Gradle Wrapper
- Spring Web, Spring Data JPA, Spring Security, Bean Validation, Flyway, and Actuator
- H2 for local demonstrations and PostgreSQL for the production profile
- JUnit 5, Mockito, AssertJ, and MockMvc

## Build and test

```shell
./gradlew clean test
./gradlew build
./gradlew jacocoTestCoverageVerification jacocoTestReport
```

The build enforces at least 85% line coverage and 80% branch coverage when branch instructions
exist. Open the human-readable JaCoCo report at
`build/reports/jacoco/test/html/index.html`.

## Enterprise code review

Run the complete repository review with:

```shell
./code-review
```

The command runs the required Gradle verification, performs a read-only enterprise-architect
review, prints the merge decision, and saves the report to
`build/reports/code-review/latest.md`. It also generates the architect-friendly HTML view at
`build/reports/code-review/latest.html`; both contain the same review, with Markdown as the source.

The report leads with the PR decision and a concise **What needs attention** table. Every verified
problem includes its severity, exact Java file and line or method, failure scenario, technical
impact, required correction, and verification command. Passing checks remain compact so architects
and developers can focus on code-review comments and required changes.

After committing this clean baseline to `main`, create a feature branch and compare it with:

```shell
git switch -c feature/your-change
# Implement and commit the feature.
./code-review main
```

Open `build/reports/code-review/latest.html` during review. Generated `build/` reports are local or
CI artifacts and are intentionally ignored by Git.

## Run locally

```shell
./gradlew bootRun
```

The default `local` profile starts the API at `http://localhost:8080/api/orders`. Health information
is public at `http://localhost:8080/actuator/health`; the local-only H2 console is available at
`http://localhost:8080/h2-console` with JDBC URL
`jdbc:h2:mem:orders`, user `sa`, and an empty password.

Read endpoints are public. Write endpoints require HTTP Basic authentication. The local
demonstration account is `reviewer` with password `review-demo-only`; override it before a shared
demo by setting `ORDER_ADMIN_PASSWORD`. This fallback is not used by the production profile.

Three demonstration orders are loaded into the in-memory H2 database at startup from
`src/main/resources/data.sql`. Changes persist while the application is running and reset to
the sample data after a restart. Flyway creates the schema from
`src/main/resources/db/migration/V1__create_customer_orders.sql`; Hibernate only validates it.

## Production profile

The `prod` profile uses PostgreSQL, disables seed data and the H2 console, validates the schema,
and acts as a stateless OAuth 2.0 JWT resource server. It reads deployment configuration from the
environment:

```shell
export SPRING_PROFILES_ACTIVE=prod
export DB_URL='jdbc:postgresql://localhost:5432/orders'
export DB_USERNAME='orders_app'
export DB_PASSWORD='replace-me'
export OIDC_ISSUER_URI='https://identity.example.com/realms/orders'
./gradlew bootRun
```

Flyway applies versioned migrations before JPA starts. Store the environment values in the
deployment platform's secret manager; do not commit them. The identity provider must issue a
`roles` claim containing `ORDER_READER` for read access, `ORDER_ADMIN` for read/write access, or
`OPERATIONS` for non-health actuator access. HTTP Basic authentication exists only in the local
demonstration profile.

Production liveness and readiness endpoints are available at `/actuator/health/liveness` and
`/actuator/health/readiness`. Both are safe for platform probes. Application logs include the
sanitized `X-Correlation-Id` value in their logging context.

## API

| Method | Path | Description |
| --- | --- | --- |
| `POST` | `/api/orders` | Create an order |
| `GET` | `/api/orders?page=0&size=20` | List orders, ordered by ID; page size is limited to 100 |
| `GET` | `/api/orders/{id}` | Retrieve an order |
| `PUT` | `/api/orders/{id}` | Replace an order |
| `DELETE` | `/api/orders/{id}` | Delete an order |

Example request:

```shell
curl -i -u reviewer:${ORDER_ADMIN_PASSWORD:-review-demo-only} http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{
    "customerName": "Grace Hopper",
    "productName": "Mechanical Keyboard",
    "quantity": 2,
    "unitPrice": 75.50,
    "status": "PENDING"
  }'
```

Valid statuses are `PENDING`, `CONFIRMED`, `SHIPPED`, `DELIVERED`, and `CANCELLED`.

`POST` creates a new order each time and is intentionally non-idempotent; clients must not retry
it blindly. Create, retrieve, and update responses include an `ETag` containing the entity version.
Clients must send that value in `If-Match` when updating or deleting; an intervening change produces
`412 Precondition Failed`, while an overlapping JPA optimistic-lock conflict produces `409 Conflict`.
Validation and known failures use one stable `ApiError` response,
while unexpected exception details are logged server-side and are not returned to clients. Every
response includes `X-Correlation-Id`; a safe caller-supplied value is propagated, otherwise the
service creates one and places it in the logging context.

## Package structure

The root package is `com.manjusha.smartcodereview`. The application uses package-by-feature
with explicit layers inside the `order` feature:

```text
com.manjusha.smartcodereview
├── config
├── order
│   ├── controller
│   ├── dto
│   ├── entity
│   ├── repository
│   └── service
└── exception
```

This keeps the Order feature cohesive while making controller, domain, persistence, and
business-service boundaries explicit. Cross-cutting security configuration lives in `config`, and
HTTP error handling lives in `exception`.
