<!--
Thank you for contributing to Levyra.
Keep the description concise, remove guidance comments, and use "N/A" where a section does not apply.
-->

## Summary

<!-- Explain what changed, why it was needed, and the user-facing result in a few clear sentences. -->


## What changed

<!-- List the most important implementation changes. Keep this focused on the actual diff. -->

- 

## Type of change

- [ ] Bug fix
- [ ] New feature
- [ ] Performance improvement
- [ ] Refactor or maintenance
- [ ] UI or UX change
- [ ] Localization or accessibility
- [ ] Build, CI, packaging, or release change
- [ ] Documentation
- [ ] Breaking change

## Scope

- **Platform:** Android / Windows Desktop / Shared infrastructure
- **Area:** Player / Streaming / Downloads / Library / UI / Lyrics / Localization / System integration / Build and release / Documentation

## Validation

### Automated checks

- [ ] Android: `./gradlew --no-daemon :app:lintRelease :app:testReleaseUnitTest :app:assembleRelease`
- [ ] Desktop: `cd desktop && ./gradlew check assemble`
- [ ] Targeted tests were added or updated
- [ ] Not applicable, with the reason explained below

### Manual testing

| Environment | Scenario | Result |
|---|---|---|
|  |  |  |

<!-- Add screenshots, recordings, logs, or benchmark results when they make the change easier to verify. -->

## Risk and compatibility

- **Risk level:** Low / Medium / High
- **Compatibility or migration notes:** None
- **Known limitations:** None
- **Rollback plan:** Revert this PR

<!-- Consider databases, preferences, downloads, cached data, protocols, background work, cancellation, lifecycle, RTL, and accessibility where relevant. -->

## Reviewer notes

<!-- Highlight files, decisions, trade-offs, or edge cases that deserve closer review. -->

- 

## Release note

<!-- Write one short user-facing sentence, or "None" for internal-only changes. -->

None

## Related issues

- Closes #
- Related to #

## Checklist

- [ ] The PR is focused and contains no unrelated changes
- [ ] The implementation follows the existing architecture and naming conventions
- [ ] Threading, lifecycle, cancellation, and resource cleanup were reviewed where relevant
- [ ] Tests, documentation, localization, and screenshots were updated where required
- [ ] No secrets, keystores, generated packages, archives, or local-only files were committed
- [ ] Android and Desktop versioning and release channels remain independent
- [ ] CI is green, or every remaining failure is explained in this PR
