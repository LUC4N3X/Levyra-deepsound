# Remote audio credentials implementation plan

> Implement inline using the executing-plans workflow.

**Goal:** Update Android Qobuz and TIDAL credentials without another APK.
**Architecture:** A shared validated snapshot store reads one fixed repository
JSON URL, caches atomically and supplies both APIs. Existing embedded values are
the offline first-install fallback.
**Tech stack:** Kotlin, coroutines, OkHttp, org.json, AtomicFile, JUnit.
**Spec:** `docs/superpowers/specs/2026-10-30-remote-audio-credentials-design.md`

## Constraints and review focus

No UI, dependencies, versions, Desktop changes or credential logging. Preserve
cancellation, network allowlists, bounded retries and existing playback owners.
Test malformed/oversized updates, disk failure, concurrent auth failures, changed
account counts and TIDAL token rotation across provider configuration changes.

## Tasks

- [x] Add failing store and API recovery tests with synthetic credentials.
- [x] Implement shared models, strict JSON parsing and serialized refresh/cache.
- [x] Add fixed-URL transport and private atomic cache; initialize at startup.
- [x] Wire Qobuz bounded account retries and TIDAL session/config rotation.
- [x] Seed remote JSON from existing embedded values; document update procedure.
- [ ] Run focused tests, quality gates, inspect complete diff and open draft PR.

## Validation observed

The isolated JVM harness passes 29 focused tests with the project Kotlin,
coroutines, OkHttp and JSON versions. The changed runtime cache source also
compiles against Android API classes. The application HTTP factory is replaced
by an isolated JVM factory in this harness; it is not a full Android build.
The fast repository gate and Kotlin footgun scan pass. The full gate was
attempted; Android tests, lint and assembly are blocked by the unavailable
Gradle distribution download. Device playback and independent review remain
unverified.
