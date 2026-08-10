# ChatGPT Levyra activation trigger

Use this as the persistent ChatGPT instruction for Levyra conversations.

## Trigger

When the owner says **"lavoriamo su Levyra"**, **"work on Levyra"**, or an
obvious equivalent, immediately switch to Levyra repository mode for the rest of
the task.

Do not require the owner to repeat the repository URL or the engineering rules.

Repository: `LUC4N3X/Levyra-deepsound`

## Automatic behavior

When repository access is available:

1. open the current `LUC4N3X/Levyra-deepsound` repository;
2. read root `AGENTS.md` before technical conclusions or edits;
3. read the nearest path-specific `AGENTS.md` files for the affected paths;
4. read `docs/ai/AI_ENGINEERING_GUARDRAILS.md`;
5. read only the relevant planning material in `docs/project/`;
6. load every matching `levyra-*` skill under `.agents/skills/`;
7. inspect current architecture, implementation, tests, build files, and
   workflows before relying on remembered repository state;
8. prefer the smallest coherent change that reuses existing ownership;
9. stop and split work when an AI engineering scope checkpoint is crossed;
10. validate with direct evidence and report what remains unverified.

If repository access is unavailable, do not pretend that current files were
inspected. Ask for or use the minimum repository material needed for the task,
or clearly limit the answer to planning based on known context.

## Anti-slop defaults

Once the trigger is active:

- code volume is not a success metric;
- broad feature-parity work must be split into reviewable phases;
- existing owners, repositories, stores, caches, clients, services, and flows
  must be reused before creating new ones;
- duplicate sources of truth and parallel infrastructure are prohibited unless
  the owner explicitly approves a demonstrated architectural need;
- unexpected diff growth triggers re-analysis instead of autonomous expansion;
- speculative refactors, future-proof abstractions, unrelated cleanup,
  dependency churn, and version changes are out of scope unless requested;
- a green build does not override architectural problems found in the final
  diff review.

## Publication boundary

The trigger grants context, not write authorization.

Never infer permission to commit, push, open or merge a pull request, tag,
release, publish, change versions, or modify repository settings. Those actions
still require explicit authorization for the exact action and scope.

## Suggested saved instruction

For a general ChatGPT conversation, the persistent instruction can be kept
compact:

> When I say "lavoriamo su Levyra", use `LUC4N3X/Levyra-deepsound` as the current
> project, inspect its current `AGENTS.md` and `docs/ai/AI_ENGINEERING_GUARDRAILS.md`
> before technical work, reuse the existing architecture, keep changes small and
> verifiable, and never publish or merge without my explicit approval.

Repository evidence always overrides remembered implementation details.
