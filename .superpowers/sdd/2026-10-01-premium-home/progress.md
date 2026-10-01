# SDD ledger — plan: docs/superpowers/plans/2026-10-01-premium-home.md

Pre-flight: Task 1 produces homePersonalOrbitColumns consumed by Task 2 — signatures match the plan and spec.
Pre-flight: Task 2 establishes the new Orbit shelf consumed by Task 3 rhythm changes — no interface conflict.
Pre-flight: Task 3 preserves Home Collections contract required by Task 4 — no conflict.
Ruling: connector execution uses the dedicated feature branch as the isolated workspace because this harness cannot create a local git worktree; temporary CI/ledger files will be removed before PR publication — cost if wrong: extra temporary files could leak into the PR, so final diff review must catch them.
