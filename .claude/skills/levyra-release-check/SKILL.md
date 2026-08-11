---
name: Levyra Release Check
description: Automatically use for Levyra emulator/device runtime verification, pre-merge/pre-release validation, versionName/versionCode, signing, APK/package output, release workflows, artifacts, checksums, and truthful manual evidence.
allowed-tools: Read, Grep, Glob, Bash
---

# Levyra validation bridge

Read and follow the canonical workflow at:

```text
.agents/skills/levyra-release-check/SKILL.md
```

That file owns semantic emulator/device validation, pre-merge/release checks, version/signing boundaries, artifact verification, and truthful evidence reporting.

Do not publish, tag, release, merge, or change version values unless the owner separately authorizes that exact action. Prefer semantic UI targets over raw coordinates for Android runtime checks and report every unperformed device-only check as unverified.
