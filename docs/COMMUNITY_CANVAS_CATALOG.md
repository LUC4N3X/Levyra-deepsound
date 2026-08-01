# Community canvas catalog

Levyra's `community-canvas` provider is an optional, read-only motion-artwork source. It reads a
JSON catalog that maps a recording to a short looping video, then hands the winning URL to the
shared motion-artwork verifier before anything is rendered. The catalog never controls playback,
never carries audio, and never selects a destination on its own: every media URL must survive the
host allowlist, the MIME probe and the redirect checks in `MotionArtworkUrlVerifier`.

## Sources

The provider tries two catalogs in order and keeps the first one that parses into at least one
usable entry:

| Order | Source | Purpose |
|:--|:--|:--|
| 1 | `LUC4N3X/Levyra-deepsound@canvas-data:catalog/community-canvas.json` | Validated Levyra mirror |
| 2 | `vivizzz007/vivimusicanvas@main:canvas.json` | Upstream community catalog |

The mirror is the pinned copy Levyra controls. Upstream stays as a fallback so the feature keeps
working when the mirror branch is missing, unreachable or structurally unusable. Both responses are
capped at 1 MiB and cached in memory for six hours.

The mirror is only accepted when it declares `version: 1` and yields at least 100 usable entries,
which is below the 150-entry floor the publishing pipeline enforces. A truncated or gutted mirror
therefore falls through to upstream instead of being cached for six hours. Upstream itself is
accepted whenever it yields at least one usable entry, because nothing else backs it up.

There is no freshness check: neither `generatedAt` nor the branch commit date is compared against
the clock, and a mirror that stops being refreshed keeps being served. That is deliberate — the
pipeline skips the commit when only `generatedAt` changed, so the timestamp does not track staleness
— but it means an abandoned mirror is served until it fails one of the structural checks above.

The whole two-source attempt is bounded by a 6 s budget that sits inside the motion-artwork engine's
`MotionArtworkConfig.requestTimeoutMs` (6.5 s by default). Each source gets the remaining budget
divided by the number of sources left, with a 2 s floor, so a slow mirror cannot starve the upstream
fallback. Fetches run on `Call.enqueue` inside `suspendCancellableCoroutine`, so cancelling the
coroutine cancels the HTTP call instead of leaving a blocked thread holding the catalog lock.

## Entry schema

```json
{
  "version": 1,
  "generatedAt": "2026-07-31T04:37:00Z",
  "source": "https://raw.githubusercontent.com/vivizzz007/vivimusicanvas/main/canvas.json",
  "items": [
    {
      "song": "Dracula",
      "artist": "Tame Impala",
      "album": "Deadbeat",
      "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/1.mp4",
      "scope": "track",
      "isrc": "AUUM72500123",
      "width": 720,
      "height": 1280
    }
  ]
}
```

Top-level `version`, `generatedAt` and `source` are mirror-only metadata. The parser ignores unknown
keys, so the upstream shape (`items` alone) is accepted unchanged.

| Field | Required | Notes |
|:--|:--|:--|
| `song` | yes | Recording title. Blank entries are dropped. |
| `artist` | yes | Split on the usual separators before matching. |
| `album` | yes | Blank entries are dropped. |
| `url` | yes | HTTPS, port 443, allowlisted host, `.mp4` or `.m3u8`. |
| `scope` | no | `track`, `song` or `album`. Anything else is treated as absent. |
| `isrc` | no | Must match `^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$`; malformed values are discarded instead of blocking the match. |
| `width` / `height` | no | Positive integers, published together. |

`scope`, `isrc`, `width` and `height` are optional extensions. Upstream does not emit them yet, and
the mirror only passes through what upstream publishes, so today every mirrored entry carries the
four required fields alone. The parser and the mirror already accept the extended form so the
catalog can adopt it without an app release.

## Scope resolution

An album canvas is one video shared by a whole release; a track canvas belongs to a single
recording. The two are matched differently: an album-scope candidate is scored on album similarity
and rejected below 0.82, while a track-scope candidate is scored mostly on title similarity.

Levyra resolves the scope in this order:

1. A recognized `scope` field wins and is authoritative.
2. Otherwise the URL directory is used as a hint: upstream stores album canvases under `/Album/`
   and track canvases under `/Song/`, and their PR validator enforces that layout.
3. Otherwise the entry is treated as a track canvas.

The directory hint is additive. An entry under `/Album/` keeps its track-scope form *and* gains an
album-scope variant, so a canvas listed once can also cover the rest of the release without losing
the exact-title match it already had. The older heuristic — the same URL repeated across two or
more songs implies an album canvas — still applies on top of both.

## Mirror pipeline

`.github/workflows/community-canvas-mirror.yml` runs daily at 04:37 UTC (06:37 Europe/Rome in
summer, 05:37 in winter) and on demand. It normalizes upstream with
`scripts/sync_community_canvas.py` and publishes the result to the orphan `canvas-data` branch,
skipping the commit when only `generatedAt` changed.

The normalizer refuses to publish when:

* upstream is unreachable, oversized or not valid JSON;
* fewer than `--min-entries` usable entries survive (150 in CI, against 187 upstream entries today);
* any entry points at a host outside the allowlist.

The last point is the drift alarm. A new upstream host fails the run and leaves the previous
snapshot in place, so the app keeps serving a known-good catalog until the host is reviewed and
added to **both** allowlists:

* `COMMUNITY_MEDIA_HOSTS` in `app/src/main/java/com/luc4n3x/levyra/feature/motion/CommunityCanvasProvider.kt`
* `ALLOWED_HOSTS` in `scripts/sync_community_canvas.py`

Adding a host to only one of them either breaks the sync or ships a catalog the app silently
discards.

## Running the normalizer locally

```bash
python3 scripts/sync_community_canvas.py --output build/canvas/community-canvas.json
python3 scripts/sync_community_canvas.py --input canvas.json --output build/canvas/community-canvas.json
```

`--input` skips the network and normalizes a local copy, which is the fastest way to check a
catalog change before it reaches upstream.
