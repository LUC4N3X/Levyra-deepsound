---
name: levyra-motion-artwork
description: Implement, debug, or review Levyra decorative motion artwork, provider matching, muted media playback, prefetch, caching, lifecycle, and remote-media security.
---

# Levyra motion artwork workflow

## Required context

1. Read the root `AGENTS.md` and `app/AGENTS.md`.
2. Read `.claude/skills/levyra-motion-artwork/SKILL.md`, `.claude/rules/player.md`, `.claude/rules/security.md`, and the relevant architecture sections.
3. Inspect the motion engine, provider matching, ViewModel ownership, artwork layer, cache, verifier, player lifecycle, and tests.

## Non-negotiable behavior

- Motion artwork belongs only to song/audio mode.
- It must never replace, overlay, delay, or interfere with native music-video mode.
- It must remain muted and independent from audible MediaSession playback.
- Static artwork must appear immediately and remain the permanent fallback.

## Guardrails

- Require canonical track/album and primary-artist compatibility; preserve grouped artist identities.
- Validate HTTPS, allowed media hosts, ports, DNS/IP destinations, MIME types, response bounds, and every redirect hop.
- Deduplicate shared lookups in engine-owned supervised work; one caller's cancellation must not poison other waiters.
- Use identity plus generation checks before publishing or clearing ownership.
- Negative-cache only conclusive no-match or invalid results, never transient provider or verifier failures.
- Limit prefetch to the immediate next item and do not instantiate a decoder for prefetched artwork.
- Bound decoder, memory, network, cache, and lifecycle resource use.

## Validation

Add focused tests for primary-versus-guest artists, grouped names, redirect and host rejection, invalid MIME, transient failure, cancellation, stale publication, audio/video mode separation, lifecycle cleanup, and static fallback.
