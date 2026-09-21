# Levyra Design System

## 1. Visual theme and atmosphere

Levyra is a cinematic, artwork-focused music interface. The visual styling keeps chrome unobtrusive: background surfaces recede into near-black tones, letting album artwork supply the colour and atmosphere.

The Home screen is a discovery surface rather than a dashboard. Its first viewport should communicate:

1. Levyra identity
2. A personal time-based greeting
3. A clear settings entry point
4. Mood selection chips
5. One featured recommendation
6. Personalized music shelves

Avoid placing utility grids, shortcut dashboards, or repeated navigation destinations between the greeting and the music.

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

- Levyra Cyan: primary playback and active-state indicator.
- Levyra Violet: secondary brand accent.
- Levyra Pink: favorites and emotional actions.
- Artwork palette: ambient halos, selected borders, and hero atmosphere.

### Rules

- Album artwork is the main source of colour.
- Brand accents are functional rather than decorative wallpaper.
- Do not place multiple competing gradients inside the same viewport.
- Never tint every card independently.
- Maintain readable white or dark foreground contrast over artwork backgrounds.

## 3. Typography

Use the platform typography stack already shipped by Levyra.

| Role | Size | Weight | Notes |
|---|---:|---:|---|
| Home greeting | 23-26sp | 800-900 | Personal and immediately readable |
| Brand wordmark | 26-30sp | 800-900 | Tight tracking |
| Section title | 20-22sp | 700-800 | Clear and compact |
| Card title | 14-16sp | 600-700 | Maximum two lines |
| Metadata | 11-13sp | 400-600 | Muted colour |
| Utility label | 11-13sp | 700-800 | Short labels only |

Use weight contrast before increasing font size. Avoid oversized editorial typography on functional screens.

## 4. Shape system

Use a small, predictable radius family.

| Token | Radius | Use |
|---|---:|---|
| Artwork small | 10-12dp | Compact rows |
| Standard card | 16-18dp | Rows and secondary modules |
| Hero | 22dp | Featured recommendation |
| Home header | 26dp | Greeting and settings identity surface |
| Large player surface | 28-34dp | Expressive player surfaces |
| Pill | 50% | Chips and compact actions |
| Circle | 50% | Avatars and icon-only controls |

Albums and playlists remain square with subtle rounded corners. Artists use circular avatars. Music videos use 16:9 framing.

## 5. Home header

The header is Levyra's main identity surface.

It contains:

- The Levyra logo and wordmark
- A clear time-based greeting
- A visible settings control with icon and text
- Subtle cyan and violet atmospheric accents
- A unified background surface rather than multiple floating pills

The greeting should feel personal and polished. Settings must be clearly labelled and meet the 48dp minimum touch target.

## 6. Home component families

### Artwork card

- Square album or playlist artwork
- 10-14dp artwork radius
- Title below artwork
- Metadata below title
- No permanent border unless active or required for contrast

### Artist item

- Circular artwork
- Artist name below
- No rectangular glass container around individual artists

### Compact track row

- 52-56dp artwork
- Title and artist in one text column
- Play, equalizer, or overflow action at the end
- Suitable for quick picks and charts

### Editorial hero

- Only one strong hero per viewport
- 22dp corner radius
- Artwork-led palette
- A single clear primary action
- No competing shortcut grid above it

## 7. Layout principles

- Horizontal screen inset: 18dp.
- Section gap: 22dp standard, 12dp compact.
- Header internal padding: 16dp.
- Header-to-moods gap: 12dp.
- Horizontal shelves may extend to the screen edge after the initial inset.
- Prefer one strong hero per screen.
- Place personalized content above broad exploration shelves.
- Do not repeat the same track in both the hero and the immediately following shelf.

### Home ordering

1. Levyra greeting and settings header
2. Mood chips
3. Levyra editorial spotlight
4. Continue listening shelf (when relevant)
5. Personalized listening and quick picks
6. New releases and albums
7. Editorial collections
8. Artists, videos, additional shelves, and charts

## 8. Depth and glass materials

Glass is a control treatment, not the default material for all content cards.

Use glass styling for:

- The Home identity header
- Top-level controls
- Compact active chips
- Player chrome
- Temporary modal overlays

Do not apply strong glass, borders, and shadows simultaneously to every card. Content cards should rely on artwork, spacing, and typography.

## 9. Levyra Aura background

The Home atmosphere consists of:

- A near-black vertical base
- Two large artwork-derived radial halos
- One faint audio-wave path with a soft echo
- A smooth fade to the base canvas before lower shelves

The background avoids complex circuit nodes, dense particles, or decorative arcs. It supports the artwork rather than competing with it.

Palette transitions crossfade smoothly when animations are enabled. Avoid infinite background animations and expensive blur operations during active scrolling.

## 10. Motion and interaction

- Minimum touch target: 48dp.
- Press scale remains subtle, typically between 0.97 and 0.99.
- Use spring motion for direct manipulation and toggles.
- Use 180 to 320ms transitions for most UI state changes.
- Artwork palette transitions may take 420 to 700ms.
- Always respect the user's `animationsEnabled` setting.
- Never run continuous decorative animations that waste battery.

## 11. Responsive behaviour and accessibility

### Small phones

- Keep the header readable without shrinking touch targets.
- Allow the settings label to shorten only as a last resort.
- Reduce section gaps before reducing interaction target sizes.
- Keep card titles to two lines maximum.

### Tablets and wide layouts

- Constrain header width where a full-width surface would look overly sparse.
- Allow larger artwork sizes and additional shelf items.
- Avoid stretching compact track rows indefinitely across wide viewports.

### Accessibility

- Preserve semantic accessibility roles and click labels.
- Provide clear text alternatives for all meaningful controls.
- Do not communicate active state through colour alone.
- Maintain at least 48dp interaction targets for all clickable elements.
- Honor system-wide reduced motion settings.

## 12. Best practices

### Recommended

- Make the greeting and Levyra brand mark the visual anchor.
- Let artwork provide the emotional colour.
- Keep background surfaces dark and quiet.
- Use consistent radii and spacing tokens.
- Distinguish albums, artists, videos, and tracks by shape.
- Reuse shared design tokens across components.

### Avoid

- Inserting shortcut grids that duplicate navigation or shelves.
- Copying proprietary fonts, icons, or branding from other products.
- Turning every section into a large editorial card.
- Adding glass, gradients, borders, and shadows to every component at once.
- Creating multiple competing hero modules on a single screen.
- Modifying data loading, playback, or navigation behavior during visual design passes.

## 13. AI implementation guidelines

When an AI coding assistant modifies Levyra UI:

1. Review this document before editing Compose code.
2. Reuse existing theme tokens or add new tokens to the appropriate design object.
3. Preserve ViewModel, playback, cache, and navigation behavior unless explicitly requested.
4. Keep the number of distinct component families small and manageable.
5. Verify both dark and light palettes, compact Home mode, and disabled animations.
6. Avoid adding external dependencies for visual effects that Compose drawing primitives can achieve natively.
7. Prefer focused component files rather than adding more code to `LevyraApp.kt`.
