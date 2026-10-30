# Remote Qobuz and TIDAL credentials

Android reads `catalog/audio-credentials.json` from this repository's `main`
branch over HTTPS. Editing this JSON updates installed clients that include this
feature; existing APKs still need one update. Embedded credentials remain the
first-install fallback. Desktop is outside this change.

## Approach

A repository JSON document reuses existing hosting and avoids an additional
service. Parsing Kotlin source remotely would couple clients to source syntax;
a separate backend would introduce unnecessary infrastructure. No remote code
is downloaded or executed.

The versioned document contains complete Qobuz and TIDAL credential sets.
Validated updates replace both sets atomically and are cached in app-private,
non-backed-up storage. Invalid, oversized, outdated or unreachable documents
leave the last valid snapshot intact. Each provider permits 1–10 credentials.

Startup and active use schedule background refreshes, at most once per hour.
Authentication rejection permits an immediate refresh, rate limited to once per
minute. Requests only target the fixed GitHub raw URL, reject redirects, cap
responses at 64 KiB and use a two-second call timeout. Tokens are never logged.

Qobuz tries each account at most once per request and can retry newly downloaded
accounts after rejection. TIDAL keeps access-session caching and refresh-token
rotation, and invalidates old sessions when its credential set changes. Server
errors, rate limiting and cancellation do not trigger credential rotation.

## Acceptance

- Cached credentials survive process restart without network access.
- A valid new revision becomes usable without reinstalling the app.
- Invalid updates preserve working credentials and cancellation propagates.
- Concurrent refreshes are coalesced and rejected tokens cannot cause loops.
- Existing providers, FLAC validation, downloads and fallback ownership remain.
