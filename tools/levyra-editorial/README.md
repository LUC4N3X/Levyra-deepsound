# Levyra editorial collector

This tool builds a small, account-free JSON catalog for Levyra from configured public editorial playlists.

It runs only in GitHub Actions. The long-lived web session value is stored as an Actions secret and is used only to obtain a short-lived bearer token during the workflow run. The generated catalog contains public metadata only: titles, artists, albums, artwork URLs, durations, ISRC values, source IDs, and chart positions.

## Security model

- The secret is never stored in the repository, APK, generated catalog, artifacts, or logs.
- Same-repository pull requests can run the live collector with the repository secret; fork and Dependabot pull requests do not receive it.
- The pull-request integration job has read-only repository permissions and never publishes data.
- The scheduled collector fails closed: if authentication, parsing, or validation fails, the last published catalog is left untouched.
- Generated output is scanned for credential-related keys before publication.
- A dedicated source account should be used. Do not reuse a personal password or session.
- Rotate the secret immediately if a workflow log, local terminal, or third-party service ever receives it.

The source account is not shipped to users. Android and Desktop will consume only the generated public catalog.

## Implementation note

The collector is an original Python implementation written for Levyra. The actively maintained SimpMusic project was used as a behavioral reference for the current `sp_dc` plus TOTP web-session flow, including the dedicated server-time endpoint and mobile web-player token profile. No SimpMusic source code was copied into Levyra.

## Repository secret

Create this repository secret:

```text
LEVYRA_EDITORIAL_SP_DC
```

Paste only the value of the `sp_dc` cookie. A complete cookie string containing `sp_dc=...` is also accepted, but the single value is preferable.

GitHub path:

```text
Repository Settings → Secrets and variables → Actions → New repository secret
```

The connector used to prepare this change cannot create or read repository secrets, so this is the only manual setup step.

An optional secret-dictionary URL can be provided as a repository variable or workflow environment value:

```text
LEVYRA_EDITORIAL_TOTP_SECRETS_URL
```

When omitted, the collector uses the current public versioned dictionary configured in `levyra_editorial/spotify.py`.

## Configuring collections

Edit `config.json`. Every collection needs:

- a stable Levyra `id`;
- a `kind`: `chart`, `editorial`, or `release`;
- an ISO 3166-1 alpha-2 market, or `GLOBAL`;
- a public playlist ID;
- an optional title override.

The initial configuration contains the official Top 50 Italy and Top 50 Global playlist IDs.

## Running locally

Never place the secret in a tracked file.

```bash
cd tools/levyra-editorial
python -m venv .venv
. .venv/bin/activate
pip install -e '.[dev]'
export LEVYRA_EDITORIAL_SP_DC='...'
levyra-editorial --config config.json --output ../../build/editorial/catalog.json
levyra-editorial --validate ../../build/editorial/catalog.json
```

## Publishing

`.github/workflows/editorial-catalog.yml` runs:

- unit tests and static checks on pull requests;
- a live read-only collection test on same-repository pull requests;
- collection every six hours and on manual dispatch;
- validation before any publication;
- artifact upload for diagnostics;
- publication of the last valid JSON to the `editorial-data` branch.

Public data URL after the first successful scheduled or manual run:

```text
https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/editorial-data/catalog/editorial.json
```

A failed run never overwrites that file.

## Data contract

```json
{
  "schemaVersion": 1,
  "generatedAt": "2026-07-29T12:00:00Z",
  "collections": [
    {
      "id": "top-50-italy",
      "kind": "chart",
      "market": "IT",
      "title": "Top 50 Italia",
      "description": "...",
      "sourceId": "...",
      "sourceUrl": "https://...",
      "artworkUrl": "https://...",
      "snapshotId": "...",
      "totalSourceItems": 50,
      "tracks": [
        {
          "position": 1,
          "id": "...",
          "uri": "...",
          "title": "...",
          "artists": [{"id": "...", "name": "..."}],
          "album": {
            "id": "...",
            "name": "...",
            "releaseDate": "2026-07-01",
            "artworkUrl": "https://...",
            "externalUrl": "https://..."
          },
          "durationMs": 185000,
          "explicit": false,
          "isrc": "...",
          "externalUrl": "https://...",
          "artworkUrl": "https://..."
        }
      ]
    }
  ]
}
```

The application layer should treat this catalog as an editorial signal, then resolve playable items through Levyra's own music pipeline.
