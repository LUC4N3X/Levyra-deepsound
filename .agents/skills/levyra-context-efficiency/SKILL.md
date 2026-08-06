---
name: levyra-context-efficiency
description: Automatically reduce AI context and token waste for Levyra by routing verbose shell work through RTK, selecting focused repository context, preserving full diagnostic evidence when needed, measuring savings, and keeping agent/plugin setup consistent across Codex, Claude Code, Antigravity, OpenClaw, and compatible runtimes. Use for builds, tests, lint, logs, broad searches, dependency output, Git/GitHub inspection, CI diagnostics, agent setup, or any task likely to produce large repetitive command output.
---

# Levyra context-efficiency workflow

## Purpose

Use RTK and focused repository discovery to reduce repetitive command output
before it reaches an AI model. This skill improves context efficiency; it does
not weaken validation, hide failures, or replace Levyra's domain skills.

Load this skill automatically whenever work involves one or more of:

- Gradle builds, tests, lint, dependency reports, or Android instrumentation;
- Git status, logs, diffs, branches, commits, or GitHub CLI output;
- CI, CodeRabbit, compiler, Android logcat, extractor, playback, or server logs;
- broad file searches, repository inventories, or dependency listings;
- agent configuration, plugin installation, RTK setup, or token-savings review;
- output that is expected to be long, repetitive, or mostly successful noise.

Always load the matching Levyra domain skill as well. This skill controls
context handling, not product behavior.

## Automatic routing

1. Detect whether `rtk` is available with `rtk --version`.
2. When available, prefer RTK for supported noisy commands:

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

3. Keep short commands and exact-output checks raw.
4. If RTK rejects a command, obscures the root cause, truncates required
   evidence, or changes exit-code interpretation, rerun the exact command raw.
5. Use verbose or stacktrace flags when the task requires complete diagnostics;
   RTK's Gradle integration passes through full output for diagnostic modes.
6. Never report a check as passed merely because filtered output is short or
   empty. Verify the exit status and final success/failure marker.

## Levyra command policy

### Prefer RTK

- `gradlew`/`gradlew.bat` build, test, lint, check, dependencies, and connected
  test tasks;
- broad `git diff`, `git log`, `git status`, and GitHub CLI inspection;
- CodeRabbit output and large review reports;
- repeated server, extractor, playback, device, CI, Docker, or adb logs;
- large `rg`, `grep`, `find`, tree, package, or dependency output.

### Prefer raw output

- a single source file or narrowly scoped diff;
- exact stdout/stderr, quoting, encoding, checksum, signature, or exit-code
  validation;
- security-sensitive URL, redirect, MIME, permission, signing, secret-scanning,
  or release evidence;
- a failing compiler/test/lint command after the compact output is insufficient;
- commands using `--stacktrace`, `--full-stacktrace`, `--info`, or `--debug`;
- any command whose complete output is required by a regression test.

## Context selection

- Read root and nearest path-specific `AGENTS.md` first.
- Read only the relevant sections of `docs/project/SPEC.md`, `ROADMAP.md`, and
  the active `TASKS.md` phase.
- Load every matching `levyra-*` skill, but do not preload unrelated skills.
- Search narrowly before opening large files.
- Prefer signatures, relevant ranges, and focused diffs over entire generated
  files or logs.
- Preserve current repository evidence over remembered behavior or agent
  summaries.

## Savings measurement

Use these commands after RTK has been active long enough to collect data:

```text
rtk gain
rtk gain --daily
rtk gain --history
rtk discover --all --since 7
rtk session
```

RTK estimates command-output token reductions. Do not describe those numbers as
an equal reduction in the total model bill, because prompts, system
instructions, conversation history, and generated output remain separate costs.

## Setup and discovery

Repository setup is documented in `docs/ai/RTK.md` and automated by:

```text
scripts/setup-ai.ps1
scripts/setup-ai.sh
```

The setup scripts install or configure only explicitly selected components,
initialize RTK instructions, hooks, or integrations for detected supported
agents, and optionally install the plugins listed in `codex-plugins.txt`.

After pulling instruction, skill, rule, or integration changes, restart the
coding agent or begin a new conversation so its skill inventory and runtime
integration are rebuilt.

## Safety boundaries

- Do not enable unrestricted sandboxing, global `danger-full-access`, or silent
  approval bypasses.
- Do not expose or filter away secrets, signing material, tokens, cookies,
  private URLs, keystores, or local properties.
- Do not install executables, plugins, hooks, or global configuration unless the
  user explicitly requested setup or passed the matching setup-script flag.
- Do not let compact output replace independent review, CI, manual device
  testing, or owner-controlled publication.
- Never infer permission to commit, push, open a PR, merge, tag, or release.

## Validation

After changing this skill, RTK configuration, setup scripts, plugin manifest, or
AI documentation, run:

```text
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

On Windows:

```text
py scripts/validate_agent_config.py
py scripts/validate_ai_efficiency.py
```

The final report must state whether RTK was available, which commands were
filtered, which were rerun raw, exact check results, blocked checks, and any
remaining diagnostic risk.
