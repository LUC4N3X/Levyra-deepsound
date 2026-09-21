# Remote announcements

Levyra can display reviewed announcements without requiring a new app release for every message. The single source of truth is:

`app/src/main/assets/config/announcements.json`

This file serves three purposes:

- It is packaged inside the APK as an offline fallback.
- GitHub release builds fetch its latest raw version from `main` to receive new notices.
- F-Droid builds disable both the remote feed and promotional cards at compile time.

No executable code, HTML, JavaScript, or APK fragments are downloaded.

## Publishing a message

1. Edit `app/src/main/assets/config/announcements.json` via a pull request.
2. Assign each message a unique, stable `id`.
3. Provide English text and translations for supported languages.
4. Set `enabled` to `true` when the message is ready.
5. Merge the change. Installed clients typically refresh within 12 hours and cache validated data locally.

The open-source support banner is delayed until the user has opened Levyra at least three times and reached 90 seconds of continuous playback. Dismissing the dialog snoozes it for three days, selecting "later" snoozes it for ten days, and tapping the GitHub link marks the campaign as permanently completed. A permanent support link remains in Settings.

General info and update notices are not subject to the engagement threshold.

To run a revised campaign for users who completed an earlier one, assign a new `id`. To end a message, set `enabled` to `false` or specify an `endAt` timestamp.

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

`maximumVersionCode`, `startAt`, `endAt`, and `actionUrl` are optional. Available styles are `open_source`, `info`, and `update`. `settingsTitle` and `settingsSubtitle` are used by the permanent Settings entry.

## Safety rules

The Android client strictly validates the announcements catalog before display:

- Only schema version 2 is accepted.
- A maximum of 20 announcements are loaded.
- Text lengths and identifier strings are bounded.
- English is required as the fallback language.
- Date formats and version ranges must be valid.
- Action links must use HTTPS and point to an official `github.com/LUC4N3X/...` path.
- Invalid or unreachable remote data never prevents the app from starting.
- The last validated cache is used when offline.
- The bundled fallback catalog is used if no remote cache exists.

The announcement engine collects no telemetry, device identifiers, or interaction tracking. Launch counts, snooze timers, and completion flags remain in local Android preferences.
