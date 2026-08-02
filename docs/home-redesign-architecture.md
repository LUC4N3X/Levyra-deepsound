# Home redesign architecture

- `ui/HomeExperience.kt` owns the artwork-led Home atmosphere and quick-access grid.
- `ui/theme/HomeDesign.kt` owns Home-specific visual tokens.
- `ui/LevyraApp.kt` keeps orchestration and existing shelves while delegating the new components.
- `DESIGN.md` defines application-wide visual guardrails.

The split intentionally avoids changes to repositories, state models, extraction, playback and persistence.
