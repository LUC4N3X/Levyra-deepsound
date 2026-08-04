# Levyra workspace rule

@../../AGENTS.md

Treat the repository-root `AGENTS.md` as Levyra's authoritative operating
contract. Before investigating, editing, reviewing, or running commands:

1. apply the nearest path-specific `AGENTS.md` files for every file in scope;
2. read only the relevant approved planning material under `docs/project/`;
3. discover and load every matching `levyra-*` skill under `.agents/skills/`;
4. inspect current code, tests, architecture, build files, and workflows before
   relying on remembered behavior or previous agent output;
5. make the smallest coherent change and preserve unrelated behavior;
6. run focused validation first, then the applicable repository checks;
7. keep implementation, validation, review, publication, merge, and release as
   separate states.

Never commit, push, open or merge a pull request, tag, publish, release, change
versions, or modify repository settings without explicit owner authorization for
the exact action and scope.

This rule is a lightweight Antigravity workspace bridge. It deliberately links
to the canonical contract instead of duplicating it, so Codex, Antigravity,
OpenClaw, and other compatible coding agents share one source of truth.
