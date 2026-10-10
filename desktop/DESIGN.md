# Levyra Desktop Design System

## Product character

Levyra Desktop is a premium, dark-first music application. The interface must feel native to a desktop music player: dense enough for large libraries, calm enough for long listening sessions, and visually led by artwork rather than decorative chrome.

The design language combines the content-first darkness, compact hierarchy, pill controls, and artwork emphasis documented in the Spotify DESIGN.md with the precise surface ladder, restrained borders, and desktop-focused navigation patterns documented in the Raycast DESIGN.md.

## Core principles

1. Content first
   - Album artwork is the main source of color.
   - Navigation and controls recede into near-black surfaces.
   - Decorative gradients remain subtle and never compete with content.

2. Desktop native
   - Use a persistent full-width sidebar instead of a narrow mobile rail.
   - Keep playback controls permanently visible in an elevated bottom dock.
   - Prefer compact, scannable sections over oversized empty areas.
   - Preserve keyboard navigation and predictable hover targets.

3. Clear hierarchy
   - Use strong weight contrast rather than many font sizes.
   - Page titles are 26–32 px.
   - Section titles are 18–21 px.
   - Body text is 13–15 px.
   - Metadata is 10.5–12 px.

4. Functional color
   - Cyan is reserved for active navigation, playback, focus, and primary actions.
   - Violet supports secondary emphasis and personal mixes.
   - Mint is reserved for discovery and fresh content.
   - Errors use red only when an action failed.

5. Tactile geometry
   - Cards use 12–18 px radii.
   - Navigation items use 10–12 px radii.
   - Search fields use a full pill.
   - Play controls are circular.
   - Avoid excessive rounding on every surface.

## Color tokens

### Dark theme

- Canvas: `#07090D`
- Base surface: `#0B0E14`
- Container: `#0F131B`
- Elevated container: `#151A23`
- Highest container: `#1B2230`
- Primary text: `#F4F7FB`
- Secondary text: `#A7B0BE`
- Hairline: `#202734`
- Strong outline: `#303949`
- Levyra cyan: `#27D9F5`
- Levyra violet: `#8B7CFF`
- Discovery mint: `#50E3B3`
- Error: `#FF7485`

### Light theme

- Canvas: `#F5F7FA`
- Base surface: `#FFFFFF`
- Container: `#F0F3F7`
- Elevated container: `#E8ECF2`
- Primary text: `#11151C`
- Secondary text: `#4A5360`
- Hairline: `#D4DAE2`
- Primary accent: `#006B7D`

## Typography

- Display: 32 px, semibold, tight tracking
- Page heading: 26 px, semibold
- Section heading: 18–21 px, semibold
- Navigation and buttons: 13 px, semibold
- Body: 13.5–15 px, regular
- Metadata: 10.5–12 px, regular or semibold

Use the system sans-serif stack. Do not bundle proprietary fonts.

## Surface hierarchy

- Level 0: app canvas
- Level 1: sidebar and main page
- Level 2: cards, inputs, queue panel
- Level 3: player dock, dialogs, menus
- Level 4: temporary overlays and contextual menus

Use one-pixel hairlines only where structure is needed. Prefer surface contrast over visible borders.

## Layout

### App shell

- Sidebar width: 220 px
- Main content top and right inset: 10 px
- Main content radius: 18 px
- Player dock inset: 10 px
- Player dock radius: 18 px
- Queue panel width: 340 px
- Minimum supported window: 960 × 640 px

### Home

The Home screen is always the startup destination.

Order:

1. Greeting and contextual subtitle
2. Global search launcher
3. Featured listening panel
4. Four quick-access cards
   - Mix per te
   - Preferiti
   - Nuove uscite
   - Top 50
5. Continue listening
6. Top 50 preview
7. User playlists
8. Favorites
9. Library management tools

Do not place playlist creation fields at the top of the page.

### Content density

- Page horizontal padding: 30 px
- Page vertical padding: 26 px
- Section gap: 18 px
- Card gap: 12 px
- Music card width: 166 px
- Quick card height: 118 px

## Components

### Sidebar item

- Height: 46 px
- Radius: 11 px
- Horizontal padding: 12 px
- Icon: 20 px
- Active background: cyan at 12% opacity
- Active icon: cyan
- Inactive icon and text: secondary text

### Search launcher

- Full-width pill
- Elevated container background
- 18 px horizontal padding
- 13 px vertical padding
- Search icon: 19 px
- No visible border in the resting state

### Quick-access card

- Height: 118 px
- Radius: 16 px
- Subtle diagonal gradient from semantic tint to container surface
- Icon tile: 34 px with 10 px radius
- One-line title and one-line metadata

### Music card

- Width: 166 px
- Radius: 14 px
- Inner padding: 8 px
- Artwork radius: 11 px
- One-line title and artist
- Slight surface lift on hover

### Featured panel

- Height: 220 px
- Radius: 22 px
- Horizontal layout on desktop
- Copy on the left, artwork on the right
- Maximum two lines for title and metadata
- Primary play button plus secondary discovery action

### Player dock

- Radius: 18 px
- One-pixel hairline border
- Strong shadow
- Solid cyan circular play button
- Artwork and metadata on the left
- Transport and progress in the center
- Stream quality, volume, and queue on the right

## Interaction

- Hover should change surface brightness or scale by no more than 2%.
- Transitions should last 150–220 ms.
- Artwork-derived accent changes may last up to 500 ms.
- Disabled cards remain visible at roughly 46% content opacity.
- Never use motion that shifts layout.

## Do

- Keep the Home screen useful on first launch by showing Top 50.
- Make all primary destinations reachable from the sidebar.
- Keep album art square and consistently cropped.
- Use artwork color only as a restrained accent.
- Keep forms inside dedicated management cards or dialogs.
- Use semantic labels and content descriptions for controls.

## Do not

- Do not recreate a mobile bottom navigation on desktop.
- Do not use a 96 px icon rail with tiny labels.
- Do not leave large empty black areas above library tools.
- Do not use cyan as a decorative background across entire pages.
- Do not add thick outlines to every card.
- Do not show fake chart data or placeholder rankings.
- Do not hide essential playback controls behind menus.
