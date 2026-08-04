# Review Controls

## Status and scoring model

Assign every applicable control exactly one status:

| Status | Display | Score | Meaning |
| --- | --- | ---: | --- |
| `PASS` | ✅ | 1.0 | Verified evidence satisfies the control. |
| `PARTIAL` | 🟡 | 0.5 | Verified evidence satisfies only part of the control. |
| `FAIL` | ❌ | 0.0 | Verified evidence contradicts the control. |
| `UNVERIFIED` | ❓ | Excluded | Evidence is insufficient; include in evidence coverage. |
| `N/A` | ➖ | Excluded | The control genuinely does not apply; exclude from coverage. |

For a category:

```text
verified applicable = PASS + PARTIAL + FAIL
applicable = verified applicable + UNVERIFIED
category satisfaction = (PASS + 0.5 * PARTIAL) / verified applicable * 100
category coverage = verified applicable / applicable * 100
```

When a category has no verified applicable control, display satisfaction as `UNSCORED`. Calculate
overall satisfaction as the weighted mean of scored category values, renormalized across only the
weights of scored categories. Overall evidence coverage is all verified applicable controls divided
by all applicable controls. `Percentage not met` is `100 - overall satisfaction`.

The percentage means **percentage of verified, applicable review controls satisfied**. Never call
it the percentage of source code that is correct, safe, or covered.

## Visible acceptance baselines

Use these baselines to make category scores understandable to architects:

| Category | Baseline |
| --- | ---: |
| Architecture and Design | 85% |
| Coding Standards | 80% |
| Code Quality and Maintainability | 80% |
| Reliability and Security | 85% |
| Testing and Verification | 80% |
| Overall | 85% |

Show the baseline and the score-to-baseline gap in the category assessment. A baseline comparison
is diagnostic; the verdict gates below remain authoritative. A numerical baseline result must
never override a Blocker, High finding, reviewed-code build/test failure, or insufficient evidence.

## Verdict gates

Apply gates in this order; a numerical score never overrides a serious verified finding:

1. Any Blocker finding produces `FAIL`.
2. A required build or test failure caused by reviewed code produces `FAIL`.
3. Line coverage below 85%, branch coverage below 80% when branch instructions exist, or failure of
   `jacocoTestCoverageVerification` produces `FAIL`.
4. Any High finding produces `CHANGES REQUIRED`.
5. Evidence coverage below 60% produces `INSUFFICIENT EVIDENCE` unless a prior fail gate applies.
6. Any Medium `FAIL`, or overall satisfaction below 70%, produces `CHANGES REQUIRED`.
7. Satisfaction of at least 85%, with no `FAIL`, produces `PASS`.
8. Otherwise produce `PASS WITH FOLLOW-UP`.

Map verdicts to PR guidance: `PASS` = approve; `PASS WITH FOLLOW-UP` = architect may approve with
tracked follow-up; `CHANGES REQUIRED` = return to developer; `FAIL` = reject/block; `INSUFFICIENT
EVIDENCE` = do not approve until verification exists. The architect owns the final decision.

## Finding evidence contract

Every `FAIL` and `PARTIAL` must include: control ID, category, severity (`Blocker`, `High`, `Medium`,
or `Low`), status, exact file and line or method, repository evidence, concrete failure scenario,
technical impact, recommended correction, verification step, and confidence (`High`, `Medium`, or
`Low`). If no source location can exist, name the missing repository artifact and explain why.

## Architecture and Design — 25%

| ID | Control |
| --- | --- |
| `AD-01` | Order code preserves controller, service, repository, DTO, and entity package boundaries defined in `AGENTS.md`. |
| `AD-02` | Dependencies flow controller → service → repository → entity without reverse or bypass dependencies. |
| `AD-03` | Controllers accept/return DTOs and do not expose JPA entities. |
| `AD-04` | Business behavior and transaction orchestration reside in services; controllers remain HTTP adapters. |
| `AD-05` | Constructor injection is used and components remain cohesive without avoidable cyclic coupling. |
| `AD-06` | API evolution, idempotency, concurrency ownership, and service-boundary decisions are explicit where the application behavior requires them. |

## Coding Standards — 20%

| ID | Control |
| --- | --- |
| `CS-01` | Java 23 and Spring Boot conventions are used consistently; code compiles successfully. |
| `CS-02` | Naming, formatting, imports, and method structure are consistent and readable. |
| `CS-03` | Request constraints align with entity column/domain constraints and all request bodies use `@Valid`. |
| `CS-04` | Repository-defined formatting or lint checks run successfully. Use `UNVERIFIED` when no such tool exists. |
| `CS-05` | The Java toolchain and wrapper make builds reproducible on the supported JDK rather than silently depending on an arbitrary local runtime. |

## Code Quality and Maintainability — 20%

| ID | Control |
| --- | --- |
| `QM-01` | Methods and classes have focused responsibilities with no material duplication or dead code. |
| `QM-02` | DTO mapping and calculated fields are deterministic, null-safe for valid domain state, and easy to extend. |
| `QM-03` | Configuration is separated appropriately by environment and demonstration-only behavior is explicit. |
| `QM-04` | README and build metadata accurately document prerequisites, endpoints, data lifetime, and verification commands. |
| `QM-05` | Dependency versions are centrally managed and no unnecessary runtime dependencies are introduced. |
| `QM-06` | Logging, correlation, health/readiness, configuration ownership, and operational documentation support diagnosis without exposing internals. |

## Reliability and Security — 20%

| ID | Control |
| --- | --- |
| `RS-01` | Service write operations have transactional boundaries and reads use appropriate read-only behavior. |
| `RS-02` | Known domain and validation failures produce stable, client-safe `ApiError` responses. |
| `RS-03` | Malformed payloads, invalid enum values, persistence failures, and unexpected exceptions do not expose internals and are logged safely. |
| `RS-04` | Write endpoints and sensitive/operational endpoints have security appropriate to their declared deployment context. |
| `RS-05` | No secrets or production credentials are committed; sensitive customer data is not unnecessarily exposed or logged. |
| `RS-06` | Persistence and collection access avoid destructive production defaults and unbounded production-scale reads. |
| `RS-07` | Concurrent updates, duplicate requests, downstream failures, timeouts, retries, and degraded behavior are handled explicitly wherever those risks apply. |

## Testing and Verification — 15%

| ID | Control |
| --- | --- |
| `TV-01` | The required clean test command succeeds for the reviewed revision. |
| `TV-02` | Service unit tests cover successful behavior and meaningful exceptional paths. |
| `TV-03` | Integration tests cover create, retrieve, update, delete, validation, and not-found HTTP behavior with isolated data. |
| `TV-04` | Tests cover global unexpected-error handling and security behavior when those concerns apply. |
| `TV-05` | JaCoCo verification succeeds with at least 85% line coverage and 80% branch coverage when branch instructions exist; exact results are reported. |
| `TV-06` | Repository-defined formatting, static-analysis, and dependency-vulnerability checks run successfully. Use `UNVERIFIED` for each capability not configured. |

## Scope handling

In repository mode, assess every control against the complete current application. In staged or
branch mode, assess changed behavior plus directly affected context. Mark a control `N/A` only when
the scoped change cannot affect it; use `UNVERIFIED` when it could apply but evidence is missing.
