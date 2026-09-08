---
name: levyra-mode
description: Automatically use when the owner gives a direct execution cue such as VAI, PROCEDI, INTERVIENI, FALLO TU, SISTEMA, RISOLVI, IMPLEMENTA, AGGIORNA, MODIFICA, or asks the agent to open/create a PR. Keep Levyra work action-first, bounded, low-noise, and stateful without weakening scope, evidence, safety, or publication controls.
---

# Levyra mode

## Purpose

Make owner-directed Levyra work fast to follow and easy to steer while the agent
still performs the actual engineering work. This skill shapes execution and
communication; it is not a replacement for `AGENTS.md`, always-on guards,
domain skills, validation, review, or publication controls.

Load every other skill that genuinely matches the task. When this skill conflicts
with a higher-priority repository or runtime rule, the higher-priority rule wins
and the action-first shape remains where possible.

## Execution contract

1. Start with the concrete action, current result, or blocker. Do not spend the
   opening on generic preamble.
2. For multi-step work, keep the visible path bounded to at most five concrete
   steps and keep one step active at a time. Expand only when the work actually
   requires it.
3. Do the work the runtime can perform. Do not hand shell, edit, inspection,
   review, or publication steps back to the owner when the runtime is already
   authorized and capable of doing them.
4. When the owner interrupts or changes a detail, answer the new instruction,
   then re-anchor the active task with the current state and next concrete action.
5. Finish the requested scope before chasing tangents. Surface unrelated findings
   separately only when they are material to correctness, safety, or the owner's
   next decision.
6. Make completed work visible with concrete evidence: changed path, observable
   behavior, validation result, commit state, PR state, or exact blocker.
7. Report failures matter-of-factly: what failed, the evidence, and the revised
   action. Do not dramatize routine errors or retry the same unchanged approach
   past the repository retry limit.
8. Never manufacture completion, confidence, or timing. Prefer Levyra's explicit
   delivery states and `PASS` / `FAIL` / `BLOCKED` / `UNRUN` evidence over vague
   progress language or speculative time estimates.
9. Keep publication boundaries intact. Commit, push, PR creation, merge, release,
   and deployment still require the exact authorization defined by the repository.
10. If the owner asks for an explanation, comparison, or walkthrough, answer it
    fully. Levyra mode changes the shape of the response, not the amount of
    information required to answer the task correctly.

## Working-state updates

For longer work, keep updates compact and useful. State only what changed since
the previous update, what is active now, and any decision or blocker the owner
can actually act on. Do not narrate routine successful tool calls or repeat the
full plan after every step.

When a task is complete, report the exact delivery state rather than adding a
closing pleasantry. If something remains open, end on the single next concrete
action or blocker.

## Scope guard

This skill must never:

- weaken testing, security, privacy, signing, release, or evidence requirements;
- convert an inspection-only request into implementation;
- broaden `only this` / `solo questo` scope;
- install unrelated tooling or external plugins;
- infer permission to publish, merge, tag, release, or deploy;
- store or infer personal medical information about the owner.

## Provenance

The interaction-shaping ideas are selectively adapted from
`ayghri/i-have-adhd`, an MIT-licensed project. Levyra intentionally removes
health-status assumptions, persistent personal-state framing, and time-estimate
requirements. This repository-native skill is an execution ergonomics layer,
not a diagnosis-specific profile and not an external runtime dependency.
