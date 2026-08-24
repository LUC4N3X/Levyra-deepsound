# Levyra always-on agent guards

`../../docs/ai/ALWAYS_ON_AGENT_GUARDS.md` is mandatory for every Levyra engineering task.

This is not a skill route and must not depend on whether Claude decides a skill is relevant. The project lifecycle hooks enforce current-file-before-mutation, scoped `AGENTS.md` injection around edits, read freshness for whole-file replacement, compaction/resume re-anchoring, comment-slop feedback, and completion auditing.

Specialized `levyra-*` skills remain additional task-specific procedures. They never replace or disable the always-on guard contract.
