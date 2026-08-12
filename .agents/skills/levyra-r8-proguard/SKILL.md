---
name: levyra-r8-proguard
description: Automatically use for Levyra R8, Proguard, minification, resource shrinking, keep rules, consumer rules, release-only crashes, reflection/serialization/JNI shrinking issues, APK size, mapping files, missing classes, or shrinker configuration review.
---

# Levyra R8 / Proguard workflow

## Why this is a first-class skill

Levyra release builds are minified and resource-shrunk. Shrinker changes can therefore improve size and optimization, but can also create failures that appear only in release builds. Treat R8/Proguard work as correctness-sensitive release engineering, not cosmetic cleanup.

This skill adapts the useful workflow from Google's official Android `r8-analyzer` skill to Levyra's current AGP 9.x setup and existing architecture. Do not vendor external analyzer scripts, blanket keep rules, or generic templates without proving they apply.

## Required context

Before changing or judging shrinker configuration:

1. Read root `AGENTS.md`, `app/AGENTS.md`, and `docs/ARCHITECTURE.md`.
2. Read `app/build.gradle.kts`, `gradle/libs.versions.toml`, `gradle.properties`, `app/proguard-rules.pro`, and any affected library `consumer-rules.pro` files.
3. Load `levyra-ci-workflows` for AGP/Gradle/tooling changes and `levyra-release-check` for release validation.
4. Load the affected domain skill as well: player, extractor, database, Compose, security, etc.
5. Inspect the exact code path that relies on reflection, generated serializers, JNI/native lookup, JavaScript bridges, service discovery, annotations, or class names before narrowing a rule.

## Current Levyra assumptions to verify, not blindly duplicate

Levyra currently uses release minification and resource shrinking. Existing rules include targeted handling for Rhino/NewPipe runtime classes, WebView `@JavascriptInterface` methods, warnings, runtime annotations, and kotlinx.serialization-generated serializers.

Treat each of these as a compatibility contract until evidence shows it can be narrowed or removed. A broad rule is a review target, not automatically dead code.

## Analysis workflow

### 1. Establish the actual toolchain

Read the repository versions instead of assuming them. Record AGP, Gradle, Kotlin, KSP, R8 behavior, minify/resource-shrink settings, and the exact release variant under investigation.

Do not upgrade AGP, Gradle, Kotlin, KSP, Compose, or dependencies as collateral cleanup.

### 2. Prefer quantitative R8 analysis when available

If the installed AGP exposes the official R8 configuration analyzer, prefer it over guessing from rule count or file size.

For AGP versions that support it, use the repository wrapper task for the app release variant, for example:

```bash
./gradlew :app:analyzeReleaseR8Config
```

Inspect the generated analyzer report and available Gradle/R8 outputs. If the environment cannot decode a generated protobuf/report format with already-approved local tooling, do not copy random conversion scripts into Levyra. Fall back to a transparent heuristic review and label it as heuristic.

Never claim a rule is high-impact solely because it is long or package-wide; use analyzer evidence when available.

### 3. Heuristic review when quantitative analysis is unavailable

Review rules in this order:

1. package-wide `-keep` rules;
2. rules already supplied by dependency consumer rules;
3. `-keep class ... { *; }` rules that may preserve unnecessary members;
4. reflection/service-loader/class-name lookups;
5. JNI/native entry points and names used across the Java/native boundary;
6. serializers, generated code, annotations, and companion/object lookup;
7. WebView JavaScript bridges;
8. `-dontwarn` rules that may be hiding a real missing-class problem;
9. retained attributes required by reflection or generated frameworks.

For every proposed removal or narrowing, identify the runtime mechanism that makes the rule necessary or unnecessary.

### 4. Consumer-rule awareness

Before adding a keep rule for a library, inspect whether the dependency already packages consumer rules. Avoid duplicating rules that the library already owns.

Before removing a Levyra rule because a library "should" provide it, verify the actual resolved artifact used by Levyra and its consumer rules. Do not rely on documentation for a different version.

### 5. Reflection, serialization, and generated code

Never shrink based on static references alone when runtime lookup is involved.

Check specifically for:

- `Class.forName`, reflection APIs, names stored in strings, service loaders, plugin registries;
- kotlinx.serialization generated serializers and companion/object lookup;
- Room/KSP generated code and schema/runtime access;
- NewPipe/Rhino runtime-generated or dynamically resolved classes;
- WebView methods annotated with `@JavascriptInterface`;
- Media3 or Android components referenced from manifest/resources rather than direct calls.

Prefer the narrowest correct member/class rule over a whole-package keep, but only after release validation proves it safe.

### 6. JNI and native libraries

For any JNI/native path, inspect both Kotlin/Java declarations and native symbol/name lookup. Renaming or removing a class/member used by JNI can fail only in release.

Do not add `-keep class ** { *; }` as a blanket JNI workaround. Keep only the actual entry points or name-sensitive types required by the native boundary.

### 7. `-dontwarn` discipline

A `-dontwarn` rule suppresses diagnostics; it does not fix missing runtime behavior.

Before adding or keeping one, determine whether the missing type is:

- truly optional and unreachable in Levyra;
- platform/JDK-only and safe for the target environment;
- provided at runtime by another artifact;
- or a real packaging/dependency problem.

Do not silence new warnings just to make the release build green.

## Size and optimization claims

Measure produced artifacts. Distinguish:

- dex/code size;
- resources;
- native libraries;
- assets/metadata;
- packaging/compression differences.

Do not claim an APK/AAB size improvement from a Proguard diff alone. Compare equivalent release artifacts built from equivalent inputs.

When resource shrinking is involved, verify resources reached indirectly through names, reflection, WebView, XML, manifests, or native code before deleting keep/discard rules.

## Release-only failure workflow

When debug works and release fails after shrinking:

1. reproduce with the exact release/minified variant;
2. capture the exception/crash and affected path;
3. inspect `mapping.txt`, missing-rules output, usage/seeds/configuration reports when generated;
4. identify whether the failure is removal, renaming, missing attributes, resource shrinking, or an unrelated release configuration difference;
5. apply the narrowest rule or code fix that addresses the root cause;
6. rebuild and rerun the same release reproduction;
7. add a regression test or release-path verification when practical.

Do not "fix" a release crash by disabling minification or resource shrinking unless the owner explicitly requests that tradeoff.

## Mapping and diagnostics

Treat mapping files and shrinker outputs as release evidence. Preserve them for the build being investigated and do not confuse outputs from different commits/variants.

Useful evidence may include:

- `mapping.txt`;
- `usage.txt` / removed-code reports when available;
- seeds/kept-item reports;
- missing rules / missing classes diagnostics;
- R8 configuration analyzer output;
- final merged configuration when the toolchain exposes it.

Do not commit generated reports unless the repository already tracks that exact artifact or the owner explicitly requests it.

## Validation gate

After any R8/Proguard change:

1. run the narrowest relevant release/minified build;
2. verify the affected runtime path on a release-like build when environment permits;
3. run focused tests for reflection/serialization/JNI/WebView/component behavior as applicable;
4. compare artifact size only if size is part of the task;
5. check for new R8 warnings, missing classes, or unexpectedly broad keeps;
6. report every blocked device/runtime check as unverified, not passed.

For high-risk changes affecting playback, extraction, Room, WebView bridges, Android Auto, notifications, update logic, or native code, a successful Gradle build alone is not sufficient evidence.

## Reporting standard

For each finding or change report:

- rule/file and reason it exists;
- evidence that it is redundant, too broad, missing, or required;
- affected runtime mechanism;
- proposed/applied minimal change;
- release-only regression risk;
- analyzer/build/runtime evidence;
- before/after artifact size when measured;
- tests performed and checks still unverified.

Separate measured findings from heuristic suspicions. Never present an analyzer hint as a proven runtime bug without confirming the affected path.
