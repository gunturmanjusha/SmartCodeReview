# Smart Code Review

Smart Code Review combines a small Order Management microservice with a repository-local,
Markdown-driven Solution Architect review model. The service is the demonstration codebase;
`AGENTS.md`, `.agents/skills/code-review`, and `./code-review` contain the review standards and
automation.

## Technology

- Java 23
- Spring Boot 4.1
- Gradle 9.6 with the Gradle Wrapper
- Spring Web, Spring Data JPA, Spring Security, Bean Validation, Flyway, and Actuator
- H2 for local demonstrations and PostgreSQL for the production profile
- JUnit 5, Mockito, AssertJ, and MockMvc

## Prerequisites

Install the following before running the application or its code review:

- Git
- JDK 23
- A shell capable of running Bash scripts (`Terminal` on macOS/Linux, or Git Bash/WSL on Windows)
- [OpenAI Codex CLI](https://developers.openai.com/codex/cli), authenticated with an account that
  can use Codex

Install Codex on macOS or Linux using the current official installer:

```shell
curl -fsSL https://chatgpt.com/codex/install.sh | sh
```

Open a new terminal, then verify the installation and authentication:

```shell
codex --version
codex login status
```

If authentication is required, run `codex login` and complete the sign-in flow. Never commit or
share an API key, access token, or Codex credential with this repository.

Verify that Gradle can select the required Java 23 toolchain:

```shell
java -version
./gradlew --version
```

## Clone and verify the project

```shell
git clone https://github.com/gunturmanjusha/SmartCodeReview.git
cd SmartCodeReview
./gradlew clean test
./gradlew build
./gradlew jacocoTestCoverageVerification jacocoTestReport
```

The Gradle Wrapper downloads the repository's supported Gradle version when necessary, so a
separate Gradle installation is not required. The build selects Java 23 and enforces at least 85%
line coverage and 80% branch coverage.

Open the human-readable JaCoCo coverage report after the build:

```shell
# macOS
open build/reports/jacoco/test/html/index.html

# Linux
xdg-open build/reports/jacoco/test/html/index.html

# Windows PowerShell
start build/reports/jacoco/test/html/index.html
```

## Run a Solution Architect code review

Run commands from the repository root. The simplest command selects the review scope from the
current branch:

```shell
./code-review
```

- On `main`, it reviews the complete repository.
- On a feature branch, it reviews only files added, modified, renamed, or deleted by that branch,
  while inspecting minimal directly affected unchanged code when context is necessary.
- Feature branches are compared with local `main`, or with `origin/main` when local `main` is
  unavailable.
- Outside Git or at a detached `HEAD`, automatic review stops because no current branch is available.

Automatic branch comparison requires committed feature changes and a clean working tree. This keeps
the tested revision identical to the reviewed revision. Use staged mode when the work is not ready
to commit.

The launcher performs the following work automatically:

1. Runs the clean Gradle test suite.
2. Runs the complete Gradle build.
3. Enforces the JaCoCo coverage gates and generates its HTML report.
4. Invokes Codex as a read-only senior Solution Architect reviewer.
5. Evaluates every applicable review control without changing application code.
6. Prints the review in the terminal and generates Markdown and HTML reports.

The evidence model covers seven solution-level categories:

- Architecture and Design
- Code Quality and Maintainability
- API and Integration Design
- Security and Data Protection
- Reliability and Operational Readiness
- Data and Persistence
- Testing and Verification

### Open the code-review report

The main architect-facing result is:

```text
build/reports/code-review/latest.html
```

Open it with:

```shell
# macOS
open build/reports/code-review/latest.html

# Linux
xdg-open build/reports/code-review/latest.html

# Windows PowerShell
start build/reports/code-review/latest.html
```

The report leads with three independent decisions—developer implementation readiness, architect
review or decision readiness, and production readiness—and a color-coded overall engineering
assessment. Dedicated sections separate developer corrections, architectural conformance
violations, architect decisions required, and evidence gaps. Issue links jump directly to the
detailed evidence and verification.
Related implementation and testing symptoms are grouped into one root-cause issue instead of
appearing as duplicate flaws. Each developer-owned issue includes a short Java, SQL, configuration,
or pseudocode fix sketch tied to the exact class or method, plus the focused test to run. Full source
files and unified diffs are deliberately omitted from the architect report. Architect-owned issues
identify the repository evidence, required decision, recommended option, and tradeoff without
inventing missing business or platform requirements. The
report also shows the evidenced business flow and solution architecture, seven-category scorecard,
compact verification summary with a clickable JaCoCo HTML report, and a clear separation between
routine developer corrections and decisions requiring architect judgment. Every verified problem
includes its control, classification, severity, exact Java file and line or method, repository
evidence, failure scenario, technical impact, recommended correction, confidence, and verification
command. Passing controls remain
compact so architects can focus on material decisions.

Every nonpassing result is classified as a **Developer implementation defect**, **Architectural
conformance violation**, **Architect decision required**, or **Evidence gap**. “Architecture flaw”
is reserved for implemented conformance violations or verified system-level architecture failures;
an unresolved production contract is routed to architect decision and may block production
readiness without blocking access to architect review. Independently actionable defects remain
separate even when they appear in the same Java class.

### Review scopes

| Command | Scope | When to use it |
| --- | --- | --- |
| `./code-review` | Automatically selected from branch | Review the complete repository on `main`; on a feature branch, review only its changes against `main`. |
| `./code-review --repository` | Complete current repository | Explicitly review the whole repository from any branch. |
| `./code-review --staged` | Staged Git changes | Review changes added with `git add` before committing. |
| `./code-review main` | Current branch compared with `main` | Explicitly select `main` as the comparison base. |
| `./code-review origin/main` | Current branch compared with `origin/main` | Compare with the locally available remote-tracking baseline. |

For branch comparison, the launcher identifies and displays the currently checked-out feature
branch. When an explicit argument is used, `main` or `origin/main` is the **base revision**, not the
feature branch name. The base revision must already exist locally; the launcher does not fetch or
change Git state. Unchanged files may be inspected only for directly affected context; unrelated
pre-existing code is outside the review scope. The full build and test suite still runs to detect
regressions caused by the feature.

### Feature-branch review workflow

```shell
git switch main
git pull
git switch -c feature/your-change

# Implement and test the feature.
./gradlew clean test

# Commit the feature so branch-comparison mode can review it through HEAD.
git add .
git commit -m "Describe the feature"

# The checked-out feature branch and main comparison base are detected automatically.
./code-review

# macOS: open the architect report.
open build/reports/code-review/latest.html
```

To review work before committing, stage the intended files and use staged mode:

```shell
git add path/to/changed/files
./code-review --staged
open build/reports/code-review/latest.html
```

### Generated reports

| Path | Purpose |
| --- | --- |
| `build/reports/code-review/latest.html` | Final, readable architect report from the most recent review. |
| `build/reports/code-review/latest.md` | Markdown source for the same review. |
| `build/reports/code-review/code-review-<timestamp>.html` | Timestamped HTML audit copy. |
| `build/reports/code-review/code-review-<timestamp>.md` | Timestamped Markdown audit copy. |
| `build/reports/code-review/verification-<timestamp>.log` | Complete build and test evidence used by the reviewer. |
| `build/reports/jacoco/test/html/index.html` | Human-readable test coverage report. |

Architects use `latest.html` and its JaCoCo link. Detailed Gradle output remains in the timestamped
verification log for audit or troubleshooting and is intentionally omitted from the architect report.

The `build/` directory is intentionally ignored by Git because reports are generated results tied to
one checkout, branch, and execution time. Anyone who clones the repository can reproduce current
reports by running `./code-review`. Reports are not automatically visible in the GitHub source tree.

### Understanding the decision

The HTML report explains whether the work is ready for normal architect approval, needs follow-up,
requires changes, must be rejected, or lacks enough evidence. Serious verified findings and build,
test, or coverage failures override a high numerical score. The standards percentage means the
percentage of verified applicable controls satisfied; it is not the percentage of source code that
is correct, secure, tested, or production-ready. Each category shows its score, baseline, evidence
coverage, result, and a concise architect summary without repeating raw control counts.

### Troubleshooting the review command

- **`codex: command not found`:** install Codex, open a new terminal, and run `codex --version`.
- **Codex is not authenticated:** run `codex login status`, followed by `codex login` if required.
- **Java toolchain not found:** install JDK 23 and verify it with `java -version` and
  `./gradlew --version`.
- **`Permission denied: ./code-review`:** run `chmod +x code-review gradlew` on macOS/Linux.
- **Staged mode reports no changes:** stage the intended files with `git add` first.
- **Automatic review requires a named Git branch:** check out `main` for a complete repository
  review or a feature branch for a change-only review.
- **Branch comparison requires a clean working tree:** commit the feature changes, or stage the
  intended changes and run `./code-review --staged`.
- **No committed branch changes were found:** confirm the feature commit exists with
  `git log --oneline main..HEAD`.
- **Base revision does not exist:** fetch it yourself if appropriate, then confirm with
  `git rev-parse --verify main` or use the correct local base revision.
- **No report appears:** inspect the terminal error and the preserved timestamped transcript under
  `build/reports/code-review/`. The launcher returns a nonzero status for execution failures and
  preserves partial output when possible.
- **Windows:** run `./code-review` from Git Bash or WSL. Use `gradlew.bat` when running Gradle
  directly from Command Prompt or PowerShell.

## Build and test commands

```shell
./gradlew clean test
./gradlew build
./gradlew jacocoTestCoverageVerification jacocoTestReport
```

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
export OIDC_AUDIENCE='smart-code-review'
./gradlew bootRun
```

Flyway applies versioned migrations before JPA starts. Store the environment values in the
deployment platform's secret manager; do not commit them. The identity provider must issue a
`roles` claim containing `ORDER_READER` for read access, `ORDER_ADMIN` for read/write access, or
`OPERATIONS` for non-health actuator access. It must also include the value configured by
`OIDC_AUDIENCE` in the token's `aud` claim. HTTP Basic authentication exists only in the local
demonstration profile.

Production liveness and readiness endpoints are available at `/actuator/health/liveness` and
`/actuator/health/readiness`. Readiness includes database health because every order operation
depends on PostgreSQL; liveness remains process-focused to avoid restart loops during database
outages. Both endpoints are safe for platform probes. Application logs include the sanitized
`X-Correlation-Id` value in their logging context.

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
