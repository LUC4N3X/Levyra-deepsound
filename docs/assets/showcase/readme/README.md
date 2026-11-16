# README screenshot cards

The gallery contains 24 independent portrait cards. Each has its own headline,
short description and screenshot on a light blue, lilac or mint surface. Soft
scalloped shapes add a restrained Material 3 Expressive detail. There are no
shared headings, enclosing panels or hidden gallery sections.

Each card is 900 × 1800 and is also exported individually. The README uses six
2400px-wide rows of four cards, with transparent gutters between them. At
viewport widths up to 600px, it selects a 1200px-wide two-by-two arrangement of
the same cards. Rounded card corners are transparent on both GitHub themes.

`scripts/render_readme_showcase.mjs` extracts the phones from the tracked
`../editorial-cards/*.webp` assets. The first four keep the existing angled
screen crops; the remaining twenty retain their complete phone frames. The
first pair preserves its original continuous phone composition. App
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
The renderer reads repository-relative sources and writes 24 individual
`card-*-studio.webp` assets and twelve `row-*-studio*.webp` grids.
Backgrounds and typography are composed directly
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
