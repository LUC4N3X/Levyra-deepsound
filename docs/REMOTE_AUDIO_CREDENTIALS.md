# Updating Android Qobuz and TIDAL credentials

Edit [`catalog/audio-credentials.json`](../catalog/audio-credentials.json) on
`main` and increase its integer `revision`. Keep `schemaVersion` at `1`.
Both provider sections must remain present and valid. Each supports 1–10
accounts/tokens; an empty or malformed section rejects the entire update.

- `qobuz.accounts`: `userAuthToken`, `appId`, `appSecret`, `userId` for each account.
- `tidal.refreshTokens`: ordered refresh-token list.
- `tidal.tvClientId`, `tvClientSecret`, `mobileClientId`: matching client values.

The JSON initially mirrors the existing embedded credentials. Updating only
`QobuzCredentials.kt` or `TidalCredentials.kt` changes the next build's fallback;
it does not update installed apps. Use the JSON for remote changes and update
embedded fallbacks separately when preparing a release.

Android checks in the background at startup and during provider use, at most
once per hour after a valid response. A failed fetch can retry after one minute.
Authentication failures allow another refresh, limited to once per minute for
both providers together. GitHub/CDN propagation can delay a published update.
No exact delivery time is guaranteed, and the app must have network access.

Only a newer, structurally valid revision replaces the complete account sets.
To undo a change, restore the intended values with a higher revision number.
The last valid JSON is saved atomically in app-private, non-backed-up storage.
Offline first installs use the embedded credentials. Remote failures preserve
the last valid configuration, without restoring removed accounts from older
configurations. Structural validity does not establish subscription validity;
test replacement credentials before publishing them.

Qobuz tries each distinct account once, then can try newly fetched credentials.
TIDAL retains its access-token cache and in-memory refresh-token rotation. A
TIDAL configuration change invalidates its existing sessions; a Qobuz-only
change leaves them intact. Server errors and rate limits do not rotate accounts.

The download uses a fixed HTTPS GitHub raw URL, no redirects, a 64 KiB limit and
a two-second call timeout. Provider authentication headers are never attached to
the configuration request. Credential payloads and headers are not logged.
The document follows the repository's existing public credential model: it is
publicly readable and must contain only values authorized for that publication.

One initial APK update containing this feature is required. Older APKs cannot
read the remote configuration. This change applies to Android only.
