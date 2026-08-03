<!--
Thank you for contributing to Levyra.
Keep this description focused and evidence-based. Remove guidance comments,
mark non-applicable checks honestly, and do not leave empty sections.
-->

## Overview

<!-- What problem does this PR solve, what approach was taken, and what changes for users? -->

**Problem**

-

**Solution**

-

**User impact**

-

## Change type

<!-- Select every item that applies. -->

- [ ] Bug fix
- [ ] New feature
- [ ] Performance improvement
- [ ] Refactor / technical debt
- [ ] UI / UX change
- [ ] Localization / accessibility
- [ ] Build / CI / release tooling
- [ ] Documentation / maintenance
- [ ] Breaking change

## Platforms and affected areas

**Platforms**

- [ ] Android
- [ ] Windows Desktop
- [ ] Shared extractor / infrastructure

**Areas**

- [ ] Playback / queue / audio DSP
- [ ] Stream extraction / catalog / networking
- [ ] Downloads / offline storage
- [ ] Library / database / preferences
- [ ] UI / Compose / navigation
- [ ] Lyrics / metadata
- [ ] Media session / Android Auto / system integration
- [ ] Localization / RTL / accessibility
- [ ] Build / packaging / release workflows
- [ ] Documentation

## Implementation notes

<!--
Explain the important technical decisions, trade-offs, migrations, lifecycle or
concurrency considerations, and anything reviewers should inspect carefully.
-->

-

## Validation

### Automated checks

- [ ] Android: `./gradlew --no-daemon :app:lintRelease :app:testReleaseUnitTest :app:assembleRelease`
- [ ] Desktop: `cd desktop && ./gradlew check assemble`
- [ ] Additional targeted tests were added or updated
- [ ] Not applicable — explained below

### Manual verification

<!-- Add one row per relevant environment or scenario. -->

| Platform / device | OS / version | Scenario tested | Result |
|---|---|---|---|
|  |  |  |  |

**Evidence**

<!-- Attach screenshots, recordings, logs, benchmark results, or before/after data where useful. -->

-

## Regression and compatibility review

- [ ] The affected user flow works from a clean launch
- [ ] Backgrounding, cancellation, retry, and error states were considered where relevant
- [ ] Playback, queue continuity, downloads, and offline behavior were checked where relevant
- [ ] Existing databases, preferences, cached data, and downloaded files remain compatible
- [ ] New or changed UI text is localized and contains no unintended hard-coded strings
- [ ] RTL layout and accessibility semantics were checked where relevant

**Known limitations or follow-up work**

-

## Risk and release impact

**Risk level:** Low / Medium / High

**Main risks**

-

**Rollback plan**

-

- [ ] No Android or Desktop version bump is required
- [ ] Required version changes are included and correct
- [ ] Database, preference, protocol, or file-format migrations are documented
- [ ] Release and packaging changes preserve the separate Android and Desktop release channels

## Final checklist

- [ ] The PR is focused and contains no unrelated changes
- [ ] The code follows the existing architecture and naming conventions
- [ ] Threading, resource cleanup, lifecycle, and cancellation were reviewed where relevant
- [ ] No secrets, keystores, generated APKs, installers, archives, or local-only files were committed
- [ ] Credits, notices, and licenses were updated for new external code or assets
- [ ] Documentation was updated when behavior, setup, architecture, or release steps changed
- [ ] CI is green, or every remaining failure is explained below

## Related issues

- Closes #
- Related to #
