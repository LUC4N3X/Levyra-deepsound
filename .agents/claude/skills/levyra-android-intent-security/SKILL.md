---
name: Levyra Android Intent Security
description: Automatically use for Levyra Android Intent, deep-link, PendingIntent, exported component, receiver, service, provider, URI-grant, FileProvider, caller-verification, or onNewIntent security work.
---

# Levyra Android Intent security bridge

Read and follow the canonical workflow at:

```text
.agents/skills/levyra-android-intent-security/SKILL.md
```

Always pair it with `levyra-security-review` and the affected Android domain
skill. The canonical skill owns the component-exposure, nested-Intent,
PendingIntent, deep-link, provider/URI-grant, caller-verification, validation,
and provenance rules.

Do not copy generic Android samples into Levyra or treat an exported component,
mutable PendingIntent, or nested Intent as a confirmed vulnerability without a
reachable attacker-controlled path and direct evidence.
