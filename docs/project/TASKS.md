# Levyra Active Tasks

## Active phase

**Name:** Android Home identity audit and dead-code cleanup  
**Roadmap track:** Track 3 - Responsive, accessible interface; Track 6 - Distribution and repository integrity  
**Status:** Implementation complete; automated validation is blocked in the connector environment  
**Scope:** Correct Home section identity for non-Latin scripts, add focused regression coverage, and remove the unused demo catalog stub. No playback, persistence schema, version, dependency, signing, packaging, Desktop, or release behavior is changed.

## Verified current behavior and root cause

`HomeRefreshStability.sectionIdentity()` reduced section titles to ASCII letters and digits. Distinct Arabic, Chinese, Japanese, Korean, Hebrew, Indic, Greek, and Cyrillic titles could therefore collapse to the same fallback identity (`section`). Because Home sanitization permits at most two occurrences of one identity, later valid localized shelves could be discarded as duplicates.

`DemoCatalogRepository` had no callers and every method returned only empty or zero-valued placeholder data.

## Acceptance criteria

- Home section identity preserves Unicode letters and numbers.
- Compatibility-equivalent Unicode forms normalize deterministically.
- Distinct non-Latin Home shelves are not discarded as duplicate fallback sections.
- Whitespace and punctuation still normalize to stable separators.
- Focused unit tests cover the original localized failure path.
- The unused demo catalog stub is removed without replacing it with another source of truth.
- Playback, queue, downloads, Room schemas, preferences, localization strings, Android and Desktop versions, workflows, signing, packaging, and releases remain unchanged.

## Work items

- [x] Inspect the current Home section sanitization and merge flow.
- [x] Confirm the failure path against supported non-Latin locales.
- [x] Replace ASCII-only identity filtering with Unicode-aware normalization.
- [x] Add focused regression coverage for Arabic, Chinese, Japanese, and Korean shelf titles.
- [x] Verify `DemoCatalogRepository` has no production or test callers.
- [x] Remove the unused demo catalog stub.
- [x] Inspect the branch diff for unrelated version, dependency, workflow, binary, generated-file, or secret changes.
- [ ] Run the focused Android unit test in a repository checkout.
- [ ] Run the broader Android unit-test suite and lint in a supported environment.
- [ ] Verify localized Home rendering on a device or emulator.

## Validation matrix

| Check | Required | Current state |
| --- | --- | --- |
| Root cause reproduced by code inspection | Yes | Verified against the ASCII-only identity filter and duplicate cap |
| Focused regression test added | Yes | Added; execution blocked because the connector environment has no repository checkout or Gradle runtime |
| `DemoCatalogRepository` reference search | Yes | Verified: no callers outside the deleted file |
| Complete branch diff inspection | Yes | Pending final comparison before pull-request creation |
| `./gradlew --no-daemon :app:testDebugUnitTest` | Yes | Blocked in the connector environment |
| `./gradlew --no-daemon :app:lintRelease` | Yes | Blocked in the connector environment |
| `./gradlew --no-daemon --no-configuration-cache assembleRelease` | Applicable before merge | Blocked in the connector environment |
| `python3 scripts/validate_agent_config.py` | Yes after task-file change | Blocked in the connector environment |
| Localized device/emulator Home check | Manual | Not performed |
| Playback, Android Auto, notification, PiP | No | Not affected by this phase |
| Desktop build and Windows checks | No | Desktop files are unchanged |
| Merge or release | Owner action | Not authorized by this phase |

## Behavior preserved

- The existing Home sanitization limits, merge ordering, track identity, cached-content retention, and structural-defer behavior remain unchanged.
- No user-visible strings or locale catalogs are modified.
- Playback, MediaSession, notification, Android Auto, queue, downloads, favorites, playlists, history, settings, backups, and Room schemas are untouched.
- Android and Desktop versions, artifacts, tags, signing, packaging, and release workflows remain independent and unchanged.

## Rollback boundary

Revert the Unicode identity change, its focused test, the demo-stub deletion, and this active-task update as one reviewable pull request. No migration or durable-data rollback is required.

## Update rule

Record test, CI, review, device, merge, and release status only from direct evidence. Replace this phase when a new reviewable task begins instead of accumulating unrelated work.
