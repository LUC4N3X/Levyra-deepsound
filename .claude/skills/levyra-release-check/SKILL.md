---
name: Levyra Release Check
description: Automatically use for Levyra emulator/device runtime verification, pre-merge/pre-release validation, versionName/versionCode, signing, APK/package output, release workflows, release-note authoring, artifacts, checksums, and truthful manual evidence.
allowed-tools: Read, Grep, Glob, Bash
---

# Levyra validation bridge

Read and follow the canonical workflow at:

```text
.agents/skills/levyra-release-check/SKILL.md
```

That file owns semantic emulator/device validation, pre-merge/release checks, version/signing boundaries, artifact verification, release-note authoring, Humanizer routing, and truthful evidence reporting.

When preparing or reviewing release notes, also read and apply:

```text
docs/ai/RELEASE_NOTES_STYLE.md
.agents/skills/levyra-humanizer/SKILL.md
```

Draft the release from current Levyra evidence first, then apply `levyra-humanizer` in embedded mode as the final prose pass. Humanizer may improve wording and structure but must not alter required headings, version fields, Markdown targets, commands, hashes, artifact names, validation state, compatibility claims, privacy claims, migration requirements, or any other factual claim. Compare the final humanized text against the factual draft before publication.

Release notes must read like polished human product writing: lead with user value, group major changes into clear editorial sections, keep engineering plumbing subordinate to the story, and reserve exact evidence for Validation. Preserve every exact heading and version field required by the current release workflows. The sparse `✦` section marker is an intentional Levyra house-style exception and may remain when the surrounding prose passes Humanizer naturally.

Do not publish, tag, release, merge, or change version values unless the owner separately authorizes that exact action. Prefer semantic UI targets over raw coordinates for Android runtime checks and report every unperformed device-only check as unverified.