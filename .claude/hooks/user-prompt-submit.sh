#!/usr/bin/env bash
# Levyra UserPromptSubmit hook.
# Routes real user requests to matching project skills before broad reading or
# editing. The hook must never break a session: it always exits 0 and prints
# valid JSON or nothing.

set -uo pipefail

payload="$(cat 2>/dev/null || true)"
command -v python3 >/dev/null 2>&1 || exit 0

printf '%s' "$payload" | python3 -c '
import json
import re
import sys

try:
    data = json.load(sys.stdin)
except Exception:
    sys.exit(0)

prompt = str(data.get("prompt", "")).lower()
if not prompt.strip():
    sys.exit(0)

AUTOMATED_MARKERS = (
    "<github-webhook-activity",
    "<untrusted_external_data",
    "<system-reminder",
)
if any(marker in prompt for marker in AUTOMATED_MARKERS):
    sys.exit(0)

ROUTES = [
    (
        "levyra-real-engineering",
        "non-trivial engineering, root-cause debugging, requirements, or architecture",
        r"new feature|nuova funzionalit|architecture|architett|refactor|riprogett|redesign|\bspec\b|specifica|roadmap|multi.?step|cross.?domain|pi[uù].*modul|across.*module|grill-with-docs|wayfinder|to-spec|to-tickets|\bbug\b|regression|regressione|test failure|test fallit|build failure|build fallit|unexpected behavior|comportamento inaspett|\bcrash\b|race condition|concurrency bug",
    ),
    (
        "levyra-player",
        "playback, queue, or MediaSession",
        r"player|playback|riproduz|queue|coda|media3|mediasession|android auto|notification|notifica|prefetch|gapless|crossfade|\bseek\b|\bskip\b|traccia|brano|audio mode|video mode",
    ),
    (
        "levyra-extractor",
        "stream extraction or network fallback",
        r"extractor|estrattor|innertube|newpipe|youtube|stream|resolver|player.?config|\btoken\b|scraping|403|throttl",
    ),
    (
        "levyra-database",
        "Room storage or schema",
        r"\broom\b|\bdao\b|entity|entità|migration|migrazion|schema|database|\bdb\b|backup|persist",
    ),
    (
        "levyra-compose",
        "Compose UI, performance, accessibility, or state projection",
        r"compose|composable|\bui\b|screen|schermat|theme|\btema\b|animation|animazion|layout|jank|recomposition|ricompos|scroll|perfetto|layout inspector|talkback|semantics|semantic|touch target|accessibilit|rtl|localizzazion|localization|string resource",
    ),
    (
        "levyra-motion-artwork",
        "motion artwork",
        r"motion artwork|motion|artwork|copertin|cover art",
    ),
    (
        "levyra-ci-workflows",
        "CI, Gradle/Kotlin tooling, or build performance",
        r"github actions|\bworkflow\b|\bci\b|fdroid|f-droid|\bgradle\b|\bagp\b|\bkotlin\b|\bksp\b|build performance|slow build|build lento|configuration cache|build cache|compile time|compilation time|build logic|gradle\.properties|artifact",
    ),
    (
        "levyra-context-efficiency",
        "noisy command output or broad repository diagnostics",
        r"\bbuild\b|\bgradle\b|\btest\b|\blint\b|logcat|\blogs?\b|git diff|git log|git status|github|\bgh\b|coderabbit|dependencies|dependency tree|broad search|ricerca ampia|setup|installazione ai|agent setup",
    ),
    (
        "levyra-security-review",
        "security, privacy, trust-boundary, or supply-chain review",
        r"security|sicurezz|secret|segret|credential|cookie|auth|ssrf|redirect|permission|permess|privacy|\bmime\b|keystore|vulnerab|exploit|threat model|trust boundary|attack surface|dependency|dipendenz|supply.?chain|workflow permission|action pin|signature|checksum|integrity|update security|deep.?link|path traversal|injection|cve|token leak|data leak",
    ),
    (
        "levyra-pr-review",
        "reviewing a branch, commit, diff, or pull request",
        r"review|revision|pull request|\bpr\b|merge|\bdiff\b|commit review|branch review|before merging|prima di merg",
    ),
    (
        "levyra-release-check",
        "runtime, pre-merge, or release validation",
        r"release|rilasci|versionname|versioncode|\bversion\b|versione|\bapk\b|signing|firma|tag\b|publish|pubblic|emulator|emulatore|physical device|device test|test dispositivo|\badb\b|connectedcheck|smoke test|runtime verification|runtime validation|verifica runtime|pre.?merge",
    ),
]

matched = [(skill, topic) for skill, topic, pattern in ROUTES if re.search(pattern, prompt)]
if not matched:
    sys.exit(0)

lines = ["Levyra automatic skill routing - this request matches project skills:", ""]
for skill, topic in matched:
    lines.append("- %s -> invoke the %s skill" % (topic, skill))
lines += [
    "",
    "Invoke every matching skill with the Skill tool BEFORE reading widely, editing, "
    "or running large commands. Do not wait for the owner to name a skill or type a "
    "slash command. For real-engineering bugs/failures, use the hypothesis-driven "
    "debugging lane before stacking speculative fixes. For CI/build-performance work, "
    "measure before changing configuration and remeasure the same path afterward. For "
    "Compose performance/accessibility work, require direct evidence where applicable. "
    "For emulator/device validation, prefer semantic UI targets over raw coordinates. "
    "For security work, preserve exact evidence and follow threat model, identification, "
    "safe validation, minimal remediation, human review, and revalidation. If a skill "
    "turns out not to apply once read, say so in one line and continue.",
]

print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "UserPromptSubmit",
        "additionalContext": "\n".join(lines),
    }
}))
' 2>/dev/null || true

exit 0
