# Player Morph Motion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make mini-player open/close motion easier to read without changing playback, navigation, gestures, layout, or the reduced-motion contract.

**Architecture:** Keep the existing `Animatable<Float>` expansion and artwork morph pipeline. Introduce player-specific soft spring tokens instead of slowing generic sheets, use the correct opening/closing spring for every settle path, and stretch the existing alpha hand-off across more of the expansion progress.

**Tech Stack:** Kotlin, Jetpack Compose animation, JUnit.

**Spec:** Owner request in the current task.

## Global Constraints

- Android only.
- Preserve playback, queue, navigation and all existing player gestures.
- Reuse the current `PlayerExpansion`, `PlayerMorphAnchors`, `LevyraMotion`, and `LevyraPlayerDesign` owners.
- Respect `animationsEnabled`; no new dependency or second animation engine.
- Do not change Android version values or unrelated UI.

## Review Focus

- Programmatic open and close use different player-specific springs.
- Drag/fling settle uses the same opening/closing motion language as taps/back.
- Mini-player chrome and full-player surface remain legible during the middle of the morph.
- Expansion remains reversible and bounded during gestures.
- Reduced motion still snaps instead of animating.

---

### Task 1: Lock the slower player transition contract

**Files:**
- Modify: `app/src/test/java/com/luc4n3x/levyra/ui/theme/LevyraMotionTest.kt`
- Modify: `app/src/test/java/com/luc4n3x/levyra/ui/player/PlayerExpansionTest.kt`

**Interfaces:**
- Consumes: current player motion and expansion helpers.
- Produces: failing tests for dedicated soft player springs and longer alpha hand-off.

- [ ] Write tests that require player-specific open/close springs to be softer than generic expand/collapse springs.
- [ ] Write tests that require mini chrome and full-player surface fades to stay active deeper into the transition.
- [ ] Run the focused tests and confirm they fail for the intended missing behavior.

### Task 2: Implement player-specific settling

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/theme/LevyraMotion.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/theme/PlayerDesign.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt`

**Interfaces:**
- Produces: `LevyraMotion.playerExpand`, `LevyraMotion.playerCollapse`, `LevyraPlayerDesign.playerExpandSpring()`, `LevyraPlayerDesign.playerCollapseSpring()`.

- [ ] Add bounded, non-bouncy player-specific spring tokens.
- [ ] Route programmatic and gesture settles through the matching opening/closing spring.
- [ ] Preserve `animationsEnabled` snapping behavior.
- [ ] Run focused tests and confirm green.

### Task 3: Stretch the visual hand-off

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/player/PlayerExpansion.kt`

**Interfaces:**
- Consumes: existing expansion fraction.
- Produces: longer mini-player chrome fade and full-player surface reveal without changing the artwork flight path.

- [ ] Extend the existing fade windows so the transition reads through the middle of the motion.
- [ ] Keep the artwork morph endpoint and gesture thresholds unchanged.
- [ ] Run focused tests, then repository quality gates when available.

### Task 4: Final review and PR

**Files:**
- Review the complete branch diff only.

- [ ] Confirm no playback/navigation/state ownership changed.
- [ ] Confirm no unrelated styling or dependency churn.
- [ ] Run `git diff --check` and the repository quality gate when available; report blocked checks truthfully.
- [ ] Open a dedicated draft PR with concise human-written notes.
