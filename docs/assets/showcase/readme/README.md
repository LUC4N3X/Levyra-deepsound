# README screenshot cards

The root README uses six desktop panels (four cards each) and six mobile panels
(the same cards in a two-by-two grid). The mobile source is selected at 600px.
Near-white backgrounds with a faint ice-blue tint, smaller teal headings and
subtle phone shadows keep every card light. The outer cards have square corners
and no borders. Only the original phone frames retain their rounded corners.
Transparent, narrow gutters work on both GitHub themes.

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
It writes only the twelve `row-*-pearl-square*.webp` panel files in the selected
output directory. The README references these distinct filenames so cached
images from the earlier dark gallery cannot be reused for the new cards.

## Backdrop provenance

`studio-backdrop.webp` was created for this redesign using the built-in
`image_gen` tool, then resized and encoded as WebP. It contains no app interface,
people, artwork, logos or text. All cards share the same delicate light backdrop.
The renderer adds a 45% white wash to soften the tint. Regeneration uses this
checked-in backdrop and does not call an image service.

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
