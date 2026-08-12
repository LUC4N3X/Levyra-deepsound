---
name: levyra-android-performance
description: Automatically use for Android runtime performance investigations involving Perfetto/System Trace, jank, latency, startup, CPU scheduling, blocking, memory, I/O, power, R8/Proguard configuration, shrinker diagnostics, keep rules, or APK-size optimization. Pair it with the affected Levyra domain skill.
---

# Levyra Android performance workflow

## Purpose

This skill is the Android-specific evidence layer for performance and shrinker work. It adapts the useful investigation discipline from Google's official `android/skills` repository to Levyra without vendoring its large reference corpus, scripts, or experimental dependencies.

It does not replace `levyra-compose`, `levyra-player`, `levyra-ci-workflows`, `levyra-release-check`, or the current architecture. Load the affected domain skill as well.

## Required context

1. Read root `AGENTS.md` and `app/AGENTS.md`.
2. Read `docs/ARCHITECTURE.md` and the affected domain skill.
3. For runtime performance, inspect the existing baseline-profile/benchmark setup and the exact code path involved.
4. For R8/shrinker work, inspect `gradle/libs.versions.toml`, Android build files, `gradle.properties`, Proguard/R8 files, consumer rules, and release configuration before proposing a change.
5. Prefer current official Android/AGP documentation when a tool, task, schema, or API is version-sensitive.

## Runtime performance: evidence before conclusions

For jank, latency, startup, memory, I/O, power, or scheduling investigations:

1. Define the symptom, package/process, build variant, reproduction path, and relevant time window.
2. Prefer release-like behavior for performance claims. Debug traces are evidence about debug behavior only.
3. Start broad with available metrics or high-level trace summaries before writing narrow custom queries.
4. Keep a chain of evidence containing verified timestamps, slice names, process/thread identifiers, durations, thread states, counters, and directly observed dependencies. Keep hypotheses separate from verified facts.
5. Never equate wall-clock duration with CPU work. For a suspicious interval, determine whether the relevant thread was running, runnable, sleeping, blocked, or in uninterruptible sleep.
6. If work is blocked, follow the dependency far enough to identify what it is waiting for, such as another lock/thread, Binder/IPC, I/O, scheduler pressure, or external process. A waiting thread is not a root cause by itself.
7. Check for a second independent bottleneck before concluding. A local UI anomaly can coexist with unrelated I/O, scheduling, graphics, memory, or power pressure.
8. Propose the smallest change that addresses the measured cause, then remeasure the same reproduction path.

When querying Perfetto/trace processor, do not guess table/module schemas. Inspect the available schema or use the exact supported module/query syntax for the installed trace processor. Preserve raw trace evidence when compact summaries would hide timing or dependency information.

## Compose and frame performance

For Compose jank, use this skill together with `levyra-compose`.

- Trace state invalidation, composition/layout/draw cost, image work, allocations, and scheduler/thread state rather than assuming recomposition is the cause.
- Correlate frame misses with the actual expensive or blocked work in the same time range.
- Use Layout Inspector/recomposition counts for composition questions and Perfetto/System Trace when the problem spans frames, threads, I/O, IPC, scheduling, memory, or power.
- Do not add `remember`, `derivedStateOf`, stability annotations, persistent collections, custom layouts, or caches without evidence that they address the measured problem.

## R8 and Proguard analysis

Treat keep-rule cleanup as correctness-sensitive performance work.

1. Determine the actual AGP/R8 configuration from repository files.
2. Prefer the official R8 configuration analyzer when the installed AGP exposes it. On AGP versions that support `analyzeReleaseR8Config`, use the repository Gradle wrapper and inspect the generated report rather than estimating rule impact from file length.
3. If quantitative analyzer tooling is unavailable, label the review heuristic. Inspect broad package-wide keeps, redundant rules already supplied by library consumer rules, reflection/serialization/JNI requirements, and rules that subsume narrower rules.
4. A broad keep rule is a suspect, not automatically a bug. Trace why the kept classes are needed before removing or narrowing it.
5. Do not copy external analyzer scripts into Levyra merely to obtain a score. Use official tasks already available in the current toolchain or perform a transparent manual analysis.
6. Never weaken reflection, serialization, JNI/native, Room, Media3, Compose, or other runtime requirements just to reduce APK size.
7. After a keep-rule change, run the narrowest release/R8 build that exercises shrinking and add focused runtime or regression coverage for the affected reflective/native path when practical.

For APK-size claims, distinguish dex/code, resources, native libraries, metadata, and packaging. Do not claim a size win from a rule diff without measuring the produced artifact.

## AGP/build interaction

Load `levyra-ci-workflows` when the performance task also changes AGP, Gradle, KSP, compiler options, build cache, or build logic. Do not turn a performance investigation into an unsolicited dependency/toolchain upgrade.

Avoid `clean` as a routine diagnostic step. It destroys incremental evidence and is not proof that a normal developer or CI build is healthy.

## Validation and reporting

Report:

- exact symptom and reproduction path;
- build/device/environment relevant to the evidence;
- verified evidence chain;
- root cause and confidence;
- alternatives disproven or still open;
- smallest proposed/applied change;
- before/after metric when measured;
- focused tests/builds/traces run;
- blocked or unverified checks;
- remaining device/OEM/release risk.

Do not turn an analyzer warning, long slice, broad keep rule, or agent suspicion into a confirmed root cause without supporting evidence.
