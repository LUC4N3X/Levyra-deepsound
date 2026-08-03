# OpenClaw Integration for Levyra

## Recommended role

Use OpenClaw as the **orchestrator and status layer** for Levyra, not as an
unrestricted all-purpose developer.

The safest useful split is:

```text
main
└── levyra
    ├── planning and repository orientation
    ├── Codex/Claude/OpenCode implementation delegation
    ├── focused validation
    ├── independent review delegation
    └── branch and pull-request status returned to main
```

The `levyra` agent should have the Levyra repository as its workspace. That lets
the agent discover the root and nested `AGENTS.md` files and the project-native
skills under `.agents/skills/`.

Keep general research, publishing, email, calendar, messaging, and system
administration in separate agents. A coding agent does not need access to all of
them.

## Workspace setup

Create or bind a dedicated agent to the real repository checkout:

```powershell
openclaw agents add levyra `
  --workspace "C:\\path\\to\\Levyra-deepsound"
```

Use the real local path. Do not point the Levyra agent at a copy that cannot
build, access Git metadata, or reproduce the owner's branch state.

After configuration, verify:

```powershell
openclaw agents list --bindings
openclaw gateway status --require-rpc
```

The agent workspace should contain:

```text
AGENTS.md
SPEC.md
ROADMAP.md
TASKS.md
.agents/skills/
app/
desktop/
.github/
docs/
```

## Delegation policy

The coordinator should target the Levyra agent explicitly rather than relying on
implicit agent selection.

Example intent:

```text
Use the levyra agent in the Levyra repository. Read AGENTS.md, SPEC.md,
ROADMAP.md, TASKS.md, and the matching native skills. Inspect the real code and
tests. Implement only the approved phase, run focused checks, review the final
diff, and return branch, commit, checks, blockers, and PR state. Do not merge,
tag, publish, release, or change repository settings.
```

Configure the main agent with an explicit allowlist that includes `levyra`.
Avoid `["*"]` unless every configured target is intentionally trusted. Keep
explicit agent selection required so a task cannot silently run under the wrong
profile.

The exact JSON keys can vary with the installed OpenClaw release; validate the
configuration with:

```powershell
openclaw doctor
openclaw gateway status --require-rpc
```

## Native versus external coding runtime

A native OpenClaw sub-agent is useful for:

- reading repository instructions;
- planning and task decomposition;
- locating files and tests;
- collecting CI or review state;
- coordinating a sequence of bounded steps.

For substantial implementation, use a configured coding runtime that can work
inside the repository and return concrete file and command evidence. Depending
on the installed setup, that may be Codex, Claude Code, OpenCode, or another ACP
runtime.

The selected runtime must still obey Levyra's repository instructions. A more
powerful runtime does not gain permission to broaden scope, push, merge, or
release.

## Skill visibility

Keep Levyra-specific skills in the repository:

```text
.agents/skills/levyra-player/
.agents/skills/levyra-extractor/
.agents/skills/levyra-database/
.agents/skills/levyra-compose/
.agents/skills/levyra-motion-artwork/
.agents/skills/levyra-desktop/
.agents/skills/levyra-security-review/
.agents/skills/levyra-ci-workflows/
.agents/skills/levyra-pr-review/
.agents/skills/levyra-release-check/
.agents/skills/levyra-project-manager/
.agents/skills/levyra-openclaw-orchestrator/
.agents/skills/levyra-engineering/
```

Do not install project-specific Levyra skills globally unless every agent should
see them. Do not grant the `levyra` agent unrelated high-impact skills merely
because they are available globally.

## Recommended execution pattern

1. `main` receives the owner's request.
2. `main` delegates to `levyra` with the repository path and explicit outcome.
3. `levyra` reads planning files and loads domain skills.
4. A coding runtime performs one focused implementation phase.
5. Focused tests and applicable broader checks run.
6. A fresh reviewer inspects the latest diff.
7. Actionable findings return to the implementation runtime.
8. Validation repeats after changes.
9. A branch and draft pull request are created only when the owner authorized
   publication.
10. `levyra` returns evidence to `main`; merge and release remain owner actions.

## Tool boundaries

Recommended for the Levyra agent:

- repository read/search;
- bounded command execution in the repository;
- Git status, diff, branch, commit, and pull-request operations when authorized;
- build/test tools required by the project;
- GitHub PR, review, and CI inspection.

Keep denied or separated unless a task explicitly needs them:

- email, calendar, contacts, and personal messaging;
- unrestricted browser sessions containing private accounts;
- password stores and unrelated home directories;
- system-wide package removal or destructive administration;
- release credentials and signing material;
- direct merge, tag, release, store upload, or repository-setting changes.

Use sandboxing and narrow allowlists where practical. Skills teach a workflow;
they are not a security boundary by themselves.

## Publication rules

OpenClaw may coordinate a branch and draft pull request only when the current
owner request explicitly authorizes it.

It must never infer permission to:

- push directly to `main`;
- merge a pull request;
- dismiss review findings without evidence;
- change Android or Desktop versions;
- tag or publish a release;
- upload store metadata;
- alter repository settings or secrets.

The final handoff must distinguish:

```text
planned
edited
locally validated
committed
pushed
pull request opened
CI passed
reviewed
merged
released
```

Only states backed by direct evidence may be reported.

## Useful recurring automation

After the GitHub tools and notification channel are deliberately configured,
OpenClaw can perform low-risk recurring work such as:

- summarize new actionable review comments on an open PR;
- report when required CI reaches a final state;
- surface a stale branch or unresolved review thread;
- prepare a daily summary of open Levyra PRs without modifying them.

Do not enable automatic code changes, merges, releases, or broad issue-driven
execution as the first automation. Start read-only, observe the output, then
grant the minimum additional capability required.

## Verification checklist

- `levyra` resolves to the intended repository workspace.
- The root `AGENTS.md` and matching native skills are visible.
- `openclaw doctor` reports a valid configuration.
- `agents_list` or equivalent discovery shows the intended target agents.
- Delegated tasks use explicit `agentId`.
- The coding runtime executes in the repository checkout.
- Unrelated personal and administrative tools are absent.
- Branch and PR actions require explicit owner authorization.
- Merge, tag, release, and settings changes remain blocked.
- The agent returns exact checks, blockers, branch, commit, and PR state.
