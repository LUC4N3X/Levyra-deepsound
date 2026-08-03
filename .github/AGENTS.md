# Levyra GitHub Automation Instructions

These instructions extend the root `AGENTS.md` for workflows, actions, issue templates, release automation, and repository scripts under `.github/`.

Before editing a workflow, load `.agents/skills/levyra-ci-workflows/SKILL.md` and, when applicable, `.agents/skills/levyra-security-review/SKILL.md` and `.agents/skills/levyra-release-check/SKILL.md`.

## Trust boundaries

- Treat `pull_request`, `pull_request_target`, forks, issue comments, workflow dispatch inputs, downloaded artifacts, and checked-out repository code as distinct trust levels.
- Never expose secrets to untrusted code, fork-controlled refs, generated scripts, logs, caches, or artifacts.
- Keep permissions explicit and at least privilege.
- Do not execute pull-request code with elevated permissions or write-capable tokens.
- Validate and quote every untrusted input passed to shell, paths, release metadata, or API calls.

## Workflow architecture

- Reuse existing Android build/release, Desktop build/release, F-Droid, extractor/config sync, validation, and duplicate-guard workflows.
- Do not create parallel workflows that publish the same artifact or respond to the same release trigger without a documented reason.
- Preserve Android and Desktop version, tag, artifact, and release separation.
- Preserve artifact names and paths consumed by later jobs.
- Make success, no-change, skipped, cancelled, and failure outcomes semantically distinct.
- Keep caches deterministic and prevent untrusted executable output from crossing trust boundaries.
- Review third-party actions for provenance and supply-chain impact before adding or upgrading them.

## Release safety

- Do not publish, tag, upload, create a release, or change repository settings unless explicitly authorized.
- Keep real signing material and credentials only in approved GitHub secrets or environments.
- Never add secret values, keystores, `.env`, `local.properties`, API tokens, or private URLs to workflow files or artifacts.
- Keep Desktop releases from becoming the repository's Android `Latest` release.

## Validation

Check YAML syntax, event filters, permissions, concurrency, matrices, conditions, shell quoting, output propagation, artifact retention, cache keys, version parsing, release tags, and secret availability.

When debugging CI, inspect the actual failing job and step logs. Do not claim a workflow is fixed until the relevant run or equivalent local command provides evidence.
