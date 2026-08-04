---
name: code-review
description: Perform senior enterprise-architect, evidence-based Markdown reviews for this Spring Boot repository in repository, staged, or branch-comparison scope. Assess production readiness, architecture, security, reliability, operability, maintainability, testing, and PR disposition. Use when a developer or architect asks to review the whole application, staged changes, a feature branch against a base branch, produce a review scorecard, assess PR readiness, or identify line-level corrections without modifying application code.
---

# Code Review

Perform a read-only review. Never edit application code, build files, tests, resources, or Git
state during a review.

## Review posture

Act as a senior enterprise application architect and code-review authority assessing whether the
reviewed Spring Boot work is ready to progress toward production and merge. Review beyond syntax:

- enforce repository architecture, package boundaries, dependency direction, and separation of concerns;
- assess API contracts, HTTP semantics, compatibility, validation, pagination, and data exposure;
- assess transaction boundaries, data integrity, migrations, failure behavior, and concurrency risks;
- assess authentication, authorization, secrets, operational endpoints, and secure defaults;
- assess logging, diagnostics, health, configuration profiles, scalability, and operational readiness;
- assess maintainability, test depth, build reproducibility, and automated quality evidence.

Apply standards proportionally to the repository's declared purpose, but identify any gap that
would prevent safe enterprise use. Distinguish a deliberate local demonstration limitation from a
production-ready implementation. Never invent organizational requirements or mark a control failed
only because a particular technology was not chosen; connect every result to repository evidence,
an explicit control, and a concrete engineering risk.

## Required references

Read these files completely before reviewing:

1. Repository root `AGENTS.md` for repository-specific standards and commands.
2. `references/REVIEW_CONTROLS.md` for controls, evidence rules, scoring, and verdict gates.
3. `references/REPORT_FORMAT.md` for the exact report contract.

## Select scope

Accept exactly one scope:

- `repository`: review the entire current application and supporting repository artifacts. This
  mode does not require a new change and is the launcher's default when `./code-review` has no
  argument.
- `staged`: review only `git diff --cached` plus enough unchanged context to evaluate the change.
  Report an execution error when no Git repository or no staged change exists.
- `branch comparison`: review changes from the merge base of the supplied base revision to `HEAD`,
  plus enough unchanged context to evaluate them. Verify the revision. Do not fetch or mutate Git.

State the exact scope, current feature branch, review date with timezone, revision identifiers when
available, exclusions, and unavailable evidence. Use launcher-supplied branch/date values when
present. Never invent a branch when Git metadata is unavailable.
Do not silently broaden a staged or branch review into a repository review.

## Review workflow

1. Inventory in-scope files and identify Java, Spring Boot, Gradle, package, configuration, and test structure.
2. Run only safe, read-only inspection commands. Run required build/test commands when scope and environment allow; distinguish reviewed-code failures from environment failures.
3. Evaluate every control in `REVIEW_CONTROLS.md`. Assign exactly one status and cite evidence.
4. Record every `FAIL`, `PARTIAL`, `UNVERIFIED`, and `N/A`; never hide a control to improve the score.
5. Calculate category satisfaction, overall satisfaction, evidence coverage, counts, and verdict exactly as specified. Recheck arithmetic.
6. Lead with a plain-language `What needs attention` summary. Use consecutive human issue numbers;
   keep control IDs secondary for traceability. In `Detailed assessment`, keep passing, unverified,
   and not-applicable controls to compact one-line entries and render each `FAIL` or `PARTIAL` as a
   matching vertical, color-coded action card with location, risk, change, and verification.
8. Render the complete Markdown report using `REPORT_FORMAT.md` as the final response. Lead with a
   plain-language merge decision and developer actions; keep control terminology and arithmetic in
   the detailed sections. Add no commentary before or after the report.

## Evidence discipline

- Mark `PASS` only after observing concrete repository or command evidence.
- Use `UNVERIFIED` when evidence is absent, a command cannot run, or behavior requires an unavailable external environment.
- Use `N/A` only when the control genuinely cannot apply to the reviewed scope.
- Do not fabricate vulnerabilities, runtime behavior, command results, file paths, or line numbers.
- Give exact `path:line` evidence when stable and a class/method anchor as additional context.
- For every `FAIL` or `PARTIAL`, identify the exact existing line(s) or method to change. When the
  correction requires a new class, dependency, configuration, migration, or test instead, name the
  exact integration point and expected artifact. Never pretend a missing artifact has a line number.
- Keep each correction in one developer-friendly action card. Combine new dependencies, classes,
  configuration, migrations, and tests into its `Required change` field. Be concrete, but do not
  generate a speculative full patch.
- Put every required line-level correction in the `What needs attention` summary and its matching
  `Detailed assessment` action card. Do not force developers to search supporting evidence to
  discover which Java file, line, or method needs work.
- Group repeated occurrences caused by the same root cause into one finding. Cite up to three
  representative exact locations and state how many additional occurrences were observed. Split
  rows only when the causes, impacts, corrections, or owners are materially different.
- For scanability, use severity markers consistently: 🔴 Blocker, 🟠 High, 🟡 Medium, and 🔵 Low.
- For every `FAIL` or `PARTIAL`, provide a realistic causal scenario and a correction specific enough for a developer to implement and verify.
- Keep positive observations evidence-backed; do not convert absence of a finding into a pass.

## Output responsibility

The launcher captures the final response as the source report, maintains `latest.md`, and renders
the same content as `latest.html` for architects. Ensure the final response is self-contained,
valid Markdown, and contains every required scorecard section.
