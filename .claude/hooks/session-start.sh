#!/usr/bin/env bash
# Levyra SessionStart hook.
#
# Reports which build and test commands can actually run in this environment so
# Claude never promises a Gradle result it cannot produce. CLAUDE.md tells Claude
# to run `./gradlew :app:testDebugUnitTest`, `:app:lintRelease`, and
# `assembleRelease`; all three need an Android SDK, which cloud and CI containers
# frequently lack.
#
# The hook must never break a session: it always exits 0 and always prints valid
# JSON on stdout.

set -uo pipefail

project_dir="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
cd "$project_dir" 2>/dev/null || exit 0

lines=()

# --- JDK ---------------------------------------------------------------------
if command -v java >/dev/null 2>&1; then
  # Skip the JAVA_TOOL_OPTIONS banner some environments print before the version.
  java_version="$(java -version 2>&1 | sed -n 's/.*version "\([^"]*\)".*/\1/p' | head -1)"
  lines+=("JDK: ${java_version:-unknown}")
else
  lines+=("JDK: not found - no Gradle build can run.")
fi

# --- Android SDK -------------------------------------------------------------
sdk_root=""
for candidate in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Android/Sdk" "/usr/lib/android-sdk" "/opt/android-sdk"; do
  if [ -n "$candidate" ] && [ -d "$candidate/platforms" ]; then
    sdk_root="$candidate"
    break
  fi
done
if [ -z "$sdk_root" ] && [ -f local.properties ]; then
  configured="$(sed -n 's/^sdk\.dir=//p' local.properties | head -1)"
  if [ -n "$configured" ] && [ -d "$configured/platforms" ]; then
    sdk_root="$configured"
  fi
fi

if [ -n "$sdk_root" ]; then
  lines+=("Android SDK: $sdk_root - :app: Gradle tasks are runnable.")
else
  lines+=("Android SDK: NOT AVAILABLE. Every ':app:' Gradle task (testDebugUnitTest, lintRelease, assembleRelease) will fail at configuration time. Do not attempt them, and do not report them as passing or as skipped-by-choice: say the Android SDK is missing in this environment and that CI (.github/workflows/pr-check.yml) is the authority.")
fi

# --- Desktop build -----------------------------------------------------------
if [ -x desktop/gradlew ]; then
  if command -v java >/dev/null 2>&1; then
    lines+=("Desktop: ./desktop/gradlew is JVM-only and does not need the Android SDK. Use 'cd desktop && ./gradlew --no-daemon check' for desktop changes.")
  fi
fi

# --- Release signing inputs --------------------------------------------------
if [ -f local.properties ]; then
  lines+=("local.properties exists. Never read, echo, or commit it; see local.properties.example for the key names only.")
fi

# --- Repository state --------------------------------------------------------
if branch="$(git rev-parse --abbrev-ref HEAD 2>/dev/null)"; then
  changed="$(git status --porcelain 2>/dev/null | wc -l | tr -d ' ')"
  lines+=("Git: on '$branch' with $changed uncommitted path(s).")
fi

context=""
for line in "${lines[@]}"; do
  context+="- $line"$'\n'
done

python3 - "$context" <<'PY' 2>/dev/null || printf '{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"Levyra toolchain probe unavailable."}}'
import json
import sys

body = "Levyra environment probe:\n" + sys.argv[1]
print(json.dumps({
    "hookSpecificOutput": {
        "hookEventName": "SessionStart",
        "additionalContext": body,
    }
}))
PY

exit 0
