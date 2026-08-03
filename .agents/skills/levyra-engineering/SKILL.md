---
name: levyra-engineering
description: Coordinate cross-domain Levyra work that spans multiple subsystems, or orient an agent when no single specialized Levyra skill is sufficient. Prefer a specialized levyra-* skill for focused tasks.
---

# Levyra engineering coordinator

Use this skill for repository orientation, architectural investigations, or changes that genuinely cross several Levyra domains. Do not use it as a substitute for a more precise specialized skill.

## Start here

1. Read the root `AGENTS.md` completely.
2. Read every nearest `AGENTS.md` covering the files in scope.
3. Read `docs/ARCHITECTURE.md` and current implementation/tests.
4. Select every specialized skill that applies.
5. Establish one coherent change contract before editing.

## Specialized skill routing

| Work touches | Load |
| --- | --- |
| Android playback, queue, Media3, MediaSession, notification, Android Auto, prefetch, audio/video modes | `levyra-player` |
| InnerTube, extraction, stream resolution, runtime player configuration, network fallback | `levyra-extractor` |
| Room, migrations, schemas, caches, stores, backups, persistent personal data | `levyra-database` |
| Android Compose UI, state, navigation, animation, lifecycle, accessibility, RTL, localization | `levyra-compose` |
| Decorative motion artwork and remote motion media | `levyra-motion-artwork` |
| Windows Desktop, Compose Multiplatform, libvlc, downloads, mini player, deep links, updates, packaging | `levyra-desktop` |
| Secrets, URLs, redirects, SSRF, MIME, permissions, privacy, update integrity, workflow exposure | `levyra-security-review` |
| GitHub Actions, CI, artifacts, F-Droid, config sync, build/release automation | `levyra-ci-workflows` |
| Branch, commit, patch, or pull request review | `levyra-pr-review` |
| Pre-merge or pre-release validation, versions, signing, artifacts, checksums, packaging | `levyra-release-check` |

Several skills may apply. A player change that modifies stream resolution must load both `levyra-player` and `levyra-extractor`; remote media normally also requires `levyra-security-review`.

## Cross-domain change contract

Before editing, state internally:

- the exact user or developer outcome;
- the current control and data flow across modules;
- behavior and compatibility that must remain unchanged;
- ownership boundaries between UI, state, repositories, players, services, databases, native resources, workflows, and releases;
- the smallest coherent set of files;
- focused tests and broader validation required;
- checks that depend on Android SDK, JDK, Windows, libvlc, signing inputs, network, CI, emulator, or device.

## Coordination rules

- Current repository content is authoritative; do not rely on remembered versions.
- Prefer one source of truth and existing infrastructure over adapters that duplicate state or behavior.
- Keep Android and Desktop versioning and release behavior independent.
- Make security, persistence, lifecycle, and cancellation behavior explicit at subsystem boundaries.
- Avoid broad refactors unless the requested result cannot be achieved safely within current architecture.
- Run focused validation per subsystem, then inspect the complete integrated diff.

## Delivery

Report rationale/root cause, files changed, subsystem interactions, behavior preserved, tests/checks with results, blocked checks, residual risk, manual validation, and publication state. Never represent a plan as an applied patch or a skipped check as a pass.
