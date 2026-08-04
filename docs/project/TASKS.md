# Levyra Active Tasks

## Active phase

**Name:** Android Home identity audit and dead-code cleanup  
**Roadmap track:** Track 3 - Responsive, accessible interface; Track 6 - Distribution and repository integrity  
**Status:** Implementation complete; final-head CI is running  
**Scope:** Correct localized Home shelf identity, keep merge keys independent from display titles, add focused regression coverage, ensure the PR workflow executes that coverage, and remove the unused demo catalog stub. No playback, persistence schema, version, dependency, signing, packaging, Desktop, or release behavior is changed.

## Verified current behavior and root cause

Home shelf handling had two separate identity problems:

1. title normalization retained only ASCII letters and digits, so distinct Arabic, Chinese, Japanese, Korean, Hebrew, Indic, Greek, Cyrillic, and Thai titles could collapse to the same fallback identity (`section`) during sanitization;
2. `mergeSections()` independently keyed previous and incoming shelves from `HomeSection.title`, even though that value is localized display text. A translated or repository-renamed title could therefore make unchanged content look like a structural replacement while structural updates were frozen.

`DemoCatalogRepository` had no callers and every method returned only empty or zero-valued placeholder data.

The selective unit-test command in `.github/workflows/pr-check.yml` did not include the new regression class, so the test would not have run in the primary pull-request gate without an explicit selector.

## Acceptance criteria

- Home title sanitization preserves Unicode letters, combining marks, and numbers.
- Compatibility-equivalent Unicode forms normalize deterministically.
- Existing Latin accent folding remains compatible (`Café` and `Cafe` keep the same title identity).
- Distinct non-Latin Home shelves are not discarded as duplicate fallback sections.
- Merge keys are derived from stable track identity rather than localized display text.
- A localized title change with unchanged content remains a non-structural update.
- A partial track refresh carries forward the previous section key when the shelves have meaningful identity overlap.
- A complete content refresh with the same title preserves the previous compatible behavior.
- A genuinely unrelated shelf replacement remains structural and is deferred while structural changes are frozen.
- The primary PR workflow executes the focused regression test.
- The unused demo catalog stub is removed without replacing it with another source of truth.
- Playback, queue, downloads, Room schemas, preferences, localization strings, Android and Desktop versions, signing, packaging, artifacts, and release behavior remain unchanged.
- Workflow triggers, permissions, signing setup, build commands, F-Droid checks, and artifact paths remain unchanged apart from adding the focused test selector and clarifying the step name.

## Work items

- [x] Inspect the current Home section sanitization and merge flow.
- [x] Confirm the Unicode failure path against supported non-Latin locales.
- [x] Replace ASCII-only title filtering with Unicode-aware normalization.
- [x] Preserve combining marks used by Indic, Thai, Arabic, and other writing systems.
- [x] Preserve the previous Latin diacritic-folding behavior.
- [x] Separate title identity used for duplicate display shelves from merge identity used for refresh continuity.
- [x] Key merges from stable track identities instead of `HomeSection.title`.
- [x] Carry previous keys across meaningful partial-refresh overlap.
- [x] Preserve same-title full-refresh compatibility.
- [x] Add regression coverage for localized title changes, partial refreshes, complete same-title refreshes, and unrelated structural replacements.
- [x] Add the focused regression class to the selective PR-check unit-test command.
- [x] Verify `DemoCatalogRepository` has no production or test callers.
- [x] Remove the unused demo catalog stub.
- [x] Inspect the branch diff for unrelated version, dependency, permission, trigger, signing, artifact, binary, generated-file, or secret changes.
- [x] Address and close the CodeRabbit stable-section-identity review thread.
- [ ] Complete the final-head PR Check workflow.
- [ ] Complete the final-head APK build and size-report workflows.
- [ ] Verify localized Home rendering on a device or emulator.

## Validation matrix

| Check | Required | Current state |
| --- | --- | --- |
| Unicode root cause reproduced by code inspection | Yes | Verified against the ASCII-only title filter and duplicate cap |
| Merge-key root cause reproduced by code inspection | Yes | Verified: previous and incoming shelves were independently keyed from localized `title` |
| Focused regression test added | Yes | Added and selected by the primary PR workflow |
| Stable merge-key review thread | Yes | CodeRabbit marked the finding addressed and resolved the thread |
| Standalone Kotlin normalization smoke check | Supporting | Passed for accented Latin, Arabic, Chinese, Japanese, Hindi, and Thai samples |
| `DemoCatalogRepository` reference search | Yes | Verified: no callers outside the deleted file |
| Workflow selector inspection | Yes | Verified: `HomeRefreshStabilityTest` is included in the existing selective unit-test command |
| Complete branch diff inspection | Yes | Focused Home identity implementation, regression tests, dead stub deletion, PR-test selector, and active-task record only |
| `python3 scripts/validate_agent_config.py` | Yes | Final-head CI pending |
| `./gradlew --no-daemon :app:lintRelease` | Yes | Final-head CI pending |
| Focused unit tests | Yes | Final-head CI pending |
| `./gradlew --no-daemon --no-configuration-cache assembleRelease` | Yes | Final-head CI pending |
| F-Droid release compile | Yes | Final-head CI pending |
| Workflow Duplicates Guard | Yes | Passed on previous heads; final-head run pending |
| SonarQube Cloud quality gate | Supporting | Passed on a previous head with zero new issues and zero security hotspots; final-head analysis pending |
| Localized device/emulator Home check | Manual | Not performed |
| Playback, Android Auto, notification, PiP | No | Not affected by this phase |
| Desktop build and Windows checks | No | Desktop files are unchanged |
| Merge or release | Owner action | Not authorized by this phase |

## Behavior preserved

- Title-based duplicate limiting remains separate from merge identity and retains Unicode-aware normalization.
- Same-title catalog refreshes can replace all tracks without being misclassified as structural.
- Localized title changes and partial refreshes update the visible section while structural changes are frozen.
- Truly unrelated replacements are still deferred until structural changes are allowed.
- Existing Home limits, ordering, track metadata merge behavior, cached-content retention, and Latin accent folding remain unchanged.
- No user-visible strings or locale catalogs are modified.
- Playback, MediaSession, notification, Android Auto, queue, downloads, favorites, playlists, history, settings, backups, and Room schemas are untouched.
- The PR workflow keeps its current event filters, permissions, concurrency, Java/SDK setup, signing strategy, lint, release build, F-Droid build, reports, and artifact handling.
- Android and Desktop versions, artifacts, tags, signing, packaging, and release workflows remain independent and unchanged.

## Rollback boundary

Revert the Home title/merge identity changes, their focused tests, the PR-check selector, the demo-stub deletion, and this active-task update as one reviewable pull request. No migration or durable-data rollback is required.

## Update rule

Record test, CI, review, device, merge, and release status only from direct evidence. Replace this phase when a new reviewable task begins instead of accumulating unrelated work.
