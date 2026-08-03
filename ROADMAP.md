# Levyra Engineering Roadmap

## Purpose

This roadmap orders durable engineering outcomes. It is not a release calendar
and does not authorize implementation by itself. The active reviewable phase,
owner decisions, and validation state are recorded in `TASKS.md`.

A task may span several roadmap tracks, but direct playback reliability, user
data safety, privacy, and explicit user choices always take precedence over
visual polish or feature breadth.

## Track 1 - Playback critical path

### Outcomes

- Direct playback starts through the lowest-latency valid path.
- Stream resolution, cache, retry, and fallback behavior remain bounded.
- Queue, Media3 player, MediaSession, notification, Android Auto, and background
  service expose one synchronized state.
- Song/audio mode and native-video mode remain explicit and independently
  correct.
- DSP work remains real-time safe and does not allocate, block, scan mutable
  collections, or perform expensive transcendentals per sample.

### Exit criteria for a phase

- The triggering scenario has a regression test where practical.
- Cancellation, stale publication, lifecycle cleanup, and fallback ordering are
  covered.
- Focused playback tests pass.
- Device-only checks are listed separately and are not implied by unit tests.

### Primary risks

Concurrency races, stale callbacks, shared-work cancellation, audio underruns,
provider changes, hidden fallback latency, and duplicated playback state.

## Track 2 - Persistence, offline use, and recovery

### Outcomes

- Room migrations are explicit and preserve existing data.
- Downloads and exported files remain bounded, resumable, and recoverable.
- Preferences, backups, queues, playlists, favorites, lyrics, and history keep
  backward-compatible identity.
- Offline content remains usable without remote decoration.

### Exit criteria for a phase

- Migration and serialization tests cover old and new representations.
- Backup and restore behavior is verified for changed settings.
- Storage growth, cleanup, cancellation, and partial failure are bounded.
- No user data is silently reset to simplify implementation.

### Primary risks

Destructive schema changes, mutable display text used as identity, partial file
writes, stale cache publication, and backup incompatibility.

## Track 3 - Responsive, accessible interface

### Outcomes

- Startup restores usable cached content before secondary refresh.
- Scrolling and common navigation remain responsive on constrained devices.
- Screen projections avoid unrelated recomposition.
- Accessibility, localization, RTL, reduced motion, lifecycle, and
  configuration changes are preserved.
- Optional motion and artwork never become correctness dependencies.

### Exit criteria for a phase

- State ownership and effect keys are explicit.
- Lazy list keys are stable and unique.
- Visible changes include localization and accessibility review.
- Performance-sensitive changes include focused measurement or a reproducible
  comparison when available.
- Screenshots or device checks are recorded for visual changes.

### Primary risks

Main-thread work, broad state observation, unstable identity, repeated decoding,
unbounded animation, inaccessible controls, and stale localized text.

## Track 4 - Extractor and remote-media resilience

### Outcomes

- Current extractor paths are observable and fail over deliberately.
- Runtime configuration, BotGuard/PO-token boundaries, cache semantics, and
  provider failures remain distinguishable.
- Remote media is validated before playback, caching, writing, or display.
- Provider degradation does not corrupt durable user state.

### Exit criteria for a phase

- Failure classes remain distinct in code and tests.
- Timeouts, redirects, MIME, response sizes, and host boundaries are bounded.
- Negative cache entries represent only conclusive misses.
- Fallback tests cover ordering and stale configuration.

### Primary risks

Provider drift, prompt or metadata injection, SSRF, redirect abuse, poisoning of
negative caches, and retry storms.

## Track 5 - Windows Desktop reliability

### Outcomes

- Desktop playback, downloads, mini player, deep links, media keys, update flow,
  and packaging remain independently testable.
- libvlc and native resources have explicit ownership and cleanup.
- Windows installers and portable artifacts preserve their established paths
  and naming.
- Desktop releases never alter Android versioning or latest-release semantics.

### Exit criteria for a phase

- Desktop wrapper checks pass in a supported environment.
- Native VLC, installer, update, protocol, and media-key checks are reported
  separately from JVM tests.
- Packaging and version changes are limited to the Desktop platform.

### Primary risks

Native handle leaks, OS-specific assumptions, packaging drift, update integrity,
and accidental Android/Desktop release coupling.

## Track 6 - Distribution and repository integrity

### Outcomes

- Pull requests provide reproducible evidence.
- CI uses least privilege and keeps untrusted code away from secrets.
- Android, F-Droid, and Desktop artifacts remain separated.
- Version, signing, checksums, release metadata, and artifact names are
  deliberate and verifiable.
- Duplicate or overlapping automation is prevented.

### Exit criteria for a phase

- Applicable CI, dependency, security, and release guards pass on the latest
  commit.
- Manual release checks remain unmarked until performed.
- Every external action is explicitly authorized.
- Rollback or revert scope is clear.

### Primary risks

Workflow privilege mistakes, secret exposure, artifact replacement, stale CI
results, accidental publication, and misleading release evidence.

## Track 7 - Agent-assisted engineering

### Outcomes

- `AGENTS.md` defines durable repository and path contracts.
- `SPEC.md`, `ROADMAP.md`, and `TASKS.md` keep requirements, sequencing, and
  current work separate.
- Native skills encode repeatable Levyra workflows instead of generic advice.
- ChatGPT, Codex, Claude Code, and OpenClaw use the same repository facts while
  retaining separate permission and publication boundaries.
- Implementation, independent review, CI, manual testing, merge, and release
  remain distinct gates.

### Exit criteria for a phase

- Agent configuration validation passes.
- Every referenced skill and planning document exists.
- The current task phase has explicit acceptance criteria and validation.
- A coding agent cannot infer permission to merge or release.
- OpenClaw delegation uses a dedicated Levyra workspace/agent and returns
  branch, diff, checks, blockers, and PR state to the owner.

### Primary risks

Stale instructions, duplicated rules, excessive tool access, self-review,
unverified completion claims, and autonomous publication beyond the owner's
request.

## Prioritization rule

When priorities conflict, use this order:

1. safety, privacy, and user data;
2. direct playback and explicit playback choices;
3. lifecycle, concurrency, and resource ownership;
4. offline reliability and recoverability;
5. responsive and accessible UI;
6. release and repository integrity;
7. optional enrichment and visual polish.

Update `TASKS.md` when activating a phase. Update this roadmap only when the
ordered outcomes, risks, or exit criteria themselves change.
