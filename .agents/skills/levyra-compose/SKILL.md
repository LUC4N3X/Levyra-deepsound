---
name: levyra-compose
description: Implement, debug, or review Levyra Jetpack Compose screens, state projections, navigation, animation, lifecycle, accessibility, performance, RTL, and localization changes.
---

# Levyra Compose workflow

## Required context

1. Read the root `AGENTS.md` and `app/AGENTS.md`.
2. Read `docs/ARCHITECTURE.md`.
3. Read `.claude/skills/levyra-compose/SKILL.md`, `.claude/rules/compose-ui.md`, `.claude/rules/localization.md`, and `.claude/rules/architecture.md`.
4. Inspect the affected composable, state holder, ViewModel, navigation path, theme, resources, and nearby tests.

## Change contract

Before editing, identify:

- the exact user-visible behavior;
- the smallest state slice the screen should observe;
- lifecycle and cleanup ownership;
- long-text, RTL, accessibility, restoration, loading, empty, error, and offline states;
- behavior that must remain unchanged on other screens and form factors.

## Guardrails

- Keep I/O, parsing, persistence, networking, and orchestration outside composables.
- Preserve unidirectional data flow and immutable UI state.
- Use stable lazy-list keys and avoid observing broader state than the screen needs.
- Key effects to their true dependencies and clean up callbacks, listeners, receivers, players, surfaces, and jobs deterministically.
- Keep cached or real content visible during refresh when safe; avoid blank-screen regressions.
- Respect reduced motion, lifecycle, low-RAM, battery/data saver, touch targets, contrast, focus, screen readers, and RTL.
- Add localization entries for every user-facing string; do not hardcode visible text.
- Avoid broad visual rewrites when a focused state or layout correction is sufficient.

## Validation

Test recomposition-sensitive paths, state restoration, rapid navigation, background/foreground transitions, long translations, RTL, large font scale, empty/error states, and accessibility semantics when applicable. Run focused UI or ViewModel tests before broader Android checks.
