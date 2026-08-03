# Levyra Active Tasks

## Active phase

**Name:** Documentation information architecture  
**Roadmap track:** Track 7 - Agent-assisted engineering  
**Status:** Implementation complete; immediate squash merge explicitly authorized  
**Scope:** Move the project planning documents into `docs/project/`, add clear
navigation for the existing documentation areas, update every repository
reference, and remove the former root copies. No Android or Desktop runtime,
dependency, version, signing, packaging, or release behavior is changed.

## Acceptance criteria

- Durable requirements live in `docs/project/SPEC.md`.
- Ordered engineering outcomes live in `docs/project/ROADMAP.md`.
- The active reviewable phase and validation evidence live in
  `docs/project/TASKS.md`.
- `docs/README.md` provides a concise map of architecture, project, AI, and asset
  documentation.
- `docs/project/README.md` explains the distinct responsibility of each project
  document.
- Root and nested agent instructions, AI workflow documents, native skills, and
  the configuration validator reference the canonical new paths.
- The former root `SPEC.md`, `ROADMAP.md`, and `TASKS.md` files are removed after
  their canonical replacements exist.
- Existing product code, versions, dependencies, signing, packaging, and release
  behavior remain unchanged.

## Work items

- [x] Define a stable documentation layout.
- [x] Add the top-level documentation index.
- [x] Add the project-documentation guide.
- [x] Move the specification into `docs/project/`.
- [x] Move the roadmap into `docs/project/`.
- [x] Replace the active task phase with this documentation reorganization.
- [x] Update repository instructions, AI documentation, skills, and validation
      paths.
- [x] Remove the former root planning files.
- [x] Inspect the final branch diff for product-code, version, dependency,
      workflow, binary, or generated-file changes.
- [x] Record the owner's explicit authorization to open and immediately
      squash-merge the documentation-only pull request without waiting for CI.

## Validation matrix

| Check | Required | Current state |
| --- | --- | --- |
| Canonical project files exist under `docs/project/` | Yes | Verified on the branch |
| Former root planning files are absent | Yes | Verified on the branch |
| Repository references use `docs/project/*` | Yes | Directly inspected in every indexed reference |
| Documentation navigation and relative links | Yes | Manually reviewed |
| `python3 scripts/validate_agent_config.py` | Normally yes | Not executed locally; connector environment has no repository checkout |
| Android unit tests, lint, and release compile | No for this phase | Not run; no product or build code changed |
| Desktop build | No for this phase | Not run; no Desktop code changed |
| Device, playback, Android Auto, notification, PiP | No for this phase | Not applicable |
| Windows installer, update, protocol, media keys | No for this phase | Not applicable |
| CI completion before merge | Owner decision | Explicitly waived for this documentation-only change |
| Squash merge | Owner action | Explicitly authorized in the initiating request |

## Behavior preserved

- Android and Desktop implementation files are untouched.
- Playback, queue, MediaSession, notification, Android Auto, downloads, Room,
  preferences, backups, localization, and UI behavior are unchanged.
- Android and Desktop versions, tags, packages, signing, artifacts, and release
  workflows remain independent.
- Existing Claude and OpenAI domain skills retain their behavior; only planning
  document paths change.

## Update rule

Replace this active phase when new work begins instead of accumulating unrelated
tasks indefinitely. Record validation from direct commands, CI runs, reviews,
manual checks, or explicit owner decisions—not from an agent narrative.
