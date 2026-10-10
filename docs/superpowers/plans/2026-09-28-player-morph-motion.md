# Player Morph Motion Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make mini-player open/close motion easier to read without changing playback, navigation, gestures, layout, or the reduced-motion contract.

**Architecture:** Keep the existing `Animatable<Float>` expansion and artwork morph pipeline. Add softer player-specific spring tokens behind the existing `LevyraPlayerDesign` expansion/collapse delegates, then stretch the current alpha hand-off across more of the expansion progress. This lets the established player call sites pick up the new motion without changing orchestration or state ownership.

**Tech Stack:** Kotlin, Jetpack Compose animation, JUnit.

**Spec:** Owner request in the current task.

## Global Constraints

- Android only.
- Preserve playback, queue, navigation and all existing player gestures.
- Reuse the current `PlayerExpansion`, `PlayerMorphAnchors`, `LevyraMotion`, and `LevyraPlayerDesign` owners.
- Respect `animationsEnabled`; no new dependency or second animation engine.
- Do not change Android version values or unrelated UI.

## Review Focus

- Existing player expansion and collapse paths receive softer dedicated motion without changing their state flow.
- Mini-player chrome and full-player surface remain legible during the middle of the morph.
- Expansion remains reversible and bounded during gestures.
- Artwork morph endpoints and gesture commit thresholds remain unchanged.
- Reduced motion behavior remains unchanged.

---

### Task 1: Lock the slower player transition contract

**Files:**
- Modify: `app/src/test/java/com/luc4n3x/levyra/ui/theme/LevyraMotionTest.kt`
- Modify: `app/src/test/java/com/luc4n3x/levyra/ui/player/PlayerExpansionTest.kt`

**Interfaces:**
- Consumes: current player motion and expansion helpers.
- Produces: tests for dedicated soft player springs and longer alpha hand-off.

- [ ] Write tests that require player-specific open/close springs to be softer than generic expand/collapse springs.
- [ ] Write tests that require mini chrome and full-player surface fades to stay active deeper into the transition.
- [ ] Run the focused tests when an executable Android build environment is available.

### Task 2: Implement player-specific settling

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/theme/LevyraMotion.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/theme/PlayerDesign.kt`

**Interfaces:**
- Produces: `LevyraMotion.playerExpand`, `LevyraMotion.playerCollapse`, and player-scoped spring delegates in `LevyraPlayerDesign`.

- [ ] Add bounded, non-bouncy player-specific spring tokens.
- [ ] Route the existing player design expansion/collapse delegates through the softer spring tokens.
- [ ] Preserve the current reduced-motion and state-orchestration behavior.
- [ ] Run focused tests when an executable Android build environment is available.

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
- [ ] Run repository CI and report local quality gates as unrun when the local build environment is unavailable.
- [ ] Open a dedicated draft PR with concise human-written notes.
