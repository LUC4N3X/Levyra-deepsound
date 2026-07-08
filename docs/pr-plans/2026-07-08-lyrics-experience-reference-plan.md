# Levyra Reference-Inspired PR Plan: Lyrics Experience + Player Polish

Date: 2026-07-08

## Goal

Open the next Levyra PR around a focused, defensible upgrade: a richer lyrics/player experience inspired by SimpMusic, LyricsPlus, Metrolist, and a Spotify-like dark music UI language, while keeping Levyra's own cinematic identity.

## Recommended PR Scope

Build "Levyra Lyrics Pro v2":

- Multi-source lyrics pipeline with clearer provider priority and confidence reporting.
- Optional TTML/word-timed lyrics parsing path, prepared for LyricsPlus-style data.
- Fullscreen lyrics overlay polish: denser, darker, artwork-driven, and easier to scan during playback.
- Small component extraction from `LevyraApp.kt` so the PR improves maintainability instead of only adding more code to an already large file.

This is the best PR candidate because Levyra already has:

- `LyricsRepository.kt` with LRCLIB + Lyrics.ovh fallback, local cache, scoring, and provider metadata.
- `LevyraUiState.kt` with lyrics state fields: provider, confidence, synced, cached, loading, active line.
- `LyricsOverlay`, `LyricsProStatusRow`, and `LyricsButton` in `LevyraApp.kt`.
- Existing tests around repository/domain behavior.

## What To Take, Levyra-Style

### From SimpMusic

Useful ideas:

- Treat lyrics as a first-class playback feature, not a small side panel.
- Expose provider labels and user-facing settings for lyrics behavior.
- Keep future room for AI translation, but do not ship it in this PR unless we have a clear API-key/privacy story.
- Study the separation of view models/screens as a direction for future cleanup.

Do not take now:

- Full KMP/Desktop scope.
- Spotify login/canvas complexity.
- SimpMusic-specific database/provider branding.
- AI translation runtime in the first PR.

### From LyricsPlus

Useful ideas:

- API contract shape: `title`, `artist`, `album`, `duration`, optional `isrc`/`platformId`, optional `source`, and `forceReload`.
- Multiple result formats: v2 structured lyrics, TTML, raw source data.
- Cache-first behavior and explicit source/processing metadata.
- User-submission architecture as a later idea, not a phone-client feature for this PR.

Do not take now:

- Hardcoded dependency on a third-party LyricsPlus server.
- Server-side Google Drive cache assumptions.
- Upload/submission flow and proof-of-work challenge.
- Provider credentials or anything requiring Apple/Musixmatch/Spotify accounts.

### From Metrolist

Useful ideas:

- Keep LRCLIB logic modular and testable.
- Use multiple search strategies: cleaned title+artist, title-only fallback, combined query, original title fallback.
- Add relaxed duration matching for better real-world lyric matches.
- Add TTML parsing tests inspired by its BetterLyrics module.
- Use Material 3 theme/palette flexibility as inspiration, not a direct visual copy.

Do not take now:

- Listen-together backend.
- Account sync and full library import.
- Full BetterLyrics remote dependency.
- Large module import that would change Levyra's build shape too much.

## Visual Direction

Reference: `awesome-design-md/design-md/spotify/DESIGN.md`.

Adapted Levyra direction:

- Add a "Studio Black" treatment for lyrics/player: near-black surfaces, compact typography, pill/circle controls.
- Let album artwork provide most color; use Levyra cyan/violet only for functional states.
- Reduce decorative gradient noise in the lyrics overlay.
- Keep Levyra's identity: cinematic glass, cyan/violet accents, high-contrast readability.
- Avoid making it look like Spotify; use the music-app ergonomics, not the brand.

## Candidate Approaches

### Approach A: Lyrics Pro v2 (Recommended)

Scope:

- Refactor lyrics parsing/scoring into smaller classes.
- Add TTML parser + word timing model.
- Add optional LyricsPlus-compatible client interface, disabled unless configured.
- Improve fullscreen lyrics overlay with word-aware rendering when available.
- Add unit tests for LRC parsing, TTML parsing, scoring, cache restore, and fallback ordering.

Why recommended:

- Strong overlap between all three reference repos.
- Uses existing Levyra architecture.
- Creates a PR that is featureful but still reviewable.

Risk:

- TTML/word timing adds data-model complexity.
- Remote lyrics providers need privacy and reliability guardrails.

### Approach B: Discovery + Library Upgrade

Scope:

- More personalized quick picks.
- Better artist/release cards.
- Listening Pulse expansion with local "scrobble" style sections.
- Home/library visual reordering.

Why not first:

- Levyra already recently touched Listening Pulse and Library.
- Higher risk of a broad UI PR with fewer clear tests.

### Approach C: Pure Visual Refresh

Scope:

- Player/lyrics/home polish only.
- Add Studio Black theme.
- Extract Compose components.
- No new network/provider work.

Why not first:

- Safer, but less meaningful as a new PR if the goal is to learn from all three reference repos.
- Does not use LyricsPlus beyond design inspiration.

## Proposed Implementation Plan

1. Create branch:
   - `feature/lyrics-pro-v2-player-polish`

2. Add domain models:
   - Extend or wrap `LyricLine` with optional `words: List<LyricWord>`.
   - Add `LyricsProviderPriority` or equivalent sealed model for source ordering.

3. Split lyrics repository internals:
   - Keep public API stable where possible: `fetch(title, artist, durationSec)`.
   - Extract title/artist normalization.
   - Extract LRC parser.
   - Add TTML parser.
   - Add result scoring/fallback ordering tests.

4. Add optional LyricsPlus-compatible provider:
   - Do not hardcode a single unofficial production URL.
   - Gate via local config/build field or disabled default.
   - Accept `title`, `artist`, `album`, `duration`.
   - Parse v2/TTML response into Levyra models.
   - Fail closed and fall back to LRCLIB.

5. Improve UI:
   - Extract lyrics UI to `ui/lyrics/`.
   - Add `LyricsOverlay`, `LyricsStatusPill`, `SyncedLyricLine`, `WordTimedLyricLine`.
   - Make fullscreen lyrics use near-black surfaces and artwork accent.
   - Keep active line readable and animated, but avoid over-animation.

6. Add settings copy/state:
   - Show provider, confidence, synced/static, cached/remote.
   - Optionally add a "prefer enhanced lyrics" toggle only if provider integration lands.

7. Tests and verification:
   - `./gradlew testDebugUnitTest`
   - If Android environment allows: `./gradlew assembleDebug`
   - Manual smoke test: play track, open lyrics, verify active line follows playback, no crash when no lyrics.

## PR Guardrails

- No direct visual clone of Spotify, SimpMusic, or Metrolist.
- No bundled secrets, provider account tokens, or hardcoded private endpoints.
- GPL-compatible attribution stays in README or PR body when adapting code-level ideas.
- Keep PR under one theme: lyrics/player experience. Discovery, accounts, listen-together, and desktop stay out.
- Prefer tests before UI breadth.

## PR Body Draft

Title:

`feat: upgrade lyrics pipeline and polish fullscreen player lyrics`

Summary:

- Adds a modular lyrics pipeline with stronger fallback ordering and provider metadata.
- Adds TTML/word-timing support foundation for enhanced synced lyrics.
- Refreshes fullscreen lyrics with a darker, artwork-led player treatment.
- Extracts lyrics UI/parsing pieces into focused files for maintainability.

References:

- SimpMusic for multi-provider lyrics/player experience concepts.
- LyricsPlus for API-shape and TTML/raw lyrics ideas.
- Metrolist for LRCLIB fallback strategy and BetterLyrics TTML parsing inspiration.
- Spotify-inspired design reference for content-first dark music UI ergonomics.

Testing:

- `./gradlew testDebugUnitTest`
- `./gradlew assembleDebug`

