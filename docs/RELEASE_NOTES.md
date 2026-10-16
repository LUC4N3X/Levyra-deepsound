# Levyra 2.6.9

## Highlights

I've spent a lot of time on the parts of Levyra people see and touch every day: the home screen widgets, the navigation bar, the player and the little details that make the app feel like yours. There is also a new way to look back at what you've been listening to.

But this update isn't only about appearance. Several fixes came directly from reports on GitHub and 4PDA. Some were small annoyances; others, like radio stations going silent or preloaded tracks not being available after a restart, needed a closer look. Thanks to everyone who took the time to explain what was happening.

## ✦ Seven new home screen widgets

Widget Studio is here. Alongside the original widget, you can choose from seven new designs: Mini, Slim, Expressive, Artwork Hero, Immersive, Vinyl and Quick Actions.

The idea is to give you something useful whether you have a small space left on your home screen or want the artwork to take centre stage. Widgets adapt to their available size, including larger layouts and landscape. You can adjust their look with artwork-based colours, your Levyra theme, Material You where supported, light or dark colours, or a custom choice.

Widget Studio lets you preview the result before saving it. Playback controls use the same player session as Levyra itself, including supported favourite, shuffle and repeat actions. The progress display can refresh in Battery saver (30 seconds, the default), Balanced (10 seconds) or Smooth (1 second) mode, so you can decide how much updating you want.

The existing widget hasn't been replaced or redesigned. It is still available as Original (default).

## ✦ A navigation bar you can really make your own

Navigation Studio has grown quite a bit. Liquid Glass Dock adds a translucent treatment with a solid fallback when blur isn't available. Split Floating Dock gives Search its own button, which separates from the tab capsule as you open it. Connected Dock joins the navigation bar and the Connected mini player into a single changing shape.

There is also a paired YouTube Music-inspired navigation and mini-player style. Choosing either half selects its matching partner. If you switch away, Levyra returns the other half to Original instead of leaving a mismatched pair behind.

The editor now has more control over indicators, labels, corner shapes, spacing, motion, surface tones and colour sources. You can follow the Levyra theme, Material You, your own accent or the current song artwork. Artwork colours change smoothly instead of flashing back to the theme while the next cover loads.

Expanding labels are now the default for the customisable navigation styles: the selected destination makes room for its name while the other tabs settle back. Existing saved choices are respected. Levyra Original keeps its familiar rendering and remains the default for anyone who hasn't customised it.

## ✦ A steadier player palette, if that's what you prefer

A few people wanted to keep one colour scheme instead of having the player change with every song. You can now choose between Dynamic, Theme colours and Custom under Settings > Player. Dynamic remains the default.

Fixed colours apply to the player surfaces, controls, progress bar and supported backdrops. You can still use Canvas, and the Adaptive backdrop can still show blurred artwork without changing your chosen palette. The Original mini player follows the same colour choice.

The first-run setup now offers light and dark appearance previews. Settings has also been reorganised into clearer groups, with a shelf for the different studios. The studio cards can scroll properly to the screen edges instead of being cut off.

## ✦ Your listening, in a receipt

Listening Insights now has a Listening Receipt you can actually save or share. It turns the listening information Levyra already keeps on your device into a 1080 × 1350 image with your listening activity, top tracks, top artist and artwork.

You can choose Aurora, Noir or Paper, see the preview in Levyra and then share the PNG through Android's share sheet or save it to Pictures/Levyra. It uses the existing local listening data rather than introducing a separate tracking service.

There is also Year in Music, a short story through your listening year: minutes, artists, tracks and listening rhythm, ending with a receipt for the year. You can tap through it yourself; automatic progression follows your animation preference. Comparisons to the previous year use the listening history actually available on the device.

## ✦ Playback that handles interruptions better

Losing a connection for a moment shouldn't mean starting the song again. The playback recovery path now handles temporary network loss and changes between networks more carefully. Instead of repeatedly rebuilding an online player while the device is offline, it waits for connectivity and uses the existing bounded retry and resume behaviour.

When a track keeps buffering, the automatic stability profile can react during that same track rather than waiting until the next one. If you've selected a custom buffer profile, Levyra respects it. Crossfade also avoids an unnecessary resync seek when the outgoing player is still buffering.

These are improvements to recovery, not a promise that every unavailable stream or long outage can be hidden. Source availability and the network still matter.

## ✦ Preloading that follows the playlist

I also revisited advanced track preloading after reports that tracks from the current queue weren't reliably available offline after restarting the app.

Fully cached tracks now keep a persistent cache hint even if they haven't been played yet, and the hint store has more room for larger preload windows. Automatic radio and YouTube Music queues can fill the selected 5, 10 or 15-track window instead of remaining shorter than the chosen setting.

You'll now find preloading under Settings > Offline downloads, below Smart Offline, where it makes more sense. There's an optional Use mobile data switch, off by default. Android Data Saver is still respected. The choices are saved with your preferences and backups.

Preloading remains optional, and cached material is still subject to cache limits and source availability. It isn't a replacement for an explicitly saved download.

## ✦ Live radio should tell you when something is wrong

Some listeners found radio stations that would sit there silently without explaining why. Live radio now detects a stalled stream instead of waiting indefinitely, shows a reconnecting state, and gives a readable message when a station can't be played.

It can refresh the station URL and, when available, try a small number of alternative streams belonging to that same broadcaster. It won't quietly switch you to a different station. PLS and M3U playlist-style station links are supported too, within bounded resolution limits.

A failed station can be restarted cleanly, and repeated attempts have limits. This work was prompted in particular by reports about Tamil and Telugu radio streams.

## ✦ Better results for regional music

Search has also been adjusted for Tamil and Telugu music. Some characters and vowel marks were being lost in the matching keys, which could make different songs look identical to the search engine. Those distinctions are now preserved.

For a sparse Tamil or Telugu search, Levyra can make one additional bounded lookup using a romanised form of the query. This gives less common titles another chance to appear without turning the search into an endless series of requests. It doesn't create new playable audio sources, but it improves how existing results are found and distinguished.

## ✦ Lyrics translation with clearer answers

Lyrics translation now keeps each translated line attached to the correct original line. It also handles an occasional line that can't be translated without shifting the rest of the song out of place.

More importantly, Levyra now explains what's happening. You can see when a translation is in progress, when the language model still needs to be downloaded, when the feature isn't supported on the device, or when something failed. On supported phones, the model status can take you to the system Live Translate settings; returning to Levyra retries the translation.

This matters especially on phones whose Android translation service is missing or doesn't provide the required language model. Levyra can explain the limitation, but it cannot manufacture a missing system translation service. The lyric option chips are also easier to read in light mode.

## ✦ YouTube Music and discovery fixes

If you've connected a YouTube Music account, tracks played in Levyra can now be registered in your YouTube Music listening history through the existing playback tracking path. Your Orbit can show recently played Levyra tracks first and use cached account recommendations to fill out the second page. Without a connected account, this behaviour is unchanged.

Tapping anywhere on a song result now starts that song; the artist subtitle no longer unexpectedly opens the artist page.

Release Radar has been corrected too. When an album has a real release identifier, tapping it opens the album rather than taking you to a search for its name. A search remains the fallback if the release identifier is unavailable.

## ✦ Better gestures and Canvas across player styles

Cover swipes now work across the player styles that were giving people trouble, including Velvet and Late Night. The gesture area is no longer tied to a spinning record or limited by the shape of a small cover. Switching styles also avoids carrying over an upside-down, hidden lyric face.

Velvet's play/pause symbol no longer morphs between shapes, and its landscape controls remain aligned with the portrait design.

The artist page now looks for its Canvas sooner and checks the available candidates in parallel instead of one after another. Gallery can show moving Canvas artwork too, rather than always stopping at a static cover. These changes still depend on whether suitable motion artwork is available.

## ✦ More thoughtful integration with your phone

There is a new Device integration panel in Settings > Integrations. Levyra detects supported device and ROM capabilities and uses the appropriate system media integration when those capabilities are available.

On supported Xiaomi, Redmi and POCO phones running the right HyperOS version, that includes Super Island with lyrics. Supported vivo and iQOO devices can use OriginOS Atomic Island. The existing OPPO, OnePlus and realme lyrics integration is preserved. Samsung, Pixel, Honor and other devices continue using the standard Android media session when a specialised island isn't available.

These integrations depend on the manufacturer's software and the features actually exposed by the ROM; simply owning a phone from one of these brands doesn't guarantee that a particular island will appear. The panel helps show what's detected and active, and there are shortcuts to relevant background-playback settings.

## ✦ Small fixes from your reports

Some details deserve a mention because they were genuinely annoying:

- The in-app update download banner now has a solid, readable surface instead of letting Home show through its text and progress bar.
- Cancelling an update no longer lets an older download's state interfere with a newer request.
- Download progress units and decimal separators now read naturally in Russian.
- Changes to Home Studio's Original layout, including section visibility and ordering, are actually applied. Resetting it returns to the classic Home.
- Navigation and studio layouts have had extra attention around scrolling, space and touch behaviour.
- The README showcase and project imagery have been refreshed, with more consistent device previews and clearer translation coverage.

Several of these corrections started as detailed community reports. Keep them coming; being able to reproduce an issue makes it much easier to fix properly.

## Validation

The changes described here were merged into `main` after the Android `v2.6.8` release. The product changes include PRs #951, #954, #956, #958, #959, #960, #962, #964, #965, #966, #970 and #972. Documentation and showcase updates were also merged during this period. This summary was prepared against `main` through commit `b76c1e74360381edc43f188776f9923d15a77e5b`.

The individual PRs include focused test changes and, for some reported problems, emulator reproduction or verification described in their PR discussions. Those checks are not evidence that the final signed 2.6.9 APK has been built or passed a complete end-to-end test.

No local Gradle build, complete emulator regression, physical-device playback test, Android Auto check or final APK signing verification was performed as part of preparing this version bump. These remain unverified for the finished 2.6.9 artifact at this stage.

The existing GitHub release workflow must still pass Android lint, build and sign the APK, verify the version and signing certificate, produce the SHA-256 checksum, publish `v2.6.9` and verify the downloadable assets. These are required publication gates, not checks claimed as already completed here.

## Versioning

- Version name: `2.6.9`
- Version code: `2060900`

This is an Android release. Levyra Desktop remains separately versioned at `1.2.2`; its release and Windows installer are not changed by this update.

## Upgrade notes

No manual data migration is expected for the version change. Existing music, playlists, downloads, listening history and saved appearance choices continue to use their normal storage paths. The Original widget and navigation style remain available, and the new designs are optional.

Dynamic player colours remain the default, and mobile-data preloading stays off unless you enable it. Some features depend on a connected YouTube Music account, a compatible Android translation service or manufacturer-specific phone software, as described above.

The signed GitHub APK and its checksum will be available from the release page after the release workflow succeeds. F-Droid and other distribution channels follow their own publication process and may not update at the same time.

## Final note

This update is a mix of things I've wanted Levyra to do for a while and things you've told me needed fixing. I like that the app can be more personal without forcing a new design on everyone, and I care just as much about a song continuing to play, a radio station giving you a useful error, or a setting doing what it says.

Thanks for listening, testing and reporting the rough edges. That's how Levyra keeps getting better.
