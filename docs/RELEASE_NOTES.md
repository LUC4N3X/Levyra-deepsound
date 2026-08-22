# Levyra 2.3.33

## Highlights

Levyra 2.3.33 is an Android reliability release focused on long-running playback. It incorporates the playback resource-lifecycle hardening completed after 2.3.22, including the fix verified for issue #427, and refreshes the bundled YouTube player configuration used by Levyra's extractor paths.

## Playback and memory reliability

- Hardened ownership and cleanup of the primary and transition Media3 players so temporary playback resources converge on one deterministic release path.
- The audio-mode transition player no longer creates video renderers, preventing muxed compatibility sources from allocating a video decoder during crossfade.
- Audio processors used by the playback service and transition player now have explicit service-instance ownership instead of relying on shared process-level state.
- Native-heap sampling runs away from the playback service's Main dispatcher and revalidates the active player before recovery work mutates playback state.
- Music resolution prefers the existing audio-only Reel path before muxed compatibility fallback, reducing unnecessary native video and buffer pressure while preserving fallback compatibility.
- Resolver stream, failed-playback and rejected-video URL caches now prune expired entries and enforce fixed bounds.
- Resolver publication is generation-aware so stale asynchronous work cannot overwrite newer playback intent.

## YouTube compatibility

- Updated the bundled YouTube player configuration and player-date metadata used by the local decoder and extractor integration.
- Preserved the existing bounded fallback order, guest-session handling and privacy boundaries.
- Kept muxed playback sources available as compatibility fallbacks when a true audio-only source cannot be resolved.

## Validation

- Focused lifecycle, cache-policy, playback-strategy, resolver-generation and offline-isolation regression coverage is included with the resource-lifecycle changes.
- The implementation at commit `7203d2d8` was exercised for 25 minutes on a physical Android device with a real playback queue, four observed track transitions, one previous-track restart, and both screen-on and screen-off/Doze operation.
- During that run, Native Heap was 71.3 MiB initially, 71.3 MiB maximum and 59.8 MiB finally. TOTAL PSS was 426.3 MiB initially, 435.4 MiB maximum and 338.5 MiB finally. TOTAL RSS was 582.1 MiB initially, 595.9 MiB maximum and 499.6 MiB finally.
- The process retained the same PID and remained playing. Codec usage stayed at one software audio decoder with a peak of one and no video decoder. No crash, ANR, OOM, LMKD event or monotonic memory growth was observed.
- This device run validates the resource-lifecycle implementation, not the final signed 2.3.33 artifact. Signing, packaging and publication remain separate release gates.

## Notes

This is an Android-only version bump. Levyra Desktop remains independently versioned and is not changed by 2.3.33.

Existing favorites, playlists, listening history, downloads, settings, playback state and backups remain part of the compatibility contract. No account system, advertising identifier, analytics pipeline or user-tracking system is introduced.

## Versioning

- Version name: `2.3.33`
- Version code: `2033300`

`gradle.properties`, the Android Gradle fallbacks, README version wiring, architecture metadata, Android platform badge and Fastlane changelogs are aligned to 2.3.33. The separate "Latest release" badge remains on the most recently published GitHub release until 2.3.33 is actually published.

## Upgrade notes

No manual migration is required. Existing libraries, favorites, playlists, listening history, downloads, settings, playback state and backups remain compatible. GitHub installations continue to use Levyra's signed GitHub release/update path, while F-Droid builds remain on the reproducible F-Droid channel and follow F-Droid's own build and index schedule.

## Final note

Levyra 2.3.33 closes the playback-memory investigation with a bounded resource lifecycle: audio-mode transitions stay audio-only, temporary players and processors have explicit owners, resolver state cannot grow without limit, and stale work cannot replace newer playback intent.
