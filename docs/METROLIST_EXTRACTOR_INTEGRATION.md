# MetrolistExtractor integration

Levyra uses MetrolistExtractor as its primary NewPipe-compatible extraction dependency.

## Dependency

```text
Group: com.github.MetrolistGroup
Artifact: MetrolistExtractor
Version: 3cd3341
Repository: https://jitpack.io
```

The version is pinned to the same MetrolistExtractor commit used by the Metrolist Android project at the time of this integration. Pinning prevents an unreviewed upstream change from altering release behavior between builds.

## Runtime behavior

Levyra initializes the extractor through its OkHttp-backed NewPipe downloader, preserves extractor-provided request headers, respects redirect policy, and uses the extractor as the primary playback resolver. The direct InnerTube resolver remains a delayed fallback when extraction fails or times out.

MetrolistExtractor handles the YouTube player-client strategy internally. Levyra does not override the player client through fork-specific APIs.

## Updating

1. Review the target MetrolistExtractor commit and its YouTube extraction changes.
2. Update `metrolistExtractor` in `gradle/libs.versions.toml`.
3. Run the dependency insight task and release build.
4. Test audio-only playback, muxed video, HLS, search fallback, downloads, and expired-stream recovery.
5. Update this document and `THIRD_PARTY_NOTICES.md` when the pinned commit changes.

## License

MetrolistExtractor is distributed under GPL-3.0. Levyra preserves the project attribution and license notices in the README, LICENSE, and THIRD_PARTY_NOTICES files.
