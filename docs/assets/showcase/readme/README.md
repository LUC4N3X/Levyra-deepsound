# README screenshot panels

Six panels group the 24 existing Android screenshots into listening, discovery,
library, personalization, offline listening and skins. Each panel has one shared
headline and four distinct cards, each with an inset title, rounded border and
its own screenshot. Graphite and warm-paper surfaces frame the cards. The first
two panels are visible in the README; four more are inside the full-gallery
disclosure.

Desktop panels are 2000 × 1580 with four screens across. At viewport widths up
to 600px, the README selects a separately composed 1000 × 2590 panel with two
screens per row, wrapped copy and larger text relative to the canvas. Rounded
outer corners are transparent on both GitHub themes.

`scripts/render_readme_showcase.mjs` extracts the phones from the tracked
`../editorial-cards/*.webp` assets. The first four keep the existing angled
screen crops; the remaining twenty retain their complete phone frames. The
second hero uses its own phone, without the previous card's spillover. App
interfaces and third-party artwork are not generated or repainted.

## Regeneration

Use Node.js 22 or newer, Sharp 0.35.3, and installed Rubik Regular/Bold fonts.
The renderer falls back to Segoe UI, DejaVu Sans or system sans serif. Inspect
the output if regenerating with different fonts.

From the repository root:

```sh
npm install --prefix .levyra-tools/readme-showcase --no-save --package-lock=false sharp@0.35.3
node scripts/render_readme_showcase.mjs --sharp-module ./.levyra-tools/readme-showcase/node_modules/sharp
```

Use `--output build/readme-showcase-preview` to render to a different directory.
The renderer reads repository-relative sources and writes twelve
`row-*-studio*.webp` files. Backgrounds and typography are composed directly
from the renderer; regeneration does not call an image service. Earlier panel
assets and their backdrop are retained for existing links but are not used by
this renderer.

## Earlier backdrop provenance

`studio-backdrop.webp` was created for the earlier pearl redesign using the built-in
`image_gen` tool, then resized and encoded as WebP. It contains no app interface,
people, artwork, logos or text. It was used by the earlier pearl panels, with a
45% white wash. The current panels do not use this asset.

Generation prompt:

> Use case: precise-object-edit. Asset type: background ONLY for professional
> music app screenshot cards in a GitHub README. Replace this dark graphite
> backdrop with a VERY DELICATE LIGHT background. Portrait 1024x1536. Smooth
> luminous pearl white on the left (#f8fcff), gently fading horizontally into pale
> ice blue on the right (#cee8f3), with very subtle soft cyan at bottom right. A
> clean airy satin-paper surface, almost flat, extremely understated, no visible
> grain. Overall very light, low saturation, calm and refined. The top must remain
> very light as dark teal headings will be added separately. One continuous
> near-white to pastel-blue gradient, no bands, no patches, no vignette.
> Constraints: background only; no phones, no interfaces, no text, no logos, no
> objects, no circles, no waves, no frames, no shadows, no dark areas. This will
> sit behind authentic unmodified screenshots. Entire image should feel soft
> and delicate, like a minimal premium product sheet.

Existing artwork and service names remain subject to the repository's
[third-party rights notice](../../../legal/LEGAL.md#3-third-party-services-and-rights).
