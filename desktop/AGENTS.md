# Levyra Desktop Module Instructions

These instructions extend the root `AGENTS.md` for every file under `desktop/`.

## Module scope

Levyra Desktop is a Windows client built with Kotlin/JVM and Compose Multiplatform. It shares the extractor and localization catalog with Android and plays audio through libvlc.

Before editing, read `desktop/README.md` and load `.agents/skills/levyra-desktop/SKILL.md` plus any additional matching security, extractor, CI, review, or release skill.

For any visual redesign, UI polish, hierarchy, spacing, typography, color, shape, motion, screenshot/reference recreation, or request to make the Desktop UI more premium, modern, distinctive, cohesive, or less AI-generated, automatically load `levyra-design-taste` together with `levyra-desktop` before editing. The design-taste skill supplements Desktop engineering rules; it never overrides accessibility, performance, lifecycle, localization, product behavior, or architecture.

## Module boundaries

- `core/` remains pure Kotlin/JVM and must not depend on Compose or native VLC APIs.
- `player/` owns the audio-player abstraction and queue; native libvlc access remains isolated in its implementation.
- `app/` owns dependency wiring, windows, Compose UI, lifecycle, onboarding, updates, protocol handling, and packaging.
- Reuse the shared extractor and localization catalog; do not fork Android logic or translations into divergent copies.
- Preserve one source of truth for playback shared by the main window, tray/background behavior, and mini player.
- Treat existing UI as a redesign by default: preserve behavior, navigation, keyboard/focus semantics and state ownership, then improve hierarchy, rhythm and visual consistency before adding decorative effects.
- Reuse Levyra theme tokens and existing components before introducing one-off colors, radii, spacing values, visual primitives or dependencies.

## Reliability and persistence

- Preserve single-instance behavior and forwarding of second-launch and deep-link requests.
- Preserve session restoration, queue semantics, shuffle/repeat, local-file preference, and next-track preloading.
- Keep downloads resumable, bounded, cancellable, persisted, and finalized through atomic moves.
- Keep native resources, callbacks, threads, libvlc instances, and shutdown ownership explicit.
- Preserve compatibility of data stored under `%APPDATA%\Levyra` unless an explicit migration is designed and tested.
- Preserve fallback behavior when global media keys are already owned by another process.

## Updates and release safety

- Verify update assets and SHA-256 data before installation; do not weaken verification.
- Desktop versioning lives only in `desktop/version.properties` and uses `desktop-v<version>` tags.
- Never change Android version values for a Desktop release.
- Preserve the rule that Desktop releases do not replace the Android release as the repository's Latest release.
- Do not commit bundled proprietary/native runtime files unless licensing, size, source, and repository policy have been explicitly reviewed.

## Validation

Use the Desktop wrapper from `desktop/` and run focused tests first, followed by applicable tasks such as:

```bash
./gradlew check
./gradlew assemble check
```

On Windows use `gradlew.bat`.

Treat MSI/EXE packaging, installer upgrade, protocol registration, tray behavior, media keys, single-instance forwarding, auto-update, real libvlc playback, visual polish, keyboard/focus behavior, and measured UI-performance claims as manual/native checks unless directly verified.