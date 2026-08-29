# Levyra Design System

## 1. Visual theme and atmosphere

Levyra is a cinematic, artwork-led music interface. It should feel immersive without looking busy: the chrome recedes into near-black surfaces, while album artwork supplies most of the colour and emotional tone.

The Home screen is a discovery surface, not a dashboard. Its first viewport should communicate:

1. Levyra identity;
2. a personal time-based greeting;
3. a clear settings entry point;
4. mood selection;
5. one featured recommendation;
6. personalised music shelves.

Do not place a utility grid, shortcut dashboard or repeated navigation destinations between the greeting and the music.

## 2. Colour palette and roles

### Base surfaces

| Token | Value | Role |
|---|---:|---|
| Canvas dark | `#08090D` | Primary dark background |
| Canvas mid | `#0C0D12` | Upper atmospheric surface |
| Header dark | `rgba(20,21,29,0.85)` | Branded Home header |
| White | `#FFFFFF` | Primary text on dark surfaces |
| White 64% | `rgba(255,255,255,0.64)` | Secondary text |

### Existing Levyra accents

- Levyra Cyan: primary playback and active-state signal.
- Levyra Violet: secondary brand accent.
- Levyra Pink: favourites and emotional actions.
- Artwork palette: ambient halos, selected borders and hero atmosphere.

### Rules

- Artwork is the main source of colour.
- Brand accents are functional, not decorative wallpaper.
- Do not place multiple strong gradients inside the same viewport.
- Never tint every card independently.
- Maintain readable white or dark foreground contrast over artwork.

## 3. Typography

Use the platform typography stack already shipped by Levyra.

| Role | Size | Weight | Notes |
|---|---:|---:|---|
| Home greeting | 23–26sp | 800–900 | Personal and immediately readable |
| Brand wordmark | 26–30sp | 800–900 | Tight tracking |
| Section title (feature) | 23sp | 900 | Personal and editorial shelves |
| Section title (standard) | 19sp | 900 | Later exploratory shelves |
| Card title | 14–16sp | 600–700 | Maximum two lines |
| Metadata | 11–13sp | 400–600 | Muted colour |
| Utility label | 11–13sp | 700–800 | Short labels only |

Use weight contrast before increasing font size. Avoid oversized editorial typography on functional screens.

## 4. Shape system

Use a small, predictable radius family.

| Token | Radius | Use |
|---|---:|---|
| Artwork small | 10–12dp | Compact rows |
| Standard card | 16–18dp | Rows and secondary modules |
| Hero | 22dp | Featured recommendation |
| Home header | 26dp | Greeting and settings identity surface |
| Large player surface | 28–34dp | Player-only expressive surfaces |
| Pill | 50% | Chips and compact actions |
| Circle | 50% | Avatars and icon-only controls |

Albums and playlists remain square with restrained corners. Artists are circular. Music videos use 16:9 framing.

## 5. Home header

The header is Levyra's main identity surface.

It contains:

- the Levyra logo and wordmark;
- a large time-based greeting;
- a full-width search entry showing the search placeholder;
- a visible settings control with icon and text;
- restrained cyan/violet atmospheric accents;
- one unified surface instead of multiple floating pills.

The greeting should feel personal and premium, not like a status chip. Settings must be clearly labelled and meet the 48dp minimum touch target.

## 6. Home component families

### Artwork card

- square album or playlist artwork;
- 156dp standard width, 182dp for the lead album shelf;
- 10–14dp artwork radius;
- title below artwork;
- metadata below title;
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

### Editorial collection card

- 268x188 landscape card with an artwork mosaic;
- the widest item on Home, used as the magazine beat between square shelves;
- title and artists over a vertical scrim, single play affordance.

### Ranking row

- no card background or border; the list sits directly on the canvas;
- right-aligned rank column, gradient numerals for the first three positions;
- 52dp artwork, title and artist in one text column;
- reserved for Top 50 so ranking never reads like another carousel.

### Editorial hero

- only one strong hero per viewport;
- 22dp radius;
- artwork-led palette;
- a single clear primary action;
- no competing shortcut grid above it.

## 7. Layout principles

- Horizontal screen inset: 18dp.
- Section stride: 40dp before a feature section, 30dp standard, 24dp before a quiet section; 28/22/18dp in compact Home.
- Header internal padding: 16dp.
- Header-to-moods gap: 12dp.
- Horizontal shelves may extend to the screen edge after the initial inset.
- Prefer one strong hero per screen.
- Place personalised content above broad exploration.
- Do not repeat the same track in the hero and immediately following collection when avoidable.

### Home ordering

1. Levyra greeting, search entry and settings header.
2. Mood chips.
3. Levyra editorial spotlight.
4. Continue listening when relevant.
5. Personalised listening and quick picks.
6. New releases and albums.
7. Editorial collections and resonance.
8. Artists, videos, additional shelves and charts.

## 8. Depth and glass

Glass is a control treatment, not the default material for all content.

Use glass for:

- the Home identity header;
- top-level controls;
- compact active chips;
- player chrome;
- temporary overlays.

Do not use strong glass, borders and shadows simultaneously on every card. Content cards should rely on artwork, spacing and typography.

## 9. Levyra Aura background

The Home atmosphere consists of:

- a near-black vertical base;
- two large artwork-derived radial halos;
- one faint audio-wave path and one softer echo;
- a strong fade to the base canvas before lower shelves.

The background must not contain circuit nodes, dense particles or multiple decorative arcs. It should support artwork rather than compete with it.

Palette transitions may crossfade when animations are enabled. The artwork halos belong to the top of the page: they fade out as the Home list scrolls away from the first item, so lower shelves sit on the neutral canvas. The fade is driven in the draw phase and is skipped when animations are disabled. Avoid infinite background animation and expensive blur during scrolling.

## 10. Motion and interaction

- Minimum touch target: 48dp.
- Press scale should remain subtle, normally `0.97–0.99`.
- Use spring motion for direct manipulation and toggles.
- Use 180–320ms transitions for most UI state changes.
- Artwork palette transitions may take 420–700ms.
- Respect the existing `animationsEnabled` setting.
- No animation should continuously consume resources solely for decoration.

## 11. Responsive behaviour and accessibility

### Small phones

- keep the header readable without shrinking its touch targets;
- allow the settings label to shorten only as a last resort;
- reduce section gaps before reducing interaction sizes;
- keep card titles to two lines maximum.

### Tablets and wide layouts

- keep a readable header width where a full-width surface would become sparse;
- allow larger artwork and more shelf items;
- do not stretch compact track rows indefinitely.

### Accessibility

- preserve semantic roles and click labels;
- provide text alternatives for meaningful controls;
- do not communicate active state by colour alone;
- maintain at least 48dp interaction targets;
- honour reduced or disabled animation preferences.

## 12. Do and do not

### Do

- make the greeting and Levyra identity the visual anchor;
- let artwork provide emotional colour;
- keep the background dark and quiet;
- use consistent radii and spacing;
- distinguish albums, artists, videos and tracks by shape;
- reuse shared design tokens.

### Do not

- insert shortcut grids that duplicate navigation or shelves;
- copy another product's proprietary fonts, icons or branding;
- turn every section into a large editorial card;
- add glass, gradient, border and shadow to every component;
- create multiple competing hero modules;
- change data loading, playback or navigation behaviour during a visual-only redesign.

## 13. Agent implementation guide

When an AI coding agent changes Levyra UI:

1. Read this file before editing Compose code.
2. Reuse existing theme tokens or add tokens to the appropriate design object.
3. Preserve ViewModel, playback, cache and navigation behaviour unless explicitly requested.
4. Keep the number of component families small.
5. Validate dark and light palettes, compact Home mode and disabled animations.
6. Avoid new dependencies for effects already possible with Compose drawing primitives.
7. Prefer focused component files over further expanding `LevyraApp.kt`.
