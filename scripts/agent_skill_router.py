#!/usr/bin/env python3
"""Shared prompt-to-skill router for Levyra coding runtimes."""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class Route:
    skill: str
    topic: str
    pattern: re.Pattern[str]


def route(skill: str, topic: str, pattern: str) -> Route:
    return Route(skill, topic, re.compile(pattern, re.I))


ROUTES = (
    route(
        "levyra-project-manager",
        "requirements, roadmap, active phase, acceptance criteria, task status, or implementation handoff",
        r"requirements?|requisit|roadmap|acceptance criteria|criteri.*accett|active phase|fase attiv|task status|stato.*task|milestone|implementation handoff|handoff.*implement|docs/project/(?:spec|roadmap|tasks)",
    ),
    route(
        "levyra-openclaw-orchestrator",
        "OpenClaw delegation or Levyra worker/reviewer/CI coordination",
        r"openclaw|levyra-worker|levyra-reviewer|levyra-ci|delegat.*(?:agent|runtime|review|ci)|orchestrat",
    ),
    route(
        "levyra-real-engineering",
        "non-trivial engineering, debugging, requirements, or architecture",
        r"new feature|nuova funzionalit|architecture|architett|refactor|riprogett|redesign|\bspec\b|specifica|roadmap|multi.?step|cross.?domain|pi[uù].*modul|across.*module|grill-with-docs|wayfinder|to-spec|to-tickets|\bbug\b|debug|diagnos|regression|regressione|test failure|test fallit|build failure|build fallit|unexpected behavior|comportamento inaspett|\bcrash\b|race condition|concurrency bug|root cause|causa radice",
    ),
    route(
        "levyra-player",
        "playback, queue, Media3, or MediaSession",
        r"player|playback|riproduz|queue|coda|media3|mediasession|android auto|notification|notifica|prefetch|gapless|crossfade|\bseek\b|\bskip\b|traccia|brano|audio mode|video mode",
    ),
    route(
        "levyra-extractor",
        "stream extraction or network fallback",
        r"extractor|estrattor|innertube|newpipe|youtube|stream|resolver|player.?config|(?:youtube|innertube|player|po)[ -]?token|potoken|visitor data|scraping|403|throttl",
    ),
    route(
        "levyra-database",
        "Room storage, persistence, or schema",
        r"\broom\b|\bdao\b|entity|entità|migration|migrazion|schema|database|\bdb\b|backup|persist",
    ),
    route(
        "levyra-compose",
        "Android Compose UI, accessibility, lifecycle, or state projection",
        r"compose|composable|\bui\b|\bscreen\b|schermat|theme|\btema\b|layout|recomposition|ricompos|scroll|layout inspector|talkback|\bsemantics\b|touch target|accessibilit|rtl|localizzazion|localization|string resource",
    ),
    route(
        "levyra-android-performance",
        "Android runtime profiling, memory stability, or measured performance",
        r"perfetto|system trace|trace processor|frame miss|frame drop|jank|latency|latenza|startup|avvio lento|cpu schedul|thread state|runnable|blocked thread|binder wait|binder spam|binder storm|lock contention|renderthread|frame timeline|gpu memory|texture upload|memory pressure|memory leak|memory growth|memory churn|native memory|native heap|\bram\b|\boom\b|out of memory|low memory|\bpss\b|\brss\b|dumpsys meminfo|heapprofd|bufferpool|bytebuffer|bitmap memory|allocat|i/o stall|io stall|d-state|lmkd|psi|wakelock|power rail|power trace|battery trace|runtime performance|performance runtime|profiling android",
    ),
    route(
        "levyra-r8-proguard",
        "R8, Proguard, minification, shrinking, or release-only behavior",
        r"\br8\b|proguard|minif|shrink resources|resource shrink|resource shrinking|keep rule|consumer rule|mapping\.txt|missing rules|missing class|missing classes|release.?only.*(?:crash|fail)|crash.*release|apk size|aab size|obfuscat|shrinker|dontwarn|keepattributes|javascriptinterface|jni.*(?:keep|shrink)|reflection.*(?:keep|shrink)|serialization.*(?:keep|shrink)|kotlin metadata",
    ),
    route(
        "levyra-android-intent-security",
        "Android Intent, PendingIntent, deep-link, or component security",
        r"pendingintent|pending intent|onnewintent|nested intent|intent redirection|intent redirect|intent sanitizer|intentsanitizer|android:exported|exported (?:activity|service|receiver|provider|component)|attivit[aà] esportat|servizio esportat|receiver esportat|provider esportat|mutable pendingintent|immutable pendingintent|flag_mutable|flag_immutable|uri grant|granturipermission|grant uri|fileprovider|contentprovider|signature permission|binder caller|callinguid|caller verification|deep.?link.*(?:intent|security|exported|permission)|intent.*(?:security|sicurezz|exported|permission|forward|redirect|nested)|component boundary|component-boundary",
    ),
    route(
        "levyra-design-taste",
        "visual design, redesign, polish, hierarchy, or anti-AI-slop UI",
        r"visual design|ui design|design ui|grafica|interfaccia|ui polish|visual polish|gerarchia visual|visual hierarchy|spacing|spaziatur|typograph|tipograf|color palette|palette colori|shape|forme|radius|corner radius|motion ui|ui motion|animazion|design reference|ui reference|riferiment.*(?:grafica|ui|schermat)|(?:ui|screen|schermat|layout|design|grafica|interfaccia).{0,30}screenshot|screenshot.{0,30}(?:ui|screen|schermat|layout|design|grafica|interfaccia|reference|riferimento)|pi[uù] bella|pi[uù] professionale|(?:\bscreen\b|schermat\w*|\bui\b|\bdesign\b|grafica|interfaccia|\blayout\b|\bplayer\b|\bhome\b|\bnow playing\b).{0,40}\b(?:premium|modern|clean|cinematic|less generic)\b|\b(?:premium|modern|clean|cinematic|less generic)\b.{0,40}(?:\bscreen\b|schermat\w*|\bui\b|\bdesign\b|grafica|interfaccia|\blayout\b|\bplayer\b|\bhome\b|\bnow playing\b)|^\s*(?:premium|modern|clean|cinematic|less generic)\s*[.!?]*\s*$|anti.?ai.?slop|glassmorphism|bento",
    ),
    route(
        "levyra-motion-artwork",
        "motion artwork",
        r"motion artwork|animated artwork|animated cover|canvas|copertin.*animat|cover art.*animat",
    ),
    route(
        "levyra-desktop",
        "Windows Desktop, Compose Multiplatform, libvlc, packaging, or desktop updates",
        r"\bdesktop\b|windows client|compose multiplatform|libvlc|mini player|mini-player|protocol registration|wix|msi|desktop update|desktop release",
    ),
    route(
        "levyra-ci-workflows",
        "CI, Gradle/Kotlin tooling, or build performance",
        r"github actions|\bworkflow\b|\bci\b|fdroid|f-droid|\bgradle\b|\bagp\b|\bksp\b|kotlin(?: compiler| plugin| toolchain| version)|(?:upgrade|update|bump|migrat).{0,24}\bkotlin\b|\bkotlin\b.{0,24}(?:gradle|compiler|ksp|toolchain)|build performance|slow build|build lento|configuration cache|build cache|compile time|compilation time|build logic|gradle\.properties|artifact",
    ),
    route(
        "levyra-context-efficiency",
        "repository exploration or high-volume context",
        r"\bbuild\b|\bgradle\b|\btest\b|\blint\b|logcat|\blogs?\b|git diff|git log|git status|github|\bgh\b|coderabbit|dependencies|dependency tree|broad search|ricerca ampia|setup|installazione ai|agent setup|analy[sz]|analizz|investigat|indag|inspect|esamina|repository|\brepo\b|codebase|root cause|causa radice|implement|refactor|riprogett|find.*(?:class|function|file)|trova.*(?:classe|funzione|file)",
    ),
    route(
        "levyra-security-review",
        "security, privacy, trust-boundary, or supply-chain review",
        r"security|sicurezz|secret|segret|credential|cookie|auth|ssrf|redirect|permission|permess|privacy|\bmime\b|keystore|vulnerab|exploit|threat model|trust boundary|attack surface|dependency|dipendenz|supply.?chain|workflow permission|action pin|signature|checksum|integrity|update security|deep.?link|pendingintent|pending intent|onnewintent|android:exported|fileprovider|contentprovider|uri grant|path traversal|injection|cve|token leak|data leak",
    ),
    route(
        "levyra-pr-review",
        "reviewing a branch, commit, diff, or pull request",
        r"review|revision|pull request|\bpr\b|merge|\bdiff\b|commit review|branch review|before merging|prima di merg",
    ),
    route(
        "levyra-release-check",
        "runtime, pre-merge, or release validation",
        r"release|rilasci|versionname|versioncode|app version|release version|versione (?:app|release)|\bapk\b|signing|firma|tag\b|publish|pubblic|emulator|emulatore|physical device|device test|test dispositivo|\badb\b|connectedcheck|smoke test|runtime verification|runtime validation|verifica runtime|pre.?merge",
    ),
    route(
        "levyra-engineering",
        "genuine cross-domain work or repository architecture orientation",
        r"cross.?domain|pi[uù].*sottosistem|multiple subsystems|several subsystems|repository orientation|architecture orientation|orient.*(?:repo|codebase|architett)",
    ),
    route(
        "levyra-android-reverse-engineering",
        "Android artifact decompilation, compiled API extraction, or binary analysis",
        r"decompil|reverse[ -]?engineer|jadx|smali|bundletool|apktool|binary analysis|r8 metadata|kotlin metadata recovery|(?:apk|xapk|aab|dex|jar|aar).{0,40}(?:analy[sz]|inspect|extract|decompil)|(?:analy[sz]|inspect|extract|decompil).{0,40}(?:apk|xapk|aab|dex|jar|aar)|extract.{0,30}(?:api|endpoint).{0,30}(?:apk|xapk|aab|dex|jar|aar)",
    ),
)

AUTOMATED_MARKERS = (
    "<github-webhook-activity",
    "<untrusted_external_data",
    "<system-reminder",
)

MEMORY_RE = re.compile(
    r"memory|\bram\b|\boom\b|out of memory|native heap|allocat|memory churn|memory growth|\bpss\b|\brss\b|lmkd|dumpsys meminfo|heapprofd|bufferpool",
    re.I,
)


def route_prompt(prompt: str) -> list[tuple[str, str]]:
    text = prompt.strip().lower()
    if not text or any(marker in text for marker in AUTOMATED_MARKERS):
        return []

    matched: list[tuple[str, str]] = []
    seen: set[str] = set()
    for item in ROUTES:
        if item.pattern.search(text) and item.skill not in seen:
            matched.append((item.skill, item.topic))
            seen.add(item.skill)

    def add(skill: str, topic: str) -> None:
        if skill not in seen:
            matched.append((skill, topic))
            seen.add(skill)

    def set_topic(skill: str, topic: str) -> None:
        for index, (matched_skill, _) in enumerate(matched):
            if matched_skill == skill:
                matched[index] = (skill, topic)
                return
        add(skill, topic)

    if "levyra-compose" in seen and "levyra-android-performance" in seen:
        add("levyra-real-engineering", "non-trivial Compose performance debugging")
    if "levyra-android-performance" in seen and MEMORY_RE.search(text):
        set_topic("levyra-real-engineering", "memory-regression root-cause analysis and evidence")
    if "levyra-r8-proguard" in seen:
        add("levyra-release-check", "minified release validation")
    if "levyra-android-intent-security" in seen:
        add("levyra-security-review", "Android component-boundary security review")
    if "levyra-design-taste" in seen:
        if "levyra-desktop" in seen:
            add("levyra-desktop", "Desktop visual implementation")
        elif re.search(r"android|compose|player|now playing|home|screen|schermat|ui|grafica|interfaccia", text):
            add("levyra-compose", "Android visual implementation")
    if "levyra-openclaw-orchestrator" in seen:
        add("levyra-context-efficiency", "compact delegation context")
        add("levyra-project-manager", "delegated acceptance criteria and handoff")
    if "levyra-android-reverse-engineering" in seen:
        add("levyra-security-review", "artifact trust-boundary and exposed-sensitive-data review")
        if re.search(r"\br8\b|proguard|obfuscat|mapping\.txt|kotlin metadata", text):
            add("levyra-r8-proguard", "obfuscation and Kotlin/R8 metadata recovery")

    return matched


def context_for(prompt: str) -> str:
    matched = route_prompt(prompt)
    lines = [
        "Levyra automatic skill routing is mandatory. The owner never needs to name a skill.",
        "Use only matching skills; do not preload the whole skill tree.",
    ]
    if not matched:
        lines.append("No specialized Levyra skill matched this prompt; the always-on agent guards still apply.")
        return "\n".join(lines)

    lines.append("Mandatory skill load before broad repository reading, editing, or shell work:")
    for skill, topic in matched:
        lines.append(f"- {topic} -> {skill} -> .agents/skills/{skill}/SKILL.md")
    lines.extend(
        [
            "Load every listed canonical skill automatically from the task itself. Do not wait for the owner to request it by name.",
            "Claude may invoke its .claude/skills bridge/plugin first, but the canonical .agents skill and Levyra guardrails remain authoritative.",
        ]
    )
    return "\n".join(lines)


def _payload() -> dict[str, Any]:
    try:
        value = json.load(sys.stdin)
    except Exception:
        return {}
    return value if isinstance(value, dict) else {}


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--prompt")
    parser.add_argument("--plain", action="store_true")
    args = parser.parse_args()

    data = {} if args.prompt is not None else _payload()
    prompt = args.prompt if args.prompt is not None else str(data.get("prompt") or "")
    context = context_for(prompt)
    if args.plain:
        print(context)
        return 0
    print(
        json.dumps(
            {
                "hookSpecificOutput": {
                    "hookEventName": "UserPromptSubmit",
                    "additionalContext": context,
                }
            }
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())