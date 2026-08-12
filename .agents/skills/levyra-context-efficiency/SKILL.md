---
name: levyra-context-efficiency
description: Automatically use at the start of any non-trivial Levyra engineering task that needs repository exploration, and for builds, tests, lint, logs, broad searches, dependency output, Git/GitHub inspection, CI diagnostics, agent setup, or other high-volume work. Reduce token waste through progressive context discovery and RTK while preserving raw evidence whenever compression could change a diagnosis.
---

# Levyra context-efficiency workflow

## Purpose

Reduce tokens by sending agents less irrelevant context, not by making technical
work less precise. This skill controls discovery and command-output volume; it
does not replace the affected Levyra domain skill or validation.

Use it automatically before broad repository reading on any non-trivial task.
Tiny, already-local edits do not need ceremony beyond the baseline below.

## Always-on context budget

Before opening large files or running broad commands:

1. identify the likely architecture owner, affected module, and exact question
   the next read must answer;
2. search names/symbols/paths first;
3. read the smallest useful range, focused diff, signature, or nearby test;
4. expand only when the bounded read leaves a concrete unanswered question;
5. do not reread unchanged files or repeat evidence already present in the
   current context;
6. load only matching `levyra-*` skills; do not preload the whole skill tree;
7. keep discarded hypotheses, superseded logs, and exploratory noise out of
   handoffs once direct evidence has replaced them.

This is progressive disclosure, not arbitrary truncation. If a missing line can
change correctness, read it.

## Automatic routing

Load this skill immediately when a task needs repository exploration or is
likely to produce repeated/high-volume output. Also load the matching product,
security, performance, CI, or release skill.

For shell work:

1. verify the expected RTK with raw `rtk --version` and `rtk gain` before the
   first noisy command in a session;
2. if unavailable, follow the owner-authorized bootstrap in root `AGENTS.md`:
   `scripts/setup-ai.ps1 -InstallRtk` on Windows or
   `./scripts/setup-ai.sh --install-rtk` on Linux/macOS;
3. if bootstrap is blocked, report it once and continue raw;
4. prefer supported RTK wrappers for noisy success-heavy output;
5. keep short commands and exact-output checks raw;
6. if RTK hides a root cause, truncates required evidence, rejects a command, or
   makes exit status/success ambiguous, rerun the exact command raw.

Useful routes include:

```text
rtk gradlew <tasks>
rtk git status
rtk git diff
rtk git log
rtk gh pr view <number>
rtk gh run list
rtk test <command>
rtk err <command>
rtk grep <pattern> <path>
rtk find <pattern> <path>
rtk log <file>
rtk summary <command>
```

Never treat compact or empty output as proof of success. Verify exit status and
the authoritative success/failure marker.

## Repository discovery ladder

Use this order unless the task already provides a narrower starting point:

```text
root / nearest AGENTS
→ matching skill(s)
→ focused search / symbol / filename
→ bounded source or test range
→ local control/data-flow expansion
→ broader file/module read only if still necessary
```

Rules:

- read only relevant sections of `SPEC.md`, `ROADMAP.md`, `TASKS.md`, and
  architecture docs;
- prefer exact symbols, call sites, tests, and focused PR hunks over whole-file
  dumps;
- do not open generated files, lockfiles, full logs, dependency trees, or large
  docs unless they directly answer the task;
- when several searches return the same evidence, stop collecting duplicates;
- use a fresh narrow search instead of scrolling an unrelated giant file;
- current repository evidence beats remembered summaries.

## Context checkpoints for long work

After a meaningful investigation or implementation phase, carry forward a
compact verified handoff rather than the entire exploratory trail:

- objective / acceptance criterion;
- verified root cause or current decision;
- architecture owner and exact files/symbols in scope;
- preserved behavior;
- applied changes or next concrete step;
- tests/checks and exact outcomes;
- unresolved risks or blocked evidence.

Do not compress away details that another agent needs to reproduce a failure or
verify a security/performance conclusion. A compact handoff replaces stale
exploration, not source-of-truth evidence.

## Prefer RTK

Use RTK for long/repetitive:

- Gradle build, test, lint, check, dependency, and connected-test output;
- broad Git/GitHub/CodeRabbit inspection;
- repeated logcat, server, extractor, playback, device, CI, Docker, or adb logs;
- large `rg`, `grep`, `find`, tree, package, or dependency output.

## Keep raw evidence

Do not compact or summarize away the deciding evidence for:

- failing compiler/test/lint diagnostics when the compact form is insufficient;
- security findings, exploit paths, URLs/redirects/MIME/permissions, secrets,
  signing, checksums, or trust boundaries;
- Perfetto trace evidence, exact SQL/query failures, thread/frame timing, Binder
  dependencies, scheduler states, or performance measurements used for a root
  cause;
- R8 missing-class/rule diagnostics, mapping evidence, release-only runtime
  failures, or analyzer output needed to justify a keep rule;
- exact stdout/stderr, quoting, encoding, protocol, or regression-test output;
- `--stacktrace`, `--full-stacktrace`, `--info`, or `--debug` diagnostics when
  those details are required.

## Token-efficiency rules

- Do not add another global compression proxy or always-on third-party token
  layer when RTK plus focused discovery already handles the problem.
- Do not trade correctness for a smaller context window.
- Do not repeat long repository instructions inside every skill; reference the
  canonical owner instead.
- Prefer thin runtime bridges to duplicated skill bodies.
- Avoid narrating routine successful tool steps back into the working context;
  retain conclusions and evidence that affect the next decision.
- For independent tickets/phases, prefer a fresh context seeded with the compact
  verified handoff over dragging forward obsolete exploration.

## Savings measurement

Use:

```text
rtk gain
rtk gain --daily
rtk gain --history
rtk discover --all --since 7
rtk session
```

RTK estimates command-output savings only. Do not equate that number with total
model-billing savings; instructions, skill bodies, conversation history,
reasoning, and generated output are separate costs.

## Setup and safety

Setup is documented in `docs/ai/RTK.md` and automated by
`scripts/setup-ai.ps1` / `scripts/setup-ai.sh`.

- Do not enable unrestricted sandboxing, global `danger-full-access`, or silent
  approval bypasses.
- Do not expose or filter away secrets, signing material, tokens, cookies,
  private URLs, keystores, or local properties.
- Do not install other executables/plugins without explicit owner authorization;
  the pinned RTK bootstrap is the standing exception defined by `AGENTS.md`.
- Do not let compact context replace tests, CI, review, or device validation.
- Never infer permission to commit, push, open/merge a PR, tag, or release.

## Validation

After changing this workflow or its routing, run:

```text
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

On Windows:

```text
py scripts/validate_agent_config.py
py scripts/validate_ai_efficiency.py
```

Final reporting must say which context/commands were compacted, which evidence
was intentionally kept raw, which commands were rerun raw, exact validation
results, and remaining diagnostic risk.