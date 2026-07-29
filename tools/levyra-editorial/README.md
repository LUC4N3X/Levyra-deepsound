# Levyra editorial collector

This repository-owned Python tool reads configured public country rankings with a dedicated source account and publishes a compact, account-free ranking catalog for Levyra.

## Security and data boundaries

- `LEVYRA_EDITORIAL_SP_DC` exists only as a GitHub Actions repository secret.
- The cookie, TOTP material and short-lived token are never written to source, artifacts, logs, JSON or the APK.
- The TOTP dictionary URL is HTTPS-allowlisted and pinned to an immutable commit.
- The public catalog contains ranking position, title, artist, album, release date, duration and explicit flag only.
- Source artwork, source URLs, source IDs and unsupported ISRC values are deliberately omitted.
- Android obtains artwork independently and keeps the exact same artwork when the selected row opens in the player.
- Required country failures block publication. Only collections explicitly marked `optional` may be skipped.
- A failed or incomplete run never replaces the last valid catalog.

The implementation is original Levyra code. SimpMusic was used only as a behavioral reference for the current `sp_dc` plus TOTP session exchange; no SimpMusic source code was copied.

## Repository secret

Create `LEVYRA_EDITORIAL_SP_DC` in:

```text
Repository Settings → Secrets and variables → Actions → New repository secret
```

Paste only the `sp_dc` value. Same-repository pull requests can run the live read-only integration test; fork and Dependabot pull requests never receive repository secrets.

## Collections

`config.json` maps Levyra's existing country chips to stable public playlist IDs. All configured markets are required unless a collection has `"optional": true`. Russia is currently optional because that public playlist can be unavailable; Levyra transparently keeps its existing YouTube/Apple fallback for any absent market. The unused global collection is not downloaded.

## Publication

The workflow runs every six hours and on manual dispatch. It validates the complete catalog, compares the raw previous JSON without `generatedAt`, and updates the data-only `editorial-data` branch only when substantive content changed.

Public URL:

```text
https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/editorial.json
```

Android uses a process-wide repository, a separate bounded HTTP cache, an `AtomicFile` disk snapshot, a 30-minute refresh target and a 48-hour maximum catalog age. Network refresh runs independently and never consumes the existing YouTube chart latency budget.
