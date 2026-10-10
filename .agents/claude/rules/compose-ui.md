---
paths:
  - "app/src/main/java/com/luc4n3x/levyra/ui/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/viewmodel/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/MainActivity.kt"
---

# Compose UI

- Keep composables declarative. Put network, database, file, extraction, and player orchestration in ViewModels, controllers, or repositories.
- Preserve unidirectional data flow and immutable UI state.
- Use stable unique keys in lazy layouts and avoid deriving identity from display text alone.
- Use `remember`, `derivedStateOf`, and reduced screen projections to avoid broad recomposition from playback ticks.
- Use `LaunchedEffect` and `DisposableEffect` with correct keys and deterministic cleanup.
- Keep real cached content visible during refresh; do not replace usable content with shimmer.
- Provide content descriptions, sensible touch targets, readable contrast, and RTL-safe layout where applicable.
- Respect animation preferences, reduced-resource devices, battery saver, and lifecycle state.
- Avoid blank frames: render static artwork/content first and layer optional animation only after it is ready.
- Do not embed user-visible strings directly when the localization catalog should own them.
