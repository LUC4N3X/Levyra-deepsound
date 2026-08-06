# Levyra workspace rule

@../../AGENTS.md

Treat the repository-root `AGENTS.md` as Levyra's authoritative operating
contract. Before investigating, editing, reviewing, or running commands:

1. apply the nearest path-specific `AGENTS.md` files for every file in scope;
2. read only the relevant approved planning material under `docs/project/`;
3. discover and load every matching `levyra-*` skill under `.agents/skills/`;
4. automatically load `levyra-context-efficiency` for builds, tests, lint,
   logs, broad searches, dependencies, Git/GitHub, CI, CodeRabbit, setup, or any
   command expected to produce large repetitive output;
5. when RTK is available, use it selectively for noisy supported commands,
   verify exit status and success/failure markers, and rerun the exact command
   raw whenever compact output hides required evidence;
6. inspect current code, tests, architecture, build files, and workflows before
   relying on remembered behavior or previous agent output;
7. make the smallest coherent change and preserve unrelated behavior;
8. run focused validation first, then the applicable repository checks;
9. keep implementation, validation, review, publication, merge, and release as
   separate states.

Keep short commands, exact-output checks, security evidence, signatures,
checksums, signing, and incomplete failure diagnostics raw. RTK reduces command
output; it is not validation authority and its savings are not equal to total
model-billing savings.

Never commit, push, open or merge a pull request, tag, publish, release, change
versions, or modify repository settings without explicit owner authorization for
the exact action and scope.

This rule is a lightweight workspace bridge. It deliberately links to the
canonical contract instead of duplicating it, so Codex, Antigravity, OpenClaw,
and other compatible coding agents share one source of truth while discovering
Levyra's context-efficiency workflow automatically.
