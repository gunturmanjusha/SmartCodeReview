# Article Notes

Proposed article title: **“I Built an AI Code Review Agent That Scales Architect-Level Standards Across Every Pull Request”**

Status: factual source notes only; this is not the final article. Repository and report results were
verified against the current working tree on 2026-08-04 and may change before publication.

# 1. Repository Overview

SmartCodeReview combines two repository-local concerns:

- A Java 23 Spring Boot 4.1 Order Management REST microservice used as the review subject.
- A Markdown-driven review architecture that tells Codex how to scope, evaluate, score, and present
  an evidence-based Solution Architect review.

The review workflow addresses a repeatability problem: developers can run the same versioned review
standards before requesting architect review, while architects receive a decision-first report that
separates routine code corrections from decisions requiring architectural judgment. The repository
does not claim that the agent makes the architect's final decision.

The demonstration service exposes create, retrieve, list, update, and delete operations for orders.
Its main stack is Java 23, Spring Boot 4.1, Gradle 9.6, Spring MVC, Spring Data JPA, Bean Validation,
Spring Security/OAuth 2.0 resource server, Flyway, Actuator, H2 for local execution, a declared
PostgreSQL production profile, JUnit 5, Mockito, AssertJ, MockMvc, and JaCoCo. Sources:
`README.md:1-15`, `build.gradle:1-76`, and `gradle/wrapper/gradle-wrapper.properties:1-9`.

Article-relevant repository tree:

```text
SmartCodeReview/
├── AGENTS.md
├── code-review
├── build.gradle
├── .agents/skills/code-review/
│   ├── SKILL.md
│   ├── references/
│   │   ├── REVIEW_CONTROLS.md
│   │   └── REPORT_FORMAT.md
│   └── scripts/RenderReport.java
├── src/main/java/com/manjusha/smartcodereview/
│   ├── config/SecurityConfig.java
│   └── order/
│       ├── controller/OrderController.java
│       ├── dto/
│       ├── entity/
│       ├── repository/OrderRepository.java
│       └── service/OrderService.java
├── src/main/resources/
│   ├── application-local.properties
│   ├── application-prod.properties
│   └── db/migration/V1__create_customer_orders.sql
├── src/test/java/com/manjusha/smartcodereview/
│   ├── ProductionSecurityIntegrationTest.java
│   └── order/
└── build/reports/
    ├── code-review/latest.md
    ├── code-review/latest.html
    └── jacoco/test/html/index.html
```

# 2. Version-Controlled Review Architecture

The five central files form a version-controlled policy pipeline:

```text
AGENTS.md (repository standards)
    ↓
SKILL.md (scope and review orchestration)
    ↓
REVIEW_CONTROLS.md (controls, scoring, severity, gates)
    ↓
REPORT_FORMAT.md (human-readable output contract)
    ↓
code-review (execution, verification, persistence, HTML rendering)
```

## AGENTS.md

`AGENTS.md` is the repository-specific engineering standard. It identifies the application and
toolchain, then defines the boundaries against which findings must be evaluated. Its rules cover:

- Package-by-feature layering and dependency direction.
- DTO-only HTTP boundaries, validation, HTTP semantics, pagination, idempotency, and concurrency.
- Service-owned transaction boundaries, repository responsibilities, migration rules, and the
  distinction between local H2 and production database evidence.
- Error translation, safe logging, authentication, authorization, JWT/JWKS verification,
  correlation IDs, and operational health behavior.
- Maintainability, unit/integration testing, regression expectations, JaCoCo thresholds, and the
  exact verification commands.

It constrains generic review behavior by naming the actual repository structure, profiles, roles,
commands, and evidence requirements. `SKILL.md` also forbids inventing organization, platform,
compliance, scale, or availability requirements not established by the repository
(`SKILL.md:13-28`).

Publication excerpt A — GitHub path `AGENTS.md#L18-L25` (8 lines):

```text
- Keep Order code under the feature root `order`, separated into `controller`, `service`,
  `repository`, `dto`, and `entity` packages. Keep cross-cutting error handling in `exception`.
- Require dependencies to flow `controller -> service -> repository -> entity`. DTOs may refer to
  public API enums, but controllers must not access repositories and entities must not depend on
  controllers, services, repositories, or DTOs.
- Accept and return DTOs at the HTTP boundary. Never expose JPA entities from controllers.
- Keep business decisions and transaction orchestration in services. Controllers translate HTTP;
  repositories provide persistence only.
```

## SKILL.md

`.agents/skills/code-review/SKILL.md` is the orchestration layer. It tells Codex to:

- Operate read-only unless implementation is separately requested.
- Establish the application, architecture, deployment evidence, integrations, and engineering
  controls before judging the code.
- Select exactly one scope: full repository, staged changes, or branch comparison.
- Inspect a staged/branch diff first and read unchanged callers, interfaces, configuration,
  persistence code, and tests only when needed to establish impact.
- Load `AGENTS.md`, apply every control from `REVIEW_CONTROLS.md` exactly once, and emit the contract
  defined by `REPORT_FORMAT.md`.
- Classify nonpassing evidence as a local implementation defect, architectural concern, deliberate
  tradeoff, or context required.
- Group symptoms by root cause, calculate satisfaction and coverage, apply verdict gates, and
  separate developer corrections from architect decisions.
- Use `UNVERIFIED` when relevant evidence or business/platform context is missing, without
  fabricating a finding.

Publication excerpt B — GitHub path `.agents/skills/code-review/SKILL.md#L50-L58` (9 lines):

```text
- `repository`: review the complete current application and supporting repository artifacts. The
  launcher selects this when argument-free `./code-review` runs on `main`, or when `--repository`
  is supplied explicitly.
- `staged`: review only `git diff --cached` and the minimum unchanged context needed to understand
  impact. Report an execution error when no staged change exists.
- `branch comparison`: review files added, modified, renamed, or deleted from the merge base of the
  supplied base revision through `HEAD`. Inspect callers, interfaces, configuration, tests, and
  unchanged code only when needed to understand the change. The launcher selects local `main`, then
  `origin/main`, for argument-free feature-branch review.
```

## REVIEW_CONTROLS.md

`.agents/skills/code-review/references/REVIEW_CONTROLS.md` is the auditable control and decision
model. Its categories and identifiers are:

- Architecture and Design: `AD-01` through `AD-06`.
- Code Quality and Maintainability: `QM-01` through `QM-06`.
- API and Integration Design: `AI-01` through `AI-06`.
- Security and Data Protection: `SD-01` through `SD-06`.
- Reliability and Operational Readiness: `RO-01` through `RO-06`.
- Data and Persistence: `DP-01` through `DP-06`.
- Testing and Verification: `TV-01` through `TV-07`.

Each control receives exactly one status. `PASS` scores 1.0, `PARTIAL` scores 0.5, and `FAIL`
scores 0.0. `UNVERIFIED` is excluded from satisfaction but included in evidence coverage; `N/A` is
excluded from both. Category satisfaction is the percentage of verified applicable controls
satisfied, not a percentage of correct source code. Overall satisfaction is the weighted mean of
scored categories; overall evidence coverage is verified applicable controls divided by all
applicable controls (`REVIEW_CONTROLS.md:15-30`).

Weights and baselines are: Architecture 20%/85%; Code Quality 15%/80%; API and Integration
15%/85%; Security 15%/85%; Reliability 15%/85%; Data and Persistence 10%/85%; Testing 10%/80%;
overall baseline 85% (`REVIEW_CONTROLS.md:32-47`).

Severity is Blocker, High, Medium, or Low. Verdict gates are applied in order: Blocker, reviewed-code
build/test failure, or coverage-gate failure produces `FAIL`; any High produces `CHANGES REQUIRED`;
coverage below 60% produces `INSUFFICIENT EVIDENCE` unless a prior fail gate applies; Medium
`FAIL` or satisfaction below 70% produces `CHANGES REQUIRED`; at least 85% with no `FAIL` produces
`PASS`; otherwise the verdict is `PASS WITH FOLLOW-UP`. A serious finding therefore overrides the
numeric baseline (`REVIEW_CONTROLS.md:49-73`). Developer action and architect judgment are explicit
required fields in the finding evidence contract (`REVIEW_CONTROLS.md:75-87`).

Publication excerpt C — GitHub path
`.agents/skills/code-review/references/REVIEW_CONTROLS.md#L5-L13` (9 lines):

```text
Assign every control exactly one status:

| Status | Display | Score | Meaning |
| --- | --- | ---: | --- |
| `PASS` | ✅ | 1.0 | Verified evidence satisfies the complete control. |
| `PARTIAL` | 🟡 | 0.5 | Verified evidence satisfies only part of the control. |
| `FAIL` | ❌ | 0.0 | Verified evidence contradicts the control. |
| `UNVERIFIED` | ❓ | Excluded | Relevant evidence is insufficient; include in evidence coverage. |
| `N/A` | ➖ | Excluded | The control genuinely cannot apply; exclude from evidence coverage. |
```

## REPORT_FORMAT.md

`.agents/skills/code-review/references/REPORT_FORMAT.md` is the output contract. It requires, in
order: a prominent plain-language decision; six-item executive assessment; at-a-glance metrics;
findings and corrections; evidence gaps; architecture summary; exact scope; seven-category
assessment; detailed control assessment; developer actions; architect decisions; nonblocking
follow-up; positive engineering decisions; verification summary; and final recommendation.

Each failed or partial finding becomes one root-cause row and one color-coded action card with its
control, category, type, owner, exact location, evidence, failure scenario, impact, correction,
short fix or implementation sketch, verification, and confidence. The report keeps unavailable
evidence separate from verified defects, avoids repeated score tables, and ends with an explicit
developer/architect split and exit criteria.

Publication excerpt D — GitHub path
`.agents/skills/code-review/references/REPORT_FORMAT.md#L23-L29` (7 lines):

```text
| Verdict | Heading | PR guidance |
| --- | --- | --- |
| `PASS` | `🟢 READY FOR ARCHITECT REVIEW` | May proceed to normal architect review and approval. |
| `PASS WITH FOLLOW-UP` | `🟡 READY WITH MINOR FOLLOW-UP` | Architect may review with tracked non-blocking follow-up. |
| `CHANGES REQUIRED` | `🟠 DEVELOPER CHANGES REQUIRED` | Return to the developer before formal architect review. |
| `FAIL` | `🔴 REVIEW BLOCKED` | Block until the critical failure is resolved. |
| `INSUFFICIENT EVIDENCE` | `⚪ INSUFFICIENT EVIDENCE` | Do not decide until missing evidence is supplied. |
```

# 3. Full Repository Review and Feature-Branch Delta Review

Verified launcher behavior:

- `./code-review` on `main` selects repository mode and reviews the complete current application and
  repository artifacts (`code-review:43-56,71-76`; `SKILL.md:50-52`).
- `./code-review` on another named branch selects local `main`; if unavailable, it selects
  `origin/main` (`code-review:57-68`).
- Branch mode validates the base revision, requires a clean working tree, calculates
  `git merge-base <base> HEAD`, rejects an empty diff, and describes the scope as files added,
  modified, renamed, or deleted from that merge base through `HEAD` (`code-review:94-120`).
- The skill limits review findings to the branch diff and permits unchanged surrounding callers,
  interfaces, configuration, tests, and other code only when needed to establish behavior or impact
  (`SKILL.md:55-63,67-70`). This is an instruction-enforced scope, not a launcher-created filesystem
  allowlist.
- `./code-review --staged` reviews `git diff --cached` plus directly affected unchanged context and
  exits with status 2 when nothing is staged (`code-review:77-89`; `SKILL.md:53-54`).
- `./code-review <base-revision>` performs an explicit merge-base comparison against that local
  revision (`code-review:94-120`).
- `./code-review --repository` explicitly requests a full review on any branch
  (`code-review:71-76`).

Exact user commands:

```bash
./code-review                 # main: repository; feature branch: automatic delta against main
./code-review --repository    # explicit full repository
./code-review --staged        # staged diff
./code-review origin/main     # explicit branch comparison base
```

# 4. The code-review Launcher

The executable root `code-review` Bash script is the execution layer.

- It resolves its own directory, uses `git rev-parse --show-toplevel`, and reads the named branch
  with `git branch --show-current`; detached HEAD is represented explicitly (`code-review:20-40`).
- It applies the scope rules in Section 3 and prints automatic selection to the terminal
  (`code-review:43-127`).
- It requires `codex` on `PATH` (`code-review:129-132`) and relies on Git, Bash, a Java 23 toolchain,
  the Gradle Wrapper, and an authenticated Codex CLI (`README.md:17-48`).
- Before Codex starts, it runs clean tests, the build, and JaCoCo coverage verification/reporting,
  retaining each exit status (`code-review:134-161`).
- It constructs a scope-specific prompt containing the branch, review date, base, verification
  statuses, evidence locations, read-only constraint, and required skill (`code-review:163-200`).
- It invokes `codex exec` ephemerally with read-only sandboxing, no approval prompts, and an output
  file for the final Markdown message (`code-review:203-215`).
- It saves timestamped verification and Codex transcripts, timestamped Markdown and HTML reports,
  and updates `build/reports/code-review/latest.md` and `latest.html` (`code-review:153-161,218-234`).
- `RenderReport.java` converts the Markdown source into the styled HTML presentation
  (`RenderReport.java:21-112`).
- A Codex failure returns the Codex status and preserves the transcript; partial Markdown is
  preserved when present. Missing final Markdown returns status 1 (`code-review:235-246`).

Publication excerpt E — GitHub path `code-review#L54-L68` (15 lines):

```bash
if [[ "$branch_name" == "main" ]]; then
  requested_scope="--repository"
  auto_repository_selected=true
else
  if git -C "$repository_root" rev-parse --verify --quiet "main^{commit}" >/dev/null; then
    requested_scope="main"
  elif git -C "$repository_root" rev-parse --verify --quiet "origin/main^{commit}" >/dev/null; then
    requested_scope="origin/main"
  else
    echo "code-review: automatic feature-branch review requires a local 'main' or 'origin/main' revision." >&2
    echo "Fetch or create the main baseline, or pass an explicit base revision." >&2
    exit 2
  fi
  auto_selected=true
fi
```

Publication excerpt F — GitHub path `code-review#L137-L150` (14 lines):

```bash
echo "Running required verification: ./gradlew clean test --console=plain"
set +e
./gradlew clean test --console=plain 2>&1 | tee "$verification_temp"
test_status=${PIPESTATUS[0]}

echo
echo "Running required verification: ./gradlew build --console=plain"
./gradlew build --console=plain 2>&1 | tee -a "$verification_temp"
build_status=${PIPESTATUS[0]}

echo
echo "Running quality gate: ./gradlew jacocoTestCoverageVerification jacocoTestReport --console=plain"
./gradlew jacocoTestCoverageVerification jacocoTestReport --console=plain 2>&1 | tee -a "$verification_temp"
coverage_status=${PIPESTATUS[0]}
```

# 5. End-to-End Review Flow

The verified implementation order differs from the initially proposed sequence because verification
runs before the agent loads repository context:

1. **Scope detection — `code-review:20-127`:** resolve repository, branch, mode, base, merge base,
   cleanliness, and nonempty diff.
2. **Build, test, and coverage verification — `code-review:134-161`:** run the three Gradle commands,
   retain statuses, and save the timestamped verification log.
3. **Review request construction — `code-review:163-200`:** pass exact scope, branch, date, base,
   verification outcomes, evidence locations, and read-only constraints to Codex.
4. **Repository and skill loading — `SKILL.md:38-44`:** Codex reads `AGENTS.md`,
   `REVIEW_CONTROLS.md`, and `REPORT_FORMAT.md` completely.
5. **Code and supporting-context inspection — `SKILL.md:65-72`:** inventory the application; for
   delta scopes, inspect the diff first and follow only context required to establish impact.
6. **Control evaluation and evidence classification — `SKILL.md:73-84` and
   `REVIEW_CONTROLS.md:3-30,75-87`:** evaluate every control exactly once, group root causes, and
   create short implementation or architecture sketches.
7. **Score and verdict calculation — `SKILL.md:75-76` and `REVIEW_CONTROLS.md:15-73`:** calculate
   satisfaction, evidence coverage, category gaps, weighted result, and ordered verdict gates.
8. **Markdown report generation — `SKILL.md:85-86`, `REPORT_FORMAT.md:1-344`, and
   `code-review:203-220`:** Codex returns only the contracted Markdown; the launcher stores the
   timestamped report and refreshes `latest.md`.
9. **HTML report generation — `code-review:221-234` and `RenderReport.java:21-112`:** render the same
   Markdown to timestamped HTML and refresh `latest.html`.

# 6. Current Verification Results

These are **current implementation results and may change before publication**. The newest complete
review is `build/reports/code-review/code-review-20260805T014442Z.md`; its review time is
2026-08-04 20:44:42 CDT.

| Verification item | Current result |
| --- | --- |
| `./gradlew clean test --console=plain` | PASS |
| `./gradlew build --console=plain` | PASS |
| `./gradlew jacocoTestCoverageVerification jacocoTestReport --console=plain` | PASS |
| Tests | 24 passed, 0 failed, 0 skipped, 0 errors |
| Line coverage | 97.63%, threshold 85% |
| Branch coverage | 85.71%, threshold 80% |
| Evidence coverage | 82.05% |
| Standards score | 85.33% of verified applicable controls satisfied |
| Final verdict | `CHANGES REQUIRED` — Developer changes required before architect review |

Evidence: `build/reports/code-review/latest.md:21-40,304-324`,
`build/reports/code-review/verification-20260805T014442Z.log`, the five current JUnit suite result
files under `build/test-results/test/`, and `build/reports/jacoco/test/html/index.html`.

# 7. Current Review Scorecard

Current results; these may change before publication.

| Category | Weight | Baseline | Current score | Evidence coverage | Status |
| --- | ---: | ---: | ---: | ---: | --- |
| Architecture and Design | 20% | 85% | 100.00% | 66.67% | Meets baseline |
| Code Quality and Maintainability | 15% | 80% | 60.00% | 83.33% | Below baseline |
| API and Integration Design | 15% | 85% | 100.00% | 66.67% | Meets baseline |
| Security and Data Protection | 15% | 85% | 70.00% | 83.33% | Below baseline |
| Reliability and Operational Readiness | 15% | 85% | 100.00% | 80.00% | Meets baseline |
| Data and Persistence | 10% | 85% | 66.67% | 100.00% | Below baseline |
| Testing and Verification | 10% | 80% | 91.67% | 85.71% | Meets baseline |
| **Overall** | **100%** | **85%** | **85.33%** | **82.05%** | Numeric baseline met; High findings require changes |

Source: `build/reports/code-review/latest.md:82-93`. The score is not the percentage of source code
that is correct, secure, tested, or production-ready.

# 8. Findings and Positive Engineering Decisions

The three most important current findings are the three High architecture findings. They may be
replaced if the repository changes before the article is finalized.

## Finding 1 — Production database readiness is not operationally proven

- **Severity/control:** High; `DP-05`.
- **Location:** `src/main/resources/application-prod.properties:1-3`,
  `src/main/resources/db/migration/V1__create_customer_orders.sql:1-11`, and the absent production
  deployment contract under `deploy/`.
- **Current behavior:** PostgreSQL dependencies and environment placeholders exist, but current
  integration tests execute H2. No deployable binding, PostgreSQL migration test, or owned backup,
  restore, availability, and credential contract is present.
- **Failure scenario:** A migration or configuration succeeds on H2 but fails on PostgreSQL, or a
  database-loss event has no owned recovery path.
- **Impact:** Production schema compatibility, durability, availability, credential delivery, and
  recovery are not demonstrated.
- **Recommended correction:** Select the PostgreSQL hosting model; add a deployable binding; run
  Flyway/JPA tests against PostgreSQL; document credential, migration, backup, restore, and
  availability ownership.
- **Verification:** Run a `PostgresPersistenceIntegrationTest` against an ephemeral PostgreSQL
  instance, then execute the documented backup/restore smoke procedure and verify CRUD plus Flyway.
- **Source:** `build/reports/code-review/latest.md:240-262`.

## Finding 2 — Production JWT authentication stops at mocked tokens

- **Severity/control:** High; `SD-03`.
- **Location:** `src/main/resources/application-prod.properties:6-7`,
  `src/test/java/com/manjusha/smartcodereview/ProductionSecurityIntegrationTest.java:27-35,56-64`,
  and the proposed contract `docs/identity-provider.md`.
- **Current behavior:** Production issuer and audience placeholders exist, but the test replaces
  `JwtDecoder` and returns constructed unsigned token objects.
- **Failure scenario:** Issuer discovery, JWKS access, signature or audience rejection, key rotation,
  or identity-provider outage behaves incorrectly while mocked route tests remain green.
- **Impact:** The production authentication trust boundary is configured but not operationally
  verified.
- **Recommended correction:** Approve the issuer, audience, JWKS/key-rotation, timeout, and failure
  contract; verify signed fixtures through an owned test JWKS boundary.
- **Verification:** Run a production identity-provider integration test and require valid tokens to
  authenticate while wrong issuer, audience, signature, and unavailable-key cases fail closed.
- **Source:** `build/reports/code-review/latest.md:196-218`.

## Finding 3 — External role lifecycle and signed-token authorization are incomplete

- **Severity/control:** High; `SD-01` and `TV-04` (`PARTIAL`).
- **Location:** `src/main/java/com/manjusha/smartcodereview/config/SecurityConfig.java:39-58`,
  `README.md:300-305`, and
  `src/test/java/com/manjusha/smartcodereview/ProductionSecurityIntegrationTest.java:56-64,73-133`.
- **Current behavior:** Route rules enforce `ORDER_READER`, `ORDER_ADMIN`, and `OPERATIONS`, and
  mocked tests cover allow/deny behavior. Claim ownership, provisioning/revocation ownership, and
  authentic signed-token behavior are not established.
- **Failure scenario:** The identity provider emits stale, unapproved, or differently shaped roles
  while mocked route tests continue to pass.
- **Impact:** Endpoint rules exist, but end-to-end authorization assurance and responsibility for
  removing access are incomplete.
- **Recommended correction:** Approve a versioned `roles: string[]` contract restricted to the three
  supported values and assign provisioning, change, and revocation ownership.
- **Verification:** Exercise the approved signed-token/JWKS fixture for every role plus missing,
  unknown, and revoked-role cases.
- **Source:** `build/reports/code-review/latest.md:170-192`.

The report also contains two current Medium developer findings: full-table materialization before
pagination (`OrderService.java:37-47`) and duplicate `findById` calls (`OrderService.java:70-75`).
They are useful line-level examples but are not among the three highest-severity findings.

Concrete positive engineering decisions currently verified:

- DTO-only controllers and service-owned persistence preserve `controller → service → repository →
  entity` boundaries (`latest.md:298`; `OrderController.java:29-80`).
- REST create/read/update/delete behavior uses validation, `Location`, ETags, and mandatory
  `If-Match` preconditions (`latest.md:299`; `OrderController.java:40-75`).
- Stable error handling and sanitized correlation context avoid exposing unexpected exception
  details and clear MDC state (`latest.md:300`).
- The production profile disables H2 facilities and includes database health in readiness
  (`latest.md:301`; `application-prod.properties:4-9`).
- All repository-required Gradle commands and both JaCoCo thresholds passed (`latest.md:302-308`).

# 9. Developer Actions and Architect Decisions

The report separates work by decision authority rather than severity alone.

Routine developer corrections:

- Replace `findAll(Sort)` plus stream slicing with `findAll(Pageable)` and verify page content,
  metadata, ordering, and repository interaction.
- Reuse the first `findById` result and verify exactly one lookup on successful get, update, and
  delete paths.
- After architecture choices are made, implement the approved PostgreSQL and identity contracts and
  add their executable integration tests.

Decisions requiring architect judgment:

- Select the PostgreSQL deployment model and assign credential, availability, migration, backup,
  and restore ownership.
- Approve issuer, audience, JWKS/key-rotation, timeout, and identity-provider failure behavior.
- Approve the external `roles` claim schema and assign provisioning and revocation ownership.

Sources: `build/reports/code-review/latest.md:276-288` and
`.agents/skills/code-review/references/REPORT_FORMAT.md:264-280`.

# 10. Limitations and Boundaries

Implementation-supported limitations:

- No deployable production topology or scaling model is present, so deployment-level availability
  and compatibility decisions are not fully verifiable (`latest.md:54,103-104`).
- H2 is the exercised database; PostgreSQL is declared but not exercised by current integration
  tests (`latest.md:65,240-262`).
- Production JWT route tests mock `JwtDecoder`; they do not verify the signed-token/JWKS boundary
  (`latest.md:196-218`).
- No formatter, lint, static-analysis, CI workflow, or dependency-vulnerability scanner is
  configured (`latest.md:55,68,274,309`).
- No changed public contract or identified external consumer commitment exists in current repository
  scope, so compatibility policy is unverified rather than failed (`latest.md:56,162`).
- Customer-data classification/retention and production recovery/runbook ownership are not supplied
  (`latest.md:57,221,231`).
- Feature-diff scoping is enforced by the launcher prompt and skill instructions. The launcher
  computes and validates a merge base, but does not create a hard filesystem allowlist
  (`code-review:94-120,163-200`; `SKILL.md:55-63`).
- The system cannot select business ownership, platform standards, data governance, production
  topology, or accepted technical debt without the corresponding organizational context. It marks
  such gaps `UNVERIFIED` or requests architect judgment instead of guessing (`SKILL.md:88-120`).
- Verdict gates provide review guidance, but the architect owns the final decision; the tool does
  not approve, reject, merge, commit, or push (`REVIEW_CONTROLS.md:63-73`).

# 11. Publication-Ready Assets

## Compact repository tree

Use the tree in Section 1. It contains only the review architecture, representative demonstration
code, tests, and generated human-readable reports.

## Short workflow representation

```text
Developer runs ./code-review
  → launcher detects main / feature / staged scope
  → Gradle test, build, and JaCoCo evidence is captured
  → Codex loads AGENTS.md + SKILL.md + controls + report contract
  → changed code (or repository) and required context are evaluated
  → control evidence is scored and verdict gates are applied
  → latest.md is saved and rendered as latest.html
  → developer fixes code; architect decides cross-system contracts
```

## Excerpt inventory

Six sanitized, publication-sized excerpts are included above:

| Asset | Subject | Exact GitHub path |
| --- | --- | --- |
| A | Repository dependency rules | `AGENTS.md#L18-L25` |
| B | Repository/staged/branch scopes | `.agents/skills/code-review/SKILL.md#L50-L58` |
| C | Status and evidence model | `.agents/skills/code-review/references/REVIEW_CONTROLS.md#L5-L13` |
| D | Verdict-to-PR guidance | `.agents/skills/code-review/references/REPORT_FORMAT.md#L23-L29` |
| E | Automatic main/feature selection | `code-review#L54-L68` |
| F | Test, build, and coverage execution | `code-review#L137-L150` |

## Compact scorecard

Use the eight-row table in Section 7. It includes weight, baseline, current satisfaction, evidence
coverage, and status without raw control-count repetition.

## Example finding

Use Finding 2 from Section 8 for a compact architecture example: the production JWT configuration
exists, but mocked decoding does not exercise issuer discovery, JWKS retrieval, signatures,
audience rejection, or rotation. The correction and verification boundary are explicit.

## Example architect decision

Use the PostgreSQL decision from Section 9: select the deployment model and assign credential,
availability, migration, backup, and restore ownership. The developer implements and tests the
selected model; the agent does not choose the operating model independently.

# 12. Source Index

| Claim | Repository file | Section, line range, function, or heading | Status |
| --- | --- | --- | --- |
| Repository combines a demo microservice with Markdown-driven review automation | `README.md` | Lines 1-6 | VERIFIED |
| Java 23, Spring Boot 4.1, Gradle 9.6 and principal dependencies | `build.gradle`; Gradle wrapper | `build.gradle:1-36`; `gradle-wrapper.properties:1-9` | VERIFIED |
| Repository-specific dependency direction and DTO boundary | `AGENTS.md` | Lines 16-26 | VERIFIED |
| API, validation, persistence, security, reliability, and testing standards | `AGENTS.md` | Lines 28-111 | VERIFIED |
| Reviews are read-only and evidence-backed | `AGENTS.md`; `SKILL.md` | `AGENTS.md:113-119`; `SKILL.md:6-9,88-120` | VERIFIED |
| Full repository, staged, and branch-comparison scopes exist | `SKILL.md`; `code-review` | `SKILL.md:46-63`; `code-review:71-120` | VERIFIED |
| Main selects full review; feature branch selects main/origin-main comparison | `code-review` | Lines 43-69 | VERIFIED |
| Branch comparison uses a merge base and requires a clean, nonempty committed diff | `code-review` | Lines 94-120 | VERIFIED |
| Feature review is limited to changed files with necessary context | `SKILL.md`; `code-review` | `SKILL.md:55-70`; `code-review:117-119` | VERIFIED (instruction-enforced) |
| Every control receives PASS/PARTIAL/FAIL/UNVERIFIED/N/A | `REVIEW_CONTROLS.md` | Lines 3-13 | VERIFIED |
| Satisfaction and evidence coverage use different denominators | `REVIEW_CONTROLS.md` | Lines 15-30 | VERIFIED |
| Seven weighted categories and baselines are defined | `REVIEW_CONTROLS.md` | Lines 32-47, 89-203 | VERIFIED |
| Serious findings override numerical scores | `REVIEW_CONTROLS.md` | Lines 49-61 | VERIFIED |
| Architect owns the final decision | `REVIEW_CONTROLS.md` | Lines 63-73 | VERIFIED |
| Report is decision-first and separates developer and architect work | `REPORT_FORMAT.md` | Lines 7-55, 264-280, 318-344 | VERIFIED |
| Launcher runs clean test, build, and JaCoCo commands | `code-review` | Lines 134-150 | VERIFIED |
| Launcher invokes Codex ephemerally in a read-only sandbox | `code-review` | Lines 163-215 | VERIFIED |
| Launcher writes timestamped/latest Markdown and HTML | `code-review`; `RenderReport.java` | `code-review:153-161,218-234`; `RenderReport.java:21-112` | VERIFIED |
| Current commands passed and 24 tests completed without failure | Latest review and test results | `latest.md:23-30,304-309`; `build/test-results/test/TEST-*.xml` | VERIFIED |
| Current coverage is 97.63% line and 85.71% branch | `build/reports/code-review/latest.md` | Lines 29-30, 273, 306-308 | VERIFIED |
| Current evidence coverage is 82.05% and standards score is 85.33% | `build/reports/code-review/latest.md` | Lines 35-38, 93 | VERIFIED |
| Current verdict is developer changes required | `build/reports/code-review/latest.md` | Lines 3-19, 313-324 | VERIFIED |
| PostgreSQL is operational in production | Repository evidence | `application-prod.properties:1-3`; `latest.md:240-262` | NOT VERIFIED |
| Real issuer/JWKS/signed-token integration works | Repository evidence | `application-prod.properties:6-7`; `latest.md:196-218` | NOT VERIFIED |
| Route roles are implemented but external lifecycle ownership is incomplete | `SecurityConfig.java`; latest review | `SecurityConfig.java:39-58`; `latest.md:170-192` | PARTIALLY VERIFIED |
| CI, formatter/lint, static analysis, and vulnerability scanning run successfully | Repository evidence | `latest.md:55,68,274,309` | NOT VERIFIED |
| Production topology, recovery ownership, and customer-data policy are established | Repository evidence | `latest.md:52-57,70-80,221,231` | NOT VERIFIED |
