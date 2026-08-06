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
5. automatically load `levyra-security-review` for vulnerability scans,
   attacker-controlled input, trust-boundary changes, authentication, secrets,
   permissions, privacy, dependency risk, update integrity, or security-related
   pull requests;
6. when RTK is available, use it selectively for noisy supported commands,
   verify exit status and success/failure markers, and rerun the exact command
   raw whenever compact output hides required evidence;
7. keep exploit evidence, security validation, signatures, checksums, secret
   scans, and exact reproduction output raw;
8. inspect current code, tests, architecture, build files, and workflows before
   relying on remembered behavior or previous agent output;
9. make the smallest coherent change and preserve unrelated behavior;
10. run focused validation first, then the applicable repository checks;
11. keep implementation, validation, review, publication, merge, and release as
    separate states.

RTK reduces command output; it is not validation authority and its savings are
not equal to total model-billing savings. Codex Security findings and generated
patches are proposals that require evidence, human review, CI, and explicit
owner-controlled publication.

Never commit, push, open or merge a pull request, tag, publish, release, change
versions, or modify repository settings without explicit owner authorization for
the exact action and scope.

This rule is a lightweight workspace bridge. It deliberately links to the
canonical contract instead of duplicating it, so Codex, Antigravity, OpenClaw,
and other compatible coding agents share one source of truth while discovering
Levyra's context-efficiency and security-review workflows automatically.
