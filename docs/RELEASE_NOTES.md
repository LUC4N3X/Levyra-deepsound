# Levyra 2.6.7

## Highlights

There's quite a lot in this update, even if the version number only moved a little. I've been working on the parts of Levyra you actually use every day, and a few things people kept asking for are finally here.

The biggest addition is optional YouTube Music sign-in. You can connect your account and bring some of your own music into Levyra, or skip it and carry on as before. Home has had more attention too, there's another player style, and I've fixed some irritating details around lyrics, search and downloads.

## ✦ YouTube Music, only if you want it

You can now sign in to YouTube Music to see your playlists, liked songs, saved albums and artists. When connected, Levyra can use your account's music feed to make Home feel more personal. Importing into your local library is an explicit action, not something it does behind your back.

There's an account screen for switching accounts and disconnecting. Saved sessions use local storage protected with Android Keystore-backed encryption. If you're not signed in, or the account feed isn't available, the usual Levyra music sources still work.

The option is in first-time setup, with a clear way to continue without an account. Existing users get a one-time introduction instead. The new onboarding text is translated into the 37 supported languages. And no, you don't have to sign in to use Levyra.

## ✦ More music in Your Orbit

Your Orbit can now show two pages of nine songs. If the list comes up short, Levyra fills the gaps with music based on your tastes, without repeating what's already in Quick Picks. If there really aren't enough different tracks, you can still see the incomplete page.

Levyra Collections look different again, with bolder artwork, more consistent colours and a less cluttered Material 3 Expressive feel. The Charts collection keeps its orange. My Soundtrack blends into Home more naturally, and the play and shuffle buttons on collection pages are easier to use.

Search suggestions now make the new words easier to spot. Recent-search cards got a bit more room too.

## ✦ Folio and the player fixes

Folio is a new player style with a more editorial layout and its own artwork animation. You can select it along with the existing looks.

There was also a strange player bug: tapping an empty spot in the expanded player could actually trigger a button on the screen underneath it. That's been fixed.

A small but useful change for downloads: if a song is already saved offline, the Download button can now ask if you want to remove it. You'll get a confirmation first. The prompt is available in all 37 languages, and removing a download doesn't remove the song from your favourites or playlists.

Motion artwork has a better fallback for video decoder errors as well.

## ✦ The smaller things

Some synced lyrics were getting their first or last letters cut off near the screen edges. That's fixed, including the way highlighted lines are laid out.

I've also adjusted the Home play buttons, the little currently-playing indicators, the music-video section and the update screen. A lot of this is the sort of thing you might not notice immediately, but I think it makes the app nicer to use.

## Validation

This Android update covers changes merged after `v2.6.6`, including YouTube Music sign-in (#918), Home and lyrics work (#917 and #921), the Folio player and download confirmation (#927), and the two-page Orbit correction (#929).

Those pull requests contain their own test reports and emulator results. That is not the same as testing the final signed 2.6.7 APK. No end-to-end physical-device validation of this final release was performed in this session.

The existing release workflow must still run Android release lint, build the signed APK, verify the version and signing certificate, produce the SHA-256 file and confirm publication. Those results aren't being claimed before the workflow runs. Android Auto, hardware-specific playback and a full manual regression pass on the final APK remain unverified here.

## Versioning

- Version name: `2.6.7`
- Version code: `2060700`

This is an Android release. Levyra Desktop follows its own version and release schedule.

## Upgrade notes

No sign-in or manual import is required. Just update normally. You can connect YouTube Music during setup or later, but you can also leave it alone.

Existing music, downloads and settings continue using Levyra's normal data paths. The GitHub APK and its checksum become available after the signed release workflow succeeds. F-Droid and other stores have their own publishing schedules.

## Final note

Thanks for the suggestions and the bug reports. Plenty of the changes here came from things people noticed while using Levyra. I'm still working on it, and I'd rather keep improving those little details than pretend it's all perfect.
