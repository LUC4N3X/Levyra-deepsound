---
name: levyra-humanizer
description: Apply a compact final prose pass to Levyra pull request descriptions, release notes, and owner-facing writing so it sounds natural without changing facts, evidence, structure, or validation state.
license: MIT
metadata:
  version: "2.11.2-levyra-compact"
  upstream: "blader/humanizer@e2e92e7b4b8229253ed5c8e81dc65463fdeddda5"
---

# Levyra humanizer

## Goal

Make finished prose sound like a person wrote it while preserving every factual
claim. This is a lightweight final pass, not a research, review, or reasoning
stage. Do not reopen the codebase or load extra evidence merely to humanize text.

## Non-negotiable claim parity

- Keep every fact, number, date, path, version, test result, checkbox, citation, limitation, and risk state unchanged unless the source itself requires correction.
- Never turn `UNRUN`, `BLOCKED`, `FAIL`, uncertainty, or missing evidence into a stronger claim.
- Preserve required templates, headings, Markdown structure, code blocks, YAML, links, and release-note schema.
- Do not invent context, praise, motives, benefits, sources, or validation.

## Natural prose pass

Prefer simple verbs, concrete nouns, varied sentence length, and the writer's
existing vocabulary. When a real writing sample is available, match its tone and
punctuation instead of imposing a generic style.

Remove only clear AI-writing residue:

- generic preambles, chatbot closers, fake-candid hooks, and "let me know" offers;
- inflated importance, sales language, vague expert claims, and generic optimism;
- filler, stacked qualifiers, forced groups of three, synonym cycling, and repeated headings;
- excessive bold mini-labels, decorative emoji, and formulaic "not X but Y" phrasing;
- em/en dashes when the writer's sample does not normally use them.

Keep technical language technical. Do not simplify a term when precision would be
lost. One pattern alone is not a reason to rewrite natural prose.

## Embedded PR/release mode

For PR descriptions, release notes, commit-adjacent prose, and other embedded
uses:

1. work only from the already-drafted text and its verified evidence;
2. make one concise rewrite pass;
3. preserve all required Levyra template sections and unperformed checkboxes;
4. return only the final text unless the caller explicitly needs a change summary.

Do not spend engineering context on examples of writing style. The final prose
pass should be much cheaper than the engineering work it describes.

## Final check

Before returning, verify: same claims, same validation truth, same required
structure, no invented facts, no lost limitation, and no obvious chatbot filler.

## Provenance

This compact Levyra adaptation keeps the useful principles of the MIT-licensed
`blader/humanizer` method while removing its large example catalog from the
runtime skill body to reduce context cost.
