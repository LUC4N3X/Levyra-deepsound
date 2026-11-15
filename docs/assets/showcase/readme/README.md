# README screenshot cards

The root README uses six desktop panels (four cards each) and six mobile panels
(the same cards in a two-by-two grid). The mobile source is selected at 600px.
Each card uses a whisper-light pastel Material 3 Expressive surface, large rounded
corners (`48px` on the `900x1800` card canvas), soft tonal blooming-rosette,
clover, squircle, and wavy motifs matching Levyra's in-app geometry, clean
badge-free typography, and subtle phone shadows. Transparent gutters work on
both GitHub Light and Dark themes.

`scripts/render_readme_showcase.mjs` composes all 24 cards from the existing,
tracked `../editorial-cards/*.webp` assets. The first four preserve their original
phone positions, angles, and card 01-to-02 diptych spillover; the other twenty retain the
complete phone frame. Only the surrounding card design and copy are rendered.
The screenshots and their third-party artwork are not generated or repainted.

## Regeneration

Use Node.js 22 or newer, Sharp 0.35.3, and installed Rubik Regular/Bold fonts.
The renderer falls back to Segoe UI, DejaVu Sans, or the system sans serif when
Rubik is absent; check text wrapping and spacing if regenerating with a different font.

From the repository root:

```sh
npm install --prefix .levyra-tools/readme-showcase --no-save --package-lock=false sharp@0.35.3
node scripts/render_readme_showcase.mjs --sharp-module ./.levyra-tools/readme-showcase/node_modules/sharp
```

Use `--output build/readme-showcase-preview` to inspect a regeneration before
replacing the published panels. The renderer always reads its screenshot
sources from the repository, regardless of the working directory, and writes
the twelve `row-*-expressive*.webp` panel files in the selected output directory.

Existing artwork and service names remain subject to the repository's
[third-party rights notice](../../../legal/LEGAL.md#3-third-party-services-and-rights).
