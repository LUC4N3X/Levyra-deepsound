# Levyra extraction core

This Kotlin/JVM module owns YouTube player analysis and audio client resolution
for Levyra's extraction engine. It contains semantic discovery, URL-factory
discovery, legacy pattern compatibility, and the existing concurrent client
hedge with bounded recovery. Android uses these implementations through
`YoutubePlayerJsAnalyzer` and `hedgedAudioWithRecovery`.

Android first consults its existing bundled and last-known-good player registry,
synchronized from Zemer as the primary source and Faraday as the secondary
source. Local discovery supplies candidates when no validated configuration is
available. The registry, remote-source trust levels, and runtime verification
remain owned by the Android decoder.

Runtime dependencies are Kotlin's standard library and the same pinned coroutines
core already used by the Android app. Hosts own HTTP, JavaScript execution,
configuration persistence, and playback. Native/iOS targets are not configured.

```kotlin
when (val analysis = YoutubePlayerAnalyzer.analyze(playerJavascript)) {
    is YoutubePlayerAnalysis.Candidates -> {
        val timestamp = analysis.signatureTimestamp
        val transforms = analysis.transforms
    }
    is YoutubePlayerAnalysis.Unavailable -> {
        val reason = analysis.reason
    }
}
```

Candidates carry signature and `n` expressions and a ranking confidence. The
host must verify them before use; discovery alone does not establish that a
stream is playable. Ambiguous pairs remain separate, in deterministic priority
order, with at most six results. Input is limited to 6,000,000 UTF-16 code units;
the Android downloader separately enforces its existing byte limit.

## Audio resolution

`AudioStreamResolver.resolve` accepts ordered tiers of suspending client attempts.
Each attempt returns a host-verified stream or `null`; `null` means inconclusive.
The host classifies conclusive no-stream exceptions through `isNoPlayableStream`.
URL, expiry, MIME, redirect, and transport validation stay in the host's existing
verification path before an attempt returns a stream.

The first valid stream wins. Tiers retain their input order and hedge delay;
losing work is cancelled and joined before returning. A recovery invocation is
allowed only after every normal attempt conclusively reports no playable stream.
An ambiguous result, network/HTTP/verification failure, or cancellation suppresses
that recovery. The result distinguishes `Resolved`, `NoPlayableStream`, and
`Inconclusive`; its default diagnostic string redacts the stream payload.

Plans permit at most 32 normal attempts across at most 32 tiers, followed by at
most one recovery. Oversized plans and negative hedge delays fail before making
requests. The Android plan currently has at most eight client profiles and keeps
its existing health ordering, hedge delay, per-client network timeouts, stream
verification, and atomic recovery permit. Cancellation is checked before a result
is published, including when a client returns without suspending.

Run from the repository root:

```bash
./gradlew -p levyra-extraction test
```

This uses the repository's wrapper and version catalog without configuring the
Android application or requiring an Android SDK. The tests replay synthetic
legacy, renamed, and unsupported player shapes, preserve semantic and factory
regressions, and exercise client competition, recovery and cancellation without
live network requests.

These components provide analysis and resolution orchestration. Host clients
still implement fetching, decoding and delivery, so this is not yet a standalone
end-to-end YouTube extractor. Live playback and comparative performance require
separate validation.
