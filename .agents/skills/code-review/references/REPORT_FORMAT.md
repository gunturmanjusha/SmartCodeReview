# Solution Architect Report Format

Return one complete Markdown document with the sections below in this exact order.

# Solution Architect Code Review

## Decision

Start with a prominent plain-language decision block:

```markdown
> ## 🟠 DEVELOPER CHANGES REQUIRED — Do not submit for architect approval yet
>
> **Developer action:** Fix the required items below and rerun the review.
> **Architect action:** Validate architecture-boundary corrections or make only the verified
> decisions identified for architect judgment; otherwise state `None`.
> **Top issue:** [Issue 1 — <plain-language problem>](#issue-1) — <one-sentence consequence>.
> **Build and tests:** <plain-language result>.
```

Use these verdict headings consistently:

| Verdict | Heading | PR guidance |
| --- | --- | --- |
| `PASS` | `🟢 READY FOR ARCHITECT REVIEW` | May proceed to normal architect review and approval. |
| `PASS WITH FOLLOW-UP` | `🟡 READY WITH MINOR FOLLOW-UP` | Architect may review with tracked non-blocking follow-up. |
| `CHANGES REQUIRED` | `🟠 DEVELOPER CHANGES REQUIRED` | Return to the developer before formal architect review. |
| `FAIL` | `🔴 REVIEW BLOCKED` | Block until the critical failure is resolved. |
| `INSUFFICIENT EVIDENCE` | `⚪ INSUFFICIENT EVIDENCE` | Do not decide until missing evidence is supplied. |

Do not show status counts before the decision.

## Executive assessment

Use exactly these six short, labelled bullets so an architect understands the result without
reading the rest of the report:

- **Application:** State the language/runtime, framework, business capability, API style, and
  architecture unit. For this repository, say `Java 23 Spring Boot Order Management REST
  microservice` when that remains supported by evidence. Call one independently deployable service
  a `microservice`, not a “microservices application.”
- **Review decision:** State the plain-language verdict and whether formal architect review may
  proceed.
- **Verified flaws:** State the number of `FAIL` and `PARTIAL` findings by severity and whether any
  one controls the verdict. Also state the architecture-flaw and developer-code-flaw counts.
- **Top risk:** Link directly to the detailed issue, name it in plain language, and explain its
  concrete failure/impact in one sentence, for example
  `[Issue 1 — App can report ready while the database is down](#issue-1)`.
- **Developer action:** Summarize the required corrections, not missing contextual evidence.
- **Architect action:** State the verified architecture correction to validate or the specific
  decision required from an architect. Do not promote unavailable external context into an action;
  otherwise state `None`.

Do not use control IDs in this section. Avoid ceremonial praise, vague phrases such as “moderate
risk” without explanation, and duplication of the category scorecard.

## At a glance

Use this table:

| Question | Answer |
| --- | --- |
| Current branch | `<current branch, detached revision, or unavailable reason>` |
| Review date | `<YYYY-MM-DD HH:MM:SS timezone and UTC offset>` |
| Review scope | `<repository / staged / branch comparison and concise diff>` |
| Can this work proceed? | `<Yes / No / Architect decision>` |
| Build and tests | `<plain-language result>` |
| Test coverage | `<line percentage vs 85%; branch percentage vs 80% or N/A; gate result>` |
| Developer fixes | `<count>` |
| Architect decisions | `<count of verified findings that require a design decision; exclude UNVERIFIED context>` |
| Architecture flaws | `<count>` |
| Developer code flaws | `<count>` |
| Checks meeting the standard | `<PASS count> of <verified applicable count>` |
| Checks needing evidence | `<UNVERIFIED count>` |
| Evidence coverage | `<n.nn>% and plain-language confidence>` |
| Standards score | `<n.nn>% — verified applicable controls satisfied>` |

State immediately below the table that the standards score is not the percentage of source code
that is correct, secure, tested, or production-ready.

## Findings and recommended corrections

For every `FAIL` and `PARTIAL`, add one row ordered by Blocker, High, Medium, then Low:

| # | Finding | Category / severity / owner | Evidence | Technical impact | Recommended correction |
| ---: | --- | --- | --- | --- | --- |
| [1](#issue-1) | JPA entity exposed through REST API | Architecture and Design · 🟡 Medium · Developer | `OrderController.java:getRaw(Long)` | Persistence fields become part of the HTTP contract. | Remove the raw endpoint and return only `OrderResponse`. |

Use consecutive human issue numbers. Never use control IDs as issue numbers. Combine repeated
occurrences with one root cause. If no verified failed or partial control exists, state that no
verified code correction is required. Do not expose control IDs in this executive-facing table;
introduce them with their evidence in the later detailed assessment. Every row must identify a
specific file plus line, method, class, configuration key, or missing integration point; explain a
concrete failure/impact; and state the smallest practical correction. This table is the primary
architect view of code flaws and recommended fixes. Link the issue number to its detailed action
card using `#issue-N`. Show the full review category, severity, and `Architect`, `Developer`, or
`Architect + Developer` owner in one compact column.

Report root causes, not control-count symptoms. When a missing test exists only because an
implementation defect lacks regression coverage, merge the test into the implementation issue and
its suggested fix. Do not create a second issue titled “X is untested.” List all affected controls
in the one detailed card; under any later category, use a short status cross-reference to that issue
so every control remains assessed exactly once.

Keep independently actionable defects separate even when they occur in the same class or review
category. Entity exposure and direct repository access, for example, are separate when each has its
own execution path, impact, and correction.

Do not use one finding to reduce every category it could hypothetically affect. Mark only controls
whose requirement is directly contradicted by current code. Keep an otherwise satisfied security,
reliability, maintainability, or testing control as `PASS`; describe possible future consequences in
the finding's impact instead of multiplying the score penalty.

Write for a senior software architect who has not read this repository. Use precise, conventional
Java, Spring, REST, security, persistence, and testing terminology, but avoid unexplained internal
control codes and unnecessarily specialized platform scenarios. Keep the finding title concise.
State repository evidence, the realistic technical impact, and an implementation-ready correction.

Never require preset findings. Select only defects demonstrated by the reviewed code, configuration,
tests, or executed verification. Missing business, deployment, consumer, or compliance information
belongs under evidence needed, not under findings, unless `AGENTS.md` explicitly requires the
capability and repository evidence proves that only a local substitute, placeholder, or mock exists.
Prefer a small number of material root causes over filling every category with a flaw.

### Evidence needed to complete the review

- Do not repeat `FAIL` or `PARTIAL` items; they are already visible in the findings table.
- Summarize all `UNVERIFIED` controls in no more than four grouped, plain-language bullets.
- Begin each bullet with its full review-category name, describe the missing evidence or decision,
  and do not expose internal control IDs here.
- Do not list `N/A`; it appears only in the detailed assessment.

## Architecture summary

Describe only evidenced facts using a compact table:

| Area | Evidence-based summary |
| --- | --- |
| Purpose and business flow | `<primary actor/request through response or persistence>` |
| Components and dependency flow | `<modules, layers, services, and dependency direction>` |
| Runtime and deployment model | `<language, framework, process, profiles, declared deployment>` |
| APIs and integrations | `<HTTP/RPC/events/downstream systems, or explicitly none observed>` |
| Data flow and persistence | `<DTO/domain/entity/database/migration path>` |
| Engineering controls | `<build, tests, coverage, CI/static analysis capabilities>` |

Do not invent a deployment topology, external integration, cloud platform, scale target, SLO, or
business requirement when the repository does not provide it.

## Review scope

State mode, repository root, current branch, review date with timezone, revision identifiers or
diff, included paths, excluded paths, working-tree state, and evidence limitations. In branch or
staged mode, list changed files and state that unrelated unchanged code was excluded. Never
substitute the base branch for the current feature branch.

## Category assessment

Use one readable table with all seven categories and an overall row:

| Category and importance | Score / baseline and coverage | Result | Architect summary |
| --- | ---: | --- | --- |

For each category, show satisfaction, baseline, signed gap, and evidence coverage in the second
column, for example `83.33% / 85.00% (-1.67); coverage 85.71%`. Set result to `✅ Meets baseline`,
`❌ Below baseline`, or `❓ Insufficient evidence`. Explain the meaningful strength or gap in the
summary. Use the weights and baselines from `REVIEW_CONTROLS.md`.

Do not add a second status-count summary, `Percentage met`, or `Percentage not met` block after
this table. Those values repeat the category assessment and make the decision harder to scan. The
`At a glance` section already contains the overall score, evidence coverage, passed-control count,
and missing-evidence count. Individual control statuses remain visible once in the detailed
assessment. State in the overall architect summary that verdict gates override numerical baselines
when a gate affects the decision.

## Detailed assessment

Group controls under all seven category headings. Every control must appear exactly once.
Introduce each control under its full category heading before showing its ID. Treat the ID as an
audit reference, not as the explanation a reader needs to understand the control.

For `PASS`, `UNVERIFIED`, and `N/A`, use one compact evidence line:

```markdown
### Architecture and Design

- ✅ Layer and dependency boundaries are verified under `src/main/java/...`. (`AD-01`)
- ❓ Production topology is not declared; a deployment diagram is needed to verify the concern. (`AD-06`)
- ➖ No message broker or event producer exists in scope. (`AI-05`)
```

For every `FAIL` and `PARTIAL`, use a matching vertical, color-coded action card. Keep every field to
one or two sentences and keep the fix sketch between 3 and 10 lines:

```markdown
### 🟡 1. JPA entity exposed through REST API

- **Control:** `AD-01`, `AD-02`, `DP-01` · Architecture and Design · `FAIL`
- **Type / classification / owner:** Architecture flaw · Local implementation defect · Developer,
  with architect validation
- **Location:** `src/main/java/.../OrderController.java:getRaw(Long)` and
  `OrderService.java:getEntity(Long)`
- **Repository evidence:** A public endpoint returns the JPA `Order` entity, while the normal endpoint
  deliberately returns `OrderResponse` and the repository standard forbids entity exposure.
- **Failure scenario:** Adding or renaming an entity field silently changes the JSON returned by the
  raw endpoint without an intentional API-contract decision.
- **Technical impact:** Transport and persistence contracts are coupled, so future entity changes can
  break clients or expose internal persistence state.
- **Recommended correction:** Remove `getRaw` and `getEntity`; keep the existing DTO-based `get`
  endpoint as the only single-order read contract.
- **Fix sketch:**

  ```java
  get(id):
      response = orderService.get(id)
      return HTTP 200
          ETag = response.version
          body = response
  ```
- **Verification:** Run `./gradlew test --tests '*OrderApiIntegrationTest'` and add an assertion that
  the response contract contains `totalPrice` rather than persistence-only behavior.
- **Confidence:** High
```

Action-card rules:

- Use the same issue number as the findings table.
- `Type / classification / owner` is required. Use exactly `Architecture flaw` or `Developer code
  flaw` for the type, followed by the evidence-based classification and owner.
- Use exact paths and lines plus a method/class anchor when code exists.
- For a missing artifact, write `New file: <expected path>` and name the existing integration point.
- `Repository evidence` must cite observed behavior, not inference presented as fact.
- `Failure scenario` must describe one realistic execution path in one sentence.
- `Technical impact` must state the engineering or business consequence without inflated severity.
- `Recommended correction` must be implementation-ready and name required contracts,
  dependencies, properties, migrations, classes, or tests; never say only “fix” or “improve.”
- `Fix sketch` is required for developer-owned findings. Use 3–10 lines of Java, SQL,
  configuration, or clear pseudocode that names the existing method or integration point. Never
  include a unified diff or full class in the architect report.
- Keep the sketch focused on the correction and its boundary. Do not add unrelated redesign,
  dependencies, framework layers, or invented APIs.
- For an architect-owned finding, include `Decision required`, `Recommended option`, `Tradeoff`, and
  `Implementation sketch`. The sketch must be 3–10 lines of configuration, deployment pseudocode,
  architecture steps, or contract definitions showing how the decision integrates; do not emit a
  full manifest or pretend the decision has already been made.
- A finding must arise from repository evidence. Do not promote `UNVERIFIED` context into a flaw or
  choose an issue because it makes the demonstration more dramatic.
- `Verification` must contain an executable command and focused expected behavior.
- `Confidence` must be `High`, `Medium`, or `Low`.
- Group one root cause across files and cite no more than three representative locations.
- Keep control, category, classification, and owner visible for auditability, but never use them as
  a substitute for the evidence or recommendation.
- Give every action-card heading the implied HTML anchor `issue-N`; the renderer creates it from the
  issue number.

For a secondary failed/partial control already covered by another root-cause card, use one compact
line under its full category heading instead of duplicating the issue:

```markdown
- 🟡 Audience regression coverage is included in [Issue 1](#issue-1). (`TV-04` · `PARTIAL`)
```

Use severity markers consistently: 🔴 Blocker, 🟠 High, 🟡 Medium, 🔵 Low.

## Architect review

### Issues developers should resolve before PR submission

List concise references to local implementation defects that developers can correct without an
architecture decision. Include issue number, control ID, owner/action, and verification. Do not
repeat full evidence. State `None` when no such issue exists.

### Decisions requiring architect judgment

List only cross-system impact, long-term design direction, business tradeoffs, scalability, data
ownership, security posture, platform standards, migration strategy, accepted technical debt, and
verified findings that cannot be corrected safely until an architect makes a choice. State the
decision needed and evidence available. Do not turn routine code corrections or missing external
context into architect decisions. Keep `UNVERIFIED` context only in the control-assessment section.
State `None` when no such decision exists; an architect may still validate completed
architecture-boundary corrections during normal review.

## Recommended follow-up

Use unchecked Markdown boxes for non-blocking improvements. Reference control IDs, locations, and
verification. Do not repeat required findings. Do not present missing evidence as a required code
change or follow-up unless gathering that evidence genuinely requires repository configuration and
is explicitly in scope.

## Positive engineering decisions

List only specific, evidence-backed strengths with control IDs and locations or commands.

## Verification summary

Keep this section short and scannable. Do not use a command-results table, reproduce Gradle task
output, expose machine-readable coverage artifacts, or dump verification logs into the architect
report. Use no more than six status lines:

```markdown
- ✅ **Build:** PASS — `./gradlew build --console=plain` completed successfully.
- ✅ **Tests:** PASS — `./gradlew clean test --console=plain`; 22 passed, 0 failed.
- ✅ **Coverage:** PASS — line 98.10% / 85%; branch 87.50% / 80%.
- ⚠️ **Additional quality checks:** NOT RUN — formatter, static analysis, CI, and dependency scan are not configured.

[Open JaCoCo HTML coverage report](../jacoco/test/html/index.html)
```

For every attempted command, include its exact command and `PASS`, `FAIL`, or `NOT RUN`, but combine
commands when their outcome is identical and the meaning remains clear. State whether a failure was
caused by reviewed code or the environment. Report absent formatting, lint, static-analysis, CI,
and dependency-vulnerability capabilities once as `UNVERIFIED`. Keep complete command output only
in the timestamped verification log maintained by the launcher.

Show exact line and branch coverage, thresholds, and gate result in the compact coverage line. When
no branch counter exists, report `N/A — no branch instructions`. Never expose or ask the architect
to read coverage XML; provide only the clickable JaCoCo HTML link.

## Final recommendation

Use this concise enterprise structure:

```markdown
> ## 🟠 DEVELOPER CHANGES REQUIRED BEFORE ARCHITECT REVIEW
>
> **Disposition:** Hold formal approval. The build may be healthy, but verified implementation defects
> or unresolved production architecture contracts still require action.
>
> **Release condition:** Resolve or explicitly accept every linked finding with evidence. A numerical
> score or passing build must not override a verified High or Blocker finding.

- **🟡 Developer next step:** Correct the linked implementation findings and add focused regression tests.
- **🟣 Architect next step:** Decide the linked cross-system, security, data, or platform contracts.
- **🟢 Exit criteria:** Required decisions are recorded, corrections and tests pass, and no verdict gate remains.
- **🔵 Re-review:** Run `./code-review` after the corrections and required verification succeed.
```

Choose the opening recommendation from: `Ready for architect review`, `Ready with minor follow-up`,
`Developer changes required before architect review`, `Major architectural changes required`, or
`Insufficient evidence`. Refer to findings by linked issue number and professional title, never by
an unexplained control ID, score formula, baseline gap, or “controlling gate.” Do not repeat scores,
coverage, command results, or evidence coverage here; they already appear above. Never imply that
the tool approved, rejected, merged, committed, or pushed. Link the disposition and action text to
the relevant professional issue titles. Keep this section compact: one decision banner and no more
than four action cards.
