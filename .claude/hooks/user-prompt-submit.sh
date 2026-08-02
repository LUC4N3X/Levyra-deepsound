#!/usr/bin/env bash
# Levyra UserPromptSubmit hook.
#
# Skill descriptions are only a hint: the model may or may not act on them. This
# hook removes the guesswork by matching the request against the topics the
# Levyra skills cover and stating, as an instruction, which skill to invoke
# before editing. Rules under .claude/rules/ already load on their own from
# 'paths:' frontmatter, so this hook deliberately covers skills only.
#
# Patterns include Italian terms because the repository owner writes requests in
# both languages.
#
# The hook must never break a session: it always exits 0, and it prints either
# valid JSON or nothing at all.

set -uo pipefail

payload="$(cat 2>/dev/null || true)"

# No python3 means no reliable way to read the prompt out of the JSON payload.
# Staying silent is correct: the session continues with skill descriptions alone.
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

# (skill, topic shown back to the model, trigger pattern)
ROUTES = [
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
        "security or privacy exposure",
        r"security|sicurezz|secret|segret|credential|ssrf|redirect|permission|permess|privacy|\bmime\b|keystore|vulnerab",
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
    "and follow its procedure. Do not wait to be asked. If a skill turns out not to "
    "apply once you have read it, say so in one line and continue.",
]

print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "UserPromptSubmit",
        "additionalContext": "\n".join(lines),
    }
}))
' 2>/dev/null || true

exit 0
