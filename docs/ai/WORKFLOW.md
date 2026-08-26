# Levyra AI Development Workflow

## Goal

Use AI agents as controlled engineering collaborators, not as a substitute for
requirements, validation, independent review, or owner approval.

The repository keeps four responsibilities separate:

| File | Responsibility |
| --- | --- |
| `AGENTS.md` and nested variants | Durable operating rules loaded by coding agents |
| `docs/project/SPEC.md` | Approved product and engineering requirements |
| `docs/project/ROADMAP.md` | Ordered outcomes, risks, and phase exit criteria |
| `docs/project/TASKS.md` | One active reviewable phase and its truthful validation state |

Native skills under `.agents/skills/` define repeatable workflows. Claude-specific
canonical rules and runtime configuration live under `.agents/claude/`; the
ignored native `.claude/` tree is only a generated projection. Codex-specific
canonical project configuration lives under `.agents/codex/` and is projected to
ignored `.codex/` when needed.

For non-trivial work, `levyra-real-engineering` adapts Matt Pocock's workflow to
Levyra. It separates unresolved decisions, specification, ticket decomposition,
implementation, and review without replacing Levyra's own requirements,
architecture, domain skills, validation, or owner-controlled publication. See
`docs/ai/MATT_POCOCK_SKILLS.md`.

## Complete lifecycle

1. **Orient**
   - Inspect the real repository, branch, worktree, current diff, architecture,
     tests, build files, and workflows.
   - Read root and nearest `AGENTS.md` files.
   - Read only the relevant parts of `docs/project/SPEC.md`,
     `docs/project/ROADMAP.md`, and the active phase in `docs/project/TASKS.md`.
   - Load every matching native skill.
   - For non-trivial features, architectural work, unclear defects, or
     multi-step changes, additionally load `levyra-real-engineering`.

2. **Resolve only genuine ambiguity**
   - Inspect repository evidence before asking the owner questions.
   - Use `grill-with-docs` when product behavior, compatibility, ownership, or a
     real trade-off remains unresolved.
   - Use `wayfinder` first when several unresolved decisions make an immediate
     specification premature.
   - Skip this stage for small or already-unambiguous work.
   - Add durable vocabulary or an ADR only when it will genuinely reduce future
     ambiguity; do not create documentation as ceremony.

3. **Define the implementation contract**
   - State the requested outcome, non-goals, and behavior that must remain
     unchanged.
   - Once intent is settled, use `to-spec` for work large enough to benefit from
     an implementation-ready specification.
   - Map the work to approved requirements and roadmap outcomes where relevant.
   - Record acceptance criteria, automated checks, manual checks, rollback, and
     owner checkpoints in `docs/project/TASKS.md` when the task is large enough
     to require a tracked phase.
   - Do not invent requirements that the owner did not approve.

4. **Split oversized work**
   - Use `to-tickets` only when the specification is too large for one coherent,
     reviewable implementation.
   - Prefer vertical, independently reviewable slices over horizontal layer
     tasks.
   - Make each ticket self-contained enough for a fresh agent context.
   - Do not create GitHub issues unless the owner explicitly authorized issue
     publication; otherwise keep the ticket set in the approved handoff or
     planning material.

5. **Plan the current ticket or phase**
   - Trace the current control and data flow.
   - Identify the verified root cause for a defect; use `diagnosing-bugs` when
     the cause remains unclear.
   - Choose the smallest coherent design that matches current architecture.
   - List expected files, risks, migration/security/localization implications,
     and focused validation.
   - Stop before implementation only when the owner reserved plan approval.

6. **Implement one reviewable ticket or phase**
   - Use `implement` and `tdd` when the upstream skills are available and the
     logic benefits from deterministic test-first coverage.
   - Prefer a fresh context when moving to an independent ticket, carrying
     forward the approved spec, exact ticket, durable decisions, and direct
     validation evidence rather than stale exploratory chatter.
   - Use a dedicated branch unless the owner explicitly authorized direct
     `main` publication for the exact scope.
   - Touch only files required by the current ticket or phase.
   - Keep unrelated cleanup, dependency changes, versions, signing, and release
     work out of the diff.
   - Add regression coverage where applicable.

7. **Run focused validation**
   - Run the smallest check that can disprove the change first.
   - Read complete errors and fix causes instead of weakening checks.
   - Treat missing SDKs, JDKs, native runtimes, signing inputs, devices, or
     operating systems as blocked checks.

8. **Run the applicable repository gate**
   - Agent configuration:
     `python3 scripts/validate_agent_config.py`
   - AI efficiency/security configuration:
     `python3 scripts/validate_ai_efficiency.py`
   - Matt Pocock integration:
     `python3 scripts/validate_matt_skills.py`
   - Android:
     `./gradlew --no-daemon :app:testDebugUnitTest`
     `./gradlew --no-daemon :app:lintRelease`
     `./gradlew --no-daemon --no-configuration-cache assembleRelease`
   - Desktop from `desktop/`:
     `./gradlew check`
     `./gradlew assemble check`
   - Diff hygiene:
     `git diff --check`

9. **Inspect the complete diff**
   - Check for unrelated edits, generated files, binaries, secrets, conflict
     markers, accidental version changes, duplicated sources of truth, and
     misleading documentation.
   - Verify that `docs/project/SPEC.md`, `docs/project/ROADMAP.md`,
     `docs/project/TASKS.md`, architecture, and user documentation remain
     synchronized where affected.

10. **Independent review**
    - Use the upstream `code-review` stage when available, then apply
      `levyra-pr-review` to the latest commit or diff.
    - Use CodeRabbit locally or on the pull request when available.
    - Prefer a fresh reviewer model or human that did not implement the change.
    - Require severity, exact location, triggering scenario, consequence,
      smallest compatible fix, and missing test coverage for each finding.
    - Do not accept speculative findings without a concrete failure path.
    - Generic style or smell advice never overrides Levyra's documented
      architecture and invariants.

11. **Publish only when authorized**
    - Stage only intended files.
    - Use a focused professional commit message.
    - Push a dedicated branch and open a draft pull request by default unless
      the owner explicitly authorized direct `main` publication for the scope.
    - Record problem, approach, impact, exact checks, blocked checks, manual
      checks, limitations, and rollback/revert scope.
    - Specification or ticket generation does not imply permission to create or
      update GitHub issues.

12. **Repeat after every push**
    - Re-run affected validation.
    - Review the latest commit rather than stale comments.
    - Fix or explain every actionable finding.
    - Keep manual checks unmarked until performed.

13. **Owner-controlled completion**
    - Merge only after the final diff, CI, applicable security checks,
      independent review, review threads, and required manual checks are clean,
      unless the owner explicitly authorizes a narrower documentation-only
      exception for the exact change.
    - Tagging, publishing, releases, store metadata, and repository settings are
      separate explicit owner actions.

## Stage selection

The Matt Pocock workflow is deliberately composable. Use the lightest stage that
reduces real uncertainty or implementation risk:

```text
small obvious change
→ normal Levyra work method

ambiguous feature / architecture
→ grill-with-docs
→ to-spec
→ to-tickets only when needed
→ implement + tdd
→ code-review + levyra-pr-review

large unresolved decision map
→ wayfinder
→ continue from the appropriate stage

unclear defect
→ diagnosing-bugs
→ minimal fix + regression test
→ code-review + levyra-pr-review
```

When the upstream skill package is installed, load the exact stage skill instead
of recreating its instructions from memory. If it is unavailable, follow the
repository-native `levyra-real-engineering` adapter and continue without
blocking ordinary work.

## Role separation

| Role | Responsibility | Must not assume |
| --- | --- | --- |
| ChatGPT Project | Requirements, investigation, architecture, planning, PR interpretation, task preparation | Repository writes or successful validation without evidence |
| Codex | Focused implementation, tests, local validation, branch/PR work when authorized | Permission to merge or release |
| Claude Code | Implementation or independent review using Claude-specific tooling | That its own report is sufficient evidence |
| Google Antigravity | Workspace implementation/review through shared `.agents` skills | That a workspace skill grants publication permission |
| CodeRabbit | Automated review signal | Authority over repository requirements |
| OpenClaw | Delegation, status collection, recurring checks, and handoff between configured agents | Broad tool access, publication, merge, or release permission |
| Owner | Scope, trade-offs, publication authorization, merge, release | None; final authority remains human |

## Review and publication gates

All implementation runtimes share one executable quality contract:

```bash
python3 scripts/ai_quality_gate.py --profile fast
python3 scripts/ai_quality_gate.py --profile full
```

The fast profile is required before commit. The full profile is required before
push or pull-request publication and adds platform checks selected from the
complete branch diff. GitHub PR Check repeats the fast preflight before its
build, lint, manifest, and packaging checks. ChatGPT must run the commands when
it has repository execution, or put them in the implementation handoff without
claiming they passed. A reviewer bot or external skill is supplementary and
cannot replace this gate.

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
- Do not treat an external skill's self-assessment as validation evidence.
- When evidence is unavailable, state the exact limitation and leave the gate
  open.
