---
name: levyra-player
description: Implement, debug, or review Levyra playback, queue, Media3, MediaSession, notification, Android Auto, prefetch, audio mode, video mode, and player lifecycle changes.
---

# Levyra player workflow

## Required context

1. Read the root `AGENTS.md` and the nearest applicable `AGENTS.md`.
2. Read `docs/ARCHITECTURE.md`.
3. Read `.claude/skills/levyra-player/SKILL.md`, `.claude/rules/player.md`, and `.claude/rules/architecture.md`.
4. Inspect the complete current path through user intent, ViewModel/controller, queue, resolver, player, service, MediaSession, notification, and tests.

## Change contract

Before editing, identify:

- the exact playback defect or desired behavior;
- ownership of every affected coroutine, callback, player, surface, decoder, and in-flight request;
- behavior that must remain unchanged;
- whether audio/song mode, native-video mode, queue state, background playback, notification, Android Auto, or lifecycle can be affected.

## Guardrails

- Preserve the user's explicit audio/song versus native-video choice.
- Keep audible playback, MediaSession, notification, Android Auto, queue, and background service synchronized.
- Keep artwork, lyrics, refresh, diagnostics, prefetch, and metadata enrichment off the direct playback critical path.
- Reuse existing Media3 objects, scopes, dispatchers, queue state, and resolver infrastructure.
- Re-throw `CancellationException` and protect late publication with identity plus generation checks.
- Do not fix races with arbitrary delays, broad retries, duplicated state, or silent exception swallowing.
- Static artwork must remain the immediate fallback; decorative media must remain muted and independent from audible playback.

## Validation

Add or update focused regression coverage for relevant cases such as rapid track changes, cancellation, same-identity replacement, queue mutation, mode switching, background/foreground transitions, notification actions, and end-of-queue behavior.

Run focused tests first, then applicable checks from `AGENTS.md`. Report Android Auto, notification, PiP, emulator, physical-device, and real playback checks as unverified unless they were actually performed.
