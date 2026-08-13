# Levyra 2.3.20

## Highlights

Levyra 2.3.20 is a focused reliability release for external playback controls. Hardware media keys, remotes, lock-screen and quick-panel transport controls and other media controllers can now skip to the next or previous track, which previously only worked inside the application. The shuffle and favorite media-control labels are also served from the string catalog instead of being hardcoded to Italian. Libraries, favorites, playlists, downloads, queue state and settings are unchanged.

## What's New

### External skip controls

- The playback session now advertises the standard next-track and previous-track transport commands, so media buttons, headset and car remotes, the lock screen, the system media panel and other media controllers can change track.
- Next and previous requests coming from outside the application are routed through the existing queue engine, so radio expansion, repeat mode, shuffle, history and queue persistence behave exactly as they do for the in-app controls.
- Previous still rewinds to the start of the current track when playback has advanced past the first seconds, matching the in-app behavior and standard media-player conventions.
- The previous redundant custom next and previous notification actions were removed now that the standard transport controls are available in every surface.

### Localization

- The shuffle and favorite media-control labels are now provided by the string catalog with an English default and an Italian translation, instead of being hardcoded to Italian in the playback service.

## Fixes

- Fixed next-track and previous-track commands being ignored by hardware media keys, remotes, the lock screen, the system media panel and other external media controllers.
- Fixed the shuffle and favorite media-control labels being hardcoded to Italian for every language.

## Notes

This is an Android-only release. The separately versioned Windows application remains on its own release channel and is not rebuilt or republished by this Android version bump. No permission, dependency, SDK, shrinker or signing configuration was changed.

## Versioning

- Version name: `2.3.20`
- Version code: `2032000`

## Validation

- Android release metadata, Gradle fallbacks, documentation badges, user agents, and Fastlane changelogs were aligned to version `2.3.20`.
- The release guard verifies version-name, version-code, tag, release-notes, signing, and publishing consistency.
- The signed GitHub APK and the reproducible F-Droid APK are produced from the same commit by the protected release workflow.
- External next and previous transport commands were verified with the debug variant on a physical Samsung SM-S936B running Android 16 (SDK 36), including track change, previous-rewind behavior and play/pause.
- Existing signing identity and GitHub/F-Droid update compatibility remain unchanged.

## Upgrade notes

No migration is required. Existing libraries, favorites, downloads, playback state, and settings remain compatible after updating. GitHub installations continue to receive the signed GitHub release; once the F-Droid metadata is merged, F-Droid installations receive the corresponding reproducible build through F-Droid.

## Final note

Levyra 2.3.20 makes the player behave correctly everywhere Android exposes media controls, so skipping a track no longer requires opening the application.
