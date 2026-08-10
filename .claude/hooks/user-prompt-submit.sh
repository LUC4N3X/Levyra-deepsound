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
        "non-trivial requirements, architecture, or multi-step engineering",
        r"new feature|nuova funzionalit|architecture|architett|refactor|riprogett|redesign|\bspec\b|specifica|roadmap|multi.?step|cross.?domain|pi[uù].*modul|across.*module|grill-with-docs|wayfinder|to-spec|to-tickets",
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
        "Compose UI or state projection",
        r"compose|composable|\bui\b|screen|schermat|theme|\btema\b|animation|animazion|layout|accessibilit|localizzazion|localization|string resource",
    ),
    (
        "levyra-motion-artwork",
        "motion artwork",
        r"motion artwork|motion|artwork|copertin|cover art",
    ),
    (
        "levyra-security-review",
        "security, privacy, trust-boundary, or supply-chain review",
        r"security|sicurezz|secret|segret|credential|cookie|auth|ssrf|redirect|permission|permess|privacy|\bmime\b|keystore|vulnerab|exploit|threat model|trust boundary|attack surface|dependency|dipendenz|supply.?chain|workflow permission|action pin|signature|checksum|integrity|update security|deep.?link|path traversal|injection|cve|token leak|data leak",
    ),
    (
        "levyra-pr-review",
        "reviewing the current diff",
        r"review|revision|pull request|\bpr\b|merge|\bdiff\b|before merging|prima di merg",
    ),
    (
        "levyra-release-check",
        "release or version safety",
        r"release|rilasci|versionname|versioncode|\bversion\b|versione|\bapk\b|signing|firma|tag\b|publish|pubblic",
    ),
]

matched = [(skill, topic) for skill, topic, pattern in ROUTES if re.search(pattern, prompt)]
if not matched:
    sys.exit(0)

lines = ["Levyra skill routing - this request matches project skills:", ""]
for skill, topic in matched:
    lines.append("- %s -> invoke the %s skill" % (topic, skill))
lines += [
    "",
    "Invoke each matching skill with the Skill tool BEFORE reading widely or editing, "
    "and follow its procedure. Do not wait to be asked. For real-engineering work, "
    "use the Matt Pocock stage skill selected by the Levyra bridge when the plugin is "
    "available, and skip the full ceremony for tiny unambiguous changes. For security "
    "work, preserve exact evidence and follow threat model, identification, safe "
    "validation, minimal remediation, human review, and revalidation. If a skill turns "
    "out not to apply once read, say so in one line and continue.",
]

print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "UserPromptSubmit",
        "additionalContext": "\n".join(lines),
    }
}))
' 2>/dev/null || true

exit 0
