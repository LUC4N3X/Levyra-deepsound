# Google Antigravity Setup for Levyra

Levyra is configured for repository-local Google Antigravity discovery without
copying project instructions into the IDE or maintaining a second skill tree.

## Automatic discovery

Open the repository root as the Antigravity workspace. Antigravity then uses:

- `AGENTS.md` as repository context;
- nearer path-specific `AGENTS.md` files for scoped Android, Desktop, CI, and
  documentation work;
- `.agents/skills/*/SKILL.md` as workspace skills;
- `.agents/rules/levyra-workspace.md` as the lightweight workspace-rule bridge
  back to the canonical root contract.

Antigravity exposes skill names and descriptions when a conversation starts and
loads the full matching `SKILL.md` only when the task requires it. No manual
skill import or duplicate Antigravity-specific skill is required.

Official references:

- [Agent Skills](https://antigravity.google/docs/skills)
- [Workspace Rules](https://antigravity.google/docs/rules-workflows)
- [Gemini CLI migration and context files](https://antigravity.google/docs/gcli-migration)

## Required workspace shape

```text
Levyra-deepsound/
├── AGENTS.md
├── app/AGENTS.md
├── desktop/AGENTS.md
├── .github/AGENTS.md
├── docs/AGENTS.md
└── .agents/
    ├── README.md
    ├── rules/
    │   └── levyra-workspace.md
    └── skills/
        └── levyra-*/SKILL.md
```

Open `Levyra-deepsound/` itself, not only `app/`, `desktop/`, or another nested
folder. Starting below the Git root can hide repository-level customizations
from workspace discovery.

## First-run verification

1. Pull the latest `main` branch and open the repository root in Antigravity.
2. Start a new Agent conversation so the workspace skill inventory is rebuilt.
3. Open the skills list or use `/skills` and confirm the `levyra-*` skills are
   present.
4. Ask the Agent to identify the applicable root and path-specific instructions
   before editing.
5. Run the repository validator:

```bash
python3 scripts/validate_agent_config.py
```

The validator checks the Antigravity bridge, required documentation, skill
frontmatter, skill names, inventory coverage, and canonical instruction links.

## Rule activation

The automatic baseline does not depend on a manually copied prompt:
Antigravity reads the workspace context and discovers `.agents/skills/` from the
repository. The workspace rule adds a direct `@../../AGENTS.md` link so the IDE
rule system resolves the same canonical contract.

When the IDE exposes activation controls for workspace rules, keep
`levyra-workspace.md` set to **Always On**. This is an additional guardrail, not
a replacement for root `AGENTS.md` discovery.

## Operating model

Use the most specific matching Levyra skill. Multiple skills may apply to one
change, for example playback plus extraction plus security review. Planning,
implementation, validation, review, publication, merge, and release remain
separate states.

Antigravity must not infer permission to commit, push, open or merge pull
requests, change versions, tag, publish, or release. Those actions require an
explicit owner instruction for the exact scope.

## Troubleshooting

If Levyra skills do not appear:

1. confirm the opened workspace is the repository root;
2. confirm the files are under `.agents/skills/<name>/SKILL.md`;
3. confirm every `SKILL.md` has YAML frontmatter with a non-empty
   `description`;
4. start a new conversation after pulling configuration changes;
5. run `python3 scripts/validate_agent_config.py` and fix every reported error.

Do not copy skills into `.gemini/skills/` or create a parallel Antigravity-only
skill directory. `.agents/skills/` is the canonical workspace location shared
by the supported agent workflows.
