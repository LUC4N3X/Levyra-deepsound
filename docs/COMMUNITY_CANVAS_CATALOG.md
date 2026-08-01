# Community canvas catalog

Levyra's `community-canvas` provider is an optional, read-only motion-artwork source. It maps a
recording to a short looping video and passes every candidate through the shared motion-artwork
matcher and URL verifier before rendering it. Catalog data never controls arbitrary destinations:
media must use HTTPS, an approved host and an MP4 or HLS path.

## Runtime lookup order

The provider now uses three layers:

| Order | Source | Purpose |
|:--|:--|:--|
| 1 | `canvas-data/catalog/index/v2` | Hash-sharded Levyra index used for normal lookups |
| 2 | `canvas-data/catalog/community-canvas.json` | Flat validated mirror used only as rollout/failure fallback |
| 3 | `vivizzz007/vivimusicanvas@main:canvas.json` | Original upstream fallback |

When the indexed mirror is healthy, the app never downloads the full catalog. A missing result in a
healthy index is conclusive and does not trigger the legacy flat download. The flat mirror and
upstream are only consulted when the manifest or a required shard is unavailable or invalid.

## Scalable sharded index

The sharded format is designed so the catalog can grow from hundreds to millions of mappings
without increasing APK size and without loading the database into memory on the phone.

For the current track, Android derives at most three canonical lookup keys:

* `i|<ISRC>` when a valid ISRC exists;
* `t|<normalized title>|<normalized artists>|<normalized album>`;
* `a|<normalized artists>|<normalized album>`.

Each key is hashed with SHA-256. The complete digest is encoded as unpadded Base64 URL-safe text and
stored in a compact row. A short hexadecimal prefix selects one shard. The app therefore fetches:

1. one small manifest, cached for six hours;
2. zero to three shards that can contain the current recording;
3. no other catalog data.

The manifest contains a bitset of existing shard prefixes. That lets the client prove that a shard
does not exist without making a 404 request. Shards are cached in an eight-entry in-memory LRU for
six hours, so albums and queues normally reuse already downloaded data.

`scripts/build_community_canvas_index.py` selects between two and five hexadecimal prefix
characters. It increases the depth automatically until every generated shard is below the 96 KiB
target, with a hard 192 KiB publication limit. More catalog entries create more server-side shards,
not a larger APK or a full-catalog client download.

The compact shard row intentionally omits title, artist and album text. The exact SHA-256 lookup
identifies the requested recording, and the candidate identity is reconstructed from the track
already playing. A row only needs the digest, media URL, scope and optional ISRC/dimensions.

This architecture supports millions of mappings. Actual coverage still depends on how many valid
canvas sources and curated entries are supplied; the index removes the client-side scaling limit but
does not invent missing videos.

## Multi-source aggregation

Catalog growth is configured in `catalog/community-canvas-sources.json`. Sources are processed in
listed order, so a curated Levyra entry can intentionally win before a duplicate from an external
catalog.

A source can be:

* a repository-local JSON file declared with `path`;
* an HTTPS JSON catalog declared with `url`;
* required, which fails publication when unavailable;
* optional, which is recorded as unavailable while healthy sources continue.

The repository currently defines:

1. `catalog/community-canvas-extra.json`, the Levyra-curated overlay;
2. the existing upstream community catalog.

Adding another reviewed source or importing thousands of validated entries into the curated overlay
requires only a data/workflow change. No Android release is needed because the next scheduled mirror
run rebuilds the flat snapshot and every shard.

All sources are merged, normalized, deduplicated and sorted. Any media host outside the allowlist
fails the run instead of silently broadening the app's network permissions.

## Flat entry schema

The normalized flat snapshot remains available for compatibility:

```json
{
  "version": 1,
  "generatedAt": "2026-08-01T04:37:00Z",
  "sources": [
    {
      "name": "vivimusicanvas",
      "location": "https://raw.githubusercontent.com/vivizzz007/vivimusicanvas/main/canvas.json",
      "required": true,
      "entries": 187,
      "status": "ok"
    }
  ],
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

| Field | Required | Notes |
|:--|:--|:--|
| `song` | yes | Recording title. Blank entries are dropped. |
| `artist` | yes | Used to build the canonical artist signature. |
| `album` | yes | Blank entries are dropped. |
| `url` | yes | HTTPS, port 443, allowlisted host, `.mp4` or `.m3u8`. |
| `scope` | no | `track`, `song` or `album`. Anything else is treated as absent. |
| `isrc` | no | Must match `^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$`. |
| `width` / `height` | no | Positive integers, published together. |

Unknown fields are ignored. The mirror version match is exact: optional fields do not require a
bump, while an incompatible schema must bump the version so older clients fall back safely.

## Scope resolution

An album canvas is one video shared by a whole release; a track canvas belongs to one recording.
Scope is resolved in this order:

1. A recognized declared `scope` is authoritative.
2. Without a declared scope, `/Album/` and `/Song/` in the media path are used as hints.
3. Otherwise the entry is treated as track-scoped.

A path-inferred `/Album/` entry keeps its exact track lookup and also receives an album lookup. The
same URL repeated across at least two songs of the same artist and album also produces one album
lookup. Album rows never carry the listed track's ISRC, so sibling tracks are not rejected.

## Security and resource limits

The normalizer and Android parser both enforce the same media-host allowlist. Adding a host requires
reviewing and changing both:

* `COMMUNITY_MEDIA_HOSTS` in `CommunityCanvasProvider.kt`;
* `ALLOWED_HOSTS` in `scripts/sync_community_canvas.py`.

Other limits:

* each configured source is capped at 64 MiB during CI ingestion;
* Android caps the legacy flat response at 1 MiB;
* the index manifest is capped at 256 KiB;
* each index shard is capped at 192 KiB;
* index lookup has a 4.5 s total budget;
* legacy mirror/upstream fallback has a separate 6 s total budget;
* all OkHttp requests are cancellable with their coroutine.

The client caches only parsed results, never both HTTP responses. There is no clock-based freshness
check because publication intentionally skips commits when only `generatedAt` changes.

## Mirror pipeline

`.github/workflows/community-canvas-mirror.yml` runs on relevant pull requests, daily at 04:37 UTC
and on demand. It:

1. loads every configured source;
2. validates, merges and normalizes entries;
3. enforces the minimum usable-entry count and host drift alarm;
4. builds the compact hash-sharded index;
5. verifies the manifest and the number/size of generated shards;
6. publishes the flat fallback and index together to the orphan `canvas-data` branch.

The publish is one Git commit, so clients never observe a manifest pointing at a partially uploaded
set of shards.

## Local commands

Normalize all configured sources and build the index:

```bash
python3 scripts/sync_community_canvas.py \
  --sources-file catalog/community-canvas-sources.json \
  --output build/canvas/community-canvas.json

python3 scripts/build_community_canvas_index.py \
  --input build/canvas/community-canvas.json \
  --output-dir build/canvas/index
```

Normalize a local fixture instead:

```bash
python3 scripts/sync_community_canvas.py \
  --input canvas.json \
  --output build/canvas/community-canvas.json \
  --min-entries 1
```

Run the Android compatibility tests and release checks:

```bash
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```
