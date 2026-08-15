# Claude Code automatic tooling bootstrap

Levyra keeps Claude Code project tooling automatic and fail-open. The existing
Claude `SessionStart` environment probe and prompt-routing hooks stay in place;
this layer adds the project-scoped jCodeMunch MCP without replacing Claude's
native repository tools.

## Automatic behavior

On `startup` and `resume`:

1. the existing Levyra environment hook checks RTK and the local toolchain;
2. `.claude/hooks/jcodemunch-start.sh` ensures the pinned jCodeMunch runtime via
   the shared `scripts/codex_jcodemunch.py` launcher and refreshes the Levyra
   index;
3. `.mcp.json` exposes that same pinned runtime to Claude as the project-scoped
   `jcodemunch` MCP server;
4. `.claude/settings.json` allows `mcp__jcodemunch`, so individual jCodeMunch
   tool calls do not require repeated permission prompts.

Claude should prefer jCodeMunch for targeted symbol-level discovery, then use
native Read/Grep/Glob/Bash whenever broader context is necessary for control
flow, lifecycle, concurrency, state, generated behavior, tests, or correctness.
Token savings never override correctness.

## First project approval

Claude Code requires approval before using a project-scoped MCP server from
`.mcp.json`. Levyra does not bypass that security decision. After the project
server is approved, normal jCodeMunch tool calls are covered by the checked-in
`mcp__jcodemunch` project permission.

If the MCP install, index, or server launch fails, Claude continues with its
native repository tools. Failure must not block coding or be treated as
permission to weaken sandboxing or validation.

## Existing Claude plugins

Levyra continues to use the existing project plugin configuration in
`.claude/settings.json`, including the Kotlin, Chris Banes, Karpathy, and Matt
Pocock skill integrations. This change does not add another compression plugin
or duplicate their responsibilities.

## Validation

```bash
python3 scripts/validate_claude_bootstrap.py
python3 -m unittest scripts.tests.test_claude_bootstrap
```

The repository AI quality gate also discovers the new unit test through its
normal `scripts/tests/test_*.py` suite.
