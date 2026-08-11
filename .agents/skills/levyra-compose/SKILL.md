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

## Compose performance diagnosis

For jank, slow scrolling, frame drops, or suspected recomposition problems, measure before redesigning the UI.

1. Start with the affected code and trace the state reads that can invalidate it.
2. Inspect stable identity in lazy layouts, broad state collection, work performed during composition, allocation churn, image sizing/loading, subcomposition, and expensive layout/intrinsic measurement.
3. Compare against a nearby smooth screen or component when one exists before introducing a new pattern.
4. If code inspection is not enough, gather direct evidence with the narrowest useful tool: Layout Inspector/recomposition counts, System Trace or Perfetto for frame timing, and the existing benchmark/baseline-profile infrastructure when a repeatable metric is appropriate.
5. Diagnose release-like behavior for performance claims. Debug-only jank or timings are evidence about the debug build, not proof of release performance.
6. Change one material performance variable at a time and remeasure. Do not cargo-cult `remember`, `derivedStateOf`, `@Stable`, `@Immutable`, persistent collections, or a new dependency without showing that it fixes the measured invalidation/allocation/layout problem.

Performance findings must distinguish code-review suspicion from measured evidence. Report the before/after metric when one was actually collected.

## Accessibility review

For changed interactive UI, verify semantics as behavior rather than merely checking that a property exists.

- Actionable controls should expose a meaningful accessible label describing the action or purpose; purely decorative images/icons should not create redundant announcements.
- Preserve logical focus/traversal order and group descendants only when the combined announcement is clearer than separate controls.
- Expose selected/checked/expanded/loading or other custom state through standard semantics or an appropriate state description when the visual state would otherwise be hidden from assistive technology.
- Keep interactive touch targets at least 48dp where platform behavior does not already enforce the minimum.
- Use heading semantics for real section headings when they improve navigation, not for decorative typography.
- Validate large font scale, long translations, RTL, contrast, and TalkBack/screen-reader behavior when the change affects them.

Do not add duplicate spoken labels around controls that already expose correct semantics through their standard component behavior.

## Validation

Test recomposition-sensitive paths, state restoration, rapid navigation, background/foreground transitions, long translations, RTL, large font scale, empty/error states, and accessibility semantics when applicable. Run focused UI or ViewModel tests before broader Android checks.

For performance work, include direct profiling/benchmark evidence when available and state clearly when performance remains code-review-only. For accessibility-sensitive changes, distinguish static semantics review from an actual TalkBack/device check.