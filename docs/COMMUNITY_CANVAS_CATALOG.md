# Community canvas catalog

Levyra's `community-canvas` provider is an optional, read-only motion-artwork source. It maps a
recording to a short looping video and sends every candidate through the shared matcher and URL
verifier before rendering it. Catalog data never controls arbitrary destinations: media must use
HTTPS, an approved host and an MP4 or HLS path.

## Runtime lookup order

The provider uses three layers:

| Order | Source | Purpose |
|:--|:--|:--|
| 1 | `canvas-data/catalog/index/v2` | Hash-sharded Levyra index used for normal lookups |
| 2 | `canvas-data/catalog/community-canvas.json` | Bounded compatibility snapshot used during rollout or index failure |
| 3 | `vivizzz007/vivimusicanvas@main:canvas.json` | Original upstream fallback |

When the indexed mirror is healthy, the app never downloads the complete catalog. A missing result
in a healthy index is conclusive and does not trigger the flat download. The bounded mirror and
upstream are consulted only when the manifest or every relevant shard is unavailable or invalid.
If one shard fails but another relevant shard produces a valid exact match, Levyra keeps that result.

## Scalable sharded index

The sharded format is designed so the catalog can grow from hundreds to millions of mappings
without increasing APK size and without loading the database into memory on the phone.

For the current track, Android derives at most three canonical lookup keys:

* `i|<ISRC>` when a valid ISRC exists;
* `t|<normalized title>|<normalized artists>|<normalized album>`;
* `a|<normalized artists>|<normalized album>`.

Python and Android use the same artist separators, normalization rules, SHA-256 digest and Base64
URL-safe encoding. CI runs fixed cross-language compatibility vectors before building or publishing
an index, preventing a silent lookup break when either implementation changes.

Each key is hashed with SHA-256. The complete digest is encoded as unpadded Base64 URL-safe text and
stored in a compact row. A short hexadecimal prefix selects one shard. The app therefore fetches:

1. one small manifest, cached for six hours;
2. zero to three shards that can contain the current recording;
3. no other catalog data.

The manifest contains:

* a bitset of existing prefixes, avoiding pointless 404 requests;
* a `contentDigest` identifying the exact index generation;
* an immutable `shardDirectory` such as `g8fac2af10c9a06ad/p2`;
* counts and the maximum generated shard size.

Shards are cached in an eight-entry in-memory LRU for six hours, so albums and queues normally reuse
already downloaded data. The cache key includes the content digest and immutable directory, so a
new manifest cannot reuse rows from an older generation.

`scripts/build_community_canvas_index.py` selects between two and five hexadecimal prefix
characters. It increases the depth automatically until every generated shard is below the 96 KiB
target, with a hard 192 KiB publication limit. More catalog entries create more server-side shards,
not a larger APK or a full-catalog client download.

The compact shard row intentionally omits title, artist and album text. The exact SHA-256 lookup
identifies the requested recording, and the candidate identity is reconstructed from the track
already playing. A row only needs the digest, media URL, scope and optional ISRC/dimensions.

This architecture supports millions of mappings on the client. Actual real-canvas coverage still
depends on how many legitimate canvas sources and curated entries are supplied; the index removes
the client-side scaling limit but does not invent missing artist videos.

## Universal local artwork fallback

Levyra keeps a real verified canvas as the preferred result. When no real canvas exists, the network
is unavailable, or a selected video errors or fails to render its first frame within six seconds,
the player animates the existing album artwork locally instead of leaving it completely static.

The fallback uses a subtle Ken Burns-style movement:

* slow scale, translation and fractional rotation over a 12-second reversible cycle;
* active only while the track is playing;
* stopped immediately when playback is paused;
* disabled in Android power-save mode and on low-RAM devices;
* independent of network access and background-data permission.

It does not generate, download, cache or bundle an additional video. It reuses the artwork already
on screen, so it adds no catalog assets and no meaningful APK-size growth. This guarantees visible
motion for unsupported recordings while preserving real artist-provided canvases whenever one is
available.

## Immutable generation publishing

Every content generation receives its own directory:

```text
catalog/index/v2/g<first-16-hex-of-contentDigest>/p<prefixChars>/shards/<prefix>.json
```

The manifest requires its directory generation to match its own `contentDigest`. New publications
never overwrite files referenced by an older manifest. A device that cached the previous manifest
can therefore complete its six-hour cache window and still retrieve the exact matching shards.

The manifest is the only mutable pointer. It and the newly generated directory are committed in one
Git transaction. Old generation directories remain available for compatibility; current clients
cannot reach them after refreshing the manifest.

## Multi-source aggregation

Catalog growth is configured in `catalog/community-canvas-sources.json`. A source can be:

* a repository-local JSON file declared with `path`;
* an HTTPS JSON catalog declared with `url`;
* required, which fails publication when unavailable;
* optional, which is recorded as unavailable while healthy sources continue.

The repository currently defines:

1. `catalog/community-canvas-extra.json`, the Levyra-curated overlay;
2. the existing upstream community catalog.

Adding another reviewed source or importing thousands of validated entries into the curated overlay
requires only a data/workflow change. No Android release is needed because the next scheduled mirror
run merges, validates, deduplicates and rebuilds every shard.

Exact duplicate rows are discarded across sources. Different approved media URLs for the same
recording remain separate candidates so the normal verifier and ranking path can choose a playable
one. Any media host outside the allowlist fails the run instead of silently broadening the app's
network permissions.

## Full build catalog and compatibility snapshot

CI creates a complete normalized catalog only as a temporary build input and workflow artifact. It
is used to generate every shard but is not committed to `canvas-data`. This avoids GitHub's single
file limits when the collection grows very large.

For older Levyra builds and index failure recovery, the workflow separately creates
`catalog/community-canvas.json`. This compatibility snapshot:

* is hard-capped at 900 KiB, below Android's 1 MiB response limit;
* always contains at least 100 valid entries or publication fails;
* records `fullEntryCount` so its partial nature is explicit;
* remains schema version 1 for clients introduced by the previous mirror PR.

With today's 187 entries the snapshot contains the entire collection. As the full index grows, only
the bounded compatibility layer is truncated; current clients continue to query all indexed rows.

A compatibility snapshot looks like this:

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
  "fullEntryCount": 187,
  "items": [
    {
      "song": "Dracula",
      "artist": "Tame Impala",
      "album": "Deadbeat",
      "url": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/1.mp4"
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
| `scope` | no | `track`, `song` or `album`. Anything else is treated as absent. |
| `isrc` | no | Must match `^[A-Z]{2}[A-Z0-9]{3}[0-9]{7}$`. |
| `width` / `height` | no | Positive integers, published together. |

Unknown fields are ignored. An incompatible index format must bump its version so older clients
fall back safely.

## Scope resolution

An album canvas is one video shared by a whole release; a track canvas belongs to one recording.
Scope is resolved in this order:

1. A recognized declared `scope` is authoritative.
2. Without a declared scope, `/Album/` and `/Song/` in the media path are used as hints.
3. Otherwise the entry is treated as track-scoped.

A path-inferred `/Album/` entry keeps its exact track lookup and also receives an album lookup. The
same URL repeated across at least two songs of the same artist and album also produces one album
lookup. Album inference examines every row in the group rather than depending on source order. Album
rows never carry the listed track's ISRC, so sibling tracks are not rejected.

## Security and resource limits

The normalizer and Android parser both enforce the same media-host allowlist. Adding a host requires
reviewing and changing both:

* `COMMUNITY_MEDIA_HOSTS` in `CommunityCanvasProvider.kt`;
* `ALLOWED_HOSTS` in `scripts/sync_community_canvas.py`.

Other limits:

* each configured source is capped at 256 MiB during CI ingestion;
* the published compatibility snapshot is capped at 900 KiB;
* Android caps the legacy flat response at 1 MiB;
* the index manifest is capped at 256 KiB;
* each index shard is capped at 192 KiB;
* index lookup has a 4.5 s total budget;
* legacy mirror/upstream fallback has a separate 6 s total budget;
* all OkHttp requests are cancellable with their coroutine.

The client caches parsed manifests, shards and fallback entries, never both raw HTTP responses.
There is no clock-based freshness check because publication intentionally skips commits when only
`generatedAt` changes.

## Mirror pipeline

`.github/workflows/community-canvas-mirror.yml` runs on relevant pull requests, daily at 04:37 UTC
and on demand. It:

1. verifies Python/Android lookup compatibility vectors;
2. loads every configured source;
3. validates, merges and normalizes the full collection;
4. creates the bounded compatibility snapshot;
5. builds the compact hash-sharded index from the complete collection;
6. verifies the manifest, immutable generation path, file count and size limits;
7. publishes the bounded fallback, manifest and new generation together to `canvas-data`.

The publisher compares both the compatibility snapshot and manifest without `generatedAt`, so a
pure timestamp change creates no commit while an index-builder or content change is not skipped.

## Local commands

Normalize all configured sources, create the bounded compatibility snapshot and build the index:

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

Normalize a local fixture instead:

```bash
python3 scripts/sync_community_canvas.py \
  --input canvas.json \
  --output build/canvas/community-canvas-full.json \
  --compat-output build/canvas/community-canvas.json \
  --min-entries 1 \
  --compat-min-entries 1
```

Run the Android compatibility tests and release checks:

```bash
./gradlew --no-daemon :app:testReleaseUnitTest :app:lintRelease :app:assembleRelease
```
