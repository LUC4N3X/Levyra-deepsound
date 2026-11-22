# Levyra 3.2.3

## Highlights

This update is about how Levyra opens and how it feels while you use it. There's a new startup animation, the app opens faster from its icon, the player has a new Material deck, and a few display bugs are gone.

## ✦ New

- Startup animation. When you open Levyra from its icon, two ribbons of light weave together and turn into the Levyra logo, which pulses once and opens onto Home. You can turn it off in Settings > Appearance > Startup animation, and it never plays when animations are off.
- Material player deck. It replaces Folio. The cover sits in a soft shape that turns slowly while music plays, the player takes its colors from the cover, and favorite, shuffle, lyrics and queue sit in one row of buttons. Repeat is now in the player's More menu.
- Share as story. The player menu makes a 9:16 image of the song you're playing, ready for Instagram or WhatsApp stories, or saves it to your gallery.
- Cover cropping. Covers that aren't square can fill square frames from the center, or with a smart crop with or without zoom. You'll find it in Settings > Appearance.
- Android 8 to 11 now get wallpaper colors and the blurred glass effect that newer versions already had.

## ✦ Improved

- Levyra opens faster from the launcher icon. It no longer starts a separate process before the app itself.
- Opening and closing the player is smoother. The screen behind it shrinks back and dims, and the mini player settles with a small bounce when the player closes.
- The first launch uses far less memory. On a 4 GB device, peak memory during setup went from about 2.5 GB to about 0.8 GB.
- Home sections only animate in when the list is still, and titles on Explore release cards scroll once instead of looping.
- Explore city charts and Top 100 follow your music region. Regions with fewer than three city charts also show the US list.

## ✦ Fixed

- Play counts such as "55 M de lectures" showing up instead of artist or album names in French and many other languages.
- Listener counts on trending artist cards getting cut off. They now shrink to fit.
- Blank icons in the launcher shortcut menu for Resume playback, Search, Library and Recognize music.
- City charts missing when Explore is in Russian.
- The Daily discover shelf disappearing from Home after you played something from it.
- A crash at startup with Cast on older Google Play services.

## Validation

These notes describe the Android 3.2.3 changes. The first-run memory figures above are from the reported 4 GB device comparison; they are not a new measurement performed during release preparation.

The Android publishing workflow is configured to check version consistency and release notes, run release lint, build the signed APK, verify the APK signature and generate a SHA-256 checksum before publication. Workflow results, the final published APK and additional manual device testing were not verified when these notes were prepared.

## Versioning

- Version name: `3.2.3`
- Version code: `3020300`

Levyra Desktop keeps its own version and release channel.

## Upgrade notes

Install the update from the same channel you currently use, such as GitHub or F-Droid. The startup animation can be turned off in Settings > Appearance > Startup animation, and cover cropping options are also under Appearance.

## Final note

This update focuses on a smoother start, a more expressive player and fixing small problems you can notice every day. Thanks for continuing to use Levyra and sharing what needs attention.
