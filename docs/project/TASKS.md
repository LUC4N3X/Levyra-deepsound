# Levyra Active Tasks

## Active phase

**Name:** Levyra Live Radio

**Roadmap tracks:** Track 1 - Playback critical path; Track 2 - Persistence; Track 3 - Interface; Track 4 - Remote-media resilience

**Status:** Completed and validated with targeted unit tests and the FAST AI quality gate.

**Scope:** Add Explore -> Live Radio with locale-aware Radio Browser discovery, premium Compose UI, local favorites/recent stations, and a transient Media3 live-stream mode. Song Radio and normal music behavior remain unchanged.

## Acceptance criteria

- All Levyra languages map to maintainable radio-language and preferred-country groups.
- Country, language, category and debounced search paths use real Radio Browser results.
- Favorites, recent stations and cached discovery remain bounded and locally available.
- Live playback has no fake duration, seek, skip, repeat or Song Radio actions.
- Direct station streams bypass the music cache, accept only HTTP/HTTPS, publish best-effort ICY metadata and retry with bounded backoff.
- Normal queue state is preserved while Live Radio is active and restored before ordinary music playback.
- New user-facing copy uses the Levyra localization catalog.

## Work items

- [x] Implement Radio Browser discovery, server selection, filtering, ranking and pagination.
- [x] Implement all-language locale preferences plus manual country/language/category selection.
- [x] Implement bounded local favorites, recent stations and catalog caches.
- [x] Add the Explore entry and the dedicated responsive Compose screen.
- [x] Add transient Media3 live playback, cache bypass, ICY metadata and bounded recovery.
- [x] Add live-specific full and mini-player controls without changing Song Radio.
- [x] Add focused mapping, filtering, URL and MIME-policy tests.
- [x] Complete available static checks and the FAST AI quality gate.

## Preserved behavior and boundaries

- No Room migration, account, telemetry, permission, dependency, version or release change.
- Song Radio, resolver-based music playback, JioSaavn, SponsorBlock, lyrics, downloads, recommendations and Home remain outside this phase.
- Full AI quality gate is intentionally excluded by owner instruction.

## Rollback boundary

Remove the dedicated radio feature and UI files, then revert only the Live Radio fields and branches in app navigation, player projection, MediaItem creation, PlaybackService and LevyraViewModel. No user database migration is involved.

## Update rule

Record validation as passing only from direct evidence; environmental Gradle failures remain blocked rather than pass.
