# Levyra Canvas catalog

Levyra's `community-canvas` provider is an optional, read-only motion-artwork source. It maps a recording to a short looping video and sends candidate URLs through a shared matcher and URL verifier before rendering. Media must use HTTPS, an approved host, and an MP4 or HLS path.

## Runtime lookup order

The provider uses two repository-owned layers:

| Order | Source | Purpose |
|:--|:--|:--|
| 1 | `canvas-data/catalog/index/v2` | Hash-sharded index used for normal lookups |
| 2 | `canvas-data/catalog/community-canvas.json` | Bounded compatibility snapshot used during rollout or index failure |

When the indexed mirror is healthy, the app never downloads the full catalog. A missing entry in a healthy index is conclusive and does not trigger the fallback file. The snapshot is consulted only when the manifest or relevant shards are unreachable or invalid. If one shard fails but another produces an exact match, Levyra keeps that result. Tracks that cannot produce an index key also query the snapshot instead of being treated as a conclusive miss.

In automatic mode, provider priority is strictly ordered: Apple Music motion artwork first, then Tidal `videoCover`, then Levyra's curated Spotify Canvas catalog, followed by static local fallback. The internal ID `community-canvas` represents this curated Spotify catalog published and sanitized by Levyra.

Android never queries a third-party catalog directly. Levyra consumes only the sanitized Spotify catalog published by its editorial workflow, validating and republishing it under the repository-owned `canvas-data` branch. A third-party provider outage cannot redirect installed clients to an unverified branch.

In the stacked fullscreen player, a resolved Canvas serves as the edge-to-edge visual layer behind playback controls. The muted decorative Media3 player is reused, while static artwork remains immediate and gains a subtle motion effect when no video is ready. Side-by-side and native video modes retain their existing layout surfaces.

## Scalable sharded index

The sharded format is designed so the catalog can scale from hundreds to millions of mappings without increasing APK size or loading large JSON files into phone memory.

For the current track, Android derives at most three canonical lookup keys:

- `i|<ISRC>` when a valid ISRC exists
- `t|<normalized title>|<normalized artists>|<normalized album>`
- `a|<normalized artists>|<normalized album>`

Python scripts and Android use the same artist separators, normalization rules, SHA-256 digest, and Base64 URL-safe encoding. CI runs cross-language compatibility vectors before building or publishing an index to prevent silent breaks.

Each key is hashed with SHA-256. The complete digest is encoded as unpadded Base64 URL-safe text and stored in a compact row. A short hexadecimal prefix selects one shard. The app fetches:

1. One small manifest, cached for six hours
2. Zero to three shards that can contain the current recording
3. No other catalog data

The manifest contains:

- A bitset of existing prefixes to avoid unnecessary 404 requests
- A `contentDigest` identifying the exact index generation
- An immutable `shardDirectory` such as `g8fac2af10c9a06ad/p2`
- Entry counts and the maximum generated shard size

Shards are cached in an eight-entry in-memory LRU for six hours, allowing albums and queues to reuse downloaded data. The cache key includes the content digest and immutable directory, preventing a new manifest from reusing rows from an older generation.

`scripts/build_community_canvas_index.py` selects between two and five hexadecimal prefix characters. It increases depth automatically until every generated shard is below the 96 KiB target, with a hard 192 KiB publication limit. More catalog entries produce more server-side shards rather than a larger APK or full-catalog client download.

The compact shard row omits title, artist, and album text. The SHA-256 lookup matches the requested recording, and candidate identity is verified against the track already playing. A row only needs the digest, media URL, scope, and optional ISRC/dimensions.

## Universal local artwork fallback

Levyra keeps a verified Canvas as the preferred visual. When no Canvas exists, the network is unavailable, or a selected video fails to render its first frame within six seconds, the player animates the existing album artwork locally.

The fallback uses a subtle Ken Burns-style movement:

- Slow scale, translation, and fractional rotation over a 12-second reversible cycle
- Active only while the track is playing
- Stopped immediately when playback is paused
- Disabled in Android power-save mode and on low-RAM devices
- Independent of network connectivity and background-data permissions

It does not generate, download, or cache an additional video. It reuses the artwork already loaded in memory, adding no catalog assets or APK size overhead.

## Immutable generation publishing

Every content generation receives its own directory:

```text
catalog/index/v2/g<first-16-hex-of-contentDigest>/p<prefixChars>/shards/<prefix>.json
```

The manifest requires its directory generation to match its own `contentDigest`. New publications never overwrite files referenced by an older manifest. A device that cached the previous manifest can complete its cache window and retrieve matching shards.

The manifest is the only mutable pointer. It and the newly generated directory are committed in a single Git transaction. Older generation directories remain available for compatibility until clients refresh the manifest.

## Repository-owned aggregation

Catalog sources are configured in `catalog/community-canvas-sources.json`. The current source is the sanitized Spotify Canvas catalog published by Levyra's editorial workflow.

The Spotify source is generated only in GitHub Actions. Tokens and session cookies never enter the catalog, Android build, or logs. Published rows contain only matching text, optional ISRC, and an allowlisted `canvaz.scdn.co` MP4 URL. If the session expires or the private endpoint changes, the editorial workflow preserves the last valid catalog, and the optional source cannot block Levyra, Apple Music, or Tidal fallback paths.

Refreshing the Spotify source requires only a trusted workflow run without requiring an app release. Exact duplicate rows are discarded across sources. Different approved media URLs for the same recording remain separate candidates so the verifier can select a playable stream. Any media host outside the allowlist fails the run immediately.

## Full build catalog and compatibility snapshot

CI creates a complete normalized catalog as a temporary build input. It generates every shard but is not committed to `canvas-data`, avoiding repository file size bloat.

For older Levyra builds and index failure recovery, the workflow creates `catalog/community-canvas.json`. This compatibility snapshot:

- Is capped at 900 KiB, staying well below Android's response limits
- Always contains at least 100 valid entries
- Records `fullEntryCount` so its partial nature is explicit
- Maintains schema version 1 for backward compatibility

A compatibility snapshot looks like this:

```json
{
  "version": 1,
  "generatedAt": "2026-08-01T04:37:00Z",
  "sources": [
    {
      "name": "spotify-editorial-canvas",
      "location": "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/spotify-canvas.json",
      "required": false,
      "entries": 187,
      "status": "ok"
    }
  ],
  "fullEntryCount": 187,
  "items": [
    {
      "song": "Dracula",
      "artist": "Tame Impala",
      "album": "Deadbeat",
      "url": "https://canvaz.scdn.co/upload/artist/video/example.cnvs.mp4"
    }
  ]
}
```

## Source entry schema

| Field | Required | Notes |
|:--|:--|:--|
| `song` | yes | Recording title. Blank entries are dropped. |
| `artist` | yes | Split with the same separators used by Android before hashing. |
| `album` | yes | Blank entries are dropped. |
| `url` | yes | HTTPS, port 443, allowlisted host, `.mp4` or `.m3u8`. |
| `scope` | no | `track`, `song`, or `album`. Other values are treated as absent. |
| `isrc` | no | Must match `^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$`. |
| `width` / `height` | no | Positive integers, published together. |

Unknown fields are ignored. An incompatible index format must bump its version so older clients fall back safely.

## Scope resolution

An album canvas is a single video shared across a whole release, while a track canvas belongs to one recording. Scope is resolved in this order:

1. A recognized declared `scope` is authoritative.
2. Without a declared scope, `/Album/` and `/Song/` in the media path are used as hints.
3. Otherwise the entry is treated as track-scoped.

A path-inferred `/Album/` entry keeps its exact track lookup and also receives an album lookup. The same URL repeated across two or more songs of the same artist and album also produces an album lookup.

## Security and resource limits

The normalizer and Android parser both enforce the same media-host allowlist:

- `COMMUNITY_MEDIA_HOSTS` in `CommunityCanvasProvider.kt`
- `ALLOWED_HOSTS` in `scripts/sync_community_canvas.py`

The only approved Canvas media destination is the exact Spotify CDN host `canvaz.scdn.co`. Subdomains, credentials, query strings, fragments, and non-standard ports are rejected before mirror validation runs.

Other limits:

- Each configured source is capped at 256 MiB during CI ingestion.
- The published compatibility snapshot is capped at 900 KiB.
- Android caps the legacy flat response at 1 MiB.
- The index manifest is capped at 256 KiB.
- Each index shard is capped at 192 KiB.
- Indexed lookups are capped at 4.5 seconds and shorten dynamically to reserve time for catalog fallback.
- Conclusive misses expire after 10 minutes.
- All OkHttp network requests can be cancelled cleanly with their coroutine.

## Mirror pipeline

`.github/workflows/community-canvas-mirror.yml` validates pull requests without needing secrets or write permissions. Publication runs from trusted `main` code, daily at 04:37 UTC or through manual dispatch:

1. Verifies Python and Android lookup compatibility vectors
2. Loads every configured source
3. Validates, merges, and normalizes the full collection
4. Creates the bounded compatibility snapshot
5. Builds the compact hash-sharded index
6. Verifies the manifest, immutable generation path, file count, and size limits
7. Publishes the bounded fallback, manifest, and new generation together to `canvas-data`

The published branch is Levyra's authoritative runtime source. The app has no network fallback to raw source catalogs.

## Local commands

Normalize sources, create the compatibility snapshot, and build the index:

```bash
python3 scripts/sync_community_canvas.py \
  --sources-file catalog/community-canvas-sources.json \
  --output build/canvas/community-canvas-full.json \
  --compat-output build/canvas/community-canvas.json

python3 scripts/build_community_canvas_index.py --self-test

python3 scripts/build_community_canvas_index.py \
  --input build/canvas/community-canvas-full.json \
  --output-dir build/canvas/index
```

Normalize a local fixture:

```bash
python3 scripts/sync_community_canvas.py \
  --input canvas.json \
  --output build/canvas/community-canvas-full.json \
  --compat-output build/canvas/community-canvas.json \
  --min-entries 1 \
  --compat-min-entries 1
```

Run Android compatibility tests:

```bash
./gradlew --no-daemon :app:testDebugUnitTest \
  --tests 'com.luc4n3x.levyra.feature.motion.*'
./gradlew --no-daemon :app:lintRelease :app:assembleRelease
```
