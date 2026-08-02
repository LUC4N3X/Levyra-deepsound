# Home redesign verification

## Automated

- `./gradlew --no-daemon :app:testReleaseUnitTest`
- `./gradlew --no-daemon :app:lintRelease`
- `./gradlew --no-daemon :app:assembleRelease`

## Device

- Open Home with no current track.
- Open Home with a paused, playing and resolving track.
- Verify every quick-access tile action.
- Toggle compact Home mode.
- Toggle animations off.
- Check dark and light theme presets.
- Scroll through all enabled Home sections.
- Switch chart region and play a chart track.
- Confirm mini-player and bottom content insets remain correct.
- Confirm settings, album, artist and track action navigation.
