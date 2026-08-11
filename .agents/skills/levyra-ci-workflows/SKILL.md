---
name: levyra-ci-workflows
description: Automatically use for Levyra GitHub Actions, CI, F-Droid, Gradle/AGP/Kotlin/KSP compatibility, build performance, configuration/build cache, artifacts, release automation, workflow security, or configuration-sync work.
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

## Gradle build-performance work

Treat build-speed changes as performance engineering, not a bag of `gradle.properties` toggles.

1. Measure a representative baseline first, including the build mode that is actually slow (clean, incremental, CI, Android, or Desktop).
2. Determine whether the time is dominated by configuration, dependency resolution, compilation/processing, execution, packaging, or cache misses before choosing a fix.
3. Inspect existing configuration-cache/build-cache behavior, task inputs/outputs, eager task creation, configuration-time I/O, KSP work, repository resolution, and JDK/toolchain consistency before adding new infrastructure.
4. Apply one material optimization at a time, rerun the same measured path, and keep the change only when the evidence shows an improvement without weakening correctness or reproducibility.
5. Prefer lazy Gradle APIs and Providers when they solve a demonstrated configuration-time problem; do not rewrite working build logic solely to match a generic optimization checklist.
6. Do not add remote caches, Develocity/Build Scan publishing, third-party analytics, new CI services, or data-uploading plugins merely for diagnostics. Any external upload or persistent service requires an explicit task plus privacy, credential, supply-chain, and maintenance review.
7. Do not skip tests/lint in CI to make a timing number look better unless the repository already has a safe path-based job design that preserves the mandatory quality gate.

Record the before/after command, environment-relevant differences, and timing or task evidence when claiming a build-performance improvement. SimpMusic's Gradle performance skill is useful here for its baseline -> isolate phase -> one change -> remeasure discipline; its generic flags and services are not Levyra defaults.

## Validation

Validate YAML structure, expressions, event filters, permissions, matrix behavior, shell quoting, paths, conditions, output propagation, artifact retention, and secret availability. Compare the workflow with the corresponding local Gradle command. For build-tooling changes, also verify the affected plugin graph, wrapper/version compatibility, and the narrowest relevant Android or Desktop build task. For performance changes, rerun the same representative build used for the baseline and report measured evidence rather than an assumed speedup. Report job-log evidence, checks not reproducible locally, and any manual release verification still required.
