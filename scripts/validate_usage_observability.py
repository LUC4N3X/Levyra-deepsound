from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CODEBURN_VERSION = "0.9.24"
HEADROOM_VERSION = "v0.3.0"

errors: list[str] = []

required = (
    ".agents/claude/settings.json",
    "scripts/setup-usage-tools.ps1",
    "scripts/setup-usage-tools.sh",
    "scripts/codeburn-levyra.ps1",
    "scripts/codeburn-levyra.sh",
    "scripts/claude-statusline.sh",
    "docs/ai/USAGE_OBSERVABILITY.md",
)

for relative in required:
    if not (ROOT / relative).is_file():
        errors.append(f"missing required file: {relative}")

settings_path = ROOT / ".agents/claude/settings.json"
if settings_path.is_file():
    try:
        settings = json.loads(settings_path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as exc:
        errors.append(f"invalid Claude settings JSON: {exc}")
    else:
        status_line = settings.get("statusLine")
        if not isinstance(status_line, dict):
            errors.append("Claude settings must define statusLine")
        else:
            if status_line.get("type") != "command":
                errors.append("Claude statusLine type must be command")
            if "scripts/claude-statusline.sh" not in str(status_line.get("command", "")):
                errors.append("Claude statusLine must route through scripts/claude-statusline.sh")
            if status_line.get("refreshInterval") != 10:
                errors.append("Claude statusLine refreshInterval must remain 10 seconds")

for relative in ("scripts/codeburn-levyra.ps1", "scripts/codeburn-levyra.sh"):
    path = ROOT / relative
    if path.is_file():
        text = path.read_text(encoding="utf-8")
        if f"codeburn@{CODEBURN_VERSION}" not in text:
            errors.append(f"{relative} must pin CodeBurn {CODEBURN_VERSION}")
        if "Levyra-deepsound" not in text or "--project" not in text:
            errors.append(f"{relative} must force the Levyra project filter")
        if "overview" not in text or "week" not in text:
            errors.append(f"{relative} must default to a weekly overview")

for relative in ("scripts/setup-usage-tools.ps1", "scripts/setup-usage-tools.sh"):
    path = ROOT / relative
    if path.is_file():
        text = path.read_text(encoding="utf-8")
        if HEADROOM_VERSION not in text:
            errors.append(f"{relative} must pin Headroom {HEADROOM_VERSION}")
        if CODEBURN_VERSION not in text:
            errors.append(f"{relative} must pin CodeBurn {CODEBURN_VERSION}")
        if ".levyra-tools" not in text:
            errors.append(f"{relative} must keep Headroom project-local")
        if "no-wire" not in text.lower() and "NoWire" not in text:
            errors.append(f"{relative} must not let Headroom rewrite global Claude settings")
        if "no-path" not in text.lower() and "NoPath" not in text:
            errors.append(f"{relative} must not let Headroom rewrite PATH")

gitignore = ROOT / ".gitignore"
if gitignore.is_file() and "/.levyra-tools/" not in gitignore.read_text(encoding="utf-8"):
    errors.append(".gitignore must exclude /.levyra-tools/")

if errors:
    for error in errors:
        print(f"FAIL: {error}")
    raise SystemExit(1)

print("PASS: Levyra AI usage observability configuration is coherent")
