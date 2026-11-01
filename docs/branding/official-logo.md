# Levyra official logo

The cyan resonance mark is Levyra's official symbol across Android and Windows.

## Master artwork

- `levyra-mark.svg`: vector master, transparent background, with a restrained cyan-to-petrol gradient.
- `levyra-mark-monochrome.svg`: solid single-color vector for themed icons and one-color treatments.
- `levyra-mark.png`: transparent raster export for platforms that require PNG.

The mark should remain unframed and unaltered. Do not add a separate play triangle, waveform, glows, shadows, or extra rings. Maintain clear space around the curves.

## Application resources

Android's adaptive launcher uses matching density-specific foreground and monochrome resources. The background is a solid dark petrol tile (`#0A1A25`). The in-app logo uses exactly the same PNG bytes as the xxxhdpi launcher foreground, preserving the existing branding test.

Android's legacy launcher PNGs, the Fastlane store icon, OpenAPK artwork, and the Windows tray/window and installer assets are generated from the same vector geometry. Desktop's ICO has multiple resolutions for Windows.

The master SVG is the source of truth for future exports. The application and store artwork use an opaque dark background only where required. No runtime image-generation dependency is added.
