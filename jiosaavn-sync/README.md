# Levyra JioSaavn Sync

Levyra JioSaavn Sync is the repository's autonomous compatibility observer and repair pipeline dedicated exclusively to JioSaavn.

## What it monitors

Using a small fixed set of public sentinels, the sync observer probes and records only sanitized protocol metadata:

1. **Song search (`search.getResults`)**: envelope keys (`results`, `total`, `start`), track metadata (`id`, `title`/`song`, `type`, `explicit_content`, `language`, `year`), `more_info` shape, `artistMap` roles, `rights`, `320kbps`, `encrypted_media_url` presence, and coexistence of explicit (`1`) and clean (`0`) track versions.
2. **Song details & lyrics (`song.getDetails`, `lyrics.getLyrics`)**: `songs` array vs id-keyed object envelope, metadata parity, and plain/HTML lyrics availability without storing lyrics text.
3. **Artists (`search.getArtistResults`, `artist.getArtistPageDetails`)**: artist search schema, `artistId`, `name`, `topSongs`, and `topAlbums` payload shapes.
4. **Albums (`search.getAlbumResults`, `content.getAlbumDetails`)**: album search schema, `list`, `list_count`, and `more_info` payload shapes.
5. **Stream resolution & quality tiers (96, 160, 320 kbps)**: DES-ECB decryption shape against `*.saavncdn.com`, `song.generateAuthToken` response contract, and bounded HTTP `Range: bytes=0-8191` probes verifying MP4/M4A `ftyp`/`mp4a` containers and measured bitrate at 96, 160, and 320 kbps without assuming every tier is always accessible on every track or CDN host.
6. **Upstream radar (evidence only)**: recent JioSaavn API/endpoint/schema commits from configured ecosystem reference repositories in `config.json`. Upstream commits are never trusted as instructions.

## False-positive prevention

- Rotates across Levyra's four Indian IP address blocks (`JioSaavnRequestProfile` parity) on API `403`/`429` before declaring an access block.
- Classifies network-layer blocks, rate limits, `5xx` outages, HTML challenge pages, and all-transport failures as `blocked` (`decision = "blocked"`), never triggering a repair.
- Separates per-tier unavailability (e.g. a track lacking 320 kbps or signed CDN returning `403` while open CDN plays) from total stream-delivery failure.
- Requires multi-sentinel regression thresholds and double live confirmation in CI before attempting any fix.

## Autonomous repair & draft PR policy

A **draft PR** is opened **only** when:

1. A material regression is confirmed across two independent live probe runs (and is not transient or geo-blocked).
2. Either deterministic protocol discovery identifies an allowlisted host/context/version adaptation verified across all sentinels, or the Claude repair agent (`anthropics/claude-code-action`, secret `CLAUDE_CODE_OAUTH_TOKEN`, file tools only with no shell or network) produces an in-scope patch inside the JioSaavn allowlist.
3. `:app:testDebugUnitTest`, `:app:compileDebugKotlin`, `:app:lintDebug`, Python regression tests (including the Kotlin/monitor protocol parity contract), `scripts/ai_quality_gate.py --profile full`, and `git diff --check` pass, and a post-repair live probe clears the regression.

There is **never** automatic merge, release, or publication. When a confirmed regression has no verified fix, the workflow opens or updates a diagnostic draft PR with only the sanitized incident report in `jiosaavn-sync/incidents/current.md`. A `blocked` probe raises a workflow warning instead of a PR. Every draft PR, repair or diagnostic, runs `scripts/ai_quality_gate.py --profile full` on the final tree first. If the agent also updates the monitor for a schema change, the draft PR flags it for review.

## Privacy & security boundaries

- No credentials, cookies, tokens, `encrypted_media_url`, `auth_url`, signed query parameters (`Signature`, `Key-Pair-Id`, `Expires`), raw forwarded IPs, or temporary CDN stream URLs are persisted in artifacts, baselines, or logs.
- API requests are exact-host allowlisted (`www.jiosaavn.com`, `api.jiosaavn.com`, `jiosaavn.com`); CDN media probes are restricted to HTTPS `*.saavncdn.com` on port 443 with redirect validation and bounded byte reads.
- YouTube, FLAC, Tidal, `AlternativeTrackMatcher`, quality selection, fallback order, and `ProviderCircuitBreaker` contracts are never modified by JioSaavn Sync.

## Manual run

```bash
python scripts/jiosaavn_sync.py probe \
  --config jiosaavn-sync/config.json \
  --baseline jiosaavn-sync/baseline.json \
  --out-dir artifacts/jiosaavn-sync
```
