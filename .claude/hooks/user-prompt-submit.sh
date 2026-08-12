#!/usr/bin/env bash
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
        "non-trivial engineering, debugging, requirements, or architecture",
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
        "levyra-android-performance",
        "Android runtime profiling or measured performance",
        r"perfetto|system trace|trace processor|frame miss|frame drop|jank|latency|latenza|startup|avvio lento|cpu schedul|thread state|runnable|blocked thread|binder wait|binder spam|binder storm|lock contention|renderthread|frame timeline|gpu memory|texture upload|memory pressure|allocat|i/o stall|io stall|d-state|lmkd|psi|wakelock|power rail|power trace|battery trace|runtime performance|performance runtime|profiling android",
    ),
    (
        "levyra-r8-proguard",
        "R8, Proguard, minification, shrinking, or release-only behavior",
        r"\br8\b|proguard|minif|shrink resources|resource shrink|resource shrinking|keep rule|consumer rule|mapping\.txt|missing rules|missing class|missing classes|release.?only.*(?:crash|fail)|crash.*release|apk size|aab size|obfuscat|shrinker|dontwarn|keepattributes|javascriptinterface|jni.*(?:keep|shrink)|reflection.*(?:keep|shrink)|serialization.*(?:keep|shrink)",
    ),
    (
        "levyra-android-intent-security",
        "Android Intent, PendingIntent, deep-link, or component security",
        r"pendingintent|pending intent|onnewintent|nested intent|intent redirection|intent redirect|intent sanitizer|intentsanitizer|android:exported|exported (?:activity|service|receiver|provider|component)|attivit[aà] esportat|servizio esportat|receiver esportat|provider esportat|mutable pendingintent|immutable pendingintent|flag_mutable|flag_immutable|uri grant|granturipermission|grant uri|fileprovider|contentprovider|signature permission|binder caller|callinguid|caller verification|deep.?link.*(?:intent|security|exported|permission)|intent.*(?:security|sicurezz|exported|permission|forward|redirect|nested)|component boundary|component-boundary",
    ),
    (
        "levyra-design-taste",
        "visual design, redesign, polish, hierarchy, or anti-AI-slop UI",
        r"visual design|ui design|design ui|grafica|interfaccia|ui polish|visual polish|gerarchia visual|visual hierarchy|spacing|spaziatur|typograph|tipograf|color palette|palette colori|shape|forme|radius|corner radius|motion ui|ui motion|animazion|screenshot|design reference|ui reference|riferiment.*(?:grafica|ui|schermat)|pi[uù] bella|pi[uù] professionale|premium (?:ui|design|grafica)|modern (?:ui|design|grafica)|cinematic (?:ui|design)|cohesive (?:ui|design)|coerent.*(?:ui|grafica|design)|distinctive (?:ui|design)|less ai.*(?:ui|design)|anti.?ai.?slop|glassmorphism|bento",
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
        "repository exploration or high-volume context",
        r"\bbuild\b|\bgradle\b|\btest\b|\blint\b|logcat|\blogs?\b|git diff|git log|git status|github|\bgh\b|coderabbit|dependencies|dependency tree|broad search|ricerca ampia|setup|installazione ai|agent setup|analy[sz]|analizz|investigat|indag|inspect|esamina|repository|\brepo\b|codebase|root cause|causa radice|implement|refactor|riprogett|find.*(?:class|function|file)|trova.*(?:classe|funzione|file)",
    ),
    (
        "levyra-security-review",
        "security, privacy, trust-boundary, or supply-chain review",
        r"security|sicurezz|secret|segret|credential|cookie|auth|ssrf|redirect|permission|permess|privacy|\bmime\b|keystore|vulnerab|exploit|threat model|trust boundary|attack surface|dependency|dipendenz|supply.?chain|workflow permission|action pin|signature|checksum|integrity|update security|deep.?link|pendingintent|pending intent|onnewintent|android:exported|fileprovider|contentprovider|uri grant|path traversal|injection|cve|token leak|data leak",
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

lines = [
    "Levyra context budget: search/path/symbol first; read bounded ranges; expand only on a concrete need; do not reread unchanged evidence.",
    "Keep security, Perfetto, R8, signing, exact failures, and decisive diagnostics raw when compression could change the conclusion.",
]

if matched:
    lines += ["", "Matching skills:"]
    for skill, topic in matched:
        lines.append("- %s -> %s" % (topic, skill))
    lines += [
        "",
        "Invoke matching skills before broad reading/editing. Apply AI_ENGINEERING_GUARDRAILS.md. "
        "Use the smallest verified change. New source code should be self-explanatory: add no "
        "explanatory comments; preserve only required license/tooling/suppression comments.",
    ]

print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "UserPromptSubmit",
        "additionalContext": "\n".join(lines),
    }
}))
' 2>/dev/null || true

exit 0
