# Levyra Claude Code

Use root `AGENTS.md` as the repository contract and
`docs/ai/AI_ENGINEERING_GUARDRAILS.md` as the shared implementation guardrail.
Apply nearer path-specific `AGENTS.md` files for files in scope.

## Automatic project tooling

The repository owner authorizes the pinned `rtk-ai/rtk` bootstrap for Levyra.
The `SessionStart` hook checks RTK automatically and installs the pinned build
when it is missing and Cargo is available. Do not ask the owner to run this
manually. If the hook reports that bootstrap is blocked, continue with raw
commands and report the limitation once.

When a task genuinely depends on prior-session context and claude-mem tools are
not available, load `levyra-context-efficiency` and follow its owner-authorized,
one-attempt automatic claude-mem bootstrap. Memory failure must never block the
task.

## Immediate context budget

Apply before broad reading on every real coding task:

- search path/symbol/call site first;
- read the smallest useful range, focused diff, or nearby test;
- expand only for a concrete unanswered question;
- do not reread unchanged evidence already in context;
- load only matching skills, never the whole skill tree;
- keep security, Perfetto, R8, signing, exact failures, and decisive diagnostics
  raw when compression could change the conclusion.

For non-trivial repository exploration or noisy output, invoke
`levyra-context-efficiency` immediately. Tiny already-local edits keep the same
baseline without loading extra skill text.

## Core engineering rules

- Protect playback reliability, explicit song/audio vs native-video choice,
  MediaSession/notification/Android Auto/queue synchronization, privacy, and
  user data before optional polish.
- Reuse the current architecture owner; never create a parallel source of truth
  for convenience.
- Keep blocking work off UI threads; preserve cancellation, lifecycle,
  concurrency, persistence, and compatibility semantics.
- State material assumptions/tradeoffs, prefer the simplest compatible path,
  and define `step -> verification` for non-trivial work.
- For non-trivial implementation, use `Plan -> Execute -> Verify`: make a brief
  evidence-based plan, implement one coherent slice, verify it before expanding.
  Do not stop for approval unless the owner reserved a checkpoint.
- Match the requested action mode. `inspect`, `review`, `diagnose`, and `report`
  authorize investigation only; `fix`, `update`, `address`, and `implement`
  authorize the requested code change and validation, but never publication.
- Treat screenshots and direct runtime observations as acceptance evidence that
  must be reconciled even when automated checks are green.
- Make surgical changes; avoid unrelated cleanup, speculative abstractions,
  dependency churn, or version changes.
- Do not add explanatory source-code comments. Prefer clear names, small
  functions, and explicit structure. Preserve only required license,
  generated/tool, lint/suppression, or real compatibility-contract comments.

## Automatic skill routing

Invoke every matching skill before broad reading/editing. Do not wait for the
owner to name it.

| Task | Skill |
| --- | --- |
| Non-trivial feature, architecture, unclear bug/regression, build/test failure, multi-step work | `levyra-real-engineering` |
| Playback, queue, Media3, MediaSession, notification, Android Auto, audio/video mode | `levyra-player` |
| InnerTube, extractor, stream resolution, retry/cache/fallback | `levyra-extractor` |
| Room, DAO, migration, schema, persistent stores | `levyra-database` |
| Compose UI/state/navigation/accessibility/RTL/localization | `levyra-compose` |
| Android runtime profiling, jank, Perfetto, CPU/thread, Binder, graphics, memory, I/O, power | `levyra-android-performance` plus affected domain skill |
| Intent/deep link/PendingIntent/exported component/provider/URI/caller boundary | `levyra-android-intent-security` plus `levyra-security-review` and affected domain skill |
| R8/Proguard/minification/shrinking/keep rules/mapping/release-only shrinker failure | `levyra-r8-proguard` plus `levyra-release-check`; add `levyra-ci-workflows` for tooling changes |
| Visual redesign/polish/hierarchy/spacing/typography/color/motion/reference/anti-AI-slop UI | `levyra-design-taste` plus matching UI skill |
| Decorative motion artwork | `levyra-motion-artwork` |
| GitHub Actions/CI/F-Droid/Gradle/AGP/Kotlin/KSP/build cache/artifacts | `levyra-ci-workflows` |
| Repository exploration, builds/tests/lint/logs/search/dependencies/Git/GitHub/CI/CodeRabbit/setup | `levyra-context-efficiency` |
| Security/privacy/trust-boundary/supply-chain work | `levyra-security-review` |
| Branch/commit/diff/pull-request review | `levyra-pr-review` plus affected skills |
| Device/runtime/pre-merge/release/signing/APK validation | `levyra-release-check` |

## Mandatory pre-delivery review

Every code-bearing task must review the actual final code/diff before delivery.
Invoke `/code-review` when available; otherwise invoke the installed
`code-review` skill/stage through `levyra-real-engineering`. Fix actionable
findings before presenting the code as final. If a fix materially changes the
solution, review the corrected final diff again.

Do not run a review before code exists merely to satisfy the rule.

## Validation and publication

Use focused checks first, then the repository quality gate:

```bash
python3 scripts/ai_quality_gate.py --profile fast
python3 scripts/ai_quality_gate.py --profile full
```

Run `fast` before commit and `full` before push/PR publication. Missing or blocked
checks are not passes. Never claim unrun validation.

Commit, push, PR, merge, tag, release, version changes, deployment, and
repository settings remain owner-controlled. A `UserPromptSubmit` hook reinforces
context budgeting and matching skill routes each turn.
