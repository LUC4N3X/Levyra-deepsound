# Levyra Active Tasks

## Active phase

**Name:** Android Home identity audit and dead-code cleanup  
**Roadmap track:** Track 3 - Responsive, accessible interface; Track 6 - Distribution and repository integrity  
**Status:** Implementation complete; final-head CI is running  
**Scope:** Correct Home section identity for non-Latin scripts, add focused regression coverage, ensure the PR workflow executes that coverage, and remove the unused demo catalog stub. No playback, persistence schema, version, dependency, signing, packaging, Desktop, or release behavior is changed.

## Verified current behavior and root cause

`HomeRefreshStability.sectionIdentity()` reduced section titles to ASCII letters and digits. Distinct Arabic, Chinese, Japanese, Korean, Hebrew, Indic, Greek, Cyrillic, and Thai titles could therefore collapse to the same fallback identity (`section`). Because Home sanitization permits at most two occurrences of one identity, later valid localized shelves could be discarded as duplicates.

`DemoCatalogRepository` had no callers and every method returned only empty or zero-valued placeholder data.

The selective unit-test command in `.github/workflows/pr-check.yml` did not include the new regression class, so the test would not have run in the primary pull-request gate without an explicit selector.

## Acceptance criteria

- Home section identity preserves Unicode letters, combining marks, and numbers.
- Compatibility-equivalent Unicode forms normalize deterministically.
- Existing Latin accent folding remains compatible (`Café` and `Cafe` keep the same identity).
- Distinct non-Latin Home shelves are not discarded as duplicate fallback sections.
- Whitespace and punctuation still normalize to stable separators.
- Focused unit tests cover the original localized failure path and scripts that require combining marks.
- The primary PR workflow executes the focused regression test.
- The unused demo catalog stub is removed without replacing it with another source of truth.
- Playback, queue, downloads, Room schemas, preferences, localization strings, Android and Desktop versions, signing, packaging, artifacts, and release behavior remain unchanged.
- Workflow triggers, permissions, signing setup, build commands, F-Droid checks, and artifact paths remain unchanged apart from adding the focused test selector and clarifying the step name.

## Work items

- [x] Inspect the current Home section sanitization and merge flow.
- [x] Confirm the failure path against supported non-Latin locales.
- [x] Replace ASCII-only identity filtering with Unicode-aware normalization.
- [x] Preserve combining marks used by Indic, Thai, Arabic, and other writing systems.
- [x] Preserve the previous Latin diacritic-folding behavior.
- [x] Add focused regression coverage for Arabic, Chinese, Japanese, Korean, Hindi, Thai, and accented Latin titles.
- [x] Add the focused regression class to the selective PR-check unit-test command.
- [x] Verify `DemoCatalogRepository` has no production or test callers.
- [x] Remove the unused demo catalog stub.
- [x] Inspect the branch diff for unrelated version, dependency, permission, trigger, signing, artifact, binary, generated-file, or secret changes.
- [ ] Complete the final-head PR Check workflow.
- [ ] Complete the final-head APK build and size-report workflows.
- [ ] Verify localized Home rendering on a device or emulator.

## Validation matrix

| Check | Required | Current state |
| --- | --- | --- |
| Root cause reproduced by code inspection | Yes | Verified against the ASCII-only identity filter and duplicate cap |
| Focused regression test added | Yes | Added and selected by the primary PR workflow |
| Standalone Kotlin compile/smoke check | Supporting | Passed with the exact normalization function for accented Latin, Arabic, Chinese, Japanese, Hindi, and Thai samples |
| `DemoCatalogRepository` reference search | Yes | Verified: no callers outside the deleted file |
| Workflow selector inspection | Yes | Verified: `HomeRefreshStabilityTest` is included in the existing selective unit-test command |
| Complete branch diff inspection | Yes | Verified: focused implementation, regression test, dead stub deletion, PR-test selector, and active-task record only |
| `python3 scripts/validate_agent_config.py` | Yes | Passed on a previous PR head; final-head run pending |
| `./gradlew --no-daemon :app:lintRelease` | Yes | Final-head CI pending |
| Focused unit tests | Yes | Final-head CI pending |
| `./gradlew --no-daemon --no-configuration-cache assembleRelease` | Yes | Final-head CI pending |
| F-Droid release compile | Yes | Final-head CI pending |
| Workflow Duplicates Guard | Yes | Passed on previous PR heads; final-head run pending |
| SonarQube Cloud quality gate | Supporting | Passed on a previous PR head with zero new issues and zero security hotspots; final-head analysis pending |
| Localized device/emulator Home check | Manual | Not performed |
| Playback, Android Auto, notification, PiP | No | Not affected by this phase |
| Desktop build and Windows checks | No | Desktop files are unchanged |
| Merge or release | Owner action | Not authorized by this phase |

## Behavior preserved

- The existing Home sanitization limits, merge ordering, track identity, cached-content retention, structural-defer behavior, and Latin accent folding remain unchanged.
- No user-visible strings or locale catalogs are modified.
- Playback, MediaSession, notification, Android Auto, queue, downloads, favorites, playlists, history, settings, backups, and Room schemas are untouched.
- The PR workflow keeps its current event filters, permissions, concurrency, Java/SDK setup, signing strategy, lint, release build, F-Droid build, reports, and artifact handling.
- Android and Desktop versions, artifacts, tags, signing, packaging, and release workflows remain independent and unchanged.

## Rollback boundary

Revert the Unicode identity change, its focused test, the PR-check selector, the demo-stub deletion, and this active-task update as one reviewable pull request. No migration or durable-data rollback is required.

## Update rule

Record test, CI, review, device, merge, and release status only from direct evidence. Replace this phase when a new reviewable task begins instead of accumulating unrelated work.
