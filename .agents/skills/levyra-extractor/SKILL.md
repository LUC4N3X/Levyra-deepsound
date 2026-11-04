---
name: levyra-extractor
description: Implement, debug, or review Levyra InnerTube, YouTube extraction, stream resolution, player-config synchronization, token, retry, timeout, cache, and network fallback behavior.
---

# Levyra extractor workflow

## Required context

1. Read the root `AGENTS.md` and the nearest applicable `AGENTS.md`.
2. Read `docs/ARCHITECTURE.md`.
3. Read `.agents/claude/rules/extractor-network.md` and `.agents/claude/rules/security.md`.
4. Inspect the resolver, clients, runtime configuration, caches, in-flight work, fallback order, workflows, and nearby tests.

## Change contract

Map the current request path and explicitly distinguish:

- successful resolution;
- conclusive no-match;
- transient transport, timeout, server, parsing, or verification failure;
- cancellation;
- stale or invalid runtime configuration.

## Guardrails

- Preserve the current bounded fallback order unless the task explicitly changes it.
- Preserve last-known-good runtime configuration until a replacement is validated and published atomically.
- Do not hardcode private credentials, cookies, account tokens, visitor data, signed URLs, or production secrets.
- Keep retries, timeouts, concurrency, traffic, cache growth, and response sizes bounded.
- Do not negative-cache transient or inconclusive failures.
- Re-throw `CancellationException`; one caller's cancellation must not poison shared work or other waiters.
- Validate provider-controlled URLs, redirect hops, hosts, ports, resolved destinations, MIME types, and response sizes.
- Do not weaken validation merely to accept one upstream response.

## Catalog facts

- Exact-artist album results list only browseId-verified releases from that artist, complete and untruncated; never add name matches.
- YouTube Music mood/genre `params` are the same in every language; only titles change. Russian YouTube Music joins artists with "и".
- Apple core genre curators exist only in the `us` storefront, and `ru` has no album shelves or album RSS feed.
- Verify import or resolver changes by replaying recorded traces (`PlaylistTraceRecorder`, `PlaylistImportTraceReplayTest`). Live runs stay at 50 tracks or fewer: YouTube Music search 403 throttling is per IP.

## Validation

Add or update focused tests for fallback order, configuration parsing and epoch changes, invalid upstream payloads, timeout, cancellation, cache semantics, stale publication, and URL/redirect rejection when applicable.

Inspect `.github/workflows/sync-player-configs.yml` for configuration changes. A successful no-change sync is not evidence that a new configuration was published.
