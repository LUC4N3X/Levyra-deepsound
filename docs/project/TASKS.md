# Levyra Active Tasks

## Active phase

**Name:** Android Home identity audit and dead-code cleanup  
**Roadmap track:** Track 3 - Responsive, accessible interface; Track 6 - Distribution and repository integrity  
**Status:** Implementation complete; repository Gradle validation is pending in CI  
**Scope:** Correct Home section identity for non-Latin scripts, add focused regression coverage, and remove the unused demo catalog stub. No playback, persistence schema, version, dependency, signing, packaging, Desktop, or release behavior is changed.

## Verified current behavior and root cause

`HomeRefreshStability.sectionIdentity()` reduced section titles to ASCII letters and digits. Distinct Arabic, Chinese, Japanese, Korean, Hebrew, Indic, Greek, Cyrillic, and Thai titles could therefore collapse to the same fallback identity (`section`). Because Home sanitization permits at most two occurrences of one identity, later valid localized shelves could be discarded as duplicates.

`DemoCatalogRepository` had no callers and every method returned only empty or zero-valued placeholder data.

## Acceptance criteria

- Home section identity preserves Unicode letters, combining marks, and numbers.
- Compatibility-equivalent Unicode forms normalize deterministically.
- Existing Latin accent folding remains compatible (`Café` and `Cafe` keep the same identity).
- Distinct non-Latin Home shelves are not discarded as duplicate fallback sections.
- Whitespace and punctuation still normalize to stable separators.
- Focused unit tests cover the original localized failure path and scripts that require combining marks.
- The unused demo catalog stub is removed without replacing it with another source of truth.
- Playback, queue, downloads, Room schemas, preferences, localization strings, Android and Desktop versions, workflows, signing, packaging, and releases remain unchanged.

## Work items

- [x] Inspect the current Home section sanitization and merge flow.
- [x] Confirm the failure path against supported non-Latin locales.
- [x] Replace ASCII-only identity filtering with Unicode-aware normalization.
- [x] Preserve combining marks used by Indic, Thai, Arabic, and other writing systems.
- [x] Preserve the previous Latin diacritic-folding behavior.
- [x] Add focused regression coverage for Arabic, Chinese, Japanese, Korean, Hindi, Thai, and accented Latin titles.
- [x] Verify `DemoCatalogRepository` has no production or test callers.
- [x] Remove the unused demo catalog stub.
- [x] Inspect the branch diff for unrelated version, dependency, workflow, binary, generated-file, or secret changes.
- [ ] Run the focused Android unit test through the repository Gradle wrapper.
- [ ] Run the broader Android unit-test suite and lint in CI or a supported checkout.
- [ ] Verify localized Home rendering on a device or emulator.

## Validation matrix

| Check | Required | Current state |
| --- | --- | --- |
| Root cause reproduced by code inspection | Yes | Verified against the ASCII-only identity filter and duplicate cap |
| Focused regression test added | Yes | Added; repository execution pending in CI |
| Standalone Kotlin compile/smoke check | Supporting | Passed with the exact normalization function for accented Latin, Arabic, Chinese, Japanese, Hindi, and Thai samples |
| `DemoCatalogRepository` reference search | Yes | Verified: no callers outside the deleted file |
| Complete branch diff inspection | Yes | Verified: only the focused implementation, regression test, dead stub deletion, and active-task record changed |
| `./gradlew --no-daemon :app:testDebugUnitTest` | Yes | Pending in CI; local repository checkout unavailable |
| `./gradlew --no-daemon :app:lintRelease` | Yes | Pending in CI; local repository checkout unavailable |
| `./gradlew --no-daemon --no-configuration-cache assembleRelease` | Applicable before merge | CI build in progress; local repository checkout unavailable |
| `python3 scripts/validate_agent_config.py` | Yes after task-file change | Pending in CI; local repository checkout unavailable |
| Workflow Duplicates Guard | Yes | Passed on the pull-request head |
| SonarQube Cloud quality gate | Supporting | Passed with zero new issues and zero security hotspots |
| Localized device/emulator Home check | Manual | Not performed |
| Playback, Android Auto, notification, PiP | No | Not affected by this phase |
| Desktop build and Windows checks | No | Desktop files are unchanged |
| Merge or release | Owner action | Not authorized by this phase |

## Behavior preserved

- The existing Home sanitization limits, merge ordering, track identity, cached-content retention, structural-defer behavior, and Latin accent folding remain unchanged.
- No user-visible strings or locale catalogs are modified.
- Playback, MediaSession, notification, Android Auto, queue, downloads, favorites, playlists, history, settings, backups, and Room schemas are untouched.
- Android and Desktop versions, artifacts, tags, signing, packaging, and release workflows remain independent and unchanged.

## Rollback boundary

Revert the Unicode identity change, its focused test, the demo-stub deletion, and this active-task update as one reviewable pull request. No migration or durable-data rollback is required.

## Update rule

Record test, CI, review, device, merge, and release status only from direct evidence. Replace this phase when a new reviewable task begins instead of accumulating unrelated work.
