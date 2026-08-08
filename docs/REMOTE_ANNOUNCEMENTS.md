# Remote announcements

Levyra can display small, reviewed announcements without requiring a new APK for every message. The single source of truth is:

`app/src/main/assets/config/announcements.json`

The same JSON file serves these purposes:

- it is packaged inside the APK as the offline and first-fetch fallback;
- upstream-published clients read its raw version from the repository's `main` branch for future announcements;
- F-Droid builds disable both the remote feed and its support card at compile time.

No executable code, HTML, JavaScript or APK fragments are downloaded.

## Publishing a message

1. Edit `app/src/main/assets/config/announcements.json` through a reviewed pull request.
2. Give every new message a unique, stable `id`.
3. Add an English translation and every supported app language.
4. Set `enabled` to `true` when the message is ready.
5. Merge the configuration change. Installed clients normally refresh within 12 hours and keep a validated local cache.

The open-source support campaign is intentionally delayed until the user has opened Levyra at least three times and has reached a positive listening moment through recent listens or 90 seconds of actual elapsed playback. Seeking forward or restoring a saved media position does not qualify. Closing the dialog or pressing Back snoozes it for three days. The explicit “later” action snoozes it for ten days. Opening the GitHub action completes the campaign for that installation. A permanent support entry remains available in Settings.

Other `info` and `update` announcements are not blocked by the support campaign's engagement threshold. Snoozing one campaign also does not suppress another eligible lower-priority announcement.

To show a substantially revised campaign to people who completed an older one, publish it with a new `id`. To stop a campaign, set `enabled` to `false` or add an `endAt` value.

Changes made after an APK is published are delivered remotely. The packaged copy changes only when a new APK is built, so the app always retains a known-good fallback when the network or remote catalog is unavailable.

## Supported schema

```json
{
  "schemaVersion": 2,
  "announcements": [
    {
      "id": "unique-message-id",
      "enabled": true,
      "priority": 50,
      "style": "info",
      "minimumVersionCode": 1,
      "maximumVersionCode": 9999999,
      "startAt": "2026-08-03T00:00:00Z",
      "endAt": "2026-08-31T23:59:59Z",
      "actionUrl": "https://github.com/LUC4N3X/Levyra-deepsound",
      "translations": {
        "en": {
          "badge": "NOTICE",
          "title": "Title",
          "body": "Message body",
          "action": "Open",
          "dismiss": "Maybe later",
          "settingsTitle": "Support Levyra on GitHub",
          "settingsSubtitle": "Leave a star to help more people discover the project."
        }
      }
    }
  ]
}
```

`maximumVersionCode`, `startAt`, `endAt` and `actionUrl` are optional. The available styles are `open_source`, `info` and `update`. `settingsTitle` and `settingsSubtitle` are used by the permanent Settings entry; older catalogs without them fall back to the action (or the announcement title when no action exists) and body copy.

## Safety rules

The Android client validates the complete catalog before using it:

- only schema version 2 is accepted;
- no more than 20 announcements are accepted;
- IDs and text lengths are bounded;
- English is required as the fallback language;
- dates and Android version ranges must be valid;
- action links must use HTTPS and point to an official `github.com/LUC4N3X/...` path;
- invalid or unavailable remote data never prevents the app from starting;
- the last validated remote catalog is used when the network is unavailable;
- the packaged catalog remains available if no validated remote catalog exists.

The engine does not collect analytics, device identifiers, GitHub star status or interaction telemetry. Launch count, snooze timing and completion state remain local in Android preferences.
