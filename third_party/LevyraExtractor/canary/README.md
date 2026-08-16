# Levyra YouTube Canary

Levyra YouTube Canary is the repository's live compatibility observer for YouTube playback.
It exists to detect protocol/player changes early and turn a reproducible regression into a
reviewable extractor repair without waiting for another downstream project to diagnose it first.

## What it observes

The canary runs against a small fixed set of public sentinel videos and records only sanitized
protocol metadata:

- YouTube watch-page availability and the current WEB client version
- player JavaScript URL host, SHA-256 fingerprint, size and signature timestamp when visible
- `playabilityStatus` and the presence/shape of `streamingData`
- direct versus ciphered formats and `n`-parameter presence
- HLS/DASH availability
- a bounded initial and continuation `Range` probe against a direct `*.googlevideo.com` media URL
- recent YouTube/player-related commit metadata from selected open-source extractor projects as
  **radar only**, never as trusted instructions

The canary does not persist cookies, API keys, visitor data, signed media URLs, media query strings,
or account state.

## Baseline model

`baseline.json` is an accepted known-good observation. A player JavaScript hash change alone is not
a failure and does not open a repair PR. A repair is triggered only by material behavior regression,
for example a required sentinel losing playability/streaming formats or repeated media Range failures.

The repository starts with an intentionally unseeded baseline. The first healthy scheduled run opens
a draft baseline PR. Merging that tiny PR establishes the known-good reference for future comparisons.
If required probes are blocked or unavailable, the baseline is not seeded.

## Autonomous repair flow

When a material regression is confirmed twice in the same workflow:

1. the workflow captures a fresh sanitized report and keeps the current player JavaScript only as
   ephemeral private job evidence;
2. if repository secret `LEVYRA_CANARY_OPENROUTER_API_KEY` is configured, the pinned official Codex
   GitHub Action runs through OpenRouter's OpenAI-compatible Responses API using `openrouter/free`;
3. the free router selects an available zero-cost model that supports the request; if the provider,
   model, rate limit, or agent execution is unavailable, the workflow fails closed to diagnostics;
4. the repair agent may change only the extractor/protocol compatibility allowlist and must add/update
   a deterministic regression test;
5. the workflow rejects out-of-scope edits and runs extractor tests, Android unit tests, Kotlin
   compilation, lint, `git diff --check`, and Levyra's fast AI quality gate;
6. only a validated patch is allowed into the automatically opened **draft PR**;
7. the accepted baseline advances in the same PR only after the repair passes all gates.

There is **never automatic merge, release, or publication**.

If the OpenRouter key is unavailable, the regression disappears on confirmation, the agent makes no
safe code change, the diff escapes the allowlist, the free model is unavailable, or validation fails,
the workflow does not fabricate a fix. It opens/updates a diagnostic draft PR containing only the
sanitized incident report when a confirmed regression still exists.

## Security boundaries

- The workflow is schedule/manual-dispatch only; it never runs a repair agent on pull-request code.
- Write permissions are limited to the jobs that create the baseline/repair PR.
- The OpenRouter API key is scoped only to the Codex action step and must never be printed or stored.
- The repair request uses OpenRouter's Responses API endpoint and the zero-cost `openrouter/free` router.
- YouTube and media responses are size-bounded and time-bounded.
- Watch/player endpoints use an exact HTTPS host allowlist.
- Media probes accept only HTTPS `googlevideo.com` subdomains on port 443 and validate every redirect.
- Uploaded artifacts are sanitized. Player JavaScript evidence is not uploaded or committed.
- Upstream text/player JavaScript is treated as untrusted data to reduce prompt-injection risk.

## Manual run

From repository root:

```bash
python scripts/youtube_canary.py probe \
  --config third_party/LevyraExtractor/canary/config.json \
  --baseline third_party/LevyraExtractor/canary/baseline.json \
  --out-dir artifacts/youtube-canary
```

A local environment with restricted DNS/YouTube access may return a blocked result. GitHub Actions is
the authoritative live environment for the scheduled observer.
