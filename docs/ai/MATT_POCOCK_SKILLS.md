# Matt Pocock Skills in Levyra

Levyra uses the engineering workflow from [`mattpocock/skills`](https://github.com/mattpocock/skills) as a supplementary method for non-trivial AI-assisted work. The repository's own `AGENTS.md`, planning files, architecture, native `levyra-*` skills, tests, and quality gates remain authoritative.

The upstream project is MIT-licensed. Levyra does not vendor or silently fork the full external skill set; supported runtimes install the official package or use the repository-native `levyra-real-engineering` adapter.

## The Levyra flow

Use the lightest lane that matches the task:

```text
small obvious change
→ normal Levyra work method

ambiguous feature / architecture
→ grill-with-docs
→ to-spec
→ to-tickets when needed
→ implement + tdd
→ code-review + levyra-pr-review

large unresolved problem
→ wayfinder
→ grill/spec/tickets only as needed

unclear defect
→ diagnosing-bugs
→ minimal fix + regression test
→ code-review + levyra-pr-review
```

Fresh context is preferred between independent implementation tickets. Carry forward the approved spec, the ticket, durable domain vocabulary/ADRs, and direct evidence rather than an entire exploratory conversation.

## Skills Levyra expects

The focused upstream set is:

- `grill-with-docs`
- `wayfinder`
- `to-spec`
- `to-tickets`
- `implement`
- `tdd`
- `diagnosing-bugs`
- `code-review`
- `domain-modeling`
- `setup-matt-pocock-skills`

`triage` is intentionally not part of Levyra's automatic setup because label vocabulary is repository policy and must not be guessed. Add it only after the owner defines the label mapping.

## Upstream repository setup is already satisfied

Matt Pocock's setup skill expects an `## Agent skills` block plus repository-owned configuration under `docs/agents/`. Levyra keeps those outputs directly in version control:

- issue tracker: `docs/agents/issue-tracker.md` -> GitHub repository `LUC4N3X/Levyra-deepsound`;
- domain docs: `docs/agents/domain.md` -> single shared `CONTEXT.md` plus `docs/adr/`, both created lazily only when useful;
- canonical project contract: root/path `AGENTS.md`;
- requirements and active planning: `docs/project/`;
- current architecture: `docs/ARCHITECTURE.md`;
- reusable Levyra procedures: `.agents/skills/`;
- publication: explicit owner authorization remains required.

Do not repeatedly run `setup-matt-pocock-skills` during ordinary Levyra work. It stays installed so the setup can be intentionally re-run if the owner wants to change the issue tracker or domain-doc layout.

## Claude Code

Matt Pocock's current upstream package is available from Claude Code's official plugin marketplace. Levyra project-enables `mattpocock-skills@claude-plugins-official` in canonical `.agents/claude/settings.json`; `scripts/sync_agent_runtime.py` projects that file to native `.claude/settings.json`, where Claude Code applies its normal project plugin controls.

Equivalent manual command:

```bash
claude plugin install mattpocock-skills@claude-plugins-official --scope project
```

The canonical `.agents/skills/levyra-real-engineering/SKILL.md` bridge remains the Levyra routing contract. Claude discovers its generated native counterpart under `.claude/skills/`, but edits belong only in `.agents/skills/`. The bridge selects the upstream stage without letting the external plugin override repository rules. After plugin, projection, or instruction changes, start a new Claude session when needed so project and skill discovery is rebuilt.

## Codex

The repository setup scripts install the focused upstream skills with the open Agent Skills `skills` CLI when Codex and Node/npm are present. The installer targets Codex globally so running setup does not dirty the Levyra checkout.

Equivalent command:

```bash
npx skills@latest add mattpocock/skills -g -a codex -y \
  -s setup-matt-pocock-skills \
  -s grill-with-docs \
  -s wayfinder \
  -s to-spec \
  -s to-tickets \
  -s implement \
  -s tdd \
  -s diagnosing-bugs \
  -s code-review \
  -s domain-modeling
```

The `-g`, `-a codex`, `-s`, and `-y` flags deliberately make the installation global, Codex-specific, focused, and non-interactive. Start a new Codex session after installation. Codex must explicitly load each stage skill when needed; do not assume Claude-specific nested slash-command behavior works cross-runtime.

Codex's Levyra-specific bridge itself is not duplicated or installed globally: Codex discovers canonical `.agents/skills/levyra-real-engineering/SKILL.md` directly from the repository.

## Google Antigravity

Antigravity discovers Levyra workspace skills from `.agents/skills/`. Therefore `levyra-real-engineering` is always available from the repository itself and routes the same lifecycle without requiring a second Antigravity-only tree.

If the upstream package is also installed for Antigravity, install it through the Agent Skills ecosystem and keep Levyra's root contract higher priority. The repository adapter remains the stable routing contract if upstream names or runtime-specific invocation behavior change.

Open the repository root as the workspace and start a new conversation after pulling skill changes.

## ChatGPT Project

A repository cannot silently install a third-party ChatGPT Project skill. `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` therefore requires ChatGPT to load `levyra-real-engineering` from the connected repository and apply the same stage routing.

When ChatGPT has direct access to an upstream skill body, it should read that body before using the stage instead of reconstructing it from memory. When it does not, the repository-native adapter is the fallback contract. The adapter and `docs/agents/` configuration keep issue-tracker, domain-language, scope, and publication behavior deterministic even without a runtime-level package installer.

## Guardrails

- External skills supplement Levyra; they never override repository invariants.
- Do not run the full pipeline for tiny unambiguous changes.
- Do not ask the owner questions that the repository can answer.
- Do not create ADRs, glossary entries, specs, or tickets as ceremony.
- Do not publish GitHub issues merely because `to-spec`, `to-tickets`, or `wayfinder` is used.
- Do not create new managers, caches, stores, wrappers, or sources of truth without a demonstrated architecture need.
- A generic code smell is not automatically a Levyra defect.
- Validation remains the Levyra quality gate, not the external skill's self-assessment.

Before commit:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before push or pull-request publication:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

Validate the integration itself with:

```bash
python3 scripts/validate_matt_skills.py
```
