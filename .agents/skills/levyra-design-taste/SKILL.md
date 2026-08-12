---
name: levyra-design-taste
description: Automatically use together with the matching Levyra UI skill for any visual redesign, UI polish, visual hierarchy, spacing, typography, color, shape, motion, screenshot/reference recreation, or request to make Levyra feel more premium, modern, distinctive, cohesive, or less AI-generated. Applies to Android Compose and Desktop Compose Multiplatform product UI.
---

# Levyra design taste

## Purpose

This skill is Levyra's product-design quality gate. It exists to prevent technically correct UI from becoming generic, templated, over-decorated, inconsistent, or recognizably AI-generated.

It does not replace `levyra-compose`, `levyra-desktop`, Material guidance, accessibility, performance work, localization, lifecycle rules, or the repository architecture. Load it alongside the matching domain skill.

Repository precedence remains:

1. root and nearest `AGENTS.md` files;
2. approved requirements and active planning;
3. current architecture and implementation;
4. matching Levyra domain skills;
5. this design-quality layer;
6. external references and inspiration.

If visual polish conflicts with playback reliability, responsiveness, accessibility, user choices, battery/resource constraints, or established architecture, the visual idea loses.

## Activation

Load this skill automatically when the task includes any of the following:

- redesigning or visually polishing a Levyra screen;
- requests such as "più bella", "più professionale", "premium", "modern", "clean", "cinematic", "minimal", "less generic", or "less AI";
- changing hierarchy, spacing, typography, color, shape, elevation, icon treatment, artwork treatment, navigation presentation, or motion;
- using screenshots, mockups, another application, or a design reference as inspiration;
- introducing or revising a reusable visual component or visual language;
- reviewing whether a UI change looks coherent with Levyra.

For Android also load `levyra-compose`. For Desktop also load `levyra-desktop`. Load performance, motion-artwork, security, or real-engineering skills when those concerns are also present.

Do not load this skill for a purely non-visual bug fix unless the proposed fix changes visible UI behavior.

## 1. Read the surface before designing

Before editing, write a short internal design read covering:

- **surface**: Home, Search, Library, Artist, Album, Now Playing, Lyrics, Settings, onboarding, dialog, mini player, Desktop window, etc.;
- **primary job**: the one thing the user needs to do or understand there;
- **visual role**: utility, discovery, immersive playback, management, confirmation, empty/error state;
- **existing language**: theme tokens, typography, shapes, artwork, navigation, component patterns, spacing rhythm;
- **preservation constraints**: behavior, gestures, information architecture, state, accessibility, localization, playback and navigation that must not change;
- **reference intent**: if a screenshot/app is supplied, identify which principles are useful rather than copying its surface literally.

Do not jump directly from "make it nicer" to a default aesthetic.

## 2. Use three design dials

Choose values from 1 to 10 before a material redesign. They are reasoning aids, not runtime settings.

- `VISUAL_VARIANCE`: 1 = strict utility and symmetry, 10 = highly expressive composition.
- `MOTION_INTENSITY`: 1 = nearly static, 10 = cinematic and interaction-heavy.
- `INFORMATION_DENSITY`: 1 = sparse/immersive, 10 = compact/operational.

Useful Levyra baselines:

| Surface | Variance | Motion | Density |
| --- | ---: | ---: | ---: |
| Settings / management | 3-4 | 1-3 | 6-8 |
| Library / Search | 4-5 | 2-4 | 5-7 |
| Home / discovery | 5-6 | 3-5 | 5-6 |
| Artist / Album | 5-7 | 3-5 | 4-6 |
| Now Playing | 6-8 | 4-7 | 3-5 |
| Lyrics / immersive mode | 6-8 | 4-7 | 2-4 |
| Onboarding / empty state | 5-7 | 3-5 | 2-4 |

Infer the values from the current product and the request. Do not maximize all three. A settings screen should not become a showcase; Now Playing should not become a dense control panel.

## 3. Preserve Levyra before adding novelty

Treat existing screens as redesigns unless the owner explicitly asks for a visual reset.

Audit first:

- existing color and typography tokens;
- spacing and radius conventions;
- icon family and control treatment;
- component reuse opportunities;
- navigation and information hierarchy;
- artwork behavior and loading fallback;
- touch, keyboard, focus and accessibility behavior;
- large-font, long-translation and RTL behavior;
- performance-sensitive composition and scrolling paths.

Prefer targeted evolution in this order:

1. hierarchy and content priority;
2. spacing and alignment rhythm;
3. typography scale/weight;
4. color/elevation/shape consistency;
5. component composition;
6. motion and decorative treatment;
7. full layout replacement only when the existing structure is the actual problem.

Never silently change navigation structure, gestures, primary actions, playback controls, semantic meaning, or user-visible behavior just to obtain a cleaner screenshot.

## 4. Anti-AI-slop rules

Do not fall back to common generator defaults unless the current Levyra language and the task justify them.

Avoid:

- glassmorphism on every container;
- arbitrary purple/blue glow or mesh gradients;
- rows of interchangeable rounded cards with no hierarchy;
- excessive pills, badges and chips used as decoration;
- giant whitespace that reduces useful music content without improving focus;
- random corner radii, opacity values, spacing values, gradients or shadows outside the established token system;
- duplicated headings, subtitles, helper labels or CTAs that say the same thing;
- motion whose only purpose is to look expensive;
- decorative blur, particles, parallax or scaling on scrolling lists without measured performance headroom;
- replacing real cached content with shimmer during ordinary refresh;
- creating a new component when an existing Levyra component can be evolved coherently;
- copying another application's brand identity, exact layout, proprietary artwork treatment or signature interaction.

A distinctive result comes from stronger hierarchy, proportion, rhythm, artwork use, typography, restraint and interaction feedback, not from stacking visual effects.

## 5. Token discipline

Inspect the actual Levyra theme and reusable components before inventing values.

- Reuse semantic colors and existing dark/light behavior.
- Reuse established typography styles before adding a new type scale.
- Reuse the project radius/shape language; avoid one-off values without a clear reason.
- Prefer a small spacing vocabulary over arbitrary `dp` values.
- Keep icon treatment consistent within the same surface.
- Introduce a new token or reusable visual primitive only when at least two real call sites need it or the existing token system cannot express an approved product requirement.

Do not create a second design system beside the current one.

## 6. References and screenshots

When the owner provides another app, screenshot, mockup or website:

1. identify what is actually attractive or useful: hierarchy, density, artwork prominence, spacing, typography, navigation clarity, motion timing, depth or interaction feedback;
2. identify what does not fit Levyra's platform, architecture or product priorities;
3. re-express the useful principles using Levyra components and tokens;
4. preserve Levyra's identity instead of cloning logos, exact geometry, copy, proprietary assets or signature visuals.

The reference is evidence of desired direction, not a source of truth.

## 7. Motion must earn its frames

Every animation should have one primary reason:

- explain state change;
- preserve spatial continuity;
- confirm an interaction;
- direct attention;
- support playback immersion without interfering with controls.

If the reason is only "looks cool", remove or simplify it.

Respect reduced motion, lifecycle state, battery/data saver where applicable, low-resource devices, background/foreground transitions and existing Compose performance rules. Static content must appear first when animation or remote media is optional.

Do not animate broad state that updates with playback ticks. Keep high-frequency values out of large recomposition scopes.

## 8. Product-state completeness

A visually polished happy path is not complete until the changed surface is also coherent for relevant states:

- loading;
- cached refresh;
- empty;
- offline;
- error;
- disabled/permission-limited;
- selected/playing/current;
- long titles and artist names;
- large font scale;
- RTL and long translations;
- keyboard/focus on Desktop;
- TalkBack/semantics and touch targets on Android.

Do not hide information or reduce accessibility to preserve a composition.

## 9. Pre-flight review

Before considering a visual change complete, inspect the actual final diff and answer:

- Is the primary action/content obvious within a glance?
- Does the screen still look like Levyra rather than a generic template?
- Did the change reuse current tokens/components before adding new ones?
- Is every decorative element doing useful visual work?
- Is motion justified and bounded?
- Are color, shape, typography and spacing internally consistent?
- Did the change preserve navigation, gestures, playback and state ownership?
- Are loading/error/offline/empty states still usable?
- Are long text, font scaling, RTL, contrast and accessibility protected?
- Did any one-off abstraction or dependency appear only to support polish?
- Was UI performance measured when the change plausibly affects scrolling, recomposition or frame time?

If any answer is no or unknown, do not call the design finished.

## 10. Validation and reporting

Use the matching platform skill for implementation validation. For visual work, distinguish clearly between:

- static code review;
- screenshot/preview inspection;
- emulator/device inspection;
- measured frame/recomposition evidence;
- accessibility/TalkBack or keyboard/focus checks.

Never claim a visual, performance or accessibility result that was not directly inspected.

Report the design rationale in terms of user hierarchy, consistency, behavior preserved and measurable risk, not adjectives alone.

## Provenance

This Levyra-native skill was informed by the anti-template and audit-first ideas in `Leonxlnx/taste-skill` (MIT, 2026), but it is intentionally rewritten for native Compose product UI. Web-specific React, Tailwind, landing-page, SEO and marketing-page rules are not vendored into Levyra.