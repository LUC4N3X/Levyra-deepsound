---
paths:
  - "app/src/main/java/com/luc4n3x/levyra/data/local/**/*.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/*Store.kt"
  - "app/src/main/java/com/luc4n3x/levyra/data/*Cache.kt"
  - "app/src/test/java/**/*Database*Test.kt"
  - "app/src/test/java/**/*Dao*Test.kt"
---

# Room, Stores, and Cache

- Every schema change requires an explicit database-version bump and migration from the previous version.
- Do not use destructive migration for user data unless the owner explicitly approves data loss.
- Keep entity, DAO, indexes, migration SQL, repository mapping, and cleanup policy consistent.
- Run database operations off the main thread and keep transactions as small as correctness allows.
- Bound caches by count and/or size, and define TTL and cleanup behavior for remote-derived data.
- Never treat transient network failure as a durable negative cache result.
- Use stable canonical keys; do not key durable data only by mutable titles or artwork URLs.
- Preserve existing downloads, favorites, playlists, queue, lyrics, listening history, and backup compatibility.
- Add migration or DAO tests for new tables, columns, indices, pruning, and old-version upgrade paths.
