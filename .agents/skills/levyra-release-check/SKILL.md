---
name: levyra-release-check
description: Automatically use for Levyra pre-merge/pre-release validation, Android emulator or physical-device runtime verification, versions, signing, builds, packaging, artifacts, checksums, workflows, secrets, and truthful manual evidence.
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