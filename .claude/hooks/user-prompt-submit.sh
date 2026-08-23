#!/usr/bin/env bash
set -uo pipefail

payload="$(cat 2>/dev/null || true)"

python_cmd=()
if command -v python3 >/dev/null 2>&1; then
  python_cmd=(python3)
elif command -v python >/dev/null 2>&1; then
  python_cmd=(python)
elif command -v py >/dev/null 2>&1; then
  python_cmd=(py -3)
else
  exit 0
fi

printf '%s' "$payload" | "${python_cmd[@]}" -c '
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
        "levyra-project-manager",
        "requirements, roadmap, active phase, acceptance criteria, or implementation handoff",
        r"requirements?|requisit|roadmap|acceptance criteria|criteri.*accett|active phase|fase attiv|task status|stato.*task|milestone|implementation handoff|handoff.*implement|docs/project/(?:spec|roadmap|tasks)",
    ),
    (
        "levyra-openclaw-orchestrator",
        "OpenClaw delegation or Levyra worker/reviewer/CI coordination",
        r"openclaw|levyra-worker|levyra-reviewer|levyra-ci|delegat.*(?:agent|runtime|review|ci)|orchestrat",
    ),
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
        "Compose UI, accessibility, lifecycle, or state projection",
        r"compose|composable|\bui\b|screen|schermat|theme|\btema\b|layout|recomposition|ricompos|scroll|layout inspector|talkback|semantics|semantic|touch target|accessibilit|rtl|localizzazion|localization|string resource",
    ),
    (
        "levyra-android-performance",
        "Android runtime profiling or measured performance",
        r"perfetto|system trace|trace processor|frame miss|frame drop|jank|latency|latenza|startup|avvio lento|cpu schedul|thread state|runnable|blocked thread|binder wait|binder spam|binder storm|lock contention|renderthread|frame timeline|gpu memory|texture upload|memory pressure|memory leak|allocat|i/o stall|io stall|d-state|lmkd|psi|wakelock|power rail|power trace|battery trace|runtime performance|performance runtime|profiling android",
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
        r"visual design|ui design|design ui|grafica|interfaccia|ui polish|visual polish|gerarchia visual|visual hierarchy|spacing|spaziatur|typograph|tipograf|color palette|palette colori|shape|forme|radius|corner radius|motion ui|ui motion|animazion|screenshot|design reference|ui reference|riferiment.*(?:grafica|ui|schermat)|pi[uù] bella|pi[uù] professionale|(?:\bscreen\b|schermat\w*|\bui\b|\bdesign\b|grafica|interfaccia|\blayout\b|\bplayer\b|\bhome\b|\bnow playing\b).{0,40}\b(?:premium|modern|clean|cinematic|less generic)\b|\b(?:premium|modern|clean|cinematic|less generic)\b.{0,40}(?:\bscreen\b|schermat\w*|\bui\b|\bdesign\b|grafica|interfaccia|\blayout\b|\bplayer\b|\bhome\b|\bnow playing\b)|^\s*(?:premium|modern|clean|cinematic|less generic)\s*[.!?]*\s*$|premium (?:ui|design|grafica)|modern (?:ui|design|grafica)|cinematic (?:ui|design)|cohesive (?:ui|design)|coerent.*(?:ui|grafica|design)|distinctive (?:ui|design)|less ai.*(?:ui|design)|anti.?ai.?slop|glassmorphism|bento",
    ),
    (
        "levyra-motion-artwork",
        "motion artwork",
        r"motion artwork|animated artwork|animated cover|canvas|copertin.*animat|cover art.*animat",
    ),
    (
        "levyra-desktop",
        "Windows Desktop, Compose Multiplatform, libvlc, packaging, or desktop updates",
        r"\bdesktop\b|windows client|compose multiplatform|libvlc|mini player|mini-player|protocol registration|wix|msi|desktop update|desktop release",
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
    (
        "levyra-engineering",
        "genuine cross-domain work or repository architecture orientation",
        r"cross.?domain|pi[uù].*sottosistem|multiple subsystems|several subsystems|repository orientation|architecture orientation|orient.*(?:repo|codebase|architett)",
    ),
]

matched = [(skill, topic) for skill, topic, pattern in ROUTES if re.search(pattern, prompt)]
matched_skills = {skill for skill, _ in matched}
companions = []

if "levyra-compose" in matched_skills and "levyra-android-performance" in matched_skills:
    companions.append(("levyra-real-engineering", "non-trivial Compose performance debugging"))
if "levyra-r8-proguard" in matched_skills:
    companions.append(("levyra-release-check", "minified release validation"))
if "levyra-android-intent-security" in matched_skills:
    companions.append(("levyra-security-review", "Android component-boundary security review"))
if "levyra-design-taste" in matched_skills:
    if "levyra-desktop" in matched_skills:
        companions.append(("levyra-desktop", "Desktop visual implementation"))
    elif re.search(r"android|compose|player|now playing|home|screen|schermat|ui|grafica|interfaccia", prompt):
        companions.append(("levyra-compose", "Android visual implementation"))
if "levyra-openclaw-orchestrator" in matched_skills:
    companions.append(("levyra-context-efficiency", "compact delegation context"))
    companions.append(("levyra-project-manager", "delegated acceptance criteria and handoff"))

for skill, topic in companions:
    if skill not in matched_skills:
        matched.append((skill, topic))
        matched_skills.add(skill)

lines = [
    "Levyra context budget: search/path/symbol first; read bounded ranges; expand only on a concrete need; do not reread unchanged evidence.",
    "Root AGENTS.md is imported by .claude/CLAUDE.md and is mandatory repository context.",
    "Evidence-gated completion: for non-trivial work define observable acceptance gates; only direct evidence is PASS; FAIL/BLOCKED/UNRUN stay open.",
    "Before delivering code, invoke /code-review when available (otherwise the code-review stage), fix actionable findings, then deliver the reviewed final code/diff.",
]

if matched:
    lines += ["", "Mandatory skill load:"]
    for skill, topic in matched:
        lines.append("- %s -> %s" % (topic, skill))
    lines += [
        "",
        "Invoke every matching project skill above before broad Read/Grep/Edit/Write/Bash work. "
        "Do not merely acknowledge the skill name. After a .claude/skills bridge is invoked, "
        "follow it to the canonical .agents/skills/<skill-name>/SKILL.md before editing. "
        "Apply AI_ENGINEERING_GUARDRAILS.md and EVIDENCE_GATED_COMPLETION.md. "
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
