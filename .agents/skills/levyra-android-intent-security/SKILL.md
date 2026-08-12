---
name: levyra-android-intent-security
description: Automatically use for Levyra Android Intent, deep-link, PendingIntent, exported component, receiver, service, provider, URI-grant, FileProvider, caller-verification, or onNewIntent security work. Pair it with levyra-security-review and the affected Android domain skill.
---

# Levyra Android Intent security workflow

## Purpose

This is Levyra's Android-specific component-boundary security layer. It adapts
selected practices from Google's `android/skills` Android Intent security guide
to Levyra's actual architecture and minSdk without vendoring the upstream skill.

Always pair it with `levyra-security-review`. Also load the affected domain skill
such as `levyra-player`, `levyra-compose`, or `levyra-release-check` when the
boundary belongs to that area.

## Required context

1. Read root `AGENTS.md` and `app/AGENTS.md`.
2. Read `docs/ARCHITECTURE.md`, `docs/ai/CODEX_SECURITY.md`, and
   `.claude/rules/security.md`.
3. Inspect the affected manifest entries, intent filters, exported state,
   permissions, component implementation, caller path, URI/provider contract,
   PendingIntent creation, and tests.
4. Treat all incoming Intents, nested Intents, deep-link data, extras, ClipData,
   URI grants, and caller identity as untrusted unless a stronger verified trust
   boundary applies.
5. Preserve the exact existing external contract unless the task explicitly
   changes it. Security hardening must not silently break legitimate Android
   Auto, notification, share, deep-link, update, or media flows.

## Component exposure

- Internal activities, services, receivers, and providers should remain
  non-exported unless external access is part of the feature contract.
- Every exported privileged component needs a concrete external caller/use case
  plus the narrowest suitable protection: explicit intent contract, permission,
  caller verification, signature trust, or validated public input.
- Do not infer safety from `android:exported=false` alone when another exported
  component can proxy attacker-controlled data into the private component.
- Review manifest aliases, intent filters, provider authorities, dynamic
  receiver flags, and alternate warm-start paths together with the primary
  component.

## Incoming and nested Intents

For an incoming Intent:

1. allow only actions/categories/data schemes/hosts/types/extras actually used by
   the feature;
2. type-check and bound attacker-controlled extras before use;
3. validate URI authorities and permission-grant flags before forwarding or
   persisting access;
4. prefer explicit internal targets;
5. apply the same validation in `onNewIntent` or any reused-activity path as in
   the initial launch path.

Never launch or forward an attacker-controlled nested Intent directly.

When nested Intent forwarding is truly required, prefer `IntentSanitizer` or an
explicit equivalent allowlist that constrains target component/package, action,
data, type, categories, extras, and permitted flags. Reject or strip URI grant
flags that are not part of the approved contract.

Do not cargo-cult an upstream sample. Verify the actual AndroidX Core version and
the exact APIs available in Levyra before choosing a sanitizer implementation.

## PendingIntent

- Default to `PendingIntent.FLAG_IMMUTABLE`.
- Use mutable PendingIntents only for a platform feature that genuinely requires
  receiver-side mutation, such as an approved remote-input flow.
- A mutable PendingIntent must be narrowly scoped to an explicit trusted target;
  never combine mutability with an unconstrained implicit Intent.
- Preserve uniqueness/request-code/update semantics relied on by notification,
  media controls, alarms, widgets, or other callers. Security changes must not
  accidentally alias unrelated PendingIntents.
- Review both the base Intent and who receives the token; a PendingIntent grants
  the receiver the creator's authority for the represented operation.

## Receivers and broadcasts

- Prefer non-exported dynamic receivers for app-internal events when a broadcast
  is actually needed; do not reintroduce deprecated local-broadcast patterns.
- Protect custom externally callable receivers with the narrowest permission or
  verified sender contract.
- Treat ordinary broadcasts as spoofable unless the platform contract provides a
  trusted/system-only boundary.
- Do not trust an action string or extra marker as caller authentication.

## Services and Binder callers

For exported or cross-app privileged services:

- identify the real Binder/caller boundary before choosing where to authenticate;
- use caller UID/package/signing checks when the feature contract depends on a
  trusted app identity;
- account for UID-to-multiple-package mappings and signing-certificate rotation
  where applicable;
- do not perform a one-time check at a lifecycle point that can be bypassed by a
  cached Binder connection if authorization is required per privileged call;
- same-process calls may follow an internal path, but that does not make external
  entry points trusted.

## Providers, FileProvider, and URI grants

- Keep internal providers non-exported.
- For exported providers, constrain read/write operations, projection, selection,
  paths, MIME types, and permissions to the documented contract.
- Grant URI access only for the exact URI and duration required; avoid broad or
  persistent grants unless the feature explicitly needs them.
- Preserve existing FileProvider path boundaries and never broaden roots merely
  to make sharing work.
- Parameterize database/provider queries instead of concatenating untrusted
  selection data.

## Deep links

- Treat scheme/host/path/query/fragment and every derived identifier as untrusted.
- Separate navigation intent from privileged operations. Opening a screen is not
  authorization to perform account, file, update, playback, or destructive
  actions.
- Validate both cold-start and `onNewIntent` delivery.
- Reject unexpected schemes, authorities, encoded traversal, malformed IDs, or
  privilege-bearing nested payloads before state mutation.

## Security review method

Follow `levyra-security-review`'s closed loop:

```text
threat model -> identify path -> safe validation -> minimal remediation
-> human review -> revalidation
```

For every finding state:

- attacker-controlled entry point;
- exported/caller trust boundary;
- exact data or token being trusted;
- path to the privileged/private operation;
- concrete consequence;
- safe reproduction or validation evidence;
- smallest compatible fix;
- regression/revalidation needed.

A broad exported component, mutable PendingIntent, or nested Intent is a review
signal, not automatically a vulnerability. Confirm the reachable failure path.

## Validation

Use the narrowest relevant checks, then the repository quality gate. Depending
on the change this may include:

- manifest/component inspection for every build/source-set variant involved;
- focused unit tests for sanitization/allowlists;
- instrumentation tests for exported-component behavior and URI grants;
- cold-start and `onNewIntent` deep-link checks;
- notification/media PendingIntent behavior;
- negative tests proving rejected callers, targets, flags, extras, or URIs;
- release/minified verification when reflection/component lookup is involved.

Device/emulator, external-caller, Android Auto, notification, provider, or
permission behavior remains unverified unless it was actually exercised.

## Provenance

This workflow is informed by Google's `android/skills` `android-intent-security`
guide. Levyra keeps a compact native adaptation rather than copying its generic
sample application assumptions or reference code. Current Android documentation,
Levyra's architecture, and direct repository evidence take precedence.
