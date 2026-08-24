#!/usr/bin/env python3
from __future__ import annotations

import json
from dataclasses import asdict, dataclass
from typing import Iterable

try:
    from scripts.agent_skill_router import context_for, route_prompt
except ModuleNotFoundError:
    from agent_skill_router import context_for, route_prompt


@dataclass(frozen=True)
class EvalCase:
    name: str
    prompt: str
    required: tuple[str, ...] = ()
    forbidden: tuple[str, ...] = ()
    max_skills: int = 6
    max_context_bytes: int = 4096


@dataclass(frozen=True)
class EvalResult:
    name: str
    status: str
    prompt: str
    selected: tuple[str, ...]
    missing: tuple[str, ...]
    forbidden_selected: tuple[str, ...]
    context_bytes: int
    max_skills: int
    max_context_bytes: int
    error: str | None = None


CASES = (
    EvalCase(
        "playback-crash",
        "Fix a playback crash when skipping tracks",
        required=("levyra-real-engineering", "levyra-player"),
        forbidden=("levyra-design-taste", "levyra-android-reverse-engineering"),
        max_skills=3,
    ),
    EvalCase(
        "compose-jank",
        "Compose jank while scrolling the album screen",
        required=(
            "levyra-compose",
            "levyra-android-performance",
            "levyra-real-engineering",
        ),
        forbidden=("levyra-design-taste",),
        max_skills=3,
    ),
    EvalCase(
        "compose-semantics",
        "Fix Compose semantics for TalkBack",
        required=("levyra-compose",),
        forbidden=("levyra-release-check",),
        max_skills=1,
    ),
    EvalCase(
        "pending-intent-security",
        "Audit mutable PendingIntent handling",
        required=("levyra-android-intent-security", "levyra-security-review"),
        max_skills=2,
    ),
    EvalCase(
        "reverse-r8",
        "Decompile this APK and recover Kotlin R8 metadata",
        required=(
            "levyra-android-reverse-engineering",
            "levyra-r8-proguard",
            "levyra-security-review",
            "levyra-release-check",
        ),
        max_skills=5,
    ),
    EvalCase(
        "normal-release-apk",
        "Build and validate the release APK",
        required=("levyra-release-check", "levyra-context-efficiency"),
        forbidden=("levyra-android-reverse-engineering",),
        max_skills=2,
    ),
    EvalCase(
        "visual-premium",
        "Make the Now Playing screen more premium",
        required=("levyra-design-taste", "levyra-compose"),
        forbidden=("levyra-ci-workflows",),
        max_skills=2,
    ),
    EvalCase(
        "design-near-miss-kotlin",
        "Upgrade to a modern Kotlin compiler version",
        required=("levyra-ci-workflows",),
        forbidden=("levyra-design-taste", "levyra-release-check"),
        max_skills=2,
    ),
    EvalCase(
        "design-near-miss-gradle",
        "Clean Gradle build outputs",
        required=("levyra-ci-workflows", "levyra-context-efficiency"),
        forbidden=("levyra-design-taste",),
        max_skills=2,
    ),
    EvalCase(
        "design-near-miss-subscription",
        "Integrate a premium subscription API",
        forbidden=("levyra-design-taste",),
        max_skills=1,
    ),
    EvalCase(
        "design-near-miss-screenshot",
        "Inspect this screenshot of a CI stack trace",
        required=("levyra-context-efficiency",),
        forbidden=("levyra-design-taste", "levyra-compose"),
        max_skills=3,
    ),
    EvalCase(
        "extractor-near-miss-agent-token",
        "Reduce Claude token usage in the coding agents",
        forbidden=("levyra-extractor",),
        max_skills=2,
    ),
    EvalCase(
        "extractor-player-token",
        "Investigate an InnerTube player token failure",
        required=("levyra-extractor", "levyra-player"),
        max_skills=3,
    ),
    EvalCase(
        "plain-kotlin-question",
        "Explain Kotlin sealed classes",
        forbidden=("levyra-ci-workflows", "levyra-release-check"),
        max_skills=1,
    ),
    EvalCase(
        "semantic-version-near-miss",
        "Prepare a semantic version release",
        required=("levyra-release-check",),
        forbidden=("levyra-compose",),
        max_skills=1,
    ),
    EvalCase(
        "debugging-route",
        "Debug an intermittent queue state corruption",
        required=("levyra-real-engineering", "levyra-player"),
        max_skills=3,
    ),
    EvalCase(
        "project-manager",
        "Update roadmap acceptance criteria for the active phase",
        required=("levyra-project-manager", "levyra-real-engineering"),
        max_skills=2,
    ),
    EvalCase(
        "openclaw-handoff",
        "Delegate this Levyra fix through OpenClaw",
        required=(
            "levyra-openclaw-orchestrator",
            "levyra-context-efficiency",
            "levyra-project-manager",
        ),
        max_skills=3,
    ),
    EvalCase(
        "cross-domain",
        "Investigate a cross-domain architecture issue across subsystems",
        required=(
            "levyra-real-engineering",
            "levyra-context-efficiency",
            "levyra-engineering",
        ),
        max_skills=3,
    ),
    EvalCase(
        "security-review",
        "Review this code for a concrete security vulnerability",
        required=("levyra-security-review", "levyra-pr-review"),
        max_skills=2,
    ),
)


def evaluate_case(case: EvalCase) -> EvalResult:
    try:
        selected = tuple(skill for skill, _ in route_prompt(case.prompt))
        context_bytes = len(context_for(case.prompt).encode("utf-8"))
    except Exception as exc:
        return EvalResult(
            name=case.name,
            status="ERROR",
            prompt=case.prompt,
            selected=(),
            missing=case.required,
            forbidden_selected=(),
            context_bytes=0,
            max_skills=case.max_skills,
            max_context_bytes=case.max_context_bytes,
            error=f"{type(exc).__name__}: {exc}",
        )

    selected_set = set(selected)
    missing = tuple(skill for skill in case.required if skill not in selected_set)
    forbidden_selected = tuple(
        skill for skill in case.forbidden if skill in selected_set
    )
    over_budget = len(selected) > case.max_skills or context_bytes > case.max_context_bytes
    duplicates = len(selected) != len(selected_set)
    status = "PASS" if not (missing or forbidden_selected or over_budget or duplicates) else "FAIL"

    return EvalResult(
        name=case.name,
        status=status,
        prompt=case.prompt,
        selected=selected,
        missing=missing,
        forbidden_selected=forbidden_selected,
        context_bytes=context_bytes,
        max_skills=case.max_skills,
        max_context_bytes=case.max_context_bytes,
    )


def evaluate_all(cases: Iterable[EvalCase] = CASES) -> tuple[EvalResult, ...]:
    return tuple(evaluate_case(case) for case in cases)


def summary(results: Iterable[EvalResult]) -> dict[str, float | int]:
    items = tuple(results)
    passed = sum(result.status == "PASS" for result in items)
    failed = sum(result.status == "FAIL" for result in items)
    errors = sum(result.status == "ERROR" for result in items)
    context_values = [result.context_bytes for result in items if result.status != "ERROR"]
    return {
        "total": len(items),
        "passed": passed,
        "failed": failed,
        "errors": errors,
        "avg_context_bytes": round(sum(context_values) / len(context_values), 2)
        if context_values
        else 0,
        "max_context_bytes": max(context_values, default=0),
    }


def main() -> int:
    results = evaluate_all()
    payload = {
        "summary": summary(results),
        "results": [asdict(result) for result in results],
    }
    print(json.dumps(payload, indent=2))
    return 0 if all(result.status == "PASS" for result in results) else 1


if __name__ == "__main__":
    raise SystemExit(main())