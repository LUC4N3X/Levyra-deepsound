# Levyra Claude Code

@AGENTS.md

Claude Code reads this file natively at project startup. The imported
`AGENTS.md` is the cross-runtime Levyra contract and must be applied before
repository work.

Claude-specific settings, hooks, agents, and path rules live under
`.agents/claude/` and may be projected locally to `.claude/`. Those runtime
helpers strengthen this contract but are not required for this root bootstrap to
work.

Do not duplicate the imported engineering rules here. Keep this bridge short so
startup context stays reliable and inexpensive.
