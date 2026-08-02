# Levyra Design System

## 1. Visual theme and atmosphere

Levyra is a cinematic, artwork-led music interface. The application should feel immersive without looking busy: the chrome recedes into near-black surfaces, while album artwork supplies most of the colour and emotional tone.

The design language combines the scanning clarity of modern music services with a distinct Levyra identity:

- content-first darkness rather than decorative gradients everywhere;
- artwork-derived colour used for atmosphere, active states and playback feedback;
- compact information density with clear section hierarchy;
- tactile controls with restrained glass treatment;
- expressive motion only when it communicates state.

The Home screen is a discovery surface, not a dashboard. A user should understand the first screen in this order:

1. identity and account controls;
2. immediate listening actions;
3. one featured recommendation;
4. personalised music shelves;
5. exploration content such as artists, videos and charts.

## 2. Colour palette and roles

### Base surfaces

| Token | Value | Role |
|---|---:|---|
| Canvas dark | `#08090D` | Primary dark background |
| Canvas mid | `#0C0D12` | Upper atmospheric surface |
| Tile dark | `#18191F` | Quick-access tiles and restrained cards |
| Tile pressed | `#202127` | Pressed or elevated tile state |
| White | `#FFFFFF` | Primary text on dark surfaces |
| White 64% | `rgba(255,255,255,0.64)` | Secondary text |

### Existing Levyra accents

- Levyra Cyan: primary playback and active-state signal.
- Levyra Violet: secondary brand accent and personalised mixes.
- Levyra Pink: favourites and emotional actions.
- Artwork palette: ambient halos, selected borders and hero atmosphere.

### Rules

- Artwork is the main source of colour.
- Brand accents are functional, not decorative wallpaper.
- Do not place multiple strong gradients inside the same viewport.
- Never tint every card independently; colour should indicate hierarchy or state.
- Maintain readable white or dark foreground contrast over artwork.

## 3. Typography

Use the platform typography stack already shipped by Levyra. Do not introduce proprietary fonts copied from another product.

| Role | Size | Weight | Notes |
|---|---:|---:|---|
| Page identity | 28–32sp | 800–900 | Tight tracking, one line where possible |
| Section title | 20–22sp | 700–800 | Clear and compact |
| Card title | 14–16sp | 600–700 | Maximum two lines |
| Metadata | 11–13sp | 400–600 | Muted colour |
| Utility label | 10–12sp | 700–800 | Short labels only |

Use weight contrast before increasing font size. Avoid oversized editorial typography on functional screens.

## 4. Shape system

Use a small, predictable radius family.

| Token | Radius | Use |
|---|---:|---|
| Artwork small | 10–12dp | Quick tiles and compact rows |
| Tile | 14dp | Quick access and compact cards |
| Standard card | 16–18dp | Rows and secondary modules |
| Hero | 22dp | Featured recommendation |
| Large player surface | 28–34dp | Player-only expressive surfaces |
| Pill | 50% | Chips and compact actions |
| Circle | 50% | Avatars and icon buttons |

Do not assign a different radius to every component. Album and playlist artwork should normally remain square with a restrained corner radius. Artists are circular. Music videos use 16:9 framing.

## 5. Home component families

The Home screen uses four primary content families.

### Quick-access tile

- two-column grid on phones;
- 70dp height;
- 56dp artwork or icon panel;
- 14dp outer radius;
- one short title and optional artist line;
- active playback state shown by equalizer or progress, not a decorative glow.

### Artwork card

- square album or playlist artwork;
- 10–14dp artwork radius;
- title below the artwork;
- metadata below the title;
- no permanent border unless active or required for contrast.

### Artist item

- circular artwork;
- artist name below;
- no rectangular glass container around every artist.

### Compact track row

- 52–56dp artwork;
- title and artist in one text column;
- play, equalizer or overflow action at the end;
- suitable for quick picks and charts.

## 6. Layout principles

- Horizontal screen inset: 18dp.
- Quick tile gap: 10dp.
- Section gap: 22dp standard, 12dp compact.
- Header-to-content gap: 10–12dp.
- Horizontal shelves may extend to the screen edge after the initial 18dp inset.
- Prefer one strong hero per screen.
- Place the most actionable personalised content above exploratory content.
- Do not repeat the same track in the hero and the immediately following collection when avoidable.

### Home ordering

1. Greeting and settings.
2. Mood chips.
3. Six quick-access destinations.
4. Levyra editorial spotlight.
5. Continue listening when relevant.
6. Personalised listening and quick picks.
7. New releases and albums.
8. Editorial collections and resonance.
9. Artists, videos, additional shelves and charts.

## 7. Depth and glass

Glass is a control treatment, not the default material for all content.

Use glass for:

- top-level icon controls;
- compact active chips;
- player chrome;
- temporary overlays.

Do not use strong glass, borders and shadows simultaneously on every card. Content cards should usually rely on artwork, spacing and typography.

Dark surfaces use subtle hairlines around `rgba(255,255,255,0.07)`. Active states may use an artwork-derived or Levyra accent border up to roughly 50% opacity.

## 8. Levyra Aura background

The Home atmosphere consists of:

- a near-black vertical base;
- two large artwork-derived radial halos;
- one faint audio-wave path and one softer echo;
- a strong fade to the base canvas before the lower shelves.

The background must not contain circuit nodes, dense particles or multiple decorative arcs. It should support the artwork rather than compete with it.

Palette transitions may crossfade when animations are enabled. Avoid infinite background animation and expensive blur during scrolling.

## 9. Motion and interaction

- Minimum touch target: 48dp.
- Press scale should remain subtle, normally `0.97–0.99`.
- Use spring motion for direct manipulation and toggles.
- Use 180–320ms transitions for most UI state changes.
- Artwork palette transitions may take 420–700ms.
- Respect the existing `animationsEnabled` setting.
- No animation should continuously consume resources solely for decoration.

## 10. Responsive behaviour

### Small phones

- retain the two-column quick grid;
- reduce section gaps before reducing touch targets;
- keep card titles to two lines maximum;
- horizontal shelves remain horizontally scrollable.

### Tablets and wide layouts

- allow larger artwork and more visible shelf items;
- keep readable content width where a full-width layout would become sparse;
- do not stretch compact track rows indefinitely.

### Accessibility

- preserve semantic roles and click labels;
- provide text alternatives for meaningful controls;
- do not communicate active state by colour alone;
- maintain at least 48dp interaction targets where possible;
- honour reduced/disabled animation preferences.

## 11. Do and do not

### Do

- let artwork provide the emotional colour;
- keep the background dark and quiet;
- use consistent radii and spacing;
- make the first viewport immediately actionable;
- distinguish albums, artists, videos and tracks by shape;
- reuse shared design tokens.

### Do not

- copy another product's proprietary fonts, icons or branding;
- turn every section into a large editorial card;
- add glass, gradient, border and shadow to the same component without a clear reason;
- create multiple competing hero modules;
- use decorative cyan or violet on every surface;
- change data loading, playback or navigation behaviour during a visual-only redesign.

## 12. Agent implementation guide

When an AI coding agent changes Levyra UI:

1. Read this file before editing Compose code.
2. Reuse existing theme tokens or add tokens to the appropriate design object.
3. Preserve ViewModel, playback, cache and navigation behaviour unless the task explicitly requires logic changes.
4. Keep the number of component families small.
5. Validate dark and light palettes, compact Home mode and disabled animations.
6. Avoid new dependencies for effects already possible with Compose drawing primitives.
7. Prefer focused component files over further expanding `LevyraApp.kt`.
