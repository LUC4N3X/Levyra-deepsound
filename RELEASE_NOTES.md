# Levyra 2.3.18

## Highlights

Levyra 2.3.18 focuses on reliable playback, safer updates, faster access to music, and a more consistent Android experience. The release strengthens YouTube Music recovery, introduces Levyra Nexus as a shared reliability layer, expands chart and track actions, and improves accessibility without changing existing libraries, downloads, or preferences.

## What's New

### Resilient playback and network routing

- Added verified YouTube Music fallback paths for cases where a primary request or media endpoint is unavailable.
- Introduced adaptive route health tracking so temporary failures do not unnecessarily disable healthy playback routes.
- Improved denial handling and retry balance to avoid repeated requests while still recovering when service access resumes.
- Updated embedded player configurations used by the local decoder.
- Hardened fallback selection so incompatible or unverifiable media alternatives are rejected.

### Levyra Nexus reliability core

- Added the dependency-light Levyra Nexus module for shared network, playback, and update decisions.
- Routed Android network intelligence through Nexus while keeping platform-specific behavior inside the app.
- Added typed results and explicit transition semantics for predictable recovery behavior.
- Introduced route-health scoring with tests for success, transient failure, denial, and recovery.
- Connected update selection to Nexus so only compatible, verifiable release artifacts can be offered.

### Charts and discovery

- Added YouTube Music chart feeds with official artwork resolution.
- Improved country-aware chart discovery and playlist shortcuts.
- Added faster access to Top 50 playlists from the relevant Home and Library surfaces.
- Refined chart parsing and playable-track matching with additional regression tests.
- Synced extractor player data with the current supported playback configuration.

### Track actions and artist presentation

- Added a long-press action sheet to tracks in the Personal Orbit shelf.
- Added consistent actions for opening albums, artists, playback, queue, favorites, and related destinations.
- Improved artist imagery and presentation consistency across Home, Library, and detail surfaces.
- Ensured the track action sheet honors the reduced-animation preference.
- Improved semantics, focus behavior, and accessible labels for assistive technologies.

### Settings and usability

- Reorganized settings into a clearer category hub.
- Improved navigation and grouping for playback, appearance, privacy, storage, and advanced options.
- Refined labels and localized copy across supported languages.
- Improved Android Auto library behavior and several navigation edge cases.

## Reliability and security

- Update artifacts now require a valid version, supported ABI, HTTPS download URL, SHA-256 digest, and compatible package metadata.
- Update comparison rejects malformed semantic versions and unsafe fallback assets.
- Network routing records failures conservatively and avoids treating one denied request as permanent route failure.
- Playback transition behavior is covered by dedicated Nexus tests.
- No analytics, advertising, or developer telemetry was added.
- F-Droid builds continue to disable the GitHub self-update prompt and use F-Droid metadata for updates.

## Fixes

- Fixed fallback behavior that could select an incompatible YouTube media candidate.
- Fixed request-denial health accounting that could penalize clients too aggressively.
- Fixed album lookup consistency in the new track action sheet.
- Fixed animation behavior in the action sheet when reduced animations are enabled.
- Fixed several chart parsing and playable-track matching edge cases.
- Fixed update selection accepting artifacts without complete verification data.
- Fixed ABI inference and semantic-version handling for release assets.
- Fixed accessibility gaps in track actions and related navigation.

## Notes

This Android release includes the application-side integration required by Levyra Nexus. The separately versioned Windows application remains on its own release channel and is not rebuilt or republished by this Android version bump.

## Versioning

- Version name: `2.3.18`
- Version code: `2031800`

## Validation

- Android release metadata, Gradle fallbacks, documentation badges, user agents, and Fastlane changelogs were aligned to version `2.3.18`.
- The release guard verifies version-name, version-code, tag, release-notes, signing, and publishing consistency.
- The signed GitHub APK and the reproducible F-Droid APK are produced from the same commit by the protected release workflow.
- Nexus route, playback-transition, and update-verification behavior is covered by automated tests.
- Existing signing identity and GitHub/F-Droid update compatibility remain unchanged.

## Upgrade notes

No migration is required. Existing libraries, favorites, downloads, playback state, and settings remain compatible after updating. GitHub installations continue to receive the signed GitHub release; once the F-Droid metadata is merged, F-Droid installations receive the corresponding reproducible build through F-Droid.

## Final note

Levyra 2.3.18 makes playback and updates more dependable while bringing faster chart access, richer track actions, and a cleaner, more accessible experience.
