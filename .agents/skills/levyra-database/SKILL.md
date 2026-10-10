---
name: levyra-database
description: Implement, debug, or review Levyra Room entities, DAOs, migrations, schemas, caches, stores, backups, downloads, favorites, playlists, history, and persistent queue data.
---

# Levyra database workflow

## Required context

1. Read the root `AGENTS.md` and `app/AGENTS.md` when working in the Android module.
2. Read `docs/ARCHITECTURE.md`.
3. Read `.agents/claude/rules/data-room.md` and `.agents/claude/rules/architecture.md`.
4. Inspect every affected entity, DAO, repository/store, schema version, migration, backup path, and test.

## Change contract

Before editing, define:

- the current schema and behavior;
- the intended schema and behavior;
- the exact migration path;
- data that must be preserved;
- identity, uniqueness, index, cleanup, TTL, and storage-bound requirements;
- effects on downloads, favorites, playlists, queue, lyrics, history, settings, and backups.

## Guardrails

- Never use destructive migration for an existing user database.
- Bump the database version exactly once for an actual schema change and add an explicit migration from the previous version.
- Keep durable identity canonical and independent from mutable display strings.
- Keep database and file I/O off the main thread.
- Bound caches and historical data with explicit cleanup behavior.
- Distinguish conclusive negative results from transient failures before persisting cache state.
- Preserve backup and restore compatibility unless the task explicitly changes the format.
- Avoid parallel stores that duplicate an existing source of truth.

## Validation

Add migration and DAO tests covering upgrade, reads, writes, uniqueness, pruning, failure recovery, and backward compatibility as applicable. Verify that existing user data survives the migration and that unaffected personal-library features keep their current behavior.
