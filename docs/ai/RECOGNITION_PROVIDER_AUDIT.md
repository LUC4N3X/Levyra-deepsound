# Recognition Provider Audit

This audit records the factual basis for shipping Levyra's Shazam-style
recognition core (`app/src/main/java/com/luc4n3x/levyra/feature/recognition/`)
as a provider abstraction with a `NoOpRecognitionProvider` default, instead of
bundling a concrete fingerprint backend.

## Correction to the reference premise

The implementation brief that started this work described the reference as
"Metrolist's MusicRecognitionService/VibraSignature." That naming does not
match the public `MetrolistGroup/Metrolist` repository. A GitHub code search
confirms `VibraSignature.kt` and `MusicRecognitionService.kt` live in
`vivizzz007/vivi-music` (path
`app/src/main/kotlin/com/music/vivi/recognition/`), a separate YouTube Music
Android client in the same InnerTune-derived family, not in Metrolist itself.
Metrolist's own issue tracker (`MetrolistGroup/Metrolist#2131`, "Add Audio
Fingerprinting/Music Recognition via Microphone") is an open feature request
that proposes ACRCloud or ShazamKit/ML Kit; it was closed with no linked pull
request and no merged code, so Metrolist has not shipped this feature. This
audit therefore evaluates the actual `vivi-music` implementation, which is the
real source of the `VibraSignature`/`MusicRecognitionService` names.

## What the reference implementation does

`MusicRecognitionService.kt` in `vivi-music` captures roughly 10 seconds of
mono 44.1 kHz microphone audio, resamples it to 16 kHz, and passes it to
`VibraSignature.kt`. That file's header states it wraps a native library that
implements "the Shazam signature algorithm" and exposes a
`ShazamSignatureGenerator.fromI16()` entry point consumed from Kotlin. The
resulting signature is sent through a `Shazam.recognize()` call with no
visible API key in the calling code, i.e. a direct call to Shazam's
undocumented, unofficial recognition endpoint rather than a public, contracted
API. The feature's own file header credits `aleksey-saenko/MusicRecognizer`
(GPLv3-or-later, later published on F-Droid as "Audile") as the design it was
adapted from; that upstream project supports AudD, ACRCloud, and Shazam
recognition, each requiring its own backend integration.

The native fingerprint code traces to the `vibra`/`libvibra` project
(`BayernMuller/vibra`), a C++ reimplementation of Shazam's signature
algorithm inspired by SongRec, licensed GPL-3.0. `vivi-music`'s own
development guide documents building this native fingerprint library
("vibrafp") as a required native-build step, i.e. an NDK/cmake cross-compiled
`.so` per ABI, not a pure-Kotlin/JVM dependency.

## Implications for Levyra

- **License**: the reference fingerprint code (`vibra`/`libvibra`) and the
  design it was adapted from (`MusicRecognizer`/Audile) are GPL-3.0(-or-later).
  Levyra itself is GPL-3.0, so license compatibility is not the blocker; the
  blocker is that vendoring or reimplementing someone else's GPL fingerprint
  algorithm line-for-line is exactly the "do NOT copy GPL code" constraint
  this task was given, and the native build/attribution overhead would need
  to be carried indefinitely.
- **Native library**: the actual fingerprint generation is native code (JNI,
  NDK-built, per-ABI `.so`), not pure Kotlin. Levyra's constraint is no native
  libs and no new Gradle dependencies, and F-Droid's reproducible-build
  requirements make bundling and maintaining a cross-compiled native
  fingerprinting library materially heavier than the rest of the client,
  which is pure Kotlin/JVM plus already-declared dependencies.
- **Endpoint**: the reference implementation calls Shazam's private,
  undocumented recognition endpoint with no publicly documented terms of
  service or contracted access. Depending on an unofficial, reverse-engineered
  endpoint is a stability and legal-risk surface Levyra should not adopt
  silently by default; it also does not match Levyra's existing pattern of
  validating provider-controlled URLs and hosts before connecting.
- **No single correct backend**: even the `MusicRecognizer`/Audile project
  Vivi-Music credits treats the backend as swappable (AudD, ACRCloud, Shazam),
  each with different licensing, key requirements, and privacy posture. That
  confirms recognition backend choice is a policy/product decision, not
  something this core module should hardcode.

## Why Levyra ships an abstraction plus a NoOp default

Given the above, this change delivers `RecognitionProvider` as an interface
(`RecognitionProvider.kt`) so the UI, controller, and preprocessing pipeline
never couple to a concrete backend, and `NoOpRecognitionProvider` as the only
shipped implementation, always returning
`RecognitionOutcome.Error(RecognitionErrorKind.Unavailable)`. This keeps the
F-Droid build clean (no native libs, no new dependencies, no bundled
proprietary or reverse-engineered endpoint calls) while leaving capture
(`MicrophoneCapture.kt`), preprocessing (`AudioPreprocessor.kt`), and the state
machine (`MusicRecognitionController.kt`) fully functional and testable.
Wiring a real backend (self-hosted fingerprinting, a documented paid API with
an owner-supplied key, or similar) is a separate, explicit product decision
outside this task's scope.

## Sources

- `MetrolistGroup/Metrolist` GitHub repository and issue #2131 (verified via
  GitHub search and issue fetch on 2026-08-18): no recognition feature is
  merged; the referenced names do not exist in this repository.
- `vivizzz007/vivi-music`, `app/src/main/kotlin/com/music/vivi/recognition/`
  (`VibraSignature.kt`, `MusicRecognitionService.kt`), found via GitHub code
  search and fetched on 2026-08-18.
- `BayernMuller/vibra` (GPL-3.0, C++ Shazam signature reimplementation
  inspired by SongRec).
- `aleksey-saenko/MusicRecognizer` (GPLv3-or-later; published on F-Droid as
  "Audile"; supports AudD, ACRCloud, and Shazam backends).
