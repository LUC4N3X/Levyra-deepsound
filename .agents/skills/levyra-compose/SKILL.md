---
name: levyra-compose
description: Implement, debug, or review Levyra Jetpack Compose screens, state projections, navigation, animation, lifecycle, accessibility, performance, RTL, localization, edge-to-edge, adaptive layout, and screenshot-validation changes.
---

# Levyra Compose workflow

## Required context

1. Read the root `AGENTS.md` and `app/AGENTS.md`.
2. Read `docs/ARCHITECTURE.md`.
3. Read `.agents/claude/rules/compose-ui.md`, `.agents/claude/rules/localization.md`, and `.agents/claude/rules/architecture.md`.
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

## Android platform UI guardrails

Adapt the stable, useful parts of Google's official Android agent guidance to Levyra's current Compose architecture. Do not force an architecture or dependency migration merely because an upstream recipe assumes one.

### Edge-to-edge and system insets

- Treat edge-to-edge as a screen/layout correctness concern, not decorative polish.
- Before adding padding, trace which parent already consumes status-bar, navigation-bar, cutout, caption-bar, or IME insets. Apply each inset once; double-padding is a regression.
- Prefer existing Material 3/`Scaffold` inset handling and pass `PaddingValues` into scrollable content where that preserves drawing behind system bars. Avoid padding a lazy-list parent when `contentPadding` is the correct ownership point.
- For text-entry screens, verify the keyboard does not obscure the focused field and do not stack `imePadding()` on top of an ancestor that already supplies/consumes IME insets.
- Keep critical controls tappable outside unsafe system areas and verify status/navigation-bar icon legibility against the actual light/dark surface.
- Full-screen dialogs and media surfaces need explicit inset review; do not assume activity-level handling automatically fixes nested windows.

### Adaptive layouts and larger form factors

- Verify the current navigation architecture before introducing adaptive-navigation APIs. Do not migrate Levyra to Navigation 3, multi-pane scenes, Grid/FlexBox, or other experimental APIs as collateral work.
- Preserve media-first full-screen behavior where detail content genuinely benefits from the space; do not introduce list-detail solely because a device is wide.
- For grids/lists, prefer the existing stable adaptive pattern already used in Levyra. When changing column behavior, base it on available width and a usable minimum item width rather than device-name checks.
- Check phone portrait/landscape plus at least one larger width when a layout change can materially alter composition, navigation, artwork sizing, or controls.
- Pointer/keyboard/desktop-style input support should reuse existing platform behavior and semantics; do not add a second interaction model unless the affected surface needs it.

### Experimental Compose APIs

Google's current Android skills include experimental Compose Styles, Grid, FlexBox, MediaQuery, and related alpha/beta APIs. They are references, not Levyra defaults.

- Do not upgrade Compose or opt the project into an experimental API merely to modernize code.
- Do not migrate custom components to the experimental Compose Styles API unless the owner explicitly asks for that experiment and the compatibility/maintenance tradeoff is reviewed.
- If an experimental API is explicitly approved, isolate the opt-in, preserve the visual/behavior baseline, and add focused validation so reverting it is straightforward.

## Compose performance diagnosis

For jank, slow scrolling, frame drops, or suspected recomposition problems, measure before redesigning the UI.

1. Start with the affected code and trace the state reads that can invalidate it.
2. Inspect stable identity in lazy layouts, broad state collection, work performed during composition, allocation churn, image sizing/loading, subcomposition, and expensive layout/intrinsic measurement.
3. Compare against a nearby smooth screen or component when one exists before introducing a new pattern.
4. If code inspection is not enough, gather direct evidence with the narrowest useful tool: Layout Inspector/recomposition counts, System Trace or Perfetto for frame timing, and the existing benchmark/baseline-profile infrastructure when a repeatable metric is appropriate.
5. Diagnose release-like behavior for performance claims. Debug-only jank or timings are evidence about the debug build, not proof of release performance.
6. Change one material performance variable at a time and remeasure. Do not cargo-cult `remember`, `derivedStateOf`, `@Stable`, `@Immutable`, persistent collections, or a new dependency without showing that it fixes the measured invalidation/allocation/layout problem.

Performance findings must distinguish code-review suspicion from measured evidence. Report the before/after metric when one was actually collected. Load `levyra-android-performance` when the investigation needs Perfetto/System Trace, thread-state, blocking, memory, I/O, power, startup, or other cross-thread/runtime evidence.

## Accessibility review

For changed interactive UI, verify semantics as behavior rather than merely checking that a property exists.

- Actionable controls should expose a meaningful accessible label describing the action or purpose; purely decorative images/icons should not create redundant announcements.
- Preserve logical focus/traversal order and group descendants only when the combined announcement is clearer than separate controls.
- Expose selected/checked/expanded/loading or other custom state through standard semantics or an appropriate state description when the visual state would otherwise be hidden from assistive technology.
- Keep interactive touch targets at least 48dp where platform behavior does not already enforce the minimum.
- Use heading semantics for real section headings when they improve navigation, not for decorative typography.
- Validate large font scale, long translations, RTL, contrast, and TalkBack/screen-reader behavior when the change affects them.

Do not add duplicate spoken labels around controls that already expose correct semantics through their standard component behavior.

## Screenshot and UI validation

Use screenshot tests as visual-regression evidence, not as behavior tests.

- Reuse Levyra's existing screenshot/preview test infrastructure if present before installing a new framework.
- For a screen-level visual change, include representative compact and expanded widths when the layout is adaptive, plus dark/light or other supported themes when they materially change the surface.
- Include large font scale for typography/layout-sensitive work and a relevant RTL locale when directionality can change the result.
- Cover materially different UI states such as loading, empty, error, offline, or selected state when the change affects them.
- Do not update golden/reference images blindly. Inspect diffs and make sure the visual change is intentional before accepting a new baseline.
- Use Compose UI behavior tests for interactions, navigation, restoration, and semantics; a pixel match does not prove behavior.

## Focused upstream reviews

On Android UI work, the product direction is Material 3 Expressive. Use Levyra components and semantic tokens first. At the later visual compliance phase, load the official upstream `material-3` skill from `hamen/material-3-skill` and relevant reference files only. For implementation involving Kotlin/Compose or state, consult the matching `kotlin-footguns` index and only implicated trap files in a separate review phase (usually `compose-screens-footguns`, `compose-visuals-footguns`, or `state-and-background-footguns`). Keep no more than two skill bodies active at once. Use `docs/ai/UPSTREAM_ANDROID_SKILLS.md` for setup, paths, and fallbacks.

## Regression guard: localized settings, skin gestures and compact search (PR #1156)

Preserve the user-visible PR #1156 fixes when changing Smart Listening/Gentle Wake, player skins, responsive rows, animation or Default search.

- **Reachable controls:** Validate narrow phones, longer translations (including Russian), font scaling and accessibility. Keep the time display and all four time-step buttons visible with 48dp touch targets; do not squeeze them into an overflowing single row.
- **Adaptive options:** Keep all music-source, crescendo-duration and shake-sensitivity options visible. Prefer FlowRow or a suitable adaptive layout to horizontally clipped content behind card gutters. Keep the tone card within available width and format times/durations using localized resources.
- **Actual gesture surface:** Canvas drawing does not mount gestures automatically. In custom skins, verify the real shared swipe hit region and pointer handling; do not intercept header actions or change gesture thresholds, settings or behavior of other skins.
- **Track animation continuity:** Key by track ID and cover state separately. Validate same-album track changes, notification/media-key changes, rapid transitions, late/missing art, no-mirror label swaps and disabled/reduced motion.
- **Default Search by lyrics:** Preserve the compact tool key in the suggestion strip rather than restoring an intrusive full-width row. Keep 48dp hit area, accessible action name, contrast and alignment with chips in dark/light/dynamic themes; leave other skins unchanged.

For affected UI, inspect compact-width renderings in English and Russian at large font scale, plus light/dark and relevant RTL cases. Use interaction and semantics tests for buttons, swipes and animation state; screenshots alone do not establish working interactions. Mark any unrun UI/device checks UNVERIFIED.

## Structural lessons

- `LevyraAppKt` sits at the JVM class-size limit; new code in `LevyraApp.kt` can fail with "Class too large". Put new composables and helpers in dedicated files and keep `LevyraApp.kt` edits to call sites.
- Content that changes parent branch (Settings vs dock, morph vs plain stack, portrait vs side-by-side) must be `movableContentOf` content (PR #1163 adds `rememberMovableSlot`), or its state and artwork reload on every switch.
- Navigation chrome changes geometry continuously through animated values such as `DockLink.attach`; never swap layouts or drop a link with a boolean in one frame.
- Artwork colors have one owner, `rememberArtworkPalette`. Surfaces that follow the current track use `retainPreviousUntilLoaded = true` and pass a blank key when fixed accents override artwork. Do not reimplement cache reads in a composable.
- Do not run continuous transforms (rotation, shape morph) on artwork during playback without a reason; they redraw every frame and clip the cover.

## Validation

Test recomposition-sensitive paths, state restoration, rapid navigation, background/foreground transitions, long translations, RTL, large font scale, empty/error states, edge-to-edge/IME ownership, relevant larger widths, and accessibility semantics when applicable. Run focused UI or ViewModel tests before broader Android checks.

For performance work, include direct profiling/benchmark evidence when available and state clearly when performance remains code-review-only. For accessibility-sensitive changes, distinguish static semantics review from an actual TalkBack/device check. For visual-regression work, distinguish reviewed screenshot diffs from merely regenerated baselines.
