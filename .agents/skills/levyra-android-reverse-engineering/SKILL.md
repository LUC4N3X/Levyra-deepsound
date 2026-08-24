---
name: levyra-android-reverse-engineering
description: Mandatory Levyra workflow for lawful Android artifact decompilation, APK/XAPK/AAB/DEX/JAR/AAR analysis, API extraction, compiled call-flow tracing, obfuscation analysis, and authorized dynamic diagnostics.
---

# Levyra Android Reverse Engineering

Use this skill whenever the task analyzes compiled Android artifacts rather than
Levyra source directly: APK, XAPK, AAB, DEX, JAR, AAR, jadx/smali output,
compiled API extraction, binary call-flow tracing, or Kotlin/R8 metadata recovery.

This is the cross-runtime canonical adapter. Claude Code may additionally use the
approved `SimoneAvogadro/android-reverse-engineering-skill` plugin. Do not load a
second upstream plugin exposing the same `android-reverse-engineering` skill or
`/decompile` command in parallel.

## Upstream basis

The preferred Claude upstream is `SimoneAvogadro/android-reverse-engineering-skill`
because its workflow is especially strong for native Kotlin/KMP, fingerprint-first
triage, R8-resistant Kotlin-name recovery, Ktor/Apollo/Koin extraction, split APKs,
and Windows/PowerShell operation.

Useful static-analysis ideas from `incogbyte/android-reverse-engineering-claude-skill`
are incorporated here without adding it as a second runtime dependency: AAB/DEX
coverage, bundletool/apktool fallbacks, GraphQL/WebSocket discovery, and focused
security auditing.

## Required workflow

1. Confirm the artifact is owner-controlled, open source, provided for analysis,
   or otherwise expressly authorized for the requested reverse-engineering work.
2. Fingerprint before full decompilation: packaging shape, native vs Flutter/RN/
   Cordova/Xamarin, HTTP stack, obfuscation, notable SDKs, native libraries.
3. Select the narrowest useful toolchain from evidence. For native Android,
   prefer jadx first; use Vineflower/Fernflower only when its output can resolve
   a real jadx limitation. Use bundletool for AAB and apktool only when resource
   decoding requires it.
4. If a required tool is missing, install only that dependency from a trusted
   upstream. Prefer user-local/project-local installation, verify its version,
   and avoid broad package upgrades. Administrator/root elevation still requires
   explicit owner authorization.
5. Inspect manifest and package structure before broad source reading. Separate
   first-party code from bundled libraries.
6. For obfuscated Kotlin/KMP, use metadata/string/annotation evidence and R8 name
   recovery before guessing ownership from mangled class names. Add
   `levyra-r8-proguard` when shrinker behavior is part of the question.
7. Trace only the call flows needed for the requested outcome. Use entry point ->
   ViewModel/controller -> repository/service -> HTTP/client boundary when that
   structure exists.
8. Extract APIs in two levels: a compact complete inventory first, then detailed
   analysis only for requested/high-value flows. Include GraphQL/WebSocket/Ktor
   when detected; do not assume Retrofit.
9. Add `levyra-security-review` for exported components, transport/pinning,
   manifest permissions, hardcoded sensitive material, crypto, update integrity,
   or other trust-boundary findings.
10. Keep generated decompile output and reports outside tracked Levyra source
    unless the owner explicitly asks to commit a specific artifact/document.

## Dynamic analysis boundary

Static analysis is the default. Frida or equivalent instrumentation is a separate
explicit phase, not an automatic escalation.

Use dynamic instrumentation only on owner-controlled or explicitly authorized
targets and only to answer the requested diagnostic question. It may be used for
runtime observation, crash/root-cause diagnosis, call tracing, or verification
of the owner's own software. Do not capture credentials, session secrets, private
user data, or automate bypasses whose purpose is to gain access to protected
third-party functionality.

## Evidence and delivery

Record artifact hash/version when available, exact tools/versions used, the
chosen decompiler path, warnings/partial-decompile limitations, and which
findings are direct evidence versus inference. A decompiler's reconstructed Java
is evidence about bytecode behavior, not proof of the original source text.

Apply `docs/ai/ALWAYS_ON_AGENT_GUARDS.md` and
`docs/ai/EVIDENCE_GATED_COMPLETION.md` throughout. Keep the production-file diff
at zero unless the active task explicitly requires a Levyra implementation
change based on the analysis.
