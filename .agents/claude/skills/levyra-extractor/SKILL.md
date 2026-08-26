---
name: Levyra Extractor Change
description: Safely changes InnerTube, LevyraExtractor, player-config sync, stream resolution, retry, token, or network fallback behavior.
---

# Levyra extractor workflow

1. Read `.claude/rules/extractor-network.md`, `.claude/rules/security.md`, and the relevant resolver/client/config workflow files.
2. Map the existing cache, in-flight, extractor/client race, fallback, timeout, and retry order.
3. Separate `Found`, conclusive `NoMatch`, transient `Failed`, and cancellation outcomes.
4. Preserve last-known-good runtime configuration until a replacement is validated and atomically published.
5. Do not hardcode credentials, cookies, account tokens, visitor data, or production API keys.
6. Keep retry count, timeout, concurrency, and traffic bounded.
7. Add tests for config parsing/epoch, fallback order, timeout, cancellation, and invalid upstream payloads.
8. Check `.github/workflows/sync-player-configs.yml`; remember that a successful no-change run creates no PR.
