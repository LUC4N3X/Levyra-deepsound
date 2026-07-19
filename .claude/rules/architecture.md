---
paths:
  - "app/src/main/java/**/*.kt"
  - "app/src/test/java/**/*.kt"
  - "docs/ARCHITECTURE.md"
---

# Architecture

- Preserve the critical path described in `docs/ARCHITECTURE.md`: tap, cached/in-flight resolution, extractor/client race, MediaItem preparation, Media3 playback.
- Secondary work must be bounded, cancellable, and unable to delay user-triggered playback.
- Prefer existing boundaries and policies over new global singletons.
- Use immutable state and atomic snapshots where multiple fields must change consistently.
- Never perform blocking network, database, disk, extraction, or media work on the main thread.
- Do not create duplicate HTTP clients, image loaders, players, databases, queue engines, or caches without a documented reason.
- Shared deduplicated jobs must run in an engine-owned supervised scope and clean up independently from caller cancellation.
- Old asynchronous work may publish only when its identity, request generation, and current feature state still match.
- Keep low-RAM, battery-saver, data-saver, background, and lifecycle behavior explicit.
