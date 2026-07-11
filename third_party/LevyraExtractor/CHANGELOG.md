# Changelog

## v1.0.0-levyra.10

- Pin the publication fallback version to v1.0.0-levyra.10.
- Publish every submodule with the JitPack-provided version.
- Verify that the extractor and timeago-parser POM and JAR files exist in Maven Local.
- Fail the JitPack build immediately when a required module is not published.

## v1.0.0-levyra.9

- Correct JitPack coordinates for the root artifact and every published submodule.
- Use JitPack-provided GROUP, ARTIFACT and VERSION values during publication.
- Remove the duplicate root dependency on timeago-parser.
- Support direct consumption of the extractor module without the generated aggregate artifact.

## v1.0.0-levyra.8

- Make Android VR, Safari, and TVHTML5 playback responses independent and resilient.
- Aggregate direct audio and video formats from every successful NewPipe client.
- Preserve per-client CPN values during stream URL construction.
- Use SABR or HLS only when direct formats are unavailable.
- Stop optional client failures from invalidating a playable response.

## v1.0.0-levyra.7

- Added independent Android VR, Safari and TVHTML5 player requests.
- Added real cross-client aggregation for audio, muxed video and video-only formats.
- Preserved a separate content playback nonce for every player client.
- Added one batched signature and throttling-parameter deobfuscation pass across all direct streams.
- Prevented an error from one player client from discarding a valid response from another client.
- Added direct URL, SABR and HLS fallback ordering.
- Added robust cipher parsing and safe query parameter handling.
- Restored DASH and HLS manifest resolution across all available player responses.
- Added bounded fallback timing so a successful player response is not held by a slower client.
- Added third-party notices and CI validation.
