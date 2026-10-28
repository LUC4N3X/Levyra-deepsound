# Levyra 3.0.5

## Highlights

The idea for this release came from a complaint I kept hearing: radio and recommendations bringing back songs people had heard a hundred times. So now there's Fresh Discovery, a setting you can switch on to keep the songs you've already played out of automatic queues. It's off unless you choose to turn it on.

Then there's a round of polish on the default look, settings that finally show the whole description instead of cutting it off halfway, a few annoying bugs with navigation and the Gallery fixed, and better translations. Two of these changes come from people outside the project, and I thank them at the end.

## ✦ Fresh Discovery

You'll find it in Settings → Player. Once it's on, radio and the automatic queue skip songs you've already heard, and so do the discoveries in Your Orbit.

You choose how far back Levyra looks: 7, 30 or 90 days, or your whole history. 30 days is the default. There's also an "Allow favorites" option, on by default, so your favorite songs can still show up even if you've just listened to them.

A few things I cared about:

- When it's off, nothing changes. Everything works exactly as it did before.
- Songs you pick yourself always play. The filter only touches what Levyra picks for you.
- If there aren't enough new songs, the queue doesn't stall: it fills up with the ones you haven't heard for the longest time.
- It's careful with duplicates. If an upload clearly matches a song you've already heard, it skips that too, but it won't guess when it isn't sure. One limit I'll be honest about: a different upload of the same song can still get through when it doesn't show up next to the one you've already heard.
- It uses the listening history already on your phone and doesn't send anything anywhere.

## ✦ A sharper default look

The default skin got a visual pass. Long lists in your library now have a scrollbar you can drag, with the letter showing as you scroll. The classic home header shrinks into a compact bar when you scroll down, and the Flow player has a bit more depth and feel under your finger.

In Settings, titles and descriptions no longer end in three dots. Fresh Discovery's new card shows the full text, and the same goes for the other settings, the headers of the Mini player, Navigation and Home studios, and the audio panel.

Eclipse, the effect from 3.0.3 that tucks the mini player into the navigation bar, now only kicks in with the Levyra Original and Liquid Capsule navigation styles, the two it was really designed for. With the other styles the dock stays as it is. Your setting is kept, so if you switch back to one of those two styles, Eclipse comes back.

## ✦ FLAC

When you ask for lossless, Levyra now tries a new source first and only uses it if it finds a matching, verified stream. If it doesn't, it falls back to the sources from before, in the same order. It's still experimental, just like in 3.0.3: it depends on outside services, and I can't promise how stable it'll be day to day. If you stay on 320 kbps, nothing changes.

## ✦ The everyday fixes

- Floating navigation is truly transparent again: the dark band that cut across the page above the dock is gone.
- Bottom navigation labels are no longer cut off at the bottom, for example in Russian or with larger system text.
- The Play button in the Gallery now stays still in every layout, instead of animating in some and not in others.
- Long-pressing tracks in Search no longer leaves the selection actions hidden behind the mini player or navigation bar.
- The French translation has been updated and finished, and some plurals in Czech, Slovak, Polish, Russian and Ukrainian are now correct.
- App and widget names no longer risk ending up empty in translations: they live in a separate file that translators don't touch.

## ✦ Thanks

This time I didn't do it all alone. Thanks to @mvanhorn for the fix to the Play button in the Gallery, and to @Mickael81 for the work on the French translation. Small contributions like these make a difference for everyone who uses Levyra every day.

## Validation

These notes cover what was merged into main after 3.0.3, from `v3.0.3` to the commit of this release. They describe the code and how it's meant to behave; they don't mean the 3.0.5 APK has already passed release testing.

For Fresh Discovery, the new unit tests on the selection logic and the translation checks across all 41 languages passed locally, along with the existing tests for automations, the radio mixer and Smart Orbit. On an emulator I checked startup with no crashes, that the setting is off by default, that the choices stick after the app is closed, switching it off from the interface, and the filter on Your Orbit with real history (on that emulator the history lookup took 54 ms the first time and 6 ms after that, off the main thread). The filter on the radio queue is covered only by unit tests: on that emulator, starting the radio didn't add any songs whether the setting was on or off, so I couldn't watch it work on a device.

The translation changes were checked by validating the XML files and with a resource merge build. The local fast AI quality gate stopped on a known line-ending issue in a file this release doesn't touch. I didn't run the full quality gate, an Android release build, a full regression pass on a physical device, Android Auto checks or signing verification: their status is unverified, not passed. I haven't checked the Eclipse limit on the other navigation styles on a device.

The GitHub release workflow still has to pass the release-note checks and Android lint, build and sign the APK, verify the version and signing certificate, generate the SHA-256 checksum and publish the files. Treat the GitHub release and the downloadable APK as available only once that workflow succeeds.

## Versioning

- Version name: `3.0.5`
- Version code: `3000500`

This is an Android release. Levyra Desktop is versioned and released separately; this update doesn't change the Windows app.

## Upgrade notes

No manual migration is expected. Your library, playlists, favorites, downloads, listening history, settings, skin and player style stay as they are. Fresh Discovery starts off, so nothing changes until you turn it on. If you were using Eclipse with a navigation style other than Levyra Original or Liquid Capsule, the dock now stays put.

Catalogs, regional releases, lyrics, search, video and lossless sources depend in part on external services: results and availability can vary by region and over time.

Once the publishing workflow finishes successfully, the signed Android APK and its checksum will be on the GitHub release page. F-Droid and other channels may update later.

## Final note

3.0.5 is about listening to something new. If radio kept bringing you back to the same ten songs, try turning on Fresh Discovery and tell me how it goes.

Thanks for listening, and for telling me when something feels off.
