# Google Antigravity Setup for Levyra

Levyra uses repository-local Google Antigravity discovery without duplicating
project instructions or maintaining an Antigravity-only skill tree.

## Automatic discovery

Open the repository root as the Antigravity workspace. Antigravity uses:

- `AGENTS.md` as the canonical repository contract;
- nearer path-specific `AGENTS.md` files for Android, Desktop, CI, and docs;
- `.agents/skills/*/SKILL.md` as shared workspace skills;
- `.agents/rules/levyra-workspace.md` as the always-on bridge to the root
  contract;
- `.rtk/filters.toml` for project-specific output compression when RTK is
  available.

Start a new Agent conversation after pulling rule or skill changes so the
workspace inventory is rebuilt.

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

Open `Levyra-deepsound/` itself, not only a nested module.

## Automatic skill routing

Use the most specific matching Levyra skills. Multiple skills may apply.

Antigravity automatically loads `levyra-real-engineering` for non-trivial
features, architectural changes, unclear defects, and multi-step work where
requirements and implementation need to be separated. That adapter chooses the
lightest useful Matt Pocock stage: `grill-with-docs` for genuine ambiguity,
`wayfinder` for a large unresolved decision map, `to-spec` once intent is
settled, `to-tickets` only when one reviewable change is too large,
`implement`/`tdd` for execution, and `code-review` plus Levyra review for the
final diff. Tiny unambiguous fixes stay on Levyra's normal work method.

The canonical adapter is
`.agents/skills/levyra-real-engineering/SKILL.md`. It works even when the
upstream package is not installed. When upstream Matt Pocock skills are also
available to Antigravity, load the exact selected stage skill instead of
reconstructing it from memory. `AGENTS.md`, current architecture, focused
Levyra domain skills, tests, validation, and owner publication controls always
remain higher priority.

Antigravity automatically loads `levyra-context-efficiency` for verbose build,
test, lint, log, search, dependency, Git/GitHub, CI, CodeRabbit, and setup work.
It automatically loads `levyra-security-review` for:

- vulnerability scans, CVEs, exploitability, or security findings;
- attacker-controlled input and trust-boundary changes;
- authentication, tokens, cookies, keys, signing, secrets, and leak risk;
- URLs, redirects, SSRF, MIME, paths, archives, injection, and deep links;
- Android permissions/exported components and Desktop listener/IPC behavior;
- workflow permissions, action pinning, artifacts, dependency changes, and
  supply-chain risk;
- privacy, update integrity, signatures, and checksums.

For security work, Antigravity follows the same shared cycle as Codex, Claude
Code, and ChatGPT Projects:

```text
threat model
→ identification
→ safe validation
→ minimal remediation
→ human review
→ revalidation
```

The full procedure lives in
`.agents/skills/levyra-security-review/SKILL.md` and
`docs/ai/CODEX_SECURITY.md`. The file name reflects the optional security engine,
but the workflow is runtime-independent.

## Evidence and RTK

RTK may compact ordinary noisy commands. Keep these raw:

- security scans and exploit validation;
- exact reproduction input/output and exit status;
- secret scans;
- hashes, checksums, and signatures;
- signing and release-integrity evidence;
- incomplete or ambiguous failures.

Rerun the exact command raw whenever compact output is insufficient.

## First-run verification

1. Pull the latest branch and open the repository root.
2. Start a new Agent conversation.
3. Open the skills list or use `/skills`.
4. Confirm `levyra-real-engineering`, `levyra-context-efficiency`, and
   `levyra-security-review` exist.
5. Ask the Agent to identify applicable root/path instructions and skills.
6. Run:

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
python3 scripts/validate_matt_skills.py
python3 scripts/validate_claude_mem.py
```

The validators check the workspace bridge, skill frontmatter/inventory, RTK
configuration, real-engineering routing, automatic security routing,
claude-mem integration, and shared documentation.

## Rule activation

When Antigravity exposes activation controls, keep
`.agents/rules/levyra-workspace.md` **Always On**. This is a guardrail in
addition to root `AGENTS.md` discovery.

## Safety and publication

Before commit, run `python3 scripts/ai_quality_gate.py --profile fast`. Before
push or pull-request publication, run
`python3 scripts/ai_quality_gate.py --profile full`. Treat missing tools and
skipped required checks as blocked, not passed.

A suspected issue is not a confirmed vulnerability until evidence supports a
concrete attack path or security failure. Generated patches require complete
diff review, focused tests, applicable CI, and revalidation.

Antigravity must not infer permission to commit, push, open or merge pull
requests, rotate credentials, change versions/settings, tag, publish, or release.
Those actions require explicit owner authorization for the exact scope.

## Troubleshooting

If skills do not appear:

1. confirm the workspace is the Git root;
2. confirm files are under `.agents/skills/<name>/SKILL.md`;
3. confirm each skill has YAML frontmatter with a non-empty `description`;
4. keep `.agents/rules/levyra-workspace.md` active;
5. start a new conversation;
6. run all four validators and fix every reported error.

Do not copy skills into `.gemini/skills/`. `.agents/skills/` is the canonical
location shared by supported runtimes.
