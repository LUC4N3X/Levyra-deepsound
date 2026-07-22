# Levyra 2.3.15

Levyra 2.3.15 is a stability-focused release built around one simple goal: music should keep playing, regardless of whether the screen is off, the device enters Doze, the connection drops, or the current track comes from local storage.

This update also improves the Library experience with dedicated smart collection pages, more predictable back navigation, and a cleaner distinction between browsing music and starting playback.

---

## Highlights

- More reliable background playback during screen-off sessions and Android Doze.
- Stronger recovery after temporary network loss, service recreation, or a stalled player.
- Correct offline playback for downloaded tracks using their real local URI.
- Smart Library pages for Favorites, Recently Played, and Most Played.
- Improved back navigation between Library sections, albums, artists, playlists, and overlays.
- Optional battery optimization exemption available directly from Levyra settings.
- Better resolver behavior when a cached or persisted source is already available offline.
- Safer playback recovery without unnecessary CPU wake time or repeated network failures.

---

## Background playback that stays alive

Long listening sessions are now significantly more resilient.

The playback service has been hardened to survive the situations that commonly interrupt audio on modern Android devices:

- the screen remains off for an extended period;
- Android enters Doze or app standby;
- the app process is recreated while playback was expected;
- the current connection disappears and returns later;
- the player becomes stuck in a buffering state;
- the UI is no longer active while the playback service continues in the background.

Sticky playback restoration now remains valid for a much longer recovery window instead of being abandoned after roughly 30 minutes.

The service also preserves the expected playback state while a background restoration is still pending. This prevents the watchdog from incorrectly deciding that playback was intentionally stopped simply because the newly recreated player does not yet have a media item.

---

## Smarter recovery after connection loss

Online playback recovery now waits for connectivity without permanently exhausting all retry attempts.

When the network disappears:

- recovery attempts are paused instead of wasted;
- the playback wake lock is released while waiting;
- the CPU is not kept awake throughout airplane mode or a prolonged outage;
- retry counters are reset once connectivity returns;
- the service resumes recovery from the previous playback position.

This prevents a temporary connection problem from leaving Levyra permanently paused after the network becomes available again.

The service also gives the UI recovery path more time to complete before attempting its own restoration. This reduces duplicate recovery operations and avoids the service and ViewModel trying to replace the same stream at the same time.

---

## Better support for VPNs and unusual networks

Stream resolution, queue advancement, radio expansion, prefetching, and background recovery now accept networks that advertise Internet capability even when Android has not marked them as fully validated.

This is useful for:

- VPN connections;
- private DNS configurations;
- captive or partially validated networks;
- devices where Android's validation probe is delayed or blocked.

Strict validation is still used where appropriate for provider health accounting, so a questionable connection does not unfairly damage resolver client scores.

---

## Offline playback rebuilt around local media

Downloaded tracks now play directly from their stored `content://` or `file://` URI.

Local playback no longer depends on online YouTube resolution and no longer triggers unrelated network work such as:

- stream re-resolution;
- online prefetching;
- radio expansion;
- SponsorBlock requests;
- motion artwork prefetch;
- remote playback recovery.

Levyra now automatically switches Media3 wake behavior between local and network playback sources.

Local files without a recognizable extension are also handled more safely by allowing Media3 to inspect the actual container instead of forcing an incorrect MIME type.

---

## Clear errors for missing or damaged downloads

Local playback failures are no longer silently swallowed.

When a downloaded file has been removed, cannot be read, is corrupted, or uses an unsupported format, Levyra now:

- stops the unsuccessful recovery loop;
- pauses playback cleanly;
- clears the loading state;
- reports a visible error to the interface.

This makes offline failures understandable instead of leaving the user with a player that simply stops without explanation.

---

## Cached playback remains available offline

The playback resolver now checks every source that can work without a new network request before requiring connectivity.

The resolution order is now effectively:

1. local file;
2. already resolved and still-valid stream URL;
3. in-memory cache;
4. persisted playback source or manifest;
5. network availability;
6. fresh online extraction.

The same principle applies to prefetching: Levyra first checks an already resolved stream and existing cache before deciding that offline mode prevents the operation.

This avoids blocking music that is already available locally or through a still-valid persisted source.

---

## Resolver health no longer degrades while offline

Temporary DNS failures and connection loss no longer poison YouTube client health scores.

When Android reports no validated connection, Levyra avoids treating network-level failures as provider failures. Expired client blocks are also normalized when resolver health is restored.

This prevents all playback clients from becoming heavily penalized simply because the device spent time offline.

---

## More reliable queue continuation

Automatic queue advancement now understands temporary offline conditions.

When the current track finishes but the next online track cannot be resolved because connectivity is unavailable, Levyra can wait and retry once the network returns instead of immediately surfacing a permanent failure.

Continuous radio also avoids running for local tracks or when there is no usable connection.

---

## Smart Library collections

Favorites, Recently Played, and Most Played are now real Library destinations.

Tapping one of these cards no longer starts the first track immediately. Each collection opens a dedicated page with:

- a clear title and description;
- track count;
- artwork-led presentation;
- full track list;
- Play button;
- Shuffle button;
- favorite and download actions;
- current-track and playback indicators.

Playback starts only when the user explicitly selects a track or presses Play or Shuffle.

This makes the Library feel more like a complete music app and less like a set of playback shortcuts.

---

## Improved back navigation

Back navigation is now more predictable across the app.

Inside the Library:

- Back closes an active selection first;
- Back closes an open smart collection and returns to the Library overview;
- Back from another Library category returns to the overview instead of jumping to Home;
- previous scroll positions are preserved when moving between Library sections.

Album and artist navigation now keeps a lightweight history stack, allowing flows such as:

- Artist → Album → Back to Artist;
- Album → Artist → Back to Album;
- Album → another Album → Back to the previous Album.

The central back handler continues to close overlays, playlists, download folders, settings panels, lyrics, queues, and other temporary screens before leaving the current area.

---

## Battery optimization controls

Levyra now includes an optional **Unrestricted background playback** entry in the Playback Resilience settings section.

The setting:

- checks whether Android is currently ignoring battery optimizations for Levyra;
- opens the system exemption screen when requested;
- refreshes its status when returning to the app;
- remains user-initiated instead of forcing a battery prompt during startup.

This is especially useful on devices with aggressive background limits, where the operating system may still stop a correctly implemented foreground playback service.

---

## Download playback cleanup

Starting a downloaded track now uses the same playback initialization path as other tracks.

This ensures that Levyra correctly resets:

- stale lyrics;
- YouTube engagement data;
- comments and replies;
- motion artwork;
- previous recovery jobs;
- old prefetch work;
- active crossfade state.

The player volume is also restored to full level when a previous crossfade is cancelled, preventing downloaded tracks from starting nearly silent.

Downloaded tracks are always forced into audio mode.

---

## Player watchdog improvements

The playback watchdog now treats both `READY` and `BUFFERING` as active playback states.

If playback is expected but the position stops advancing for too long, Levyra can trigger a controlled restoration from the current position.

The watchdog also respects active recovery operations and does not clear persisted playback state while restoration is still in progress.

---

## Media item handling

Media item creation is more flexible for local sources.

- `content://` URIs no longer receive a forced MIME type when the real type is unknown.
- Extensionless `file://` sources can be inspected by Media3.
- Video MIME metadata is only added when a valid value is available.
- Local and online media continue to use the correct playback mode and wake behavior.

Regression coverage has been added for local URI handling.

---

## Localization

The new battery optimization setting has been added across all supported localization bundles.

The release also keeps runtime localization validation intact, ensuring that required keys remain available for every supported language.

---

## Internal stability work

This release includes several structural improvements intended to make future playback work safer:

- background recovery logic split into smaller, clearer functions;
- radio tail loading separated into request, validation, and append stages;
- side-job cancellation consolidated before resolving a new track;
- online and local recovery paths kept separate;
- recovery state reset after healthy playback resumes;
- foreground service state protected during task removal;
- persistent queue restoration reused by sticky playback recovery.

These changes reduce duplicated logic and make failure handling easier to reason about without changing the expected user-facing flow.

---

## Versioning

- Version name: `2.3.15`
- Version code: `2031500`

The version code now correctly matches the 2.3.15 release line.

---

## Validation

The release workflow is expected to validate:

- Android Lint;
- release unit tests;
- release Kotlin compilation;
- signed APK generation;
- release metadata and version consistency;
- duplicate workflow protection;
- APK size reporting.

On-device verification should cover:

- at least 45–60 minutes of screen-off playback;
- online playback through a temporary connection loss;
- playback after Android recreates the service;
- offline playback from downloaded tracks;
- a missing or deleted local file;
- queue advancement while offline and after reconnecting;
- Library smart collection navigation;
- Album → Artist → Album back navigation;
- playback with battery optimization enabled and exempted.

---

## Upgrade notes

No library migration or manual data reset is required.

Existing favorites, playlists, listening history, downloads, queue state, and settings remain compatible with this release.

For the most reliable long-running playback on devices with aggressive battery management, open:

**Settings → Playback Resilience → Unrestricted background playback**

and grant the system exemption when needed.

---

## Final note

Levyra 2.3.15 is less about adding another flashy feature and more about making the features already there behave like they should.

Music should not stop because the screen has been off for a while. A temporary outage should not destroy the playback session. Opening a Library collection should not unexpectedly start a song. Pressing Back should take you somewhere that makes sense.

This release moves Levyra much closer to that standard.
