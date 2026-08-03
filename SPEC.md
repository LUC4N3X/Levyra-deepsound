# Levyra Product and Engineering Specification

## Purpose

This document defines Levyra's durable product behavior, engineering boundaries,
and acceptance criteria. It complements `docs/ARCHITECTURE.md`: the
specification states what must remain true, while the architecture document
describes how the current repository implements it.

Current implementation, tests, build files, workflows, and the nearest
`AGENTS.md` always override stale assumptions. Proposed behavior belongs here
only after the owner has approved it as a project requirement.

## Product scope

Levyra provides independent Android and Windows Desktop music clients.

The Android client is a Kotlin application built around Jetpack Compose,
AndroidX Media3, Room, WorkManager, OkHttp, Coil, and repository-owned playback
and persistence layers.

The Windows Desktop client is an independent Compose Multiplatform application
with its own playback, packaging, versioning, and release lifecycle.

## Required product behavior

### Playback

- A direct user playback request has priority over artwork, lyrics, refresh,
  diagnostics, metadata enrichment, prefetch, and animation.
- Android users retain an explicit choice between song/audio mode and native
  video mode. The application must not silently merge, remove, or override that
  choice.
- Audible playback, queue state, MediaSession, notification, Android Auto, and
  background service state remain synchronized.
- Static artwork is always a valid fallback. Decorative motion artwork is
  muted, optional, limited to song/audio mode, and must never delay playback.
- Stream resolution, fallback, retry, cache, and cancellation behavior must be
  bounded and observable. An inconclusive failure must not be stored as a
  conclusive negative result.
- Older asynchronous work must not publish over newer user intent.

### User data and settings

- Downloads, favorites, playlists, queues, lyrics, history, settings,
  localization, onboarding state, sessions, and backups are preserved unless a
  change explicitly targets them.
- Persistent schema changes require explicit, non-destructive migrations and
  regression coverage.
- Durable identity must not depend on mutable display text.
- Backup and restore behavior must remain backward compatible when new settings
  are introduced.

### Interface and accessibility

- Compose state follows unidirectional data flow and keeps screen projections
  stable and appropriately scoped.
- Network, database, parsing, decoding, file, metadata, and blocking native work
  remain off the UI thread.
- Lists use stable unique identity and avoid unnecessary recomposition.
- User-facing text is localized through the existing localization system.
- Accessibility, RTL behavior, reduced-motion choices, lifecycle, and
  configuration changes are considered for visible changes.
- Cached usable content remains visible while secondary refresh work runs.

### Offline and network behavior

- Offline content remains usable without requiring remote enrichment.
- Retries, timeouts, response sizes, concurrency, caches, storage growth,
  downloads, and prefetch are bounded.
- Provider-controlled URLs and redirects are treated as untrusted input.
- Cancellation is not reported, cached, or counted as an ordinary failure.

## Architecture boundaries

- Preserve the current unidirectional flow:
  `user intent -> controller/ViewModel -> repository/player operation ->
  immutable state -> UI`.
- Reuse existing clients, stores, caches, dispatchers, scopes, queues, lifecycle
  owners, players, extractors, and persistence.
- Do not create a second source of truth for playback, queue, persistence,
  localization, update state, or release state.
- Resource ownership must be explicit for coroutines, callbacks, receivers,
  surfaces, native handles, decoders, players, files, caches, and in-flight
  requests.
- Shared work must outlive any one caller when other callers still require it.
- Android and Desktop modules must not acquire accidental release or version
  coupling.

## Security and privacy

- Levyra does not add account login, cookies, private tokens, telemetry,
  tracking, or unrelated data collection without explicit approval.
- Secrets, signing material, keystores, private URLs, `.env`, and
  `local.properties` never enter the repository.
- Remote URLs are validated across scheme, host, port, user info, DNS/IP
  destination, redirect hops, MIME, timeout, filename/path, and response size
  where applicable.
- Android permissions and GitHub Actions permissions follow least privilege.
- Transport, redirect, MIME, checksum, signature, or host validation must not be
  weakened to make one provider response pass.
- Fork code, workflow inputs, downloaded artifacts, deep links, update
  metadata, filenames, and local IPC are untrusted at their relevant boundary.

## Platform and release boundaries

- Android and Desktop version values, packages, tags, artifacts, signing,
  installers, and releases are independent.
- Version values change only as part of an explicitly authorized release task.
- Publishing, merging, tagging, releasing, changing repository settings, and
  uploading store metadata require explicit owner authorization.
- Source builds intended for F-Droid preserve their separate unsigned and
  reproducible-build requirements.

## Engineering process requirements

- Read applicable `AGENTS.md` files, this specification, the active roadmap and
  task phase, relevant native skills, current code, and nearby tests before
  editing.
- Identify the verified current behavior and root cause before implementing a
  defect fix.
- Prefer the smallest coherent change that preserves unrelated behavior.
- Avoid drive-by refactors, dependency churn, version changes, formatting
  noise, speculative abstraction, and parallel infrastructure.
- Add or update regression coverage for defects, migrations, matching,
  security boundaries, lifecycle, and concurrency when applicable.
- Run focused validation first, then the applicable broader repository checks.
- Treat a blocked check as blocked, never as passed.
- Use a dedicated branch and draft pull request by default when publication is
  authorized.

## Non-goals

- Replacing current architecture with a generic framework because an agent
  prefers a different pattern.
- Making Android and Desktop feature-identical at the cost of platform
  reliability.
- Allowing artwork, animation, lyrics, or enrichment to own the playback
  critical path.
- Adding broad autonomous publication or release permissions to an AI agent.
- Treating generated code or an agent report as validation evidence.
- Copying external agent instructions without adapting them to Levyra's current
  repository and risks.

## Acceptance criteria for any change

A change is ready for review only when:

1. its requirement and scope are explicit;
2. behavior that must remain unchanged is identified;
3. the implementation matches current architecture and ownership;
4. relevant focused tests pass or are truthfully reported as blocked;
5. applicable Android, Desktop, documentation, CI, security, or release checks
   are defined and run where the environment supports them;
6. the complete diff contains no unrelated edits, generated artifacts, secrets,
   conflict markers, or accidental version changes;
7. manual checks are listed and left unverified until actually performed;
8. `SPEC.md`, `ROADMAP.md`, `TASKS.md`, architecture, and user documentation are
   synchronized when the change affects them;
9. review findings are fixed or explicitly explained against the latest commit;
10. merge and release remain separate owner-controlled actions.
