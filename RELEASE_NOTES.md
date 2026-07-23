# Levyra 2.3.16

## Highlights

Levyra 2.3.16 brings a major Home experience upgrade focused on discovery, personalization, visual polish, and consistency.

## What's New

### Smarter Levyra Selection

- Added a daily editorial spotlight for fresh releases, charting tracks, and personalized picks.
- Spotlight content now respects Home visibility settings.
- Mood selection now directly affects spotlight ranking.
- The active spotlight remains stable while its track is playing.
- Release labels now use verified calendar dates and avoid misleading claims.

### Levyra Collections

- Added curated collections for Fresh, Local, Workout, Chill, Focus, Party, Rap, Pop, and Discovery.
- Collections are ranked and rotated to provide more variety over time.
- Duplicate and low-quality collections are filtered automatically.
- Hidden Home sections no longer reappear through editorial collections.
- Collection descriptions were rewritten with neutral, user-focused wording.

### Mood-Aware Home

- Mood filters now change the actual Home content instead of only reordering visible cards.
- Spotlight, collections, quick picks, charts, and recommendations adapt to the selected mood.
- Mood scoring considers tags, energy, and listening behavior.

### Adaptive Home Background

- The Home header now reacts to the artwork palette of the current Levyra Selection.
- Added a subtle animated transition between artwork colors.
- The Animations setting is fully respected, with instant color changes when animations are disabled.
- Palette loading and persistence run outside the Compose main thread.

### Progressive Home Loading

- Important sections now render first for a faster perceived startup.
- Secondary content appears progressively without replacing already loaded sections.
- Reduced unnecessary shimmer and visual flicker during refreshes.

### Redesigned Artists Section

- Rebuilt the Artists section with a cleaner editorial layout.
- Removed oversized artist badges and heavy card containers.
- Added larger portraits, refined dynamic borders, improved spacing, and clearer typography.
- Improved consistency with the rest of the Home design.

### Localization

- Added and updated editorial Home translations across all supported languages.
- Completed missing Greek and Filipino collection labels.
- Removed hardcoded platform references from user-facing collection copy.
- Improved chart and release wording for factual accuracy.

## Fixes

- Fixed incorrect UTC release timestamp parsing.
- Fixed calendar-day calculations around daylight-saving time changes.
- Fixed singles being incorrectly labeled as new albums.
- Fixed chart tracks being described as newly entered without verified entry data.
- Fixed collection ordering that could permanently hide Pop, Editorial, and Discovery collections.
- Fixed the adaptive background ignoring the disabled-animations setting.
- Fixed a compilation error in the redesigned Artists section.

## Notes

This release includes extensive Home UI and recommendation-engine changes. Existing libraries, downloads, playback state, and user settings remain compatible.

## Versioning

- Version name: `2.3.16`
- Version code: `2031600`

## Validation

- The F-Droid release build completed successfully with `-PlevyraFdroidBuild=true`.
- Release notes and Fastlane changelogs were updated for version code `2031600`.
- Existing release signing and GitHub/F-Droid update compatibility remain unchanged.

## Upgrade notes

No migration is required. Existing libraries, downloads, playback state, and user settings remain compatible after updating.

## Final note

Levyra 2.3.16 makes Home faster, more adaptive, and more useful while preserving the playback and library behavior users already rely on.
