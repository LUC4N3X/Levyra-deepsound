# Levyra Always-On Agent Guards

This is Levyra's non-optional execution harness for AI-assisted engineering.
It applies to Claude Code, Codex, ChatGPT, Google Antigravity/Gemini, OpenCode,
OpenClaw-delegated coding runtimes, and any compatible agent working on this
repository.

These guards are not skills. A model does not decide whether to activate them.
They apply on every engineering task and remain active when no specialized
`levyra-*` skill matches.

Runtime-specific hooks may enforce parts of this contract mechanically. When a
runtime cannot provide equivalent hooks, its repository/project instructions
must apply the same contract directly and the repository validators remain the
backstop.

## 1. Scoped instructions are mandatory

Before inspecting or changing a repository path, apply:

1. root `AGENTS.md`;
2. every nearer `AGENTS.md` from the repository root to the target path;
3. this always-on guard;
4. `docs/ai/AI_ENGINEERING_GUARDRAILS.md` for implementation and review;
5. matching specialized skills only after the mandatory context above is active.

Never rely on memory of an `AGENTS.md`. Current files win. Runtime hooks should
inject or re-anchor applicable scoped instructions around mutation events when
the runtime supports it.

## 2. Matching skills are selected automatically

The owner never has to name a Levyra skill. Every supported runtime must infer
matching skills from the current task before broad repository reading, editing,
or shell work.

- Claude Code and Codex use `scripts/agent_skill_router.py` from their prompt
  hooks so both runtimes share the same deterministic routing table.
- Google Antigravity/Gemini, OpenCode, ChatGPT, and delegated runtimes use the
  same root `AGENTS.md` routing contract and canonical `.agents/skills/` paths;
  shell-capable runtimes may invoke the shared router directly.
- Load every genuinely matching skill, including companion skills required by
  the routing contract, but never preload the whole skill tree.
- A runtime must not wait for the owner to say "use skill X".
- Specialized skills add domain procedure; they never disable this always-on
  harness or owner publication controls.

## 3. Current file before mutation

An existing file must be grounded in its current repository content before it is
mutated.

- Read or automatically inject the current target before `Edit`, `Write`,
  `apply_patch`, multi-edit, or equivalent mutation.
- Whole-file replacement of an existing file requires a current full-file read
  in the active task/session when the runtime can track reads.
- If the file changed after that read, the previous read is stale.
- Patch-style edits may use an automatically injected bounded current region when
  that region contains the patch anchors; large unrelated files must not be
  dumped into context merely to satisfy this guard.
- Never inject or mutate secrets, `.env`, `local.properties`, keystores, signing
  material, or equivalent protected local files.

This is a freshness guard, not a ritual. Do not repeatedly force the same read
when the current hash is already grounded. Do not use a blanket overwrite ban
that traps the agent in retry loops.

## 4. Minimum-change tie-breaker

When two implementations satisfy the same requirements and evidence gates,
prefer the one that:

1. changes fewer production files;
2. adds fewer new symbols and abstractions;
3. crosses fewer existing ownership boundaries;
4. leaves more unrelated code byte-for-byte untouched.

File count is a tie-breaker, not a reason to hide a correctness dependency. Do
not compress a coherent multi-file fix into the wrong owner merely to produce a
smaller number.

## 5. Acceptance gates are always active

Every code-bearing, build/configuration, migration, performance, security, CI,
or agent-configuration change uses evidence-gated completion from
`docs/ai/EVIDENCE_GATED_COMPLETION.md`.

A tiny single-file non-behavioral documentation/copy edit may use one implicit
gate. Everything else must establish observable acceptance conditions before or
at the start of implementation.

Only direct evidence is `PASS`. `FAIL`, `BLOCKED`, and `UNRUN` remain open.

After the last material edit, the current generation of the work must have:

- a focused validation appropriate to the change;
- a review of the actual final diff;
- `git diff --check` or equivalent whitespace/conflict validation;
- the mandatory pre-delivery code review for code-bearing work;
- no unresolved always-on guard finding.

A runtime must not call the task complete merely because it compiled once before
later edits or because another agent said it passed.

A genuinely `BLOCKED` gate may end the current turn after the remaining possible
checks are complete. Report the unavailable prerequisite exactly, leave the gate
open, and do not describe the task as complete. The harness must never force an
agent into an infinite retry loop merely because a device, SDK, executable,
permission, or other required environment is unavailable.

## 6. Durable task checkpoint and retry discipline

For an open engineering task, keep a small local checkpoint outside tracked
repository content. The checkpoint may preserve only the minimum continuity
needed to resume safely:

- latest redacted task goal;
- changed paths and current edit generation;
- validation and final-diff review generation;
- `ACTIVE`, `BLOCKED`, or completed state;
- one concrete next action;
- a fingerprint/count for the latest failed shell command.

Do not create `task_plan.md`, `findings.md`, `progress.md`, `GATES.md`, or another
tracked task-state framework merely to satisfy this rule. Existing project
planning files remain authoritative when the work is already a tracked phase.

The durable checkpoint belongs under Git metadata or equivalent local ephemeral
storage and must never contain credentials, raw access values, keystores,
cookies, private URLs, signing material, or full command output.

An identical shell command may be retried once after its first failure when a
transient failure is plausible. If the exact command fails twice in the same
edit generation, a third identical attempt is forbidden until the hypothesis,
input, environment, installation state, or implementation materially changes.
Change the diagnostic instead of burning tokens on a loop.

## 7. Compaction and resume must re-anchor state

Compaction, summarization, resume, `/clear`, or another deliberate fresh Claude
context must not silently drop an open task contract.

Re-anchor, at minimum:

- the owner's latest requested outcome and hard scope boundaries;
- applicable root/scoped instructions and this always-on guard;
- files already changed;
- the latest verified root cause or rationale;
- acceptance-gate status;
- validation performed after the latest edit;
- final-diff review state;
- the next concrete action from the durable checkpoint;
- publication authorization and actual publication state.

Never treat a compacted summary or local checkpoint as newer evidence than the
repository.

For Claude Code, context hygiene is deliberate. `/clear` is useful between
unrelated completed tasks, especially after a session has accumulated failed
approaches, but must never be used while a task is still `ACTIVE` or `BLOCKED`
without first preserving the checkpoint. Project hooks cannot execute slash
commands; when the safe-clear threshold is reached they may remind Claude to use
`/clear` before the next unrelated task, and `SessionStart(clear)` must restore
only the repository guards/checkpoint needed for the fresh session.

## 8. Required tools may be installed narrowly

A runtime may install a missing tool when the active task genuinely requires it
and the installation materially improves correctness or validation.

- Check whether the required tool and a suitable version already exist first.
- Install only the specific missing dependency; never run broad system/package
  upgrades merely to satisfy an agent workflow.
- Prefer an official/reputable upstream and a project-local or user-local install
  when practical.
- Verify the installed command/version before relying on it.
- Do not silently add unrelated plugins, daemons, telemetry, services, or global
  packages.
- Do not elevate to administrator/root or weaken sandbox/approval controls unless
  the owner has explicitly authorized that elevation.
- If a safe installation path is unavailable, mark the affected gate `BLOCKED`
  and report the exact prerequisite instead of bypassing security controls.

## 9. Android reverse-engineering route is mandatory when applicable

Tasks that decompile or analyze APK/XAPK/AAB/DEX/JAR/AAR artifacts, use jadx or
smali, extract APIs from compiled Android artifacts, recover R8/Kotlin metadata,
or trace compiled call flows must load `levyra-android-reverse-engineering`
before broad artifact work. Add `levyra-security-review` for trust-boundary,
manifest, transport, exposed-secret, or API-security findings, and add
`levyra-r8-proguard` when obfuscation/shrinker behavior is material.

Use a fingerprint-first workflow. Determine framework, HTTP stack, obfuscation,
native libraries, and packaging shape before spending context on a full Java
or Kotlin decompile. For native Kotlin/KMP artifacts, prefer evidence-preserving
Kotlin/R8 metadata recovery and string/annotation anchors over guesses based on
obfuscated class names.

Claude may use the checked-in approved upstream Android reverse-engineering
plugin. Other runtimes use Levyra's native adapter. Missing required tools such
as jadx, bundletool, apktool, or an optional second decompiler may be installed
under the narrow-installation rule above.

Dynamic instrumentation is not an automatic continuation of static analysis.
Use it only for owner-controlled or explicitly authorized targets and only for
the requested diagnostic purpose. Never collect credentials/secrets or automate
bypasses whose purpose is to gain access to protected third-party functionality.

## 10. AI-comment slop is rejected

New source comments must carry information that cannot live safely in clear code.
Reject newly added comments that narrate the agent's process, introduce numbered
implementation steps, announce what "we" are about to do, restate an obvious
line, or leave generated tutorial prose in production code.

Preserve comments that are legally or mechanically required, including license
headers, generated/tool directives, lint/suppression markers, protocol or
compatibility contracts, and genuinely non-obvious safety invariants.

The repository comment-slop checker scans added source lines; it does not demand
rewriting unrelated pre-existing comments.

## 11. Structural navigation is deterministic when applicable

For symbol, call-flow, reference, ownership, or rename work, use the narrowest
available structural tool before broad text search:

1. project jCodeMunch when available for indexed symbol/relation discovery;
2. an already-available LSP for definition/references/diagnostics when it answers
   the question more directly;
3. an already-available AST-aware search for structural multi-file matching;
4. native bounded search/read as the correctness fallback.

Do not install a new LSP or AST engine merely to satisfy this ordering unless the
active task actually needs that capability. Structural tooling reduces discovery
noise; it never replaces current source reads, tests, or exact diagnostics.

## 12. Safety and publication do not weaken

These guards never authorize unrestricted sandboxes, `danger-full-access`,
approval bypasses, telemetry, unrelated external plugins, commit, push, PR
creation, merge, tag, release, deployment, version changes, or repository-setting
changes.

Task-required tool installation under section 8 is explicitly narrower than a
general plugin or system-modification authorization and does not expand
publication permissions.

The existing owner-controlled publication rules remain authoritative.

## Delivery state

Keep these states distinct:

`planned -> edited -> locally validated -> final diff reviewed -> committed -> pushed -> pull request opened -> CI passed -> independently reviewed -> merged -> released`

Never collapse missing states into `done`.
