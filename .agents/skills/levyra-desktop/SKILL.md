---
name: levyra-desktop
description: Implement, debug, or review Levyra Desktop for Windows, including Kotlin/JVM, Compose Multiplatform, libvlc, queue, downloads, persistence, mini player, deep links, updates, packaging, and desktop releases.
---

# Levyra Desktop workflow

## Required context

1. Read the root `AGENTS.md` and `desktop/AGENTS.md`.
2. Read `desktop/README.md` and the relevant architecture documentation.
3. Inspect the affected `desktop/core`, `desktop/player`, and `desktop/app` code plus nearby tests.
4. Inspect Desktop workflows and `desktop/version.properties` when packaging, updates, or releases are involved.

## Architecture boundaries

- `desktop/core` remains pure Kotlin/JVM and must not depend on Compose or native VLC APIs.
- `desktop/player` owns the `AudioPlayer` abstraction and queue; native libvlc access stays isolated in its implementation.
- `desktop/app` wires dependencies, lifecycle, windows, UI, updates, protocol handling, and packaging.
- Reuse the shared extractor and localization catalog rather than creating divergent Desktop copies.
- Android and Desktop versioning, build, packaging, and release tags remain independent.

## Guardrails

- Preserve single-instance behavior and forward second-launch/deep-link requests to the existing process.
- Preserve queue, session restoration, local-file preference, resumable downloads, and atomic finalization.
- Keep libvlc lifecycle, callbacks, native resources, and shutdown ownership explicit.
- Keep the main player and mini player synchronized without creating a second playback source of truth.
- Preserve global media-key behavior and handle ownership failure without breaking in-window controls.
- Validate update assets and SHA-256 checks before installation; never weaken verification.
- Preserve local data under `%APPDATA%\Levyra` and existing JSON compatibility unless an explicit migration is designed.
- Keep shared translations synchronized; Desktop-only strings must remain clearly scoped.

## Validation

Run focused JVM tests first, then from `desktop/` run applicable tasks such as `./gradlew check` or `./gradlew assemble check`. Treat Windows installer, EXE/MSI packaging, protocol registration, tray, media keys, auto-update, single-instance behavior, and real libvlc playback as manual/native checks unless directly verified.
