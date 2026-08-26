---
name: Levyra Database Change
description: Plans and validates Room entity, DAO, migration, cache, store, backup, and schema changes in Levyra.
---

# Levyra database workflow

1. Inspect `LevyraDatabase`, every affected entity/DAO/repository, current schema version, backups, and tests.
2. Define the old and new schema before editing.
3. Bump the database version exactly once and add an explicit migration from the previous version.
4. Preserve existing user data; do not introduce destructive migration.
5. Add required indices, uniqueness, TTL/count bounds, and cleanup queries.
6. Keep durable identity canonical and independent from mutable display strings.
7. Add migration/DAO tests covering upgrade, reads, writes, pruning, and negative-cache semantics where relevant.
8. Confirm downloads, favorites, playlists, queue, lyrics, listening history, and backup behavior are unaffected.
