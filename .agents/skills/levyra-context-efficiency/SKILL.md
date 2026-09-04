---
name: levyra-context-efficiency
description: Automatically use at the start of any non-trivial Levyra engineering task that needs repository exploration, cross-session memory, and for builds, tests, lint, logs, broad searches, dependency output, Git/GitHub inspection, CI diagnostics, agent setup, or other high-volume work. Reduce token waste through progressive context discovery, optional claude-mem retrieval, and RTK while preserving raw evidence whenever compression could change a diagnosis.
---

# Levyra context-efficiency workflow

## Purpose

Reduce tokens by sending agents less irrelevant context, not by making technical
work less precise. This skill controls discovery, persistent-memory retrieval,
and command-output volume; it does not replace the affected Levyra domain skill
or validation.

Use it automatically before broad repository reading on any non-trivial task and
when a task depends on useful context from an earlier session. Tiny,
already-local edits do not need ceremony beyond the baseline below.

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

Load this skill immediately when a task needs repository exploration, useful
cross-session continuity, or is likely to produce repeated/high-volume output.
Also load the matching product, security, performance, CI, or release skill.

For shell work:

1. before the first noisy command, automatically ensure the expected RTK without
   asking the owner: run `scripts/ensure-rtk.ps1 -Quiet` in Windows PowerShell or
   `./scripts/ensure-rtk.sh --quiet` in Bash/WSL/Linux/macOS;
2. the ensure script validates raw `rtk --version` and `rtk gain` and installs
   only the owner-authorized pinned `rtk-ai/rtk` revision when needed;
3. if Cargo is unavailable or installation fails, report the bootstrap limit
   once and continue raw instead of weakening sandboxing or validation;
4. the older manual bootstrap remains available through
   `scripts/setup-ai.ps1 -InstallRtk` or `./scripts/setup-ai.sh --install-rtk`;
5. prefer supported RTK wrappers for noisy success-heavy output;
6. keep short commands and exact-output checks raw;
7. if RTK hides a root cause, truncates required evidence, rejects a command, or
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
rtk adb logcat -d -t 400
rtk adb -s <serial> logcat -d -t 400
rtk summary adb shell dumpsys <service>
```

Never treat compact or empty output as proof of success. Verify exit status and
the authoritative success/failure marker.

## Android and ADB output

Treat ADB by output shape instead of trying to maximize RTK adoption percentage.
A tiny raw device query costs less context than an unnecessary wrapper, while an
unbounded logcat or dumpsys can flood a session.

For routine textual diagnostics:

- prefer a bounded source command first, especially `adb logcat -d -t 400`;
- invoke the project filter explicitly as `rtk adb logcat -d -t 400` or
  `rtk adb -s <serial> logcat -d -t 400`;
- use `rtk summary adb shell dumpsys <service>` for large textual ADB output that
  has no dedicated project filter;
- narrow dumpsys to the relevant service/package before summarizing whenever
  possible;
- rerun the exact raw ADB command when the compact form can hide the deciding
  failure, lifecycle, package, permission, performance, or device evidence.

Keep raw when compression provides no useful win or could corrupt the payload:

- `adb devices` and one-line `getprop`, `pm path`, readiness, input, or control
  checks;
- `adb exec-out screencap -p`, redirected screenshots, binary stdout, file
  transfers, and other payload-producing commands;
- exact reproduction output that must be preserved byte-for-byte.

Do not chase `rtk discover` adoption by wrapping tiny ADB commands. The goal is
less model context without weakening Android diagnostics.

## Repository discovery ladder

Use this order unless the task already provides a narrower starting point:

```text
root / nearest AGENTS
→ matching skill(s)
→ optional focused claude-mem lookup when prior-session context matters
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

## Persistent session memory

Levyra uses `thedotmack/claude-mem` only as an optional retrieval layer. Read
`docs/ai/CLAUDE_MEM.md` before installing, repairing, changing, or depending on
that integration.

When the runtime exposes claude-mem memory/MCP tools and the task depends on
previous work, use them automatically with progressive disclosure:

```text
search
→ timeline when chronological context matters
→ get_observations for only the relevant IDs
→ verify the result against the current repository
```

Prefer project-scoped and narrowly worded searches. Fetch detailed observations
only after the compact result index identifies a relevant item.

Useful triggers include continuation requests, repeated bugs/CI failures,
earlier rejected approaches, prior architectural decisions, and work that would
otherwise require reconstructing a previous investigation.

If a local shell-capable Claude Code, Codex CLI, or Antigravity runtime needs
that prior-session context but claude-mem tools are absent, the owner authorizes
one automatic attempt to run the pinned dedicated setup:

```text
Windows: scripts/setup-claude-mem.ps1
Bash/WSL/Linux/macOS: ./scripts/setup-claude-mem.sh
```

Do not ask the owner to type the setup command. Do not repeatedly reinstall in
the same task. If setup, worker health, or MCP discovery remains unavailable,
continue without claude-mem.

Memory is never authoritative. `AGENTS.md`, current code, approved planning,
tests, CI, device/runtime evidence, and direct owner decisions outrank stored
observations. If memory is stale or conflicts with current evidence, discard the
memory conclusion and follow the current source of truth.

Fail open:

- if the worker or MCP tools are unavailable, unhealthy, slow, or unsupported,
  continue without memory;
- never block a build, edit, review, test, or answer waiting for claude-mem;
- do not claim a memory lookup happened when the runtime did not expose the
  tools;
- do not enable cloud sync or experimental semantic injection implicitly;
- never store or retrieve secrets, keystores, `.env`, cookies, tokens, signing
  material, private URLs, or `local.properties` as project memory.

For ChatGPT specifically, use claude-mem only when a compatible MCP app is
actually connected. Repository configuration alone cannot make ChatGPT reach a
local claude-mem worker.

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
- claude-mem is a focused cross-session retrieval layer, not a replacement for
  RTK and not an excuse to inject large histories into every prompt.
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
rtk gain --graph
rtk discover --all --since 7
rtk session
```

RTK estimates command-output savings only. Do not equate that number with total
model-billing savings; instructions, skill bodies, conversation history,
reasoning, generated output, and memory retrieval are separate costs.

## Setup and safety

RTK setup is documented in `docs/ai/RTK.md`. Automatic session/task bootstrap
uses `scripts/ensure-rtk.ps1` or `scripts/ensure-rtk.sh`; the broader manual setup
remains available through `scripts/setup-ai.ps1` / `scripts/setup-ai.sh`.

The owner also authorizes the pinned claude-mem integration to bootstrap
automatically only when prior-session memory is materially useful and the local
runtime does not already expose its memory tools. Manual forcing remains
available through `scripts/setup-ai.ps1 -ClaudeMem` or
`./scripts/setup-ai.sh --claude-mem`.

- Do not enable unrestricted sandboxing, global `danger-full-access`, or silent
  approval bypasses.
- Do not expose or filter away secrets, signing material, tokens, cookies,
  private URLs, keystores, or local properties.
- Do not install other executables/plugins without explicit owner authorization;
  the pinned RTK and claude-mem bootstraps are the standing exceptions defined
  by the repository owner.
- Do not let compact context or persistent memory replace tests, CI, review, or
  device validation.
- Never infer permission to commit, push, open/merge a PR, tag, or release.

## Validation

After changing this workflow or its routing, run:

```text
python3 scripts/validate_claude_mem.py
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

On Windows:

```text
py scripts/validate_claude_mem.py
py scripts/validate_agent_config.py
py scripts/validate_ai_efficiency.py
```

Final reporting must say which memory/context/commands were used or compacted,
which evidence was intentionally kept raw, which commands were rerun raw, exact
validation results, and remaining diagnostic risk.
