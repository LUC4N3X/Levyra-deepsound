---
name: levyra-android-performance
description: Automatically use for Android runtime performance investigations involving Perfetto/System Trace, jank, latency, startup, CPU scheduling, blocking, memory, I/O, IPC, graphics, power, or measured frame/runtime bottlenecks. Pair it with the affected Levyra domain skill.
---

# Levyra Android performance workflow

## Purpose

This skill is the Android-specific evidence layer for runtime performance. It
adapts the strongest parts of Google's official `android/skills`
`perfetto-trace-analysis` workflow to Levyra without vendoring its large
reference corpus or creating a second profiling system.

It does not replace `levyra-compose`, `levyra-player`, `levyra-ci-workflows`,
`levyra-release-check`, `levyra-r8-proguard`, or the current architecture. Load
the affected domain skill as well. Use `levyra-r8-proguard` instead for shrinker,
keep-rule, resource-shrinking, mapping, or APK-size work.

## Required context

1. Read root `AGENTS.md` and `app/AGENTS.md`.
2. Read `docs/ARCHITECTURE.md` and the affected domain skill.
3. Inspect the existing baseline-profile/benchmark setup and exact code path.
4. Record package/process, build variant, device/emulator, reproduction path,
   trace source, and relevant time window before drawing a conclusion.
5. Prefer release-like behavior for performance claims. Debug traces are evidence
   about debug behavior only.
6. Prefer current official Android/Perfetto documentation when a metric, module,
   schema, counter, or API is version-sensitive.

## Chain of evidence

Keep verified evidence separate from hypotheses throughout the investigation.
For a supplied Perfetto trace, maintain a compact analysis note next to the trace
when the runtime can safely create one. Record only verified facts such as:

- timestamps and time windows;
- process `upid` and thread `utid` identities;
- slice names/durations;
- thread states and scheduling latency;
- frame timeline evidence;
- counters and memory/power values;
- Binder/flow dependencies;
- I/O blocked functions and wakeups;
- query text/result needed to reproduce a finding.

Do not write guesses into the evidence record as if they were facts. If the
runtime cannot create a scratchpad, keep the same separation explicitly in the
working report.

## Investigation protocol

### 1. Define the symptom

State what is slow or janky, where it occurs, how to reproduce it, and what user
behavior is affected. Separate cold/warm/hot startup, first playback, scrolling,
track transitions, image-heavy surfaces, background playback, and idle battery
behavior instead of treating them as one generic performance problem.

### 2. Start broad

Use high-level metrics and broad trace inspection before narrow custom SQL.
Identify the active Levyra process, the relevant time range, frame misses,
long-running/blocked threads, memory pressure, I/O, Binder/IPC, and power/system
anomalies that overlap the symptom.

### 3. Form one hypothesis at a time

Use the prompt, current code path, and observed evidence to choose the next
question. State why a query or trace inspection is being run before treating its
result as meaningful.

### 4. Follow dependencies to the cause

Never equate wall-clock duration with CPU work. For every suspicious long slice,
check the exact overlapping `thread_state` interval and distinguish:

- Running: consuming CPU;
- Runnable: ready but waiting for CPU;
- Sleeping: waiting voluntarily;
- uninterruptible/D-state: commonly blocked on I/O/kernel work;
- blocked on lock/Binder/another thread.

If a thread is waiting, follow the dependency. A waiting main thread is not the
root cause until the blocker, server, I/O operation, lock owner, scheduler
contention, or external process is identified.

### 5. Search for independent bottlenecks

Do not stop after the first anomaly. Perform one broader system check before
concluding so a local Compose issue does not hide an unrelated I/O, scheduler,
Binder, graphics-memory, LMKD, or power problem.

### 6. Fix and remeasure

Change the smallest material cause, then re-run the same reproduction path on an
equivalent build/device. Do not declare success from code inspection alone when
the original claim was performance.

## Perfetto SQL discipline

When using `trace_processor`, treat SQL correctness as part of the evidence.

- Never guess a table, view, column, or `INCLUDE PERFETTO MODULE` name. Inspect
  the installed Perfetto schema/stdlib or current official reference first.
- Prefer existing Perfetto standard-library tables/views/macros over manual
  timestamp arithmetic when they express the same intent.
- Join threads/processes with `utid`/`upid`, not recycled OS `tid`/`pid`, unless
  the specific table contract requires otherwise.
- Handle incomplete intervals where `dur = -1` using the trace end rather than
  summing or bounding them as negative durations.
- For two time ranges, use interval-overlap semantics; do not require one event
  to start inside the other interval.
- Prefer standard interval helpers or properly partitioned `SPAN_JOIN` when
  combining interval sets. Materialize intermediate tables where Perfetto
  requires it.
- Use `=` for exact matching. `GLOB` is case-sensitive and uses `*` and `?`, so
  substring matching should look like `GLOB '*needle*'`. `LIKE` uses `%` and `_`
  and is ASCII-case-insensitive by default; use it only when those wildcard or
  case semantics are intended. Verify the installed schema/query behavior before
  relying on either operator in evidence.
- Use `EXTRACT_ARG` for structured args rather than parsing display strings.
- Prefix columns with aliases in non-trivial joins so query meaning remains
  reviewable.
- Keep queries idempotent when they create Perfetto objects. A query used during
  iterative diagnosis should be safe to rerun.
- If a provided user query is being validated, preserve its analytical intent;
  do not simplify away an overlap, dependency, percentile, or attribution merely
  to make SQL execute.

A failed query is not evidence. Fix schema/syntax/logic and rerun it before using
its result.

## CPU and scheduler analysis

For CPU-bound or scheduling-sensitive paths:

- compare time spent Running versus Runnable for critical threads;
- quantify scheduling latency where useful and inspect high-percentile outliers
  such as p95/p99 instead of relying on one anecdotal pause;
- if Runnable time is high, inspect competing work and whether other CPUs were
  idle before blaming the app thread itself;
- correlate key-thread placement with CPU frequency and core behavior before
  inferring slow-code execution;
- inspect frequency/governor evidence when wall time rises without a matching
  increase in running CPU time;
- for blocked RenderThread/main/player threads, identify the waker or dependency;
- distinguish userspace work from kernel time when the trace supports it.

Do not "optimize" thread priorities, affinities, dispatchers, or coroutine
structure from intuition alone.

## Compose, frames, and graphics

For UI jank, pair this skill with `levyra-compose`.

- Compare actual versus expected frame timeline evidence instead of inferring
  jank only from a visibly long composable call.
- Correlate missed frames with main-thread, RenderThread, GPU, texture upload,
  image decode, layout/draw, allocation, or scheduler evidence in the same time
  window.
- Inspect graphics-memory pressure when artwork-heavy surfaces are involved;
  distinguish CPU bitmap memory from GPU/buffer memory and look for duplicated
  large images or costly intermediate render targets when supported by the
  trace.
- A large texture upload or buffer allocation is a lead, not a conclusion; prove
  its temporal relationship to missed frames.
- Use Layout Inspector/recomposition counts for composition questions and
  Perfetto/System Trace when the problem spans frames, threads, I/O, IPC,
  scheduling, memory, or power.
- Do not add `remember`, `derivedStateOf`, stability annotations, persistent
  collections, custom layouts, or caches without evidence they address the
  measured invalidation/allocation/layout problem.

## Binder and IPC

When IPC is in the critical path:

- inspect repeated Binder transactions for bursts/spam rather than blaming one
  normal call;
- identify the server process and separate client wait from server execution;
- use flow/dependency evidence to cross process boundaries when available;
- correlate high Binder concurrency with CPU scheduling before attributing
  latency to Binder itself;
- trace the calling stack only after the problematic transaction pattern has
  been established.

Do not dive into Binder internals when a clearer local bottleneck already
explains the symptom unless the evidence still leaves material latency
unexplained.

## Startup and critical path

- Distinguish cold, warm, and hot startup before comparing timings.
- Identify whether delay belongs to process start, class loading, dependency
  initialization, page faults/I/O, database work, network/config fetches,
  composition/layout, player/service startup, or work scheduled after first
  frame.
- If the problem disappears on a second launch, investigate page cache/cold I/O
  before assuming initialization code became faster.
- Do not move required work later merely to improve a headline startup number if
  that creates a visible stall or delays first playback interaction.
- When Baseline Profiles or Macrobenchmarks cover the path, reuse them and
  compare equivalent variants/devices.

## I/O analysis

For D-state or I/O stalls:

- inspect `blocked_function`/kernel reason when available;
- look for page faults, readahead, filesystem integrity work, database/file
  access, or network/socket work overlapping the stall;
- follow the wakeup path to relevant kernel/kworker activity when needed;
- check for high-frequency tiny reads before adding a cache or changing
  dispatchers;
- identify the exact file/socket/database path before proposing a fix.

A dispatcher change does not fix storage contention by itself.

## Memory analysis

Distinguish:

- retained/leaked objects;
- short-lived allocation churn;
- graphics/buffer memory;
- system-wide pressure/swap;
- low-memory-killer events.

When available, inspect LMKD/PSI evidence, RSS trends, swap/kswapd pressure,
bitmap/object outliers, duplicate bitmaps, and heap retainer paths. Do not infer a
memory leak from high peak RSS alone.

For artwork-heavy flows, correlate bitmap dimensions/count, decoded image size,
GPU/buffer usage, and lifecycle retention with the exact screen/player state.

## Power and background behavior

For battery/power investigations:

- start from actual energy/power-rail or platform power evidence when available;
- inspect suspend state and kernel wakelocks during screen-off/idle periods;
- correlate modem/network power with traffic attributable to Levyra;
- correlate playback service, MediaSession, Bluetooth/media routes, jobs, timers,
  artwork/lyrics refresh, prefetch, and animations with the same time window;
- use a comparable energy unit when quantifying alternatives;
- do not infer battery impact from CPU percentage alone.

Playback that is intentionally active must not be "optimized" by breaking the
foreground/background media contract.

## AGP/build interaction

Load `levyra-ci-workflows` when a runtime-performance task also changes AGP,
Gradle, KSP, compiler options, build cache, or build logic. Do not turn a runtime
investigation into an unsolicited dependency/toolchain upgrade.

Avoid `clean` as a routine diagnostic step. It destroys incremental evidence and
is not proof that a normal developer or CI build is healthy.

## Validation and reporting

Report:

- exact symptom and reproduction path;
- build/device/environment and trace source;
- verified chain of evidence with timestamps/threads/processes where relevant;
- root cause and confidence;
- alternatives disproven or still open;
- smallest proposed/applied change;
- before/after metric when measured;
- focused tests/builds/traces/queries run;
- blocked or unverified checks;
- remaining device/OEM/release risk.

Do not turn a long slice, frame miss, Binder call, high CPU interval, allocation
burst, D-state, wakelock, or agent suspicion into a confirmed root cause without
supporting evidence.

## Provenance

This workflow is informed by Google's `android/skills`
`perfetto-trace-analysis` skill and its CPU/graphics/I/O/IPC/memory/power and SQL
reference guidance. Levyra keeps a compact, repository-native adaptation;
current Perfetto documentation, the installed trace schema, and direct evidence
always take precedence over copied query recipes.
