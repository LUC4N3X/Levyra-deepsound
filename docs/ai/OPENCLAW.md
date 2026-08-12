# OpenClaw Integration for Levyra

## Role

Use OpenClaw as Levyra's persistent orchestration, memory, review, and status
layer. Keep implementation grounded in the repository and keep publication under
explicit owner control.

The default Levyra profile has three roles:

```text
primary Levyra worker
├── implementation and orchestration
├── levyra-reviewer
│   └── independent latest-diff review
└── levyra-ci
    └── PR, CI, logs and validation evidence
```

Do not add another agent unless a repeated workload has a distinct context,
permission, or evidence boundary that these three roles cannot cover cleanly.
More agents are not automatically more capable; unnecessary delegation costs
context and coordination.

## VPS bootstrap

For the existing Linux/OpenClaw installation, run from the Levyra checkout:

```bash
git pull --ff-only origin main
bash scripts/setup-openclaw-levyra.sh
```

The bootstrap is idempotent. By default it:

- preserves an existing `levyra-worker` or `levyra` primary agent;
- uses the existing `workspace-levyra/repo` checkout;
- creates `levyra-reviewer` and `levyra-ci` only when missing;
- creates separate evidence workspaces/checkouts for the two specialists;
- exposes the current repository-native `levyra-*` skills through thin workspace
  bridges instead of copying their full contents;
- adds a small always-on Levyra orchestration block to the primary workspace
  `AGENTS.md` without replacing existing instructions;
- creates `MEMORY.md` only when a workspace does not already have one;
- enables bounded cross-conversation recall for the primary Levyra agent;
- enables Active Memory in `escalate` mode with recent, precision-heavy recall;
- enables memory-core Dreaming unless disabled through the environment;
- adds a twice-daily read-only `levyra-ci` audit when no audit with the same name
  already exists;
- validates OpenClaw configuration, doctor status, memory status, Gateway RPC,
  agent bindings, and the CI audit registration.

Environment overrides:

```text
LEVYRA_OPENCLAW_AGENT
LEVYRA_OPENCLAW_WORKSPACE
LEVYRA_REPO
LEVYRA_REVIEW_WORKSPACE
LEVYRA_CI_WORKSPACE
LEVYRA_REPO_URL
LEVYRA_OPENCLAW_AUDIT_CRON
LEVYRA_OPENCLAW_AUDIT_TZ
LEVYRA_OPENCLAW_ENABLE_ACTIVE_MEMORY
LEVYRA_OPENCLAW_ENABLE_DREAMING
LEVYRA_OPENCLAW_INSTALL_CRON
```

## Primary Levyra worker

The primary worker owns implementation and orchestration. For every non-trivial
Levyra task it should:

1. work inside `repo/`;
2. read root and nearest scoped `AGENTS.md` files;
3. apply the context budget before broad repository reading;
4. load the matching repository-native skills;
5. implement the smallest verified change;
6. run focused validation and applicable repository gates;
7. run the required `code-review` stage before presenting code as final;
8. delegate a fresh bounded review to `levyra-reviewer`;
9. delegate CI/PR/log diagnosis to `levyra-ci` instead of filling the
   implementation session with broad logs;
10. fix actionable findings and revalidate before handoff.

The bootstrap preserves any existing sub-agent allowlist and adds the two Levyra
specialists rather than replacing unrelated authorized targets.

## `levyra-reviewer`

`levyra-reviewer` is an independent evidence agent. It may refresh its private
checkout and inspect remote PR refs, but it does not implement its own findings,
edit production source, commit, push, merge, release, or change repository
settings.

Every finding should include:

- severity and confidence;
- exact location;
- triggering scenario;
- consequence;
- smallest compatible fix;
- missing regression coverage.

Review context should start with the latest diff/commit and only the surrounding
ownership needed to decide correctness.

## `levyra-ci`

`levyra-ci` owns current-head evidence for:

- open PR state;
- required GitHub Actions checks;
- failing jobs and exact steps;
- bounded raw failure logs;
- unresolved review threads;
- stale branches and superseded runs;
- reproducible validation status.

It does not edit source, commit, push, merge, release, change workflows, alter
secrets, or change repository settings.

Both evidence agents deny OpenClaw filesystem write/edit/apply-patch tools,
restrict filesystem tools to their workspace, disable elevated tools, and use
Gateway host execution in OpenClaw `auto` mode with strict inline-eval review.
This preserves access to the VPS Git/`gh` environment while keeping shell misses
behind OpenClaw's native execution reviewer.

## Context budget

Do not send full conversations between agents.

The primary worker should hand off only:

- objective and acceptance criteria;
- invariants that must remain unchanged;
- current branch/PR/SHA when relevant;
- latest diff or changed files;
- smallest useful surrounding code/evidence;
- checks already run and exact failures;
- unresolved risks;
- the exact question the specialist must answer.

Reviewer and CI agents expand context only for a concrete unanswered question.
Keep security, signing, release, R8, Perfetto, protocol, and exact failure
evidence raw whenever compression could alter the conclusion.

## Memory

Memory is evidence support, not a second source of truth.

Long-term memory may retain:

- stable verified architecture ownership;
- recurring engineering failure patterns;
- validated diagnostic techniques;
- durable owner preferences and explicit workflow decisions.

Do not retain secrets, credentials, signing material, transient branch heads,
current PR state, current CI state, temporary hypotheses, or generated logs in
long-term memory.

The primary worker uses bounded cross-conversation recall. Active Memory runs in
`escalate` mode so the extra recall path is spent on relevant past-context
questions rather than every ordinary turn. Dreaming may consolidate durable
signals into `MEMORY.md`; current repository evidence always wins over promoted
memory.

## Recurring audit

The default audit runs through `levyra-ci` twice daily using an isolated
`light-context` session. The audit prompt explicitly points to `./repo` and the
canonical repository instructions because lightweight cron runs intentionally do
not inject the normal full workspace bootstrap context.

It is evidence-only: no source edits, commits, branches, pushes, merges,
releases, settings changes, or secret access.

## Skill visibility

Canonical Levyra skills stay in:

```text
repo/.agents/skills/
```

The VPS bootstrap creates thin workspace bridges that point back to these files.
Never fork the full skill text into OpenClaw workspace memory. The repository
remains the single source of truth and newly added `levyra-*` skills become
visible after the bootstrap is refreshed.

## Publication rules

OpenClaw must never infer permission to:

- push directly to `main`;
- merge a pull request;
- dismiss review findings without evidence;
- change Android or Desktop versions;
- tag or publish a release;
- upload store metadata;
- alter repository settings, workflows, secrets, or signing material.

A current explicit owner instruction may authorize a specific branch, commit,
push, PR, or direct-main action. That authorization does not imply merge,
release, or repository-setting permission.

## Verification

After bootstrap, verify:

```bash
openclaw config validate
openclaw doctor
openclaw memory status --agent levyra-worker
openclaw gateway status --require-rpc
openclaw agents list --bindings
openclaw cron list --agent levyra-ci
```

If the primary agent is named `levyra` rather than `levyra-worker`, use that ID
for the memory command. `scripts/setup-openclaw-levyra.sh` detects the correct
primary ID automatically.

The final handoff from OpenClaw must distinguish `planned`, `edited`, `locally
validated`, `committed`, `pushed`, `pull request opened`, `CI passed`,
`independently reviewed`, `merged`, and `released`. Only states backed by direct
evidence may be reported.
