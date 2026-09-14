# Levyra Active Tasks

## Active phase

**Name:** Listening Insights

**Roadmap tracks:** Track 2 - Persistence; Track 3 - Responsive, accessible interface

**Status:** Implemented locally and statically reviewed. Build, test execution, and smartphone validation remain unverified; Gradle work was stopped at the owner's direction after host loopback failures, and no device is connected.

**Scope:** Add a premium full-screen Listening Insights experience from Library/Pulse. It projects existing `listen_events` and `listen_lifetime_*` data into period metrics, activity, rhythm, discovery, top music, and a paginated searchable history without creating another tracker or requiring an account.

## Acceptance criteria

- Period navigation covers 24H, 7D, 30D, 6M, and ALL with one compact, accessible selector.
- Metrics include listen time, policy-counted plays, distinct tracks and artists, completion, discovery, peak hour, peak day, and previous-period trend where meaningful.
- Hourly, daily, weekly, and monthly chart projections remain readable for their periods and animate only when animations are enabled.
- Top artists and tracks use existing artwork and playback/navigation owners; Discovery has a distinct editorial treatment.
- History is grouped, searchable on demand, keyset-paginated, and rendered with stable keys and content types.
- Aggregate and page queries stay off the main thread and do not load the complete event table for period changes.
- Existing Library, Pulse, Your Sound, Recap, tracking, playback, backup, and offline behavior remain intact.
- User-facing copy uses Levyra's localization catalog; dark/light, font scaling, RTL, TalkBack, and reduced motion are reviewed.

## Work items

- [x] Analyze Vivi Music History/Stats and Levyra Library/Pulse/Recap visual patterns.
- [x] Reconcile existing event, lifetime, policy, DNA, Pulse, and Recap data owners.
- [x] Add bounded aggregate and keyset history queries with focused tests.
- [x] Add immutable insights models, repository projection, and screen ViewModel.
- [x] Add Library/Pulse entry, full-screen Compose UI, charts, search, and history.
- [x] Complete a second visual pass for hierarchy, spacing, type, artwork, motion, and density.
- [ ] Verify build, focused tests, smartphone layout/scroll behavior, final diff, and required AI quality gates.

## Preserved behavior and boundaries

- `ListenPlayPolicy` and the existing event/lifetime stores remain the only listening source of truth.
- No account, remote-history dependency, telemetry, permission, new chart dependency, version, release, or publication change.
- Raw event detail is retained for a bounded three years; previously pruned events cannot be reconstructed, and lifetime totals must remain truthful beyond that detailed window.
- Playback, queue, Recap, Your Sound, downloads, favorites, playlists, lyrics, backups, and Desktop remain outside behavioral change.

## Rollback boundary

Remove the dedicated insights models, repository, ViewModel, UI and DAO projections, then revert only the Library/Pulse entry and overlay visibility wiring. Existing listen events and lifetime aggregates require no conversion or destructive rollback.

## Update rule

Execute in reviewable phases: data projection, state/navigation, visual implementation, visual refinement, then validation. After each phase run focused checks and inspect its diff. Smartphone, accessibility, scroll/recomposition, and screenshot checks remain unverified until directly observed; environmental Gradle or device failures remain blocked rather than pass.
