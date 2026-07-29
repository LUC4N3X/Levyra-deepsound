# Remote announcements

Levyra can display small, one-time announcements without requiring a new APK for every message. The single source of truth is:

`app/src/main/assets/config/announcements.json`

The same reviewed JSON file serves two purposes:

- it is packaged inside the APK as the offline and first-fetch fallback;
- published clients read its raw version from the repository's `main` branch for future announcements.

No executable code, HTML, JavaScript or APK fragments are downloaded.

## Publishing a message

1. Edit `app/src/main/assets/config/announcements.json` through a reviewed pull request.
2. Give every new message a unique, stable `id`.
3. Add an English translation and any other supported languages.
4. Set `enabled` to `true` when the message is ready.
5. Merge the configuration change. Installed clients normally refresh within 12 hours and also keep a validated local cache.

Each message is displayed once per installation. To show a revised campaign again, publish it with a new `id`. To stop a campaign, set `enabled` to `false` or add an `endAt` value.

Changes made after an APK is published are delivered remotely. The packaged copy changes only when a new APK is built, so the app always retains a known-good fallback even when the network or the remote catalog is unavailable.

## Supported schema

```json
{
  "schemaVersion": 1,
  "announcements": [
    {
      "id": "unique-message-id",
      "enabled": true,
      "priority": 50,
      "style": "info",
      "minimumVersionCode": 1,
      "maximumVersionCode": 9999999,
      "startAt": "2026-07-29T00:00:00Z",
      "endAt": "2026-08-31T23:59:59Z",
      "actionUrl": "https://github.com/LUC4N3X/Levyra-deepsound",
      "translations": {
        "en": {
          "badge": "NOTICE",
          "title": "Title",
          "body": "Message body",
          "action": "Open",
          "dismiss": "Continue"
        }
      }
    }
  ]
}
```

`maximumVersionCode`, `startAt`, `endAt` and `actionUrl` are optional. The available styles are `open_source`, `info` and `update`.

## Safety rules

The Android client validates the complete catalog before using it:

- only schema version 1 is accepted;
- no more than 20 announcements are accepted;
- IDs and text lengths are bounded;
- English is required as the fallback language;
- dates and Android version ranges must be valid;
- action links must use HTTPS and point to an official `github.com/LUC4N3X/...` path;
- invalid or unavailable remote data never prevents the app from starting;
- the last validated remote catalog is used when the network is unavailable;
- the packaged catalog remains available if no validated remote catalog exists.

The engine does not collect analytics, device identifiers, star status or interaction telemetry.
