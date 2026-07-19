---
name: Levyra Compose Change
description: Implements or reviews Levyra Compose screens, state projections, animation, lifecycle, accessibility, and localization changes.
---

# Levyra Compose workflow

1. Read `.claude/rules/compose-ui.md` and the affected composable, state, ViewModel, theme, and string files.
2. Keep I/O and orchestration outside composables.
3. Minimize the state observed by each screen and preserve stable lazy-list keys.
4. Use correctly keyed effects and deterministic cleanup for listeners, callbacks, players, receivers, and surfaces.
5. Keep cached/real content visible during refresh and provide a non-animated fallback.
6. Respect animation preference, lifecycle, low-RAM, battery/data saver, RTL, accessibility, and touch targets.
7. Add/update localization entries instead of hardcoding user-facing text.
8. Check recomposition scope and test long text, state restoration, rapid navigation, and background/foreground behavior when applicable.
