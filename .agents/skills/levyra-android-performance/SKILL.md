---
name: levyra-android-performance
description: Automatically use for Android runtime performance investigations involving Perfetto/System Trace, jank, latency, startup, CPU scheduling, blocking, memory, I/O, power, or measured frame/runtime bottlenecks. Pair it with the affected Levyra domain skill.
---

# Levyra Android performance workflow

## Purpose

This skill is the Android-specific evidence layer for runtime performance. It adapts the useful investigation discipline from Google's official `android/skills` repository to Levyra without vendoring its large reference corpus, scripts, or experimental dependencies.

It does not replace `levyra-compose`, `levyra-player`, `levyra-ci-workflows`, `levyra-release-check`, `levyra-r8-proguard`, or the current architecture. Load the affected domain skill as well. Use `levyra-r8-proguard` instead for shrinker, keep-rule, resource-shrinking, mapping, or APK-size work.

## Required context

1. Read root `AGENTS.md` and `app/AGENTS.md`.
2. Read `docs/ARCHITECTURE.md` and the affected domain skill.
3. Inspect the existing baseline-profile/benchmark setup and the exact code path involved.
4. Record the package/process, build variant, device/emulator, reproduction path, and relevant time window before drawing a performance conclusion.
5. Prefer current official Android/Perfetto documentation when a metric, query module, schema, or API is version-sensitive.

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

## Startup and critical-path work

- Distinguish cold, warm, and hot startup before comparing timings.
- Identify whether delay belongs to process start, class loading, dependency initialization, database work, network/config fetches, composition/layout, player/service startup, or work scheduled after first frame.
- Do not move required work later merely to improve a headline startup number if that creates a visible stall or delays first playback interaction.
- When Baseline Profiles or Macrobenchmarks cover the path, reuse that infrastructure and compare equivalent variants/devices.

## Memory, I/O, and power

- For memory pressure, distinguish retained objects/leaks from short-lived allocation churn and from system pressure. Use heap/leak evidence when retention is suspected.
- For I/O stalls, identify the exact file/socket/database operation and thread state before changing dispatchers or adding caches.
- For power investigations, correlate wakeups, network activity, playback/service ownership, timers, jobs, animations, and background work with the actual trace window. Do not infer battery impact from CPU percentage alone.
- Keep direct playback as the critical path; diagnostics, artwork, lyrics, prefetch, refresh, and enrichment must yield when they compete with playback.

## AGP/build interaction

Load `levyra-ci-workflows` when a runtime-performance task also changes AGP, Gradle, KSP, compiler options, build cache, or build logic. Do not turn a runtime investigation into an unsolicited dependency/toolchain upgrade.

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

Do not turn a long slice, frame miss, high CPU interval, allocation burst, or agent suspicion into a confirmed root cause without supporting evidence.