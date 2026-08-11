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

fallback_json='{"hookSpecificOutput":{"hookEventName":"SessionStart","additionalContext":"Levyra environment probe unavailable. Verify the toolchain before claiming any build or test result."}}'

project_dir="${CLAUDE_PROJECT_DIR:-$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)}"
if ! cd "$project_dir" 2>/dev/null; then
  printf '%s\n' "$fallback_json"
  exit 0
fi

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
# A 'platforms' directory alone proves nothing: a partial or stale SDK can be
# present while the platform the app compiles against is missing, and Gradle
# still fails at configuration time. Require the compileSdk platform itself,
# read from the build file so this does not drift when compileSdk is bumped.
compile_sdk="$(sed -n 's/^[[:space:]]*compileSdk[[:space:]]*=[[:space:]]*\([0-9]\{1,\}\).*/\1/p' app/build.gradle.kts 2>/dev/null | head -1)"

candidates=("${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Android/Sdk" "/usr/lib/android-sdk" "/opt/android-sdk")
if [ -f local.properties ]; then
  candidates+=("$(sed -n 's/^sdk\.dir=//p' local.properties | head -1)")
fi

sdk_root=""
sdk_platform=""
sdk_partial=""
for candidate in "${candidates[@]}"; do
  if [ -z "$candidate" ] || [ ! -d "$candidate" ]; then
    continue
  fi

  missing=""
  found_platform=""
  if [ -n "$compile_sdk" ]; then
    # Recent SDK Manager releases store the base minor SDK as android-<api>.0
    # (for example API 37 as android-37.0), while older installations may still
    # use android-<api>. Accept both, but do not treat a later minor SDK such as
    # android-37.1 as satisfying compileSdk = 37.
    for platform_path in \
      "$candidate/platforms/android-$compile_sdk" \
      "$candidate/platforms/android-$compile_sdk.0"; do
      if [ -f "$platform_path/android.jar" ]; then
        found_platform="${platform_path##*/}"
        break
      fi
    done
    if [ -z "$found_platform" ]; then
      missing="platforms;android-$compile_sdk (or platforms;android-$compile_sdk.0)"
    fi
  elif ! compgen -G "$candidate/platforms/android-*/android.jar" >/dev/null 2>&1; then
    missing="an android-<compileSdk> platform"
  fi

  # Build-tools binaries are extensionless on Unix and use .exe on Windows.
  if ! compgen -G "$candidate/build-tools/*/aapt2" >/dev/null 2>&1 && \
     ! compgen -G "$candidate/build-tools/*/aapt2.exe" >/dev/null 2>&1; then
    missing="${missing:+$missing and }build-tools"
  fi

  if [ -z "$missing" ]; then
    sdk_root="$candidate"
    sdk_platform="$found_platform"
    break
  fi
  if [ -z "$sdk_partial" ]; then
    sdk_partial="$candidate is missing $missing"
  fi
done

sdk_unusable_advice="Do not attempt them, and do not report them as passing or as skipped-by-choice: say the Android SDK is unusable in this environment and that CI (.github/workflows/pr-check.yml) is the authority."
if [ -n "$sdk_root" ]; then
  lines+=("Android SDK: $sdk_root, with platform ${sdk_platform:-android-${compile_sdk:-unknown}} and build-tools present. ':app:' Gradle tasks should configure. That is a precondition, not a result: still run the task and report only what actually ran.")
elif [ -n "$sdk_partial" ]; then
  lines+=("Android SDK: incomplete - $sdk_partial. Every ':app:' Gradle task (testDebugUnitTest, lintRelease, assembleRelease) still fails at configuration time until those packages are installed via sdkmanager. $sdk_unusable_advice")
else
  lines+=("Android SDK: NOT AVAILABLE. Every ':app:' Gradle task (testDebugUnitTest, lintRelease, assembleRelease) will fail at configuration time. $sdk_unusable_advice")
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

python3 - "$context" <<'PY' 2>/dev/null || printf '%s\n' "$fallback_json"
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