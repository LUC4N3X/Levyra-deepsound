---
name: levyra-player
description: Implement, debug, or review Levyra playback, queue, Media3, MediaSession, notification, Android Auto, prefetch, audio mode, video mode, and player lifecycle changes.
---

# Levyra player workflow

## Required context

1. Read the root `AGENTS.md` and the nearest applicable `AGENTS.md`.
2. Read `docs/ARCHITECTURE.md`.
3. Read `.agents/claude/rules/player.md` and `.agents/claude/rules/architecture.md`.
4. Inspect the complete current path through user intent, ViewModel/controller, queue, resolver, player, service, MediaSession, notification, and tests.

## Change contract

Before editing, identify:

- the exact playback defect or desired behavior;
- ownership of every affected coroutine, callback, player, surface, decoder, and in-flight request;
- behavior that must remain unchanged;
- whether audio/song mode, native-video mode, queue state, background playback, notification, Android Auto, lifecycle, or memory behavior can be affected.

## Guardrails

- Preserve the user's explicit audio/song versus native-video choice.
- Keep audible playback, MediaSession, notification, Android Auto, queue, and background service synchronized.
- Keep artwork, lyrics, refresh, diagnostics, prefetch, and metadata enrichment off the direct playback critical path.
- Reuse existing Media3 objects, scopes, dispatchers, queue state, and resolver infrastructure.
- Re-throw `CancellationException` and protect late publication with identity plus generation checks.
- Do not fix races with arbitrary delays, broad retries, duplicated state, or silent exception swallowing.
- Static artwork must remain the immediate fallback; decorative media must remain muted and independent from audible playback.

## Playback memory invariant

Issue #427 established that apparently small hot-path changes can create catastrophic native-memory growth even when Java heap behavior looks reasonable. Preserve the current stable-memory behavior as part of the playback contract.

- Before adding work to a repeated playback/recommendation/DSP/prefetch callback, ask whether it allocates, compiles, decodes, copies, registers, retains, or grows state on every invocation.
- Never compile `Regex`/`Pattern` or call `.toRegex()` inside repeated recommendation, metadata, playback-tick, frame, sample, or audio-buffer paths. Reuse immutable instances unless correctness requires otherwise and evidence proves the cost is bounded.
- Avoid per-buffer/sample/frame object, array, `ByteBuffer`, string-formatting, parser, and collection allocation in DSP/audio hot paths. Reuse the existing buffers/processors where ownership allows.
- Keep prefetch, resolver/recommendation bookkeeping, retry state, queue-side caches, and media-keyed maps bounded. Track cleanup across NEXT/PREV, replacement, cancellation, mode changes, service recreation, and end of queue.
- Release obsolete player/controller/decoder/surface/listener/job resources at the owning lifecycle boundary. Do not let stale generations retain the old track graph after a transition.
- Audio/song mode must not accidentally select or retain video tracks, video decoders, surfaces, or video-sized buffer pools. Native-video mode remains explicitly video-capable.
- Do not use `System.gc()`, periodic player recycling, forced pause/resume, broad cache clearing, or service/process restart as a substitute for locating an allocation or retention source.
- For a change that can materially affect playback/native/graphics memory, pair this skill with `levyra-android-performance` and validate the memory trend, not only a point-in-time heap value. Include native heap and process PSS/RSS when possible.
- Stable acceptance means memory reaches a bounded plateau/oscillation after warm-up and normal track transitions rather than climbing monotonically with playback duration. Device-specific absolute MB values may differ.

## Validation

Add or update focused regression coverage for relevant cases such as rapid track changes, cancellation, same-identity replacement, queue mutation, mode switching, background/foreground transitions, notification actions, end-of-queue behavior, and bounded cache/lifecycle cleanup.

When a change materially touches playback allocation, buffering, DSP, recommendations, prefetch, decoding, or resource lifetime, prefer a sustained physical-device playback run after warm-up with several normal track transitions. Target 20–30 minutes when practical and inspect native heap plus PSS/RSS; report this as `BLOCKED`/unverified if it cannot be performed rather than claiming memory stability from unit tests alone.

Run focused tests first, then applicable checks from `AGENTS.md`. Report Android Auto, notification, PiP, emulator, physical-device, real playback, and memory-stability checks as unverified unless they were actually performed.
