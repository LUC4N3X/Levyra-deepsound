---
name: levyra-ci-workflows
description: Implement, debug, or review Levyra GitHub Actions, CI checks, Android and Desktop builds, artifact handling, release automation, F-Droid, configuration sync, permissions, and workflow security.
---

# Levyra CI and workflow workflow

## Required context

1. Read the root `AGENTS.md` and `.github/AGENTS.md`.
2. Read `.claude/rules/testing-release.md` and `.claude/rules/security.md`.
3. Inspect every workflow, reusable action, script, build file, secret/input contract, artifact path, and trigger affected by the change.
4. Inspect recent failing job logs when the task is a CI failure; do not infer a root cause from the check title alone.

## Guardrails

- Keep workflow permissions at least privilege and explicit where practical.
- Treat `pull_request`, `pull_request_target`, forks, checked-out code, issue comments, and workflow dispatch inputs as distinct trust boundaries.
- Never expose secrets to untrusted code or upload secret-bearing files as artifacts.
- Reuse existing release, validation, extractor-sync, F-Droid, and duplicate-guard workflows instead of creating parallel automation.
- Pin or constrain third-party actions according to the repository's existing policy and review supply-chain impact before adding one.
- Keep Android and Desktop release triggers, versions, tags, artifacts, and Latest-release behavior separate.
- Preserve artifact names and paths relied on by downstream jobs or release steps.
- Ensure caches use safe, deterministic keys and cannot substitute untrusted executable output across trust boundaries.
- Make no-change, skipped, cancelled, and failed outcomes visible and semantically distinct.

## Validation

Validate YAML structure, expressions, event filters, permissions, matrix behavior, shell quoting, paths, conditions, output propagation, artifact retention, and secret availability. Compare the workflow with the corresponding local Gradle command. Report job-log evidence, checks not reproducible locally, and any manual release verification still required.
