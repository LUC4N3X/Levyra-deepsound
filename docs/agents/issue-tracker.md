# Issue tracker: GitHub

Levyra's real issue tracker is GitHub Issues in `LUC4N3X/Levyra-deepsound`.

Use the runtime's authenticated GitHub integration when one is available. In a local clone with GitHub CLI configured, use `gh` and let the repository remote resolve the target repository.

## Read operations

- Read an issue and comments before treating its body as current requirements.
- List only the labels, state, comments, dependencies, and metadata needed for the task.
- Resolve a bare `#<number>` as an issue or pull request before acting on it.
- Current repository code, `AGENTS.md`, approved planning, and owner decisions override stale issue text.

## Write operations

Creating, editing, labelling, assigning, commenting on, closing, or linking GitHub issues is an external repository action in Levyra. Do it only when the owner explicitly authorizes that exact publication scope.

When a Matt Pocock skill says to publish a spec, ticket set, wayfinder map, or other artifact to the issue tracker but publication has not been authorized, prepare the complete issue-ready content in the current handoff or approved planning location and leave GitHub unchanged.

Do not silently turn a planning conversation into GitHub issues.

## Pull requests as a triage surface

PRs as a request surface: no.

A pull request may be reviewed when requested, but it is not automatically part of Matt Pocock's triage state machine.

## Ticket and wayfinder semantics

When issue publication is explicitly authorized:

- use GitHub sub-issues for parent -> child relationships when the runtime/API supports them;
- use native blocking/dependency links when available;
- otherwise preserve `Parent` and `Blocked by` relationships in the issue body;
- keep each ticket independently reviewable and sized as a vertical slice;
- never infer merge, release, version-change, or deployment permission from issue publication.

When publication is not authorized, represent the same parent/blocking relationships in the local spec/ticket handoff instead.
