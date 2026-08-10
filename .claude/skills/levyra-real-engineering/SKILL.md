---
name: levyra-real-engineering
description: Use for non-trivial Levyra features, architectural changes, unclear bugs, multi-step work, or requests where requirements and implementation need to be separated before editing. Skip for tiny unambiguous fixes.
---

# Levyra real-engineering bridge

Read and follow the canonical adapter at:

```text
.agents/skills/levyra-real-engineering/SKILL.md
```

That file owns Levyra-specific precedence, scope, validation, and publication rules.

When the `mattpocock-skills` plugin is available, invoke the exact upstream stage skill required by the adapter (`grill-with-docs`, `wayfinder`, `to-spec`, `to-tickets`, `implement`, `tdd`, `diagnosing-bugs`, `code-review`, or `domain-modeling`) rather than recreating its procedure from memory.

Do not run the full workflow for a small, already-unambiguous change. External skills supplement Levyra and never override `AGENTS.md`, current architecture, focused `levyra-*` domain skills, tests, or owner-controlled publication.
