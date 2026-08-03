# Levyra AI Development Workflow

## Goal

Use AI agents as controlled engineering collaborators, not as a substitute for
requirements, validation, independent review, or owner approval.

The repository keeps four responsibilities separate:

| File | Responsibility |
| --- | --- |
| `AGENTS.md` and nested variants | Durable operating rules loaded by coding agents |
| `SPEC.md` | Approved product and engineering requirements |
| `ROADMAP.md` | Ordered outcomes, risks, and phase exit criteria |
| `TASKS.md` | One active reviewable phase and its truthful validation state |

Native skills under `.agents/skills/` define repeatable workflows. Detailed
domain playbooks remain under `.claude/skills/` and `.claude/rules/` where
existing OpenAI skills reference them.

## Complete lifecycle

1. **Orient**
   - Inspect the real repository, branch, worktree, current diff, architecture,
     tests, build files, and workflows.
   - Read root and nearest `AGENTS.md` files.
   - Read `SPEC.md`, `ROADMAP.md`, and the active phase in `TASKS.md`.
   - Load every matching native skill.

2. **Define the phase**
   - State the requested outcome and non-goals.
   - Identify behavior that must remain unchanged.
   - Map the work to specification requirements and roadmap outcomes.
   - Record acceptance criteria, automated checks, manual checks, rollback, and
     owner checkpoints in `TASKS.md` when the task is large enough to require a
     tracked phase.

3. **Plan**
   - Trace the current control and data flow.
   - Identify the verified root cause for a defect.
   - Choose the smallest coherent design that matches current architecture.
   - List expected files, risks, migration/security/localization implications,
     and focused validation.
   - Stop before implementation only when the owner reserved plan approval.

4. **Implement one reviewable phase**
   - Use a dedicated branch.
   - Touch only files required by the active phase.
   - Keep unrelated cleanup, dependency changes, versions, signing, and release
     work out of the diff.
   - Add regression coverage where applicable.

5. **Run focused validation**
   - Run the smallest check that can disprove the change first.
   - Read complete errors and fix causes instead of weakening checks.
   - Treat missing SDKs, JDKs, native runtimes, signing inputs, devices, or
     operating systems as blocked checks.

6. **Run the applicable repository gate**
   - Agent configuration:
     `python3 scripts/validate_agent_config.py`
   - Android:
     `./gradlew --no-daemon :app:testDebugUnitTest`
     `./gradlew --no-daemon :app:lintRelease`
     `./gradlew --no-daemon --no-configuration-cache assembleRelease`
   - Desktop from `desktop/`:
     `./gradlew check`
     `./gradlew assemble check`
   - Diff hygiene:
     `git diff --check`

7. **Inspect the complete diff**
   - Check for unrelated edits, generated files, binaries, secrets, conflict
     markers, accidental version changes, duplicated sources of truth, and
     misleading documentation.
   - Verify that `SPEC.md`, `ROADMAP.md`, `TASKS.md`, architecture, and user
     documentation remain synchronized where affected.

8. **Independent review**
   - Use `levyra-pr-review` on the latest commit.
   - Use CodeRabbit locally or on the pull request when available.
   - Prefer a fresh reviewer model or human that did not implement the change.
   - Require severity, exact location, triggering scenario, consequence,
     smallest compatible fix, and missing test coverage for each finding.
   - Do not accept speculative findings without a concrete failure path.

9. **Publish only when authorized**
   - Stage only intended files.
   - Use a focused professional commit message.
   - Push a dedicated branch and open a draft pull request by default.
   - Record problem, approach, impact, exact checks, blocked checks, manual
     checks, limitations, and rollback/revert scope.

10. **Repeat after every push**
    - Re-run affected validation.
    - Review the latest commit rather than stale comments.
    - Fix or explain every actionable finding.
    - Keep manual checks unmarked until performed.

11. **Owner-controlled completion**
    - Merge only after the final diff, CI, applicable security checks,
      independent review, review threads, and required manual checks are clean.
    - Tagging, publishing, releases, store metadata, and repository settings are
      separate explicit owner actions.

## Role separation

| Role | Responsibility | Must not assume |
| --- | --- | --- |
| ChatGPT Project | Requirements, investigation, architecture, planning, PR interpretation, task preparation | Repository writes or successful validation without evidence |
| Codex | Focused implementation, tests, local validation, branch/PR work when authorized | Permission to merge or release |
| Claude Code | Implementation or independent review using Claude-specific tooling | That its own report is sufficient evidence |
| CodeRabbit | Automated review signal | Authority over repository requirements |
| OpenClaw | Delegation, status collection, recurring checks, and handoff between configured agents | Broad tool access, publication, merge, or release permission |
| Owner | Scope, trade-offs, publication authorization, merge, release | None; final authority remains human |

## Review and publication gates

A pull request is not ready merely because code compiles.

Required evidence depends on scope, but the gate may include:

- focused regression tests;
- Android or Desktop wrapper checks;
- agent-configuration validation;
- lint and packaging;
- security and dependency checks;
- CodeRabbit;
- fresh independent review;
- screenshots or device checks;
- Android Auto, notification, PiP, media-key, installer, update, protocol, or
  native VLC checks;
- truthful unresolved limitations.

A check performed on an older commit does not validate a newer commit when the
change can affect that check.

## Task handoff contract

Every implementation or review agent should return:

- objective and final scope;
- root cause or rationale;
- exact files changed;
- behavior preserved;
- commands/checks and their results;
- skipped or blocked checks and why;
- remaining risks;
- manual checks still required;
- branch and commit;
- pull request state and URL when one exists;
- explicit statement that merge/release did not occur unless separately
  authorized and verified.

## Failure handling

- Do not hide failing output.
- Do not change tests to match incorrect behavior.
- Do not weaken security validation for one provider response.
- Do not convert cancellation into failure.
- Do not mark a task complete because an agent says it is complete.
- When evidence is unavailable, state the exact limitation and leave the gate
  open.
