# Report Format

Return one complete Markdown document with the sections below in this order.

# Code Review Report

## Decision

Start with a prominent, plain-language decision block:

```markdown
> ## 🟠 CHANGES REQUIRED — Do not merge yet
>
> **Developer action:** Fix the required items below and rerun the review.  
> **Architect action:** Return the PR for changes.  
> **Why:** <one sentence naming the findings that triggered the verdict gate>.  
> **Build and tests:** <plain-language result>.
```

Use these labels consistently:

| Verdict | Heading | PR guidance |
| --- | --- | --- |
| `PASS` | `🟢 READY FOR ARCHITECT APPROVAL` | May approve and merge after normal architect review. |
| `PASS WITH FOLLOW-UP` | `🟡 READY WITH FOLLOW-UP` | Architect may approve with tracked follow-up. |
| `CHANGES REQUIRED` | `🟠 CHANGES REQUIRED` | Do not merge; return to developer. |
| `FAIL` | `🔴 REJECT / BLOCK` | Reject or block until the critical failure is resolved. |
| `INSUFFICIENT EVIDENCE` | `⚪ NOT READY FOR DECISION` | Gather missing evidence before approval. |

Do not show raw `PASS`, `PARTIAL`, `FAIL`, `UNVERIFIED`, and `N/A` counts before this decision.

## At a glance

Use a developer-friendly table:

| Question | Answer |
| --- | --- |
| Feature branch | `<current branch, detached revision, or unavailable reason>` |
| Review date | `<YYYY-MM-DD HH:MM:SS timezone and UTC offset>` |
| Can this PR merge? | `<Yes / No / Architect decision>` |
| Did the build and tests pass? | `<plain-language result>` |
| Test coverage | `<line percentage vs 85%; branch percentage vs 80% or N/A; gate result>` |
| Required fixes | `<count of failed controls and serious partial findings>` |
| Recommended improvements | `<count>` |
| Checks meeting the standard | `<PASS count> of <verified applicable count>` |
| Checks needing evidence | `<UNVERIFIED count>` |
| Review confidence | `<evidence coverage and plain-language interpretation>` |
| Standards score | `<n.nn>% — verified applicable controls satisfied>` |

Follow the table with one sentence explaining that the standards score is not the percentage of
source code that is correct.

## What needs attention

This is the architect's working section and must appear before scope and scoring details. Present
plain-language issue names first; control IDs are secondary traceability references, not issue
numbers.

For every `FAIL` and `PARTIAL`, add one row ordered by severity and then likely business impact:

| # | Issue | Severity | Decision impact | Where | Required action |
| ---: | --- | --- | --- | --- | --- |
| 1 | Unsafe exception handling (`RS-03`) | 🟠 High | Blocks approval | `GlobalExceptionHandler.java:17` | Return a stable error and add regression tests. |

Use simple consecutive numbers (`1`, `2`, `3`) only in this table and the matching detailed action
cards. Never use control IDs such as `RS-03` as the user-facing issue number. If there are no
verified failed or partial controls, state that no verified code corrections are required.

Follow with **Evidence still missing**, grouping `UNVERIFIED` controls into no more than three
plain-language bullets. Explain what is absent and why it matters; do not disguise missing evidence
as a pass or a verified defect.

## Review scope

State mode, repository root, feature/current branch, review date with timezone, reviewed
revisions/diff when available, included paths, excluded paths, and evidence limitations. Never
substitute the base branch for the current feature branch in a branch-comparison review.

## Category assessment

Do not put `PASS`, `PARTIAL`, `FAIL`, `UNVERIFIED`, or `N/A` counts in this table. Those raw counts
do not help a developer understand the result. Show one readable result and explanation instead:

| Category and importance | Score / baseline | Result | Architect summary |
| --- | ---: | --- | --- |

Show importance in the category label, for example `Architecture and Design (25%)`. Include all
five categories and an overall row. Use the baselines in `REVIEW_CONTROLS.md` and show score and
baseline together, for example `83.33% / 85.00% (-1.67)`. Set `Result` to `✅ Meets baseline`, `❌ Below baseline`, or
`❓ Insufficient evidence`. Explain the actual strength or gap in `Architect summary`. State below
the table that verdict gates override numerical baselines.

## Detailed assessment

Merge control evidence and required changes here. Group content by the five categories. Do not
create separate `Control evidence`, `Required changes`, `Findings`, or supporting-evidence sections.

For `PASS`, `UNVERIFIED`, and `N/A`, use one compact line per control with the readable result first
and the control ID last:

```markdown
### Architecture and Design

- ✅ Layer packages verified under `src/main/java/.../order`. (`AD-01`)
- ❓ Formatter command is not configured. (`CS-04`)
- ➖ Not applicable because <short reason>. (`AD-05`)
```

For every `FAIL` and `PARTIAL`, use one vertical color-coded action card:

```markdown
### 🟠 1. Unsafe exception handling

- **Control:** `RS-03` · Reliability and Security · `FAIL`
- **Where:** `src/.../GlobalExceptionHandler.java:17-30`
- **Why it matters:** Only two exception types are translated; malformed input can return an
  inconsistent contract and unsafe client error. Confidence: High.
- **Required change:** Add client-safe malformed/unexpected handlers, safe logging, and focused tests.
- **Verify:** `./gradlew clean test --tests '*OrderApiIntegrationTest'`
```

Rules:

- Use the same plain consecutive issue number assigned in **What needs attention**. The control ID
  belongs only in the `Control` field.
- `Where` must contain exact paths and lines when code exists, with a method/class anchor when useful.
- If code does not yet exist, write `New file: <exact expected path>` and also name the existing
  integration point, such as `build.gradle:dependencies`.
- `Why it matters` must compactly include evidence, a concrete failure scenario, impact, and
  confidence. Limit it to two short sentences.
- `Required change` must describe the correction concretely, including required
  dependencies, configuration, migrations, classes, API contracts, or tests. Do not say only
  "fix", "improve", "refactor", or "add security".
- `Verify` must contain an executable command and, where relevant, the focused test behavior.

Use these severity markers everywhere severity appears:

| Marker | Severity | Meaning |
| --- | --- | --- |
| 🔴 | Blocker | Critical issue that blocks the review immediately. |
| 🟠 | High | Serious issue requiring changes before approval. |
| 🟡 | Medium | Material engineering gap that should be corrected. |
| 🔵 | Low | Non-blocking improvement. |

When one root cause affects many files, group it into one action card. Cite up to three representative
`path:line` locations and state the number of additional confirmed occurrences; do not create one
card per file. When it needs new integration, name both the build/configuration insertion point and
the expected new artifact in `Required change`. Do not merely say
"add security", "improve tests", or "handle errors".

If there are no required changes, say so explicitly; never invent one. Every control result must
appear exactly once in this detailed section; the attention table is only a concise summary.

## Recommended follow-up

Use unchecked Markdown boxes for non-blocking improvements. Reference control IDs, locations, and
verification. Do not repeat required items from the top section. Do not present `UNVERIFIED` as a
code change unless gathering evidence requires repository configuration.

## Positive observations

List only evidence-backed strengths with control IDs and locations or commands.

## Build and verification results

For every command attempted, show exact command, exit status, result, and relevant output summary.
List configured checks that were unavailable or absent. Show a concise JaCoCo summary containing
line and branch percentages, thresholds, and the verification result. JaCoCo XML may be inspected
internally as machine evidence, but never expose raw XML, an XML path, or an XML-derived detail dump
in the architect report. If no branch counter exists, report branch coverage as `N/A — no branch
instructions` rather than inventing 100%. Include a prominent clickable relative link as
`[Open JaCoCo HTML coverage report](../jacoco/test/html/index.html)` so an architect never needs to
open the XML report.

## Final recommendation

State verdict, applicable gate, required developer action, architect/PR disposition, and next review
scope. Never imply the tool itself approved, merged, rejected, committed, or pushed.
