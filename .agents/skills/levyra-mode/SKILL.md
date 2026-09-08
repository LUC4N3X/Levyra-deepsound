---
name: levyra-mode
description: Use only for explicit owner execution cues such as VAI, PROCEDI, INTERVIENI, FAI TU, FALLO TU, or requests to open/create a PR. Keep execution fast and low-noise while preserving full Levyra engineering rigor.
---

# Levyra mode

## Purpose

Make owner-directed work faster to execute and easier to follow. Save tokens by
removing redundant context and narration, never by reducing technical analysis.
Root/scoped `AGENTS.md`, always-on guards, domain skills, validation, review, and
publication controls remain authoritative.

## Full engineering rigor

For every code, build, workflow, schema, or agent-config change:

1. inspect the current owner/control flow and nearby tests before editing;
2. identify the root cause or exact implementation contract instead of stacking guesses;
3. make the smallest coherent change and preserve lifecycle, cancellation, concurrency, data, security, and compatibility semantics that apply;
4. run focused validation after the latest material edit and keep failures classified from evidence;
5. inspect the complete final diff, run `git diff --check`, and perform the required code-review gate before delivery or publication.

Never skip source inspection, a necessary test, decisive diagnostics, security
review, or final code review to save tokens. Token efficiency is allowed to
remove repetition, not engineering depth.

## Token discipline

- Load only skills that materially affect the current task.
- Search symbols/paths first and read bounded source ranges before whole files.
- Do not reread unchanged instructions, files, logs, or evidence already in context.
- Compact routine successful command output; preserve or rerun raw when exact evidence can change the diagnosis.
- Keep updates to new evidence, current action, and real blockers.
- Carry forward compact verified handoffs, not exploratory chatter or disproved hypotheses.

## Execution shape

- Start with the action, result, or blocker; avoid generic preamble.
- Keep visible multi-step plans to at most five concrete steps.
- Perform authorized tool/edit/review work directly instead of handing it back to the owner.
- Finish the requested scope before unrelated cleanup.
- Report exact delivery and validation state; never manufacture completion or timing.
- Commit, push, PR, merge, release, deployment, version changes, and repository settings still require the authorization defined by Levyra.

## Provenance

The action-first interaction ideas are selectively adapted from the MIT-licensed
`ayghri/i-have-adhd` project. Levyra keeps only the execution ergonomics and does
not add diagnosis-specific behavior or an external runtime dependency.
