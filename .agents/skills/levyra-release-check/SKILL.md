---
name: levyra-release-check
description: Validate Levyra before merge or release, including Android and Desktop versions, signing, builds, packaging, artifacts, checksums, workflows, secrets, and truthful manual verification.
---

# Levyra release validation workflow

## Required context

1. Read the root `AGENTS.md`, `app/AGENTS.md`, `desktop/AGENTS.md`, and `.github/AGENTS.md` as applicable.
2. Read `.claude/skills/levyra-release-check/SKILL.md` and `.claude/rules/testing-release.md`.
3. Inspect the complete diff, version files, build configuration, signing inputs, release workflows, artifact paths, and release notes.

## Boundaries

Do not change versions, publish, tag, merge, upload, or release unless those actions are separately and explicitly authorized.

Android and Desktop versioning are independent:

- Android uses `levyraVersionName` and `levyraVersionCode` in the existing Android configuration.
- Desktop uses `desktop/version.properties` and `desktop-v<version>` tags.

Never update one platform's version merely because the other platform is releasing.

## Validation checklist

- inspect repository status/diff for unrelated edits, conflict markers, secrets, keystores, APKs, ZIPs, generated output, and private configuration;
- verify version changes are intentional, monotonic, and limited to the requested platform;
- run focused tests first;
- run the applicable Android or Desktop wrapper checks when the environment supports them;
- verify signing and API inputs are supplied only through approved local or CI mechanisms;
- compare workflow changes with existing Android release, Desktop release, F-Droid, extractor sync, duplicate guards, and artifact conventions;
- verify package names, filenames, checksums, update metadata, release tags, and expected output paths;
- distinguish CI evidence, local evidence, emulator/device evidence, and unperformed checks.

## Reporting

List every command/check with its result. A blocked or skipped check is not a pass. Report manual playback, Android Auto, notification, PiP, Windows installer, update, protocol, media-key, and native VLC checks as unverified unless actually performed.
