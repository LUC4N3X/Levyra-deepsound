# README screenshot cards

The root README uses six desktop panels (four cards each) and six mobile panels
(the same cards in a two-by-two grid). The mobile source is selected at 600px.
Transparent gutters and rounded corners work on both GitHub themes.

`scripts/render_readme_showcase.mjs` composes all 24 cards from the existing,
tracked `../editorial-cards/*.webp` assets. The first four preserve their original
phone positions, angles and crops; the other twenty retain the complete phone.
Only the surrounding card design and copy change. The screenshots and their
third-party artwork are not generated or repainted.

## Regeneration

Use Node.js 22 or newer, Sharp 0.35.3, and installed Rubik Regular/Bold fonts.
Rubik was already used by the repository's earlier showcase generator. The
renderer falls back to DejaVu Sans or the system sans serif when Rubik is absent;
check text wrapping and spacing if regenerating with a different font.

From the repository root:

```sh
npm install --prefix .levyra-tools/readme-showcase --no-save --package-lock=false sharp@0.35.3
node scripts/render_readme_showcase.mjs --sharp-module ./.levyra-tools/readme-showcase/node_modules/sharp
```

Use `--output build/readme-showcase-preview` to inspect a regeneration before
replacing the published panels. The renderer always reads its screenshot
sources and backdrop from the repository, regardless of the working directory.
It writes only the twelve panel files in the selected output directory.

## Backdrop provenance

`studio-backdrop.webp` was created for this redesign using the built-in
`image_gen` tool, then resized and encoded as WebP. It contains no app interface,
people, artwork, logos or text. The light-theme card uses a solid pale surface.
Regeneration uses this checked-in backdrop and does not call an image service.

Generation prompt:

> Use case: product-mockup. Asset type: background texture ONLY for the existing
> Levyra Android app screenshot cards in its GitHub README. Generate a restrained,
> premium studio backdrop, portrait 1024x1536. Nearly black graphite anodized
> aluminum, subtle fine satin grain, very soft cool silver studio illumination
> falling diagonally from upper left to lower right. Mostly uniform charcoal
> (#14191f to #242c35), enough separation for a black phone frame to sit above.
> Quiet, tactile, realistic and understated like an audio equipment catalog. Top
> third dark and clean for white headings to be added later. Edge to edge
> continuous surface, no horizon. No objects, no phones, no typography, no logos,
> no circles, no waveforms, no neon, no purple, no colorful gradients, no panels,
> no borders. This image will be used as a real backdrop under deterministic
> typography and unmodified app screenshots, so do not include any UI.

Existing artwork and service names remain subject to the repository's
[third-party rights notice](../../../legal/LEGAL.md#3-third-party-services-and-rights).
