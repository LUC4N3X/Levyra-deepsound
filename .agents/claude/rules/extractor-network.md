---
paths:
  - "app/src/main/java/com/luc4n3x/levyra/data/Youtube*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/NewPipeRuntime.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/LevyraStreamHedge.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/network/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/security/**/*.kt"
  - ".github/workflows/nightly-extractor-check.yml"
  - ".github/workflows/sync-player-configs.yml"
  - "third_party/**/*"
---

# Extractor and Network

- Preserve the fast valid playback path and the existing fallback/race strategy. Do not serialize independent resolvers unless correctness requires it.
- Treat synced player configs as versioned external data. Keep a last-known-good path and avoid invalidating a working config before the replacement is validated.
- The config-sync workflow may run without opening a PR when upstream files have not changed. Do not assume missing PR means missing schedule execution.
- Never hardcode private API keys, cookies, account tokens, PO tokens, visitor data, or credentials.
- Use the shared Levyra HTTP client factory and bounded connect/read/call timeouts.
- Distinguish conclusive no-match from timeout, transport, HTTP, parsing, and verification failures.
- Preserve cancellation and avoid retries that multiply traffic without a strict cap and backoff.
- Validate remote configuration structure before publication and keep hot updates atomic.
- Add focused tests for parsing, config epochs, fallback order, retry bounds, and cancellation behavior.
