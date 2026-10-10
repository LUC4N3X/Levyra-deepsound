# Levyra Release Notes Style

This document is the editorial contract for Android and Desktop release notes.
It complements the release workflows and version checks; it does not replace
truthful validation, signing, packaging, or publication evidence.

## Goal

Release notes should make a real person want to try the new version while still
being useful to developers and existing users.

Write them like a polished product release from a serious open-source project,
not like a generated commit summary, an internal engineering report, or a dump
of merged pull requests.

The reader should quickly understand:

1. what is meaningfully better;
2. why the changes matter in daily use;
3. what remains compatible and private;
4. what was actually validated;
5. whether any migration or action is required.

## Voice

Use natural, confident, professional English.

The tone should be human and editorial: clear enough for ordinary users, precise
enough for technical readers, and restrained enough to remain credible.

Prefer:

- concrete user outcomes over implementation jargon;
- short paragraphs mixed with focused bullets;
- active language such as `Levyra now...`, `You can now...`, or `Search is now better at...`;
- explanations of why a feature matters, not only what class or subsystem changed;
- technical details only when they explain reliability, compatibility, privacy,
  performance, or a non-obvious design choice.

Avoid:

- robotic phrases such as `This release contains the following changes`;
- opening with commit counts, hashes, PR numbers, file names, or build plumbing;
- one giant bullet list of unrelated changes;
- marketing exaggeration, fake superlatives, or claims unsupported by evidence;
- repeating the same feature in multiple sections;
- exposing internal secrets, signing material, private URLs, or sensitive operational details;
- claiming a build, device test, Android Auto test, signer verification, or release publication that did not actually happen.

## Required shape

Keep the machine-required release sections that the current repository workflows
validate exactly:

- `## Highlights`
- `## Validation`
- `## Versioning`
- `## Upgrade notes`
- `## Final note`

Between `Highlights` and `Validation`, add user-facing editorial sections for the
features that actually matter in that release. Prefer concise headings using the
Levyra marker:

```text
## ✦ A more personal Levyra
## ✦ Playback that remembers where you were
## ✦ Living Artwork
## ✦ Better lyric sharing
## ✦ Android Auto improvements
```

The exact headings must follow the real release. Do not force a section when
there is nothing meaningful to say.

## Opening

Start with:

```text
# Levyra <version>
```

Then write two to four short paragraphs before or inside `## Highlights` that
summarize the release in human terms.

For a large release, explain the direction of the update rather than leading
with the number of commits. Commit ranges and exact SHAs belong in `Validation`
when they are useful evidence.

Good opening characteristics:

- names the most important improvements;
- says what feels different for the user;
- avoids pretending every release is revolutionary;
- establishes one coherent theme for the version.

## Feature sections

For each major area:

1. lead with the user-facing change;
2. explain the practical benefit in one or two paragraphs;
3. add bullets only when they improve scanning;
4. mention the underlying architecture only when it explains why the behavior is safer or more reliable.

A strong section should answer both `what changed?` and `why should I care?`.

For example, do not stop at:

```text
Added a service-owned sleep timer.
```

Prefer the fuller idea:

```text
Levyra finally has a proper sleep timer. Because the timer belongs to the
playback service, it keeps working even after the player screen is closed.
```

Do not invent behavior merely to make the prose sound better.

## Technical material

Keep internal implementation detail subordinate to the product story.

Good technical detail includes:

- a non-destructive migration and what it preserves;
- why playback resume is now more reliable;
- why a local statistic remains private;
- how a fallback preserves older Android compatibility;
- why a system-level control continues working outside a screen lifecycle.

Move raw commit ranges, exact SHAs, regression-test inventory, signer checks,
workflow responsibilities, and unperformed manual checks to `## Validation`.

## Privacy and compatibility

When relevant, state privacy and compatibility plainly rather than as generic
marketing.

Examples:

- personal listening data remains on device;
- no account, advertising identifier, analytics, or tracking system was added;
- existing favorites, playlists, downloads, queue, history, settings, playback
  state, and backups remain compatible;
- Android and Desktop versions remain independently versioned.

Only include claims that are true for the current release.

## Validation

This section is factual, not promotional.

Separate clearly:

- checks actually run;
- CI evidence;
- emulator or physical-device evidence;
- Android Auto or other platform-specific evidence;
- checks that remain unperformed or blocked.

Do not convert the existence of tests into a claim that the final signed artifact
was tested. Do not reuse old physical-device evidence as if it were a new run.

## Versioning

Keep the exact canonical values required by the release workflow:

```text
- Version name: `<version>`
- Version code: `<versionCode>`
```

For Android-only releases, explicitly state that Levyra Desktop remains
independently versioned when applicable. Do the inverse for Desktop-only
releases.

## Upgrade notes

Tell existing users what they need to do.

Prefer a direct `No manual migration is required` when that is true, then explain
what is preserved and any channel-specific behavior such as GitHub versus
F-Droid.

If a manual migration is required, put it first and make the steps impossible to
miss.

## Final note

End with a concise editorial takeaway.

The final paragraph should describe what the release means for Levyra as a
product, not restate file names, version wiring, or CI implementation.

For larger releases it is appropriate to use short rhythmic statements, for
example:

```text
Discovery is more personal.
Search is more trustworthy.
Playback is harder to break.
Personal data stays local.
```

Keep this grounded in the actual release.

## Fastlane changelog

Fastlane changelogs are intentionally much shorter than the full GitHub release
body.

Summarize the most important user-visible improvements in one compact paragraph
or a few short lines. Do not copy the entire release notes document into
Fastlane metadata.

## Final authoring check

Before a release is published, verify that the notes:

- read naturally from top to bottom;
- explain benefits before internals;
- have no duplicate or filler sections;
- contain no unfinished placeholders;
- preserve the exact workflow-required headings and version values;
- distinguish evidence from assumptions;
- make no unsupported validation or publication claims;
- match the current repository rather than remembered or planned behavior;
- feel written for people rather than generated from `git log`.
