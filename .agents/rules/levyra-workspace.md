# Levyra workspace rule

@../../AGENTS.md

Treat the repository-root `AGENTS.md` as Levyra's authoritative operating
contract. Before investigating, editing, reviewing, or running commands:

1. apply the nearest path-specific `AGENTS.md` files for every file in scope;
2. read only relevant approved planning material under `docs/project/`;
3. discover and load every matching `levyra-*` skill under `.agents/skills/`;
4. read `../../docs/ai/AI_ENGINEERING_GUARDRAILS.md` before production-code
   implementation or broad review and apply its architecture-first, reuse-first,
   complexity-budget, scope-checkpoint, and diff-quality rules;
5. automatically load `levyra-real-engineering` for non-trivial features,
   architectural changes, unclear defects, or multi-step work where requirements
   and implementation should be separated; use only the stages needed and skip
   the full workflow for tiny, already-unambiguous changes;
6. automatically load `levyra-context-efficiency` for builds, tests, lint,
   logs, broad searches, dependencies, Git/GitHub, CI, CodeRabbit, setup, or any
   command expected to produce large repetitive output;
7. automatically load `levyra-security-review` for vulnerability scans,
   attacker-controlled input, trust-boundary changes, authentication, secrets,
   permissions, privacy, dependency risk, update integrity, or security-related
   pull requests;
8. automatically load `levyra-design-taste` together with the matching UI skill
   for visual redesigns, UI polish, hierarchy, spacing, typography, color, shape,
   motion, screenshot/reference work, or requests to make Levyra more premium,
   modern, distinctive, cohesive, or less AI-generated;
9. automatically load `levyra-android-performance` for Android jank, latency,
   startup, Perfetto/System Trace, CPU/thread-state, blocking, memory, I/O, power,
   or measured runtime-performance investigations, together with the affected
   domain skill;
10. automatically load `levyra-r8-proguard` for R8/Proguard, minification,
    resource shrinking, keep/consumer rules, release-only shrinker failures,
    reflection/serialization/JNI shrinker issues, mapping output, missing classes,
    or APK-size optimization;
11. when RTK is available, use it selectively for noisy supported commands,
    verify exit status and success/failure markers, and rerun the exact command
    raw whenever compact output hides required evidence;
12. keep exploit evidence, security validation, hashes, signatures, secret scans,
    signing, and exact reproduction output raw;
13. inspect current code, tests, architecture, build files, dependencies, and
    workflows before relying on memory or previous agent output;
14. for security work, follow threat model, identification, safe validation,
    minimal remediation, human review, and revalidation;
15. identify the current architecture owner and expected production files before
    editing, then make the smallest coherent change and preserve unrelated
    behavior;
16. if the implementation crosses an AI guardrail scope checkpoint, re-evaluate
    and split the work instead of expanding autonomously unless the owner
    explicitly approves the larger scope;
17. run focused validation first, then
    `python3 scripts/ai_quality_gate.py --profile fast` before commit and
    `python3 scripts/ai_quality_gate.py --profile full` before push or PR;
18. inspect final diff statistics and the complete diff for unnecessary code
    growth, duplicate ownership, speculative abstractions, generated churn, and
    unrelated edits;
19. treat every blocked or skipped required gate check as not passed;
20. keep implementation, validation, review, publication, merge, and release as
    separate states.

## Automatic task-to-skill routing

Codex, Antigravity, OpenCode, OpenClaw, and any other compatible runtime using
this workspace must select matching skills from the task itself. Never require
the owner to name a skill, type a slash command, or remind the agent to use one.
Load multiple skills when several rows apply.

- bugs, regressions, test/build failures, races, crashes, or unexpected behavior
  that need investigation -> `levyra-real-engineering` plus the affected domain
  skill; use its hypothesis-driven debugging lane before speculative fixes;
- Compose UI/state/accessibility work -> `levyra-compose`; if the task is jank,
  scrolling, frame misses, Layout Inspector, Perfetto/System Trace, scheduling,
  blocking, memory, I/O, power, or another measured Android runtime-performance
  problem, also load `levyra-android-performance`;
- visual redesign, polish, hierarchy, spacing, typography, color, shape, motion,
  screenshot/reference recreation, "più bella", "premium", "modern", "clean",
  "cinematic", "cohesive", or anti-AI-slop UI work -> `levyra-design-taste`
  plus `levyra-compose` on Android or `levyra-desktop` on Desktop;
- R8, Proguard, minification, resource shrinking, keep rules, consumer rules,
  release-only shrinker crashes, mapping/missing classes, reflection or JNI
  shrinker issues, or APK-size work -> `levyra-r8-proguard`; also load
  `levyra-ci-workflows` for build-tooling changes and `levyra-release-check` for
  release/minified runtime validation;
- GitHub Actions, CI, F-Droid, Gradle, AGP, Kotlin, KSP, configuration/build
  cache, build speed, artifacts, or workflow automation ->
  `levyra-ci-workflows`; also load `levyra-context-efficiency` when command
  output is expected to be noisy;
- emulator/device smoke tests, `adb` runtime verification, pre-merge evidence,
  signing, APK/package checks, or release validation -> `levyra-release-check`;
- branch, commit, diff, or pull-request review -> `levyra-pr-review` plus any
  matching domain/security skill.

These automatic routes activate the repository-local workflows that incorporate
curated practices from the reviewed external skill sources. External packages
are not required at runtime for these Levyra-native behaviors.

For real-engineering work, follow
`../../.agents/skills/levyra-real-engineering/SKILL.md`. When the upstream Matt
Pocock package is available, load the exact stage skill selected by that adapter
instead of reconstructing it from memory. The adapter remains authoritative for
Levyra scope, architecture, validation, and publication boundaries.

For visual product work, follow
`../../.agents/skills/levyra-design-taste/SKILL.md` together with the applicable
platform skill. The design-taste skill is a Levyra-native adaptation of reviewed
anti-template design practices; it does not vendor or require the upstream web
skill at runtime.

For Android runtime-performance work, follow
`../../.agents/skills/levyra-android-performance/SKILL.md` and keep measured trace
or benchmark evidence separate from hypotheses. For shrinker work, follow
`../../.agents/skills/levyra-r8-proguard/SKILL.md`; do not fix release-only
failures with blanket keep rules or by disabling minification/resource shrinking.

RTK reduces command output; it is not validation authority and its savings are
not equal to total model-billing savings. Security-engine findings and generated
patches are proposals that require evidence, complete diff review, CI, and
explicit owner-controlled publication.

Never commit, push, open or merge a pull request, tag, publish, release, change
versions, or modify repository settings without explicit owner authorization for
the exact action and scope.

This lightweight bridge gives Codex, Antigravity, OpenCode, OpenClaw, and other
compatible workspaces one source of truth while discovering Levyra's real-
engineering, context-efficiency, design-taste, Android-performance, R8/Proguard,
AI engineering guardrails, and security-review workflows automatically.