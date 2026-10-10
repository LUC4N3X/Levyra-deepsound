# Domain Docs

Levyra uses a single shared domain-language context for the repository. Platform-specific architecture and constraints remain in `docs/ARCHITECTURE.md`, path-specific `AGENTS.md` files, and focused `levyra-*` skills rather than separate domain glossaries.

## Before exploring

Read these when they exist and are relevant:

- `CONTEXT.md` at the repository root for durable project vocabulary;
- `docs/adr/` for architectural decisions that touch the area being changed;
- `docs/ARCHITECTURE.md` for current ownership and data/control flow;
- the nearest path-specific `AGENTS.md` and matching native Levyra skills.

If `CONTEXT.md` or `docs/adr/` does not exist, proceed silently. Do not create empty scaffolding merely to satisfy the workflow. `grill-with-docs` or `domain-modeling` may create/update them lazily only when a genuinely reusable term or durable architectural decision is resolved.

## Layout

```text
/
├── CONTEXT.md                 optional, created lazily when useful
└── docs/
    └── adr/                   optional, durable architectural decisions only
```

Android and Desktop remain separate implementation targets, but they share Levyra's product language. Do not introduce `CONTEXT-MAP.md` or per-platform context trees unless the repository actually develops distinct domain vocabularies that justify them.

## Vocabulary

When `CONTEXT.md` defines a term, use that term consistently in specs, ticket titles, code identifiers, hypotheses, and tests. Do not invent synonyms that create a second vocabulary.

If a needed concept is missing, first check whether the repository already names it elsewhere. Add it to the glossary only when the concept is reusable enough to reduce future ambiguity.

## ADRs

An ADR is for a durable decision with a real trade-off and meaningful reversal cost. It is not a diary of every implementation choice.

If proposed work contradicts an existing ADR, surface the conflict explicitly before editing. Current owner-approved requirements may reopen an ADR, but an agent must not silently override one.
