---
name: levyra-release-check
description: Automatically use for Levyra pre-merge/pre-release validation, Android emulator or physical-device runtime verification, versions, signing, builds, packaging, artifacts, checksums, workflows, secrets, release-note authoring, and truthful manual evidence.
---

# Levyra release validation workflow

## Required context

1. Read the root `AGENTS.md`, `app/AGENTS.md`, `desktop/AGENTS.md`, and `.github/AGENTS.md` as applicable.
2. Read `.claude/skills/levyra-release-check/SKILL.md` and `.claude/rules/testing-release.md`.
3. Read `docs/ai/RELEASE_NOTES_STYLE.md` whenever preparing, rewriting, reviewing, or publishing release notes.
4. Load `.agents/skills/levyra-humanizer/SKILL.md` whenever authoring or rewriting release notes, Fastlane changelogs, release descriptions, or other product-facing release prose.
5. Inspect the complete diff, version files, build configuration, signing inputs, release workflows, artifact paths, and release notes.

## Boundaries

Do not change versions, publish, tag, merge, upload, or release unless those actions are separately and explicitly authorized.

Android and Desktop versioning are independent:

- Android uses `levyraVersionName` and `levyraVersionCode` in the existing Android configuration.
- Desktop uses `desktop/version.properties` and `desktop-v<version>` tags.

Never update one platform's version merely because the other platform is releasing.

## Release-note authoring contract

Treat release notes as a product-facing artifact, not a generated commit summary.

Follow `docs/ai/RELEASE_NOTES_STYLE.md` as the canonical Levyra editorial contract, then apply `levyra-humanizer` in embedded mode as the mandatory final prose pass.

Authoring order:

1. build the factual release story from the current repository, merged changes, version wiring, and direct validation evidence;
2. write the release notes in Levyra's product voice, preserving every workflow-required heading and exact version field;
3. run `levyra-humanizer` over the user-facing prose to remove AI-sounding patterns, filler, inflated claims, repetitive structure, stock wording, forced punchlines, unnecessary bold, and chatbot artifacts;
4. compare the humanized result against the factual draft and restore any claim, caveat, version value, validation detail, Markdown target, or required heading that was accidentally changed or removed;
5. read the complete final notes once as a user and once as a release reviewer before publication.

The Humanizer pass may rewrite prose and paragraph structure, but it must not:

- invent or strengthen facts, validation, compatibility, privacy, migration, performance, signing, or publication claims;
- remove a real limitation, failed check, blocked check, unperformed test, or compatibility caveat;
- alter `# Levyra <version>`, `## Highlights`, `## Validation`, `## Versioning`, `## Upgrade notes`, `## Final note`, version name/code lines, code blocks, Markdown link targets, hashes, artifact names, commands, or machine-required text;
- turn technical evidence into marketing language;
- erase intentional Levyra house style merely because a generic Humanizer pattern would normally avoid it. In particular, the `✦` section marker is an allowed Levyra editorial convention when used sparingly and consistently.

Follow these editorial rules:

- write natural, professional English that sounds human rather than AI-generated;
- open with what the release changes for users, not commit counts, hashes, PR numbers, or file wiring;
- group meaningful changes into reader-facing sections and prefer `## ✦ ...` headings for those sections when the release benefits from them;
- explain both what changed and why it matters in ordinary use;
- keep low-level architecture detail only when it explains reliability, compatibility, privacy, performance, or a deliberate design choice;
- keep exact commit ranges, SHAs, test inventory, workflow responsibilities, and unperformed checks in `## Validation`;
- preserve the exact workflow-required headings `## Highlights`, `## Validation`, `## Versioning`, `## Upgrade notes`, and `## Final note`;
- keep Fastlane changelogs compact and user-facing instead of duplicating the full GitHub release body;
- never invent validation, device evidence, compatibility, privacy, migration, or publication claims to make the release sound stronger.

Before publication, read the complete release notes once as a user. Rewrite any section that still reads like `git log`, an internal engineering ticket, a mechanical list of commits, or generic AI product copy.

## Validation checklist

- inspect repository status/diff for unrelated edits, conflict markers, secrets, keystores, APKs, ZIPs, generated output, and private configuration;
- verify version changes are intentional, monotonic, and limited to the requested platform;
- verify release notes follow `docs/ai/RELEASE_NOTES_STYLE.md` and `levyra-humanizer`, remain truthful, and match the current repository;
- compare the pre-humanizer and post-humanizer factual claims when release prose was rewritten;
- run focused tests first;
- run the applicable Android or Desktop wrapper checks when the environment supports them;
- verify signing and API inputs are supplied only through approved local or CI mechanisms;
- compare workflow changes with existing Android release, Desktop release, F-Droid, extractor sync, duplicate guards, and artifact conventions;
- verify package names, filenames, checksums, update metadata, release tags, and expected output paths;
- distinguish CI evidence, local evidence, emulator/device evidence, and unperformed checks.

## Android emulator and device evidence

When an Android change needs runtime verification, prefer semantic, reproducible interaction over screen-coordinate automation.

- Confirm `adb`, the intended SDK/JDK, the connected target, and the exact app/build variant before testing.
- If more than one device or emulator is connected, select the serial explicitly; never assume the first target is correct.
- Separate build, install, launch, interaction, log inspection, and final-state evidence so a successful earlier step is not mistaken for end-to-end success.
- Prefer UI hierarchy, resource IDs, visible text, and accessibility/content descriptions for navigation and assertions. Use raw coordinates only when no stable semantic target exists, and record that limitation.
- Capture focused logcat for the app/process around the reproduction window. Compact repetitive success noise when useful, but keep complete failure, crash, stacktrace, security, and exact-reproduction evidence available raw.
- For playback changes, verify the exact affected path (song/audio mode, native-video mode, queue transition, background/foreground, notification, or other relevant behavior) rather than treating a successful app launch as playback validation.
- Emulator success does not prove physical-device, Android Auto, notification, Bluetooth/media-key, battery, OEM, or hardware-decoder behavior. Report each category separately.

This adopts the strongest idea from SimpMusic's Android emulator workflow: semantic navigation and structured evidence instead of brittle pixel automation, without vendoring its helper scripts into Levyra.

## Reporting

List every command/check with its result. A blocked or skipped check is not a pass. Report manual playback, Android Auto, notification, PiP, Windows installer, update, protocol, media-key, and native VLC checks as unverified unless actually performed.