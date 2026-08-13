# Solution Architect Review Controls

## Status and scoring model

Assign every control exactly one status:

| Status | Display | Score | Meaning |
| --- | --- | ---: | --- |
| `PASS` | ✅ | 1.0 | Verified evidence satisfies the complete control. |
| `PARTIAL` | 🟡 | 0.5 | Verified evidence satisfies only part of the control. |
| `FAIL` | ❌ | 0.0 | Verified evidence contradicts the control. |
| `UNVERIFIED` | ❓ | Excluded | Relevant evidence is insufficient; include in evidence coverage. |
| `N/A` | ➖ | Excluded | The control genuinely cannot apply; exclude from evidence coverage. |

For each category:

```text
verified applicable = PASS + PARTIAL + FAIL
applicable = verified applicable + UNVERIFIED
category satisfaction = (PASS + 0.5 * PARTIAL) / verified applicable * 100
category evidence coverage = verified applicable / applicable * 100
```

When no control is verified applicable, display satisfaction as `UNSCORED`. Calculate overall
satisfaction as the weighted mean of scored categories, renormalized across only their weights.
Overall evidence coverage is all verified applicable controls divided by all applicable controls.
`Percentage not met` is `100 - overall satisfaction`.

The percentage means **percentage of verified, applicable review controls satisfied**. Never
describe it as the percentage of source code that is correct, safe, secure, tested, or production-ready.

## Category weights and baselines

| Category | Weight | Baseline |
| --- | ---: | ---: |
| Architecture and Design | 20% | 85% |
| Code Quality and Maintainability | 15% | 80% |
| API and Integration Design | 15% | 85% |
| Security and Data Protection | 15% | 85% |
| Reliability and Operational Readiness | 15% | 85% |
| Data and Persistence | 10% | 85% |
| Testing and Verification | 10% | 80% |
| **Overall** | **100%** | **85%** |

Show each score, baseline gap, and evidence coverage. Retain status counts for arithmetic and the
compact `At a glance` totals, but do not repeat per-category raw counts in the architect report.
Baselines are diagnostic; the readiness gates remain authoritative.

## Readiness boundaries and ordered gates

Report three independent decisions. A numerical score never overrides a serious verified finding,
and a result on one boundary must not be substituted for another.

### Developer implementation readiness

Use `PASS` or `CHANGES REQUIRED`. Apply these gates in order:

1. A required build/test failure caused by reviewed code, a coverage-gate failure, or any Blocker
   developer implementation defect or architectural conformance violation produces `CHANGES REQUIRED`.
2. Any High developer implementation defect or architectural conformance violation produces
   `CHANGES REQUIRED` and blocks PR approval.
3. Any Medium `FAIL` in either of those classifications, or overall satisfaction below 70%, produces
   `CHANGES REQUIRED`.
4. Otherwise produce `PASS`.

Architect decisions and evidence gaps do not count as implemented developer defects. They may
require later developer work after a decision is recorded, but do not change this boundary merely
because that work cannot yet be implemented.

### Architect review readiness

Use `READY FOR ARCHITECT REVIEW`, `READY FOR ARCHITECT DECISION`, or `NOT READY because required
implementation evidence is still missing`. Apply these gates in order:

1. A required build/test/coverage failure or a material implementation gap that prevents meaningful
   architecture evaluation produces `NOT READY because required implementation evidence is still missing`.
2. One or more `Architect decision required` findings produces `READY FOR ARCHITECT DECISION`.
3. Otherwise produce `READY FOR ARCHITECT REVIEW`.

Developer defects that do not prevent evaluation may coexist with `READY FOR ARCHITECT DECISION`;
they remain visible under developer implementation readiness. An architect-owned decision routes
work to the architect and never produces “do not submit for architect review” by itself.

### Production or release readiness

Use `READY`, `NOT READY`, or `INSUFFICIENT EVIDENCE`. Apply these gates in order:

1. Any Blocker, required build/test/coverage failure, unresolved material architect decision,
   architectural conformance violation, or High developer defect produces `NOT READY`.
2. A material evidence gap affecting production behavior, or overall evidence coverage below 60%,
   produces `INSUFFICIENT EVIDENCE` unless the prior gate already establishes `NOT READY`.
3. Any remaining verified release-affecting `FAIL` or `PARTIAL` produces `NOT READY`.
4. Otherwise produce `READY`.

Baselines remain diagnostic. Standards satisfaction does not override these gates. The architect
owns the final approval decision; the tool never approves, rejects, merges, commits, or pushes.

## Finding evidence contract

Every `FAIL` and `PARTIAL` must include: control ID; category; severity; classification (`Developer
implementation defect`, `Architectural conformance violation`, `Architect decision required`, or
`Evidence gap`);
exact file and line range or method; observed implementation; concrete failure scenario; technical
or business impact; smallest practical correction; verification step; confidence (`High`, `Medium`,
or `Low`); and whether developer action or architect judgment owns the next decision.

Use `Blocker` only for severe security exposure, data corruption, irreversible loss, or a broken
critical path. Use `High` for realistic incorrect behavior, authorization failure, major reliability
risk, or significant architecture violation. Use `Medium` for material maintainability, validation,
failure-handling, coupling, or test gaps. Use `Low` for localized clarity, consistency,
diagnosability, or low-risk maintainability concerns. Do not inflate severity.

## Architecture and Design — 20%

| ID | Control |
| --- | --- |
| `AD-01` | Modules, packages, layers, and services preserve repository-defined boundaries and dependency direction without cycles or bypasses. |
| `AD-02` | Controller, orchestration, business, persistence, and integration responsibilities are separated; domain decisions do not leak unnecessarily into transport, storage, or framework types. |
| `AD-03` | Coupling, cohesion, abstractions, interfaces, patterns, and extension points match demonstrated needs without duplication or speculative indirection. |
| `AD-04` | Transaction, state, concurrency, and shared-mutable-state ownership are explicit, with no hidden temporal or call-order dependency. |
| `AD-05` | API, event, persistence, and data-contract changes preserve compatibility or provide an explicit evolution and migration decision. |
| `AD-06` | The declared deployment and execution model avoids evidenced single points of failure, unsafe scaling assumptions, and unowned cross-component decisions. |

## Code Quality and Maintainability — 15%

| ID | Control |
| --- | --- |
| `QM-01` | Main execution paths and relevant null, empty, boundary, date, numeric, enum, and collection cases are technically correct. |
| `QM-02` | Classes and methods are focused; control flow, naming, and intent are readable without material dead code, duplication, or unnecessary complexity. |
| `QM-03` | Exception translation, resource lifecycle, concurrency, thread safety, and blocking behavior are correct for the execution model. |
| `QM-04` | Performance, memory, environmental coupling, and testability risks are bounded and explicit rather than hidden in implementation details. |
| `QM-05` | Language/framework conventions, dependency management, pinned toolchains, configuration ownership, and documentation support maintainable and reproducible development. |
| `QM-06` | Repository-defined formatting or lint enforcement runs successfully; use `UNVERIFIED` when no capability is configured. |

## API and Integration Design — 15%

| ID | Control |
| --- | --- |
| `AI-01` | HTTP/RPC boundaries use intentional DTOs, trust-boundary validation, correct status semantics, stable error contracts, and deterministic serialization. |
| `AI-02` | Public API and integration contracts address backward compatibility, versioning, schema evolution, and consumer impact where change requires them. |
| `AI-03` | Idempotency, duplicate requests, pagination, filtering, ordering, and replay behavior are defined wherever clients or orchestrators can trigger them. |
| `AI-04` | External calls define appropriate timeouts, bounded retry with backoff/jitter, circuit breaking, partial-failure behavior, and safe fallbacks where applicable. |
| `AI-05` | Messaging integrations define delivery, duplication, ordering, replay, poison-message, and schema-evolution behavior where applicable. |
| `AI-06` | Cross-system, database, and messaging consistency boundaries avoid unsafe dual writes and make compensation or recovery ownership explicit where applicable. |

For this repository, assess compatibility against an actual changed contract or an explicitly
supported consumer commitment. If neither exists in scope, use `UNVERIFIED` or `N/A`; do not invent
an architecture flaw solely because a versioning policy is absent from this demonstration service.

## Security and Data Protection — 15%

| ID | Control |
| --- | --- |
| `SD-01` | Authentication, endpoint authorization, object-level access control, and operational access controls match declared trust boundaries; the external role/claim contract is owned and operationally verified rather than only mocked. |
| `SD-02` | Untrusted input is validated against injection, unsafe deserialization, path traversal, SSRF, and equivalent reachable attack paths. |
| `SD-03` | Secrets, credentials, cryptography, token parsing, issuer/audience validation, key handling, and the production identity-provider/JWKS integration use safe, operationally verified platform mechanisms. |
| `SD-04` | Responses, logs, metrics, traces, and DTOs avoid excessive or sensitive-data exposure and do not disclose internal failure details. |
| `SD-05` | Personal or regulated data classification, minimization, retention, deletion, residency, and audit needs are implemented or explicitly identified when repository evidence makes them applicable. |
| `SD-06` | Environment defaults, management endpoints, local facilities, production profiles, and security error behavior are secure for their declared context. |

For this repository, the presence of `customerName` alone does not establish a retention,
classification, residency, or audit requirement. When no governing business or platform policy is
available, assess `SD-05` as `UNVERIFIED` and request the missing decision; do not present it as a
verified defect.

## Reliability and Operational Readiness — 15%

| ID | Control |
| --- | --- |
| `RO-01` | Downstream calls bound time, retries, backoff, jitter, cancellation, and retry-storm risk where applicable. |
| `RO-02` | Idempotency, replay safety, partial failure, transaction consistency, fallback, and graceful degradation protect the primary business flow. |
| `RO-03` | Threads, connections, queues, pools, resource limits, startup, shutdown, and cancellation behavior are bounded and lifecycle-safe where applicable. |
| `RO-04` | Environment configuration is safe, and health, liveness, readiness, and dependency signals accurately represent service availability without exposing secrets. |
| `RO-05` | Logs, metrics, tracing, correlation identifiers, and error context make important production failures diagnosable without duplicate or sensitive logging. |
| `RO-06` | Failure isolation, recovery, reprocessing, rollback, and operational ownership are explicit for evidenced failure modes. |

## Data and Persistence — 10%

| ID | Control |
| --- | --- |
| `DP-01` | Entity, domain, DTO, and persistence concerns are separated appropriately; lazy-loading and persistence-context behavior do not leak across boundaries. |
| `DP-02` | Queries avoid evidenced N+1 behavior, unbounded reads, missing pagination, avoidable full scans, and missing indexes for established access paths. |
| `DP-03` | Transaction scope, propagation, rollback, atomicity, and error translation preserve consistency across each business operation. |
| `DP-04` | Locking, optimistic or pessimistic concurrency, uniqueness, and duplicate handling prevent lost or inconsistent updates where applicable. |
| `DP-05` | A durable target database is operationally evidenced; migrations, schema evolution, rollback/roll-forward compatibility, backup/restore ownership, and database-specific coupling are safe for the declared deployment model. |
| `DP-06` | Constraints, precision, nullability, referential integrity, retention, and deletion behavior preserve data quality and applicable protection requirements. |

For this repository, assess database constraints against evidenced write paths and integrity risks.
Do not assume direct imports, additional writers, or bypass paths that the repository does not show.
When a new constraint is justified, recommend a forward Flyway migration rather than editing an
applied migration.

## Repository production-readiness interpretation

Apply these explicit repository standards in full-repository review:

- If H2 is the only exercised runtime database and PostgreSQL exists only as a driver plus unresolved
  properties, mark `DP-05` `FAIL` as `Architect decision required`, not as an architecture flaw.
  Require a decision on the deployable PostgreSQL contract, credentials, availability, backup,
  restore, and recovery ownership, followed by target-engine migration verification. Do not claim
  that PostgreSQL is running merely because configuration exists.
- If production JWT behavior is exercised only through a mocked `JwtDecoder` and no signed-token,
  JWKS, issuer, or identity-provider contract test exists, mark `SD-03` `FAIL` as an independent
  `Architect decision required` for issuer, audience, JWKS, key rotation, validation, and failure
  behavior; do not label the unresolved trust contract an architecture flaw.
- If route-level role checks exist but the external roles-claim schema, allowed values,
  provisioning/revocation ownership, and signed-token allow/deny behavior are not evidenced, mark
  `SD-01` `PARTIAL` as `Architect decision required`; acknowledge the implemented rules and request
  a decision on the claim contract and lifecycle ownership rather than calling it an architecture flaw.
- Keep those three root causes separate. Their owners, failure scenarios, decisions, and
  verification plans are different.
- `AI-03` evaluates the client-visible pagination contract. When page/size validation, ordering,
  metadata, and results are correct, an internal full-table read does not reduce `AI-03`; assess the
  unbounded query through `DP-02` and `QM-04` only as a `Developer implementation defect`.
- Under `QM-02`, treat repeated identical repository reads inside one method with no intervening
  state change as a separate `Developer implementation defect`: the method must reuse the first
  result. This is independent of full-table pagination because it affects single-resource get,
  update, and delete paths and has its own correction and verification.

## Testing and Verification — 10%

| ID | Control |
| --- | --- |
| `TV-01` | Required clean test and build commands succeed for the reviewed filesystem state or revision. |
| `TV-02` | Tests cover primary success paths, validation failures, boundary conditions, error contracts, and meaningful regression behavior. |
| `TV-03` | Tests cover applicable downstream failures, rollback, retries, duplicate delivery or requests, concurrency, cancellation, and recovery behavior. |
| `TV-04` | Authorization, object access, serialization, API contracts, and security failure behavior have risk-proportionate tests. |
| `TV-05` | Unit, integration, and contract tests assert observable behavior, remain deterministic and isolated, and avoid excessive mocking or shared state. |
| `TV-06` | JaCoCo verification succeeds with at least 85% line coverage and 80% branch coverage when branch instructions exist; report exact results. |
| `TV-07` | Repository-defined formatting, static-analysis, CI, and dependency-vulnerability checks run successfully; use `UNVERIFIED` for each absent capability. |

For this repository, judge concurrency testing in proportion to the reviewed change and established
risk. Do not create a standalone finding for a hypothetical concurrency path when version checks,
optimistic locking, and relevant behavior tests already provide reasonable evidence.

## Scope handling

In repository mode, assess every control against the complete current solution. In staged or branch
mode, assess the changed behavior and directly affected context only. Mark a control `N/A` when the
scoped system or change cannot affect it. Use `UNVERIFIED` when it is relevant but evidence or
business/platform context is missing. Do not penalize a repository for having no messaging,
downstream client, cloud service, batch path, or regulated data when evidence shows the concern does
not apply.
