# Levyra Active Tasks

## Active phase

**Name:** Agent-assisted engineering foundation  
**Roadmap track:** Track 7 - Agent-assisted engineering  
**Status:** In review  
**Scope:** Repository-local planning, orchestration, and validation files only.
No Android or Desktop product behavior, dependency, version, signing, or release
change is part of this phase.

## Acceptance criteria

- Durable requirements live in `SPEC.md`.
- Ordered engineering outcomes live in `ROADMAP.md`.
- This file records only the active reviewable phase and truthful validation
  state.
- A native project-management skill coordinates planning without replacing
  domain skills.
- A native OpenClaw skill defines safe delegation and owner-controlled
  publication boundaries.
- AI workflow documentation explains implementation, independent review, CI,
  manual testing, merge, and release as separate gates.
- A standard-library validation script checks required files, skill metadata,
  and documented skill references.
- The existing PR workflow runs that validation.
- Existing product code, versions, dependencies, signing, and release behavior
  remain unchanged.

## Work items

- [x] Compare the useful Titus AI workflow patterns with Levyra's existing
      repository instructions and native skills.
- [x] Keep Levyra's existing specialized `AGENTS.md` hierarchy as the primary
      source of project behavior.
- [x] Add `SPEC.md`, `ROADMAP.md`, and `TASKS.md` with distinct responsibilities.
- [x] Add the `levyra-project-manager` native skill.
- [x] Add the `levyra-openclaw-orchestrator` native skill.
- [x] Add the complete AI development workflow and OpenClaw integration guide.
- [x] Update root, OpenAI, and ChatGPT routing documentation.
- [x] Add `scripts/validate_agent_config.py`.
- [x] Add agent-configuration validation to the existing PR check.
- [ ] Confirm the agent-configuration validation job passes on the pull request.
- [ ] Complete an independent latest-commit review and address actionable
      findings.
- [ ] Merge only after owner approval.

## Validation matrix

| Check | Required | Current state |
| --- | --- | --- |
| `python3 scripts/validate_agent_config.py` | Yes | Pending CI on the published branch |
| Markdown path and command review | Yes | Performed during authoring; CI validates required paths |
| Android unit tests, lint, and release compile | Existing PR gate | Pending CI; no product code changed |
| Desktop build | No for this docs/config-only phase | Not run |
| Device, playback, Android Auto, notification, PiP | No for this phase | Not applicable |
| Windows installer, update, protocol, media keys | No for this phase | Not applicable |
| Independent review | Yes | Pending |
| Merge or release | Separate owner action | Not authorized by this file |

## Behavior preserved

- Android and Desktop implementation files are untouched.
- Playback, queue, MediaSession, notification, Android Auto, downloads, Room,
  preferences, backups, localization, and UI behavior are unchanged.
- Android and Desktop versions, tags, packages, signing, artifacts, and release
  workflows remain independent.
- Existing Claude and OpenAI domain skills remain the detailed engineering
  playbooks.

## Follow-up queue

These items are not activated by this phase:

- apply the documented OpenClaw workspace and agent configuration on the owner's
  machine;
- add a scheduled GitHub review-summary workflow in OpenClaw after tool access
  and notification behavior are explicitly approved;
- revise roadmap priorities when the owner selects the next product phase;
- archive or replace this active phase after merge.

## Update rule

Do not mark a check complete from an agent's narrative. Record the exact command,
CI run, review, device check, or owner decision that provides the evidence.
Replace this active phase when new work begins instead of accumulating unrelated
tasks indefinitely.
