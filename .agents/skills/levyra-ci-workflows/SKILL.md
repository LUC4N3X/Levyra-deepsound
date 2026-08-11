---
name: levyra-ci-workflows
description: Implement, debug, or review Levyra GitHub Actions, CI checks, Android and Desktop builds, artifact handling, release automation, F-Droid, configuration sync, permissions, workflow security, and Kotlin/Gradle/AGP build-tooling compatibility.
---

# Levyra CI and workflow workflow

## Required context

1. Read the root `AGENTS.md` and `.github/AGENTS.md`.
2. Read `.claude/rules/testing-release.md` and `.claude/rules/security.md`.
3. Inspect every workflow, reusable action, script, build file, secret/input contract, artifact path, and trigger affected by the change.
4. Inspect recent failing job logs when the task is a CI failure; do not infer a root cause from the check title alone.

## Guardrails

- Keep workflow permissions at least privilege and explicit where practical.
- Treat `pull_request`, `pull_request_target`, forks, checked-out code, issue comments, and workflow dispatch inputs as distinct trust boundaries.
- Never expose secrets to untrusted code or upload secret-bearing files as artifacts.
- Reuse existing release, validation, extractor-sync, F-Droid, and duplicate-guard workflows instead of creating parallel automation.
- Pin or constrain third-party actions according to the repository's existing policy and review supply-chain impact before adding one.
- Keep Android and Desktop release triggers, versions, tags, artifacts, and Latest-release behavior separate.
- Preserve artifact names and paths relied on by downstream jobs or release steps.
- Ensure caches use safe, deterministic keys and cannot substitute untrusted executable output across trust boundaries.
- Make no-change, skipped, cancelled, and failed outcomes visible and semantically distinct.

## Kotlin and AGP 9 guardrails

Levyra currently uses AGP 9.x and built-in Kotlin support in the Android application module. Treat build-tooling edits as compatibility work, not routine cleanup.

Before changing AGP, Kotlin, KSP, Gradle, Compose tooling, source sets, compiler options, or module plugins:

1. inspect `settings.gradle.kts`, root and affected module `build.gradle.kts` files, `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties`, and `gradle.properties`;
2. classify the affected module from its actual plugins instead of inferring KMP merely because the repository also contains a Desktop client;
3. compare the proposed change with the current official Kotlin/Android Gradle guidance when behavior is version-sensitive;
4. keep the change limited to the migration or compatibility issue that actually exists.

Levyra-specific rules:

- Do not reintroduce `org.jetbrains.kotlin.android` into the AGP 9 Android app; AGP provides built-in Kotlin support for the Android application plugin.
- Keep annotation processing on KSP where supported. Do not introduce `org.jetbrains.kotlin.kapt` as a convenience workaround; any legacy-kapt fallback requires an explicit task and compatibility justification.
- When compiler options must change, use the current Kotlin compiler DSL rather than reviving deprecated `android.kotlinOptions` configuration.
- Do not restructure Android and Desktop into a shared KMP application merely because a generic migration guide recommends that layout. Levyra's Android and Desktop products remain independently versioned and released unless the owner approves an architectural change.
- Do not upgrade AGP, Kotlin, KSP, Gradle, JDK, SDK tools, or Compose dependencies as collateral cleanup in an unrelated CI/build fix.
- If the official `Kotlin/kotlin-agent-skills` package is available, use `kotlin-tooling-agp9-migration` as an additional reference for AGP 9 migration work, then adapt it to Levyra's real module graph and repository rules rather than copying its target structure wholesale.

These guardrails intentionally take the useful AGP 9 compatibility checks from JetBrains' Kotlin agent skill while keeping Levyra's existing architecture authoritative.

## Validation

Validate YAML structure, expressions, event filters, permissions, matrix behavior, shell quoting, paths, conditions, output propagation, artifact retention, and secret availability. Compare the workflow with the corresponding local Gradle command. For build-tooling changes, also verify the affected plugin graph, wrapper/version compatibility, and the narrowest relevant Android or Desktop build task. Report job-log evidence, checks not reproducible locally, and any manual release verification still required.
