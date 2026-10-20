---
name: levyra-context-efficiency
description: Use for genuinely high-volume Levyra work such as builds, tests, lint, logs, broad searches, dependency output, Git/GitHub or CodeRabbit inspection, CI diagnostics, agent setup, cross-domain exploration, or useful cross-session retrieval. Reduce token waste without reducing engineering rigor.
---

# Levyra context-efficiency workflow

## Purpose

Spend model context on code and decisive evidence. This skill reduces repeated
instructions, broad reads, noisy command output, and stale history. It never
replaces domain skills, source inspection, testing, review, or exact diagnostics.

**Token savings must come from context and output, never from shallower reasoning.**
If omitted evidence can change correctness, read or rerun it.

## Automatic routing

Route this skill only when the task is likely to create substantial repository or
command-output volume. Tiny edits, ordinary explanations, and already-local code
changes should not load it just because the prompt says "implement", "modify",
"analyze", or "inspect".

Before broad reading:

1. identify the architecture owner and the exact question the next read answers;
2. search symbol/path/call site first;
3. read the smallest useful source/test range;
4. expand only when a concrete unanswered question remains;
5. do not reread unchanged evidence already in context;
6. load only the domain/companion skills that materially affect correctness.

## RTK

For shell-capable noisy work, prefer the repository RTK layer after checking it.
Use `scripts/ensure-rtk.ps1 -Quiet` on Windows or `./scripts/ensure-rtk.sh --quiet`
elsewhere. Manual repair remains available through
`scripts/setup-ai.ps1 -InstallRtk` or `scripts/setup-ai.sh --install-rtk`.
If RTK is unavailable, continue raw rather than weakening validation.

Useful compact routes include:

```text
rtk gradlew <tasks>
rtk git diff
rtk git status
rtk gh pr view <number>
rtk test <command>
rtk err <command>
rtk grep <pattern> <path>
rtk log <file>
rtk adb logcat -d -t 400
rtk summary adb shell dumpsys <service>
```

Compact output is not proof of success. Check the command exit status and the
authoritative success/failure marker. If compression hides the deciding cause,
rerun the exact command raw.

## Keep decisive evidence raw

Do not compress away evidence needed for:

- compiler/test/lint failures whose exact diagnostic matters;
- security, redirects, MIME, permissions, secrets, signing, checksums, or trust boundaries;
- Perfetto/thread/frame timing, SQL/query failures, concurrency, or memory root cause;
- R8/Proguard missing-class, mapping, metadata, or release-only failures;
- exact protocol, quoting, encoding, stdout/stderr, or regression reproduction.

For ADB, bound noisy textual output first. Keep tiny control queries raw. Never
wrap binary/payload commands such as `adb exec-out screencap -p` in a text
compression path.

## Reversible Context Optimization (CCR)

For large, finite **text-only** commands that RTK does not already compact, use
`python scripts/levyra_context_optimizer.py run -- <command>` selectively.
The original output is cached when it fits the local LRU budget (150 entries,
25 MB) and storage is available. Nonzero exits are shown verbatim; exit codes
are preserved. This wrapper buffers output until completion, so do not use it
for interactive, long-running, binary or timing-sensitive commands.

Retrieve an output without re-running the command while its cache entry exists:

```text
python scripts/levyra_context_optimizer.py retrieve <cache_id>
python scripts/levyra_context_optimizer.py retrieve <cache_id> --lines <start>:<end>
```

**Do not chain RTK and the optimizer** on the same command. RTK has no
universally reliable output marker; the optimizer only recognizes its own
`#levyra-raw:` / `[Levyra Context:` markers. Prefer RTK for commands it
already handles well. Never re-run state-changing commands to recover output.
If the raw output cannot be cached, the optimizer returns it uncompressed.

**Privacy:** the cache stores local plaintext. Never route commands that may
print credentials, cookies, `.env`, keystores, signed URLs or private data
through the cache. Run them natively, or use `run --no-cache -- <command>`
when a wrapper is specifically needed. Do not upload cache contents.

## Cross-session context

Use claude-mem only when earlier-session context materially affects the task and
the runtime exposes it. Retrieve progressively: `search` -> `timeline` when
chronology matters -> `get_observations` for only relevant IDs -> verify against
the current repository. Current code, tests, CI, runtime evidence, and owner
decisions always outrank memory.

**Fail open:** if memory tooling is unavailable, unhealthy, or unsupported,
continue ordinary engineering without it. Do not enable cloud sync or semantic
injection implicitly. For ChatGPT, Repository configuration alone cannot make ChatGPT reach a
local claude-mem worker.

If a shell-capable runtime genuinely needs the optional integration and it is
missing, one bounded setup attempt may use `scripts/setup-ai.ps1` or
`scripts/setup-ai.sh`. Manual forcing remains `scripts/setup-ai.ps1 -ClaudeMem`
or `./scripts/setup-ai.sh --claude-mem`. Failure must not block ordinary work.

Never store or retrieve secrets, tokens, cookies, keystores, private URLs,
`.env`, or `local.properties` through project memory.

## Long-task checkpoints

Carry forward only the verified goal, root cause/decision, affected files or
symbols, preserved behavior, current edit state, validation results, real
blockers, and one next action. Drop superseded logs and disproved hypotheses.
A compact handoff never replaces source-of-truth evidence.

## Safety

- Do not trade correctness, review depth, or testing for a smaller context window.
- Do not enable `danger-full-access`, approval bypasses, or unrestricted sandboxing.
- Do not install unrelated plugins or broad system upgrades.
- Do not let RTK or memory hide security/signing/runtime evidence.
- Never infer permission to commit, push, open/merge a PR, tag, release, or deploy.

## Validation

After changing this workflow or its routing, run:

```text
python3 scripts/validate_claude_mem.py
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
python3 scripts/evaluate_skill_routing.py
```

On Windows use `py` for the same scripts when appropriate.
